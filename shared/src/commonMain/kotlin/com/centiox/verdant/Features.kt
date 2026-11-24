package com.centiox.verdant

import kotlinx.datetime.*
import kotlinx.serialization.Serializable

@Serializable data class Split(val category: String, val amount: Long)

@Serializable
data class Account(
    val id: String = "main",
    val name: String = "Main account",
    val openingBalance: Long = 0,
    val archived: Boolean = false,
    val kind: String = "General",
)

@Serializable
data class CategorySetting(
    val name: String,
    val icon: String = "Other",
    val archived: Boolean = false,
)

@Serializable
data class Recurring(
    val id: String = newId(),
    val title: String,
    val amount: Long,
    val type: String = "expense",
    val category: String = "Other",
    val accountId: String = "main",
    val nextDate: String,
    val frequency: String = "monthly",
    val anchorDay: Int = nextDate.takeLast(2).toInt(),
    val active: Boolean = true,
    val fee: TransactionFee? = null,
)

@Serializable data class Contribution(val id: String = newId(), val amount: Long, val date: String)

@Serializable
data class SavingsGoal(
    val id: String = newId(),
    val name: String,
    val target: Long,
    val dueDate: String? = null,
    val contributions: List<Contribution> = emptyList(),
)

@Serializable
data class EntryTemplate(val id: String = newId(), val name: String, val entry: Entry)

@Serializable data class DeletedEntry(val entry: Entry, val deletedOn: String)

data class BudgetPeriod(val start: LocalDate, val end: LocalDate, val key: String) {
    val label: String
        get() =
            "${friendlyDate(start.toString())} – ${friendlyDate(end.minus(1, DateTimeUnit.DAY).toString())}"

    fun contains(date: String) = date >= start.toString() && date < end.toString()
}

fun friendlyDate(value: String): String {
    val d = LocalDate.parse(value)
    return "${d.day} ${d.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${d.year}"
}

fun daysBetween(start: String, end: String): Int =
    LocalDate.parse(start).daysUntil(LocalDate.parse(end))

fun validDate(value: String) =
    runCatching { LocalDate.parse(value).let { it.year in 1..9999 && it.toString() == value } }
        .getOrDefault(false)

fun validBudgetKey(key: String): Boolean =
    if ('|' !in key) Regex("[0-9]{4}-(0[1-9]|1[0-2])").matches(key) && key >= MIN_MONTH
    else {
        val parts = key.split('|')
        parts.size == 3 &&
            parts[0] in listOf("weekly", "payday") &&
            parts[1].toIntOrNull() in 1..28 &&
            validDate(parts[2])
    }

fun BudgetData.allCategories(): List<String> =
    (categorySettings.map { it.name } + categories).distinct()

fun BudgetData.activeCategories(): List<String> =
    allCategories().filter { name -> categorySettings.none { it.name == name && it.archived } }

fun BudgetData.categoryIconKey(name: String) =
    categorySettings.find { it.name == name }?.icon ?: name

fun Entry.spentIn(category: String): Long {
    val principal =
        if (type != "expense") 0L
        else if (splits.isEmpty()) {
            if (this.category == category) amount else 0L
        } else splits.filter { it.category == category }.sumOf { it.amount }
    return principal + if (category == FEE_CATEGORY) fee?.amount ?: 0L else 0L
}

fun BudgetData.accountBalance(id: String): Long =
    accounts.firstOrNull { it.id == id }?.openingBalance.orZero() +
        transactions.sumOf {
            val fee = it.fee?.amount ?: 0L
            when {
                it.type == "transfer" && it.accountId == id -> -it.amount - fee
                it.type == "transfer" && it.targetAccountId == id -> it.amount
                it.accountId == id -> (if (it.type == "income") it.amount else -it.amount) - fee
                else -> 0L
            }
        }

private fun Long?.orZero() = this ?: 0L

fun BudgetData.accountName(id: String) =
    accounts.firstOrNull { it.id == id }?.name ?: "Unknown account"

