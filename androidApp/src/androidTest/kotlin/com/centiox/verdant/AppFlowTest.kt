package com.centiox.verdant

import android.util.AtomicFile
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso
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
            .around(
                object : TestWatcher() {
                    override fun failed(e: Throwable, description: org.junit.runner.Description) {
                        runCatching {
                            screenshot("android-flow-failure.png")
                            compose.onRoot().printToLog("VerdantFlow")
                        }
                    }
                }
            )

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
    fun scrollingAndProfileKeyboardKeepContentReachable() {
        compose.waitUntil(15000) {
            compose.onAllNodesWithTag("start").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("start").performScrollTo().performClick()
        compose.onNodeWithTag("profile-name").performTextInput("Scroll owner")
        compose.onNodeWithTag("sample-data").performClick()
        // Let Android finish the IME layout change before targeting the setup button.
        Espresso.closeSoftKeyboard()
        compose.waitForIdle()
        compose.onNodeWithTag("finish-setup").performScrollTo().performClick()
        compose.waitUntil(15000) {
            compose.onAllNodesWithTag("total-balance").fetchSemanticsNodes().isNotEmpty()
        }
        fun assertAboveNavigation(node: SemanticsNodeInteraction) {
            node.assertIsDisplayed()
            Assert.assertTrue(
                node.fetchSemanticsNode().boundsInRoot.bottom <=
                    compose
                        .onNodeWithContentDescription("Home")
                        .fetchSemanticsNode()
                        .boundsInRoot
                        .top
            )
        }
        compose.onNodeWithContentDescription("Transactions").performClick()
        compose.onNodeWithTag("transaction-list").performTouchInput { swipeUp() }
        compose.onNodeWithTag("transaction-list").performScrollToNode(hasText("Monthly salary"))
        assertAboveNavigation(compose.onNodeWithText("Monthly salary"))
        screenshot("android-scroll-activity.png")
        compose.onNodeWithContentDescription("Budgets").performClick()
        compose.onNodeWithTag("budgets-scroll").performTouchInput { swipeUp() }
        compose.onNodeWithText("Utilities").performScrollTo()
        assertAboveNavigation(compose.onNodeWithText("Utilities"))
        screenshot("android-scroll-budgets.png")
        compose.onNodeWithContentDescription("Analytics").performClick()
        compose.onNodeWithTag("analytics-scroll").performTouchInput { swipeUp() }
        compose.onNodeWithText("Internet bill").performScrollTo()
        assertAboveNavigation(compose.onNodeWithText("Internet bill"))
        screenshot("android-scroll-insights.png")
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithTag("settings-scroll").performTouchInput { swipeUp() }
        compose.onNodeWithTag("settings-footer").performScrollTo()
        assertAboveNavigation(compose.onNodeWithTag("settings-footer"))
        screenshot("android-scroll-settings.png")
        compose
            .onNodeWithTag("settings-name")
            .performScrollTo()
            .performTextReplacement("Updated profile")
        Espresso.closeSoftKeyboard()
        compose.waitForIdle()
        compose.onNodeWithText("Save preferences").performScrollTo().performClick()
        compose.onNodeWithText("Make it yours.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Home").performClick()
        compose.waitUntil(15000) {
            compose.onAllNodesWithText("Updated profile").fetchSemanticsNodes().isNotEmpty()
        }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(15000) {
            compose.onAllNodesWithText("Updated profile").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun appearanceSurvivesActivityRecreation() {
        compose.waitUntil(15000) {
            compose.onAllNodesWithTag("start").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("start").performScrollTo().performClick()
        compose.onNodeWithTag("profile-name").performTextInput("Appearance owner")
        // Let Android finish the IME layout change before targeting the setup button.
        Espresso.closeSoftKeyboard()
        compose.waitForIdle()
        compose.onNodeWithTag("finish-setup").performScrollTo().performClick()
        compose.waitUntil(15000) {
            compose.onAllNodesWithTag("total-balance").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithContentDescription("Ocean background").performScrollTo().performClick()
        compose.waitUntil(15000) {
            compose
                .onAllNodes(hasContentDescription("Ocean background") and isSelected())
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose.onNodeWithTag("background-motion").performScrollTo().performClick()
        compose.waitUntil(15000) {
            compose
                .onAllNodes(hasTestTag("background-motion") and isOff())
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(15000) {
            compose
                .onAllNodesWithContentDescription("Ocean background")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose
            .onNodeWithContentDescription("Ocean background")
            .performScrollTo()
            .assertIsSelected()
        compose.onNodeWithTag("background-motion").performScrollTo().assertIsOff()
        screenshot("android-appearance.png")
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
        // Let Android finish the IME layout change before targeting the setup button.
        Espresso.closeSoftKeyboard()
        compose.waitForIdle()
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
        Espresso.closeSoftKeyboard()
        compose.waitForIdle()
        compose.onNodeWithTag("save-entry").performScrollTo().performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithTag("form-error").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("form-error").assertTextContains("2 decimal", substring = true)
        compose.onNodeWithTag("entry-amount").performTextReplacement("49.99")
        Espresso.closeSoftKeyboard()
        compose.waitForIdle()
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
        Espresso.closeSoftKeyboard()
        compose.waitForIdle()
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
