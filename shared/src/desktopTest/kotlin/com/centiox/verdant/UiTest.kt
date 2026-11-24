package com.centiox.verdant

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.jetbrains.skia.EncodedImageFormat

@OptIn(ExperimentalTestApi::class)
class UiTest {
    private fun storage(initial: BudgetData? = null) =
        object : BudgetStorage {
            var text = initial?.let(::encodeBackup)

            override suspend fun read() = text

            override suspend fun write(json: String) {
                text = json
            }
        }

    private fun SkikoComposeUiTest.save(name: String) {
        val directory = File(System.getProperty("verdant.screenshots"))
        directory.mkdirs()
        mainClock.advanceTimeBy(400)
        waitForIdle()
        val image = onAllNodes(isRoot()).onLast().captureToImage().asSkiaBitmap()
        File(directory, name)
            .writeBytes(
                org.jetbrains.skia.Image.makeFromBitmap(image)
                    .encodeToData(EncodedImageFormat.PNG)!!
                    .bytes
            )
    }

    @Test
    fun polishedScreensAtPhoneSizes() {
        for (width in listOf(360, 393, 430)) runSkikoComposeUiTest(
            size = Size(width.toFloat(), 900f)
        ) {
            setContent { App(BudgetRepository(storage(sampleData("Alex Morgan", "USD"))), {}, {}) }
            waitUntil(timeoutMillis = 10000) {
                onAllNodesWithTag("total-balance").fetchSemanticsNodes().isNotEmpty()
            }
            save("kotlin-home-$width.png")
            onNodeWithContentDescription("Analytics").performClick()
            waitForIdle()
            save("kotlin-analytics-$width.png")
            onNodeWithContentDescription("Budgets").performClick()
            waitForIdle()
            save("kotlin-budgets-$width.png")
        }
    }

    private fun SkikoComposeUiTest.motionFrame(name: String): ByteArray {
        waitForIdle()
        val bytes =
            org.jetbrains.skia.Image.makeFromBitmap(
                    onAllNodes(isRoot()).onLast().captureToImage().asSkiaBitmap()
                )
                .encodeToData(EncodedImageFormat.PNG)!!
                .bytes
        val directory = File(System.getProperty("verdant.screenshots"))
        directory.mkdirs()
        File(directory, name).writeBytes(bytes)
        return bytes
    }

    @Test
    fun pageMotionHasIntermediateFramesAndSettles() =
        runSkikoComposeUiTest(size = Size(360f, 760f)) {
            setContent {
                App(
                    BudgetRepository(
                        storage(sampleData("Alex", "USD").copy(animateBackground = false))
                    ),
                    {},
                    {},
                )
            }
            waitUntil(timeoutMillis = 10000) {
                onAllNodesWithTag("total-balance").fetchSemanticsNodes().isNotEmpty()
            }
            mainClock.autoAdvance = false
            mainClock.advanceTimeBy(1000)
            onNodeWithContentDescription("Settings").performClick()
            mainClock.advanceTimeByFrame()
            val start = motionFrame("motion-page-start.png")
            mainClock.advanceTimeBy(96)
            val middle = motionFrame("motion-page-middle.png")
            mainClock.advanceTimeBy(600)
            val end = motionFrame("motion-page-settled.png")
            mainClock.advanceTimeBy(400)
            val still = motionFrame("motion-page-still.png")
            assertFalse(
                start.contentEquals(middle),
                "Page transition must render intermediate frames",
            )
            assertFalse(
                middle.contentEquals(end),
                "Transition must complete beyond its middle frame",
            )
            assertTrue(
                end.contentEquals(still),
                "Completed transition must settle without residual movement",
            )
            onNodeWithText("Make it yours.").assertIsDisplayed()
        }

    @Test
    fun spendingChartRevealsSmoothlyAndSettles() =
        runSkikoComposeUiTest(size = Size(360f, 200f)) {
            mainClock.autoAdvance = false
            setContent {
                SpendingLine(
                    listOf(10L, 80L, 35L, 90L, 45L, 60L),
                    androidx.compose.ui.Modifier.fillMaxSize(),
                )
            }
            mainClock.advanceTimeByFrame()
            val start = motionFrame("motion-chart-start.png")
            mainClock.advanceTimeBy(240)
            val middle = motionFrame("motion-chart-middle.png")
            mainClock.advanceTimeBy(1000)
            val end = motionFrame("motion-chart-settled.png")
            mainClock.advanceTimeBy(400)
            val still = motionFrame("motion-chart-still.png")
            assertFalse(start.contentEquals(middle))
            assertFalse(middle.contentEquals(end))
            assertTrue(end.contentEquals(still))
        }

