package com.centiox.verdant

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.centiox.verdant.resources.*
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.datetime.minus
import org.jetbrains.compose.resources.painterResource

enum class Page(val label: String) {
    Home("Home"),
    Transactions("Transactions"),
    Budgets("Budgets"),
    Analytics("Analytics"),
    Settings("Settings"),
}

@Composable
fun WelcomeScreen(onStart: () -> Unit, onRestore: (() -> Unit)? = null) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val artHeight = (maxHeight * .45f).coerceIn(240.dp, 450.dp)
        val titleSize = if (maxWidth < 370.dp) 29.sp else 32.sp
        Column(
            Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(horizontal = 25.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Image(
                    painterResource(Res.drawable.coins),
                    null,
                    Modifier.fillMaxWidth().height(artHeight),
                    contentScale = ContentScale.FillWidth,
                    alignment = Alignment.TopCenter,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Maintain Your\nPersonal Finance,\nMade Simple",
                    fontSize = titleSize,
                    lineHeight = 39.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-.7).sp,
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    "Manage your finances with ease. Build a little clarity today, and make room for what matters tomorrow.",
                    fontSize = 12.sp,
                    lineHeight = 21.sp,
                    color = Muted,
                )
                Spacer(Modifier.height(26.dp))
            }
            Column {
                PrimaryButton("Start Now", onStart, Modifier.testTag("start"), glow = true)
                if (onRestore != null)
                    TextButton(onRestore, Modifier.fillMaxWidth()) {
                        Text("Restore an existing backup", fontSize = 11.sp, color = Muted)
                    }
                Spacer(Modifier.height(12.dp))
                Text(
                    "PRIVATE BY DESIGN · YOUR DEVICE, YOUR DATA",
                    fontSize = 8.sp,
                    letterSpacing = 1.sp,
                    color = Muted,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun ChoiceField(
    label: String,
    value: String,
    choices: List<String>,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    Column(modifier) {
        Text(label, fontSize = 12.sp, color = Muted)
        Spacer(Modifier.height(7.dp))
        Box {
            OutlinedButton(
                enabled = enabled,
                onClick = {
                    focus.clearFocus()
                    keyboard?.hide()
                    expanded = true
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                shape = RoundedCornerShape(13.dp),
                contentPadding = PaddingValues(horizontal = 14.dp),
                border = BorderStroke(1.dp, Color(0xFF41463A)),
            ) {
                Text(
                    value,
                    Modifier.weight(1f),
                    textAlign = TextAlign.Start,
                    color = Color.White,
                    fontSize = 13.sp,
                )
                Icon(Icons.Outlined.ExpandMore, null, Modifier.size(18.dp), tint = Muted)
            }
            DropdownMenu(expanded && enabled, onDismissRequest = { expanded = false }) {
                choices.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item, fontSize = 13.sp) },
                        onClick = {
                            onChange(item)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun SetupScreen(busy: Boolean, onStart: (String, String, Boolean) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var currency by rememberSaveable { mutableStateOf("USD") }
    var sample by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(27.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Outlined.Spa, null, tint = Lime, modifier = Modifier.size(34.dp))
        Spacer(Modifier.height(35.dp))
        Text("Make it personal.", fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Text(
            "A fresh start for your finances.\nA few details, and you’re ready.",
            fontSize = 13.sp,
            lineHeight = 23.sp,
            color = Muted,
        )
        Spacer(Modifier.height(30.dp))
        OutlinedTextField(
            name,
            { name = it.take(50) },
            label = { Text("Your name") },
            placeholder = { Text("What should we call you?") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("profile-name"),
        )
        Spacer(Modifier.height(20.dp))
        ChoiceField("Currency", currency, currencies, { currency = it })
        Row(
            Modifier.fillMaxWidth().padding(vertical = 13.dp).clickable { sample = !sample },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(sample, { sample = it }, modifier = Modifier.testTag("sample-data"))
            Text("Explore with sample data", fontSize = 12.sp, color = Muted)
        }
        PrimaryButton(
            if (busy) "Creating your space…" else "Let’s get started",
            { onStart(name.trim().ifEmpty { "Friend" }, currency, sample) },
            enabled = !busy,
            modifier = Modifier.testTag("finish-setup"),
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "No accounts. No ads. Just you and your money.",
            fontSize = 10.sp,
            color = Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun AppHeader(
    page: Page,
    data: BudgetData,
    onPage: (Page) -> Unit,
    onAlerts: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = if (page == Page.Home) 18.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (page == Page.Home) {
            Row(
                Modifier.weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(role = Role.Button) { onPage(Page.Settings) }
                    .semantics { contentDescription = "Open profile settings" }
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier.size(43.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = .1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(data.name.take(1).uppercase(), fontSize = 18.sp)
                }
                Column(Modifier.weight(1f)) {
                    Text("Welcome Back", fontSize = 11.sp, color = Color(0xFFBEBFBA))
                    Spacer(Modifier.height(2.dp))
                    Text(
                        data.name,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            RoundAction(
                Icons.Outlined.NotificationsNone,
                "View budget alerts",
                onAlerts ?: { onPage(Page.Budgets) },
            )
        } else {
            RoundAction(Icons.Outlined.ArrowBack, "Back to home", { onPage(Page.Home) })
            Text(
                when (page) {
                    Page.Transactions -> "Activity"
                    Page.Analytics -> "Insights"
                    else -> page.label
                },
                Modifier.weight(1f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
            )
            if (page != Page.Settings)
                RoundAction(Icons.Outlined.Tune, "Open settings", { onPage(Page.Settings) })
            else Spacer(Modifier.size(48.dp))
        }
    }
}

@Composable
fun PageHeader(
    page: Page,
    data: BudgetData,
    month: String,
    onPage: (Page) -> Unit,
    onMonth: (String) -> Unit,
) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))) {
        LiquidBackground(data.backgroundColor, data.animateBackground, Modifier.matchParentSize())
        Column(Modifier.fillMaxWidth()) {
            AppHeader(page, data, onPage)
            if (page != Page.Settings) MonthPicker(month, onMonth)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    data: BudgetData,
    month: String,
    onPage: (Page) -> Unit,
    onAdd: (String) -> Unit,
    onBudget: () -> Unit,
    onEdit: (Entry) -> Unit,
    onAlerts: () -> Unit = {},
    onTogglePrivacy: () -> Unit = {},
) {
    val fontScale = LocalDensity.current.fontScale
    val totals = totals(data, month)
    Column(
        Modifier.fillMaxSize()
            .background(HomeBackground)
            .verticalScroll(rememberScrollState())
            .testTag("home-screen")
    ) {
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
        ) {
            LiquidBackground(
                data.backgroundColor,
                data.animateBackground,
                Modifier.matchParentSize(),
            )
            Column(Modifier.fillMaxWidth()) {
                AppHeader(Page.Home, data, onPage, onAlerts)
                Column(
                    Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TextButton(onTogglePrivacy) {
                        Text("Total Balance", fontSize = 16.sp, color = Color(0xFFDCE3D5))
                        Spacer(Modifier.width(8.dp))
                        Text(if (data.hideBalances) "Show" else "Hide", fontSize = 10.sp)
                    }
                    Spacer(Modifier.height(7.dp))
                    BasicText(
                        displayMoney(totals.balance, data.currency),
                        style =
                            LocalTextStyle.current.copy(
                                fontSize = 37.sp,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-1.2).sp,
                                textAlign = TextAlign.Center,
                            ),
                        autoSize = TextAutoSize.StepBased(minFontSize = 16.sp, maxFontSize = 37.sp),
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(horizontal = 18.dp)
                                .testTag("total-balance")
                                .semantics { liveRegion = LiveRegionMode.Polite },
                        maxLines = 1,
                    )
                    if (data.transactions.any { it.fee?.confirmed == false })
                        Text(
                            "Includes ${displayMoney(data.transactions.filter { it.fee?.confirmed == false }.sumOf { it.fee?.amount ?: 0L }, data.currency)} unconfirmed fee estimates",
                            Modifier.padding(horizontal = 22.dp, vertical = 6.dp),
                            fontSize = 10.sp,
                            color = Muted,
                        )
                    Spacer(Modifier.height(10.dp))
                    val net = totals.income - totals.spent
                    Row(
                        Modifier.widthIn(max = 320.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = .05f))
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            if (net < 0) Icons.Outlined.SouthWest else Icons.Outlined.NorthEast,
                            null,
                            Modifier.size(15.dp),
                            if (net < 0) ExpenseRed else IncomeGreen,
                        )
                        Text(
                            if (totals.income == 0L && totals.spent == 0L) "No activity this month"
                            else if (net == 0L) "Income matches spending"
                            else
                                "${if (net > 0) "+" else ""}${displayMoney(net, data.currency)} this month",
                            Modifier.weight(1f, fill = false).testTag("monthly-cashflow"),
                            fontSize = 12.sp,
                            color = if (net < 0) ExpenseRed else Color(0xFFE0E8DA),
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    FlowRow(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        maxItemsInEachRow = if (fontScale >= 1.6f) 2 else 3,
                    ) {
                        listOf(
                                Triple(Icons.Outlined.Payments, "Income", { onAdd("income") }),
                                Triple(Icons.Outlined.ShoppingBag, "Expense", { onAdd("expense") }),
                                Triple(Icons.Outlined.ReceiptLong, "Budget", onBudget),
                            )
                            .forEach { (icon, label, action) ->
                                val description =
                                    if (label == "Expense") "Add transaction"
                                    else if (label == "Budget") "Set a budget" else label
                                Column(
                                    Modifier.weight(1f)
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable(role = Role.Button, onClick = action)
                                        .semantics { contentDescription = description }
                                        .padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Box(
                                        Modifier.size(52.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = .07f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            icon,
                                            null,
                                            Modifier.size(23.dp),
                                            if (label == "Income") IncomeGreen
                                            else if (label == "Expense") ExpenseRed else Lime,
                                        )
                                    }
                                    Text(label, fontSize = 12.sp, maxLines = 1)
                                }
                            }
                    }
                }
            }
        }
        Column(Modifier.padding(21.dp), verticalArrangement = Arrangement.spacedBy(21.dp)) {
            Column(Modifier.fillMaxWidth().testTag("home-transactions")) {
                SectionHeader(
                    "Transactions",
                    "See all",
                    { onPage(Page.Transactions) },
                    horizontalPadding = 0.dp,
                )
                val rows =
                    data.transactions
                        .filter { it.date.startsWith(month) }
                        .asReversed()
                        .sortedByDescending { it.date }
                        .take(5)
                if (rows.isEmpty())
                    EmptyState(
                        "A fresh start",
                        "Add your first transaction to see your money in motion.",
                        action = "Add a transaction",
                        onAction = { onAdd("expense") },
                    )
                else EntryRows(rows, data.currency, onEdit, horizontalPadding = 0.dp)
            }
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Income ${displayMoney(totals.income,data.currency)}",
                    fontSize = 10.sp,
                    color = Muted,
                )
                Text(
                    "Spent ${displayMoney(totals.spent,data.currency)}",
                    fontSize = 10.sp,
                    color = Muted,
                )
            }
        }
    }
}

@Composable
fun TransactionsScreen(
    data: BudgetData,
    month: String,
    onAdd: () -> Unit,
    onEdit: (Entry) -> Unit,
    query: String,
    filter: String,
    category: String?,
    onQueryChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onCategoryChange: (String?) -> Unit,
    onBulkCategory: suspend (Set<String>, String) -> Unit = { _, _ -> },
    onBulkDelete: (Set<String>) -> Unit = {},
) {
    var advanced by rememberSaveable { mutableStateOf(false) }
    var account by rememberSaveable { mutableStateOf("") }
    var from by rememberSaveable { mutableStateOf("") }
    var to by rememberSaveable { mutableStateOf("") }
    var minAmount by rememberSaveable { mutableStateOf("") }
    var maxAmount by rememberSaveable { mutableStateOf("") }
    var appliedFrom by rememberSaveable { mutableStateOf("") }
    var appliedTo by rememberSaveable { mutableStateOf("") }
    var appliedMin by rememberSaveable { mutableStateOf<Long?>(null) }
    var appliedMax by rememberSaveable { mutableStateOf<Long?>(null) }
    var sort by rememberSaveable { mutableStateOf("Newest") }
    var status by rememberSaveable { mutableStateOf("All") }
    var selecting by rememberSaveable { mutableStateOf(false) }
    var selected by remember { mutableStateOf(arrayListOf<String>()) }
    var bulkCategory by rememberSaveable { mutableStateOf(data.activeCategories().first()) }
    var deleting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var bulkBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope { kotlinx.coroutines.Dispatchers.Main.immediate }
    val listState = rememberLazyListState()
    val filtered =
        query.isNotEmpty() ||
            filter != "all" ||
            category != null ||
            account.isNotEmpty() ||
            appliedFrom.isNotEmpty() ||
            appliedTo.isNotEmpty() ||
            appliedMin != null ||
            appliedMax != null ||
            status != "All"
    fun resetFilters() {
        onQueryChange("")
        onFilterChange("all")
        onCategoryChange(null)
        account = ""
        from = ""
        to = ""
        minAmount = ""
        maxAmount = ""
        appliedFrom = ""
        appliedTo = ""
        appliedMin = null
        appliedMax = null
        status = "All"
        error = null
    }
    val signature =
        "$month|$query|$filter|$category|$account|$appliedFrom|$appliedTo|$appliedMin|$appliedMax|$sort|$status"
    var lastSearch by rememberSaveable { mutableStateOf(signature) }
    LaunchedEffect(signature) {
        if (signature != lastSearch) {
            listState.scrollToItem(0)
            lastSearch = signature
        }
        selected = arrayListOf()
    }
    LaunchedEffect(data.transactions) {
        selected = ArrayList(selected.filter { id -> data.transactions.any { it.id == id } })
    }
    val rows =
        remember(data.transactions, signature) {
            val base =
                filteredTransactions(
                        data,
                        if (appliedFrom.isNotEmpty() || appliedTo.isNotEmpty()) "" else month,
                        query,
                        filter,
                        category,
                    )
                    .filter {
                        (account.isEmpty() ||
                            it.accountId == account ||
                            it.targetAccountId == account) &&
                            (appliedFrom.isEmpty() || it.date >= appliedFrom) &&
                            (appliedTo.isEmpty() || it.date <= appliedTo) &&
                            (appliedMin == null || it.amount >= appliedMin!!) &&
                            (appliedMax == null || it.amount <= appliedMax!!) &&
                            (status == "All" || it.cleared == (status == "Cleared"))
                    }
            when (sort) {
                "Oldest" -> base.sortedBy { it.date }
                "Highest amount" -> base.sortedByDescending { it.amount }
                "Lowest amount" -> base.sortedBy { it.amount }
                else -> base
            }
        }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 21.dp).testTag("transaction-list"),
        state = listState,
        contentPadding = PaddingValues(top = 20.dp, bottom = 32.dp),
    ) {
        item {
            Row(
                Modifier.padding(bottom = 15.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    query,
                    { onQueryChange(it.take(200)) },
                    placeholder = { Text("Search transactions", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = {
                        if (query.isNotEmpty())
                            IconButton({ onQueryChange("") }) {
                                Icon(Icons.Outlined.Close, "Clear search")
                            }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).testTag("transaction-search"),
                )
                RoundAction(Icons.Outlined.Add, "Add transaction", onAdd, tint = Lime)
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                        "all" to "All",
                        "expense" to "Expenses",
                        "income" to "Income",
                        "transfer" to "Transfers",
                    )
                    .forEach { (value, label) ->
                        FilterChip(
                            filter == value,
                            { onFilterChange(value) },
                            label = { Text(label, fontSize = 12.sp) },
                        )
                    }
            }
            Row {
                TextButton({ advanced = !advanced }) {
                    Text(if (advanced) "Hide filters" else "More filters", fontSize = 11.sp)
                }
                TextButton({
                    selecting = !selecting
                    selected = arrayListOf()
                }) {
                    Text(if (selecting) "Done selecting" else "Select records", fontSize = 11.sp)
                }
            }
        }
        if (advanced)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ChoiceField(
                        "Account",
                        if (account.isEmpty()) "All accounts" else data.accountName(account),
                        listOf("All accounts") + data.accounts.map { it.name },
                        { value -> account = data.accounts.find { it.name == value }?.id.orEmpty() },
                    )
                    ChoiceField(
                        "Sort",
                        sort,
                        listOf("Newest", "Oldest", "Highest amount", "Lowest amount"),
                        { sort = it },
                    )
                    ChoiceField(
                        "Statement status",
                        status,
                        listOf("All", "Cleared", "Pending"),
                        { status = it },
                    )
                    Field("From date · YYYY-MM-DD", from, { from = it.take(10) })
                    Field("To date · YYYY-MM-DD", to, { to = it.take(10) })
                    Field("Minimum amount · optional", minAmount, { minAmount = it.take(32) })
                    Field("Maximum amount · optional", maxAmount, { maxAmount = it.take(32) })
                    Text(
                        "Date ranges override the month selected above.",
                        fontSize = 11.sp,
                        color = Muted,
                    )
                    OutlinedButton(
                        {
                            try {
                                require(from.isBlank() || validDate(from)) {
                                    "Use YYYY-MM-DD for the start date."
                                }
                                require(to.isBlank() || validDate(to)) {
                                    "Use YYYY-MM-DD for the end date."
                                }
                                require(from.isBlank() || to.isBlank() || from <= to) {
                                    "Start date must come before end date."
                                }
                                fun bound(v: String): Long? =
                                    if (v.isBlank()) null
                                    else if (Regex("0+(?:[.,]0{0,2})?").matches(v.trim())) 0L
                                    else parseAmount(v)
                                val low = bound(minAmount)
                                val high = bound(maxAmount)
                                require(low == null || high == null || low <= high) {
                                    "Minimum must not exceed maximum."
                                }
                                appliedFrom = from.trim()
                                appliedTo = to.trim()
                                appliedMin = low
                                appliedMax = high
                                advanced = false
                                error = null
                            } catch (e: Exception) {
                                error = e.message
                            }
                        },
                        Modifier.fillMaxWidth(),
                    ) {
                        Text("Apply date & amount filters")
                    }
                }
            }
        if (category != null)
            item {
                InputChip(
                    true,
                    { onCategoryChange(null) },
                    { Text(category, fontSize = 12.sp) },
                    { Icon(Icons.Outlined.Close, "Remove category filter", Modifier.size(16.dp)) },
                )
            }
        if (error != null) item { Text(error!!, color = ErrorColor, fontSize = 12.sp) }
        if (selecting)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${selected.size} selected", fontSize = 12.sp)
                    TextButton({ selected = ArrayList(rows.map { it.id }) }) {
                        Text("Select filtered records")
                    }
                    ChoiceField(
                        "New category",
                        bulkCategory,
                        data.activeCategories(),
                        { bulkCategory = it },
                    )
                    Text(
                        "Recategorizing assigns the whole amount to one category and removes any splits. Transfers are left unchanged.",
                        fontSize = 11.sp,
                        color = Muted,
                    )
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        TextButton(
                            {
                                if (!bulkBusy) {
                                    bulkBusy = true
                                    scope.launch {
                                        try {
                                            onBulkCategory(selected.toSet(), bulkCategory)
                                            selected = arrayListOf()
                                            error = null
                                        } catch (e: CancellationException) {
                                            throw e
                                        } catch (e: Exception) {
                                            error = e.message
                                        } finally {
                                            bulkBusy = false
                                        }
                                    }
                                }
                            },
                            enabled = selected.isNotEmpty() && !bulkBusy,
                        ) {
                            Text("Apply category")
                        }
                        TextButton(
                            { deleting = true },
                            enabled = selected.isNotEmpty() && !bulkBusy,
                        ) {
                            Text("Delete selected", color = ExpenseRed)
                        }
                    }
                }
            }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${rows.size} transaction${if (rows.size == 1) "" else "s"}",
                    Modifier.weight(1f).padding(vertical = 12.dp),
                    fontSize = 11.sp,
                    color = Muted,
                )
                if (filtered && rows.isNotEmpty())
                    TextButton(::resetFilters) { Text("Clear filters", fontSize = 11.sp) }
            }
        }
        if (rows.isEmpty())
            item {
                EmptyState(
                    "No transactions found",
                    if (filtered) "Clear your filters or try another month."
                    else "Add your first transaction for ${monthLabel(month)}.",
                    Icons.Outlined.Search,
                    action = if (filtered) "Clear filters" else "Add a transaction",
                    onAction = if (filtered) ::resetFilters else onAdd,
                )
            }
        itemsIndexed(rows, key = { _, e -> e.id }) { index, entry ->
            Column {
                if (index > 0) HorizontalDivider(thickness = .5.dp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selecting)
                        Checkbox(
                            entry.id in selected,
                            { checked ->
                                selected =
                                    ArrayList(
                                        if (checked) selected + entry.id else selected - entry.id
                                    )
                            },
                        )
                    EntryRows(
                        listOf(entry),
                        data.currency,
                        { e ->
                            if (selecting)
                                selected =
                                    ArrayList(
                                        if (e.id in selected) selected - e.id else selected + e.id
                                    )
                            else onEdit(e)
                        },
                        Modifier.weight(1f).testTag("transaction-${entry.id}"),
                        horizontalPadding = 0.dp,
                    )
                }
            }
        }
    }
    if (deleting)
        AlertDialog(
            { deleting = false },
            { Text("Delete ${selected.size} transactions?") },
            {
                Text(
                    "Undo restores this batch while the message is visible. Tools keeps up to 200 recently deleted entries for 30 days."
                )
            },
            {
                TextButton({
                    onBulkDelete(selected.toSet())
                    deleting = false
                }) {
                    Text("Delete", color = ExpenseRed)
                }
            },
            { TextButton({ deleting = false }) { Text("Cancel") } },
        )
}

