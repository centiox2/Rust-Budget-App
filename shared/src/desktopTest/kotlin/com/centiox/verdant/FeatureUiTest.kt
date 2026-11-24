package com.centiox.verdant

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.*
import java.io.File
import kotlin.test.*
import org.jetbrains.skia.EncodedImageFormat

@OptIn(ExperimentalTestApi::class)
class FeatureUiTest {
    @get:org.junit.Rule val timeout = org.junit.rules.Timeout.seconds(120)

    private fun SkikoComposeUiTest.save(name: String) {
        mainClock.advanceTimeBy(800)
        waitForIdle()
        val dir = File(System.getProperty("verdant.screenshots")).apply { mkdirs() }
        val bitmap = onAllNodes(isRoot()).onLast().captureToImage().asSkiaBitmap()
        File(dir, name)
            .writeBytes(
                org.jetbrains.skia.Image.makeFromBitmap(bitmap)
                    .encodeToData(EncodedImageFormat.PNG)!!
                    .bytes
            )
    }

    @Test
    fun analyticsIncludesCustomCategoriesAndFeeOnlyTransfers() =
        runSkikoComposeUiTest(size = Size(360f, 900f)) {
            val fee = quoteMpesaFee("Agent withdrawal", 100000)!!
            val data =
                BudgetData(
                    name = "Alex",
                    onboarded = true,
                    reducedMotion = true,
                    accounts = listOf(Account(), Account("cash", "Cash")),
                    categorySettings = listOf(CategorySetting("Family")),
                    transactions =
                        listOf(
                            Entry(
                                "send",
                                "Family support",
                                10000,
                                "expense",
                                "Family",
                                today().toString(),
                            ),
                            Entry(
                                "cash",
                                "Cash withdrawal",
                                100000,
                                "transfer",
                                "Other",
                                today().toString(),
                                targetAccountId = "cash",
                                fee = fee,
                            ),
                        ),
                )
            setContent { VerdantTheme(data) { AnalyticsScreen(data, currentMonth(), {}, {}, {}) } }
            onNodeWithTag("analytics-category-Family").performScrollTo().assertIsDisplayed()
            onNodeWithTag("analytics-category-Transaction fees")
                .performScrollTo()
                .assertIsDisplayed()
            save("feature-custom-category-insights-360.png")
        }

    @Test
    fun csvImportRequiresPreviewAndShowsOnlyNewRecords() =
        runSkikoComposeUiTest(size = Size(393f, 900f)) {
            val data = BudgetData(name = "Alex", onboarded = true, reducedMotion = true)
            var imported: List<Entry>? = null
            setContent {
                VerdantTheme(data) {
                    CsvImportEditor(
                        "title,amount,date,type\nImported lunch,12.50,2026-11-24,expense",
                        data,
                        {},
                        { imported = it },
                    )
                }
            }
            onNodeWithText("Import new transactions", useUnmergedTree = true)
                .performScrollTo()
                .performClick()
            onNodeWithText("Preview this mapping first.").assertExists()
            assertNull(imported)
            onNodeWithText("Preview import").performScrollTo().performClick()
            waitUntil(timeoutMillis = 10000) {
                onAllNodesWithText("Imported lunch").fetchSemanticsNodes().isNotEmpty()
            }
            save("feature-csv-preview-393.png")
            onNodeWithText("Import new transactions", useUnmergedTree = true)
                .performScrollTo()
                .performClick()
            waitUntil(timeoutMillis = 10000) { imported != null }
            assertEquals(1250, imported!!.single().amount)
        }

