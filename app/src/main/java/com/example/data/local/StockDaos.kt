package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    @Query("SELECT * FROM stocks ORDER BY isArchived ASC, symbol ASC")
    fun observeStocks(): Flow<List<StockEntity>>

    @Query("SELECT * FROM stocks ORDER BY isArchived ASC, symbol ASC")
    suspend fun getStocks(): List<StockEntity>

    @Query("SELECT * FROM stocks WHERE symbol = :symbol AND exchange = :exchange LIMIT 1")
    suspend fun findStock(symbol: String, exchange: String): StockEntity?

    @Query("SELECT * FROM stocks WHERE id = :id")
    suspend fun getStockById(id: Long): StockEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStock(stock: StockEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllStocks(stocks: List<StockEntity>)

    @Update
    suspend fun updateStock(stock: StockEntity)

    @Query("SELECT * FROM stock_holdings ORDER BY updatedAt DESC")
    fun observeHoldings(): Flow<List<StockHoldingEntity>>

    @Query("SELECT * FROM stock_holdings ORDER BY updatedAt DESC")
    suspend fun getHoldings(): List<StockHoldingEntity>

    @Query("SELECT * FROM stock_holdings WHERE stockId = :stockId LIMIT 1")
    suspend fun getHoldingForStock(stockId: Long): StockHoldingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHolding(holding: StockHoldingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllHoldings(holdings: List<StockHoldingEntity>)

    @Query("SELECT * FROM stock_transactions ORDER BY tradeDateEpochDay DESC, timestamp DESC, id DESC")
    fun observeTransactions(): Flow<List<StockTransactionEntity>>

    @Query("SELECT * FROM stock_transactions ORDER BY tradeDateEpochDay DESC, timestamp DESC, id DESC")
    suspend fun getTransactions(): List<StockTransactionEntity>

    @Query("SELECT * FROM stock_transactions WHERE stockId = :stockId ORDER BY tradeDateEpochDay ASC, timestamp ASC, id ASC")
    suspend fun getTransactionsForStockAsc(stockId: Long): List<StockTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(tx: StockTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTransactions(txs: List<StockTransactionEntity>)

    @Query("SELECT * FROM stock_price_snapshots ORDER BY updatedAtMillis DESC, id DESC")
    fun observePriceSnapshots(): Flow<List<StockPriceSnapshotEntity>>

    @Query("SELECT * FROM stock_price_snapshots ORDER BY updatedAtMillis DESC, id DESC")
    suspend fun getPriceSnapshots(): List<StockPriceSnapshotEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPriceSnapshot(snapshot: StockPriceSnapshotEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPriceSnapshots(snapshots: List<StockPriceSnapshotEntity>)

    @Query("DELETE FROM stock_price_snapshots")
    suspend fun deleteAllPriceSnapshots()

    @Query("DELETE FROM stock_transactions")
    suspend fun deleteAllTransactions()

    @Query("DELETE FROM stock_holdings")
    suspend fun deleteAllHoldings()

    @Query("DELETE FROM stocks")
    suspend fun deleteAllStocks()
}
