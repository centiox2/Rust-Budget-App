@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.centiox.verdant

import kotlin.random.Random
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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

fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

fun currentMonth(): String = today().toString().take(7)

fun newId(): String =
    Random.nextLong().toULong().toString(16) + Random.nextLong().toULong().toString(16)

fun monthLabel(month: String): String {
    val date = LocalDate.parse("$month-01")
    return "${date.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${date.year}"
}

fun shiftMonth(month: String, delta: Int): String =
    LocalDate.parse("$month-01").plus(delta, DateTimeUnit.MONTH).toString().take(7)

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
)

data class Totals(
    val income: Long,
    val spent: Long,
    val balance: Long,
    val byCategory: Map<String, Long>,
)

fun parseAmount(text: String): Long {
    val value = text.trim()
    require(Regex("[0-9]+(\\.[0-9]{1,2})?").matches(value)) {
        "Use a positive amount with up to 2 decimal places."
    }
    val parts = value.split('.')
    val whole = parts[0].toLongOrNull() ?: error("Amount is too large.")
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
                it.title.trim().isNotEmpty() &&
                it.title.length <= 100 &&
                it.amount in 1..MAX_AMOUNT &&
                it.type in listOf("income", "expense") &&
                it.category in categories
        ) {
            "Invalid transaction."
        }
        require(
            runCatching { LocalDate.parse(it.date).toString() == it.date }.getOrDefault(false)
        ) {
            "Invalid transaction date."
        }
    }
    data.budgets.forEach { (month, limits) ->
        require(Regex("[0-9]{4}-(0[1-9]|1[0-2])").matches(month)) { "Invalid budget month." }
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

private val backupJson = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun encodeBackup(data: BudgetData): String = backupJson.encodeToString(validate(data))

fun decodeBackup(raw: String): BudgetData {
    require(raw.length <= 5_000_000) { "Backup is too large." }
    val fields = backupJson.parseToJsonElement(raw).jsonObject
    require(
        fields.keys.containsAll(
            listOf("version", "name", "currency", "onboarded", "transactions", "budgets")
        )
    ) {
        "This is not a complete Verdant backup."
    }
    return validate(backupJson.decodeFromString<BudgetData>(raw))
}

interface BudgetStorage {
    suspend fun read(): String?

    suspend fun write(json: String)
}

class BudgetRepository(private val storage: BudgetStorage) {
    private val mutex = Mutex()
    private val mutableData = MutableStateFlow(BudgetData())
    val data = mutableData.asStateFlow()

    suspend fun load() =
        mutex.withLock { storage.read()?.let { mutableData.value = decodeBackup(it) } }

    suspend fun update(transform: (BudgetData) -> BudgetData) =
        mutex.withLock {
            val next = validate(transform(mutableData.value))
            storage.write(encodeBackup(next))
            mutableData.value = next
        }

    suspend fun restore(raw: String) {
        val imported = decodeBackup(raw)
        update { imported }
    }
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
