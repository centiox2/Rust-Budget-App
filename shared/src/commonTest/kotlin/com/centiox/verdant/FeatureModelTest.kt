package com.centiox.verdant

import kotlin.test.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*

class FeatureModelTest {
    private fun row(
        id: String = "one",
        amount: Long = 10000,
        type: String = "expense",
        category: String = "Groceries",
        date: String = "2026-11-24",
        account: String = "main",
        target: String? = null,
        splits: List<Split> = emptyList(),
        fee: TransactionFee? = null,
    ) = Entry(id, "A purchase", amount, type, category, date, account, target, splits, fee = fee)

    @Test
    fun templatesAcceptTheSameTitleLengthAsTransactions() {
        val entry = row().copy(title = "A".repeat(100))
        val data =
            BudgetData(
                transactions = listOf(entry),
                templates = listOf(EntryTemplate(name = entry.title, entry = entry)),
            )
        assertEquals(data, decodeBackup(encodeBackup(data)))
    }

    @Test
    fun draftsCannotResurrectCompletedRecordsOrLoseChangedTransfers() {
        val entry = row(type = "transfer", target = "cash")
        val data =
            BudgetData(
                accounts = listOf(Account(), Account("cash", "Cash"), Account("bank", "Bank")),
                transactions = listOf(entry),
            )
        val fresh =
            EntryDraft(
                entry.id,
                title = "A purchase",
                amount = "100.00",
                type = "transfer",
                category = "Groceries",
                date = "2026-11-24",
                targetAccountId = "cash",
            )
        assertNull(data.acceptDraft(fresh).entryDraft)
        val edit = fresh.copy(sourceId = entry.id)
        assertNull(data.acceptDraft(edit).entryDraft)
        assertEquals(
            "bank",
            data.acceptDraft(edit.copy(targetAccountId = "bank")).entryDraft!!.targetAccountId,
        )
        assertNotNull(
            data.acceptDraft(edit.copy(splits = listOf(SplitDraft("Other", "bad")))).entryDraft
        )
        assertNull(data.copy(transactions = emptyList()).acceptDraft(edit).entryDraft)
    }

    @Test
    fun transferPreservesNetWorthAndFeeIsAnExpenseOnce() {
        val fee = quoteMpesaFee("Agent withdrawal", 100000)!!.copy(confirmed = true)
        val data =
            BudgetData(
                accounts =
                    listOf(
                        Account(openingBalance = 200000, kind = "M-Pesa"),
                        Account("cash", "Cash", kind = "Cash"),
                    ),
                transactions =
                    listOf(
                        row(
                            amount = 100000,
                            type = "transfer",
                            account = "main",
                            target = "cash",
                            fee = fee,
                        )
                    ),
            )
        validate(data)
        assertEquals(97100, data.accountBalance("main"))
        assertEquals(100000, data.accountBalance("cash"))
        val totals = totals(data, "2026-11")
        assertEquals(197100, totals.balance)
        assertEquals(0, totals.income)
        assertEquals(2900, totals.spent)
        assertEquals(2900, totals.byCategory[FEE_CATEGORY])
    }

    @Test
    fun splitSpendingAndFeesMatchTheTotal() {
        val data =
            BudgetData(
                transactions =
                    listOf(
                        row(
                            splits = listOf(Split("Groceries", 6500), Split("Dining", 3500)),
                            fee = TransactionFee(700, "Send to M-Pesa", true),
                        )
                    )
            )
        validate(data)
        val totals = totals(data, "2026-11")
        assertEquals(6500, totals.byCategory["Groceries"])
        assertEquals(3500, totals.byCategory["Dining"])
        assertEquals(10700, totals.spent)
        assertEquals(totals.spent, totals.byCategory.values.sum())
        assertEquals(1, filteredTransactions(data, "2026-11", category = "Dining").size)
        assertFailsWith<IllegalArgumentException> {
            validate(data.copy(transactions = listOf(row(splits = listOf(Split("Dining", 1000))))))
        }
    }

    @Test
    fun categoryRenamePreservesHistoricalAllocationsAndTemplates() {
        val e = row(splits = listOf(Split("Groceries", 10000)))
        val data =
            BudgetData(
                transactions = listOf(e),
                budgets = mapOf("2026-11" to mapOf("Groceries" to 50000)),
                recurring =
                    listOf(
                        Recurring(
                            title = "Shop",
                            amount = 100,
                            category = "Groceries",
                            nextDate = "2026-11-25",
                        )
                    ),
                templates = listOf(EntryTemplate(name = "Shop", entry = e)),
            )
        val renamed = data.renameCategory("Groceries", "Food shop")
        validate(renamed)
        assertEquals(10000, totals(renamed, "2026-11").byCategory["Food shop"])
        assertEquals(50000, renamed.budgets["2026-11"]!!["Food shop"])
        assertEquals("Food shop", renamed.recurring.single().category)
        assertEquals("Food shop", renamed.templates.single().entry.splits.single().category)
        assertFalse("Groceries" in renamed.activeCategories())
        assertTrue("Groceries" in renamed.allCategories())
        assertFailsWith<IllegalArgumentException> { data.renameCategory("Groceries", "Dining") }
    }

