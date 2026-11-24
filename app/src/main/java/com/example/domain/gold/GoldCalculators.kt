package com.example.domain.gold

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

data class GoldHoldingsMetrics(
    val totalPhysicalNetMcg: Long,
    val weight24kMcg: Long,
    val weight22kMcg: Long,
    val weight18kMcg: Long,
    val weight14kAndOtherMcg: Long,
    val totalFineGoldMcg: Long,
    val total24kEquivalentMcg: Long,
)

data class GoldValuationBreakdown(
    val fineGoldMcg: Long,
    val equivalent24kMcg: Long,
    val benchmark24kValuePaise: Long,
    val purityBenchmarkValuePaise: Long,
    val estimatedResaleValuePaise: Long,
    val acquisitionCostPaise: Long,
    val unrealizedGainLossPaise: Long,
    val unrealizedGainLossBps: Int,
)

data class GoldPortfolioValuation(
    val holdings: GoldHoldingsMetrics,
    val totalAcquisitionCostPaise: Long,
    val currentBenchmarkValuePaise: Long,
    val currentPurityValuePaise: Long,
    val estimatedResaleValuePaise: Long,
    val unrealizedGainLossPaise: Long,
    val realizedGainLossPaise: Long,
)

data class GoldHoldingInput(
    val id: Long,
    val categoryId: Long,
    val purity: GoldPurity,
    val purityPercentText: String,
    val netWeightMcg: Long,
    val acquisitionCostPaise: Long,
    val status: GoldItemStatus,
)

data class GoldRateInput(
    val rate24kBasePaise: Long,
    val rate24kFinalPaise: Long,
    val rate22kFinalPaise: Long,
    val rate18kFinalPaise: Long,
    val rate14kFinalPaise: Long,
)

object GoldCalculator {
    private val MC = MathContext(18, RoundingMode.HALF_UP)
    private val HUNDRED = BigDecimal(100)
    private val MCG_PER_GRAM = BigDecimal(WeightFormatter.MCG_PER_GRAM)

    fun purityFraction(purity: GoldPurity, customPercentText: String): BigDecimal {
        if (purity != GoldPurity.CUSTOM && purity.exactNumerator != null && purity.exactDenominator != null) {
            return BigDecimal(purity.exactNumerator).divide(BigDecimal(purity.exactDenominator), MC)
        }
        val pct = runCatching { BigDecimal(customPercentText.trim()) }.getOrNull()
            ?.takeIf { it > BigDecimal.ZERO && it <= HUNDRED }
            ?: BigDecimal(purity.defaultPercentText)
        return pct.divide(HUNDRED, MC)
    }

    /**
     * Fine gold weight = Net gold weight * Purity fraction.
     * Example: 35 g * (22/24) = 32.083333 g -> 32_083_333 micrograms.
     */
    fun calculateFineGoldMicrograms(
        netWeightMcg: Long,
        purity: GoldPurity,
        customPercentText: String,
    ): Long {
        if (netWeightMcg <= 0L) return 0L
        val netGrams = WeightFormatter.microgramsToGrams(netWeightMcg)
        val fraction = purityFraction(purity, customPercentText)
        val fineGrams = netGrams.multiply(fraction, MC)
        return WeightFormatter.gramsToMicrograms(fineGrams)
    }

    /**
     * Base gold value in paise = (netWeightGrams * ratePerGramPaise), rounded HALF_UP to nearest paise.
     */
    fun calculateGoldValuePaise(netWeightMcg: Long, ratePerGramPaise: Long): Long {
        if (netWeightMcg <= 0L || ratePerGramPaise <= 0L) return 0L
        val grams = BigDecimal(netWeightMcg).divide(MCG_PER_GRAM, MC)
        return grams.multiply(BigDecimal(ratePerGramPaise), MC)
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()
    }

