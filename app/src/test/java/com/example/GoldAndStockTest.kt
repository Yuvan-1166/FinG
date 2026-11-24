package com.example

import com.example.data.backup.DataTransferService
import com.example.data.gold.GoldMarketSchedule
import com.example.data.gold.NebulaMetalsClient
import com.example.data.local.AccountEntity
import com.example.data.local.GoldItemEntity
import com.example.data.repository.DatabaseSnapshot
import com.example.domain.finance.AccountBalanceCalculator
import com.example.domain.finance.AccountInput
import com.example.domain.finance.DashboardCalculator
import com.example.domain.finance.TransactionInput
import com.example.domain.gold.GoldCalculator
import com.example.domain.gold.GoldHoldingInput
import com.example.domain.gold.GoldItemStatus
import com.example.domain.gold.GoldPurity
import com.example.domain.gold.GoldRateInput
import com.example.domain.gold.GoldValuationMethod
import com.example.domain.gold.WeightFormatter
import com.example.domain.model.AccountType
import com.example.domain.model.TransactionType
import com.example.domain.stock.ShareFormatter
import com.example.domain.stock.StockCalculator
import com.example.domain.stock.StockTradeInput
import com.example.domain.stock.StockTxType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class GoldAndStockTest {

    @Test
    fun goldPurityConversion_35gOf22kEquals32_083333gFineGold() {
        val netMcg = 35L * WeightFormatter.MCG_PER_GRAM
        val fineMcg = GoldCalculator.calculateFineGoldMicrograms(netMcg, GoldPurity.K22, "91.6667")
        assertEquals(32_083_333L, fineMcg)
        assertEquals("32.083333 g", WeightFormatter.formatFineGrams(fineMcg))
    }

    @Test
    fun partialSale_allocatesAcquisitionCostProportionally() {
        val originalCost = 300_000_00L // ₹3,00,000
        val totalWeight = 40L * WeightFormatter.MCG_PER_GRAM // 40 g
        val soldWeight = 10L * WeightFormatter.MCG_PER_GRAM // 10 g (25%)
        val allocated = GoldCalculator.allocateAcquisitionCostPaise(originalCost, totalWeight, soldWeight)
        assertEquals(75_000_00L, allocated)
    }

    @Test
    fun goldPortfolioValuation_separatesBenchmarkPurityAndResale() {
        val items = listOf(
            GoldHoldingInput(
                id = 1L,
                categoryId = 1L,
                purity = GoldPurity.K22,
                purityPercentText = "91.6667",
                netWeightMcg = 12L * WeightFormatter.MCG_PER_GRAM,
                acquisitionCostPaise = 90_000_00L,
                status = GoldItemStatus.ACTIVE,
            ),
            GoldHoldingInput(
                id = 2L,
                categoryId = 7L,
                purity = GoldPurity.K24,
                purityPercentText = "100",
                netWeightMcg = 10L * WeightFormatter.MCG_PER_GRAM,
                acquisitionCostPaise = 75_000_00L,
                status = GoldItemStatus.ACTIVE,
            ),
            GoldHoldingInput(
                id = 3L,
                categoryId = 7L,
                purity = GoldPurity.K18,
                purityPercentText = "75",
                netWeightMcg = 8L * WeightFormatter.MCG_PER_GRAM,
                acquisitionCostPaise = 40_000_00L,
                status = GoldItemStatus.SOLD, // Excluded from active holdings
            ),
        )
        val rate = GoldRateInput(
            rate24kBasePaise = 8_000_00L, // ₹8,000/g ex-GST
            rate24kFinalPaise = 8_240_00L, // ₹8,240/g incl. GST
            rate22kFinalPaise = 7_553_00L,
            rate18kFinalPaise = 6_180_00L,
            rate14kFinalPaise = 4_807_00L,
        )

        val portfolio = GoldCalculator.valuatePortfolio(
            items = items,
            rates = rate,
            realizedGainLossPaise = 12_000_00L,
            preferredMethod = GoldValuationMethod.FINE_GOLD_24K,
        )

        assertEquals(22L * WeightFormatter.MCG_PER_GRAM, portfolio.holdings.totalPhysicalNetMcg)
        assertEquals(10L * WeightFormatter.MCG_PER_GRAM, portfolio.holdings.weight24kMcg)
        assertEquals(12L * WeightFormatter.MCG_PER_GRAM, portfolio.holdings.weight22kMcg)
        // 12g * 22/24 = 11g fine + 10g 24K = 21g fine gold
        assertEquals(21L * WeightFormatter.MCG_PER_GRAM, portfolio.holdings.totalFineGoldMcg)
        assertEquals(165_000_00L, portfolio.totalAcquisitionCostPaise)
        assertEquals(21L * 8_240_00L, portfolio.currentBenchmarkValuePaise)
        assertTrue(portfolio.estimatedResaleValuePaise < portfolio.currentBenchmarkValuePaise)
        assertEquals(12_000_00L, portfolio.realizedGainLossPaise)
    }

    @Test
    fun nebulaMetalsResponseParser_parsesAllPuritiesInPaise() {
        val sampleJson = """
            {
              "date": "2026-11-24",
              "city": "Chennai",
              "market": "Chennai Bullion",
              "timestamp": "2026-11-24T10:05:00+05:30",
              "prices": {
                "gold24k": { "basePricePerGram": 8000, "gstAmountPerGram": 240, "finalPricePerGram": 8240, "currency": "INR" },
                "gold22k": { "basePricePerGram": 7333, "gstAmountPerGram": 220, "finalPricePerGram": 7553, "currency": "INR" },
                "gold18k": { "basePricePerGram": 6000, "gstAmountPerGram": 180, "finalPricePerGram": 6180, "currency": "INR" },
                "gold14k": { "basePricePerGram": 4667, "gstAmountPerGram": 140, "finalPricePerGram": 4807, "currency": "INR" }
              },
              "metadata": { "gst_included": true, "sources_used": 4, "confidence": "high" }
            }
        """.trimIndent()

        val snapshot = NebulaMetalsClient.parseNebulaMetalsResponse(sampleJson)
        assertEquals("Chennai", snapshot.city)
        assertEquals("2026-11-24", snapshot.rateDateIso)
        assertEquals(8000_00L, snapshot.rate24kBasePaise)
        assertEquals(240_00L, snapshot.rate24kGstPaise)
        assertEquals(8240_00L, snapshot.rate24kFinalPaise)
        assertEquals(7553_00L, snapshot.rate22kFinalPaise)
        assertEquals(6180_00L, snapshot.rate18kFinalPaise)
        assertEquals(4807_00L, snapshot.rate14kFinalPaise)
        assertTrue(snapshot.gstIncluded)
        assertEquals(4, snapshot.sourcesUsed)
    }

    @Test
    fun goldMarketSchedule_enforcesIstWeekdayAfter10Am() {
        val ist = ZoneId.of("Asia/Kolkata")
        // Tuesday 09:45 AM IST -> NOT eligible
        val tueMorning = ZonedDateTime.of(2026, 11, 24, 9, 45, 0, 0, ist)
        assertFalse(GoldMarketSchedule.evaluateFetchWindow(tueMorning).isEligibleNow)

        // Tuesday 10:15 AM IST -> Eligible
        val tueAfterTen = ZonedDateTime.of(2026, 11, 24, 10, 15, 0, 0, ist)
        assertTrue(GoldMarketSchedule.evaluateFetchWindow(tueAfterTen).isEligibleNow)

        // Saturday 12:00 PM IST -> Weekend NOT eligible
        val saturday = ZonedDateTime.of(2026, 11, 28, 12, 0, 0, 0, ist)
        assertFalse(GoldMarketSchedule.evaluateFetchWindow(saturday).isEligibleNow)
    }

    @Test
    fun stockCalculator_weightedAverageCostAndPartialSale() {
        val qty10 = ShareFormatter.parseSharesToScale4("10")!!
        val qty5 = ShareFormatter.parseSharesToScale4("5")!!
        val qty6 = ShareFormatter.parseSharesToScale4("6")!!

        val position = StockCalculator.replayPosition(
            listOf(
                StockTradeInput(StockTxType.BUY, qty10, 1_000_00L, 0L, 1L),
                StockTradeInput(StockTxType.BUY, qty5, 1_300_00L, 0L, 2L),
                StockTradeInput(StockTxType.SELL, qty6, 1_500_00L, 0L, 3L),
            )
        )
        // Total cost before sell: 10*1000 + 5*1300 = 16,500 across 15 shares -> avg ₹1,100
        // Sell 6 shares @ 1500 = 9,000 proceeds - 6,600 cost basis = +2,400 realized P&L
        assertEquals(9L * ShareFormatter.SCALE4, position.quantityScale4)
        assertEquals(1_100_00L, position.averageCostPaise)
        assertEquals(9_900_00L, position.totalInvestedPaise)
        assertEquals(2_400_00L, position.realizedGainLossPaise)
    }

    @Test
    fun assetTransactions_updateBankBalanceWithoutInflatingExpensesOrIncome() {
        val bank = AccountInput(1L, "HDFC", AccountType.SAVINGS, 100_000_00L, false)
        val day = LocalDate.of(2026, 11, 10)
        val ts = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val txs = listOf(
            TransactionInput(1L, TransactionType.INCOME, 50_000_00L, ts, 1L, null, 1L, null, false),
            TransactionInput(2L, TransactionType.EXPENSE, 10_000_00L, ts, 1L, null, 2L, null, false),
            TransactionInput(3L, TransactionType.ASSET_PURCHASE, 30_000_00L, ts, 1L, null, null, null, false),
            TransactionInput(4L, TransactionType.ASSET_SALE, 15_000_00L, ts, 1L, null, null, null, false),
        )

        val balances = AccountBalanceCalculator.calculateBalances(listOf(bank), txs)
        // 100k + 50k - 10k - 30k + 15k = 125k
        assertEquals(125_000_00L, balances[1L])

        val summary = DashboardCalculator.calculateSummary(
            accounts = listOf(bank),
            accountBalances = balances,
            periodTransactions = txs,
        )
        // Asset purchases and sales MUST NOT inflate ordinary income/expense metrics
        assertEquals(50_000_00L, summary.totalIncomePaise)
        assertEquals(10_000_00L, summary.grossExpensePaise)
    }

    @Test
    fun databaseSnapshotJson_roundTripsGoldItemsAndBackwardsCompatibleV1() {
        val snapshot = DatabaseSnapshot(
            schemaVersion = 2,
            accounts = listOf(AccountEntity(id = 1L, name = "Bank", type = "SAVINGS", openingBalancePaise = 50_000_00L)),
            goldItems = listOf(
                GoldItemEntity(
                    id = 10L,
                    title = "22K Coin",
                    categoryId = 7L,
                    purity = GoldPurity.K22.name,
                    purityPercentText = "91.6667",
                    grossWeightMcg = 8_000_000L,
                    netWeightMcg = 8_000_000L,
                    originalNetWeightMcg = 8_000_000L,
                    acquisitionCostPaise = 60_000_00L,
                    originalAcquisitionCostPaise = 60_000_00L,
                    purchaseDateEpochDay = 20000L,
                )
            ),
        )
        val raw = DataTransferService.exportSnapshotToJson(snapshot)
        val parsed = DataTransferService.parseSnapshotFromJson(raw).getOrThrow()
        assertEquals(1, parsed.goldItems.size)
        assertEquals("22K Coin", parsed.goldItems.first().title)

        // Old v1 JSON without gold fields still parses cleanly
        val v1Json = """{"schemaVersion":1,"exportedAt":1700000000000,"accounts":[],"categories":[]}"""
        val v1Parsed = DataTransferService.parseSnapshotFromJson(v1Json).getOrThrow()
        assertTrue(v1Parsed.goldItems.isEmpty())
    }
}
