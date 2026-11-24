package com.centiox.verdant

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import kotlinx.datetime.*

class PlatformServices(
    val supportsLock: Boolean = false,
    val authenticate: ((Boolean) -> Unit) -> Unit = { it(false) },
    val update: (BudgetData) -> Unit = {},
    val haptic: () -> Unit = {},
    val pinWidget: (() -> Unit)? = null,
    val saveDraft: ((EntryDraft?) -> Unit)? = null,
    val openNotificationSettings: (() -> Unit)? = null,
)

@Composable
fun ToolRow(
    title: String,
    description: String,
    action: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        Modifier.fillMaxWidth()
            .then(if (action != null) Modifier.clickable(onClick = action) else Modifier)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text(description, fontSize = 12.sp, color = Muted)
        content()
    }
    HorizontalDivider(thickness = .5.dp)
}

@Composable
fun Field(label: String, value: String, change: (String) -> Unit, tag: String = "") {
    OutlinedTextField(
        value,
        { change(it.take(1000)) },
        Modifier.fillMaxWidth().testTag(tag.ifEmpty { "field-$label" }),
        label = { Text(label) },
        singleLine = true,
    )
}

@Composable
fun ToggleRow(label: String, detail: String, value: Boolean, change: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 58.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp)
            Text(detail, fontSize = 11.sp, color = Muted)
        }
        Switch(value, change, modifier = Modifier.testTag("toggle-$label"))
    }
}

@Composable
fun Editor(
    title: String,
    onDismiss: () -> Unit,
    onSave: suspend () -> Unit,
    saveLabel: String = "Save",
    content: @Composable ColumnScope.() -> Unit,
) {
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope { kotlinx.coroutines.Dispatchers.Main.immediate }
    val sheetState = rememberSheetController()
    SheetOverlay({ if (!busy) onDismiss() }, Card, sheetState, !busy) {
        Column(
            Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                IconButton({
                    if (!busy) scope.launch { finishSheet({ sheetState.hide() }, onDismiss) }
                }) {
                    Icon(Icons.Outlined.Close, "Close editor")
                }
            }
            content()
            if (error != null)
                Text(
                    error!!,
                    color = ErrorColor,
                    fontSize = 12.sp,
                    modifier = Modifier.testTag("tool-error"),
                )
            PrimaryButton(
                if (busy) "Saving…" else saveLabel,
                {
                    if (!busy) {
                        busy = true
                        scope.launch {
                            try {
                                onSave()
                                finishSheet({ sheetState.hide() }, onDismiss)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                error = e.message ?: "Could not save."
                            } finally {
                                busy = false
                            }
                        }
                    }
                },
                enabled = !busy,
            )
        }
    }
}

