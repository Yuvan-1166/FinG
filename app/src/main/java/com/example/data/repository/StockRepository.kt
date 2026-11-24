package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.RupeeLedgerDatabase
import com.example.data.local.StockDao
import com.example.data.local.StockEntity
import com.example.data.local.StockHoldingEntity
import com.example.data.local.StockPriceSnapshotEntity
import com.example.data.local.StockTransactionEntity
import com.example.data.local.TransactionDao
import com.example.data.local.TransactionEntity
import com.example.domain.model.TransactionType
import com.example.domain.stock.ShareFormatter
import com.example.domain.stock.StockCalculator
import com.example.domain.stock.StockExchange
import com.example.domain.stock.StockTradeInput
import com.example.domain.stock.StockTxType
import kotlinx.coroutines.flow.Flow

class StockRepository(
    private val db: RupeeLedgerDatabase,
    private val stockDao: StockDao = db.stockDao(),
    private val transactionDao: TransactionDao = db.transactionDao(),
) {
    val stocksFlow: Flow<List<StockEntity>> = stockDao.observeStocks()
    val holdingsFlow: Flow<List<StockHoldingEntity>> = stockDao.observeHoldings()
    val transactionsFlow: Flow<List<StockTransactionEntity>> = stockDao.observeTransactions()
    val priceSnapshotsFlow: Flow<List<StockPriceSnapshotEntity>> = stockDao.observePriceSnapshots()

    suspend fun recordStockTrade(
        symbol: String,
        companyName: String,
        exchange: StockExchange,
        sector: String,
        type: StockTxType,
        quantityScale4: Long,
        pricePerSharePaise: Long,
        chargesPaise: Long,
        tradeDateEpochDay: Long,
        accountId: Long?,
        notes: String,
    ): Long {
        val cleanSymbol = symbol.trim().uppercase()
        require(cleanSymbol.isNotEmpty()) { "Stock ticker symbol is required." }
        require(quantityScale4 > 0L) { "Share quantity must be positive." }
        require(pricePerSharePaise > 0L) { "Share price must be positive." }

        return db.withTransaction {
            val now = System.currentTimeMillis()
            val existingStock = stockDao.findStock(cleanSymbol, exchange.name)
            val stockId = if (existingStock != null) {
                if (companyName.isNotBlank() && existingStock.companyName != companyName.trim()) {
                    stockDao.updateStock(
                        existingStock.copy(
                            companyName = companyName.trim(),
                            sector = sector.ifBlank { existingStock.sector },
                            isArchived = false,
                        )
                    )
                }
                existingStock.id
            } else {
                require(type == StockTxType.BUY) { "Buy shares of $cleanSymbol before recording a sale." }
                stockDao.insertStock(
                    StockEntity(
                        symbol = cleanSymbol,
                        companyName = companyName.ifBlank { cleanSymbol },
                        exchange = exchange.name,
                        sector = sector.trim(),
                        createdAt = now,
                    )
                )
            }

            val existingHolding = stockDao.getHoldingForStock(stockId)
            var txRealized = 0L
            if (type == StockTxType.SELL) {
                val heldQty = existingHolding?.quantityScale4 ?: 0L
                require(heldQty >= quantityScale4) {
                    "Cannot sell ${ShareFormatter.formatShares(quantityScale4)} shares; only ${ShareFormatter.formatShares(heldQty)} held."
                }
                val alloc = StockCalculator.calculateSaleAllocation(
                    currentQtyScale4 = heldQty,
                    currentTotalInvestedPaise = existingHolding?.totalInvestedPaise ?: 0L,
                    soldQtyScale4 = quantityScale4,
                    sellPricePerSharePaise = pricePerSharePaise,
                    chargesPaise = chargesPaise,
                )
                txRealized = alloc.realizedGainLossPaise
            }

            val totalAmount = StockCalculator.calculateTradeTotalPaise(
                type = type,
                quantityScale4 = quantityScale4,
                pricePerSharePaise = pricePerSharePaise,
                chargesPaise = chargesPaise,
            )

            val linkedTxId = if (accountId != null && totalAmount > 0L) {
                val txType = if (type == StockTxType.BUY) {
                    TransactionType.ASSET_PURCHASE.name
                } else {
                    TransactionType.ASSET_SALE.name
                }
                transactionDao.insert(
                    TransactionEntity(
                        type = txType,
                        amountPaise = totalAmount,
                        timestamp = now,
                        accountId = accountId,
                        merchant = "$cleanSymbol (${exchange.name})",
                        description = "${type.label} ${ShareFormatter.formatShares(quantityScale4)} shares of $cleanSymbol",
                        notes = notes,
                        createdAt = now,
                        updatedAt = now,
                    )
                )
            } else null

            stockDao.insertTransaction(
                StockTransactionEntity(
                    stockId = stockId,
                    type = type.name,
                    quantityScale4 = quantityScale4,
                    pricePerSharePaise = pricePerSharePaise,
                    chargesPaise = chargesPaise,
                    totalAmountPaise = totalAmount,
                    realizedGainLossPaise = txRealized,
                    tradeDateEpochDay = tradeDateEpochDay,
                    timestamp = now,
                    accountId = accountId,
                    linkedTransactionId = linkedTxId,
                    notes = notes,
                )
            )

            val allTxs = stockDao.getTransactionsForStockAsc(stockId).map {
                StockTradeInput(
                    type = runCatching { StockTxType.valueOf(it.type) }.getOrDefault(StockTxType.BUY),
                    quantityScale4 = it.quantityScale4,
                    pricePerSharePaise = it.pricePerSharePaise,
                    chargesPaise = it.chargesPaise,
                    tradeDateEpochDay = it.tradeDateEpochDay,
                )
            }
            val computed = StockCalculator.replayPosition(allTxs)

            val manualPrice = existingHolding?.currentManualPricePaise?.takeIf { it > 0L }
                ?: pricePerSharePaise
            val priceTime = existingHolding?.priceUpdatedAtMillis ?: now

            stockDao.upsertHolding(
                StockHoldingEntity(
                    id = existingHolding?.id ?: 0L,
                    stockId = stockId,
                    quantityScale4 = computed.quantityScale4,
                    averageCostPaise = computed.averageCostPaise,
                    totalInvestedPaise = computed.totalInvestedPaise,
                    realizedGainLossPaise = computed.realizedGainLossPaise,
                    currentManualPricePaise = manualPrice,
                    priceUpdatedAtMillis = priceTime,
                    updatedAt = now,
                )
            )
            stockId
        }
    }

    suspend fun updateManualPrice(stockId: Long, pricePaise: Long, note: String = "") {
        require(pricePaise > 0L) { "Manual price must be positive." }
        db.withTransaction {
            val now = System.currentTimeMillis()
            val holding = stockDao.getHoldingForStock(stockId) ?: return@withTransaction
            stockDao.upsertHolding(
                holding.copy(
                    currentManualPricePaise = pricePaise,
                    priceUpdatedAtMillis = now,
                    updatedAt = now,
                )
            )
            stockDao.insertPriceSnapshot(
                StockPriceSnapshotEntity(
                    stockId = stockId,
                    pricePaise = pricePaise,
                    updatedAtMillis = now,
                    note = note,
                )
            )
        }
    }
}
