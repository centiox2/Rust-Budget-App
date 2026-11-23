package com.centiox.verdant

import android.util.AtomicFile
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.*
import org.junit.rules.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppFlowTest {
    private val compose = createAndroidComposeRule<MainActivity>()
    @get:Rule
    val rules: TestRule =
        RuleChain.outerRule(
                object : ExternalResource() {
                    override fun before() {
                        AtomicFile(
                                File(
                                    InstrumentationRegistry.getInstrumentation()
                                        .targetContext
                                        .filesDir,
                                    "verdant-budget.vdb",
                                )
                            )
                            .delete()
                    }
                }
            )
            .around(compose)

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        listOf(
                "mkdir -p /sdcard/Download/verdant-test-screenshots",
                "screencap -p /sdcard/Download/verdant-test-screenshots/$name",
            )
            .forEach { command ->
                android.os.ParcelFileDescriptor.AutoCloseInputStream(
                        automation.executeShellCommand(command)
                    )
                    .use { it.readBytes() }
            }
    }

    @Test
    fun nativeLedgerFlowAndPersistence() {
        compose.waitUntil(15000) {
            compose.onAllNodesWithTag("start").fetchSemanticsNodes().isNotEmpty()
        }
        screenshot("android-welcome.png")
        compose.onNodeWithTag("start").performScrollTo().performClick()
        compose.onNodeWithTag("profile-name").performTextInput("Alex Morgan")
        compose.onNodeWithTag("sample-data").performClick()
        compose.onNodeWithTag("finish-setup").performScrollTo().performClick()
        compose.waitUntil(15000) {
            compose.onAllNodesWithTag("total-balance").fetchSemanticsNodes().isNotEmpty()
        }
        val initial = totals(sampleData("Alex Morgan", "USD"), currentMonth())
        compose.onNodeWithTag("total-balance").assertTextEquals(money(initial.balance, "USD"))
        compose.activityRule.scenario.recreate()
        compose.waitUntil(15000) {
            compose.onAllNodesWithTag("total-balance").fetchSemanticsNodes().isNotEmpty()
        }
        screenshot("android-home.png")
        compose.onNodeWithContentDescription("Add transaction").performClick()
        compose.onNodeWithTag("entry-amount").performTextInput("2.999")
        compose.onNodeWithTag("entry-title").performTextInput("Native test purchase")
        compose.onNodeWithTag("save-entry").performScrollTo().performClick()
        compose.onNodeWithTag("form-error").assertTextContains("2 decimal", substring = true)
        compose.onNodeWithTag("entry-amount").performTextReplacement("49.99")
        compose.onNodeWithTag("save-entry").performScrollTo().performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithTag("save-entry").fetchSemanticsNodes().isEmpty()
        }
        compose
            .onNodeWithTag("total-balance")
            .assertTextEquals(money(initial.balance - 4999, "USD"))
        val stored = File(compose.activity.filesDir, "verdant-budget.vdb").readBytes()
        Assert.assertTrue(
            stored.take(4).toByteArray().contentEquals(byteArrayOf(0x56, 0x44, 0x42, 0x32))
        )
        Assert.assertFalse(String(stored, Charsets.UTF_8).contains("Native test purchase"))
        compose.activityRule.scenario.recreate()
        compose.waitUntil(15000) {
            compose.onAllNodesWithTag("total-balance").fetchSemanticsNodes().isNotEmpty()
        }
        compose
            .onNodeWithTag("total-balance")
            .assertTextEquals(money(initial.balance - 4999, "USD"))
        compose.onNodeWithContentDescription("Analytics").performClick()
        compose
            .onNodeWithTag("analytics-total")
            .assertTextEquals(money(initial.spent + 4999, "USD"))
        screenshot("android-analytics.png")
        compose.onNodeWithContentDescription("Budgets").performClick()
        screenshot("android-budgets.png")
        compose.onNodeWithText("Set limits").performClick()
        compose.onNodeWithTag("budget-Groceries").performTextReplacement("100")
        compose.onNodeWithTag("save-budgets").performScrollTo().performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithTag("save-budgets").fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithText("$50.75 over limit").assertExists()
        compose.onNodeWithContentDescription("Transactions").performClick()
        compose.onNodeWithTag("transaction-search").performTextInput("Native test purchase")
        compose.onAllNodesWithText("Native test purchase").onLast().performClick()
        compose.onNodeWithText("Delete transaction").performScrollTo().performClick()
        compose.onNodeWithText("Delete", useUnmergedTree = true).performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithTag("save-entry").fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithContentDescription("Home").performClick()
        compose.onNodeWithTag("total-balance").assertTextEquals(money(initial.balance, "USD"))
    }
}
