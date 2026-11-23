package com.centiox.verdant

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.Density
import java.io.File
import kotlin.test.Test
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