fun validateFeatures(d: BudgetData) {
    require(d.accounts.isNotEmpty() && d.accounts.size <= 100 && d.accounts.any { !it.archived }) {
        "Keep at least one active account."
    }
    require(d.accounts.map { it.id }.distinct().size == d.accounts.size) { "Duplicate accounts." }
    require(d.accounts.map { it.name.trim().lowercase() }.distinct().size == d.accounts.size) {
        "Account names must be unique."
    }
    d.accounts.forEach {
        require(
            it.id.isNotBlank() &&
                it.id.length <= 80 &&
                it.name.isNotBlank() &&
                it.name == it.name.trim() &&
                it.name.length <= 50 &&
                it.openingBalance in -MAX_AMOUNT..MAX_AMOUNT &&
                it.kind in listOf("General", "Cash", "M-Pesa", "Bank", "Savings")
        ) {
            "Invalid account."
        }
    }
    require(
        d.categorySettings.size <= 100 &&
            d.categorySettings.map { it.name }.distinct().size == d.categorySettings.size
    ) {
        "Duplicate or too many categories."
    }
    d.categorySettings.forEach {
        require(
            it.name.trim() == it.name &&
                it.name.isNotBlank() &&
                it.name.length <= 40 &&
                it.icon in categories
        ) {
            "Invalid category."
        }
    }
    require(d.activeCategories().isNotEmpty()) { "Keep at least one active category." }
    require(d.budgetCycle in listOf("monthly", "weekly", "payday") && d.payday in 1..28) {
        "Invalid budget period."
    }
    fun entry(e: Entry) {
        require(
            e.id.isNotBlank() &&
                e.id.length <= 80 &&
                e.title.isNotBlank() &&
                e.title.length <= 100 &&
                e.amount in 1..MAX_AMOUNT &&
                e.type in listOf("income", "expense", "transfer") &&
                e.category in d.allCategories() &&
                validDate(e.date)
        ) {
            "Invalid stored transaction."
        }
        require(e.accountId in d.accounts.map { it.id } && e.note.length <= 1000) {
            "Invalid transaction account or note."
        }
        if (e.type == "transfer")
            require(
                e.targetAccountId in d.accounts.map { it.id } &&
                    e.targetAccountId != e.accountId &&
                    e.splits.isEmpty()
            ) {
                "Select two different accounts for a transfer."
            }
        else require(e.targetAccountId == null) { "Only transfers have a destination account." }
        e.fee?.let {
            require(
                it.amount in 0..MAX_AMOUNT &&
                    it.kind in feeKinds &&
                    it.tariff.length <= 200 &&
                    it.source.length <= 300 &&
                    it.bandMin in 0..MAX_AMOUNT &&
                    it.bandMax in it.bandMin..MAX_AMOUNT
            ) {
                "Invalid transaction fee."
            }
        }
        if (e.splits.isNotEmpty())
            require(
                e.type == "expense" &&
                    e.splits.size <= 30 &&
                    e.splits.all {
                        it.amount in 1..MAX_AMOUNT && it.category in d.allCategories()
                    } &&
                    e.splits.sumOf { it.amount } == e.amount
            ) {
                "Split amounts must add up exactly to the transaction total."
            }
    }
    d.transactions.forEach(::entry)
    require(
        d.recurring.size <= 500 && d.recurring.map { it.id }.distinct().size == d.recurring.size
    ) {
        "Invalid recurring payments."
    }
    d.recurring.forEach {
        entry(
            Entry(
                it.id,
                it.title,
                it.amount,
                it.type,
                it.category,
                it.nextDate,
                it.accountId,
                fee = it.fee,
            )
        )
        require(
            it.id.isNotBlank() &&
                it.id.length <= 48 &&
                it.title.isNotBlank() &&
                it.title.length <= 100 &&
                it.amount in 1..MAX_AMOUNT &&
                it.type in listOf("income", "expense") &&
                it.category in d.allCategories() &&
                it.accountId in d.accounts.map { a -> a.id } &&
                validDate(it.nextDate) &&
                it.frequency in listOf("weekly", "monthly", "yearly") &&
                it.anchorDay in 1..31
        ) {
            "Invalid scheduled payment."
        }
    }
    require(d.goals.size <= 100 && d.goals.map { it.id }.distinct().size == d.goals.size) {
        "Invalid goals."
    }
    d.goals.forEach { g ->
        require(
            g.name.isNotBlank() &&
                g.name.length <= 60 &&
                g.target in 1..MAX_AMOUNT &&
                (g.dueDate == null || validDate(g.dueDate)) &&
                g.contributions.size <= 1000 &&
                g.contributions.map { it.id }.distinct().size == g.contributions.size &&
                g.contributions.all { it.amount in 1..MAX_AMOUNT && validDate(it.date) } &&
                g.contributions.sumOf { it.amount } <= MAX_AMOUNT
        ) {
            "Invalid savings goal."
        }
    }
    require(
        d.templates.size <= 100 && d.templates.map { it.id }.distinct().size == d.templates.size
    ) {
        "Invalid templates."
    }
    d.templates.forEach {
        require(it.name.isNotBlank() && it.name.length <= 100) { "Name your template." }
        entry(it.entry)
    }
    require(d.deleted.size <= 200 && d.deleted.all { validDate(it.deletedOn) }) {
        "Invalid deleted entries."
    }
    d.deleted.forEach { entry(it.entry) }
    d.entryDraft?.let { draft ->
        require(
            draft.id.isNotBlank() &&
                draft.id.length <= 80 &&
                draft.type in listOf("income", "expense", "transfer") &&
                draft.title.length <= 100 &&
                draft.amount.length <= 32 &&
                draft.note.length <= 1000 &&
                draft.date.length <= 10 &&
                draft.splits.size <= 30 &&
                draft.splits.all { it.category.length <= 40 && it.amount.length <= 32 } &&
                (draft.sourceId == null || d.transactions.any { it.id == draft.sourceId })
        ) {
            "Invalid saved draft."
        }
    }
    require(d.lastBackupDate == null || validDate(d.lastBackupDate)) { "Invalid backup date." }
}

