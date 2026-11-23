package com.centiox.verdant

import android.app.Application
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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BudgetViewModel(app: Application) : AndroidViewModel(app) {
    val repository = BudgetRepository(AndroidBudgetStorage(app))
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
            var pendingExport by remember { mutableStateOf("") }
            var importResult by remember { mutableStateOf<((String?) -> Unit)?>(null) }
            val export =
                rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument("application/json")
                ) { uri ->
                    if (uri != null)
                        lifecycleScope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                                        it.write(pendingExport)
                                    } ?: error("Could not open this file.")
                                }
                                Toast.makeText(
                                        this@MainActivity,
                                        "Backup saved",
                                        Toast.LENGTH_SHORT,
                                    )
                                    .show()
                            } catch (e: Exception) {
                                Toast.makeText(
                                        this@MainActivity,
                                        "Export failed: ${e.message}",
                                        Toast.LENGTH_LONG,
                                    )
                                    .show()
                            }
                        }
                }
            val restore =
                rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    if (uri != null)
                        lifecycleScope.launch {
                            try {
                                val json =
                                    withContext(Dispatchers.IO) {
                                        contentResolver
                                            .openInputStream(uri)
                                            ?.bufferedReader()
                                            ?.use { reader ->
                                                val text = reader.readText()
                                                require(text.length <= 5_000_000) {
                                                    "Backup is too large."
                                                }
                                                text
                                            } ?: error("Could not read this file.")
                                    }
                                importResult?.invoke(json)
                            } catch (e: Exception) {
                                Toast.makeText(
                                        this@MainActivity,
                                        "Restore failed: ${e.message}",
                                        Toast.LENGTH_LONG,
                                    )
                                    .show()
                            }
                        }
                    else importResult?.invoke(null)
                }
            App(
                model.repository,
                onExport = { json ->
                    pendingExport = json
                    export.launch("verdant-${today()}.json")
                },
                onImport = { callback ->
                    importResult = callback
                    restore.launch(
                        arrayOf("application/json", "text/plain", "application/octet-stream")
                    )
                },
                backHandler = { enabled, action -> BackHandler(enabled = enabled, onBack = action) },
            )
        }
    }
}
