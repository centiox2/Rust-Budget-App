package com.centiox.verdant

import android.app.Application
import android.view.WindowManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.rules.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LockFlowTest {
    private val compose = createAndroidComposeRule<MainActivity>()
    @get:Rule
    val rules: TestRule =
        RuleChain.outerRule(
                object : ExternalResource() {
                    override fun before() = runBlocking {
                        val app =
                            InstrumentationRegistry.getInstrumentation()
                                .targetContext
                                .applicationContext as Application
                        AndroidBudgetStorage(app)
                            .write(
                                encodeBackup(
                                    sampleData("Private owner", "KES").copy(appLock = true)
                                )
                            )
                    }
                }
            )
            .around(compose)

    @Test
    fun lockedLedgerAndDialogsStayHiddenAcrossRecreation() {
        compose.waitUntil(15000) {
            compose.onAllNodesWithText("Your ledger is locked.").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Private owner").assertDoesNotExist()
        compose.onNodeWithTag("total-balance").assertDoesNotExist()
        compose.onNodeWithText("Unlock").assertIsDisplayed()
        compose.runOnUiThread {
            Assert.assertTrue(
                compose.activity.window.attributes.flags and
                    WindowManager.LayoutParams.FLAG_SECURE != 0
            )
        }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(15000) {
            compose.onAllNodesWithText("Your ledger is locked.").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("total-balance").assertDoesNotExist()
    }
}