fun BudgetData.renameCategory(old: String, new: String): BudgetData {
    require(old != FEE_CATEGORY || new.trim() == old) {
        "The transaction-fee category keeps its name so automatic fee reporting stays consistent."
    }
    val name = new.trim()
    require(name == old || name !in allCategories()) { "That category already exists." }
    fun rename(e: Entry) =
        e.copy(
            category = if (e.category == old) name else e.category,
            splits = e.splits.map { if (it.category == old) it.copy(category = name) else it },
        )
    val setting =
        categorySettings.find { it.name == old }
            ?: CategorySetting(old, if (old in categories) old else "Other")
    // Keep built-in names archived after a rename so existing backups remain meaningful.
    val settings =
        categorySettings.filterNot { it.name == old } +
            setting.copy(name = name) +
            if (old in categories && old != name) listOf(CategorySetting(old, old, true))
            else emptyList()
    return copy(
        categorySettings = settings,
        transactions = transactions.map(::rename),
        recurring = recurring.map { if (it.category == old) it.copy(category = name) else it },
        templates = templates.map { it.copy(entry = rename(it.entry)) },
        deleted = deleted.map { it.copy(entry = rename(it.entry)) },
        budgets =
            budgets.mapValues { (_, limits) ->
                limits.mapKeys { if (it.key == old) name else it.key }
            },
        entryDraft =
            entryDraft?.let { draft ->
                draft.copy(
                    category = if (draft.category == old) name else draft.category,
                    splits =
                        draft.splits.map {
                            if (it.category == old) it.copy(category = name) else it
                        },
                )
            },
        lastExpenseCategory = if (lastExpenseCategory == old) name else lastExpenseCategory,
        lastIncomeCategory = if (lastIncomeCategory == old) name else lastIncomeCategory,
    )
}

fun BudgetData.deleteEntries(ids: Set<String>): BudgetData {
    val removed = transactions.filter { it.id in ids }
    return copy(
        entryDraft = entryDraft?.takeUnless { it.sourceId in ids },
        transactions = transactions.filterNot { it.id in ids },
        deleted = (deleted + removed.map { DeletedEntry(it, today().toString()) }).takeLast(200),
    )
}

