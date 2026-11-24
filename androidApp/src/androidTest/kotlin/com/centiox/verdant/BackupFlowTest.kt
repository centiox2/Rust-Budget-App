package com.centiox.verdant

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import android.net.Uri
import android.util.AtomicFile
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.*
import org.junit.rules.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupFlowTest {
    private val compose = createAndroidComposeRule<MainActivity>()
    @get:Rule
    val rules: TestRule =
        RuleChain.outerRule(
                object : ExternalResource() {
                    override fun before() {
                        val dir =
                            InstrumentationRegistry.getInstrumentation().targetContext.filesDir
                        AtomicFile(File(dir, "verdant-budget.vdb")).delete()
                        AtomicFile(File(dir, "verdant-budget.json")).delete()
                    }
                }
            )
            .around(compose)

    @Before
    fun interceptDocumentPickers() {
        Intents.init()
    }

    @After
    fun releaseInterception() {
        Intents.release()
    }

    @Test
    fun exportResetAndRestoreThroughNativeDocumentContracts() {
        val uri = Uri.parse("content://com.centiox.verdant.test.backups/backup.json")
        val response =
            ActivityResult(
                Activity.RESULT_OK,
                Intent()
                    .setData(uri)
                    .addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    ),
            )
        Intents.intending(hasAction(Intent.ACTION_CREATE_DOCUMENT)).respondWith(response)
        Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(response)
        compose.waitUntil(15000) {
            compose.onAllNodesWithTag("start").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("start").performScrollTo().performClick()
        compose.onNodeWithTag("profile-name").performTextInput("Backup owner")
        compose.onNodeWithTag("sample-data").performClick()
        compose.onNodeWithTag("finish-setup").performScrollTo().performClick()
        compose.waitUntil(15000) {
            compose.onAllNodesWithTag("total-balance").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Export backup").performScrollTo().performClick()
        var exported: BudgetData? = null
        compose.waitUntil(15000) {
            runCatching {
                    val text =
                        compose.activity.contentResolver
                            .openInputStream(uri)!!
                            .bufferedReader()
                            .use { it.readText() }
                    exported = decodeBackup(text)
                    exported!!.transactions.size == 9
                }
                .getOrDefault(false)
        }
        Assert.assertEquals("Backup owner", exported!!.name)
        Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Reset all data").performScrollTo().performSemanticsAction(
            SemanticsActions.OnClick
        ) {
            it()
        }
        compose.waitUntil(10000) {
            compose.onAllNodes(isDialog()).fetchSemanticsNodes().isNotEmpty()
        }
        compose
            .onNode(hasText("Reset all data") and hasAnyAncestor(isDialog()))
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.waitUntil(10000) {
            compose.onAllNodesWithTag("start").fetchSemanticsNodes().isNotEmpty()
        }
        compose
            .onNodeWithText("Restore an existing backup")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.waitUntil(15000) {
            compose.onAllNodesWithText("Restore your backup?").fetchSemanticsNodes().isNotEmpty()
        }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(15000) {
            compose.onAllNodesWithText("Restore your backup?").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Restore backup").performSemanticsAction(SemanticsActions.OnClick) {
            it()
        }
        compose.waitUntil(15000) {
            compose.onAllNodesWithTag("total-balance").fetchSemanticsNodes().isNotEmpty()
        }
        compose
            .onNodeWithTag("total-balance")
            .assertTextEquals(money(totals(exported!!, currentMonth()).balance, "USD"))
    }
}
