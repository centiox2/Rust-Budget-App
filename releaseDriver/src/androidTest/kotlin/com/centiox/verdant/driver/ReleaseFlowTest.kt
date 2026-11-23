package com.centiox.verdant.driver

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import java.math.BigDecimal
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith

/** Controls the installed production APK from another process, without linking app code. */
@RunWith(AndroidJUnit4::class)
class ReleaseFlowTest {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val app = "com.centiox.verdant"

    @get:Rule
    val failureEvidence =
        object : TestWatcher() {
            override fun failed(error: Throwable, description: Description) {
                screenshot("android-release-failure.png")
                val xml = java.io.ByteArrayOutputStream()
                device.dumpWindowHierarchy(xml)
                xml.toString("UTF-8").chunked(3500).forEach {
                    android.util.Log.e("VerdantReleaseEvidence", it)
                }
            }
        }

    private fun node(selector: BySelector): UiObject2 =
        device.wait(Until.findObject(selector), 20_000)
            ?: error("Missing $selector. Foreground: ${device.currentPackageName}")

    private fun click(selector: BySelector) {
        node(selector).click()
        device.waitForIdle()
    }

    private fun scrollClick(selector: BySelector) {
        repeat(7) {
            val found = device.findObject(selector)
            if (found != null && found.visibleBounds.height() > 10) {
                found.click()
                device.waitForIdle()
                return
            }
            device.swipe(
                device.displayWidth / 2,
                device.displayHeight * 3 / 4,
                device.displayWidth / 2,
                device.displayHeight / 3,
                30,
            )
            device.waitForIdle()
        }
        error("Could not scroll to $selector")
    }

    private fun launch() {
        val result = device.executeShellCommand("am start -W -n $app/.MainActivity")
        assertTrue(
            "Launch: $result; foreground: ${device.currentPackageName}",
            device.wait(Until.hasObject(By.pkg(app)), 20_000),
        )
    }

    private fun hideKeyboard() {
        device.waitForIdle()
        if (
            InstrumentationRegistry.getInstrumentation().uiAutomation.windows.any {
                it.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD
            }
        ) {
            device.pressBack()
            device.waitForIdle()
        }
    }

    private fun screenshot(name: String) {
        device.waitForIdle()
        device.executeShellCommand("mkdir -p /sdcard/Download/verdant-test-screenshots")
        device.executeShellCommand("screencap -p /sdcard/Download/verdant-test-screenshots/$name")
    }

    private fun amount(text: String) = BigDecimal(text.replace("$", "").replace(",", ""))

    @Test
    fun signedReleaseLedgerSurvivesProcessRestart() {
        assertTrue(device.executeShellCommand("pm clear $app").contains("Success"))
        launch()
        node(By.text("Start Now"))
        screenshot("android-release-welcome.png")
        click(By.text("Start Now"))
        node(By.clazz("android.widget.EditText")).text = "Alex Morgan"
        hideKeyboard()
        click(By.text("Explore with sample data"))
        scrollClick(By.text("Let’s get started"))
        val balance = amount(node(By.res("total-balance")).text)
        screenshot("android-release-home.png")
        click(By.desc("Add transaction"))
        val fields = device.wait(Until.findObjects(By.clazz("android.widget.EditText")), 20_000)
        assertTrue("Amount and title fields are accessible", fields.size >= 2)
        fields[0].text = "49.99"
        fields[1].text = "Release verification purchase"
        hideKeyboard()
        scrollClick(By.text("Add transaction"))
        assertTrue(device.wait(Until.gone(By.text("A little money moment.")), 20_000))
        assertEquals(
            balance.subtract(BigDecimal("49.99")),
            amount(node(By.res("total-balance")).text),
        )

        device.executeShellCommand("am force-stop $app")
        launch()
        assertEquals(
            balance.subtract(BigDecimal("49.99")),
            amount(node(By.res("total-balance")).text),
        )
        click(By.desc("Analytics"))
        node(By.text("Spending Overview"))
        screenshot("android-release-analytics.png")
        click(By.desc("Budgets"))
        node(By.text("Your categories"))
        screenshot("android-release-budgets.png")
        click(By.desc("Transactions"))
        node(By.res("transaction-search")).text = "Release verification purchase"
        hideKeyboard()
        // The result row is a separate node from the search input.
        val purchase =
            node(By.textContains("Release verification purchase").clazz("android.widget.TextView"))
        purchase.click()
        node(By.text("Edit transaction"))
        hideKeyboard()
        scrollClick(By.text("Delete transaction"))
        click(By.text("Delete"))
        assertTrue(device.wait(Until.gone(By.text("Edit transaction")), 20_000))
        click(By.desc("Home"))
        assertEquals(balance, amount(node(By.res("total-balance")).text))
    }
}