fun BudgetData.undoDelete(ids: Set<String>): BudgetData {
    val restore =
        deleted
            .filter { it.entry.id in ids && transactions.none { e -> e.id == it.entry.id } }
            .map { it.entry }
    return copy(
        transactions = transactions + restore,
        deleted = deleted.filterNot { it.entry.id in ids },
    )
}

fun nextOccurrence(r: Recurring): String {
    val d = LocalDate.parse(r.nextDate)
    if (r.frequency == "weekly") return d.plus(7, DateTimeUnit.DAY).toString()
    val target = d.plus(if (r.frequency == "yearly") 12 else 1, DateTimeUnit.MONTH)
    val first = LocalDate(target.year, target.month, 1)
    val last = first.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).day
    return LocalDate(target.year, target.month, minOf(r.anchorDay, last)).toString()
}

fun BudgetData.payScheduled(id: String): BudgetData {
    val r = recurring.first { it.id == id }
    require(r.active && r.nextDate <= today().toString()) { "This payment is not due yet." }
    val entryId = "scheduled-${r.id}-${r.nextDate}"
    require(transactions.none { it.id == entryId }) { "That occurrence has already been recorded." }
    return withEntry(
            Entry(
                entryId,
                r.title,
                r.amount,
                r.type,
                r.category,
                r.nextDate,
                r.accountId,
                fee = r.fee?.copy(confirmed = false),
            )
        )
        .copy(
            recurring =
                recurring.map { if (it.id == id) it.copy(nextDate = nextOccurrence(r)) else it }
        )
}

fun BudgetData.period(anchor: LocalDate): BudgetPeriod {
    val start =
        when (budgetCycle) {
            "weekly" -> anchor.minus(anchor.dayOfWeek.ordinal, DateTimeUnit.DAY)
            "payday" ->
                LocalDate(anchor.year, anchor.month, payday).let {
                    if (anchor.day < payday) it.minus(1, DateTimeUnit.MONTH) else it
                }
            else -> LocalDate(anchor.year, anchor.month, 1)
        }
    val end =
        if (budgetCycle == "weekly") start.plus(7, DateTimeUnit.DAY)
        else start.plus(1, DateTimeUnit.MONTH)
    return BudgetPeriod(
        start,
        end,
        if (budgetCycle == "monthly") start.toString().take(7)
        else "$budgetCycle|${if (budgetCycle == "weekly") 1 else payday}|$start",
    )
}

fun BudgetData.periodForMonth(month: String): BudgetPeriod =
    period(
        if (month == currentMonth()) today()
        else LocalDate.parse("$month-${payday.toString().padStart(2, '0')}")
    )

fun periodFromKey(key: String): BudgetPeriod {
    if ('|' !in key) {
        val d = LocalDate.parse("$key-01")
        return BudgetPeriod(d, d.plus(1, DateTimeUnit.MONTH), key)
    }
    val p = key.split('|')
    val d = LocalDate.parse(p[2])
    return BudgetPeriod(
        d,
        d.plus(
            if (p[0] == "weekly") 7 else 1,
            if (p[0] == "weekly") DateTimeUnit.DAY else DateTimeUnit.MONTH,
        ),
        key,
    )
}

fun BudgetData.periodSpending(p: BudgetPeriod, category: String): Long =
    transactions.filter { p.contains(it.date) }.sumOf { it.spentIn(category) }

fun BudgetData.limitsFor(p: BudgetPeriod): Map<String, Long> {
    val base = budgets[p.key].orEmpty()
    if (!rollover) return base
    val prefix = if ('|' in p.key) p.key.substringBeforeLast('|') + "|" else ""
    val planned =
        budgets
            .filterKeys { if (prefix.isEmpty()) '|' !in it else it.startsWith(prefix) }
            .filterKeys { periodFromKey(it).start < p.start }
    if (planned.isEmpty()) return base
    val first = planned.keys.minOf { periodFromKey(it).start }
    val earlier =
        transactions
            .filter { it.date >= first.toString() && it.date < p.start.toString() }
            .groupBy { period(LocalDate.parse(it.date)).key }
    val events = (planned.keys + earlier.keys).distinct().sortedBy { periodFromKey(it).start }
    val names = (base.keys + planned.values.flatMap { it.keys }).distinct()
    return names
        .associateWith { category ->
            var carry = 0L
            events.forEach { key ->
                carry =
                    (carry + (planned[key]?.get(category) ?: 0L) -
                            earlier[key].orEmpty().sumOf { it.spentIn(category) })
                        .coerceAtLeast(0L)
            }
            (base[category] ?: 0L) + carry
        }
        .filterValues { it > 0 }
}

