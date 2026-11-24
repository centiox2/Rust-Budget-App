package com.centiox.verdant

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*

@Composable
fun FeeReport(data: BudgetData, onExport: (String) -> Unit = {}) {
    var month by rememberSaveable { mutableStateOf(currentMonth()) }
    var all by rememberSaveable { mutableStateOf(false) }
    var kind by rememberSaveable { mutableStateOf("All types") }
    val records =
        data.transactions.filter {
            it.fee != null &&
                (all || it.date.startsWith(month)) &&
                (kind == "All types" || it.fee.kind == kind)
        }
    val buckets = feeBreakdown(records)
    val confirmed = records.filter { it.fee!!.confirmed }.sumOf { it.fee!!.amount }
    val estimated = records.filterNot { it.fee!!.confirmed }.sumOf { it.fee!!.amount }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("M-Pesa & transaction fees", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        ToggleRow("All time", "Turn off to inspect one month.", all, { all = it })
        if (!all) MonthPicker(month, { month = it })
        ChoiceField("Transaction type", kind, listOf("All types") + feeKinds, { kind = it })
        Text("Confirmed fees", fontSize = 12.sp, color = Muted)
        AmountText(
            displayMoney(confirmed, data.currency),
            Modifier.fillMaxWidth(),
            fontSize = 30.sp,
            color = ExpenseRed,
        )
        Text(
            "${records.count { it.fee!!.confirmed }} confirmed transactions · ${records.count { !it.fee!!.confirmed }} awaiting fee confirmation",
            fontSize = 11.sp,
            color = Muted,
        )
        Text(
            "Reference estimates: ${displayMoney(estimated, data.currency)}",
            fontSize = 12.sp,
            color = Muted,
        )
        Text(
            "Fees are counted once in spending. Transfers and cash withdrawals do not count the principal as an expense when recorded as transfers between your accounts.",
            fontSize = 11.sp,
            color = Muted,
        )
        if (buckets.isEmpty())
            EmptyState(
                "See the cost of moving money",
                "Add a transaction fee to an entry to start tracking it here.",
            )
        buckets.forEach { b ->
            ToolRow(
                b.kind,
                "Amount band: ${displayMoney(b.minimum, data.currency)} – ${displayMoney(b.maximum, data.currency)}",
            ) {
                Text(
                    "${b.count} transactions · ${displayMoney(b.principal, data.currency)} moved",
                    fontSize = 12.sp,
                )
                Text(
                    "Confirmed fees ${displayMoney(b.confirmed, data.currency)} · estimates ${displayMoney(b.estimated, data.currency)}",
                    fontSize = 11.sp,
                    color = Muted,
                )
                Text(
                    "Average fee ${displayMoney(b.total / b.count, data.currency)} · ${if (b.principal > 0) (b.total * 1000.0 / b.principal).toInt() / 10.0 else 0.0}% of amount moved",
                    fontSize = 11.sp,
                    color = Muted,
                )
            }
        }
        if (records.isNotEmpty())
            OutlinedButton({ onExport(exportFeeCsv(records)) }, Modifier.fillMaxWidth()) {
                Text("Export fee breakdown")
            }
        Text("Individual charges", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        records
            .asReversed()
            .sortedByDescending { it.date }
            .take(100)
            .forEach { e ->
                ToolRow(e.title, "${friendlyDate(e.date)} · ${e.fee!!.kind}") {
                    Text(
                        "Amount ${displayMoney(e.amount, data.currency)} · fee ${displayMoney(e.fee!!.amount, data.currency)}",
                        fontSize = 12.sp,
                    )
                    Text(
                        if (e.fee!!.confirmed) "Confirmed charge"
                        else "Unconfirmed reference estimate",
                        fontSize = 11.sp,
                        color = if (e.fee!!.confirmed) Muted else Orange,
                    )
                }
            }
        Text("Reference tariffs", fontSize = 15.sp)
        Text(
            "Offline reference, checked against published tariff reproductions on 24 Nov 2026. Always use your receipt or statement to confirm actual fees. Paybill depends on who pays; Pochi, fuel-station, and bank charges require manual entry. Historical charges are never recalculated when reference rates change.",
            fontSize = 11.sp,
            color = Muted,
        )
        val uri = LocalUriHandler.current
        TextButton({ uri.openUri(MPESA_SOURCE) }) { Text("Open Safaricom tariffs") }
    }
}

fun exportFeeCsv(entries: List<Entry>): String {
    fun q(v: String) = "\"" + v.replace("\"", "\"\"") + "\""
    val header =
        listOf(
            "fee_type",
            "band_min",
            "band_max",
            "transactions",
            "principal",
            "confirmed_fees",
            "estimated_fees",
        )
    return (listOf(header) +
            feeBreakdown(entries).map {
                listOf(
                    it.kind,
                    amountInput(it.minimum),
                    amountInput(it.maximum),
                    it.count.toString(),
                    amountInput(it.principal),
                    amountInput(it.confirmed),
                    amountInput(it.estimated),
                )
            })
        .joinToString("\r\n") { it.joinToString(",", transform = ::q) }
}
