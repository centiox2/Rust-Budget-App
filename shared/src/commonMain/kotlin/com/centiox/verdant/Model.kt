@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.centiox.verdant

import kotlin.random.Random
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

val categories =
    listOf(
        "Groceries",
        "Dining",
        "Transport",
        "Shopping",
        "Utilities",
        "Health",
        "Entertainment",
        "Other",
    )
val currencies = listOf("USD", "KES", "EUR", "GBP", "INR")
const val MAX_AMOUNT = 100_000_000_000L
const val DEFAULT_BACKGROUND = "#67A642"
const val MIN_MONTH = "0001-01"
const val MAX_MONTH = "9999-12"

fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

fun currentMonth(): String = today().toString().take(7)

fun newId(): String =
    Random.nextLong().toULong().toString(16) + Random.nextLong().toULong().toString(16)

fun monthLabel(month: String): String {
    val date = LocalDate.parse("$month-01")
    return "${date.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${date.year}"
}

fun shiftMonth(month: String, delta: Int): String {
    val date = LocalDate.parse("$month-01")
    val index = ((date.year - 1) * 12L + date.month.ordinal + delta).coerceIn(0, 9999 * 12L - 1)
    return "${(index / 12 + 1).toString().padStart(4, '0')}-${(index % 12 + 1).toString().padStart(2, '0')}"
}

@Serializable
data class Entry(
    val id: String = newId(),
    val title: String,
    val amount: Long,
    val type: String,
    val category: String,
    val date: String,
)

@Serializable
data class BudgetData(
    val version: Int = 1,
    val name: String = "",
    val currency: String = "USD",
    val onboarded: Boolean = false,
    val transactions: List<Entry> = emptyList(),
    val budgets: Map<String, Map<String, Long>> = emptyMap(),
    val backgroundColor: String = DEFAULT_BACKGROUND,
    val animateBackground: Boolean = true,
)

data class Totals(
    val income: Long,
    val spent: Long,
    val balance: Long,
    val byCategory: Map<String, Long>,
)

/** A stable entry ID makes a retry after activity recreation idempotent. */
fun BudgetData.withEntry(entry: Entry): BudgetData =
    copy(
        transactions =
            if (transactions.any { it.id == entry.id })
                transactions.map { if (it.id == entry.id) entry else it }
            else transactions + entry
    )

fun parseAmount(text: String): Long {
    val value = text.trim().replace(',', '.')
    require(Regex("(?:[0-9]+(?:\\.[0-9]{0,2})?|\\.[0-9]{1,2})").matches(value)) {
        "Use a positive amount with up to 2 decimal places."
    }
    val parts = value.split('.')
    val whole = requireNotNull(parts[0].ifEmpty { "0" }.toLongOrNull()) { "Amount is too large." }
    require(whole <= MAX_AMOUNT / 100) { "Amount is too large." }
    val minor = parts.getOrElse(1) { "" }.padEnd(2, '0').toLong()
    val result = whole * 100 + minor
    require(result in 1..MAX_AMOUNT) { "Amount must be between 0.01 and 1 billion." }
    return result
}

fun amountInput(value: Long): String = "${value / 100}.${(value % 100).toString().padStart(2, '0')}"

fun money(value: Long, currency: String): String {
    val absolute = if (value < 0) -value else value
    val whole = (absolute / 100).toString().reversed().chunked(3).joinToString(",").reversed()
    val symbol =
        when (currency) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "INR" -> "₹"
            else -> "KES "
        }
    return "${if (value < 0) "−" else ""}$symbol$whole.${(absolute % 100).toString().padStart(2, '0')}"
}

fun validate(data: BudgetData): BudgetData {
    require(Regex("#[0-9a-fA-F]{6}").matches(data.backgroundColor)) {
        "Invalid background color. Use a six-digit hex color."
    }
    require(
        data.version == 1 &&
            data.name.length <= 50 &&
            (!data.onboarded || data.name.isNotBlank()) &&
            data.currency in currencies
    ) {
        "Unsupported or invalid Verdant backup."
    }
    require(data.transactions.size <= 10_000) { "A ledger can contain up to 10,000 transactions." }
    require(data.transactions.map { it.id }.toSet().size == data.transactions.size) {
        "Duplicate transaction IDs."
    }
    data.transactions.forEach {
        require(
            it.id.isNotBlank() &&
                it.id.length <= 80 &&
                it.title.trim().isNotEmpty() &&
                it.title.length <= 100 &&
                it.amount in 1..MAX_AMOUNT &&
                it.type in listOf("income", "expense") &&
                it.category in categories
        ) {
            "Invalid transaction."
        }
        require(
            runCatching {
                    LocalDate.parse(it.date).let { date ->
                        date.year in 1..9999 && date.toString() == it.date
                    }
                }
                .getOrDefault(false)
        ) {
            "Invalid transaction date."
        }
    }
    data.budgets.forEach { (month, limits) ->
        require(Regex("[0-9]{4}-(0[1-9]|1[0-2])").matches(month) && month >= MIN_MONTH) {
            "Invalid budget month."
        }
        limits.forEach { (category, amount) ->
            require(category in categories && amount in 1..MAX_AMOUNT) {
                "Invalid category budget."
            }
        }
    }
    return data
}