    @Test
    fun accountCreationCategoryAndGoalEditorsPersistAndScroll() =
        runSkikoComposeUiTest(size = Size(360f, 760f)) {
            var ledger by
                mutableStateOf(
                    BudgetData(
                        name = "Alex",
                        onboarded = true,
                        currency = "KES",
                        reducedMotion = true,
                    )
                )
            setContent {
                VerdantTheme(ledger) {
                    PlanningScreen(
                        "Accounts",
                        ledger,
                        { ledger = validate(it(ledger)) },
                        {},
                        {},
                        {},
                        {},
                        {},
                        {},
                        PlatformServices(pinWidget = {}),
                    )
                }
            }
            onNodeWithText("Add account").performClick()
            onNodeWithTag("field-Account name").performTextInput("Cash wallet")
            onNodeWithTag("field-Opening balance").performTextReplacement("5000")
            onNodeWithText("Save", useUnmergedTree = true).performScrollTo().performClick()
            waitUntil(timeoutMillis = 10000) { ledger.accounts.size == 2 }
            onNodeWithText("Cash wallet").assertExists()
            save("feature-accounts-360.png")
            onNodeWithText("Categories", useUnmergedTree = true).performClick()
            onNodeWithText("Add category").performClick()
            onNodeWithTag("field-Category name").performTextInput("Family support")
            onNodeWithText("Save", useUnmergedTree = true).performScrollTo().performClick()
            waitUntil(timeoutMillis = 10000) { "Family support" in ledger.activeCategories() }
            onNodeWithText("Family support").performScrollTo().assertIsDisplayed()
            onNodeWithText("Goals", useUnmergedTree = true).performScrollTo().performClick()
            onNodeWithText("Create savings goal").performClick()
            save("feature-goal-editor-360.png")
            onNodeWithTag("field-Goal name").performTextInput("Emergency fund")
            onNodeWithTag("field-Target amount").performTextInput("20000")
            onNodeWithText("Save", useUnmergedTree = true).performScrollTo().performClick()
            waitUntil(timeoutMillis = 10000) { ledger.goals.isNotEmpty() }
            onNodeWithText("Add contribution", useUnmergedTree = true)
                .performScrollTo()
                .performClick()
            onNodeWithTag("field-Contribution amount").performTextInput("2500")
            onNodeWithText("Save", useUnmergedTree = true).performScrollTo().performClick()
            waitUntil(timeoutMillis = 10000) { ledger.goals.single().contributions.isNotEmpty() }
            assertEquals(250000, ledger.goals.single().contributions.single().amount)
            assertTrue(ledger.transactions.isEmpty())
            save("feature-goals-360.png")
        }

    @Test
    fun mpesaEntryCalculatesConfirmsAndReportsFeeSeparately() =
        runSkikoComposeUiTest(size = Size(393f, 900f)) {
            val ledger =
                BudgetData(
                    name = "Alex",
                    onboarded = true,
                    currency = "KES",
                    reducedMotion = true,
                    accounts = listOf(Account(kind = "M-Pesa", openingBalance = 500000)),
                )
            var saved by mutableStateOf<Entry?>(null)
            var open by mutableStateOf(true)
            setContent {
                VerdantTheme(ledger) {
                    if (open)
                        EntrySheet(
                            null,
                            "expense",
                            currentMonth(),
                            { open = false },
                            {
                                saved = it
                                open = false
                            },
                            {},
                        )
                    else
                        PlanningScreen(
                            "Fees",
                            ledger.copy(transactions = listOfNotNull(saved)),
                            {},
                            {},
                            {},
                            {},
                            {},
                            {},
                            {},
                            PlatformServices(),
                        )
                }
            }
            onNodeWithTag("entry-amount").performTextInput("1000")
            onNodeWithTag("entry-title").performTextInput("Family payment")
            onNodeWithTag("toggle-Fee confirmed").performScrollTo().performClick()
            save("feature-mpesa-entry-393.png")
            onNodeWithTag("save-entry").performScrollTo().performClick()
            waitUntil(timeoutMillis = 10000) { saved != null }
            assertEquals(1300, saved!!.fee!!.amount)
            assertTrue(saved!!.fee!!.confirmed)
            val totals = totals(ledger.copy(transactions = listOf(saved!!)), currentMonth())
            assertEquals(101300, totals.spent)
            assertEquals(1300, totals.byCategory[FEE_CATEGORY])
            onNodeWithText("Confirmed fees").assertExists()
            save("feature-fee-report-393.png")
        }

    @Test
    fun planningTabsRemainReachableAfterLongScrollAtLargeText() =
        runSkikoComposeUiTest(
            size = Size(360f, 760f),
            density = androidx.compose.ui.unit.Density(1f, 1.4f),
        ) {
            val ledger =
                BudgetData(
                    name = "Alex",
                    onboarded = true,
                    currency = "KES",
                    reducedMotion = true,
                    categorySettings = (1..30).map { CategorySetting("Custom category $it") },
                )
            setContent {
                VerdantTheme(ledger) {
                    PlanningScreen(
                        "Categories",
                        ledger,
                        {},
                        {},
                        {},
                        {},
                        {},
                        {},
                        {},
                        PlatformServices(pinWidget = {}),
                    )
                }
            }
            onNodeWithText("Custom category 30").performScrollTo().assertIsDisplayed()
            onNodeWithText("Accounts", useUnmergedTree = true).performClick()
            onNodeWithText("Add account").assertIsDisplayed()
            onNodeWithText("Tools", useUnmergedTree = true).performScrollTo().performClick()
            onNodeWithText("Quick-entry widget").performScrollTo()
            save("feature-tools-bottom-360.png")
        }
}
