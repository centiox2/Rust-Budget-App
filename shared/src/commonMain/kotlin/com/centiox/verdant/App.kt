package com.centiox.verdant

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.clearAndSetSemantics
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
    platform: PlatformServices = PlatformServices(),
    quickAddType: String? = null,
    onQuickAddConsumed: () -> Unit = {},
    requestedTools: String? = null,
    onRequestedToolsConsumed: () -> Unit = {},
) {
    val data by repository.data.collectAsState()
    VerdantTheme(data, platform.haptic) {
        val reduced = data.reducedMotion
        val focus = LocalFocusManager.current
        val keyboard = LocalSoftwareKeyboardController.current
        val scope = rememberCoroutineScope { kotlinx.coroutines.Dispatchers.Main.immediate }
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
        var workspace by rememberSaveable { mutableStateOf<String?>(null) }
        var seed by remember { mutableStateOf<Entry?>(null) }
        var pendingNewType by rememberSaveable { mutableStateOf<String?>(null) }
        var budgetKey by rememberSaveable { mutableStateOf(currentMonth()) }
        var importKind by rememberSaveable { mutableStateOf("backup") }
        var portability by remember { mutableStateOf<String?>(null) }
        var importRaw by remember { mutableStateOf<String?>(null) }
        val pageStates = rememberSaveableStateHolder()
        LaunchedEffect(data) { platform.update(data) }
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
            if (data.entryDraft != null) {
                pendingNewType = type
                return
            }
            editingId = null
            seed = null
            initialType = type
            sheet = "entry"
        }
        fun edit(entry: Entry) {
            focus.clearFocus()
            keyboard?.hide()
            seed = null
            editingId = entry.id
            initialType = entry.type
            sheet = "entry"
        }
        fun action(message: String, undo: (() -> Unit)? = null, operation: suspend () -> Unit) {
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
                if (result.isNotEmpty())
                    snackbar.showSnackbar(result, if (undo != null) "Undo" else null, undo)
            }
        }
        backHandler(
            workspace != null ||
                (ready && !data.onboarded && !welcome) ||
                (ready && data.onboarded && page != Page.Home && sheet == null && confirm == null)
        ) {
            if (workspace != null) workspace = null
            else if (!data.onboarded) welcome = true else go(Page.Home)
        }
        fun dismissConfirm() {
            if (confirm == "restore") {
                pendingBackup = null
                onExternalBackupConsumed()
            }
            confirm = null
        }
        suspend fun prepareImport(raw: String) {
            if (withContext(Dispatchers.Default) { isProtectedBackup(raw) }) {
                importRaw = raw
                portability = "decrypt"
                return
            }
            if (importKind == "csv") {
                importRaw = raw
                portability = "csv"
                return
            }
            val parsed = withContext(Dispatchers.Default) { decodeBackup(raw) }
            backupCount = parsed.transactions.size
            pendingBackup = raw
            confirm = "restore"
        }
        LaunchedEffect(externalBackup) {
            externalBackup?.let { raw ->
                try {
                    prepareImport(raw)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    onExternalBackupConsumed()
                    scope.launch { snackbar.showSnackbar(e.message ?: "Choose a valid backup.") }
                }
                // Keep the platform payload until confirmation/cancellation so activity
                // recreation can rebuild the preview without putting a ledger in saved state.
            }
        }
        fun importBackup(kind: String = "backup") {
            importKind = kind
            onImport { raw ->
                if (raw != null)
                    scope.launch {
                        try {
                            prepareImport(raw)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            snackbar.showSnackbar(e.message ?: "Could not read this file.")
                        }
                    }
            }
        }
        LaunchedEffect(quickAddType, ready, data.onboarded) {
            if (quickAddType != null && ready && data.onboarded) {
                add(quickAddType)
                onQuickAddConsumed()
            }
        }
        LaunchedEffect(requestedTools, ready, data.onboarded) {
            if (requestedTools != null && ready && data.onboarded) {
                workspace = requestedTools
                onRequestedToolsConsumed()
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
                    .then(if (workspace != null) Modifier.clearAndSetSemantics {} else Modifier)
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
                            PrimaryButton("Restore a backup", { importBackup() })
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
                            transitionSpec = {
                                fadeIn(tween(if (reduced) 0 else 220)) togetherWith
                                    fadeOut(tween(if (reduced) 0 else 140))
                            },
                        ) { showWelcome ->
                            if (showWelcome)
                                WelcomeScreen(
                                    onStart = { welcome = false },
                                    onRestore = { importBackup() },
                                )
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
                                    // Clear outgoing text before revealing the new page.
                                    // Overlapping
                                    // translucent text makes an otherwise subtle transition look
                                    // muddy.
                                    (fadeIn(
                                        tween(
                                            if (reduced) 0 else 180,
                                            delayMillis = if (reduced) 0 else 90,
                                        )
                                    ) +
                                        slideInHorizontally(
                                            tween(
                                                if (reduced) 0 else 240,
                                                delayMillis = if (reduced) 0 else 70,
                                            )
                                        ) {
                                            direction * 16
                                        }) togetherWith
                                        (fadeOut(tween(if (reduced) 0 else 90)) +
                                            slideOutHorizontally(tween(if (reduced) 0 else 140)) {
                                                -direction * 10
                                            })
                                },
                                label = "page",
                            ) { selected ->
                                pageStates.SaveableStateProvider(selected.name) {
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
                                                            budgetKey =
                                                                data
                                                                    .period(
                                                                        kotlinx.datetime.LocalDate
                                                                            .parse(
                                                                                today().toString()
                                                                            )
                                                                    )
                                                                    .key
                                                            sheet = "budget"
                                                        },
                                                        ::edit,
                                                        { workspace = "Alerts" },
                                                        {
                                                            scope.launch {
                                                                repository.update {
                                                                    it.copy(
                                                                        hideBalances =
                                                                            !it.hideBalances
                                                                    )
                                                                }
                                                            }
                                                        },
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
                                                        { ids, category ->
                                                            repository.update { d ->
                                                                d.copy(
                                                                    transactions =
                                                                        d.transactions.map {
                                                                            if (
                                                                                it.id in ids &&
                                                                                    it.type !=
                                                                                        "transfer"
                                                                            )
                                                                                it.copy(
                                                                                    category =
                                                                                        category,
                                                                                    splits =
                                                                                        emptyList(),
                                                                                )
                                                                            else it
                                                                        }
                                                                )
                                                            }
                                                        },
                                                        { ids ->
                                                            val snapshot =
                                                                data.transactions.filter {
                                                                    it.id in ids
                                                                }
                                                            action(
                                                                "Transactions deleted.",
                                                                undo = {
                                                                    scope.launch {
                                                                        try {
                                                                            repository.update {
                                                                                it.restoreEntries(
                                                                                    snapshot
                                                                                )
                                                                            }
                                                                        } catch (e: Exception) {
                                                                            snackbar.showSnackbar(
                                                                                e.message
                                                                                    ?: "Could not restore this batch."
                                                                            )
                                                                        }
                                                                    }
                                                                },
                                                            ) {
                                                                repository.update {
                                                                    it.deleteEntries(ids)
                                                                }
                                                            }
                                                        },
                                                    )
                                                Page.Budgets ->
                                                    BudgetsScreen(
                                                        data,
                                                        month,
                                                        { key ->
                                                            budgetKey = key
                                                            sheet = "budget"
                                                        },
                                                        if (
                                                            data.budgetCycle == "monthly" &&
                                                                data.budgets[month]
                                                                    .isNullOrEmpty() &&
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
                                                        {
                                                            if (it == FEE_CATEGORY)
                                                                workspace = "Fees"
                                                            else showTransactions("expense", it)
                                                        },
                                                        { workspace = "Fees" },
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
                                                        { importBackup() },
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
                                                        { workspace = it },
                                                    )
                                            }
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
            if (workspace != null && ready && data.onboarded)
                Box(
                    Modifier.fillMaxSize()
                        .background(HomeBackground)
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Box(Modifier.widthIn(max = 620.dp).fillMaxSize()) {
                        PlanningScreen(
                            workspace!!,
                            data,
                            repository::update,
                            { workspace = null },
                            { entry ->
                                workspace = null
                                editingId = null
                                seed = entry
                                initialType = entry.type
                                sheet = "entry"
                            },
                            onExport,
                            { importBackup("csv") },
                            { portability = "export" },
                            { importBackup() },
                            platform,
                        )
                    }
                }
            SnackbarHost(
                snackbar,
                Modifier.align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 90.dp)
                    .widthIn(max = 560.dp),
            )
        }
        val editing = data.transactions.find { it.id == editingId } ?: seed
        if (ready && data.onboarded && sheet == "entry")
            EntrySheet(
                editing,
                initialType,
                month,
                { sheet = null },
                { entry ->
                    repository.update { current ->
                        current
                            .withEntry(entry)
                            .copy(
                                entryDraft = null,
                                lastAccountId = entry.accountId,
                                lastExpenseCategory =
                                    if (entry.type == "expense") entry.category
                                    else current.lastExpenseCategory,
                                lastIncomeCategory =
                                    if (entry.type == "income") entry.category
                                    else current.lastIncomeCategory,
                                lastMpesaFeeKind = entry.fee?.kind ?: current.lastMpesaFeeKind,
                            )
                    }
                    scope.launch {
                        snackbar.showSnackbar(
                            if (editingId == null) "Transaction added." else "Transaction updated."
                        )
                    }
                },
                { confirm = "delete" },
                isNew = editingId == null,
                draft = data.entryDraft,
                onDraft = { draft ->
                    platform.saveDraft?.invoke(draft)
                        ?: run { scope.launch { repository.update { it.acceptDraft(draft) } } }
                },
                onDuplicate = { old ->
                    editingId = null
                    seed =
                        old.copy(
                            id = newId(),
                            date = today().toString(),
                            cleared = false,
                            fee = old.fee?.copy(confirmed = false),
                        )
                    initialType = old.type
                },
                onTemplate = { old ->
                    action("Template saved.") {
                        repository.update { d ->
                            val template =
                                EntryTemplate(
                                    d.templates.firstOrNull { it.name == old.title }?.id ?: newId(),
                                    old.title,
                                    old,
                                )
                            d.copy(
                                templates =
                                    d.templates.filterNot { it.id == template.id } + template
                            )
                        }
                    }
                },
            )
        if (ready && data.onboarded && sheet == "budget")
            BudgetSheet(
                data.budgets[budgetKey].orEmpty(),
                budgetKey,
                { sheet = null },
                { limits ->
                    repository.update { it.copy(budgets = it.budgets + (budgetKey to limits)) }
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
                                "This replaces your current ledger with $backupCount transactions and the backed-up accounts, budgets, goals, schedules, templates, and appearance settings. Device lock must be set up locally again. Export your current data first if you want to keep it."
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
                            val deletedIds = setOfNotNull(editingId)
                            action(
                                when (selected) {
                                    "delete" -> "Transaction deleted."
                                    "restore" -> "Backup restored."
                                    else -> "Your ledger has been reset."
                                },
                                undo =
                                    if (selected == "delete")
                                        ({
                                            scope.launch {
                                                repository.update { it.undoDelete(deletedIds) }
                                            }
                                        })
                                    else null,
                            ) {
                                when (selected) {
                                    "delete" ->
                                        repository.update {
                                            it.deleteEntries(deletedIds).copy(entryDraft = null)
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

        if (pendingNewType != null && data.entryDraft != null)
            AlertDialog(
                { pendingNewType = null },
                { Text("Continue your draft?") },
                { Text("An unfinished transaction is saved on this device.") },
                {
                    TextButton({
                        val draft = data.entryDraft!!
                        editingId = draft.sourceId
                        seed = null
                        initialType = draft.type
                        sheet = "entry"
                        pendingNewType = null
                    }) {
                        Text("Resume draft")
                    }
                },
                {
                    TextButton({
                        val type = pendingNewType!!
                        pendingNewType = null
                        action("") {
                            repository.update { it.copy(entryDraft = null) }
                            editingId = null
                            seed = null
                            initialType = type
                            sheet = "entry"
                        }
                    }) {
                        Text("Start new")
                    }
                },
            )
        if (portability == "export" || portability == "decrypt")
            ProtectedBackupEditor(
                portability == "export",
                importRaw,
                data,
                {
                    portability = null
                    importRaw = null
                    onExternalBackupConsumed()
                },
            ) { result ->
                if (portability == "export") onExport(result)
                else {
                    importKind = "backup"
                    prepareImport(result)
                }
                portability = null
                importRaw = null
            }
        if (portability == "csv" && importRaw != null)
            CsvImportEditor(
                importRaw!!,
                data,
                {
                    portability = null
                    importRaw = null
                    onExternalBackupConsumed()
                },
            ) { entries ->
                repository.update { it.copy(transactions = it.transactions + entries) }
                workspace = null
                portability = null
                importRaw = null
                onExternalBackupConsumed()
                showTransactions()
            }
    }
}
