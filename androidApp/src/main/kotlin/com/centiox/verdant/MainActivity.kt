package com.centiox.verdant

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class BudgetViewModel(private val app: Application) : AndroidViewModel(app) {
    val repository = BudgetRepository(AndroidBudgetStorage(app))
    private val stagedExport = AndroidBudgetStorage(app, "verdant-export.vdb", false)
    private val imported = MutableStateFlow<String?>(null)
    val importedBackup = imported.asStateFlow()
    var unlocked by mutableStateOf(false)

    fun consumeImport() {
        imported.value = null
    }

    fun saveDraft(draft: EntryDraft?) {
        viewModelScope.launch {
            try {
                repository.update { it.acceptDraft(draft) }
            } catch (_: Exception) {
                Toast.makeText(app, "Draft could not be saved", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun stageExport(payload: String, ready: () -> Unit) =
        viewModelScope.launch {
            try {
                stagedExport.write(payload)
                ready()
            } catch (_: Exception) {
                Toast.makeText(app, "Could not prepare export", Toast.LENGTH_LONG).show()
            }
        }

    fun cancelExport() {
        viewModelScope.launch { stagedExport.delete() }
    }

    fun exportTo(uri: Uri) =
        viewModelScope.launch {
            try {
                // Persisted encrypted staging survives process death while the document picker is
                // open.
                val payload =
                    stagedExport.read() ?: error("Choose Export again; this request expired.")
                withContext(Dispatchers.IO) {
                    app.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                        it.write(payload)
                    } ?: error("Could not open destination.")
                }
                if (payload.trimStart().startsWith('{'))
                    repository.update { it.copy(lastBackupDate = today().toString()) }
                stagedExport.delete()
                Toast.makeText(app, "Export saved", Toast.LENGTH_SHORT).show()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                Toast.makeText(app, "Export failed. Your ledger is unchanged.", Toast.LENGTH_LONG)
                    .show()
            }
        }

    fun importFrom(uri: Uri) =
        viewModelScope.launch {
            try {
                val text =
                    withContext(Dispatchers.IO) {
                        app.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                            val result = StringBuilder()
                            val buffer = CharArray(8192)
                            while (true) {
                                val n = reader.read(buffer)
                                if (n < 0) break
                                require(result.length + n <= 8_000_000) { "File is too large." }
                                result.append(buffer, 0, n)
                            }
                            result.toString()
                        } ?: error("Could not read this file.")
                    }
                imported.value = text
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Toast.makeText(
                        app,
                        e.message ?: "Import failed. Your ledger is unchanged.",
                        Toast.LENGTH_LONG,
                    )
                    .show()
            }
        }
}

class MainActivity : FragmentActivity() {
    private val model by viewModels<BudgetViewModel>()
    private var quickAdd by mutableStateOf<String?>(null)
    private var tools by mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readShortcuts(intent)
    }

    private fun readShortcuts(intent: Intent) {
        quickAdd = intent.getStringExtra("quickAdd")?.takeIf { it in listOf("income", "expense") }
        tools =
            intent.getStringExtra("openTools")?.takeIf {
                it in
                    listOf("Alerts", "Accounts", "Categories", "Payments", "Goals", "Fees", "Tools")
            }
    }

    private fun authenticate(result: (Boolean) -> Unit) {
        val mask =
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if (
            BiometricManager.from(this).canAuthenticate(mask) != BiometricManager.BIOMETRIC_SUCCESS
        ) {
            Toast.makeText(
                    this,
                    "Set up a device screen lock or biometric authentication in Android settings first.",
                    Toast.LENGTH_LONG,
                )
                .show()
            result(false)
            return
        }
        val prompt =
            BiometricPrompt(
                this,
                ContextCompat.getMainExecutor(this),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(
                        value: BiometricPrompt.AuthenticationResult
                    ) {
                        model.unlocked = true
                        result(true)
                    }

                    override fun onAuthenticationError(code: Int, message: CharSequence) {
                        result(false)
                    }
                },
            )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Verdant")
                .setSubtitle("Use your device biometric or screen-lock credential")
                .setAllowedAuthenticators(mask)
                .build()
        )
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) model.unlocked = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readShortcuts(intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
        )
        setContent {
            val imported by model.importedBackup.collectAsState()
            val data by model.repository.data.collectAsState()
            var initiallyLoaded by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                runCatching { model.repository.load() }
                initiallyLoaded = true
            }
            val export =
                rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument("application/octet-stream")
                ) { uri ->
                    if (uri != null) model.exportTo(uri) else model.cancelExport()
                }
            val restore =
                rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    if (uri != null) model.importFrom(uri)
                }
            val permission =
                rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
            val platform = remember {
                PlatformServices(
                    supportsLock = true,
                    authenticate = ::authenticate,
                    update = { next ->
                        ReminderWorker.schedule(applicationContext, next)
                        getSharedPreferences("verdant-native", MODE_PRIVATE)
                            .edit()
                            .putString("accent", next.backgroundColor)
                            .apply()
                        QuickEntryWidget.refresh(applicationContext)
                        if (
                            next.reminders &&
                                android.os.Build.VERSION.SDK_INT >= 33 &&
                                ContextCompat.checkSelfPermission(
                                    this,
                                    android.Manifest.permission.POST_NOTIFICATIONS,
                                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            val prefs = getSharedPreferences("verdant-native", MODE_PRIVATE)
                            if (!prefs.getBoolean("askedNotifications", false)) {
                                prefs.edit().putBoolean("askedNotifications", true).apply()
                                permission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    },
                    haptic = {
                        window.decorView.performHapticFeedback(
                            android.view.HapticFeedbackConstants.KEYBOARD_TAP
                        )
                    },
                    pinWidget = { QuickEntryWidget.pin(this) },
                    saveDraft = model::saveDraft,
                    openNotificationSettings = {
                        startActivity(
                            Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)
                        )
                    },
                )
            }
            val locked = data.appLock && !model.unlocked
            SideEffect {
                if (data.appLock) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
            val pages = rememberSaveableStateHolder()
            if (initiallyLoaded && !locked)
                pages.SaveableStateProvider("unlocked-ledger") {
                    App(
                        model.repository,
                        onExport = { payload ->
                            model.stageExport(payload) {
                                export.launch(
                                    "verdant-${today()}.${if (payload.trimStart().startsWith('{')) "json" else "csv"}"
                                )
                            }
                        },
                        onImport = {
                            restore.launch(
                                arrayOf(
                                    "application/json",
                                    "text/*",
                                    "application/octet-stream",
                                    "application/vnd.ms-excel",
                                )
                            )
                        },
                        backHandler = { enabled, action -> BackHandler(enabled, action) },
                        externalBackup = imported,
                        onExternalBackupConsumed = model::consumeImport,
                        rootModifier = Modifier.semantics { testTagsAsResourceId = true },
                        platform = platform,
                        quickAddType = quickAdd,
                        onQuickAddConsumed = {
                            quickAdd = null
                            intent.removeExtra("quickAdd")
                        },
                        requestedTools = tools,
                        onRequestedToolsConsumed = {
                            tools = null
                            intent.removeExtra("openTools")
                        },
                    )
                }
            else
                VerdantTheme(data) {
                    Column(
                        Modifier.fillMaxSize()
                            .background(HomeBackground)
                            .safeDrawingPadding()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Outlined.Spa, null, Modifier.size(48.dp), Lime)
                        Spacer(Modifier.height(24.dp))
                        Text(
                            if (locked) "Your ledger is locked." else "Opening your ledger…",
                            fontSize = 24.sp,
                        )
                        if (locked) {
                            Spacer(Modifier.height(24.dp))
                            PrimaryButton("Unlock", { authenticate {} })
                            BackHandler { finish() }
                        }
                    }
                }
        }
    }
}
