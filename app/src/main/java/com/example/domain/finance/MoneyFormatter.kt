package com.example.domain.finance

import com.example.domain.model.CurrencyDisplayMode
import kotlin.math.abs

/**
 * India-specific monetary formatter and parser operating strictly on Long paise (1 INR = 100 paise).
 * Zero floating-point arithmetic is used in financial calculations.
 */
object MoneyFormatter {

    fun format(
        paise: Long,
        showDecimals: Boolean = true,
        compact: Boolean = false,
        showPlusSign: Boolean = false,
    ): String {
        val negative = paise < 0
        val absPaise = if (paise == Long.MIN_VALUE) Long.MAX_VALUE else abs(paise)
        val signPrefix = when {
            negative -> "-"
            showPlusSign && paise > 0 -> "+"
            else -> ""
        }

        if (compact) {
            return "$signPrefix${formatCompactIndian(absPaise)}"
        }

        val rupees = absPaise / 100L
        val remPaise = absPaise % 100L
        val groupedRupees = formatIndianGrouping(rupees)
        return if (showDecimals) {
            val dec = remPaise.toString().padStart(2, '0')
            "${signPrefix}₹$groupedRupees.$dec"
        } else {
            val roundedRupees = if (remPaise >= 50L) rupees + 1L else rupees
            "${signPrefix}₹${formatIndianGrouping(roundedRupees)}"
        }
    }

    fun formatWithMode(
        paise: Long,
        mode: CurrencyDisplayMode,
        showDecimals: Boolean,
        showPlusSign: Boolean = false,
    ): String {
        return format(
            paise = paise,
            showDecimals = showDecimals,
            compact = mode == CurrencyDisplayMode.COMPACT_INR,
            showPlusSign = showPlusSign,
        )
    }

    fun formatIndianGrouping(rupees: Long): String {
        val raw = rupees.toString()
        if (raw.length <= 3) return raw
        val lastThree = raw.substring(raw.length - 3)
        val prefix = raw.substring(0, raw.length - 3)
        val sb = StringBuilder()
        var count = 0
        for (i in prefix.length - 1 downTo 0) {
            sb.append(prefix[i])
            count++
            if (count == 2 && i != 0) {
                sb.append(',')
                count = 0
            }
        }
        return "${sb.reverse()},$lastThree"
    }

    private fun formatCompactIndian(absPaise: Long): String {
        // Pure integer division & remainder formatting for Lakhs and Crores
        val crorePaise = 1_000_000_000L // 1,00,00,000 * 100
        val lakhPaise = 10_000_000L     // 1,00,000 * 100
        val thousandPaise = 100_000L    // 1,000 * 100

        return when {
            absPaise >= crorePaise -> {
                val whole = absPaise / crorePaise
                val tenths = ((absPaise % crorePaise) * 100L) / crorePaise
                "₹$whole.${tenths.toString().padStart(2, '0')}Cr"
            }
            absPaise >= lakhPaise -> {
                val whole = absPaise / lakhPaise
                val tenths = ((absPaise % lakhPaise) * 100L) / lakhPaise
                "₹$whole.${tenths.toString().padStart(2, '0')}L"
            }
            absPaise >= thousandPaise -> {
                val whole = absPaise / thousandPaise
                val tenths = ((absPaise % thousandPaise) * 10L) / thousandPaise
                "₹$whole.${tenths}K"
            }
            else -> {
                val rupees = absPaise / 100L
                val dec = (absPaise % 100L).toString().padStart(2, '0')
                if (absPaise % 100L == 0L) "₹$rupees" else "₹$rupees.$dec"
            }
        }
    }

    /**
     * Parses a user-entered INR string (e.g. "1250", "1,250.50", "₹500.5") to Long paise.
     * Returns null if invalid or overflowing.
     */
    fun parseRupeesToPaise(input: String): Long? {
        val cleaned = input.trim()
            .replace("₹", "")
            .replace(",", "")
            .replace(" ", "")
        if (cleaned.isEmpty()) return null
        val isNegative = cleaned.startsWith("-")
        val unsigned = if (isNegative || cleaned.startsWith("+")) cleaned.substring(1) else cleaned
        if (unsigned.isEmpty()) return null

        val parts = unsigned.split(".")
        if (parts.size > 2) return null

        val wholePartStr = parts[0].ifEmpty { "0" }
        val fracPartStr = if (parts.size == 2) parts[1] else ""
        if (!wholePartStr.all { it.isDigit() } || !fracPartStr.all { it.isDigit() }) return null
        if (fracPartStr.length > 2) return null

        val whole = wholePartStr.toLongOrNull() ?: return null
        val paisePart = when (fracPartStr.length) {
            0 -> 0L
            1 -> fracPartStr.toLong() * 10L
            2 -> fracPartStr.toLong()
            else -> return null
        }

        if (whole > (Long.MAX_VALUE - paisePart) / 100L) return null
        val total = whole * 100L + paisePart
        return if (isNegative) -total else total
    }

    fun paiseToPlainInput(paise: Long): String {
        val absPaise = abs(paise)
        val rupees = absPaise / 100L
        val rem = absPaise % 100L
        val sign = if (paise < 0) "-" else ""
        return if (rem == 0L) "$sign$rupees" else "$sign$rupees.${rem.toString().padStart(2, '0')}"
    }
}
