package com.centiox.verdant

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
fun AppHeader(page: Page, data: BudgetData, onPage: (Page) -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = if (page == Page.Home) 18.dp else 23.dp),
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
                { onPage(Page.Budgets) },
            )
        } else {
            RoundAction(Icons.Outlined.ArrowBack, "Back to home", { onPage(Page.Home) })
            Text(page.label, Modifier.weight(1f), fontSize = 16.sp, textAlign = TextAlign.Center)
            if (page != Page.Settings)
                RoundAction(Icons.Outlined.Tune, "Open settings", { onPage(Page.Settings) })
            else Spacer(Modifier.size(48.dp))
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
                AppHeader(Page.Home, data, onPage)
                Column(
                    Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Total Balance", fontSize = 16.sp, color = Color(0xFFDCE3D5))
                    Spacer(Modifier.height(7.dp))
                    BasicText(
                        money(totals.balance, data.currency),
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
                            if (net < 0) Orange else Lime,
                        )
                        Text(
                            if (totals.income == 0L && totals.spent == 0L) "No activity this month"
                            else if (net == 0L) "Income matches spending"
                            else
                                "${if (net > 0) "+" else ""}${money(net, data.currency)} this month",
                            Modifier.weight(1f, fill = false).testTag("monthly-cashflow"),
                            fontSize = 12.sp,
                            color = if (net < 0) Orange else Color(0xFFE0E8DA),
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    FlowRow(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        maxItemsInEachRow = if (fontScale >= 1.6f) 2 else 4,
                    ) {
                        listOf(
                                Triple(Icons.Outlined.Payments, "Income", { onAdd("income") }),
                                Triple(Icons.Outlined.ShoppingBag, "Expense", { onAdd("expense") }),
                                Triple(Icons.Outlined.ReceiptLong, "Budget", onBudget),
                                Triple(Icons.Outlined.GridView, "More", { onPage(Page.Settings) }),
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
                                        Icon(icon, null, Modifier.size(23.dp), Color(0xFFE3EDDD))
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
                    "Income ${money(totals.income,data.currency)}",
                    fontSize = 10.sp,
                    color = Muted,
                )
                Text("Spent ${money(totals.spent,data.currency)}", fontSize = 10.sp, color = Muted)
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
) {
    val filtered = query.isNotEmpty() || filter != "all" || category != null
    fun resetFilters() {
        onQueryChange("")
        onFilterChange("all")
        onCategoryChange(null)
    }
    val rows =
        remember(data.transactions, month, filter, query, category) {
            filteredTransactions(data, month, query, filter, category)
        }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 21.dp).testTag("transaction-list"),
        contentPadding = PaddingValues(bottom = 24.dp),
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
                Modifier.padding(bottom = 15.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("all" to "All", "expense" to "Expenses", "income" to "Income").forEach {
                    (value, label) ->
                    FilterChip(
                        filter == value,
                        { onFilterChange(value) },
                        label = { Text(label, fontSize = 12.sp) },
                    )
                }
            }
        }
        if (category != null)
            item {
                InputChip(
                    selected = true,
                    onClick = { onCategoryChange(null) },
                    label = { Text(category, fontSize = 12.sp) },
                    trailingIcon = {
                        Icon(Icons.Outlined.Close, "Remove category filter", Modifier.size(16.dp))
                    },
                )
            }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${rows.size} transaction${if(rows.size==1) "" else "s"}",
                    fontSize = 11.sp,
                    color = Muted,
                    modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                )
                if (filtered && rows.isNotEmpty())
                    TextButton(::resetFilters) { Text("Clear filters", fontSize = 11.sp) }
            }
        }
        if (rows.isEmpty())
            item {
                Panel {
                    EmptyState(
                        "No transactions found",
                        if (filtered) "Clear your filters or try another month."
                        else "Add your first transaction for ${monthLabel(month)}.",
                        Icons.Outlined.Search,
                        action = if (filtered) "Clear filters" else "Add a transaction",
                        onAction = if (filtered) ::resetFilters else onAdd,
                    )
                }
            }
        itemsIndexed(rows, key = { _, entry -> entry.id }) { index, entry ->
            val shape =
                RoundedCornerShape(
                    topStart = if (index == 0) 24.dp else 0.dp,
                    topEnd = if (index == 0) 24.dp else 0.dp,
                    bottomStart = if (index == rows.lastIndex) 24.dp else 0.dp,
                    bottomEnd = if (index == rows.lastIndex) 24.dp else 0.dp,
                )
            Column(Modifier.clip(shape).background(Card)) {
                if (index > 0) HorizontalDivider(color = Line, thickness = .5.dp)
                EntryRows(
                    listOf(entry),
                    data.currency,
                    onEdit,
                    Modifier.testTag("transaction-${entry.id}"),
                )
            }
        }
    }
}

