package com.centiox.verdant

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
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
fun WelcomeScreen(onStart: () -> Unit) {
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
                Spacer(Modifier.height(18.dp))
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
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label, fontSize = 12.sp, color = Muted)
        Spacer(Modifier.height(7.dp))
        Box {
            OutlinedButton(
                onClick = { expanded = true },
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
            DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
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
        Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 23.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (page == Page.Home) {
            Box(
                Modifier.size(43.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF46563C), Color(0xFF202C1D))))
                    .clickable { onPage(Page.Settings) },
                contentAlignment = Alignment.Center,
            ) {
                Text(data.name.take(1).uppercase(), fontSize = 18.sp)
            }
            Column(Modifier.weight(1f)) {
                Text("Welcome Back", fontSize = 11.sp, color = Color(0xFFBEBFBA))
                Spacer(Modifier.height(2.dp))
                Text(data.name, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            RoundAction(
                Icons.Outlined.NotificationsNone,
                "View budget alerts",
                { onPage(Page.Budgets) },
            )
        } else {
            RoundAction(Icons.Outlined.ArrowBack, "Back to home", { onPage(Page.Home) })
            Text(page.label, Modifier.weight(1f), fontSize = 16.sp, textAlign = TextAlign.Center)
            RoundAction(Icons.Outlined.Tune, "Open settings", { onPage(Page.Settings) })
        }
    }
}

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
    val limits = data.budgets[month].orEmpty()
    val budget = limits.values.sum()
    val spent = limits.keys.sumOf { totals.byCategory[it] ?: 0 }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("home-screen")) {
        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 25.dp, bottomEnd = 25.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF0A130D), Color(0xFF1B3316), Color(0xFF416723)),
                        start = androidx.compose.ui.geometry.Offset.Zero,
                        end = androidx.compose.ui.geometry.Offset.Infinite,
                    )
                )
        ) {
            AppHeader(Page.Home, data, onPage)
            Column(
                Modifier.fillMaxWidth().padding(top = 15.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Total Balance", fontSize = 16.sp, color = Color(0xFFDCE3D5))
                Spacer(Modifier.height(7.dp))
                BasicText(
                    money(totals.balance, data.currency),
                    style =
                        MaterialTheme.typography.headlineLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-1.2).sp,
                            textAlign = TextAlign.Center,
                        ),
                    autoSize = TextAutoSize.StepBased(minFontSize = 16.sp, maxFontSize = 37.sp),
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .testTag("total-balance"),
                    maxLines = 1,
                )
                Spacer(Modifier.height(11.dp))
                Row(
                    Modifier.clip(CircleShape)
                        .background(Color.White.copy(alpha = .07f))
                        .padding(horizontal = 13.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Outlined.NorthEast, null, Modifier.size(17.dp))
                    Text(
                        if (totals.income > 0)
                            "${((totals.income-totals.spent)*100.0/totals.income).roundToInt()}% saved"
                        else "Your fresh start",
                        fontSize = 13.sp,
                    )
                }
                Spacer(Modifier.height(31.dp))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 17.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    listOf(
                            Triple(Icons.Outlined.Payments, "Income", { onAdd("income") }),
                            Triple(Icons.Outlined.ShoppingBag, "Expense", { onAdd("expense") }),
                            Triple(Icons.Outlined.ReceiptLong, "Budget", onBudget),
                            Triple(Icons.Outlined.GridView, "More", { onPage(Page.Settings) }),
                        )
                        .forEach { (icon, label, action) ->
                            Column(
                                Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                RoundAction(
                                    icon,
                                    if (label == "Expense") "Add transaction"
                                    else if (label == "Budget") "Set a budget" else label,
                                    action,
                                    tint = Color(0xFFE3EDDD),
                                    size = 57.dp,
                                )
                                Text(label, fontSize = 13.sp)
                            }
                        }
                }
            }
        }
        Column(Modifier.padding(21.dp), verticalArrangement = Arrangement.spacedBy(21.dp)) {
            Box(
                Modifier.fillMaxWidth()
                    .height(145.dp * fontScale.coerceAtLeast(1f))
                    .clip(RoundedCornerShape(23.dp))
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFF10120E), Color(0xFF030B00)))
                    )
                    .border(1.dp, Color(0xFF33392D), RoundedCornerShape(23.dp))
            ) {
                RibbonArt(
                    Modifier.align(Alignment.BottomEnd)
                        .offset(x = 30.dp, y = 20.dp)
                        .width(190.dp)
                        .height(155.dp)
                )
                Column(Modifier.padding(18.dp).fillMaxWidth(.72f)) {
                    Text("Save & Grow", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (budget > 0)
                            "${money((budget-spent).coerceAtLeast(0),data.currency)} of your budget left"
                        else "A little planning. More possibilities.",
                        fontSize = 11.sp,
                        color = Muted,
                        maxLines = 2,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onBudget,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, Color(0xFF306A24)),
                        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 3.dp),
                        modifier = Modifier.heightIn(min = 36.dp),
                    ) {
                        Text("View Budgets", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
            Panel {
                SectionHeader("Transactions", "See all", { onPage(Page.Transactions) })
                HorizontalDivider(color = Line, thickness = .5.dp)
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
                else EntryRows(rows, data.currency, onEdit)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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
    initialQuery: String = "",
) {
    var query by rememberSaveable(initialQuery) { mutableStateOf(initialQuery) }
    var filter by rememberSaveable { mutableStateOf("all") }
    val rows =
        remember(data.transactions, month, filter, query) {
            data.transactions
                .asReversed()
                .filter {
                    it.date.startsWith(month) &&
                        (filter == "all" || it.type == filter) &&
                        "${it.title} ${it.category}".contains(query, true)
                }
                .sortedByDescending { it.date }
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
                    { query = it },
                    placeholder = { Text("Search transactions", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
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
                        { filter = value },
                        label = { Text(label, fontSize = 12.sp) },
                    )
                }
            }
        }
        item {
            Text(
                "${rows.size} transaction${if(rows.size==1) "" else "s"}",
                fontSize = 11.sp,
                color = Muted,
                modifier = Modifier.padding(bottom = 15.dp),
            )
        }
        if (rows.isEmpty())
            item {
                Panel {
                    EmptyState(
                        "No transactions found",
                        "Try another search or month, or add a new entry.",
                        Icons.Outlined.Search,
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
fun BudgetsScreen(data: BudgetData, month: String, onSet: () -> Unit) {
    val limits = data.budgets[month].orEmpty()
    val totals = totals(data, month)
    val total = limits.values.sum()
    val spent = limits.keys.sumOf { totals.byCategory[it] ?: 0 }
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
                Text(
                    money(spent, data.currency),
                    fontSize = 29.sp,
                    fontWeight = FontWeight.SemiBold,
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
        limits.entries.chunked(2).forEach { pair ->
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
                            Text(category, fontSize = 14.sp)
                            Text(
                                money(used, data.currency),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.SemiBold,
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
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

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
                    Text(
                        money(totals.spent, data.currency),
                        fontSize = 27.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("analytics-total"),
                    )
                }
                SpendingLine(daily, Modifier.fillMaxWidth().height(140.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("1", "8", "15", "22", daily.size.toString()).forEach {
                        Text(it, fontSize = 10.sp, color = Muted)
                    }
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
        Row(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
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
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("$percent%", Modifier.weight(1f), fontSize = 27.sp)
                            Text(money(amount, data.currency), fontSize = 10.sp, color = Muted)
                        }
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
        Panel {
            Column(Modifier.padding(21.dp), verticalArrangement = Arrangement.spacedBy(17.dp)) {
                Text("Your profile", fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    name,
                    { name = it.take(50) },
                    singleLine = true,
                    label = { Text("Name") },
                    shape = RoundedCornerShape(13.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                ChoiceField("Display currency", currency, currencies, { currency = it })
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
            "Verdant Budget · 2.0.0\nBuilt with Kotlin Multiplatform\nRoom for what matters.",
            fontSize = 10.sp,
            lineHeight = 22.sp,
            color = Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
        )
    }
}
