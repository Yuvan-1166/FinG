package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GoldDao {
    // Categories
    @Query("SELECT * FROM gold_categories ORDER BY isArchived ASC, isPredefined DESC, name ASC")
    fun observeCategories(): Flow<List<GoldCategoryEntity>>

    @Query("SELECT * FROM gold_categories ORDER BY isArchived ASC, isPredefined DESC, name ASC")
    suspend fun getCategories(): List<GoldCategoryEntity>

    @Query("SELECT COUNT(*) FROM gold_categories")
    suspend fun countCategories(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCategory(category: GoldCategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCategories(categories: List<GoldCategoryEntity>)

    @Update
    suspend fun updateCategory(category: GoldCategoryEntity)

    @Query("SELECT COUNT(*) FROM gold_items WHERE categoryId = :categoryId OR subcategoryId = :categoryId")
    suspend fun countItemsUsingCategory(categoryId: Long): Int

    @Query("SELECT COUNT(*) FROM gold_categories WHERE parentCategoryId = :categoryId")
    suspend fun countSubcategories(categoryId: Long): Int

    @Query("DELETE FROM gold_categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Long)

    // Items
    @Query("SELECT * FROM gold_items ORDER BY purchaseDateEpochDay DESC, id DESC")
    fun observeItems(): Flow<List<GoldItemEntity>>

    @Query("SELECT * FROM gold_items ORDER BY purchaseDateEpochDay DESC, id DESC")
    suspend fun getItems(): List<GoldItemEntity>

    @Query("SELECT * FROM gold_items WHERE id = :id")
    suspend fun getItemById(id: Long): GoldItemEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItem(item: GoldItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllItems(items: List<GoldItemEntity>)

    @Update
    suspend fun updateItem(item: GoldItemEntity)

    // Purchases & Purchase Items
    @Query("SELECT * FROM gold_purchases ORDER BY timestamp DESC, id DESC")
    fun observePurchases(): Flow<List<GoldPurchaseEntity>>

    @Query("SELECT * FROM gold_purchases ORDER BY timestamp DESC, id DESC")
    suspend fun getPurchases(): List<GoldPurchaseEntity>

    @Query("SELECT * FROM gold_purchase_items ORDER BY id DESC")
    fun observePurchaseItems(): Flow<List<GoldPurchaseItemEntity>>

    @Query("SELECT * FROM gold_purchase_items ORDER BY id DESC")
    suspend fun getPurchaseItems(): List<GoldPurchaseItemEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPurchase(purchase: GoldPurchaseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPurchases(purchases: List<GoldPurchaseEntity>)

    @Update
    suspend fun updatePurchase(purchase: GoldPurchaseEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPurchaseItem(item: GoldPurchaseItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPurchaseItems(items: List<GoldPurchaseItemEntity>)

    // Sales & Sale Items
    @Query("SELECT * FROM gold_sales ORDER BY timestamp DESC, id DESC")
    fun observeSales(): Flow<List<GoldSaleEntity>>

    @Query("SELECT * FROM gold_sales ORDER BY timestamp DESC, id DESC")
    suspend fun getSales(): List<GoldSaleEntity>

    @Query("SELECT * FROM gold_sale_items ORDER BY id DESC")
    fun observeSaleItems(): Flow<List<GoldSaleItemEntity>>

    @Query("SELECT * FROM gold_sale_items ORDER BY id DESC")
    suspend fun getSaleItems(): List<GoldSaleItemEntity>

    @Query("SELECT * FROM gold_sale_items WHERE goldItemId = :goldItemId ORDER BY id DESC")
    fun observeSaleItemsForGoldItem(goldItemId: Long): Flow<List<GoldSaleItemEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSale(sale: GoldSaleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSales(sales: List<GoldSaleEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSaleItem(item: GoldSaleItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSaleItems(items: List<GoldSaleItemEntity>)

    // Obligations & Settlements
    @Query("SELECT * FROM gold_obligations ORDER BY createdAt DESC, id DESC")
    fun observeObligations(): Flow<List<GoldObligationEntity>>

    @Query("SELECT * FROM gold_obligations ORDER BY createdAt DESC, id DESC")
    suspend fun getObligations(): List<GoldObligationEntity>

    @Query("SELECT * FROM gold_obligations WHERE id = :id")
    suspend fun getObligationById(id: Long): GoldObligationEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertObligation(obligation: GoldObligationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllObligations(obligations: List<GoldObligationEntity>)

    @Update
    suspend fun updateObligation(obligation: GoldObligationEntity)

    @Query("SELECT * FROM gold_obligation_settlements ORDER BY timestamp DESC, id DESC")
    fun observeSettlements(): Flow<List<GoldObligationSettlementEntity>>

    @Query("SELECT * FROM gold_obligation_settlements ORDER BY timestamp DESC, id DESC")
    suspend fun getSettlements(): List<GoldObligationSettlementEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSettlement(settlement: GoldObligationSettlementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSettlements(settlements: List<GoldObligationSettlementEntity>)

    // Market Rates
    @Query("SELECT * FROM gold_rate_snapshots ORDER BY rateDateIso DESC, fetchedAtMillis DESC, id DESC")
    fun observeRateSnapshots(): Flow<List<GoldRateSnapshotEntity>>

    @Query("SELECT * FROM gold_rate_snapshots ORDER BY rateDateIso DESC, fetchedAtMillis DESC, id DESC")
    suspend fun getRateSnapshots(): List<GoldRateSnapshotEntity>

    @Query("SELECT * FROM gold_rate_snapshots WHERE rateDateIso = :rateDateIso AND source = :source AND city = :city LIMIT 1")
    suspend fun findRateSnapshot(rateDateIso: String, source: String, city: String): GoldRateSnapshotEntity?

    @Query("SELECT * FROM gold_rate_snapshots ORDER BY rateDateIso DESC, fetchedAtMillis DESC LIMIT 1")
    suspend fun getLatestRateSnapshot(): GoldRateSnapshotEntity?

    @Query("SELECT * FROM gold_rate_snapshots WHERE rateDateIso <= :rateDateIso ORDER BY rateDateIso DESC, fetchedAtMillis DESC LIMIT 1")
    suspend fun getRateSnapshotOnOrBefore(rateDateIso: String): GoldRateSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRateSnapshot(snapshot: GoldRateSnapshotEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllRateSnapshots(snapshots: List<GoldRateSnapshotEntity>)

    // Valuation Snapshots
    @Query("SELECT * FROM gold_valuation_snapshots ORDER BY snapshotDateEpochDay ASC, recordedAtMillis ASC")
    fun observeValuationSnapshots(): Flow<List<GoldValuationSnapshotEntity>>

    @Query("SELECT * FROM gold_valuation_snapshots ORDER BY snapshotDateEpochDay ASC, recordedAtMillis ASC")
    suspend fun getValuationSnapshots(): List<GoldValuationSnapshotEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertValuationSnapshot(snapshot: GoldValuationSnapshotEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllValuationSnapshots(snapshots: List<GoldValuationSnapshotEntity>)

    // Bulk clears for restore
    @Query("DELETE FROM gold_obligation_settlements")
    suspend fun deleteAllSettlements()

    @Query("DELETE FROM gold_obligations")
    suspend fun deleteAllObligations()

    @Query("DELETE FROM gold_sale_items")
    suspend fun deleteAllSaleItems()

    @Query("DELETE FROM gold_sales")
    suspend fun deleteAllSales()

    @Query("DELETE FROM gold_purchase_items")
    suspend fun deleteAllPurchaseItems()

    @Query("DELETE FROM gold_purchases")
    suspend fun deleteAllPurchases()

    @Query("DELETE FROM gold_items")
    suspend fun deleteAllItems()

    @Query("DELETE FROM gold_categories")
    suspend fun deleteAllCategories()

    @Query("DELETE FROM gold_rate_snapshots")
    suspend fun deleteAllRateSnapshots()

    @Query("DELETE FROM gold_valuation_snapshots")
    suspend fun deleteAllValuationSnapshots()
}