    /**
     * Allocates acquisition cost proportionally for partial gold sales.
     */
    fun allocateAcquisitionCostPaise(
        remainingAcquisitionCostPaise: Long,
        remainingNetWeightMcg: Long,
        soldNetWeightMcg: Long,
    ): Long {
        if (remainingAcquisitionCostPaise <= 0L || remainingNetWeightMcg <= 0L || soldNetWeightMcg <= 0L) return 0L
        if (soldNetWeightMcg >= remainingNetWeightMcg) return remainingAcquisitionCostPaise
        val ratio = BigDecimal(soldNetWeightMcg).divide(BigDecimal(remainingNetWeightMcg), MC)
        return BigDecimal(remainingAcquisitionCostPaise)
            .multiply(ratio, MC)
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()
            .coerceIn(0L, remainingAcquisitionCostPaise)
    }

    fun calculateHoldingsMetrics(items: List<GoldHoldingInput>): GoldHoldingsMetrics {
        var totalNet = 0L
        var w24 = 0L
        var w22 = 0L
        var w18 = 0L
        var wOther = 0L
        var totalFine = 0L

        for (item in items) {
            if (item.status == GoldItemStatus.SOLD || item.status == GoldItemStatus.ARCHIVED) continue
            if (item.netWeightMcg <= 0L) continue
            totalNet += item.netWeightMcg
            when (item.purity) {
                GoldPurity.K24 -> w24 += item.netWeightMcg
                GoldPurity.K22 -> w22 += item.netWeightMcg
                GoldPurity.K18 -> w18 += item.netWeightMcg
                GoldPurity.K14, GoldPurity.CUSTOM -> wOther += item.netWeightMcg
            }
            totalFine += calculateFineGoldMicrograms(item.netWeightMcg, item.purity, item.purityPercentText)
        }

        return GoldHoldingsMetrics(
            totalPhysicalNetMcg = totalNet,
            weight24kMcg = w24,
            weight22kMcg = w22,
            weight18kMcg = w18,
            weight14kAndOtherMcg = wOther,
            totalFineGoldMcg = totalFine,
            total24kEquivalentMcg = totalFine,
        )
    }

    fun rateForPurity(
        rates: GoldRateInput,
        purity: GoldPurity,
        customPercentText: String,
    ): Long {
        return when (purity) {
            GoldPurity.K24 -> rates.rate24kFinalPaise
            GoldPurity.K22 -> if (rates.rate22kFinalPaise > 0L) rates.rate22kFinalPaise
            else calculateDerivedRate(rates.rate24kFinalPaise, purity, customPercentText)
            GoldPurity.K18 -> if (rates.rate18kFinalPaise > 0L) rates.rate18kFinalPaise
            else calculateDerivedRate(rates.rate24kFinalPaise, purity, customPercentText)
            GoldPurity.K14 -> if (rates.rate14kFinalPaise > 0L) rates.rate14kFinalPaise
            else calculateDerivedRate(rates.rate24kFinalPaise, purity, customPercentText)
            GoldPurity.CUSTOM -> calculateDerivedRate(rates.rate24kFinalPaise, purity, customPercentText)
        }
    }

    private fun calculateDerivedRate(rate24kPaise: Long, purity: GoldPurity, customPercentText: String): Long {
        if (rate24kPaise <= 0L) return 0L
        val frac = purityFraction(purity, customPercentText)
        return BigDecimal(rate24kPaise).multiply(frac, MC).setScale(0, RoundingMode.HALF_UP).toLong()
    }

