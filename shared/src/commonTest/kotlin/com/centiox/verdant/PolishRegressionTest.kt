package com.centiox.verdant

import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.*

class PolishRegressionTest {
    @Test
    fun appearancePersistsWithoutBreakingOlderBackups() {
        val original = sampleData("Owner", "USD")
        val colored = original.copy(backgroundColor = "#268CBA", animateBackground = false)
        assertEquals(colored, decodeBackup(encodeBackup(colored)))
        val legacy =
            JsonObject(
                    Json.parseToJsonElement(encodeBackup(original)).jsonObject.filterKeys {
                        it !in listOf("backgroundColor", "animateBackground")
                    }
                )
                .toString()
        assertEquals(original, decodeBackup(legacy))
        assertFailsWith<IllegalArgumentException> {
            validate(original.copy(backgroundColor = "#ZZZZZZ"))
        }
        assertEquals("#A1B2C3", colorHex(backgroundColor("#a1b2c3")))
    }

    @Test
    fun decimalKeyboardFormatsStayExactAndRejectGrouping() {
        mapOf(".50" to 50L, ",50" to 50L, "12," to 1200L, "12." to 1200L, " 12,34 " to 1234L)
            .forEach { (text, cents) -> assertEquals(cents, parseAmount(text)) }
        listOf("1,234", "1,000.00", "1.000,00", "999999999999999999999999", ",", ".", "0,00")
            .forEach { assertFailsWith<IllegalArgumentException> { parseAmount(it) } }
    }

    @Test
    fun categoryDrilldownDoesNotSearchUnrelatedTitles() {
        val data =
            BudgetData(
                transactions =
                    listOf(
                        Entry("food", "Weekly shop", 500, "expense", "Groceries", "2026-11-02"),
                        Entry(
                            "other",
                            "Groceries storage container",
                            900,
                            "expense",
                            "Shopping",
                            "2026-11-03",
                        ),
                        Entry("income", "Weekly refund", 300, "income", "Groceries", "2026-11-04"),
                    )
            )
        assertEquals(
            listOf("food"),
            filteredTransactions(data, "2026-11", " weekly ", "expense", "Groceries").map { it.id },
        )
        assertEquals(
            listOf("other", "food"),
            filteredTransactions(data, "2026-11", "groceries", "expense").map { it.id },
        )
    }

    @Test
    fun freshUpdateLoadsExistingLedgerBeforeEditing() = runTest {
        val original = sampleData("Owner", "KES")
        var disk = encodeBackup(original)
        val repository =
            BudgetRepository(
                object : BudgetStorage {
                    override suspend fun read() = disk

                    override suspend fun write(json: String) {
                        disk = json
                    }
                }
            )
        repository.update { it.copy(name = "Updated") }
        assertEquals(original.copy(name = "Updated"), decodeBackup(disk))
    }

    @Test
    fun recoveryCanReplaceUnreadableStorageWithoutReadingIt() = runTest {
        var disk = "unreadable"
        val repository =
            BudgetRepository(
                object : BudgetStorage {
                    override suspend fun read(): String = error("Cannot decrypt")

                    override suspend fun write(json: String) {
                        disk = json
                    }
                }
            )
        val original = sampleData("Recovered", "USD")
        repository.restore(encodeBackup(original))
        assertEquals(original, decodeBackup(disk))
        repository.reset()
        assertEquals(BudgetData(), decodeBackup(disk))
    }

    @Test
    fun copyingBudgetLimitsPreservesTransactionsAndExistingPlans() {
        val source = sampleData("Owner", "USD")
        val target = shiftMonth(currentMonth(), 1)
        val copied = source.withPreviousMonthLimits(target)
        assertEquals(source.budgets[currentMonth()], copied.budgets[target])
        assertEquals(source.transactions, copied.transactions)
        assertEquals(source.budgets[currentMonth()], copied.budgets[currentMonth()])
        assertFailsWith<IllegalArgumentException> { copied.withPreviousMonthLimits(target) }
    }

    @Test
    fun supportedCalendarRangeCannotBeNavigatedPast() {
        assertEquals(MIN_MONTH, shiftMonth(MIN_MONTH, -1))
        assertEquals(MAX_MONTH, shiftMonth(MAX_MONTH, 1))
        assertEquals("2027-01", shiftMonth("2026-12", 1))
        val entry = Entry("dated", "Old entry", 100, "expense", "Other", "0000-01-01")
        assertFailsWith<IllegalArgumentException> {
            validate(BudgetData(transactions = listOf(entry)))
        }
        assertFailsWith<IllegalArgumentException> {
            validate(BudgetData(budgets = mapOf("0000-01" to mapOf("Other" to 100L))))
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun interruptedAnimationClosesSavedSheetButParentCancellationDoesNot() = runTest {
        var dismissed = false
        finishSheet({ throw CancellationException("Animation replaced") }, { dismissed = true })
        assertTrue(dismissed)
        dismissed = false
        val task = launch { finishSheet({ awaitCancellation() }, { dismissed = true }) }
        runCurrent()
        task.cancelAndJoin()
        assertFalse(dismissed)
    }
}
