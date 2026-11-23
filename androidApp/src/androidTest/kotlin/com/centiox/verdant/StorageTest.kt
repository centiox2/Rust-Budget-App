package com.centiox.verdant

import android.app.Application
import android.util.AtomicFile
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StorageTest {
    private val app
        get() =
            InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
                as Application

    @Before
    fun clean() {
        AtomicFile(File(app.filesDir, "verdant-budget.vdb")).delete()
        AtomicFile(File(app.filesDir, "verdant-budget.json")).delete()
    }

    @Test
    fun encryptedRoundTripAndTamperDetection() = runBlocking {
        val store = AndroidBudgetStorage(app)
        val json = encodeBackup(sampleData("Private ledger", "USD"))
        store.write(json)
        val disk = File(app.filesDir, "verdant-budget.vdb")
        val bytes = disk.readBytes()
        Assert.assertFalse(String(bytes, Charsets.UTF_8).contains("Private ledger"))
        Assert.assertEquals(json, store.read())
        bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte()
        disk.writeBytes(bytes)
        var rejected = false
        try {
            store.read()
        } catch (_: Exception) {
            rejected = true
        }
        Assert.assertTrue("Modified encrypted data must be rejected", rejected)
        Assert.assertArrayEquals(
            "Failed reads must not overwrite the file",
            bytes,
            disk.readBytes(),
        )
    }

    @Test
    fun legacyJsonMigratesOnlyAfterSuccessfulEncryption() = runBlocking {
        val legacy = File(app.filesDir, "verdant-budget.json")
        val json = encodeBackup(sampleData("Migration", "KES"))
        legacy.writeText(json)
        val store = AndroidBudgetStorage(app)
        Assert.assertEquals(decodeBackup(json), decodeBackup(store.read()!!))
        Assert.assertTrue(File(app.filesDir, "verdant-budget.vdb").exists())
        Assert.assertFalse(legacy.exists())
        Assert.assertEquals(decodeBackup(json), decodeBackup(store.read()!!))
    }

    @Test
    fun interruptedAtomicWriteRecoversPreviousLedger() = runBlocking {
        val store = AndroidBudgetStorage(app)
        val json = encodeBackup(sampleData("Recovered", "USD"))
        store.write(json)
        val target = File(app.filesDir, "verdant-budget.vdb")
        val backup = File(app.filesDir, "verdant-budget.vdb.bak")
        Assert.assertTrue(target.renameTo(backup))
        Assert.assertEquals(json, store.read())
    }
}
