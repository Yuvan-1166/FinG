package com.example.domain.gold

import java.math.BigDecimal
import java.math.RoundingMode

enum class GoldPurity(
    val label: String,
    val shortLabel: String,
    val finenessLabel: String,
    val exactNumerator: Int?,
    val exactDenominator: Int?,
    val defaultPercentText: String,
) {
    K24("24K / 999", "24K", "999", 24, 24, "100"),
    K22("22K / 916", "22K", "916", 22, 24, "91.6667"),
    K18("18K / 750", "18K", "750", 18, 24, "75"),
    K14("14K / 585", "14K", "585", 14, 24, "58.3333"),
    CUSTOM("Custom Purity", "Custom", "Custom", null, null, "91.6"),
}

enum class GoldItemStatus(val label: String) {
    ACTIVE("Active"),
    PARTIALLY_SOLD("Partially Sold"),
    SOLD("Sold"),
    ARCHIVED("Archived"),
}

enum class GoldObligationDirection(val label: String) {
    INCOMING("Incoming Gold"),
    OUTGOING("Outgoing Gold"),
}

enum class GoldObligationStatus(val label: String) {
    OPEN("Pending"),
    PARTIAL("Partially Settled"),
    SETTLED("Settled"),
    OVERDUE("Overdue"),
    CANCELLED("Cancelled"),
}

enum class GoldRateSource(val label: String) {
    NEBULA_METALS("NebulaMetals"),
    MANUAL("Manual Entry"),
}

enum class GoldValuationMethod(val label: String) {
    FINE_GOLD_24K("Fine Gold × 24K Rate"),
    CORRESPONDING_PURITY("Net Weight × Purity Rate"),
}

object WeightFormatter {
    const val MCG_PER_GRAM = 1_000_000L
    private val MCG_BD = BigDecimal(MCG_PER_GRAM)

    fun gramsToMicrograms(grams: BigDecimal): Long {
        return grams.multiply(MCG_BD).setScale(0, RoundingMode.HALF_UP).toLong()
    }

    fun microgramsToGrams(mcg: Long): BigDecimal {
        return BigDecimal(mcg).divide(MCG_BD, 6, RoundingMode.HALF_UP)
    }

    fun parseGramsToMicrograms(input: String): Long? {
        val cleaned = input.trim().replace(",", "").replace("g", "", ignoreCase = true).trim()
        if (cleaned.isEmpty()) return null
        val bd = runCatching { BigDecimal(cleaned) }.getOrNull() ?: return null
        if (bd < BigDecimal.ZERO) return null
        val scaled = bd.multiply(MCG_BD).setScale(0, RoundingMode.HALF_UP)
        if (scaled > BigDecimal(Long.MAX_VALUE)) return null
        return scaled.toLong()
    }

    fun formatGrams(mcg: Long, decimals: Int = 3, includeUnit: Boolean = true): String {
        val bd = microgramsToGrams(mcg).setScale(decimals, RoundingMode.HALF_UP).stripTrailingZeros()
        val text = if (bd.compareTo(BigDecimal.ZERO) == 0) "0" else bd.toPlainString()
        return if (includeUnit) "$text g" else text
    }

    fun formatFineGrams(mcg: Long): String {
        val bd = microgramsToGrams(mcg).setScale(6, RoundingMode.HALF_UP).stripTrailingZeros()
        val text = if (bd.compareTo(BigDecimal.ZERO) == 0) "0" else bd.toPlainString()
        return "$text g"
    }

    fun plainInput(mcg: Long): String {
        if (mcg == 0L) return "0"
        return microgramsToGrams(mcg).stripTrailingZeros().toPlainString()
    }
}