@Composable
fun BudgetsScreen(
    data: BudgetData,
    month: String,
    onSet: () -> Unit,
    onCopyPrevious: (() -> Unit)? = null,
) {
    val limits = data.budgets[month].orEmpty()
    val totals = totals(data, month)
    val total = limits.values.sum()
    val spent = limits.keys.sumOf { totals.byCategory[it] ?: 0 }
    val columns =
        if (
            LocalDensity.current.fontScale >= 1.3f ||
                limits.any { (category, limit) ->
                    money(limit, data.currency).length > 16 ||
                        money(totals.byCategory[category] ?: 0, data.currency).length > 16
                }
        )
            1
        else 2
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 21.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Panel {
            Column(Modifier.padding(21.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "MONTHLY SPENDING PLAN",
                    fontSize = 9.sp,
                    letterSpacing = 1.6.sp,
                    color = Muted,
                )
                AmountText(
                    money(spent, data.currency),
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 29.sp,
                )
                Text("of ${money(total,data.currency)} planned", fontSize = 12.sp, color = Muted)
                Progress(if (total > 0) spent.toFloat() / total else 0f)
                Text(
                    if (total > 0)
                        "${money((total-spent).coerceAtLeast(0),data.currency)} remaining in planned categories"
                    else "Give every little bit a direction.",
                    fontSize = 11.sp,
                    color = Muted,
                )
            }
        }
        SectionHeader("Your categories", "Set limits", onSet)
        if (limits.isEmpty())
            EmptyState(
                "Plan a little. Breathe easier.",
                "Set a limit for groceries, transport, or anything else.",
                action = "Create a budget",
                onAction = onSet,
            )
        if (limits.isEmpty() && onCopyPrevious != null)
            OutlinedButton(onCopyPrevious, Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.ContentCopy, null, Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Use last month's limits", fontSize = 12.sp)
            }
        limits.entries.chunked(columns).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                pair.forEach { (category, limit) ->
                    val used = totals.byCategory[category] ?: 0
                    Panel(Modifier.weight(1f).clickable(onClick = onSet)) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                categoryIcon(category),
                                null,
                                tint = categoryColor(category),
                                modifier = Modifier.size(25.dp),
                            )
                            Text(category, fontSize = 14.sp, minLines = 2, maxLines = 2)
                            AmountText(
                                money(used, data.currency),
                                modifier =
                                    Modifier.fillMaxWidth().testTag("budget-spent-$category"),
                                fontSize = 19.sp,
                            )
                            Text(
                                "of ${money(limit,data.currency)}",
                                fontSize = 10.sp,
                                color = Muted,
                            )
                            Progress(
                                used.toFloat() / limit,
                                color = if (used > limit) Orange else Lime,
                            )
                            Text(
                                "${money(kotlin.math.abs(limit-used),data.currency)} ${if(used>limit)"over limit" else "left to spend"}",
                                fontSize = 10.sp,
                                color = if (used > limit) Orange else Muted,
                            )
                        }
                    }
                }
                if (pair.size == 1 && columns == 2) Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnalyticsScreen(
    data: BudgetData,
    month: String,
    onEdit: (Entry) -> Unit,
    onAll: () -> Unit,
    onCategory: (String) -> Unit,
) {
    val totals = totals(data, month)
    val expenses = data.transactions.filter { it.type == "expense" && it.date.startsWith(month) }
    val daily = dailyValues(expenses, month)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 21.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Panel {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Spending Overview",
                        Modifier.weight(1f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Box(
                        Modifier.size(43.dp).clip(CircleShape).background(Color(0xFF2A2C27)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.NorthEast, null, Modifier.size(23.dp))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Last Entry", fontSize = 13.sp, color = Muted)
                        Text(expenses.maxOfOrNull { it.date } ?: "No entries yet", fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            if (totals.income > 0)
                                "${(totals.spent*100.0/totals.income).roundToInt()}%"
                            else "—",
                            fontSize = 29.sp,
                        )
                        Text("of income spent", fontSize = 8.sp, color = Muted)
                    }
                }
                Column {
                    Text("Total spent this month", fontSize = 10.sp, color = Muted)
                    AmountText(
                        money(totals.spent, data.currency),
                        fontSize = 27.sp,
                        modifier = Modifier.fillMaxWidth().testTag("analytics-total"),
                    )
                }
                SpendingLine(daily, Modifier.fillMaxWidth().height(140.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    (0..4)
                        .map { (daily.lastIndex * it / 4.0).roundToInt() + 1 }
                        .forEach { Text(it.toString(), fontSize = 10.sp, color = Muted) }
                }
                Text(
                    "Daily expenses · ${monthLabel(month)}",
                    fontSize = 9.sp,
                    color = Muted,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
            maxItemsInEachRow =
                if (
                    LocalDensity.current.fontScale >= 1.3f ||
                        listOf("Groceries", "Utilities").any {
                            money(totals.byCategory[it] ?: 0, data.currency).length > 16
                        }
                )
                    1
                else 2,
        ) {
            listOf("Groceries", "Utilities").forEach { category ->
                val amount = totals.byCategory[category] ?: 0
                val limit = data.budgets[month]?.get(category)
                val percent =
                    if (limit != null) (amount * 100.0 / limit).roundToInt()
                    else if (totals.spent > 0) (amount * 100.0 / totals.spent).roundToInt() else 0
                val color = if (category == "Groceries") Color(0xFFBF7847) else Color(0xFFB6CB4C)
                Panel(Modifier.weight(1f).clickable { onCategory(category) }) {
                    Column(
                        Modifier.padding(13.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(category, Modifier.weight(1f), fontSize = 12.sp)
                            Text("Monthly", fontSize = 8.sp, color = color)
                        }
                        val values =
                            dailyValues(expenses.filter { it.category == category }, month)
                                .chunked(5)
                                .map { it.sum() }
                        SpendingLine(values, Modifier.fillMaxWidth().height(68.dp), color, false)
                        Text("$percent%", fontSize = 27.sp)
                        AmountText(
                            money(amount, data.currency),
                            Modifier.fillMaxWidth(),
                            fontSize = 12.sp,
                            color = Muted,
                            weight = FontWeight.Normal,
                        )
                        Text(
                            if (limit != null) "of category budget" else "of monthly spending",
                            fontSize = 8.sp,
                            color = Muted,
                        )
                    }
                }
            }
        }
        Panel {
            SectionHeader("Spending Transactions", "See all", onAll)
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
                )
        }
        if (expenses.isNotEmpty())
            Panel {
                SectionHeader("Where it went")
                categories
                    .filter { (totals.byCategory[it] ?: 0) > 0 }
                    .sortedByDescending { totals.byCategory[it] }
                    .forEach { category ->
                        val amount = totals.byCategory[category] ?: 0
                        Column(
                            Modifier.fillMaxWidth()
                                .clickable { onCategory(category) }
                                .padding(horizontal = 17.dp, vertical = 13.dp)
                        ) {
                            Row {
                                Text(category, Modifier.weight(1f), fontSize = 12.sp)
                                Text(money(amount, data.currency), fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(10.dp))
                            Progress(
                                amount.toFloat() / totals.spent,
                                color = categoryColor(category),
                            )
                        }
                    }
                Spacer(Modifier.height(8.dp))
            }
        Spacer(Modifier.height(10.dp))
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
) {
    var name by rememberSaveable(data.name) { mutableStateOf(data.name) }
    var currency by rememberSaveable(data.currency) { mutableStateOf(data.currency) }
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 21.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        AppearancePanel(data, busy, onAppearance)
        Panel {
            Column(Modifier.padding(21.dp), verticalArrangement = Arrangement.spacedBy(17.dp)) {
                Text("Your profile", fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    name,
                    { name = it.take(50) },
                    enabled = !busy,
                    singleLine = true,
                    label = { Text("Name") },
                    shape = RoundedCornerShape(13.dp),
                    modifier = Modifier.fillMaxWidth(),
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
                    fontSize = 10.sp,
                    color = Muted,
                )
                PrimaryButton(
                    "Save preferences",
                    { onSave(name.trim().ifEmpty { "Friend" }, currency) },
                    enabled = !busy,
                )
            }
        }
        Panel {
            Column(Modifier.padding(21.dp)) {
                Text("Your data, your control.", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Stored privately on this device. Backup files contain readable financial records; choose a private location. Keep a backup before uninstalling.",
                    fontSize = 12.sp,
                    lineHeight = 21.sp,
                    color = Muted,
                )
                Spacer(Modifier.height(13.dp))
                listOf(
                        Triple(Icons.Outlined.FileDownload, "Export backup", onExport),
                        Triple(Icons.Outlined.FileUpload, "Restore backup", onImport),
                        Triple(Icons.Outlined.DeleteOutline, "Reset all data", onReset),
                    )
                    .forEach { (icon, label, action) ->
                        HorizontalDivider(color = Line)
                        TextButton(
                            action,
                            Modifier.fillMaxWidth().heightIn(min = 55.dp),
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
                                fontSize = 12.sp,
                            )
                            Icon(
                                Icons.Outlined.ChevronRight,
                                null,
                                Modifier.size(17.dp),
                                tint = Muted,
                            )
                        }
                    }
            }
        }
        Text(
            "Verdant Budget · 2.1.0\nRoom for what matters.",
            fontSize = 10.sp,
            lineHeight = 22.sp,
            color = Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
        )
    }
}