    @Test
    fun accountReferencesCannotBeBrokenAndActiveAccountIsRequired() {
        assertFailsWith<IllegalArgumentException> {
            validate(BudgetData(accounts = listOf(Account(archived = true))))
        }
        assertFailsWith<IllegalArgumentException> {
            validate(BudgetData(transactions = listOf(row(account = "missing"))))
        }
        assertFailsWith<IllegalArgumentException> {
            validate(BudgetData(transactions = listOf(row(type = "transfer", target = "main"))))
        }
    }

    @Test
    fun recurringDatesKeepTheirOriginalDayAcrossShortMonths() {
        val jan = Recurring(title = "Rent", amount = 1000, nextDate = "2027-01-31")
        assertEquals("2027-02-28", nextOccurrence(jan))
        assertEquals("2027-03-31", nextOccurrence(jan.copy(nextDate = "2027-02-28")))
        val leap = jan.copy(nextDate = "2024-02-29", frequency = "yearly", anchorDay = 29)
        assertEquals("2025-02-28", nextOccurrence(leap))
        assertEquals("2028-02-29", nextOccurrence(leap.copy(nextDate = "2027-02-28")))
    }

    @Test
    fun scheduledPaymentIsRecordedOnlyWhenConfirmedAndAdvancesAtomically() {
        val r =
            Recurring(id = "rent", title = "Rent", amount = 20000, nextDate = today().toString())
        val data = BudgetData(recurring = listOf(r))
        assertEquals(0, totals(data, currentMonth()).spent)
        val paid = data.payScheduled(r.id)
        validate(paid)
        assertEquals(1, paid.transactions.size)
        assertEquals(20000, totals(paid, currentMonth()).spent)
        assertFailsWith<IllegalArgumentException> { paid.payScheduled(r.id) }
        assertEquals(r.id, paid.recurring.single().id)
    }

    @Test
    fun weeklyAndPaydayPeriodsCrossMonthsCorrectly() {
        val weekly = BudgetData(budgetCycle = "weekly", payday = 25).period(LocalDate(2026, 11, 1))
        assertEquals(LocalDate(2026, 10, 26), weekly.start)
        assertEquals(LocalDate(2026, 11, 2), weekly.end)
        val payday = BudgetData(budgetCycle = "payday", payday = 25).period(LocalDate(2026, 11, 24))
        assertEquals(LocalDate(2026, 10, 25), payday.start)
        assertEquals(LocalDate(2026, 11, 25), payday.end)
        assertTrue(validBudgetKey(payday.key))
        assertFalse(validBudgetKey("weekly|1|bad"))
    }

    @Test
    fun rolloverIncludesSpendingInUnplannedPeriodsWithoutLosingMoney() {
        val data =
            BudgetData(
                rollover = true,
                budgets = mapOf("2026-09" to mapOf("Groceries" to 10000)),
                transactions = listOf(row(date = "2026-10-15", amount = 2500)),
            )
        assertEquals(7500, data.limitsFor(data.period(LocalDate(2026, 11, 24)))["Groceries"])
        val next = data.copy(budgets = data.budgets + ("2026-11" to mapOf("Groceries" to 5000)))
        assertEquals(12500, next.limitsFor(next.period(LocalDate(2026, 11, 24)))["Groceries"])
        val overspent = data.copy(transactions = listOf(row(date = "2026-10-15", amount = 15000)))
        assertTrue(overspent.limitsFor(overspent.period(LocalDate(2026, 11, 24))).isEmpty())
    }

    @Test
    fun undoAndBatchRestoreDoNotDuplicateEditedEntries() {
        val original = BudgetData(transactions = (0 until 300).map { row(id = "id$it") })
        val removed = original.deleteEntries(original.transactions.map { it.id }.toSet())
        assertEquals(200, removed.deleted.size)
        val restored = removed.restoreEntries(original.transactions)
        assertEquals(300, restored.transactions.size)
        validate(restored)
        assertEquals(restored, restored.restoreEntries(original.transactions))
        val undo = original.deleteEntries(setOf("id0")).undoDelete(setOf("id0"))
        assertEquals(300, undo.transactions.size)
    }

    @Test
    fun goalsDoNotAlterAccountBalancesAndContributionsValidate() {
        val data =
            BudgetData(
                goals =
                    listOf(
                        SavingsGoal(
                            name = "Reserve",
                            target = 100000,
                            contributions =
                                listOf(Contribution(amount = 2000, date = today().toString())),
                        )
                    )
            )
        validate(data)
        assertEquals(0, totals(data, currentMonth()).balance)
        assertFailsWith<IllegalArgumentException> {
            validate(data.copy(goals = listOf(data.goals.single().copy(target = 0))))
        }
    }

    @Test
    fun legacyBackupsMigrateWithoutDroppingDataAndNewBackupsUseVersionTwo() {
        val original = BudgetData(name = "Alex", onboarded = true, transactions = listOf(row()))
        val old = encodeBackup(original.copy(version = 1))
        val loaded = decodeBackup(old)
        assertEquals(2, loaded.version)
        assertEquals(original.transactions, loaded.transactions)
        assertEquals(original, decodeBackup(encodeBackup(original)))
        assertFailsWith<IllegalArgumentException> {
            decodeBackup(encodeBackup(original).replace("\"version\": 2", "\"version\": 99"))
        }
    }