    @Test
    fun allPagesScrollAtPhoneAndLargeTextSizes() {
        for (scale in listOf(1f, 1.4f)) runSkikoComposeUiTest(
            size = Size(360f, 760f),
            density = Density(1f, scale),
        ) {
            val data =
                sampleData("Alex", "KES")
                    .copy(
                        budgets = mapOf(currentMonth() to categories.associateWith { 50000L }),
                        transactions =
                            (0 until 1000).map {
                                Entry(
                                    "scroll-$it",
                                    "Record $it",
                                    1500L + it,
                                    "expense",
                                    categories[it % categories.size],
                                    today().toString(),
                                )
                            },
                    )
            setContent { App(BudgetRepository(storage(data)), {}, {}) }
            waitUntil(timeoutMillis = 10000) {
                onAllNodesWithTag("total-balance").fetchSemanticsNodes().isNotEmpty()
            }
            fun assertAboveNavigation(node: SemanticsNodeInteraction) {
                node.assertIsDisplayed()
                val bounds = node.fetchSemanticsNode().boundsInRoot
                val nav = onNodeWithContentDescription("Home").fetchSemanticsNode().boundsInRoot
                assertTrue(
                    bounds.bottom <= nav.top,
                    "Last content must remain above navigation: $bounds / $nav",
                )
            }
            onNodeWithContentDescription("Transactions").performClick()
            save("coherent-activity-$scale.png")
            repeat(3) { onNodeWithTag("transaction-list").performTouchInput { swipeUp() } }
            assertTrue(onAllNodesWithTag("transaction-scroll-999").fetchSemanticsNodes().isEmpty())
            onNodeWithTag("transaction-list").performScrollToNode(hasText("Record 0"))
            assertAboveNavigation(onNodeWithText("Record 0"))
            save("coherent-activity-bottom-$scale.png")
            onNodeWithTag("transaction-list").performScrollToIndex(0)
            onNodeWithTag("transaction-search").assertIsDisplayed()
            onNodeWithContentDescription("Budgets").performClick()
            save("coherent-budgets-$scale.png")
            repeat(2) { onNodeWithTag("budgets-scroll").performTouchInput { swipeUp() } }
            onNodeWithText("Other").performScrollTo()
            assertAboveNavigation(onNodeWithText("Other"))
            save("coherent-budgets-bottom-$scale.png")
            onNodeWithText("MONTHLY SPENDING PLAN").performScrollTo().assertIsDisplayed()
            onNodeWithContentDescription("Analytics").performClick()
            save("coherent-insights-$scale.png")
            repeat(2) { onNodeWithTag("analytics-scroll").performTouchInput { swipeUp() } }
            onNodeWithText("Record 997").performScrollTo()
            assertAboveNavigation(onNodeWithText("Record 997"))
            save("coherent-insights-bottom-$scale.png")
            onNodeWithTag("analytics-total").performScrollTo().assertIsDisplayed()
            onNodeWithContentDescription("Settings").performClick()
            save("coherent-settings-$scale.png")
            repeat(2) { onNodeWithTag("settings-scroll").performTouchInput { swipeUp() } }
            onNodeWithTag("settings-footer").performScrollTo()
            assertAboveNavigation(onNodeWithTag("settings-footer"))
            save("coherent-settings-bottom-$scale.png")
            onNodeWithText("Reset all data").performScrollTo().performClick()
            onNodeWithText("Cancel").performClick()
            onNodeWithText("Make it yours.").performScrollTo().assertIsDisplayed()
            save("coherent-settings-return-$scale.png")
        }
    }