fun totals(data: BudgetData, month: String): Totals {
    val rows = data.transactions.filter { it.date.startsWith(month) }
    return Totals(
        rows.filter { it.type == "income" }.sumOf { it.amount },
        rows.filter { it.type == "expense" }.sumOf { it.amount },
        data.transactions.sumOf { if (it.type == "income") it.amount else -it.amount },
        categories.associateWith { c ->
            rows.filter { it.type == "expense" && it.category == c }.sumOf { it.amount }
        },
    )
}

fun filteredTransactions(
    data: BudgetData,
    month: String,
    query: String = "",
    type: String = "all",
    category: String? = null,
): List<Entry> =
    data.transactions
        .asReversed()
        .filter {
            it.date.startsWith(month) &&
                (type == "all" || it.type == type) &&
                (category == null || it.category == category) &&
                "${it.title} ${it.category}".contains(query.trim(), true)
        }
        .sortedByDescending { it.date }

fun BudgetData.withPreviousMonthLimits(month: String): BudgetData {
    require(budgets[month].isNullOrEmpty()) { "This month already has budget limits." }
    val previous = budgets[shiftMonth(month, -1)]
    require(!previous.isNullOrEmpty()) { "There are no limits to copy from last month." }
    return copy(budgets = budgets + (month to previous.toMap()))
}

private val backupJson = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun encodeBackup(data: BudgetData): String {
    val text = backupJson.encodeToString(validate(data))
    require(text.length <= 5_000_000) {
        "This ledger is too large to save. Export and archive older records first."
    }
    return text
}

fun decodeBackup(raw: String): BudgetData {
    require(raw.length <= 5_000_000) { "Backup is too large." }
    val cleaned = raw.trim().removePrefix("\uFEFF")
    val fields =
        try {
            backupJson.parseToJsonElement(cleaned).jsonObject
        } catch (_: Exception) {
            throw IllegalArgumentException("Choose a valid Verdant JSON backup.")
        }
    require(
        fields.keys.containsAll(
            listOf("version", "name", "currency", "onboarded", "transactions", "budgets")
        )
    ) {
        "This is not a complete Verdant backup."
    }
    val parsed =
        try {
            fields.getValue("transactions").jsonArray.forEach { item ->
                require(
                    item.jsonObject.keys.containsAll(
                        listOf("id", "title", "amount", "type", "category", "date")
                    )
                ) {
                    "Incomplete transaction."
                }
            }
            backupJson.decodeFromString<BudgetData>(cleaned)
        } catch (_: Exception) {
            throw IllegalArgumentException(
                "The backup contains an invalid or incomplete transaction."
            )
        }
    return validate(parsed)
}

interface BudgetStorage {
    suspend fun read(): String?

    suspend fun write(json: String)
}

class BudgetRepository(private val storage: BudgetStorage) {
    private val mutex = Mutex()
    private var initialized = false
    private val mutableData = MutableStateFlow(BudgetData())
    val data = mutableData.asStateFlow()

    suspend fun load() = mutex.withLock { readCurrent() }

    private suspend fun readCurrent() {
        val raw = storage.read()
        mutableData.value =
            withContext(Dispatchers.Default) { raw?.let(::decodeBackup) ?: BudgetData() }
        initialized = true
    }

    private suspend fun commit(next: BudgetData) {
        val encoded = withContext(Dispatchers.Default) { encodeBackup(next) }
        storage.write(encoded)
        mutableData.value = next
        initialized = true
    }

    /** Also safe when an Android document result arrives before the new activity loads its UI. */
    suspend fun snapshot(): String =
        mutex.withLock {
            if (!initialized) {
                readCurrent()
            }
            withContext(Dispatchers.Default) { encodeBackup(mutableData.value) }
        }

    suspend fun update(transform: (BudgetData) -> BudgetData) =
        mutex.withLock {
            if (!initialized) readCurrent()
            commit(transform(mutableData.value))
        }

    suspend fun restore(raw: String) {
        val imported = withContext(Dispatchers.Default) { decodeBackup(raw) }
        // A recovery restore must also work when the existing file cannot be decrypted.
        mutex.withLock { commit(imported) }
    }

    suspend fun reset() = mutex.withLock { commit(BudgetData()) }
}

fun sampleData(name: String, currency: String): BudgetData {
    val month = currentMonth()
    fun entry(type: String, title: String, amount: Long, category: String, day: Int) =
        Entry(
            title = title,
            amount = amount,
            type = type,
            category = category,
            date = "$month-${minOf(day, today().day).toString().padStart(2, '0')}",
        )
    return BudgetData(
        name = name,
        currency = currency,
        onboarded = true,
        budgets =
            mapOf(
                month to
                    mapOf(
                        "Groceries" to 50000,
                        "Dining" to 20000,
                        "Transport" to 18000,
                        "Shopping" to 30000,
                        "Utilities" to 25000,
                    )
            ),
        transactions =
            listOf(
                entry("income", "Monthly salary", 420000, "Other", 1),
                entry("expense", "Fresh market", 6840, "Groceries", 3),
                entry("expense", "Coffee & company", 1250, "Dining", 5),
                entry("expense", "City commute", 2400, "Transport", 7),
                entry("expense", "Weekly groceries", 8235, "Groceries", 9),
                entry("expense", "Internet bill", 5999, "Utilities", 12),
                entry("expense", "A little something", 8900, "Shopping", 15),
                entry("income", "Freelance project", 65000, "Other", 17),
                entry("expense", "Dinner with friends", 4650, "Dining", 20),
            ),
    )
}
