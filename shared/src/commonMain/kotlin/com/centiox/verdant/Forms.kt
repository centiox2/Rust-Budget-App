@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.centiox.verdant

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.datetime.*

internal suspend fun finishSheet(hide: suspend () -> Unit, dismiss: () -> Unit) {
    try {
        hide()
    } catch (e: CancellationException) {
        currentCoroutineContext().ensureActive()
    }
    dismiss()
}

@Composable
fun EntrySheet(
    entry: Entry?,
    initialType: String,
    month: String,
    onDismiss: () -> Unit,
    onSave: suspend (Entry) -> Unit,
    onDelete: () -> Unit,
    isNew: Boolean = entry == null,
    draft: EntryDraft? = null,
    onDraft: (EntryDraft?) -> Unit = {},
    onDuplicate: (Entry) -> Unit = {},
    onTemplate: (Entry) -> Unit = {},
) {
    val ledger = LocalLedger.current
    val restoring = draft?.takeIf { it.sourceId == if (isNew) null else entry?.id }
    val categories =
        (ledger.activeCategories() + listOfNotNull(entry?.category, restoring?.category)).distinct()
    val draftId = rememberSaveable(entry?.id) { entry?.id ?: restoring?.id ?: newId() }
    var type by
        rememberSaveable(entry?.id) {
            mutableStateOf(restoring?.type ?: entry?.type ?: initialType)
        }
    var title by
        rememberSaveable(entry?.id) { mutableStateOf(restoring?.title ?: entry?.title ?: "") }
    var amount by
        rememberSaveable(entry?.id) {
            mutableStateOf(
                restoring?.amount
                    ?: entry?.takeIf { it.amount > 0 }?.let { amountInput(it.amount) }
                    ?: ""
            )
        }
    var category by
        rememberSaveable(entry?.id) {
            mutableStateOf(
                restoring?.category
                    ?: entry?.category
                    ?: (if (initialType == "income") ledger.lastIncomeCategory
                        else ledger.lastExpenseCategory)
                        .takeIf { it in categories }
                    ?: categories.first()
            )
        }
    var date by
        rememberSaveable(entry?.id) {
            mutableStateOf(
                restoring?.date
                    ?: entry?.date
                    ?: if (month == currentMonth()) today().toString() else "$month-01"
            )
        }
    var account by
        rememberSaveable(entry?.id) {
            mutableStateOf(
                restoring?.accountId
                    ?: entry?.accountId
                    ?: ledger.accounts
                        .firstOrNull { it.id == ledger.lastAccountId && !it.archived }
                        ?.id
                    ?: ledger.accounts.first { !it.archived }.id
            )
        }
    var destination by
        rememberSaveable(entry?.id) {
            mutableStateOf(
                restoring?.targetAccountId
                    ?: entry?.targetAccountId
                    ?: ledger.accounts.firstOrNull { !it.archived && it.id != account }?.id
            )
        }
    var note by
        rememberSaveable(entry?.id) { mutableStateOf(restoring?.note ?: entry?.note.orEmpty()) }
    var cleared by
        rememberSaveable(entry?.id) {
            mutableStateOf(restoring?.cleared ?: entry?.cleared ?: false)
        }
    var splitCategories by
        rememberSaveable(entry?.id) {
            mutableStateOf(
                ArrayList(
                    restoring?.splits?.map { it.category }
                        ?: entry?.splits?.map { it.category }.orEmpty()
                )
            )
        }
    var splitAmounts by
        rememberSaveable(entry?.id) {
            mutableStateOf(
                ArrayList(
                    restoring?.splits?.map { it.amount }
                        ?: entry?.splits?.map { amountInput(it.amount) }.orEmpty()
                )
            )
        }
    var calculator by remember { mutableStateOf(false) }
    var calculatorInput by remember { mutableStateOf("") }
    var feeEnabled by
        rememberSaveable(entry?.id) {
            mutableStateOf(
                restoring?.feeEnabled
                    ?: (entry?.fee != null ||
                        (isNew &&
                            ledger.currency == "KES" &&
                            initialType != "income" &&
                            (ledger.accounts.find { it.id == account }?.kind == "M-Pesa" ||
                                (ledger.mpesaFeesDefault &&
                                    ledger.accounts.find { it.id == account }?.kind == "General"))))
            )
        }
    var feeKind by
        rememberSaveable(entry?.id) {
            mutableStateOf(restoring?.feeKind ?: entry?.fee?.kind ?: ledger.lastMpesaFeeKind)
        }
    var feeInput by
        rememberSaveable(entry?.id) {
            mutableStateOf(restoring?.feeInput ?: entry?.fee?.amount?.let(::amountInput).orEmpty())
        }
    var feeAutomatic by
        rememberSaveable(entry?.id) {
            mutableStateOf(restoring?.feeAutomatic ?: (isNew && ledger.currency == "KES"))
        }
    var feeConfirmed by
        rememberSaveable(entry?.id) {
            mutableStateOf(restoring?.feeConfirmed ?: entry?.fee?.confirmed ?: false)
        }
    val principal = runCatching { parseAmount(amount) }.getOrNull()
    val quote =
        if (ledger.currency == "KES" && principal != null) quoteMpesaFee(feeKind, principal)
        else null
    var quoteSignature by
        rememberSaveable(entry?.id) {
            mutableStateOf(
                if (restoring?.feeAutomatic == true) "${restoring.feeKind}|${restoring.amount}"
                else ""
            )
        }
    LaunchedEffect(feeAutomatic, feeKind, amount) {
        if (feeAutomatic) {
            feeInput = quote?.amount?.let(::amountInput).orEmpty()
            if (quoteSignature != "$feeKind|$amount") feeConfirmed = false
            quoteSignature = "$feeKind|$amount"
        }
    }
    LaunchedEffect(type) {
        if (feeAutomatic && type == "income") feeKind = "Receive money"
        else if (feeAutomatic && feeKind == "Receive money") feeKind = ledger.lastMpesaFeeKind
    }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val completed = remember(entry?.id) { mutableStateOf(false) }
    LaunchedEffect(
        draftId,
        type,
        title,
        amount,
        category,
        date,
        account,
        destination,
        note,
        cleared,
        splitCategories,
        splitAmounts,
        busy,
        feeEnabled,
        feeKind,
        feeInput,
        feeAutomatic,
        feeConfirmed,
    ) {
        delay(500)
        if (!busy && !completed.value && (title.isNotBlank() || amount.isNotBlank()))
            onDraft(
                EntryDraft(
                    draftId,
                    if (isNew) null else entry?.id,
                    type,
                    title,
                    amount,
                    category,
                    date,
                    account,
                    destination,
                    note,
                    splitCategories.mapIndexed { i, c -> SplitDraft(c, splitAmounts[i]) },
                    cleared,
                    feeEnabled,
                    feeKind,
                    feeInput,
                    feeAutomatic,
                    feeConfirmed,
                )
            )
    }
    val latestDraft by
        rememberUpdatedState(
            EntryDraft(
                draftId,
                if (isNew) null else entry?.id,
                type,
                title,
                amount,
                category,
                date,
                account,
                destination,
                note,
                splitCategories.mapIndexed { i, c -> SplitDraft(c, splitAmounts[i]) },
                cleared,
                feeEnabled,
                feeKind,
                feeInput,
                feeAutomatic,
                feeConfirmed,
            )
        )
    val latestBusy by rememberUpdatedState(busy)
    val draftCallback by rememberUpdatedState(onDraft)
    DisposableEffect(draftId) {
        onDispose {
            if (
                !latestBusy &&
                    !completed.value &&
                    (latestDraft.title.isNotBlank() || latestDraft.amount.isNotBlank())
            )
                draftCallback(latestDraft)
        }
    }
    var calendar by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope { kotlinx.coroutines.Dispatchers.Main.immediate }
    val focus = LocalFocusManager.current
    val sheetState = rememberSheetController()
    SheetOverlay(
        onDismissRequest = { if (!busy) onDismiss() },
        containerColor = Card,
        sheetState = sheetState,
        dismissEnabled = !busy,
    ) {
        Column(
            Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(17.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (isNew) "A little money moment." else "Edit transaction",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    {
                        scope.launch {
                            focus.clearFocus()
                            finishSheet({ sheetState.hide() }, onDismiss)
                        }
                    },
                    enabled = !busy,
                ) {
                    Icon(Icons.Outlined.Close, "Close")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("expense" to "Expense", "income" to "Income", "transfer" to "Transfer")
                    .forEach { (value, label) ->
                        FilterChip(
                            type == value,
                            { type = value },
                            enabled =
                                !busy &&
                                    (value != "transfer" ||
                                        ledger.accounts.count { !it.archived } > 1),
                            label = { Text(label, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    if (value == "income") Icons.Outlined.SouthWest
                                    else Icons.Outlined.NorthEast,
                                    null,
                                    Modifier.size(16.dp),
                                    tint =
                                        if (value == "income") IncomeGreen
                                        else if (value == "expense") ExpenseRed else Lime,
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
            }
            OutlinedTextField(
                amount,
                { amount = it.take(32) },
                enabled = !busy,
                label = { Text("Amount") },
                placeholder = { Text("0.00") },
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next,
                    ),
                keyboardActions =
                    KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                singleLine = true,
                textStyle =
                    LocalTextStyle.current.copy(fontSize = 27.sp, fontWeight = FontWeight.SemiBold),
                shape = RoundedCornerShape(15.dp),
                modifier = Modifier.fillMaxWidth().testTag("entry-amount"),
            )
            OutlinedTextField(
                title,
                { title = it.take(100) },
                enabled = !busy,
                label = { Text("What was it for?") },
                placeholder = {
                    Text(
                        if (type == "expense") "e.g. Weekly groceries" else "e.g. Monthly salary",
                        fontSize = 12.sp,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions =
                    KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                modifier = Modifier.fillMaxWidth().testTag("entry-title"),
            )
            TextButton({
                calculatorInput = amount
                calculator = true
            }) {
                Text("Amount calculator")
            }
            AccountChoice(ledger, account, { account = it })
            if (type == "transfer")
                AccountChoice(
                    ledger,
                    destination ?: account,
                    { destination = it },
                    "Destination account",
                )
            else
                ChoiceField(
                    "Category",
                    category,
                    categories,
                    { category = it },
                    Modifier.testTag("entry-category"),
                    enabled = !busy,
                )
            if (type == "expense") {
                splitCategories.forEachIndexed { i, c ->
                    ChoiceField(
                        "Split ${i + 1} category",
                        c,
                        (categories + c).distinct(),
                        { v -> splitCategories = ArrayList(splitCategories).also { it[i] = v } },
                    )
                    Field(
                        "Split ${i + 1} amount",
                        splitAmounts[i],
                        { v -> splitAmounts = ArrayList(splitAmounts).also { it[i] = v.take(32) } },
                    )
                    TextButton({
                        splitCategories = ArrayList(splitCategories).also { it.removeAt(i) }
                        splitAmounts = ArrayList(splitAmounts).also { it.removeAt(i) }
                    }) {
                        Text("Remove split")
                    }
                }
                TextButton({
                    if (splitCategories.size < 30) {
                        splitCategories = ArrayList(splitCategories + category)
                        splitAmounts = ArrayList(splitAmounts + "")
                    }
                }) {
                    Text("Add category split")
                }
                if (splitCategories.isNotEmpty())
                    Text(
                        "Split amounts must total the amount above exactly.",
                        fontSize = 11.sp,
                        color = Muted,
                    )
            }
            Row {
                TextButton({ date = today().toString() }) { Text("Today") }
                TextButton({ date = today().minus(1, DateTimeUnit.DAY).toString() }) {
                    Text("Yesterday")
                }
            }
            OutlinedTextField(
                date,
                { date = it.take(10) },
                enabled = !busy,
                label = { Text("Date") },
                placeholder = { Text("YYYY-MM-DD") },
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        enabled = !busy,
                        onClick = {
                            focus.clearFocus()
                            calendar = true
                        },
                    ) {
                        Icon(Icons.Outlined.CalendarMonth, "Choose date")
                    }
                },
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                modifier = Modifier.fillMaxWidth().testTag("entry-date"),
            )
            ToggleRow(
                "Track transaction fee",
                "Keep the fee separate from the amount paid or transferred.",
                feeEnabled,
                { feeEnabled = it },
            )
            if (feeEnabled) {
                ChoiceField("M-Pesa transaction type", feeKind, feeKinds, { feeKind = it })
                if (ledger.currency == "KES")
                    ToggleRow(
                        "Use reference fee",
                        "An offline estimate; your actual charge may differ.",
                        feeAutomatic,
                        { feeAutomatic = it },
                    )
                if (feeAutomatic && quote == null)
                    Text(
                        "No reliable reference quote for this amount/type. Enter the actual fee from your statement. Expired promotions are not applied.",
                        fontSize = 11.sp,
                        color = Orange,
                    )
                Field(
                    "Fee actually charged · enter 0 if free",
                    feeInput,
                    {
                        feeInput = it.take(32)
                        feeAutomatic = false
                        feeConfirmed = true
                    },
                )
                if (quote != null)
                    Text(
                        "Reference fee: ${money(quote.amount, ledger.currency)} · ${money(quote.bandMin, ledger.currency)}–${money(quote.bandMax, ledger.currency)} band",
                        fontSize = 11.sp,
                        color = Muted,
                    )
                val enteredFee = runCatching { zeroOrAmount(feeInput) }.getOrNull()
                if (principal != null && enteredFee != null)
                    Text(
                        "${if (type == "income") "Net received" else "Total account debit"}: ${money(if (type == "income") principal - enteredFee else principal + enteredFee, ledger.currency)}",
                        fontSize = 13.sp,
                    )
                ToggleRow(
                    "Fee confirmed",
                    "I checked this charge against the receipt or statement.",
                    feeConfirmed,
                    { feeConfirmed = it },
                )
                Text(
                    "Choose the correct Paybill tariff. For cash withdrawals, use Transfer into a Cash account so the cash itself is not counted as spending.",
                    fontSize = 11.sp,
                    color = Muted,
                )
            }
            Field("Note · optional", note, { note = it.take(1000) })
            ToggleRow(
                "Cleared",
                "This transaction matches your account statement.",
                cleared,
                { cleared = it },
            )
            if (error != null)
                Text(
                    error!!,
                    color = ErrorColor,
                    fontSize = 12.sp,
                    modifier = Modifier.testTag("form-error"),
                )
            PrimaryButton(
                if (busy) "Saving…" else if (isNew) "Add transaction" else "Save changes",
                {
                    if (busy) return@PrimaryButton
                    error = null
                    busy = true
                    scope.launch {
                        try {
                            try {
                                val next =
                                    Entry(
                                        draftId,
                                        title.trim(),
                                        parseAmount(amount),
                                        type,
                                        category,
                                        date,
                                        account,
                                        if (type == "transfer") destination else null,
                                        if (type == "expense")
                                            splitCategories.mapIndexed { i, c ->
                                                Split(c, parseAmount(splitAmounts[i]))
                                            }
                                        else emptyList(),
                                        cleared,
                                        note,
                                        if (!feeEnabled) null
                                        else if (feeAutomatic && quote != null)
                                            quote.copy(confirmed = feeConfirmed)
                                        else if (
                                            entry?.fee != null &&
                                                entry.amount == parseAmount(amount) &&
                                                entry.fee.kind == feeKind &&
                                                entry.fee.amount == zeroOrAmount(feeInput)
                                        )
                                            entry.fee.copy(confirmed = feeConfirmed)
                                        else
                                            manualFee(
                                                zeroOrAmount(feeInput),
                                                feeKind,
                                                parseAmount(amount),
                                                feeConfirmed,
                                            ),
                                    )
                                validate(ledger.copy(transactions = listOf(next)))
                                onSave(next)
                                completed.value = true
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                error =
                                    if (e is IllegalArgumentException) e.message
                                    else
                                        "Could not save this transaction. Your saved data is unchanged."
                                return@launch
                            }
                            focus.clearFocus()
                            finishSheet({ sheetState.hide() }, onDismiss)
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy,
                modifier = Modifier.testTag("save-entry"),
            )
            if (entry != null && !isNew) {
                Row {
                    TextButton({ onDuplicate(entry) }) { Text("Duplicate") }
                    TextButton({ onTemplate(entry) }) { Text("Save as template") }
                }
                TextButton(onDelete, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.DeleteOutline, null, Modifier.size(17.dp), tint = Orange)
                    Spacer(Modifier.width(8.dp))
                    Text("Delete transaction", color = ExpenseRed, fontSize = 12.sp)
                }
            }
        }
    }
    if (calculator)
        Editor(
            "Amount calculator",
            { calculator = false },
            { amount = amountInput(calculateAmount(calculatorInput)) },
            "Use result",
        ) {
            Field(
                "Expression · + − × ÷ and parentheses",
                calculatorInput,
                { calculatorInput = it.take(128) },
            )
            Text(
                "The final result is rounded to two decimal places.",
                fontSize = 11.sp,
                color = Muted,
            )
        }
    if (calendar)
        BudgetDatePicker(
            date,
            { calendar = false },
            {
                date = it
                calendar = false
            },
        )
}

@Composable
fun BudgetSheet(
    limits: Map<String, Long>,
    month: String,
    onDismiss: () -> Unit,
    onSave: suspend (Map<String, Long>) -> Unit,
) {
    val categories = (LocalLedger.current.activeCategories() + limits.keys).distinct()
    var values by
        rememberSaveable(month) {
            mutableStateOf(ArrayList(categories.map { limits[it]?.let(::amountInput) ?: "" }))
        }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope { kotlinx.coroutines.Dispatchers.Main.immediate }
    val focus = LocalFocusManager.current
    val sheetState = rememberSheetController()
    SheetOverlay(
        onDismissRequest = { if (!busy) onDismiss() },
        containerColor = Card,
        sheetState = sheetState,
        dismissEnabled = !busy,
    ) {
        Column(
            Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Set your intentions.",
                    Modifier.weight(1f),
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(
                    {
                        scope.launch {
                            focus.clearFocus()
                            finishSheet({ sheetState.hide() }, onDismiss)
                        }
                    },
                    enabled = !busy,
                ) {
                    Icon(Icons.Outlined.Close, "Close")
                }
            }
            Text(
                "${if ('|' in month) "Limits for ${periodFromKey(month).label}" else "Monthly limits for ${monthLabel(month)}"}. Leave blank or enter zero to remove a limit.",
                fontSize = 12.sp,
                color = Muted,
            )
            categories.forEachIndexed { i, category ->
                OutlinedTextField(
                    values[i],
                    { next -> values = ArrayList(values).also { it[i] = next.take(32) } },
                    enabled = !busy,
                    label = { Text(category) },
                    singleLine = true,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction =
                                if (i == categories.lastIndex) ImeAction.Done else ImeAction.Next,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onNext = { focus.moveFocus(FocusDirection.Down) },
                            onDone = { focus.clearFocus() },
                        ),
                    shape = RoundedCornerShape(13.dp),
                    modifier = Modifier.fillMaxWidth().testTag("budget-$category"),
                )
            }
            if (error != null)
                Text(
                    error!!,
                    fontSize = 12.sp,
                    color = ErrorColor,
                    modifier = Modifier.testTag("budget-error"),
                )
            PrimaryButton(
                if (busy) "Saving…" else "Save budgets",
                {
                    if (busy) return@PrimaryButton
                    error = null
                    busy = true
                    scope.launch {
                        try {
                            try {
                                val next =
                                    categories
                                        .mapIndexedNotNull { i, c ->
                                            val text = values[i].trim().replace(',', '.')
                                            if (
                                                text.isBlank() ||
                                                    Regex("(?:0+(?:\\.0{0,2})?|\\.0{1,2})")
                                                        .matches(text)
                                            )
                                                null
                                            else
                                                c to
                                                    try {
                                                        parseAmount(text)
                                                    } catch (e: IllegalArgumentException) {
                                                        throw IllegalArgumentException(
                                                            "$c: ${e.message}"
                                                        )
                                                    }
                                        }
                                        .toMap()
                                onSave(next)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                error =
                                    if (e is IllegalArgumentException) e.message
                                    else "Could not save budgets. Your saved data is unchanged."
                                return@launch
                            }
                            focus.clearFocus()
                            finishSheet({ sheetState.hide() }, onDismiss)
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy,
                modifier = Modifier.testTag("save-budgets"),
            )
        }
    }
}
