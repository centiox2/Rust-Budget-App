package com.centiox.verdant

import android.app.Application
import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.rules.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeatureFlowTest {
    private val compose = createAndroidComposeRule<MainActivity>()
    private val app
        get() =
            InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
                as Application

    @get:Rule
    val rules: TestRule =
        RuleChain.outerRule(
                object : ExternalResource() {
                    override fun before() = runBlocking {
                        AndroidBudgetStorage(app)
                            .write(
                                encodeBackup(
                                    BudgetData(
                                        onboarded = true,
                                        name = "Feature owner",
                                        currency = "KES",
                                        reducedMotion = true,
                                        accounts =
                                            listOf(
                                                Account(kind = "M-Pesa", openingBalance = 500000),
                                                Account("cash", "Cash", kind = "Cash"),
                                            ),
                                    )
                                )
                            )
                    }
                }
            )
            .around(compose)

    @Test
    fun mpesaFeesQuickEntryAndRecreationPersist() {
        compose.waitUntil(15000) {
            compose.onAllNodesWithTag("total-balance").fetchSemanticsNodes().isNotEmpty()
        }
        // The widget launches the same explicit intent, without copying any private ledger data.
        compose.runOnUiThread {
            compose.activity.startActivity(
                Intent(compose.activity, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra("quickAdd", "expense")
            )
        }
        compose.waitUntil(15000) {
            compose.onAllNodesWithTag("entry-amount").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("entry-amount").performTextInput("1000")
        compose.onNodeWithTag("entry-title").performTextInput("Native M-Pesa payment")
        compose.onNodeWithTag("toggle-Fee confirmed").performScrollTo().performClick()
        compose.onNodeWithTag("save-entry").performScrollTo().performClick()
        compose.waitUntil(15000) {
            compose.onAllNodesWithTag("entry-amount").fetchSemanticsNodes().isEmpty()
        }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(15000) {
            compose.onAllNodesWithTag("total-balance").fetchSemanticsNodes().isNotEmpty()
        }
        val saved = runBlocking { decodeBackup(AndroidBudgetStorage(app).read()!!) }
        Assert.assertEquals(1, saved.transactions.size)
        Assert.assertEquals(1300L, saved.transactions.single().fee!!.amount)
        Assert.assertTrue(saved.transactions.single().fee!!.confirmed)
        Assert.assertNull("Saved records must not return as unfinished drafts", saved.entryDraft)
        compose.runOnUiThread {
            compose.activity.startActivity(
                Intent(compose.activity, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra("openTools", "Fees")
            )
        }
        compose.waitUntil(15000) {
            compose.onAllNodesWithText("Confirmed fees").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Confirmed fees").assertIsDisplayed()
        compose.onNodeWithText("Individual charges").performScrollTo().assertIsDisplayed()
    }
}
