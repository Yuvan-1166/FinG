package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "gold_categories",
    foreignKeys = [
        ForeignKey(
            entity = GoldCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentCategoryId"],
            onDelete = ForeignKey.RESTRICT,
        )
    ],
    indices = [
        Index(value = ["parentCategoryId"]),
        Index(value = ["isArchived"]),
    ],
)
data class GoldCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val parentCategoryId: Long? = null,
    val iconName: String = "diamond",
    val isPredefined: Boolean = false,
    val isArchived: Boolean = false,
)

@Serializable
@Entity(
    tableName = "gold_items",
    foreignKeys = [
        ForeignKey(
            entity = GoldCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = GoldCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["subcategoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["subcategoryId"]),
        Index(value = ["status"]),
        Index(value = ["purity"]),
        Index(value = ["purchaseId"]),
    ],
)
data class GoldItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val categoryId: Long,
    val subcategoryId: Long? = null,
    val purity: String, // GoldPurity name
    val purityPercentText: String, // e.g. "91.6667"
    val isBulkLot: Boolean = false,
    val quantity: Int = 1,
    val originalQuantity: Int = 1,
    val grossWeightMcg: Long,
    val stoneWeightMcg: Long = 0L,
    val netWeightMcg: Long,
    val originalNetWeightMcg: Long,
    val acquisitionRatePerGramPaise: Long = 0L,
    val makingChargesPaise: Long = 0L,
    val wastageChargesPaise: Long = 0L,
    val gstPaise: Long = 0L,
    val otherChargesPaise: Long = 0L,
    val acquisitionCostPaise: Long,
    val originalAcquisitionCostPaise: Long,
    val purchaseDateEpochDay: Long,
    val merchantName: String = "",
    val hallmarkCode: String = "",
    val invoiceRef: String = "",
    val storageLocation: String = "",
    val notes: String = "",
    val itemPhotoUri: String? = null,
    val receiptUri: String? = null,
    val purchaseId: Long? = null,
    val status: String = "ACTIVE", // GoldItemStatus
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "gold_purchases",
    indices = [
        Index(value = ["purchaseDateEpochDay"]),
        Index(value = ["paymentAccountId"]),
        Index(value = ["linkedTransactionId"]),
    ],
)
data class GoldPurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val purchaseDateEpochDay: Long,
    val timestamp: Long,
    val sellerName: String = "",
    val invoiceRef: String = "",
    val totalNetWeightMcg: Long,
    val totalFineGoldMcg: Long,
    val goldSubtotalPaise: Long,
    val makingChargesPaise: Long = 0L,
    val wastageChargesPaise: Long = 0L,
    val gstPaise: Long = 0L,
    val otherChargesPaise: Long = 0L,
    val totalPaidPaise: Long,
    val paymentAccountId: Long? = null,
    val linkedTransactionId: Long? = null,
    val receiptUri: String? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "gold_purchase_items",
    foreignKeys = [
        ForeignKey(
            entity = GoldPurchaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchaseId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = GoldItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["goldItemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["purchaseId"]),
        Index(value = ["goldItemId"]),
    ],
)
data class GoldPurchaseItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val purchaseId: Long,
    val goldItemId: Long,
    val title: String,
    val purity: String,
    val purityPercentText: String,
    val quantity: Int,
    val grossWeightMcg: Long,
    val stoneWeightMcg: Long,
    val netWeightMcg: Long,
    val fineGoldMcg: Long,
    val ratePerGramPaise: Long,
    val makingChargesPaise: Long,
    val wastageChargesPaise: Long,
    val gstPaise: Long,
    val otherChargesPaise: Long,
    val totalCostPaise: Long,
)

@Serializable
@Entity(
    tableName = "gold_sales",
    indices = [
        Index(value = ["saleDateEpochDay"]),
        Index(value = ["depositAccountId"]),
        Index(value = ["linkedTransactionId"]),
    ],
)
data class GoldSaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val saleDateEpochDay: Long,
    val timestamp: Long,
    val buyerName: String = "",
    val totalSoldNetWeightMcg: Long,
    val totalSoldFineGoldMcg: Long,
    val saleRatePerGramPaise: Long,
    val grossSaleAmountPaise: Long,
    val deductionsPaise: Long = 0L,
    val netProceedsPaise: Long,
    val allocatedCostBasisPaise: Long,
    val realizedGainLossPaise: Long,
    val depositAccountId: Long? = null,
    val linkedTransactionId: Long? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "gold_sale_items",
    foreignKeys = [
        ForeignKey(
            entity = GoldSaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = GoldItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["goldItemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["saleId"]),
        Index(value = ["goldItemId"]),
    ],
)
data class GoldSaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val saleId: Long,
    val goldItemId: Long,
    val itemTitleSnapshot: String,
    val puritySnapshot: String,
    val soldQuantity: Int,
    val soldNetWeightMcg: Long,
    val soldFineGoldMcg: Long,
    val proceedsPaise: Long,
    val allocatedCostBasisPaise: Long,
    val realizedGainLossPaise: Long,
)

@Serializable
@Entity(
    tableName = "gold_obligations",
    indices = [
        Index(value = ["direction"]),
        Index(value = ["status"]),
        Index(value = ["dueDateEpochDay"]),
    ],
)
data class GoldObligationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val direction: String, // GoldObligationDirection
    val counterpartyName: String,
    val description: String = "",
    val purity: String,
    val purityPercentText: String,
    val totalPromisedWeightMcg: Long,
    val settledWeightMcg: Long = 0L,
    val agreedValuePaise: Long = 0L,
    val createdDateEpochDay: Long,
    val dueDateEpochDay: Long? = null,
    val status: String = "OPEN", // GoldObligationStatus
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "gold_obligation_settlements",
    foreignKeys = [
        ForeignKey(
            entity = GoldObligationEntity::class,
            parentColumns = ["id"],
            childColumns = ["obligationId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = GoldItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["goldItemId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["obligationId"]),
        Index(value = ["goldItemId"]),
        Index(value = ["settlementDateEpochDay"]),
    ],
)
data class GoldObligationSettlementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val obligationId: Long,
    val settlementDateEpochDay: Long,
    val timestamp: Long,
    val settledWeightMcg: Long,
    val goldItemId: Long? = null,
    val convertedToInventory: Boolean = false,
    val notes: String = "",
)