@Composable
fun PlanningScreen(
    initial: String,
    data: BudgetData,
    update: suspend ((BudgetData) -> BudgetData) -> Unit,
    onClose: () -> Unit,
    onNew: (Entry) -> Unit,
    onExport: (String) -> Unit,
    onImportCsv: () -> Unit,
    onProtectedExport: () -> Unit,
    onRestore: () -> Unit,
    platform: PlatformServices,
) {
    var tab by rememberSaveable(initial) { mutableStateOf(initial) }
    var editor by rememberSaveable { mutableStateOf<String?>(null) }
    var entity by rememberSaveable { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope { kotlinx.coroutines.Dispatchers.Main.immediate }
    fun act(operation: suspend () -> Unit) {
        scope.launch {
            try {
                operation()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = e.message ?: "Could not complete that action."
            }
        }
    }
    fun edit(kind: String, id: String? = null) {
        entity = id
        editor = kind
    }
    val contentScroll = rememberSaveable(tab, saver = ScrollState.Saver) { ScrollState(0) }
    Column(
        Modifier.fillMaxSize()
            .background(HomeBackground)
            .pointerInput(Unit) { detectTapGestures {} }
            .testTag("planning-screen")
    ) {
        Box(Modifier.fillMaxWidth()) {
            LiquidBackground(
                data.backgroundColor,
                data.animateBackground,
                Modifier.matchParentSize(),
            )
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundAction(Icons.Outlined.ArrowBack, "Close tools", onClose)
                Text(
                    tab,
                    Modifier.weight(1f).padding(start = 16.dp),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Row(
            Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("Alerts", "Accounts", "Categories", "Payments", "Goals", "Fees", "Tools")
                .forEach { label ->
                    val bring = remember { BringIntoViewRequester() }
                    LaunchedEffect(tab == label) {
                        if (tab == label) {
                            withFrameNanos {}
                            bring.bringIntoView()
                        }
                    }
                    FilterChip(
                        tab == label,
                        { tab = label },
                        { Text(label, fontSize = 12.sp) },
                        Modifier.bringIntoViewRequester(bring),
                    )
                }
        }
        Column(
            Modifier.weight(1f)
                .verticalScroll(contentScroll)
                .padding(horizontal = 21.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (message != null) {
                Text(message!!, color = ErrorColor, fontSize = 12.sp)
                TextButton({ message = null }) { Text("Dismiss") }
            }
            when (tab) {
                "Alerts" -> {
                    Text("Your attention, where it matters.", fontSize = 18.sp)
                    if (data.alerts().isEmpty())
                        EmptyState("All clear", "There are no budget or payment alerts right now.")
                    data.alerts().forEach { (title, detail) -> ToolRow(title, detail) }
                    TextButton({ tab = "Payments" }) { Text("View scheduled payments") }
                }
                "Accounts" -> {
                    PrimaryButton("Add account", { edit("account") })
                    TextButton({
                        onNew(
                            Entry(
                                title = "Transfer",
                                amount = 0,
                                type = "transfer",
                                category = "Other",
                                date = today().toString(),
                            )
                        )
                    }) {
                        Text("Transfer between accounts")
                    }
                    data.accounts.forEach { account ->
                        ToolRow(
                            account.name + if (account.archived) " · archived" else "",
                            "${data.transactions.count { it.accountId == account.id || it.targetAccountId == account.id }} transactions",
                        ) {
                            AmountText(
                                displayMoney(data.accountBalance(account.id), data.currency),
                                fontSize = 23.sp,
                            )
                            Row(Modifier.horizontalScroll(rememberScrollState())) {
                                TextButton({ edit("account", account.id) }) { Text("Edit") }
                                TextButton({ edit("reconcile", account.id) }) { Text("Reconcile") }
                                TextButton({
                                    act {
                                        update { d ->
                                            d.copy(
                                                accounts =
                                                    d.accounts.map {
                                                        if (it.id == account.id)
                                                            it.copy(archived = !it.archived)
                                                        else it
                                                    }
                                            )
                                        }
                                    }
                                }) {
                                    Text(if (account.archived) "Restore" else "Archive")
                                }
                            }
                        }
                    }
                }
                "Categories" -> {
                    PrimaryButton("Add category", { edit("category") })
                    data.allCategories().forEach { name ->
                        val setting = data.categorySettings.find { it.name == name }
                        ToolRow(
                            name + if (setting?.archived == true) " · archived" else "",
                            "Used in ${data.transactions.count { e -> e.category == name || e.splits.any { it.category == name } }} transactions",
                        ) {
                            Row(Modifier.horizontalScroll(rememberScrollState())) {
                                TextButton({ edit("category", name) }) { Text("Edit") }
                                TextButton({
                                    act {
                                        update { d ->
                                            val all =
                                                d.allCategories()
                                                    .map { c ->
                                                        d.categorySettings.find { it.name == c }
                                                            ?: CategorySetting(
                                                                c,
                                                                if (c in categories) c else "Other",
                                                            )
                                                    }
                                                    .toMutableList()
                                            val index = all.indexOfFirst { it.name == name }
                                            if (index > 0) {
                                                val item = all.removeAt(index)
                                                all.add(index - 1, item)
                                            }
                                            d.copy(categorySettings = all)
                                        }
                                    }
                                }) {
                                    Text("Move up")
                                }
                                TextButton({
                                    act {
                                        update { d ->
                                            val old =
                                                d.categorySettings.find { it.name == name }
                                                    ?: CategorySetting(
                                                        name,
                                                        if (name in categories) name else "Other",
                                                    )
                                            d.copy(
                                                categorySettings =
                                                    d.categorySettings.filterNot {
                                                        it.name == name
                                                    } + old.copy(archived = !old.archived)
                                            )
                                        }
                                    }
                                }) {
                                    Text(if (setting?.archived == true) "Restore" else "Archive")
                                }
                            }
                        }
                    }
                }
                "Payments" -> {
                    PrimaryButton("Schedule payment or income", { edit("recurring") })
                    Text("Upcoming calendar", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Scheduled items stay separate from your balance until you record them.",
                        fontSize = 12.sp,
                        color = Muted,
                    )
                    data.recurring
                        .sortedBy { it.nextDate }
                        .forEach { payment ->
                            ToolRow(
                                payment.title + if (!payment.active) " · paused" else "",
                                "${friendlyDate(payment.nextDate)} · ${payment.frequency} · ${data.accountName(payment.accountId)}",
                            ) {
                                AmountText(
                                    displayMoney(payment.amount, data.currency),
                                    color =
                                        if (payment.type == "income") IncomeGreen else ExpenseRed,
                                )
                                Row(Modifier.horizontalScroll(rememberScrollState())) {
                                    if (payment.active && payment.nextDate <= today().toString())
                                        TextButton({
                                            act { update { it.payScheduled(payment.id) } }
                                        }) {
                                            Text(
                                                if (payment.type == "income") "Record received"
                                                else "Mark paid"
                                            )
                                        }
                                    TextButton({ edit("recurring", payment.id) }) { Text("Edit") }
                                    TextButton({
                                        act {
                                            update { d ->
                                                d.copy(
                                                    recurring =
                                                        d.recurring.map {
                                                            if (it.id == payment.id)
                                                                it.copy(active = !it.active)
                                                            else it
                                                        }
                                                )
                                            }
                                        }
                                    }) {
                                        Text(if (payment.active) "Pause" else "Resume")
                                    }
                                }
                            }
                        }
                    if (data.recurring.isEmpty())
                        EmptyState(
                            "Plan the predictable",
                            "Add your rent, subscriptions, salary, or another repeating payment.",
                        )
                }
                "Goals" -> {
                    PrimaryButton("Create savings goal", { edit("goal") })
                    Text(
                        "Goals track money you set aside. Contributions do not create expenses or move money between accounts.",
                        fontSize = 12.sp,
                        color = Muted,
                    )
                    data.goals.forEach { goal ->
                        val saved = goal.contributions.sumOf { it.amount }
                        ToolRow(
                            goal.name,
                            goal.dueDate?.let { "Target: ${friendlyDate(it)}" } ?: "No deadline",
                        ) {
                            AmountText(
                                "${displayMoney(saved, data.currency)} / ${displayMoney(goal.target, data.currency)}",
                                Modifier.fillMaxWidth(),
                            )
                            Progress(saved.toFloat() / goal.target)
                            goal.dueDate?.let { due ->
                                val remaining = (goal.target - saved).coerceAtLeast(0)
                                val months =
                                    ((daysBetween(today().toString(), due).coerceAtLeast(1) + 29) /
                                            30)
                                        .coerceAtLeast(1)
                                Text(
                                    if (due < today().toString() && remaining > 0)
                                        "Deadline passed · ${displayMoney(remaining, data.currency)} remaining"
                                    else
                                        "${displayMoney((remaining + months - 1) / months, data.currency)} per month to reach the target",
                                    fontSize = 11.sp,
                                    color = Muted,
                                )
                            }
                            Row {
                                TextButton({ edit("contribute", goal.id) }) {
                                    Text("Add contribution")
                                }
                                TextButton({ edit("goal", goal.id) }) { Text("Edit goal") }
                            }
                            goal.contributions.takeLast(5).reversed().forEach { c ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "${friendlyDate(c.date)} · ${displayMoney(c.amount, data.currency)}",
                                        Modifier.weight(1f),
                                        fontSize = 11.sp,
                                    )
                                    TextButton({
                                        act {
                                            update { d ->
                                                d.copy(
                                                    goals =
                                                        d.goals.map {
                                                            if (it.id == goal.id)
                                                                it.copy(
                                                                    contributions =
                                                                        it.contributions
                                                                            .filterNot { old ->
                                                                                old.id == c.id
                                                                            }
                                                                )
                                                            else it
                                                        }
                                                )
                                            }
                                        }
                                    }) {
                                        Text("Remove", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                "Fees" -> FeeReport(data, onExport)
                "Tools" -> {
                    ToolRow(
                        "Budget periods",
                        "Choose monthly, weekly, or payday cycles and optional rollover.",
                        { edit("period") },
                    )
                    ToolRow(
                        "CSV export",
                        "Export all records for a spreadsheet. Full backups also preserve settings, goals, and schedules.",
                        { onExport(exportCsv(data)) },
                    )
                    ToolRow(
                        "CSV import",
                        "Map columns, preview transactions, and review duplicates before importing.",
                        onImportCsv,
                    )
                    ToolRow(
                        "Protected backup",
                        "Create a portable, password-protected copy of the complete ledger.",
                        onProtectedExport,
                    )
                    ToolRow(
                        "Restore a backup",
                        "Preview and confirm before replacing your ledger.",
                        onRestore,
                    )
                    ToolRow(
                        "Preferences & privacy",
                        "Balance privacy, motion, haptics, reminders, and app lock.",
                        { edit("preferences") },
                    )
                    platform.pinWidget?.let {
                        ToolRow(
                            "Quick-entry widget",
                            "Add an expense shortcut to your Android home screen.",
                            it,
                        )
                    }
                    Text("Templates", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Save a transaction as a template from its edit form.",
                        fontSize = 12.sp,
                        color = Muted,
                    )
                    data.templates.forEach { template ->
                        ToolRow(
                            template.name,
                            "${displayMoney(template.entry.amount, data.currency)} · ${template.entry.category}",
                        ) {
                            Row {
                                TextButton({
                                    onNew(
                                        template.entry.copy(
                                            id = newId(),
                                            date = today().toString(),
                                            cleared = false,
                                        )
                                    )
                                }) {
                                    Text("Use template")
                                }
                                TextButton({
                                    act {
                                        update { d ->
                                            d.copy(
                                                templates =
                                                    d.templates.filterNot { it.id == template.id }
                                            )
                                        }
                                    }
                                }) {
                                    Text("Remove")
                                }
                            }
                        }
                    }
                    if (data.deleted.isNotEmpty()) {
                        Text("Recently deleted", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Available for 30 days, up to the most recent 200 entries.",
                            fontSize = 11.sp,
                            color = Muted,
                        )
                        data.deleted.asReversed().take(30).forEach { deleted ->
                            ToolRow(
                                deleted.entry.title,
                                "Deleted ${friendlyDate(deleted.deletedOn)}",
                            ) {
                                TextButton({
                                    act { update { it.undoDelete(setOf(deleted.entry.id)) } }
                                }) {
                                    Text("Restore transaction")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    when (editor) {
        "account",
        "reconcile" -> AccountEditor(data, entity, editor == "reconcile", { editor = null }, update)
        "category" -> CategoryEditor(data, entity, { editor = null }, update)
        "recurring" -> RecurringEditor(data, entity, { editor = null }, update)
        "goal",
        "contribute" -> GoalEditor(data, entity, editor == "contribute", { editor = null }, update)
        "period" -> PeriodEditor(data, { editor = null }, update)
        "preferences" -> PreferencesEditor(data, platform, { editor = null }, update)
    }
}

private fun signedAmount(text: String): Long {
    val s = text.trim().replace('−', '-')
    if (s in listOf("", "0", "0.00")) return 0
    return parseAmount(s.removePrefix("-")) * if (s.startsWith('-')) -1 else 1
}

@Composable
private fun AccountEditor(
    data: BudgetData,
    id: String?,
    reconcile: Boolean,
    dismiss: () -> Unit,
    update: suspend ((BudgetData) -> BudgetData) -> Unit,
) {
    val old = data.accounts.find { it.id == id }
    val draftId = rememberSaveable { old?.id ?: newId() }
    var kind by rememberSaveable { mutableStateOf(old?.kind ?: "General") }
    var name by rememberSaveable { mutableStateOf(old?.name.orEmpty()) }
    var amount by rememberSaveable {
        mutableStateOf(
            if (reconcile) ""
            else
                old?.openingBalance?.let { if (it < 0) "-${amountInput(-it)}" else amountInput(it) }
                    ?: "0.00"
        )
    }
    Editor(
        if (reconcile) "Reconcile account" else if (old == null) "New account" else "Edit account",
        dismiss,
        {
            val balance = signedAmount(amount)
            update { d ->
                if (reconcile)
                    d.copy(
                        accounts =
                            d.accounts.map {
                                if (it.id == id)
                                    it.copy(
                                        openingBalance =
                                            it.openingBalance + balance - d.accountBalance(it.id)
                                    )
                                else it
                            }
                    )
                else {
                    require(name.trim().isNotEmpty()) { "Name the account." }
                    require(
                        d.accounts.none { it.id != draftId && it.name.equals(name.trim(), true) }
                    ) {
                        "That account name is already used."
                    }
                    val account =
                        Account(draftId, name.trim(), balance, old?.archived ?: false, kind)
                    d.copy(
                        accounts =
                            if (old == null) d.accounts + account
                            else d.accounts.map { if (it.id == id) account else it }
                    )
                }
            }
        },
    ) {
        if (!reconcile) {
            Field("Account name", name, { name = it.take(50) })
            ChoiceField(
                "Account type",
                kind,
                listOf("General", "Cash", "M-Pesa", "Bank", "Savings"),
                { kind = it },
            )
        }
        Field(
            if (reconcile) "Actual account balance" else "Opening balance",
            amount,
            { amount = it.take(32) },
        )
        Text(
            if (reconcile)
                "This adjusts the opening balance to match your actual balance without adding income or expenses."
            else "Use separate accounts for Cash, M-Pesa, Bank, or Savings.",
            fontSize = 12.sp,
            color = Muted,
        )
    }
}

@Composable
private fun CategoryEditor(
    data: BudgetData,
    name: String?,
    dismiss: () -> Unit,
    update: suspend ((BudgetData) -> BudgetData) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(name.orEmpty()) }
    var icon by rememberSaveable {
        mutableStateOf(name?.let(data::categoryIconKey)?.takeIf { it in categories } ?: "Other")
    }
    Editor(
        if (name == null) "New category" else "Edit category",
        dismiss,
        {
            update { d ->
                val next =
                    if (name != null) d.renameCategory(name, title)
                    else {
                        require(title.trim() !in d.allCategories()) { "That category exists." }
                        d.copy(
                            categorySettings =
                                d.categorySettings + CategorySetting(title.trim(), icon)
                        )
                    }
                next.copy(
                    categorySettings =
                        next.categorySettings.map {
                            if (it.name == title.trim()) it.copy(icon = icon) else it
                        }
                )
            }
        },
    ) {
        Field("Category name", title, { title = it.take(40) })
        ChoiceField("Icon", icon, categories, { icon = it })
        Icon(categoryIcon(icon), null, Modifier.size(28.dp), Lime)
        Text(
            "Renaming updates existing transactions, splits, budgets, and schedules. Archiving preserves history.",
            fontSize = 11.sp,
            color = Muted,
        )
    }
}

@Composable
private fun RecurringEditor(
    data: BudgetData,
    id: String?,
    dismiss: () -> Unit,
    update: suspend ((BudgetData) -> BudgetData) -> Unit,
) {
    val old = data.recurring.find { it.id == id }
    val draftId = rememberSaveable { old?.id ?: newId() }
    var title by rememberSaveable { mutableStateOf(old?.title.orEmpty()) }
    var amount by rememberSaveable { mutableStateOf(old?.amount?.let(::amountInput).orEmpty()) }
    var type by rememberSaveable { mutableStateOf(old?.type ?: "expense") }
    var category by rememberSaveable { mutableStateOf(old?.category ?: "Other") }
    var account by rememberSaveable { mutableStateOf(old?.accountId ?: data.lastAccountId) }
    var date by rememberSaveable { mutableStateOf(old?.nextDate ?: today().toString()) }
    var frequency by rememberSaveable { mutableStateOf(old?.frequency ?: "monthly") }
    var feeEnabled by rememberSaveable { mutableStateOf(old?.fee != null) }
    var feeKind by rememberSaveable { mutableStateOf(old?.fee?.kind ?: "Send to M-Pesa") }
    var feeInput by rememberSaveable {
        mutableStateOf(old?.fee?.amount?.let(::amountInput).orEmpty())
    }
    Editor(
        "Scheduled payment",
        dismiss,
        {
            val r =
                Recurring(
                    draftId,
                    title.trim(),
                    parseAmount(amount),
                    type,
                    category,
                    account,
                    date,
                    frequency,
                    date.takeLast(2).toIntOrNull() ?: 0,
                    old?.active ?: true,
                    if (feeEnabled)
                        manualFee(zeroOrAmount(feeInput), feeKind, parseAmount(amount), false)
                    else null,
                )
            update { d ->
                d.copy(
                    recurring =
                        if (old == null) d.recurring + r
                        else d.recurring.map { if (it.id == id) r else it }
                )
            }
        },
    ) {
        Field("Title", title, { title = it.take(100) })
        Field("Amount", amount, { amount = it.take(32) })
        ChoiceField("Type", type, listOf("expense", "income"), { type = it })
        ChoiceField(
            "Category",
            category,
            (data.activeCategories() + category).distinct(),
            { category = it },
        )
        AccountChoice(data, account, { account = it })
        Field("Next due date · YYYY-MM-DD", date, { date = it.take(10) })
        ChoiceField("Repeat", frequency, listOf("weekly", "monthly", "yearly"), { frequency = it })
        ToggleRow(
            "Include transaction fee",
            "Expected fees are recorded as estimates until checked.",
            feeEnabled,
            { feeEnabled = it },
        )
        if (feeEnabled) {
            ChoiceField("M-Pesa transaction type", feeKind, feeKinds, { feeKind = it })
            if (data.currency == "KES")
                TextButton({
                    val quote =
                        runCatching { quoteMpesaFee(feeKind, parseAmount(amount)) }.getOrNull()
                    if (quote != null) feeInput = amountInput(quote.amount)
                }) {
                    Text("Use reference estimate")
                }
            Field("Expected fee", feeInput, { feeInput = it.take(32) })
            Text(
                "Check the actual fee after marking this payment paid. No reference quote is available for some types and amounts.",
                fontSize = 11.sp,
                color = Muted,
            )
        }
    }
}

@Composable
fun AccountChoice(
    data: BudgetData,
    selected: String,
    onChange: (String) -> Unit,
    label: String = "Account",
) {
    val accounts = data.accounts.filter { !it.archived || it.id == selected }
    ChoiceField(
        label,
        data.accountName(selected),
        accounts.map { it.name },
        { value -> onChange(accounts.first { it.name == value }.id) },
    )
}

@Composable
private fun GoalEditor(
    data: BudgetData,
    id: String?,
    contribution: Boolean,
    dismiss: () -> Unit,
    update: suspend ((BudgetData) -> BudgetData) -> Unit,
) {
    val old = data.goals.find { it.id == id }
    val draftId = rememberSaveable { old?.id ?: newId() }
    var name by rememberSaveable { mutableStateOf(old?.name.orEmpty()) }
    var amount by rememberSaveable {
        mutableStateOf(if (contribution) "" else old?.target?.let(::amountInput).orEmpty())
    }
    var date by rememberSaveable {
        mutableStateOf(if (contribution) today().toString() else old?.dueDate.orEmpty())
    }
    Editor(
        if (contribution) "Add contribution" else "Savings goal",
        dismiss,
        {
            val value = parseAmount(amount)
            update { d ->
                if (contribution)
                    d.copy(
                        goals =
                            d.goals.map {
                                if (it.id == id)
                                    it.copy(
                                        contributions =
                                            it.contributions +
                                                Contribution(amount = value, date = date)
                                    )
                                else it
                            }
                    )
                else {
                    val goal =
                        SavingsGoal(
                            draftId,
                            name.trim(),
                            value,
                            date.ifBlank { null },
                            old?.contributions.orEmpty(),
                        )
                    d.copy(
                        goals =
                            if (old == null) d.goals + goal
                            else d.goals.map { if (it.id == id) goal else it }
                    )
                }
            }
        },
    ) {
        if (!contribution) Field("Goal name", name, { name = it.take(60) })
        Field(
            if (contribution) "Contribution amount" else "Target amount",
            amount,
            { amount = it.take(32) },
        )
        Field(
            if (contribution) "Date · YYYY-MM-DD" else "Target date · optional YYYY-MM-DD",
            date,
            { date = it.take(10) },
        )
    }
}

@Composable
private fun PeriodEditor(
    data: BudgetData,
    dismiss: () -> Unit,
    update: suspend ((BudgetData) -> BudgetData) -> Unit,
) {
    var cycle by rememberSaveable { mutableStateOf(data.budgetCycle) }
    var day by rememberSaveable { mutableStateOf(data.payday.toString()) }
    var rollover by rememberSaveable { mutableStateOf(data.rollover) }
    Editor(
        "Budget periods",
        dismiss,
        {
            val payday = day.toIntOrNull() ?: error("Enter a day between 1 and 28.")
            update { it.copy(budgetCycle = cycle, payday = payday, rollover = rollover) }
        },
    ) {
        ChoiceField("Cycle", cycle, listOf("monthly", "weekly", "payday"), { cycle = it })
        if (cycle == "payday") Field("Payday · day 1–28", day, { day = it.take(2) })
        ToggleRow(
            "Carry remaining money forward",
            "Unused limits roll into the next planned period of the same cycle.",
            rollover,
            { rollover = it },
        )
        Text(
            "Each cycle has its own limits. Existing monthly budgets remain stored when you switch cycles.",
            fontSize = 11.sp,
            color = Muted,
        )
    }
}

@Composable
private fun PreferencesEditor(
    data: BudgetData,
    platform: PlatformServices,
    dismiss: () -> Unit,
    update: suspend ((BudgetData) -> BudgetData) -> Unit,
) {
    var hide by rememberSaveable { mutableStateOf(data.hideBalances) }
    var reduced by rememberSaveable { mutableStateOf(data.reducedMotion) }
    var haptics by rememberSaveable { mutableStateOf(data.haptics) }
    var reminders by rememberSaveable { mutableStateOf(data.reminders) }
    var backup by rememberSaveable { mutableStateOf(data.backupReminder) }
    var lock by rememberSaveable { mutableStateOf(data.appLock) }
    var mpesa by rememberSaveable { mutableStateOf(data.mpesaFeesDefault) }
    Editor(
        "Preferences & privacy",
        dismiss,
        {
            if (lock && !data.appLock) {
                val accepted =
                    suspendCancellableCoroutine<Boolean> { c ->
                        platform.authenticate { if (c.isActive) c.resumeWith(Result.success(it)) }
                    }
                require(accepted) {
                    "Device authentication was not completed. App lock is unchanged."
                }
            }
            update {
                it.copy(
                    hideBalances = hide,
                    reducedMotion = reduced,
                    haptics = haptics,
                    reminders = reminders,
                    backupReminder = backup,
                    appLock = lock,
                    mpesaFeesDefault = mpesa,
                )
            }
        },
    ) {
        ToggleRow(
            "Default M-Pesa fee tracking",
            "Suggest fees for KES entries in M-Pesa or untyped accounts. Actual charges can be confirmed or overridden.",
            mpesa,
            { mpesa = it },
        )
        ToggleRow(
            "Hide balances",
            "Hide monetary values in summaries and transaction lists.",
            hide,
            { hide = it },
        )
        ToggleRow(
            "Reduce motion",
            "Use still headers and immediate page/control changes.",
            reduced,
            { reduced = it },
        )
        ToggleRow(
            "Haptic feedback",
            "Light feedback on supported devices.",
            haptics,
            { haptics = it },
        )
        ToggleRow(
            "Payment & budget reminders",
            "Optional Android notifications. Alerts are always available in the app.",
            reminders,
            { reminders = it },
        )
        if (platform.openNotificationSettings != null) {
            Text(
                "Android must also allow notifications. Delivery follows Android battery and background scheduling rules.",
                fontSize = 11.sp,
                color = Muted,
            )
            TextButton(platform.openNotificationSettings) { Text("Android notification settings") }
        }
        ToggleRow(
            "Backup reminder",
            "Remind me to make a backup after seven days.",
            backup,
            { backup = it },
        )
        if (platform.supportsLock)
            ToggleRow(
                "App lock",
                "Use your device biometric or screen-lock credential. Re-lock after leaving the app.",
                lock,
                { lock = it },
            )
    }
}
