package com.centiox.verdant

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun App(
    repository: BudgetRepository,
    onExport: (String) -> Unit,
    onImport: ((String?) -> Unit) -> Unit,
    backHandler: @Composable (Boolean, () -> Unit) -> Unit = { _, _ -> },
    externalBackup: String? = null,
    onExternalBackupConsumed: () -> Unit = {},
    rootModifier: Modifier = Modifier,
) = VerdantTheme {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val data by repository.data.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var ready by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var welcome by rememberSaveable { mutableStateOf(true) }
    var pageName by rememberSaveable { mutableStateOf(Page.Home.name) }
    val page = Page.valueOf(pageName)
    var month by rememberSaveable { mutableStateOf(currentMonth()) }
    var sheet by rememberSaveable { mutableStateOf<String?>(null) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var initialType by rememberSaveable { mutableStateOf("expense") }
    var confirm by remember { mutableStateOf<String?>(null) }
    var pendingBackup by remember { mutableStateOf<String?>(null) }
    var backupCount by remember { mutableStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var transactionFilter by rememberSaveable { mutableStateOf("all") }
    var transactionCategory by rememberSaveable { mutableStateOf<String?>(null) }
    fun go(next: Page) {
        focus.clearFocus()
        keyboard?.hide()
        pageName = next.name
    }
    fun showTransactions(type: String = "all", category: String? = null) {
        query = ""
        transactionFilter = type
        transactionCategory = category
        go(Page.Transactions)
    }
    fun add(type: String) {
        focus.clearFocus()
        keyboard?.hide()
        if (page == Page.Home) month = currentMonth()
        editingId = null
        initialType = type
        sheet = "entry"
    }
    fun edit(entry: Entry) {
        focus.clearFocus()
        keyboard?.hide()
        editingId = entry.id
        initialType = entry.type
        sheet = "entry"
    }
    fun action(message: String, operation: suspend () -> Unit) {
        if (busy) return
        busy = true
        focus.clearFocus()
        keyboard?.hide()
        scope.launch {
            var result = message
            try {
                operation()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                result = e.message ?: "Something went wrong. Your saved data is unchanged."
            } finally {
                busy = false
            }
            if (result.isNotEmpty()) snackbar.showSnackbar(result)
        }
    }
    backHandler(
        (ready && !data.onboarded && !welcome) ||
            (ready && data.onboarded && page != Page.Home && sheet == null && confirm == null)
    ) {
        if (!data.onboarded) welcome = true else go(Page.Home)
    }
    fun dismissConfirm() {
        if (confirm == "restore") {
            pendingBackup = null
            onExternalBackupConsumed()
        }
        confirm = null
    }
    LaunchedEffect(externalBackup) {
        externalBackup?.let { raw ->
            try {
                backupCount =
                    withContext(Dispatchers.Default) { decodeBackup(raw).transactions.size }
                pendingBackup = raw
                confirm = "restore"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onExternalBackupConsumed()
                snackbar.showSnackbar(e.message ?: "Choose a valid Verdant backup.")
            }
        }
    }
    fun importBackup() {
        onImport { raw ->
            if (raw != null)
                scope.launch {
                    try {
                        backupCount =
                            withContext(Dispatchers.Default) { decodeBackup(raw).transactions.size }
                        pendingBackup = raw
                        confirm = "restore"
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        snackbar.showSnackbar(e.message ?: "Invalid backup.")
                    }
                }
        }
    }
    fun exportBackup() {
        scope.launch {
            try {
                onExport(withContext(Dispatchers.Default) { encodeBackup(data) })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                snackbar.showSnackbar(e.message ?: "Could not prepare the backup.")
            }
        }
    }
    LaunchedEffect(repository) {
        try {
            repository.load()
            ready = true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            loadError =
                "The saved ledger could not be opened. It has not been overwritten. Restore a backup, or start a new ledger."
        }
    }
    Box(
        rootModifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (ready && data.onboarded) Box(Modifier.matchParentSize().background(HomeBackground))
        else
            LiquidBackground(
                data.backgroundColor,
                data.animateBackground,
                Modifier.matchParentSize(),
            )
        Column(
            Modifier.widthIn(max = 620.dp)
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            when {
                loadError != null ->
                    Column(
                        Modifier.fillMaxSize().padding(28.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Outlined.AccountBalanceWallet,
                            null,
                            tint = Lime,
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(Modifier.height(22.dp))
                        Text("Let’s recover your ledger.", fontSize = 24.sp)
                        Spacer(Modifier.height(15.dp))
                        Text(loadError!!, fontSize = 13.sp, color = Muted)
                        Spacer(Modifier.height(22.dp))
                        PrimaryButton("Restore a backup", ::importBackup)
                        TextButton({ confirm = "reset" }, Modifier.fillMaxWidth()) {
                            Text("Start a new ledger")
                        }
                    }
                !ready ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            CircularProgressIndicator(
                                color = Lime,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(28.dp),
                            )
                            Text("Finding your balance…", fontSize = 12.sp, color = Muted)
                        }
                    }
                !data.onboarded ->
                    AnimatedContent(
                        welcome,
                        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(140)) },
                    ) { showWelcome ->
                        if (showWelcome)
                            WelcomeScreen(onStart = { welcome = false }, onRestore = ::importBackup)
                        else
                            SetupScreen(busy) { name, currency, sample ->
                                action("Your space is ready.") {
                                    repository.update {
                                        if (sample) sampleData(name, currency)
                                        else
                                            BudgetData(
                                                name = name,
                                                currency = currency,
                                                onboarded = true,
                                            )
                                    }
                                }
                            }
                    }
                else -> {
                    Box(Modifier.weight(1f)) {
                        AnimatedContent(
                            page,
                            transitionSpec = {
                                val direction =
                                    if (targetState.ordinal >= initialState.ordinal) 1 else -1
                                // Clear outgoing text before revealing the new page. Overlapping
                                // translucent text makes an otherwise subtle transition look muddy.
                                (fadeIn(tween(180, delayMillis = 90)) +
                                    slideInHorizontally(tween(240, delayMillis = 70)) {
                                        direction * 16
                                    }) togetherWith
                                    (fadeOut(tween(90)) +
                                        slideOutHorizontally(tween(140)) { -direction * 10 })
                            },
                            label = "page",
                        ) { selected ->
                            Column(Modifier.fillMaxSize()) {
                                if (selected != Page.Home) {
                                    PageHeader(selected, data, month, ::go, { month = it })
                                }
                                Box(Modifier.weight(1f)) {
                                    when (selected) {
                                        Page.Home ->
                                            HomeScreen(
                                                data,
                                                currentMonth(),
                                                { destination ->
                                                    if (
                                                        destination == Page.Transactions ||
                                                            destination == Page.Budgets
                                                    )
                                                        month = currentMonth()
                                                    if (destination == Page.Transactions)
                                                        showTransactions()
                                                    else go(destination)
                                                },
                                                ::add,
                                                {
                                                    month = currentMonth()
                                                    sheet = "budget"
                                                },
                                                ::edit,
                                            )
                                        Page.Transactions ->
                                            TransactionsScreen(
                                                data,
                                                month,
                                                { add("expense") },
                                                ::edit,
                                                query,
                                                transactionFilter,
                                                transactionCategory,
                                                { query = it },
                                                { transactionFilter = it },
                                                { transactionCategory = it },
                                            )
                                        Page.Budgets ->
                                            BudgetsScreen(
                                                data,
                                                month,
                                                { sheet = "budget" },
                                                if (
                                                    data.budgets[month].isNullOrEmpty() &&
                                                        !data.budgets[shiftMonth(month, -1)]
                                                            .isNullOrEmpty()
                                                ) {
                                                    {
                                                        val targetMonth = month
                                                        action("Budget limits copied.") {
                                                            repository.update {
                                                                it.withPreviousMonthLimits(
                                                                    targetMonth
                                                                )
                                                            }
                                                        }
                                                    }
                                                } else null,
                                            )
                                        Page.Analytics ->
                                            AnalyticsScreen(
                                                data,
                                                month,
                                                ::edit,
                                                { showTransactions("expense") },
                                                { showTransactions("expense", it) },
                                            )
                                        Page.Settings ->
                                            SettingsScreen(
                                                data,
                                                busy,
                                                { name, currency ->
                                                    action("Preferences saved.") {
                                                        repository.update {
                                                            it.copy(
                                                                name = name,
                                                                currency = currency,
                                                            )
                                                        }
                                                    }
                                                },
                                                ::exportBackup,
                                                ::importBackup,
                                                { confirm = "reset" },
                                                { hex, moving ->
                                                    action("") {
                                                        repository.update {
                                                            it.copy(
                                                                backgroundColor = hex,
                                                                animateBackground = moving,
                                                            )
                                                        }
                                                    }
                                                },
                                            )
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0xFF1B2117), thickness = .5.dp)
                    BottomNavigation(page, ::go)
                }
            }
        }
        SnackbarHost(
            snackbar,
            Modifier.align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 90.dp)
                .widthIn(max = 560.dp),
        )
    }
    val editing = data.transactions.find { it.id == editingId }
    if (ready && data.onboarded && sheet == "entry")
        EntrySheet(
            editing,
            initialType,
            month,
            { sheet = null },
            { entry ->
                repository.update { current -> current.withEntry(entry) }
                scope.launch {
                    snackbar.showSnackbar(
                        if (editingId == null) "Transaction added." else "Transaction updated."
                    )
                }
            },
            { confirm = "delete" },
        )
    if (ready && data.onboarded && sheet == "budget")
        BudgetSheet(
            data.budgets[month].orEmpty(),
            month,
            { sheet = null },
            { limits ->
                repository.update { it.copy(budgets = it.budgets + (month to limits)) }
                scope.launch { snackbar.showSnackbar("Budgets saved.") }
            },
        )
    if (confirm != null)
        AlertDialog(
            onDismissRequest = { if (!busy) dismissConfirm() },
            title = {
                Text(
                    when (confirm) {
                        "delete" -> "Delete this transaction?"
                        "restore" -> "Restore your backup?"
                        else -> "Start fresh?"
                    }
                )
            },
            text = {
                Text(
                    when (confirm) {
                        "delete" -> "This entry will be permanently removed."
                        "restore" ->
                            "This replaces your current ledger with $backupCount backed-up transactions. Export your current data first if you want to keep it."
                        else ->
                            "All transactions, budgets, and preferences will be removed. Export a backup first if you want to keep them."
                    }
                )
            },
            dismissButton = {
                TextButton({ dismissConfirm() }, enabled = !busy) { Text("Cancel") }
            },
            confirmButton = {
                TextButton(
                    {
                        val selected = confirm
                        action(
                            when (selected) {
                                "delete" -> "Transaction deleted."
                                "restore" -> "Backup restored."
                                else -> "Your ledger has been reset."
                            }
                        ) {
                            when (selected) {
                                "delete" ->
                                    repository.update {
                                        it.copy(
                                            transactions =
                                                it.transactions.filterNot { entry ->
                                                    entry.id == editingId
                                                }
                                        )
                                    }
                                "restore" ->
                                    repository.restore(
                                        pendingBackup ?: error("Select a backup again.")
                                    )
                                else -> repository.reset()
                            }
                            if (selected == "restore") {
                                pendingBackup = null
                                onExternalBackupConsumed()
                                pageName = Page.Home.name
                                month = currentMonth()
                                query = ""
                                transactionFilter = "all"
                                transactionCategory = null
                            }
                            confirm = null
                            sheet = null
                            loadError = null
                            ready = true
                            if (selected == "reset") {
                                welcome = true
                                pageName = Page.Home.name
                                month = currentMonth()
                                query = ""
                                transactionFilter = "all"
                                transactionCategory = null
                            }
                        }
                    },
                    enabled = !busy,
                ) {
                    Text(
                        if (busy) "Please wait…"
                        else
                            when (confirm) {
                                "delete" -> "Delete"
                                "restore" -> "Restore backup"
                                else -> "Reset all data"
                            }
                    )
                }
            },
        )
}