@Serializable
@Entity(
    tableName = "gold_rate_snapshots",
    indices = [
        Index(value = ["rateDateIso"]),
        Index(value = ["source"]),
        Index(value = ["fetchedAtMillis"]),
    ],
)
data class GoldRateSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val city: String,
    val market: String = "",
    val rateDateIso: String, // yyyy-MM-dd
    val source: String, // GoldRateSource
    val fetchedAtMillis: Long,
    val apiTimestampIso: String = "",
    val gstIncluded: Boolean = true,
    val sourcesUsed: Int = 0,
    val confidence: String = "",
    val rate24kBasePaise: Long,
    val rate24kGstPaise: Long,
    val rate24kFinalPaise: Long,
    val rate22kBasePaise: Long,
    val rate22kGstPaise: Long,
    val rate22kFinalPaise: Long,
    val rate18kBasePaise: Long,
    val rate18kGstPaise: Long,
    val rate18kFinalPaise: Long,
    val rate14kBasePaise: Long,
    val rate14kGstPaise: Long,
    val rate14kFinalPaise: Long,
)

@Serializable
@Entity(
    tableName = "gold_valuation_snapshots",
    indices = [
        Index(value = ["snapshotDateEpochDay"]),
        Index(value = ["recordedAtMillis"]),
    ],
)
data class GoldValuationSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val snapshotDateEpochDay: Long,
    val recordedAtMillis: Long,
    val totalNetWeightMcg: Long,
    val totalFineGoldMcg: Long,
    val totalAcquisitionCostPaise: Long,
    val benchmark24kValuePaise: Long,
    val correspondingPurityValuePaise: Long,
    val estimatedResaleValuePaise: Long,
    val rate24kFinalPaise: Long,
)