@Composable
fun BudgetsScreen(
    data: BudgetData,
    month: String,
    onSet: (String) -> Unit,
    onCopyPrevious: (() -> Unit)? = null,
) {
    var anchor by
        rememberSaveable(month) {
            mutableStateOf(
                (if (month == currentMonth()) today()
                    else
                        kotlinx.datetime.LocalDate.parse(
                            "$month-${data.payday.toString().padStart(2,'0')}"
                        ))
                    .toString()
            )
        }
    val period = data.period(kotlinx.datetime.LocalDate.parse(anchor))
    val limits = data.limitsFor(period)
    val total = limits.values.sum()
    val spent = limits.keys.sumOf { data.periodSpending(period, it) }
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag("budgets-scroll")
            .padding(horizontal = 21.dp)
            .padding(top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        if (data.budgetCycle != "monthly")
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton({
                    anchor = period.start.minus(1, kotlinx.datetime.DateTimeUnit.DAY).toString()
                }) {
                    Icon(Icons.Outlined.ChevronLeft, "Previous budget period")
                }
                Text(period.label, Modifier.weight(1f), fontSize = 11.sp)
                IconButton({ anchor = period.end.toString() }) {
                    Icon(Icons.Outlined.ChevronRight, "Next budget period")
                }
            }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                if (data.budgetCycle == "monthly") "MONTHLY SPENDING PLAN"
                else "${data.budgetCycle.uppercase()} SPENDING PLAN",
                fontSize = 9.sp,
                letterSpacing = 1.6.sp,
                color = Muted,
            )
            AmountText(
                displayMoney(spent, data.currency),
                Modifier.fillMaxWidth(),
                fontSize = 32.sp,
            )
            Text(
                "of ${displayMoney(total, data.currency)} planned",
                fontSize = 12.sp,
                color = Muted,
            )
            Progress(
                if (total > 0) spent.toFloat() / total else 0f,
                color = if (spent > total && total > 0) Orange else Lime,
            )
            Text(
                if (total == 0L) "Give every little bit a direction."
                else if (spent > total)
                    "${displayMoney(spent - total, data.currency)} over your planned limits"
                else
                    "${displayMoney(total - spent, data.currency)} remaining in planned categories",
                fontSize = 11.sp,
                color = if (spent > total && total > 0) Orange else Muted,
            )
        }
        HorizontalDivider(thickness = .5.dp)
        Column {
            SectionHeader(
                "Your categories",
                "Set limits",
                { onSet(period.key) },
                horizontalPadding = 0.dp,
            )
            if (limits.isEmpty())
                EmptyState(
                    "Plan a little. Breathe easier.",
                    "Set a limit for groceries, transport, or anything else.",
                    action = "Create a budget",
                    onAction = { onSet(period.key) },
                )
            if (limits.isEmpty() && onCopyPrevious != null)
                OutlinedButton(onCopyPrevious, Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.ContentCopy, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Use last month's limits", fontSize = 12.sp)
                }
            limits.forEach { (category, limit) ->
                val used = data.periodSpending(period, category)
                Column(
                    Modifier.fillMaxWidth()
                        .testTag("budget-category-$category")
                        .clickable(role = Role.Button, onClick = { onSet(period.key) })
                        .padding(vertical = 15.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val stacked =
                        LocalDensity.current.fontScale >= 1.3f ||
                            displayMoney(used, data.currency).length > 16 ||
                            displayMoney(limit, data.currency).length > 16
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CategoryMark(category)
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Text(category, fontSize = 14.sp)
                            if (!stacked)
                                AmountText(
                                    displayMoney(used, data.currency),
                                    Modifier.fillMaxWidth().testTag("budget-spent-$category"),
                                    fontSize = 20.sp,
                                )
                        }
                        if (!stacked)
                            Column(
                                Modifier.width(100.dp),
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Text("Planned", fontSize = 10.sp, color = Muted)
                                AmountText(
                                    displayMoney(limit, data.currency),
                                    Modifier.widthIn(max = 100.dp),
                                    fontSize = 12.sp,
                                    color = Muted,
                                )
                            }
                        Icon(Icons.Outlined.ChevronRight, null, Modifier.size(16.dp), Muted)
                    }
                    if (stacked) {
                        AmountText(
                            displayMoney(used, data.currency),
                            Modifier.fillMaxWidth().testTag("budget-spent-$category"),
                            fontSize = 22.sp,
                        )
                        Text(
                            "of ${displayMoney(limit, data.currency)}",
                            fontSize = 11.sp,
                            color = Muted,
                        )
                    }
                    Progress(used.toFloat() / limit, color = if (used > limit) Orange else Lime)
                    Text(
                        "${displayMoney(kotlin.math.abs(limit - used), data.currency)} ${if (used > limit) "over limit" else "left to spend"}",
                        fontSize = 11.sp,
                        color = if (used > limit) Orange else Muted,
                    )
                }
                HorizontalDivider(thickness = .5.dp)
            }
        }
    }
}