    /**
     * Valuates a single active gold holding.
     * - Benchmark value: Fine gold content * 24K benchmark rate.
     * - Corresponding purity value: Net gold weight * purity benchmark rate.
     * - Estimated realizable resale value: Fine gold * 24K base rate (ex-GST) less standard 2% melting/spread deduction.
     *   (Never assumes making charges, GST, or stones are recovered upon resale.)
     */
    fun valuateItem(
        item: GoldHoldingInput,
        rates: GoldRateInput?,
        resaleDeductionPercent: Int = 2,
    ): GoldValuationBreakdown {
        val fineMcg = calculateFineGoldMicrograms(item.netWeightMcg, item.purity, item.purityPercentText)
        if (rates == null || rates.rate24kFinalPaise <= 0L) {
            return GoldValuationBreakdown(
                fineGoldMcg = fineMcg,
                equivalent24kMcg = fineMcg,
                benchmark24kValuePaise = 0L,
                purityBenchmarkValuePaise = 0L,
                estimatedResaleValuePaise = 0L,
                acquisitionCostPaise = item.acquisitionCostPaise,
                unrealizedGainLossPaise = 0L,
                unrealizedGainLossBps = 0,
            )
        }

        val benchmark24kVal = calculateGoldValuePaise(fineMcg, rates.rate24kFinalPaise)
        val purityRate = rateForPurity(rates, item.purity, item.purityPercentText)
        val purityVal = calculateGoldValuePaise(item.netWeightMcg, purityRate)

        val base24k = if (rates.rate24kBasePaise > 0L) rates.rate24kBasePaise else rates.rate24kFinalPaise
        val grossMeltVal = calculateGoldValuePaise(fineMcg, base24k)
        val clampedDeduction = resaleDeductionPercent.coerceIn(0, 25)
        val resaleVal = (grossMeltVal * (100L - clampedDeduction)) / 100L

        val unrealized = benchmark24kVal - item.acquisitionCostPaise
        val bps = if (item.acquisitionCostPaise > 0L) {
            ((unrealized * 10000L) / item.acquisitionCostPaise).toInt()
        } else 0

        return GoldValuationBreakdown(
            fineGoldMcg = fineMcg,
            equivalent24kMcg = fineMcg,
            benchmark24kValuePaise = benchmark24kVal,
            purityBenchmarkValuePaise = purityVal,
            estimatedResaleValuePaise = resaleVal,
            acquisitionCostPaise = item.acquisitionCostPaise,
            unrealizedGainLossPaise = unrealized,
            unrealizedGainLossBps = bps,
        )
    }

    fun valuatePortfolio(
        items: List<GoldHoldingInput>,
        rates: GoldRateInput?,
        realizedGainLossPaise: Long,
        preferredMethod: GoldValuationMethod = GoldValuationMethod.FINE_GOLD_24K,
        resaleDeductionPercent: Int = 2,
    ): GoldPortfolioValuation {
        val active = items.filter {
            it.status != GoldItemStatus.SOLD && it.status != GoldItemStatus.ARCHIVED && it.netWeightMcg > 0L
        }
        val holdings = calculateHoldingsMetrics(active)
        var totalCost = 0L
        var totalBenchmark24k = 0L
        var totalPurityVal = 0L
        var totalResale = 0L

        for (item in active) {
            val v = valuateItem(item, rates, resaleDeductionPercent)
            totalCost += v.acquisitionCostPaise
            totalBenchmark24k += v.benchmark24kValuePaise
            totalPurityVal += v.purityBenchmarkValuePaise
            totalResale += v.estimatedResaleValuePaise
        }

        val effectiveMarketValue = when (preferredMethod) {
            GoldValuationMethod.FINE_GOLD_24K -> totalBenchmark24k
            GoldValuationMethod.CORRESPONDING_PURITY -> totalPurityVal
        }
        val unrealized = if (rates != null && rates.rate24kFinalPaise > 0L) {
            effectiveMarketValue - totalCost
        } else 0L

        return GoldPortfolioValuation(
            holdings = holdings,
            totalAcquisitionCostPaise = totalCost,
            currentBenchmarkValuePaise = totalBenchmark24k,
            currentPurityValuePaise = totalPurityVal,
            estimatedResaleValuePaise = totalResale,
            unrealizedGainLossPaise = unrealized,
            realizedGainLossPaise = realizedGainLossPaise,
        )
    }
}
