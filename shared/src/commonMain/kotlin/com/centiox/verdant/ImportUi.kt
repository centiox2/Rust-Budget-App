package com.centiox.verdant

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*

@Composable
fun ProtectedBackupEditor(
    exporting: Boolean,
    raw: String?,
    data: BudgetData,
    dismiss: () -> Unit,
    complete: suspend (String) -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var repeated by remember { mutableStateOf("") }
    Editor(
        if (exporting) "Protected backup" else "Unlock backup",
        dismiss,
        {
            if (exporting) require(password == repeated) { "The passwords do not match." }
            val result =
                withContext(Dispatchers.Default) {
                    if (exporting) protectBackup(encodeBackup(data), password)
                    else unprotectBackup(requireNotNull(raw), password).also(::decodeBackup)
                }
            complete(result)
        },
        if (exporting) "Choose backup destination" else "Preview backup",
    ) {
        OutlinedTextField(
            password,
            { password = it.take(256) },
            Modifier.fillMaxWidth(),
            label = { Text("Backup password") },
            singleLine = true,
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            keyboardOptions =
                androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                    autoCorrectEnabled = false,
                ),
        )
        if (exporting) {
            OutlinedTextField(
                repeated,
                { repeated = it.take(256) },
                Modifier.fillMaxWidth(),
                label = { Text("Repeat password") },
                singleLine = true,
                visualTransformation =
                    androidx.compose.ui.text.input.PasswordVisualTransformation(),
                keyboardOptions =
                    androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                        autoCorrectEnabled = false,
                    ),
            )
            Text(
                "Use at least 12 characters. Keep this password somewhere safe; it is needed to restore the backup on another device.",
                fontSize = 12.sp,
                color = Muted,
            )
        }
    }
}

@Composable
fun CsvImportEditor(
    raw: String,
    data: BudgetData,
    dismiss: () -> Unit,
    complete: suspend (List<Entry>) -> Unit,
) {
    val parsed = remember(raw) { runCatching { readCsv(raw) } }
    if (parsed.isFailure) {
        AlertDialog(
            dismiss,
            { Text("CSV could not be read") },
            { Text(parsed.exceptionOrNull()?.message.orEmpty()) },
            { TextButton(dismiss) { Text("Close") } },
            {},
        )
        return
    }
    val rows = parsed.getOrThrow()
    val headers = rows.first()
    val initial = remember(raw) { defaultCsvMapping(headers) }
    var title by rememberSaveable { mutableIntStateOf(initial.title) }
    var amount by rememberSaveable { mutableIntStateOf(initial.amount) }
    var date by rememberSaveable { mutableIntStateOf(initial.date) }
    var type by rememberSaveable { mutableIntStateOf(initial.type) }
    var category by rememberSaveable { mutableIntStateOf(initial.category) }
    var account by rememberSaveable { mutableIntStateOf(initial.account) }
    var advanced by rememberSaveable { mutableStateOf(false) }
    var destination by rememberSaveable { mutableIntStateOf(initial.destination) }
    var splits by rememberSaveable { mutableIntStateOf(initial.splits) }
    var fee by rememberSaveable { mutableIntStateOf(initial.fee) }
    var note by rememberSaveable { mutableIntStateOf(initial.note) }
    var cleared by rememberSaveable { mutableIntStateOf(initial.cleared) }
    var recordId by rememberSaveable { mutableIntStateOf(initial.id) }
    var format by rememberSaveable { mutableStateOf("ISO") }
    var skipMatching by rememberSaveable { mutableStateOf(true) }
    var preview by remember { mutableStateOf<CsvPreview?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var working by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope { kotlinx.coroutines.Dispatchers.Main.immediate }
    fun reset() {
        preview = null
        error = null
    }
    Editor(
        "Import CSV",
        dismiss,
        {
            val result = requireNotNull(preview) { "Preview this mapping first." }
            require(result.entries.isNotEmpty()) { "There are no new records to import." }
            complete(result.entries)
        },
        "Import new transactions",
    ) {
        fun label(index: Int) = if (index < 0) "Not mapped" else "${index + 1}. ${headers[index]}"
        val choices = listOf("Not mapped") + headers.mapIndexed { i, h -> "${i + 1}. $h" }
        @Composable
        fun mapping(name: String, index: Int, change: (Int) -> Unit) {
            ChoiceField(
                name,
                label(index),
                choices,
                { value ->
                    change(choices.indexOf(value) - 1)
                    reset()
                },
            )
        }
        Text(
            "${rows.size - 1} rows. Map your columns, then preview. Matching records are skipped.",
            fontSize = 12.sp,
            color = Muted,
        )
        mapping("Title", title) { title = it }
        mapping("Amount", amount) { amount = it }
        mapping("Date", date) { date = it }
        mapping("Type · optional", type) { type = it }
        mapping("Category · optional", category) { category = it }
        mapping("Account · optional", account) { account = it }
        TextButton({ advanced = !advanced }) {
            Text(if (advanced) "Hide advanced columns" else "Map additional columns")
        }
        if (advanced) {
            mapping("Destination account · transfers", destination) { destination = it }
            mapping("Record ID", recordId) { recordId = it }
            mapping("Note", note) { note = it }
            mapping("Cleared · true/false", cleared) { cleared = it }
            mapping("Splits · Verdant JSON", splits) { splits = it }
            mapping("Fee · Verdant JSON", fee) { fee = it }
        }
        ChoiceField(
            "Date format",
            format,
            listOf("ISO", "DD/MM/YYYY"),
            {
                format = it
                reset()
            },
        )
        ToggleRow(
            "Skip matching transactions",
            "Matching IDs are always skipped. Turn this off to retain legitimate identical purchases.",
            skipMatching,
            {
                skipMatching = it
                reset()
            },
        )
        Text(
            "Without a type column, + amounts are income and other amounts are expenses. Unknown categories become Other; unknown account names must be created first.",
            fontSize = 11.sp,
            color = Muted,
        )
        OutlinedButton(
            {
                if (!working) {
                    working = true
                    scope.launch {
                        try {
                            preview =
                                withContext(Dispatchers.Default) {
                                    previewCsv(
                                        raw,
                                        initial.copy(
                                            title = title,
                                            amount = amount,
                                            date = date,
                                            type = type,
                                            category = category,
                                            account = account,
                                            dateFormat = format,
                                            destination = destination,
                                            id = recordId,
                                            note = note,
                                            cleared = cleared,
                                            splits = splits,
                                            fee = fee,
                                        ),
                                        data,
                                        skipMatching,
                                    )
                                }
                            error = null
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            error = e.message
                        } finally {
                            working = false
                        }
                    }
                }
            },
            Modifier.fillMaxWidth(),
            enabled = !working,
        ) {
            Text(if (working) "Checking…" else "Preview import")
        }
        if (error != null) Text(error!!, color = ErrorColor, fontSize = 12.sp)
        preview?.let { p ->
            Text(
                "${p.entries.size} new · ${p.duplicates} duplicates skipped · ${p.skippedUnknownCategories} categories mapped to Other",
                fontSize = 12.sp,
                color = Lime,
            )
            p.entries.take(8).forEach { e ->
                ToolRow(
                    e.title,
                    "${friendlyDate(e.date)} · ${e.category} · ${data.accountName(e.accountId)}",
                ) {
                    AmountText(money(e.amount, data.currency))
                }
            }
            if (p.entries.size > 8)
                Text("And ${p.entries.size - 8} more records.", fontSize = 11.sp, color = Muted)
        }
    }
}