@Composable
private fun CategoryMark(category: String) {
    Box(
        Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(alpha = .055f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(categoryIcon(category), null, Modifier.size(20.dp), categoryColor(category))
    }
}

@Composable
fun AnalyticsScreen(
    data: BudgetData,
    month: String,
    onEdit: (Entry) -> Unit,
    onAll: () -> Unit,
    onCategory: (String) -> Unit,
    onFees: () -> Unit = {},
) {
    val totals = totals(data, month)
    val expenses =
        data.transactions.filter {
            (it.type == "expense" || (it.fee?.amount ?: 0) > 0) && it.date.startsWith(month)
        }
    val daily = dailyValues(data.transactions, month)
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag("analytics-scroll")
            .padding(horizontal = 21.dp)
            .padding(top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Spending Overview", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text("Total spent this month", fontSize = 11.sp, color = Muted)
            AmountText(
                displayMoney(totals.spent, data.currency),
                Modifier.fillMaxWidth().testTag("analytics-total"),
                fontSize = 32.sp,
            )
            Text(
                if (totals.income > 0)
                    "${(totals.spent * 100.0 / totals.income).roundToInt()}% of income spent"
                else "Add income to see your spending ratio",
                fontSize = 11.sp,
                color = Muted,
            )
            SpendingLine(daily, Modifier.fillMaxWidth().height(140.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                (0..4)
                    .map { (daily.lastIndex * it / 4.0).roundToInt() + 1 }
                    .forEachIndexed { index, day ->
                        Text(
                            day.toString(),
                            Modifier.weight(1f).testTag("chart-day-$index"),
                            fontSize = 10.sp,
                            color = Muted,
                            textAlign =
                                when (index) {
                                    0 -> TextAlign.Start
                                    4 -> TextAlign.End
                                    else -> TextAlign.Center
                                },
                        )
                    }
            }
            Text("Daily expenses · ${monthLabel(month)}", fontSize = 10.sp, color = Muted)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Income", fontSize = 11.sp, color = Muted)
                AmountText(
                    displayMoney(totals.income, data.currency),
                    Modifier.fillMaxWidth(),
                    fontSize = 18.sp,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Net this month", fontSize = 11.sp, color = Muted)
                AmountText(
                    displayMoney(totals.income - totals.spent, data.currency),
                    Modifier.fillMaxWidth(),
                    fontSize = 18.sp,
                    color = if (totals.income < totals.spent) Orange else Lime,
                )
            }
        }
        val feeRows = data.transactions.filter { it.date.startsWith(month) && it.fee != null }
        ToolRow(
            "Transaction fees",
            "${feeRows.size} transactions with fees · ${feeRows.count { it.fee?.confirmed == false }} unconfirmed estimates",
            onFees,
        ) {
            AmountText(
                displayMoney(feeRows.sumOf { it.fee?.amount ?: 0L }, data.currency),
                Modifier.fillMaxWidth(),
                fontSize = 24.sp,
                color = ExpenseRed,
            )
            Text("View by type and amount band", fontSize = 11.sp, color = Lime)
        }
        val previous = totals(data, shiftMonth(month, -1))
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("Compared with last month", fontSize = 13.sp)
            Text(
                "${displayMoney(previous.spent, data.currency)} spent in ${monthLabel(shiftMonth(month, -1))}",
                fontSize = 11.sp,
                color = Muted,
            )
            val change = totals.spent - previous.spent
            Text(
                if (previous.spent == 0L) "No prior spending to compare"
                else
                    "${displayMoney(kotlin.math.abs(change), data.currency)} ${if (change > 0) "more" else "less"} spent",
                fontSize = 12.sp,
                color = if (change > 0) ExpenseRed else IncomeGreen,
            )
            val upcoming =
                data.recurring
                    .filter {
                        it.active &&
                            it.type == "expense" &&
                            it.nextDate.startsWith(month) &&
                            it.nextDate >= today().toString()
                    }
                    .sumOf { it.amount + (it.fee?.amount ?: 0) }
            Text(
                "Next scheduled expenses in this month: ${displayMoney(upcoming, data.currency)}",
                fontSize = 11.sp,
                color = Muted,
            )
        }
        HorizontalDivider(thickness = .5.dp)
        if (expenses.isNotEmpty())
            Column {
                SectionHeader("Where it went", horizontalPadding = 0.dp)
                data
                    .allCategories()
                    .filter { (totals.byCategory[it] ?: 0) > 0 }
                    .sortedByDescending { totals.byCategory[it] }
                    .forEach { category ->
                        val amount = totals.byCategory[category] ?: 0
                        Column(
                            Modifier.fillMaxWidth()
                                .testTag("analytics-category-$category")
                                .clickable(role = Role.Button) { onCategory(category) }
                                .padding(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                CategoryMark(category)
                                Text(category, Modifier.weight(1f), fontSize = 13.sp)
                                Text(
                                    "${(amount * 100.0 / totals.spent).roundToInt()}%",
                                    fontSize = 12.sp,
                                    color = Muted,
                                )
                                Icon(Icons.Outlined.ChevronRight, null, Modifier.size(16.dp), Muted)
                            }
                            AmountText(
                                displayMoney(amount, data.currency),
                                Modifier.fillMaxWidth(),
                                fontSize = 18.sp,
                            )
                            Progress(
                                amount.toFloat() / totals.spent,
                                color = categoryColor(category),
                            )
                        }
                        HorizontalDivider(thickness = .5.dp)
                    }
            }
        Column(Modifier.testTag("analytics-recent")) {
            SectionHeader("Spending Transactions", "See all", onAll, horizontalPadding = 0.dp)
            if (expenses.isEmpty())
                EmptyState(
                    "Your story starts here.",
                    "Add expenses to reveal your spending patterns.",
                    Icons.Outlined.BarChart,
                )
            else
                EntryRows(
                    expenses.asReversed().sortedByDescending { it.date }.take(3),
                    data.currency,
                    onEdit,
                    horizontalPadding = 0.dp,
                )
        }
    }
}

