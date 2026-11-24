package com.centiox.verdant

import android.app.Application
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Authenticated encryption at rest; the encryption key never leaves Android Keystore. */
class AndroidBudgetStorage(
    app: Application,
    fileName: String = "verdant-budget.vdb",
    private val migrateLegacy: Boolean = true,
) : BudgetStorage {
    private val file = AtomicFile(File(app.filesDir, fileName))
    private val legacy = AtomicFile(File(app.filesDir, "verdant-budget.json"))
    private val magic = byteArrayOf(0x56, 0x44, 0x42, 0x32) // VDB2 storage envelope

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("verdant.ledger.v2", null) as? SecretKey)?.let {
            return it
        }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            .apply {
                init(
                    KeyGenParameterSpec.Builder(
                            "verdant.ledger.v2",
                            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                        )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .setRandomizedEncryptionRequired(true)
                        .build()
                )
            }
            .generateKey()
    }

    override suspend fun read(): String? =
        withContext(Dispatchers.IO) {
            if (file.baseFile.exists() || File(file.baseFile.path + ".bak").exists()) {
                require(file.baseFile.length() <= 20_000_000) { "Saved ledger is too large." }
                val bytes = file.openRead().use { it.readBytes() }
                require(bytes.size > 32 && bytes.take(4).toByteArray().contentEquals(magic)) {
                    "Unsupported ledger storage format. Restore a backup to recover."
                }
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(
                    Cipher.DECRYPT_MODE,
                    key(),
                    GCMParameterSpec(128, bytes.copyOfRange(4, 16)),
                )
                cipher.updateAAD(magic)
                String(cipher.doFinal(bytes.copyOfRange(16, bytes.size)), Charsets.UTF_8)
            } else if (
                migrateLegacy &&
                    (legacy.baseFile.exists() || File(legacy.baseFile.path + ".bak").exists())
            ) {
                require(legacy.baseFile.length() <= 20_000_000) { "Saved ledger is too large." }
                val json = legacy.openRead().bufferedReader().use { it.readText() }
                // Validate before migration; retain the old file if the encrypted write fails.
                val validated = encodeBackup(decodeBackup(json))
                write(validated)
                legacy.delete()
                validated
            } else null
        }

    override suspend fun write(json: String) =
        withContext(Dispatchers.IO) {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key())
            cipher.updateAAD(magic)
            val encrypted = magic + cipher.iv + cipher.doFinal(json.toByteArray(Charsets.UTF_8))
            val stream = file.startWrite()
            try {
                stream.write(encrypted)
                file.finishWrite(stream)
            } catch (e: Exception) {
                file.failWrite(stream)
                throw e
            }
        }

    suspend fun delete() = withContext(Dispatchers.IO) { file.delete() }
}