fun BudgetData.alerts(): List<Pair<String, String>> {
    val p = period(today())
    val limits = limitsFor(p)
    val budgetAlerts =
        limits.mapNotNull { (c, limit) ->
            val spent = periodSpending(p, c)
            if (spent * 100 / limit >= 80)
                "$c ${if (spent >= limit) "over budget" else "near its limit"}" to
                    "${money(spent, currency)} of ${money(limit, currency)}"
            else null
        }
    val due =
        recurring
            .filter { it.active && it.nextDate <= today().plus(3, DateTimeUnit.DAY).toString() }
            .map { it.title to "${money(it.amount, currency)} · due ${friendlyDate(it.nextDate)}" }
    val backup =
        if (
            backupReminder &&
                (lastBackupDate == null || daysBetween(lastBackupDate, today().toString()) >= 7)
        )
            listOf(
                "Back up your ledger" to
                    "Keep a portable copy of your transactions, budgets, and settings."
            )
        else emptyList()
    return budgetAlerts + due + backup
}

expect fun calculateAmount(expression: String): Long

@Serializable data class SplitDraft(val category: String, val amount: String)

@Serializable
data class EntryDraft(
    val id: String = newId(),
    val sourceId: String? = null,
    val type: String = "expense",
    val title: String = "",
    val amount: String = "",
    val category: String = "Other",
    val date: String = today().toString(),
    val accountId: String = "main",
    val targetAccountId: String? = null,
    val note: String = "",
    val splits: List<SplitDraft> = emptyList(),
    val cleared: Boolean = false,
    val feeEnabled: Boolean = false,
    val feeKind: String = "Send to M-Pesa",
    val feeInput: String = "",
    val feeAutomatic: Boolean = true,
    val feeConfirmed: Boolean = false,
)

fun BudgetData.restoreEntries(snapshot: List<Entry>): BudgetData {
    val existing = transactions.map { it.id }.toSet()
    val ids = snapshot.map { it.id }.toSet()
    return copy(
        transactions = transactions + snapshot.filterNot { it.id in existing },
        deleted = deleted.filterNot { it.entry.id in ids },
    )
}

fun BudgetData.acceptDraft(draft: EntryDraft?): BudgetData {
    if (draft == null) return copy(entryDraft = null)
    val existing = transactions.firstOrNull { it.id == draft.id }
    if (draft.sourceId == null && existing != null) return this
    if (draft.sourceId != null && transactions.none { it.id == draft.sourceId }) return this
    val matches =
        existing != null &&
            existing.title == draft.title.trim() &&
            existing.amount == runCatching { parseAmount(draft.amount) }.getOrNull() &&
            existing.type == draft.type &&
            existing.category == draft.category &&
            existing.date == draft.date &&
            existing.accountId == draft.accountId &&
            existing.targetAccountId == draft.targetAccountId &&
            existing.note == draft.note &&
            existing.cleared == draft.cleared &&
            existing.splits.size == draft.splits.size &&
            existing.splits ==
                draft.splits.mapNotNull { split ->
                    runCatching { Split(split.category, parseAmount(split.amount)) }.getOrNull()
                } &&
            (existing.fee != null) == draft.feeEnabled &&
            (!draft.feeEnabled ||
                (existing.fee?.amount == runCatching { zeroOrAmount(draft.feeInput) }.getOrNull() &&
                    existing.fee?.kind == draft.feeKind &&
                    existing.fee?.confirmed == draft.feeConfirmed))
    return if (matches) copy(entryDraft = null) else copy(entryDraft = draft)
}
