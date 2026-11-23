package com.centiox.verdant

import android.app.Application
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BudgetViewModel(private val app: Application) : AndroidViewModel(app) {
    val repository = BudgetRepository(AndroidBudgetStorage(app))
    private val imported = MutableStateFlow<String?>(null)
    val importedBackup = imported.asStateFlow()

    fun consumeImport() {
        imported.value = null
    }

    fun exportTo(uri: Uri) =
        viewModelScope.launch {
            try {
                // Do not depend on a transient Activity field when a document picker returns.
                val json = repository.snapshot()
                withContext(Dispatchers.IO) {
                    app.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                        it.write(json)
                    } ?: error("Could not open the backup destination.")
                }
                Toast.makeText(app, "Backup saved", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(
                        app,
                        "Could not export the backup. Your ledger is unchanged.",
                        Toast.LENGTH_LONG,
                    )
                    .show()
            }
        }

    fun importFrom(uri: Uri) =
        viewModelScope.launch {
            try {
                val json =
                    withContext(Dispatchers.IO) {
                        app.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                            val result = StringBuilder()
                            val buffer = CharArray(8192)
                            while (true) {
                                val read = reader.read(buffer)
                                if (read < 0) break
                                require(result.length + read <= 5_000_000) {
                                    "Backup is too large."
                                }
                                result.append(buffer, 0, read)
                            }
                            result.toString()
                        } ?: error("Could not read the backup.")
                    }
                imported.value = json
            } catch (e: Exception) {
                Toast.makeText(
                        app,
                        if (e is IllegalArgumentException) e.message
                        else "Could not read this backup. Your ledger is unchanged.",
                        Toast.LENGTH_LONG,
                    )
                    .show()
            }
        }
}

class MainActivity : ComponentActivity() {
    private val model by viewModels<BudgetViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
        )
        setContent {
            val imported by model.importedBackup.collectAsState()
            val export =
                rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument("application/json")
                ) { uri ->
                    if (uri != null) model.exportTo(uri)
                }
            val restore =
                rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    if (uri != null) model.importFrom(uri)
                }
            App(
                model.repository,
                onExport = { export.launch("verdant-${today()}.json") },
                onImport = {
                    restore.launch(
                        arrayOf("application/json", "text/plain", "application/octet-stream")
                    )
                },
                backHandler = { enabled, action ->
                    BackHandler(enabled = enabled, onBack = action)
                },
                externalBackup = imported,
                onExternalBackupConsumed = model::consumeImport,
            )
        }
    }
}