@Composable
fun SettingsScreen(
    data: BudgetData,
    busy: Boolean,
    onSave: (String, String) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onReset: () -> Unit,
    onAppearance: (String, Boolean) -> Unit,
    onTools: (String) -> Unit = {},
) {
    var name by rememberSaveable(data.name) { mutableStateOf(data.name) }
    var currency by rememberSaveable(data.currency) { mutableStateOf(data.currency) }
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag("settings-scroll")
            .imePadding()
            .padding(horizontal = 21.dp)
            .padding(top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp),
    ) {
        ToolRow(
            "Planning & tools",
            "Accounts, scheduled payments, goals, categories, privacy, and data tools.",
            { onTools("Tools") },
        )
        AppearancePanel(data, busy, onAppearance)
        HorizontalDivider(thickness = .5.dp)
        Column(
            Modifier.testTag("settings-profile"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Your profile", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                name,
                { name = it.take(50) },
                enabled = !busy,
                singleLine = true,
                label = { Text("Name") },
                shape = RoundedCornerShape(13.dp),
                modifier = Modifier.fillMaxWidth().testTag("settings-name"),
            )
            ChoiceField(
                "Display currency",
                currency,
                currencies,
                { currency = it },
                enabled = !busy,
            )
            Text(
                "Changing currency changes the symbol only. Amounts are not converted.",
                fontSize = 11.sp,
                lineHeight = 18.sp,
                color = Muted,
            )
            PrimaryButton(
                "Save preferences",
                { onSave(name.trim().ifEmpty { "Friend" }, currency) },
                enabled = !busy,
            )
        }
        HorizontalDivider(thickness = .5.dp)
        Column {
            Text("Your data, your control.", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Text(
                "Stored privately on this device. Backups contain readable financial records; keep them somewhere private.",
                fontSize = 12.sp,
                lineHeight = 21.sp,
                color = Muted,
            )
            Spacer(Modifier.height(14.dp))
            listOf(
                    Triple(Icons.Outlined.FileDownload, "Export backup", onExport),
                    Triple(Icons.Outlined.FileUpload, "Restore backup", onImport),
                    Triple(Icons.Outlined.DeleteOutline, "Reset all data", onReset),
                )
                .forEach { (icon, label, action) ->
                    TextButton(
                        action,
                        Modifier.fillMaxWidth().heightIn(min = 58.dp),
                        enabled = !busy,
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Icon(
                            icon,
                            null,
                            Modifier.size(20.dp),
                            tint = if (label.startsWith("Reset")) Orange else Lime,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            label,
                            Modifier.weight(1f),
                            textAlign = TextAlign.Start,
                            color = if (label.startsWith("Reset")) Orange else Color.White,
                            fontSize = 13.sp,
                        )
                        Icon(Icons.Outlined.ChevronRight, null, Modifier.size(17.dp), Muted)
                    }
                    HorizontalDivider(thickness = .5.dp)
                }
        }
        Text(
            "Verdant Budget · 3.0.0\nRoom for what matters.",
            fontSize = 10.sp,
            lineHeight = 22.sp,
            color = Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().testTag("settings-footer").padding(vertical = 12.dp),
        )
    }
}