    @Test
    fun csvRoundTripKeepsTransfersSplitsFeesNotesAndSpreadsheetSafety() {
        val transfer =
            row(
                id = "transfer",
                type = "transfer",
                target = "cash",
                fee = TransactionFee(2900, "Agent withdrawal", true),
            )
        val expense =
            row(id = "split", splits = listOf(Split("Groceries", 6000), Split("Dining", 4000)))
                .copy(title = "=SUM(1,2)", note = "A quoted \"note\"\nnext line")
        val source =
            BudgetData(
                accounts = listOf(Account(), Account("cash", "Cash")),
                transactions = listOf(transfer, expense),
            )
        val csv = exportCsv(source)
        assertTrue(csv.contains("'=SUM"))
        val empty = source.copy(transactions = emptyList())
        val preview = previewCsv(csv, defaultCsvMapping(readCsv(csv).first()), empty)
        assertEquals(source.transactions, preview.entries)
        assertEquals(0, preview.duplicates)
        assertEquals(2, previewCsv(csv, defaultCsvMapping(readCsv(csv).first()), source).duplicates)
    }

    @Test
    fun csvMappingSupportsQuotesSemicolonsAndExplicitDuplicateChoice() {
        val csv =
            "when;description;value;kind\r\n24/11/2026;\"Tea; café\";25;expense\r\n24/11/2026;\"Tea; café\";25;expense"
        val mapping = CsvMapping(1, 2, 0, 3, dateFormat = "DD/MM/YYYY")
        assertEquals(1, previewCsv(csv, mapping, BudgetData()).entries.size)
        assertEquals(2, previewCsv(csv, mapping, BudgetData(), false).entries.size)
        assertFailsWith<IllegalArgumentException> { readCsv("a,b\n\"unfinished,b") }
        assertFailsWith<IllegalArgumentException> {
            previewCsv(csv, mapping.copy(dateFormat = "ISO"), BudgetData())
        }
    }

    @Test
    fun feeTariffBoundariesAndUnsupportedCasesAreExplicit() {
        assertEquals(0, quoteMpesaFee("Send to M-Pesa", 10000)!!.amount)
        assertEquals(700, quoteMpesaFee("Send to M-Pesa", 10100)!!.amount)
        assertEquals(1300, quoteMpesaFee("Send to M-Pesa", 100000)!!.amount)
        assertEquals(2300, quoteMpesaFee("Send to M-Pesa", 100100)!!.amount)
        assertEquals(2900, quoteMpesaFee("Agent withdrawal", 100000)!!.amount)
        assertEquals(30900, quoteMpesaFee("Agent withdrawal", 25_000_000)!!.amount)
        assertNull(quoteMpesaFee("Agent withdrawal", 4900))
        assertNull(quoteMpesaFee("Send to M-Pesa", 10050))
        assertNull(quoteMpesaFee("Pochi la Biashara", 100000))
        assertNull(quoteMpesaFee("Buy Goods fuel station", 100000))
        assertEquals(0, quoteMpesaFee("Buy Goods till", 100000)!!.amount)
        assertEquals(1300, quoteMpesaFee("Paybill · shared", 200000)!!.amount)
        assertEquals(2000, quoteMpesaFee("Paybill · I pay", 200000)!!.amount)
        assertEquals(0, quoteMpesaFee("Paybill · business pays", 200000)!!.amount)
    }

    @Test
    fun feeBreakdownSeparatesTypesBandsAndConfirmedCharges() {
        val rows =
            listOf(
                row(
                    id = "a",
                    amount = 100000,
                    fee = quoteMpesaFee("Send to M-Pesa", 100000)!!.copy(confirmed = true),
                ),
                row(id = "b", amount = 100100, fee = quoteMpesaFee("Send to M-Pesa", 100100)),
                row(
                    id = "c",
                    amount = 100000,
                    type = "transfer",
                    target = "cash",
                    fee = quoteMpesaFee("Agent withdrawal", 100000)!!.copy(confirmed = true),
                ),
            )
        val buckets = feeBreakdown(rows)
        assertEquals(3, buckets.size)
        assertEquals(4200, buckets.sumOf { it.confirmed })
        assertEquals(2300, buckets.sumOf { it.estimated })
    }

    @Test
    fun atomicFailedFeatureChangesPreserveExistingLedger() = runTest {
        var raw = encodeBackup(BudgetData())
        val repo =
            BudgetRepository(
                object : BudgetStorage {
                    override suspend fun read() = raw

                    override suspend fun write(json: String) {
                        error("disk full")
                    }
                }
            )
        repo.load()
        assertFailsWith<IllegalStateException> {
            repo.update { it.copy(accounts = it.accounts + Account("cash", "Cash")) }
        }
        assertEquals(BudgetData(), repo.data.value)
        assertEquals(BudgetData(), decodeBackup(raw))
    }
}
