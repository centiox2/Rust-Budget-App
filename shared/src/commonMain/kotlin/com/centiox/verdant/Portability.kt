package com.centiox.verdant

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** RFC 4180 reader: quoted separators, newlines, escaped quotes, BOM, and CRLF. */
fun readCsv(raw: String): List<List<String>> {
    require(raw.length <= 5_000_000) { "CSV is too large." }
    val text = raw.removePrefix("\uFEFF")
    val first = text.lineSequence().firstOrNull().orEmpty()
    val separator = if (first.count { it == ';' } > first.count { it == ',' }) ';' else ','
    val rows = mutableListOf<List<String>>()
    val row = mutableListOf<String>()
    val field = StringBuilder()
    var quoted = false
    var closed = false
    var i = 0
    fun pushField() {
        row += field.toString()
        field.clear()
        closed = false
    }
    fun pushRow() {
        pushField()
        if (row.any { it.isNotBlank() }) rows += row.toList()
        row.clear()
        require(rows.size <= 10_001) { "CSV contains too many rows." }
    }
    while (i < text.length) {
        val c = text[i]
        when {
            c == '"' && quoted ->
                if (i + 1 < text.length && text[i + 1] == '"') {
                    field.append('"')
                    i++
                } else {
                    quoted = false
                    closed = true
                }
            c == '"' && field.isEmpty() && !closed -> quoted = true
            quoted -> field.append(c)
            c == separator -> pushField()
            c == '\n' || c == '\r' -> {
                pushRow()
                if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
            }
            closed -> require(c.isWhitespace()) { "Unexpected characters after a quoted field." }
            c == '"' -> error("Quote an entire CSV field, not part of it.")
            else -> field.append(c)
        }
        i++
    }
    require(!quoted) { "CSV contains an unclosed quote." }
    if (field.isNotEmpty() || row.isNotEmpty()) pushRow()
    require(rows.size >= 2) { "CSV needs a header and at least one transaction." }
    require(rows.first().size <= 40 && rows.all { it.size == rows.first().size }) {
        "CSV rows have different column counts."
    }
    return rows
}

data class CsvMapping(
    val title: Int,
    val amount: Int,
    val date: Int,
    val type: Int = -1,
    val category: Int = -1,
    val account: Int = -1,
    val id: Int = -1,
    val dateFormat: String = "ISO",
    val destination: Int = -1,
    val splits: Int = -1,
    val cleared: Int = -1,
    val note: Int = -1,
    val fee: Int = -1,
    val format: Int = -1,
)

fun defaultCsvMapping(headers: List<String>): CsvMapping {
    fun col(vararg names: String) = headers.indexOfFirst { it.trim().lowercase() in names }
    return CsvMapping(
        col("title", "description", "payee", "name"),
        col("amount", "value"),
        col("date", "transaction date"),
        col("type"),
        col("category"),
        col("account", "wallet"),
        col("id"),
        destination = col("destination"),
        splits = col("splits"),
        cleared = col("cleared"),
        note = col("note"),
        fee = col("fee"),
        format = col("verdant_csv_format"),
    )
}

data class CsvPreview(
    val entries: List<Entry>,
    val duplicates: Int,
    val skippedUnknownCategories: Int,
)

private fun fingerprint(e: Entry) =
    listOf(
            e.title.trim().lowercase(),
            e.amount.toString(),
            e.type,
            e.category,
            e.date,
            e.accountId,
            e.targetAccountId.orEmpty(),
            e.splits.joinToString { "${it.category}:${it.amount}" },
            e.fee?.let { "${it.amount}:${it.kind}" }.orEmpty(),
        )
        .joinToString("|")

