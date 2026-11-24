package com.centiox.verdant

import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest

class ModelTest {
    @Test
    fun decimalMoneyIsExact() {
        assertEquals(29L, parseAmount("0.29"))
        assertEquals(1999L, parseAmount("19.99"))
        assertEquals(100000000000L, parseAmount("1000000000"))
        listOf("0", "-1", "1.999", "1e4", "NaN", "", "1000000000.01", "999999999999999999999999999")
            .forEach { assertFails { parseAmount(it) } }
        assertEquals("−$1,234.50", money(-123450, "USD"))
    }

    @Test
    fun totalsSeparateMonthFromLifetime() {
        val data =
            BudgetData(
                transactions =
                    listOf(
                        Entry(
                            title = "Old salary",
                            amount = 10000,
                            type = "income",
                            category = "Other",
                            date = "2026-10-01",
                        ),
                        Entry(
                            title = "Salary",
                            amount = 50000,
                            type = "income",
                            category = "Other",
                            date = "2026-11-01",
                        ),
                        Entry(
                            title = "Market",
                            amount = 1200,
                            type = "expense",
                            category = "Groceries",
                            date = "2026-11-03",
                        ),
                    )
            )
        val t = totals(data, "2026-11")
        assertEquals(50000L, t.income)
        assertEquals(1200L, t.spent)
        assertEquals(58800L, t.balance)
        assertEquals(1200L, t.byCategory["Groceries"])
    }

    @Test
    fun backupsRejectInvalidEntriesAndDates() {
        val data = sampleData("Alex", "KES")
        assertEquals(data, decodeBackup(encodeBackup(data)))
        assertFails {
            validate(data.copy(transactions = data.transactions + data.transactions.first()))
        }
        assertFails {
            validate(
                data.copy(
                    transactions = listOf(data.transactions.first().copy(date = "2026-02-30"))
                )
            )
        }
        assertFails {
            validate(data.copy(transactions = listOf(data.transactions.first().copy(amount = -1))))
        }
        assertFails { validate(data.copy(budgets = mapOf("2026-13" to mapOf("Groceries" to 100)))) }
        assertFails { decodeBackup("{\"version\":99}") }
        assertFails { decodeBackup("{}") }
        assertEquals("January 2027", monthLabel(shiftMonth("2026-12", 1)))
    }

    @Test
    fun failedWritesAndBadRestoresPreserveLedger() = runTest {
        var disk: String? = null
        var fail = false
        val storage =
            object : BudgetStorage {
                override suspend fun read() = disk

                override suspend fun write(json: String) {
                    if (fail) error("Disk full")
                    disk = json
                }
            }
        val repo = BudgetRepository(storage)
        repo.load()
        val original = sampleData("Alex", "USD")
        repo.update { original }
        fail = true
        assertFails { repo.update { it.copy(name = "Lost") } }
        assertEquals(original, repo.data.value)
        assertFails { repo.restore("invalid") }
        assertEquals(original, repo.data.value)
        fail = false
        val reloaded = BudgetRepository(storage)
        reloaded.load()
        assertEquals(original, reloaded.data.value)
    }

    @Test
    fun concurrentWritesDoNotLoseEntries() = runTest {
        val storage =
            object : BudgetStorage {
                override suspend fun read(): String? = null

                override suspend fun write(json: String) {
                    delay(1)
                }
            }
        val repository = BudgetRepository(storage)
        coroutineScope {
            repeat(100) { index ->
                launch {
                    repository.update {
                        it.copy(
                            transactions =
                                it.transactions +
                                    Entry(
                                        id = index.toString(),
                                        title = "Entry $index",
                                        amount = 100,
                                        type = "income",
                                        category = "Other",
                                        date = "2026-11-23",
                                    )
                        )
                    }
                }
            }
        }
        assertEquals(100, repository.data.value.transactions.size)
        assertEquals(10000L, totals(repository.data.value, "2026-11").balance)
    }

    @Test
    fun exportSnapshotLoadsBeforeReturning() = runTest {
        val original = sampleData("Restored process", "KES")
        val storage =
            object : BudgetStorage {
                override suspend fun read() = encodeBackup(original)

                override suspend fun write(json: String) {}
            }
        val repo = BudgetRepository(storage)
        assertEquals(original, decodeBackup(repo.snapshot()))
        assertEquals(original, repo.data.value)
    }

    @Test
    fun retriedDraftDoesNotCreateDuplicateMoney() {
        val entry =
            Entry(
                id = "stable-draft",
                title = "Salary",
                amount = 50000,
                type = "income",
                category = "Other",
                date = "2026-11-23",
            )
        val once = BudgetData().withEntry(entry)
        val retried = once.withEntry(entry)
        assertEquals(1, retried.transactions.size)
        assertEquals(50000L, totals(retried, "2026-11").balance)
        val edited = retried.withEntry(entry.copy(amount = 60000))
        assertEquals(1, edited.transactions.size)
        assertEquals(60000L, totals(edited, "2026-11").balance)
    }
}
