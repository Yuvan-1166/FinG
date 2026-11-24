package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        SavingsGoalEntity::class,
        GoalContributionEntity::class,
        RecurringPaymentEntity::class,
        DebtEntity::class,
        DebtRepaymentEntity::class,
        AppPreferenceEntity::class,
        // Iteration 2: Gold Treasury & Stocks
        GoldCategoryEntity::class,
        GoldItemEntity::class,
        GoldPurchaseEntity::class,
        GoldPurchaseItemEntity::class,
        GoldSaleEntity::class,
        GoldSaleItemEntity::class,
        GoldObligationEntity::class,
        GoldObligationSettlementEntity::class,
        GoldRateSnapshotEntity::class,
        GoldValuationSnapshotEntity::class,
        StockEntity::class,
        StockHoldingEntity::class,
        StockTransactionEntity::class,
        StockPriceSnapshotEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class RupeeLedgerDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun recurringPaymentDao(): RecurringPaymentDao
    abstract fun debtDao(): DebtDao
    abstract fun appPreferenceDao(): AppPreferenceDao
    abstract fun goldDao(): GoldDao
    abstract fun stockDao(): StockDao

    companion object {
        const val DATABASE_NAME = "rupee_ledger.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `gold_categories` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `parentCategoryId` INTEGER,
                        `iconName` TEXT NOT NULL,
                        `isPredefined` INTEGER NOT NULL,
                        `isArchived` INTEGER NOT NULL,
                        FOREIGN KEY(`parentCategoryId`) REFERENCES `gold_categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gold_categories_parentCategoryId` ON `gold_categories` (`parentCategoryId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gold_categories_isArchived` ON `gold_categories` (`isArchived`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `gold_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `categoryId` INTEGER NOT NULL,
                        `subcategoryId` INTEGER,
                        `purity` TEXT NOT NULL,
                        `purityPercentText` TEXT NOT NULL,
                        `isBulkLot` INTEGER NOT NULL,
                        `quantity` INTEGER NOT NULL,
                        `originalQuantity` INTEGER NOT NULL,
                        `grossWeightMcg` INTEGER NOT NULL,
                        `stoneWeightMcg` INTEGER NOT NULL,
                        `netWeightMcg` INTEGER NOT NULL,
                        `originalNetWeightMcg` INTEGER NOT NULL,
                        `acquisitionRatePerGramPaise` INTEGER NOT NULL,
                        `makingChargesPaise` INTEGER NOT NULL,
                        `wastageChargesPaise` INTEGER NOT NULL,
                        `gstPaise` INTEGER NOT NULL,
                        `otherChargesPaise` INTEGER NOT NULL,
                        `acquisitionCostPaise` INTEGER NOT NULL,
                        `originalAcquisitionCostPaise` INTEGER NOT NULL,
                        `purchaseDateEpochDay` INTEGER NOT NULL,
                        `merchantName` TEXT NOT NULL,
                        `hallmarkCode` TEXT NOT NULL,
                        `invoiceRef` TEXT NOT NULL,
                        `storageLocation` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `itemPhotoUri` TEXT,
                        `receiptUri` TEXT,
                        `purchaseId` INTEGER,
                        `status` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`categoryId`) REFERENCES `gold_categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`subcategoryId`) REFERENCES `gold_categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gold_items_categoryId` ON `gold_items` (`categoryId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gold_items_subcategoryId` ON `gold_items` (`subcategoryId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gold_items_status` ON `gold_items` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gold_items_purity` ON `gold_items` (`purity`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gold_items_purchaseId` ON `gold_items` (`purchaseId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `gold_purchases` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `purchaseDateEpochDay` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `sellerName` TEXT NOT NULL,
                        `invoiceRef` TEXT NOT NULL,
                        `totalNetWeightMcg` INTEGER NOT NULL,
                        `totalFineGoldMcg` INTEGER NOT NULL,
                        `goldSubtotalPaise` INTEGER NOT NULL,
                        `makingChargesPaise` INTEGER NOT NULL,
                        `wastageChargesPaise` INTEGER NOT NULL,
                        `gstPaise` INTEGER NOT NULL,
                        `otherChargesPaise` INTEGER NOT NULL,
                        `totalPaidPaise` INTEGER NOT NULL,
                        `paymentAccountId` INTEGER,
                        `linkedTransactionId` INTEGER,
                        `receiptUri` TEXT,
                        `notes` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gold_purchases_purchaseDateEpochDay` ON `gold_purchases` (`purchaseDateEpochDay`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gold_purchases_paymentAccountId` ON `gold_purchases` (`paymentAccountId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gold_purchases_linkedTransactionId` ON `gold_purchases` (`linkedTransactionId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `gold_purchase_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `purchaseId` INTEGER NOT NULL,
                        `goldItemId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `purity` TEXT NOT NULL,
                        `purityPercentText` TEXT NOT NULL,
                        `quantity` INTEGER NOT NULL,
                        `grossWeightMcg` INTEGER NOT NULL,
                        `stoneWeightMcg` INTEGER NOT NULL,
                        `netWeightMcg` INTEGER NOT NULL,
                        `fineGoldMcg` INTEGER NOT NULL,
                        `ratePerGramPaise` INTEGER NOT NULL,
                        `makingChargesPaise` INTEGER NOT NULL,
                        `wastageChargesPaise` INTEGER NOT NULL,
                        `gstPaise` INTEGER NOT NULL,
                        `otherChargesPaise` INTEGER NOT NULL,
                        `totalCostPaise` INTEGER NOT NULL,
                        FOREIGN KEY(`purchaseId`) REFERENCES `gold_purchases`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`goldItemId`) REFERENCES `gold_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gold_purchase_items_purchaseId` ON `gold_purchase_items` (`purchaseId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gold_purchase_items_goldItemId` ON `gold_purchase_items` (`goldItemId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `gold_sales` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `saleDateEpochDay` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `buyerName` TEXT NOT NULL,
                        `totalSoldNetWeightMcg` INTEGER NOT NULL,
                        `totalSoldFineGoldMcg` INTEGER NOT NULL,
                        `saleRatePerGramPaise` INTEGER NOT NULL,
                        `grossSaleAmountPaise` INTEGER NOT NULL,
                        `deductionsPaise` INTEGER NOT NULL,
                        `netProceedsPaise` INTEGER NOT NULL,
                        `allocatedCostBasisPaise` INTEGER NOT NULL,
                        `realizedGainLossPaise` INTEGER NOT NULL,
                        `depositAccountId` INTEGER,
                        `linkedTransactionId` INTEGER,
                        `notes` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gold_sales_saleDateEpochDay` ON `gold_sales` (`saleDateEpochDay`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gold_sales_depositAccountId` ON `gold_sales` (`depositAccountId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gold_sales_linkedTransactionId` ON `gold_sales` (`linkedTransactionId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `gold_sale_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `saleId` INTEGER NOT NULL,
                        `goldItemId` INTEGER NOT NULL,
                        `itemTitleSnapshot` TEXT NOT NULL,
                        `puritySnapshot` TEXT NOT NULL,
                        `soldQuantity` INTEGER NOT NULL,
                        `soldNetWeightMcg` INTEGER NOT NULL,
                        `soldFineGoldMcg` INTEGER NOT NULL,
                        `proceedsPaise` INTEGER NOT NULL,
                        `allocatedCostBasisPaise` INTEGER NOT NULL,
                        `realizedGainLossPaise` INTEGER NOT NULL,
                        FOREIGN KEY(`saleId`) REFERENCES `gold_sales`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`goldItemId`) REFERENCES `gold_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gold_sale_items_saleId` ON `gold_sale_items` (`saleId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gold_sale_items_goldItemId` ON `gold_sale_items` (`goldItemId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `gold_obligations` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `direction` TEXT NOT NULL,
                        `counterpartyName` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `purity` TEXT NOT NULL,
                        `purityPercentText` TEXT NOT NULL,
                        `totalPromisedWeightMcg` INTEGER NOT NULL,
                        `settledWeightMcg` INTEGER NOT NULL,
                        `agreedValuePaise` INTEGER NOT NULL,
                        `createdDateEpochDay` INTEGER NOT NULL,
                        `dueDateEpochDay` INTEGER,
                        `status` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gold_obligations_direction` ON `gold_obligations` (`direction`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gold_obligations_status` ON `gold_obligations` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gold_obligations_dueDateEpochDay` ON `gold_obligations` (`dueDateEpochDay`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `gold_obligation_settlements` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `obligationId` INTEGER NOT NULL,
                        `settlementDateEpochDay` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `settledWeightMcg` INTEGER NOT NULL,
                        `goldItemId` INTEGER,
                        `convertedToInventory` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        FOREIGN KEY(`obligationId`) REFERENCES `gold_obligations`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`goldItemId`) REFERENCES `gold_items`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gold_obligation_settlements_obligationId` ON `gold_obligation_settlements` (`obligationId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gold_obligation_settlements_goldItemId` ON `gold_obligation_settlements` (`goldItemId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gold_obligation_settlements_settlementDateEpochDay` ON `gold_obligation_settlements` (`settlementDateEpochDay`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `gold_rate_snapshots` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `city` TEXT NOT NULL,
                        `market` TEXT NOT NULL,
                        `rateDateIso` TEXT NOT NULL,
                        `source` TEXT NOT NULL,
                        `fetchedAtMillis` INTEGER NOT NULL,
                        `apiTimestampIso` TEXT NOT NULL,
                        `gstIncluded` INTEGER NOT NULL,
                        `sourcesUsed` INTEGER NOT NULL,
                        `confidence` TEXT NOT NULL,
                        `rate24kBasePaise` INTEGER NOT NULL,
                        `rate24kGstPaise` INTEGER NOT NULL,
                        `rate24kFinalPaise` INTEGER NOT NULL,
                        `rate22kBasePaise` INTEGER NOT NULL,
                        `rate22kGstPaise` INTEGER NOT NULL,
                        `rate22kFinalPaise` INTEGER NOT NULL,
                        `rate18kBasePaise` INTEGER NOT NULL,
                        `rate18kGstPaise` INTEGER NOT NULL,
                        `rate18kFinalPaise` INTEGER NOT NULL,
                        `rate14kBasePaise` INTEGER NOT NULL,
                        `rate14kGstPaise` INTEGER NOT NULL,
                        `rate14kFinalPaise` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gold_rate_snapshots_rateDateIso` ON `gold_rate_snapshots` (`rateDateIso`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gold_rate_snapshots_source` ON `gold_rate_snapshots` (`source`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gold_rate_snapshots_fetchedAtMillis` ON `gold_rate_snapshots` (`fetchedAtMillis`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `gold_valuation_snapshots` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `snapshotDateEpochDay` INTEGER NOT NULL,
                        `recordedAtMillis` INTEGER NOT NULL,
                        `totalNetWeightMcg` INTEGER NOT NULL,
                        `totalFineGoldMcg` INTEGER NOT NULL,
                        `totalAcquisitionCostPaise` INTEGER NOT NULL,
                        `benchmark24kValuePaise` INTEGER NOT NULL,
                        `correspondingPurityValuePaise` INTEGER NOT NULL,
                        `estimatedResaleValuePaise` INTEGER NOT NULL,
                        `rate24kFinalPaise` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gold_valuation_snapshots_snapshotDateEpochDay` ON `gold_valuation_snapshots` (`snapshotDateEpochDay`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_gold_valuation_snapshots_recordedAtMillis` ON `gold_valuation_snapshots` (`recordedAtMillis`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `stocks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `symbol` TEXT NOT NULL,
                        `companyName` TEXT NOT NULL,
                        `exchange` TEXT NOT NULL,
                        `sector` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `isArchived` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_stocks_symbol_exchange` ON `stocks` (`symbol`, `exchange`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `stock_holdings` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `stockId` INTEGER NOT NULL,
                        `quantityScale4` INTEGER NOT NULL,
                        `averageCostPaise` INTEGER NOT NULL,
                        `totalInvestedPaise` INTEGER NOT NULL,
                        `realizedGainLossPaise` INTEGER NOT NULL,
                        `currentManualPricePaise` INTEGER NOT NULL,
                        `priceUpdatedAtMillis` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`stockId`) REFERENCES `stocks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_stock_holdings_stockId` ON `stock_holdings` (`stockId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `stock_transactions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `stockId` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `quantityScale4` INTEGER NOT NULL,
                        `pricePerSharePaise` INTEGER NOT NULL,
                        `chargesPaise` INTEGER NOT NULL,
                        `totalAmountPaise` INTEGER NOT NULL,
                        `realizedGainLossPaise` INTEGER NOT NULL,
                        `tradeDateEpochDay` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `accountId` INTEGER,
                        `linkedTransactionId` INTEGER,
                        `notes` TEXT NOT NULL,
                        FOREIGN KEY(`stockId`) REFERENCES `stocks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_transactions_stockId` ON `stock_transactions` (`stockId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_transactions_tradeDateEpochDay` ON `stock_transactions` (`tradeDateEpochDay`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `stock_price_snapshots` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `stockId` INTEGER NOT NULL,
                        `pricePaise` INTEGER NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL,
                        `note` TEXT NOT NULL,
                        FOREIGN KEY(`stockId`) REFERENCES `stocks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_price_snapshots_stockId` ON `stock_price_snapshots` (`stockId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_price_snapshots_updatedAtMillis` ON `stock_price_snapshots` (`updatedAtMillis`)")
            }
        }

        fun create(context: Context): RupeeLedgerDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                RupeeLedgerDatabase::class.java,
                DATABASE_NAME,
            )
                .addMigrations(MIGRATION_1_2)
                .build()
        }
    }
}