fun previewCsv(
    raw: String,
    mapping: CsvMapping,
    data: BudgetData,
    skipMatching: Boolean = true,
): CsvPreview {
    val rows = readCsv(raw)
    val headers = rows.first()
    require(listOf(mapping.title, mapping.amount, mapping.date).all { it in headers.indices }) {
        "Map the title, amount, and date columns."
    }
    val ids = data.transactions.map { it.id }.toMutableSet()
    val fingerprints = data.transactions.map(::fingerprint).toMutableSet()
    var duplicates = 0
    var unknown = 0
    val result = mutableListOf<Entry>()
    rows.drop(1).forEachIndexed { index, row ->
        fun field(n: Int): String {
            val value = row.getOrNull(n)?.trim().orEmpty()
            return if (
                row.getOrNull(mapping.format) == "verdant-csv-1" &&
                    value.length > 1 &&
                    value[0] == '\'' &&
                    value[1] in "=+-@\t\r\n "
            )
                value.drop(1)
            else value
        }
        try {
            val rawAmount = field(mapping.amount).replace("−", "-")
            val amount = parseAmount(rawAmount.removePrefix("-").removePrefix("+"))
            val type =
                field(mapping.type).lowercase().ifBlank {
                    if (rawAmount.startsWith('+')) "income" else "expense"
                }
            require(type in listOf("income", "expense", "transfer")) {
                "Use income, expense, or transfer for the transaction type."
            }
            val category =
                field(mapping.category)
                    .ifBlank { "Other" }
                    .let {
                        if (it in data.allCategories()) it
                        else {
                            unknown++
                            "Other"
                        }
                    }
            val accountName = field(mapping.account)
            val account =
                data.accounts
                    .firstOrNull { it.id == accountName || it.name.equals(accountName, true) }
                    ?.id
                    ?: if (accountName.isBlank()) "main"
                    else error("Unknown account: $accountName. Create it before importing.")
            val date =
                if (mapping.dateFormat == "DD/MM/YYYY") {
                    val p = field(mapping.date).split('/')
                    require(p.size == 3) { "Use DD/MM/YYYY." }
                    "${p[2]}-${p[1].padStart(2,'0')}-${p[0].padStart(2,'0')}"
                } else field(mapping.date)
            val destinationName = field(mapping.destination)
            val destination =
                if (type == "transfer")
                    data.accounts
                        .firstOrNull {
                            it.id == destinationName || it.name.equals(destinationName, true)
                        }
                        ?.id ?: error("Map the destination account for transfers.")
                else null
            val splits =
                if (field(mapping.splits).isBlank()) emptyList()
                else Json.decodeFromString<List<Split>>(field(mapping.splits))
            val fee =
                if (field(mapping.fee).isBlank()) null
                else Json.decodeFromString<TransactionFee>(field(mapping.fee))
            val entry =
                Entry(
                    field(mapping.id).ifBlank(::newId),
                    field(mapping.title),
                    amount,
                    type,
                    category,
                    date,
                    account,
                    destination,
                    splits,
                    field(mapping.cleared).equals("true", true),
                    field(mapping.note),
                    fee,
                )
            validate(data.copy(transactions = listOf(entry), entryDraft = null))
            if (entry.id in ids || (skipMatching && fingerprint(entry) in fingerprints))
                duplicates++
            else {
                ids += entry.id
                fingerprints += fingerprint(entry)
                result += entry
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("CSV row ${index + 2}: ${e.message}")
        }
    }
    require(data.transactions.size + result.size <= 10_000) {
        "Import would exceed 10,000 transactions."
    }
    return CsvPreview(result, duplicates, unknown)
}

fun exportCsv(data: BudgetData): String {
    fun q(value: String) = "\"" + value.replace("\"", "\"\"") + "\""
    val headers =
        listOf(
            "id",
            "title",
            "amount",
            "type",
            "category",
            "date",
            "account",
            "destination",
            "splits",
            "cleared",
            "note",
            "fee",
            "verdant_csv_format",
        )
    val records =
        data.transactions.map { e ->
            listOf(
                e.id,
                e.title,
                amountInput(e.amount),
                e.type,
                e.category,
                e.date,
                data.accountName(e.accountId),
                e.targetAccountId?.let(data::accountName).orEmpty(),
                if (e.splits.isEmpty()) "" else Json.encodeToString(e.splits),
                e.cleared.toString(),
                e.note,
                e.fee?.let { Json.encodeToString(it) }.orEmpty(),
                "verdant-csv-1",
            )
        }
    // Formula-like user text is quoted and prefixed for safe spreadsheet display.
    fun safe(v: String) =
        if (
            v.trimStart().firstOrNull() in listOf('=', '@', '+', '-') ||
                v.startsWith('\t') ||
                v.startsWith('\r')
        )
            "'$v"
        else v
    return (listOf(headers) + records.map { it.map(::safe) }).joinToString("\r\n") {
        it.joinToString(",", transform = ::q)
    }
}

expect fun protectBackup(json: String, password: String): String

expect fun unprotectBackup(envelope: String, password: String): String

fun isProtectedBackup(raw: String) =
    runCatching {
            kotlinx.serialization.json.Json.parseToJsonElement(raw).let {
                it is kotlinx.serialization.json.JsonObject && "verdantEncrypted" in it
            }
        }
        .getOrDefault(false)