    @Test
    fun homeLinksUseCurrentMonthAndActionLabelsWork() =
        runSkikoComposeUiTest(size = Size(393f, 900f)) {
            val original = sampleData("Alex", "USD")
            val previous = shiftMonth(currentMonth(), -1)
            val data =
                original.copy(
                    transactions =
                        original.transactions +
                            Entry(
                                "older",
                                "Previous month entry",
                                100,
                                "expense",
                                "Other",
                                "$previous-01",
                            )
                )
            setContent { App(BudgetRepository(storage(data)), {}, {}) }
            waitUntil(timeoutMillis = 10000) {
                onAllNodesWithTag("total-balance").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithContentDescription("Budgets").performClick()
            onNodeWithContentDescription("Previous month").performClick()
            onNodeWithContentDescription("Home").performClick()
            onNodeWithText("Budget").performClick()
            onNodeWithText(
                    "Monthly limits for ${monthLabel(currentMonth())}. Leave blank or enter zero to remove a limit."
                )
                .assertExists()
            onNodeWithContentDescription("Close").performClick()
            onNodeWithContentDescription("Transactions").performClick()
            onNodeWithContentDescription("Previous month").performClick()
            onNodeWithText("1 transaction").assertExists()
            onNodeWithContentDescription("Home").performClick()
            onNodeWithText("See all").performScrollTo().performClick()
            onNodeWithText(monthLabel(currentMonth())).assertExists()
            onNodeWithText("9 transactions").assertExists()
            onNodeWithContentDescription("Home").performClick()
            onNodeWithText("Expense").performClick()
            onNodeWithTag("entry-amount").assertExists()
            onNodeWithContentDescription("Close").performClick()
            onNodeWithText("Income").performClick()
            onNode(hasText("Income") and isSelected()).assertExists()
        }

    @Test
    fun homeShowsCashFlowWithoutPromotionalOrTransactionCards() =
        runSkikoComposeUiTest(size = Size(360f, 900f)) {
            val data =
                BudgetData(
                    name = "Alex",
                    onboarded = true,
                    transactions =
                        listOf(
                            Entry(
                                "spent",
                                "Groceries",
                                100,
                                "expense",
                                "Groceries",
                                today().toString(),
                            )
                        ),
                    budgets = mapOf(currentMonth() to mapOf("Groceries" to 50L)),
                )
            setContent { App(BudgetRepository(storage(data)), {}, {}) }
            waitUntil(timeoutMillis = 10000) {
                onAllNodesWithTag("total-balance").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithTag("monthly-cashflow").assertTextEquals("−$1.00 this month")
            onNodeWithText("Save & Grow").assertDoesNotExist()
            onNodeWithText("Time to rebalance").assertDoesNotExist()
            onNodeWithTag("home-transactions").assertExists()
            onNodeWithText("Groceries").assertExists()
            save("kotlin-home-over-budget.png")
        }

    @Test
    fun customCalendarUpdatesTheEntryDate() =
        runSkikoComposeUiTest(size = Size(393f, 852f)) {
            val repository = BudgetRepository(storage(sampleData("Alex", "USD")))
            setContent { App(repository, {}, {}) }
            waitUntil(timeoutMillis = 10000) {
                onAllNodesWithTag("total-balance").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithContentDescription("Add transaction").performClick()
            onNodeWithContentDescription("Choose date").performScrollTo().performClick()
            onNodeWithContentDescription("1 ${monthLabel(currentMonth())}")
                .performScrollTo()
                .performClick()
            save("kotlin-custom-calendar.png")
            onNodeWithText("Done").performClick()
            onNodeWithTag("entry-date").assertTextContains("${currentMonth()}-01")
            save("kotlin-custom-entry.png")
        }

    @Test
    fun appearancePickerPersistsCustomColorAndMotionChoice() =
        runSkikoComposeUiTest(size = Size(393f, 900f)) {
            val repository = BudgetRepository(storage(sampleData("Alex", "USD")))
            setContent { App(repository, {}, {}) }
            waitUntil(timeoutMillis = 10000) {
                onAllNodesWithTag("total-balance").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithContentDescription("Settings").performClick()
            onNodeWithContentDescription("Ocean background").performScrollTo().performClick()
            waitUntil(timeoutMillis = 10000) { repository.data.value.backgroundColor == "#268CBA" }
            onNodeWithTag("background-motion").performScrollTo().performClick()
            waitUntil(timeoutMillis = 10000) { !repository.data.value.animateBackground }
            onNodeWithTag("background-motion").assertIsOff()
            save("kotlin-liquid-settings.png")
            onNodeWithText("Custom color").performScrollTo().performClick()
            onNodeWithTag("background-hex").performTextReplacement("#NOTHEX")
            onNodeWithText("Apply color").assertIsNotEnabled()
            onNodeWithTag("background-hex").performTextReplacement("#8C5BCE")
            save("kotlin-color-picker.png")
            onNodeWithContentDescription("Red color channel").performSemanticsAction(
                SemanticsActions.SetProgress
            ) {
                it(.8f)
            }
            onNodeWithTag("background-hex").assertTextContains("#CC5BCE")
            onNodeWithText("Apply color").performClick()
            waitUntil(timeoutMillis = 10000) { repository.data.value.backgroundColor == "#CC5BCE" }
            assertFalse(repository.data.value.animateBackground)
            onNodeWithContentDescription("Home").performClick()
            save("kotlin-liquid-purple-home.png")
        }

    @Test
    fun liquidPixelsMoveAndStayStillWhenPaused() =
        runSkikoComposeUiTest(size = Size(360f, 600f)) {
            mainClock.autoAdvance = false
            var moving by mutableStateOf(true)
            setContent {
                LiquidBackground(
                    DEFAULT_BACKGROUND,
                    moving,
                    androidx.compose.ui.Modifier.fillMaxSize(),
                )
            }
            mainClock.advanceTimeByFrame()
            fun pixels(): ByteArray =
                org.jetbrains.skia.Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap())
                    .encodeToData(EncodedImageFormat.PNG)!!
                    .bytes
            val first = pixels()
            save("kotlin-liquid-phase-a.png")
            mainClock.advanceTimeBy(20_000)
            assertFalse(first.contentEquals(pixels()), "The liquid background must actually move.")
            save("kotlin-liquid-phase-b.png")
            runOnIdle { moving = false }
            mainClock.advanceTimeByFrame()
            val paused = pixels()
            mainClock.advanceTimeBy(20_000)
            assertTrue(paused.contentEquals(pixels()), "Paused backgrounds must not drift.")
        }

    @Test
    fun categoryFiltersSurviveNavigationAndCanBeCleared() =
        runSkikoComposeUiTest(size = Size(360f, 900f)) {
            val sample = sampleData("Alex", "USD")
            val data =
                sample.copy(
                    transactions =
                        sample.transactions +
                            Entry(
                                "collision",
                                "Groceries storage",
                                500,
                                "expense",
                                "Shopping",
                                today().toString(),
                            )
                )
            setContent { App(BudgetRepository(storage(data)), {}, {}) }
            waitUntil(timeoutMillis = 10000) {
                onAllNodesWithTag("total-balance").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithContentDescription("Analytics").performClick()
            onAllNodes(hasText("Groceries") and hasClickAction())
                .onFirst()
                .performScrollTo()
                .performClick()
            onNodeWithText("2 transactions").assertExists()
            onNodeWithText("Groceries storage").assertDoesNotExist()
            onNodeWithContentDescription("Home").performClick()
            onNodeWithContentDescription("Transactions").performClick()
            onNodeWithText("2 transactions").assertExists()
            onNodeWithTag("transaction-search").performTextInput(" Weekly ")
            onNodeWithText("1 transaction").assertExists()
            onNodeWithContentDescription("Analytics").performClick()
            onNodeWithContentDescription("Transactions").performClick()
            onNodeWithTag("transaction-search").assertTextContains(" Weekly ")
            save("kotlin-filtered-activity.png")
            onNodeWithContentDescription("Clear search").performClick()
            onNodeWithText("2 transactions").assertExists()
            onNodeWithText("Clear filters").performClick()
            onNodeWithText("10 transactions").assertExists()
        }

    @Test
    fun copyPreviousLimitsAndReturnToCurrentMonth() =
        runSkikoComposeUiTest(size = Size(360f, 900f)) {
            val original = sampleData("Alex", "USD")
            val repository = BudgetRepository(storage(original))
            setContent { App(repository, {}, {}) }
            waitUntil(timeoutMillis = 10000) {
                onAllNodesWithTag("total-balance").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithContentDescription("Budgets").performClick()
            onNodeWithContentDescription("Next month").performClick()
            val next = shiftMonth(currentMonth(), 1)
            onNodeWithText("Use last month's limits").performScrollTo().performClick()
            waitUntil(timeoutMillis = 10000) { repository.data.value.budgets[next] != null }
            assertEquals(original.budgets[currentMonth()], repository.data.value.budgets[next])
            assertEquals(original.transactions, repository.data.value.transactions)
            onNodeWithContentDescription("Home").performClick()
            onNodeWithContentDescription("Budgets").performClick()
            onNodeWithText(monthLabel(next)).assertExists()
            save("kotlin-copied-budgets.png")
            onNodeWithContentDescription("Go to current month").performClick()
            onNodeWithText(monthLabel(currentMonth())).assertExists()
        }

    @Test
    fun largeCurrencyAmountsFitAtLargeTextSizes() =
        runSkikoComposeUiTest(size = Size(360f, 900f), density = Density(1f, 1.4f)) {
            val data =
                BudgetData(
                    name = "Alex",
                    currency = "KES",
                    onboarded = true,
                    transactions =
                        listOf(
                            Entry(
                                "large",
                                "Groceries",
                                MAX_AMOUNT,
                                "expense",
                                "Groceries",
                                today().toString(),
                            )
                        ),
                    budgets = mapOf(currentMonth() to mapOf("Groceries" to MAX_AMOUNT)),
                )
            setContent { App(BudgetRepository(storage(data)), {}, {}) }
            waitUntil(timeoutMillis = 10000) {
                onAllNodesWithTag("total-balance").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithContentDescription("Analytics").performClick()
            onNodeWithTag("analytics-total").performSemanticsAction(
                SemanticsActions.GetTextLayoutResult
            ) { action ->
                val results = mutableListOf<TextLayoutResult>()
                assertTrue(action(results))
                assertFalse(results.single().hasVisualOverflow)
            }
            save("kotlin-analytics-large-text.png")
            onNodeWithContentDescription("Budgets").performClick()
            onNodeWithTag("budget-spent-Groceries", useUnmergedTree = true)
                .performScrollTo()
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                    val results = mutableListOf<TextLayoutResult>()
                    assertTrue(action(results))
                    assertFalse(results.single().hasVisualOverflow)
                }
            save("kotlin-budgets-large-text.png")
        }

    @Test
    fun failedSaveKeepsDraftAndRetryKeepsItsIdentity() =
        runSkikoComposeUiTest(size = Size(393f, 852f)) {
            val attempts = mutableListOf<Entry>()
            var dismissed = false
            setContent {
                VerdantTheme {
                    EntrySheet(
                        null,
                        "expense",
                        currentMonth(),
                        { dismissed = true },
                        { entry ->
                            attempts += entry
                            if (attempts.size == 1) throw java.io.IOException("Disk unavailable")
                        },
                        {},
                    )
                }
            }
            onNodeWithTag("entry-amount").performTextInput(",50")
            onNodeWithTag("entry-title").performTextInput("Morning tea")
            onNodeWithTag("save-entry").performScrollTo().performClick()
            onNodeWithTag("form-error").assertExists()
            onNodeWithTag("entry-title").assertTextContains("Morning tea")
            onNodeWithTag("save-entry").performScrollTo().performClick()
            waitUntil(timeoutMillis = 10000) { dismissed }
            assertEquals(2, attempts.size)
            assertEquals(attempts[0].id, attempts[1].id)
            assertEquals(50L, attempts.last().amount)
        }

    @Test
    fun welcomeAndEntryFlow() =
        runSkikoComposeUiTest(size = Size(393f, 852f)) {
            val repository = BudgetRepository(storage())
            setContent { App(repository, {}, {}) }
            waitUntil(timeoutMillis = 10000) {
                onAllNodesWithTag("start").fetchSemanticsNodes().isNotEmpty()
            }
            save("kotlin-welcome.png")
            onNodeWithTag("start").performScrollTo().performClick()
            onNodeWithTag("profile-name").performTextInput("Alex")
            onNodeWithTag("finish-setup").performScrollTo().performClick()
            waitUntil(timeoutMillis = 10000) {
                onAllNodesWithTag("total-balance").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithContentDescription("Add transaction").performClick()
            waitForIdle()
            save("kotlin-entry.png")
            onNodeWithTag("entry-amount").performTextInput("12.99")
            onNodeWithTag("entry-title").performTextInput("Morning coffee")
            onNodeWithTag("save-entry").performScrollTo().performClick()
            waitForIdle()
            onNodeWithTag("total-balance").assertTextEquals("−$12.99")
            onNodeWithContentDescription("Set a budget").performClick()
            onNodeWithTag("budget-Groceries").performTextInput("100")
            onNodeWithTag("save-budgets").performScrollTo().performClick()
            waitForIdle()
            onNodeWithContentDescription("Budgets").performClick()
            onNodeWithText("$100.00 left to spend").assertExists()
        }

    @Test
    fun longHistoryStaysVirtualizedAndLargeTextFits() =
        runSkikoComposeUiTest(size = Size(360f, 900f), density = Density(1f, 1.4f)) {
            val data =
                BudgetData(
                    name = "Alex",
                    onboarded = true,
                    transactions =
                        (0 until 1000).map {
                            Entry(
                                id = "row-$it",
                                title = "Record $it",
                                amount = 100_000_000_000L,
                                type = "income",
                                category = "Other",
                                date = today().toString(),
                            )
                        },
                )
            setContent { App(BudgetRepository(storage(data)), {}, {}) }
            waitUntil(timeoutMillis = 10000) {
                onAllNodesWithTag("total-balance").fetchSemanticsNodes().isNotEmpty()
            }
            save("kotlin-large-text.png")
            onNodeWithContentDescription("Transactions").performClick()
            onNodeWithText("Record 999").assertExists()
            check(onAllNodes(hasClickAction()).fetchSemanticsNodes().size < 40) {
                "Long histories must not compose all rows."
            }
            onNodeWithTag("transaction-list").performScrollToNode(hasText("Record 0"))
            onNodeWithText("Record 0").assertExists()
        }
}
