package com.centiox.verdant

import kotlinx.serialization.Serializable

const val FEE_CATEGORY = "Transaction fees"
const val MPESA_TARIFF_REFERENCE = "Offline reference 2026-11-24; verify the actual charge"
const val MPESA_SOURCE = "https://www.safaricom.co.ke/personal/m-pesa/mpesa-charges"
val feeKinds =
    listOf(
        "Send to M-Pesa",
        "Send to other network",
        "Agent withdrawal",
        "ATM withdrawal",
        "Buy Goods till",
        "Buy Goods fuel station",
        "Paybill · I pay",
        "Paybill · shared",
        "Paybill · business pays",
        "Paybill · unknown tariff",
        "Pochi la Biashara",
        "Bank transfer",
        "Receive money",
        "Other / manual",
    )

@Serializable
data class TransactionFee(
    val amount: Long,
    val kind: String,
    val confirmed: Boolean = false,
    val tariff: String = MPESA_TARIFF_REFERENCE,
    val bandMin: Long = 0,
    val bandMax: Long = 0,
    val source: String = MPESA_SOURCE,
)

data class FeeBand(val minimum: Long, val maximum: Long, val send: Long, val agent: Long?)

// Minor units are used throughout. Bounds are inclusive; fractional shillings require an actual
// fee.
val mpesaBands =
    listOf(
        FeeBand(1, 49, 0, null),
        FeeBand(50, 100, 0, 11),
        FeeBand(101, 500, 7, 29),
        FeeBand(501, 1000, 13, 29),
        FeeBand(1001, 1500, 23, 29),
        FeeBand(1501, 2500, 33, 29),
        FeeBand(2501, 3500, 53, 52),
        FeeBand(3501, 5000, 57, 69),
        FeeBand(5001, 7500, 78, 87),
        FeeBand(7501, 10000, 90, 115),
        FeeBand(10001, 15000, 100, 167),
        FeeBand(15001, 20000, 105, 185),
        FeeBand(20001, 35000, 108, 197),
        FeeBand(35001, 50000, 108, 278),
        FeeBand(50001, 250000, 108, 309),
    )
private val paybillMax =
    listOf(
        49L,
        100,
        500,
        1000,
        1500,
        2500,
        3500,
        5000,
        7500,
        10000,
        15000,
        20000,
        25000,
        30000,
        35000,
        40000,
        45000,
        50000,
        70000,
        250000,
    )
private val paybillFull =
    listOf(0L, 0, 5, 10, 15, 20, 25, 34, 42, 48, 57, 62, 67, 72, 83, 99, 103, 108, 108, 108)
private val paybillShared =
    listOf(0L, 0, 5, 10, 10, 13, 16, 16, 17, 18, 18, 19, 20, 20, 21, 23, 23, 24, 24, 24)

fun quoteMpesaFee(kind: String, principal: Long): TransactionFee? {
    if (principal % 100 != 0L || principal !in 100..25_000_000) return null
    val shillings = principal / 100
    val band = mpesaBands.firstOrNull { shillings in it.minimum..it.maximum } ?: return null
    var minimum = band.minimum
    var maximum = band.maximum
    val charge =
        when (kind) {
            "Send to M-Pesa",
            "Send to other network" -> band.send
            "Agent withdrawal" -> band.agent ?: return null
            "ATM withdrawal" ->
                when (shillings) {
                    in 200..2500 -> {
                        minimum = 200
                        maximum = 2500
                        35L
                    }
                    in 2501..5000 -> {
                        minimum = 2501
                        maximum = 5000
                        69L
                    }
                    in 5001..10000 -> {
                        minimum = 5001
                        maximum = 10000
                        115L
                    }
                    in 10001..35000 -> {
                        minimum = 10001
                        maximum = 35000
                        203L
                    }
                    else -> return null
                }
            "Buy Goods till",
            "Receive money",
            "Paybill · business pays" -> 0L
            "Paybill · I pay",
            "Paybill · shared" -> {
                val index = paybillMax.indexOfFirst { shillings <= it }
                minimum = if (index == 0) 1 else paybillMax[index - 1] + 1
                maximum = paybillMax[index]
                if (kind == "Paybill · shared") paybillShared[index] else paybillFull[index]
            }
            else -> return null // Do not silently reuse expired Pochi promotions or guess fuel/bank
        // charges.
        }
    return TransactionFee(
        charge * 100,
        kind,
        false,
        MPESA_TARIFF_REFERENCE,
        minimum * 100,
        maximum * 100,
    )
}

fun manualFee(amount: Long, kind: String, principal: Long, confirmed: Boolean): TransactionFee {
    val band =
        mpesaBands.firstOrNull { principal >= it.minimum * 100 && principal <= it.maximum * 100 }
    return TransactionFee(
        amount,
        kind,
        confirmed,
        "Manually entered charge",
        band?.minimum?.times(100) ?: principal,
        band?.maximum?.times(100) ?: principal,
        "Receipt or statement",
    )
}

fun zeroOrAmount(text: String): Long =
    if (Regex("0+(?:[.,]0{0,2})?").matches(text.trim())) 0 else parseAmount(text)

data class FeeBucket(
    val kind: String,
    val minimum: Long,
    val maximum: Long,
    val count: Int,
    val principal: Long,
    val confirmed: Long,
    val estimated: Long,
) {
    val total
        get() = confirmed + estimated
}

fun feeBreakdown(entries: List<Entry>): List<FeeBucket> =
    entries
        .filter { it.fee != null }
        .groupBy { Triple(it.fee!!.kind, it.fee.bandMin, it.fee.bandMax) }
        .map { (key, rows) ->
            FeeBucket(
                key.first,
                key.second,
                key.third,
                rows.size,
                rows.sumOf { it.amount },
                rows.filter { it.fee!!.confirmed }.sumOf { it.fee!!.amount },
                rows.filterNot { it.fee!!.confirmed }.sumOf { it.fee!!.amount },
            )
        }
        .sortedWith(compareBy<FeeBucket> { it.kind }.thenBy { it.minimum })
