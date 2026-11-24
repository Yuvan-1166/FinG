package com.example.domain.stock

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

enum class StockExchange(val label: String) {
    NSE("NSE"),
    BSE("BSE"),
}

enum class StockTxType(val label: String) {
    BUY("Buy"),
    SELL("Sell"),
}

object ShareFormatter {
    const val SCALE4 = 10_000L
    private val SCALE4_BD = BigDecimal(SCALE4)

    fun parseSharesToScale4(input: String): Long? {
        val cleaned = input.trim().replace(",", "")
        if (cleaned.isEmpty()) return null
        val bd = runCatching { BigDecimal(cleaned) }.getOrNull() ?: return null
        if (bd <= BigDecimal.ZERO) return null
        val scaled = bd.multiply(SCALE4_BD).setScale(0, RoundingMode.HALF_UP)
        if (scaled > BigDecimal(Long.MAX_VALUE)) return null
        return scaled.toLong()
    }

    fun formatShares(quantityScale4: Long): String {
        val bd = BigDecimal(quantityScale4).divide(SCALE4_BD, 4, RoundingMode.HALF_UP).stripTrailingZeros()
        return if (bd.compareTo(BigDecimal.ZERO) == 0) "0" else bd.toPlainString()
    }
}

data class StockTradeInput(
    val type: StockTxType,
    val quantityScale4: Long,
    val pricePerSharePaise: Long,
    val chargesPaise: Long,
    val tradeDateEpochDay: Long,
)

data class ComputedStockPosition(
    val quantityScale4: Long,
    val averageCostPaise: Long,
    val totalInvestedPaise: Long,
    val realizedGainLossPaise: Long,
)

data class StockSaleAllocation(
    val allocatedCostBasisPaise: Long,
    val netProceedsPaise: Long,
    val realizedGainLossPaise: Long,
)

object StockCalculator {
    private val MC = MathContext(18, RoundingMode.HALF_UP)
    private val SCALE4_BD = BigDecimal(ShareFormatter.SCALE4)

    fun calculateTradeTotalPaise(
        type: StockTxType,
        quantityScale4: Long,
        pricePerSharePaise: Long,
        chargesPaise: Long,
    ): Long {
        val shares = BigDecimal(quantityScale4).divide(SCALE4_BD, MC)
        val gross = shares.multiply(BigDecimal(pricePerSharePaise), MC).setScale(0, RoundingMode.HALF_UP).toLong()
        return when (type) {
            StockTxType.BUY -> gross + chargesPaise
            StockTxType.SELL -> (gross - chargesPaise).coerceAtLeast(0L)
        }
    }

    fun calculateSaleAllocation(
        currentQtyScale4: Long,
        currentTotalInvestedPaise: Long,
        soldQtyScale4: Long,
        sellPricePerSharePaise: Long,
        chargesPaise: Long,
    ): StockSaleAllocation {
        require(soldQtyScale4 > 0L && soldQtyScale4 <= currentQtyScale4) {
            "Cannot sell more shares than currently held."
        }
        val allocatedCost = if (soldQtyScale4 == currentQtyScale4) {
            currentTotalInvestedPaise
        } else {
            val ratio = BigDecimal(soldQtyScale4).divide(BigDecimal(currentQtyScale4), MC)
            BigDecimal(currentTotalInvestedPaise).multiply(ratio, MC)
                .setScale(0, RoundingMode.HALF_UP)
                .toLong()
                .coerceIn(0L, currentTotalInvestedPaise)
        }
        val proceeds = calculateTradeTotalPaise(StockTxType.SELL, soldQtyScale4, sellPricePerSharePaise, chargesPaise)
        return StockSaleAllocation(
            allocatedCostBasisPaise = allocatedCost,
            netProceedsPaise = proceeds,
            realizedGainLossPaise = proceeds - allocatedCost,
        )
    }

    fun replayPosition(trades: List<StockTradeInput>): ComputedStockPosition {
        var qtyScale4 = 0L
        var totalCostPaise = 0L
        var realizedPaise = 0L

        for (trade in trades.sortedBy { it.tradeDateEpochDay }) {
            when (trade.type) {
                StockTxType.BUY -> {
                    val cost = calculateTradeTotalPaise(
                        StockTxType.BUY,
                        trade.quantityScale4,
                        trade.pricePerSharePaise,
                        trade.chargesPaise,
                    )
                    qtyScale4 += trade.quantityScale4
                    totalCostPaise += cost
                }
                StockTxType.SELL -> {
                    if (qtyScale4 <= 0L) continue
                    val sellQty = trade.quantityScale4.coerceAtMost(qtyScale4)
                    val alloc = calculateSaleAllocation(
                        currentQtyScale4 = qtyScale4,
                        currentTotalInvestedPaise = totalCostPaise,
                        soldQtyScale4 = sellQty,
                        sellPricePerSharePaise = trade.pricePerSharePaise,
                        chargesPaise = trade.chargesPaise,
                    )
                    qtyScale4 -= sellQty
                    totalCostPaise -= alloc.allocatedCostBasisPaise
                    realizedPaise += alloc.realizedGainLossPaise
                }
            }
        }

        val avgCost = if (qtyScale4 > 0L) {
            val shares = BigDecimal(qtyScale4).divide(SCALE4_BD, MC)
            BigDecimal(totalCostPaise).divide(shares, MC).setScale(0, RoundingMode.HALF_UP).toLong()
        } else 0L

        return ComputedStockPosition(
            quantityScale4 = qtyScale4,
            averageCostPaise = avgCost,
            totalInvestedPaise = totalCostPaise,
            realizedGainLossPaise = realizedPaise,
        )
    }

    fun calculateMarketValuePaise(quantityScale4: Long, marketPricePerSharePaise: Long): Long {
        if (quantityScale4 <= 0L || marketPricePerSharePaise <= 0L) return 0L
        val shares = BigDecimal(quantityScale4).divide(SCALE4_BD, MC)
        return shares.multiply(BigDecimal(marketPricePerSharePaise), MC)
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()
    }
}
