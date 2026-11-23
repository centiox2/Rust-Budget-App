@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.centiox.verdant

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
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
import kotlin.time.Instant
import kotlinx.coroutines.launch
import kotlinx.datetime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrySheet(
    entry: Entry?,
    initialType: String,
    month: String,
    onDismiss: () -> Unit,
    onSave: suspend (Entry) -> Unit,
    onDelete: () -> Unit,
) {
    var type by rememberSaveable(entry?.id) { mutableStateOf(entry?.type ?: initialType) }
    var title by rememberSaveable(entry?.id) { mutableStateOf(entry?.title ?: "") }
    var amount by
        rememberSaveable(entry?.id) { mutableStateOf(entry?.let { amountInput(it.amount) } ?: "") }
    var category by rememberSaveable(entry?.id) { mutableStateOf(entry?.category ?: "Other") }
    var date by
        rememberSaveable(entry?.id) {
            mutableStateOf(
                entry?.date ?: if (month == currentMonth()) today().toString() else "$month-01"
            )
        }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var calendar by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { if (!busy) onDismiss() },
        containerColor = Card,
        sheetState = sheetState,
    ) {
        Column(
            Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(17.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (entry == null) "A little money moment." else "Edit transaction",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    {
                        scope.launch {
                            focus.clearFocus()
                            sheetState.hide()
                            onDismiss()
                        }
                    },
                    enabled = !busy,
                ) {
                    Icon(Icons.Outlined.Close, "Close")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("expense" to "Expense", "income" to "Income").forEach { (value, label) ->
                    FilterChip(
                        type == value,
                        { type = value },
                        label = { Text(label) },
                        leadingIcon = {
                            Icon(
                                if (value == "income") Icons.Outlined.SouthWest
                                else Icons.Outlined.NorthEast,
                                null,
                                Modifier.size(16.dp),
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            OutlinedTextField(
                amount,
                { amount = it.take(32) },
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
                textStyle = MaterialTheme.typography.headlineMedium,
                shape = RoundedCornerShape(15.dp),
                modifier = Modifier.fillMaxWidth().testTag("entry-amount"),
            )
            OutlinedTextField(
                title,
                { title = it.take(100) },
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
            ChoiceField(
                "Category",
                category,
                categories,
                { category = it },
                Modifier.testTag("entry-category"),
            )
            OutlinedTextField(
                date,
                { date = it.take(10) },
                label = { Text("Date") },
                placeholder = { Text("YYYY-MM-DD") },
                singleLine = true,
                trailingIcon = {
                    IconButton({ calendar = true }) {
                        Icon(Icons.Outlined.CalendarMonth, "Choose date")
                    }
                },
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                modifier = Modifier.fillMaxWidth().testTag("entry-date"),
            )
            if (error != null)
                Text(
                    error!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.testTag("form-error"),
                )
            PrimaryButton(
                if (busy) "Saving…" else if (entry == null) "Add transaction" else "Save changes",
                {
                    error = null
                    scope.launch {
                        busy = true
                        try {
                            val next =
                                Entry(
                                    entry?.id ?: newId(),
                                    title.trim(),
                                    parseAmount(amount),
                                    type,
                                    category,
                                    date,
                                )
                            validate(BudgetData(transactions = listOf(next)))
                            onSave(next)
                            focus.clearFocus()
                            sheetState.hide()
                            onDismiss()
                        } catch (e: Exception) {
                            error =
                                if (e is IllegalArgumentException) e.message
                                else
                                    "Could not save this transaction. Your saved data is unchanged."
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy,
                modifier = Modifier.testTag("save-entry"),
            )
            if (entry != null)
                TextButton(onDelete, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.DeleteOutline, null, Modifier.size(17.dp), tint = Orange)
                    Spacer(Modifier.width(8.dp))
                    Text("Delete transaction", color = Orange, fontSize = 12.sp)
                }
        }
    }
    if (calendar) {
        val state =
            rememberDatePickerState(
                initialSelectedDateMillis =
                    runCatching {
                            LocalDate.parse(date)
                                .takeIf { it.year in 1900..2100 }
                                ?.atStartOfDayIn(TimeZone.UTC)
                                ?.toEpochMilliseconds()
                        }
                        .getOrNull()
            )
        DatePickerDialog(
            onDismissRequest = { calendar = false },
            confirmButton = {
                TextButton({
                    state.selectedDateMillis?.let {
                        date =
                            Instant.fromEpochMilliseconds(it)
                                .toLocalDateTime(TimeZone.UTC)
                                .date
                                .toString()
                    }
                    calendar = false
                }) {
                    Text("Done")
                }
            },
            dismissButton = { TextButton({ calendar = false }) { Text("Cancel") } },
        ) {
            DatePicker(state)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSheet(
    limits: Map<String, Long>,
    month: String,
    onDismiss: () -> Unit,
    onSave: suspend (Map<String, Long>) -> Unit,
) {
    var values by
        rememberSaveable(month) {
            mutableStateOf(ArrayList(categories.map { limits[it]?.let(::amountInput) ?: "" }))
        }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { if (!busy) onDismiss() },
        containerColor = Card,
        sheetState = sheetState,
    ) {
        Column(
            Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
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
                            sheetState.hide()
                            onDismiss()
                        }
                    },
                    enabled = !busy,
                ) {
                    Icon(Icons.Outlined.Close, "Close")
                }
            }
            Text(
                "Monthly limits for ${monthLabel(month)}. Leave blank or enter zero to remove a limit.",
                fontSize = 12.sp,
                color = Muted,
            )
            categories.forEachIndexed { i, category ->
                OutlinedTextField(
                    values[i],
                    { next -> values = ArrayList(values).also { it[i] = next.take(32) } },
                    label = { Text(category) },
                    singleLine = true,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next,
                        ),
                    keyboardActions =
                        KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                    shape = RoundedCornerShape(13.dp),
                    modifier = Modifier.fillMaxWidth().testTag("budget-$category"),
                )
            }
            if (error != null)
                Text(error!!, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            PrimaryButton(
                if (busy) "Saving…" else "Save budgets",
                {
                    error = null
                    scope.launch {
                        busy = true
                        try {
                            val next =
                                categories
                                    .mapIndexedNotNull { i, c ->
                                        val text = values[i].trim()
                                        if (text.isBlank() || Regex("0+(\\.0{1,2})?").matches(text))
                                            null
                                        else c to parseAmount(text)
                                    }
                                    .toMap()
                            onSave(next)
                            focus.clearFocus()
                            sheetState.hide()
                            onDismiss()
                        } catch (e: Exception) {
                            error =
                                if (e is IllegalArgumentException) e.message
                                else "Could not save budgets. Your saved data is unchanged."
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
