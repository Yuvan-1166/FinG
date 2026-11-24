package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.gold.GoldMarketRateProvider
import com.example.data.gold.GoldMarketSchedule
import com.example.data.gold.NebulaMetalsClient
import com.example.data.local.DatabaseSeeder
import com.example.data.local.GoldCategoryEntity
import com.example.data.local.GoldDao
import com.example.data.local.GoldItemEntity
import com.example.data.local.GoldObligationEntity
import com.example.data.local.GoldObligationSettlementEntity
import com.example.data.local.GoldPurchaseEntity
import com.example.data.local.GoldPurchaseItemEntity
import com.example.data.local.GoldRateSnapshotEntity
import com.example.data.local.GoldSaleEntity
import com.example.data.local.GoldSaleItemEntity
import com.example.data.local.GoldValuationSnapshotEntity
import com.example.data.local.RupeeLedgerDatabase
import com.example.data.local.TransactionDao
import com.example.data.local.TransactionEntity
import com.example.domain.gold.GoldCalculator
import com.example.domain.gold.GoldHoldingInput
import com.example.domain.gold.GoldItemStatus
import com.example.domain.gold.GoldObligationDirection
import com.example.domain.gold.GoldObligationStatus
import com.example.domain.gold.GoldPurity
import com.example.domain.gold.GoldRateInput
import com.example.domain.gold.GoldRateSource
import com.example.domain.gold.WeightFormatter
import com.example.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZonedDateTime

data class GoldPurchaseLineDraft(
    val title: String,
    val categoryId: Long,
    val subcategoryId: Long? = null,
    val purity: GoldPurity,
    val purityPercentText: String,
    val isBulkLot: Boolean = false,
    val quantity: Int = 1,
    val grossWeightMcg: Long,
    val stoneWeightMcg: Long = 0L,
    val netWeightMcg: Long,
    val ratePerGramPaise: Long,
    val makingChargesPaise: Long = 0L,
    val wastageChargesPaise: Long = 0L,
    val gstPaise: Long = 0L,
    val otherChargesPaise: Long = 0L,
    val totalCostPaise: Long,
    val hallmarkCode: String = "",
    val storageLocation: String = "",
    val itemPhotoUri: String? = null,
    val notes: String = "",
)

data class GoldPurchaseDraft(
    val purchaseDateEpochDay: Long,
    val sellerName: String,
    val invoiceRef: String,
    val paymentAccountId: Long? = null,
    val receiptUri: String? = null,
    val notes: String = "",
    val items: List<GoldPurchaseLineDraft>,
)

data class GoldSaleLineDraft(
    val goldItemId: Long,
    val soldQuantity: Int,
    val soldNetWeightMcg: Long,
    val grossProceedsPaise: Long,
    val deductionsPaise: Long,
)

data class GoldSaleDraft(
    val saleDateEpochDay: Long,
    val buyerName: String,
    val saleRatePerGramPaise: Long,
    val depositAccountId: Long? = null,
    val notes: String = "",
    val lines: List<GoldSaleLineDraft>,
)

sealed interface GoldRateRefreshResult {
    data class Fetched(val snapshot: GoldRateSnapshotEntity) : GoldRateRefreshResult
    data class AlreadyCachedToday(val snapshot: GoldRateSnapshotEntity, val message: String) : GoldRateRefreshResult
    data class OutsideMarketWindow(val cached: GoldRateSnapshotEntity?, val reason: String) : GoldRateRefreshResult
    data class Error(val cached: GoldRateSnapshotEntity?, val message: String) : GoldRateRefreshResult
}

class GoldRepository(
    private val db: RupeeLedgerDatabase,
    private val goldDao: GoldDao = db.goldDao(),
    private val transactionDao: TransactionDao = db.transactionDao(),
    private val marketRateProvider: GoldMarketRateProvider = NebulaMetalsClient(),
) {
    val categoriesFlow: Flow<List<GoldCategoryEntity>> = goldDao.observeCategories()
    val itemsFlow: Flow<List<GoldItemEntity>> = goldDao.observeItems()
    val purchasesFlow: Flow<List<GoldPurchaseEntity>> = goldDao.observePurchases()
    val purchaseItemsFlow: Flow<List<GoldPurchaseItemEntity>> = goldDao.observePurchaseItems()
    val salesFlow: Flow<List<GoldSaleEntity>> = goldDao.observeSales()
    val saleItemsFlow: Flow<List<GoldSaleItemEntity>> = goldDao.observeSaleItems()
    val obligationsFlow: Flow<List<GoldObligationEntity>> = goldDao.observeObligations()
    val settlementsFlow: Flow<List<GoldObligationSettlementEntity>> = goldDao.observeSettlements()
    val rateSnapshotsFlow: Flow<List<GoldRateSnapshotEntity>> = goldDao.observeRateSnapshots()
    val valuationSnapshotsFlow: Flow<List<GoldValuationSnapshotEntity>> = goldDao.observeValuationSnapshots()

    suspend fun ensureSeeded() {
        DatabaseSeeder.seedGoldCategoriesIfEmpty(goldDao)
    }

    // --- Categories ---
    suspend fun saveCategory(category: GoldCategoryEntity): Long {
        return if (category.id == 0L) {
            goldDao.insertCategory(category)
        } else {
            goldDao.updateCategory(category)
            category.id
        }
    }

    suspend fun toggleCategoryArchive(category: GoldCategoryEntity) {
        goldDao.updateCategory(category.copy(isArchived = !category.isArchived))
    }

    suspend fun deleteCategory(categoryId: Long): DeleteResult {
        val itemCount = goldDao.countItemsUsingCategory(categoryId)
        val subCount = goldDao.countSubcategories(categoryId)
        return when {
            itemCount > 0 -> DeleteResult.BlockedInUse("Category has $itemCount gold item(s). Archive it instead.")
            subCount > 0 -> DeleteResult.BlockedInUse("Remove subcategories first.")
            else -> {
                goldDao.deleteCategoryById(categoryId)
                DeleteResult.Deleted
            }
        }
    }

    // --- Standalone Item Management ---
    suspend fun saveItem(item: GoldItemEntity): Long {
        require(item.netWeightMcg > 0L) { "Net gold weight must be positive." }
        require(item.grossWeightMcg >= item.netWeightMcg) { "Gross weight must be at least net weight." }
        val now = System.currentTimeMillis()
        val id = db.withTransaction {
            val savedId = if (item.id == 0L) {
                goldDao.insertItem(item.copy(createdAt = now, updatedAt = now))
            } else {
                goldDao.updateItem(item.copy(updatedAt = now))
                item.id
            }
            recordValuationSnapshotInternal(LocalDate.now().toEpochDay(), now)
            savedId
        }
        return id
    }

    suspend fun toggleArchiveItem(item: GoldItemEntity) {
        val nextStatus = if (item.status == GoldItemStatus.ARCHIVED.name) {
            if (item.netWeightMcg < item.originalNetWeightMcg && item.netWeightMcg > 0L) {
                GoldItemStatus.PARTIALLY_SOLD.name
            } else if (item.netWeightMcg == 0L) {
                GoldItemStatus.SOLD.name
            } else {
                GoldItemStatus.ACTIVE.name
            }
        } else {
            GoldItemStatus.ARCHIVED.name
        }
        goldDao.updateItem(item.copy(status = nextStatus, updatedAt = System.currentTimeMillis()))
    }

    // --- Buy Gold ---
    suspend fun recordPurchase(draft: GoldPurchaseDraft): Long {
        require(draft.items.isNotEmpty()) { "Add at least one gold item to the purchase." }
        draft.items.forEach {
            require(it.netWeightMcg > 0L) { "Net gold weight for ${it.title} must be positive." }
            require(it.totalCostPaise > 0L) { "Total cost for ${it.title} must be positive." }
        }

        return db.withTransaction {
            val now = System.currentTimeMillis()
            val totalNetMcg = draft.items.sumOf { it.netWeightMcg }
            val totalFineMcg = draft.items.sumOf {
                GoldCalculator.calculateFineGoldMicrograms(it.netWeightMcg, it.purity, it.purityPercentText)
            }
            val goldSubtotal = draft.items.sumOf {
                GoldCalculator.calculateGoldValuePaise(it.netWeightMcg, it.ratePerGramPaise)
            }
            val making = draft.items.sumOf { it.makingChargesPaise }
            val wastage = draft.items.sumOf { it.wastageChargesPaise }
            val gst = draft.items.sumOf { it.gstPaise }
            val other = draft.items.sumOf { it.otherChargesPaise }
            val totalPaid = draft.items.sumOf { it.totalCostPaise }

            val linkedTxId = if (draft.paymentAccountId != null) {
                transactionDao.insert(
                    TransactionEntity(
                        type = TransactionType.ASSET_PURCHASE.name,
                        amountPaise = totalPaid,
                        timestamp = now,
                        accountId = draft.paymentAccountId,
                        merchant = draft.sellerName.ifBlank { "Gold Purchase" },
                        description = "Gold purchase (${WeightFormatter.formatGrams(totalNetMcg)})",
                        notes = draft.notes,
                        receiptUri = draft.receiptUri,
                        createdAt = now,
                        updatedAt = now,
                    )
                )
            } else null

            val purchaseId = goldDao.insertPurchase(
                GoldPurchaseEntity(
                    purchaseDateEpochDay = draft.purchaseDateEpochDay,
                    timestamp = now,
                    sellerName = draft.sellerName,
                    invoiceRef = draft.invoiceRef,
                    totalNetWeightMcg = totalNetMcg,
                    totalFineGoldMcg = totalFineMcg,
                    goldSubtotalPaise = goldSubtotal,
                    makingChargesPaise = making,
                    wastageChargesPaise = wastage,
                    gstPaise = gst,
                    otherChargesPaise = other,
                    totalPaidPaise = totalPaid,
                    paymentAccountId = draft.paymentAccountId,
                    linkedTransactionId = linkedTxId,
                    receiptUri = draft.receiptUri,
                    notes = draft.notes,
                    createdAt = now,
                )
            )

            for (line in draft.items) {
                val fineMcg = GoldCalculator.calculateFineGoldMicrograms(
                    line.netWeightMcg,
                    line.purity,
                    line.purityPercentText,
                )
                val itemId = goldDao.insertItem(
                    GoldItemEntity(
                        title = line.title,
                        categoryId = line.categoryId,
                        subcategoryId = line.subcategoryId,
                        purity = line.purity.name,
                        purityPercentText = line.purityPercentText,
                        isBulkLot = line.isBulkLot,
                        quantity = line.quantity.coerceAtLeast(1),
                        originalQuantity = line.quantity.coerceAtLeast(1),
                        grossWeightMcg = line.grossWeightMcg.coerceAtLeast(line.netWeightMcg),
                        stoneWeightMcg = line.stoneWeightMcg.coerceAtLeast(0L),
                        netWeightMcg = line.netWeightMcg,
                        originalNetWeightMcg = line.netWeightMcg,
                        acquisitionRatePerGramPaise = line.ratePerGramPaise,
                        makingChargesPaise = line.makingChargesPaise,
                        wastageChargesPaise = line.wastageChargesPaise,
                        gstPaise = line.gstPaise,
                        otherChargesPaise = line.otherChargesPaise,
                        acquisitionCostPaise = line.totalCostPaise,
                        originalAcquisitionCostPaise = line.totalCostPaise,
                        purchaseDateEpochDay = draft.purchaseDateEpochDay,
                        merchantName = draft.sellerName,
                        hallmarkCode = line.hallmarkCode,
                        invoiceRef = draft.invoiceRef,
                        storageLocation = line.storageLocation,
                        notes = line.notes,
                        itemPhotoUri = line.itemPhotoUri,
                        receiptUri = draft.receiptUri,
                        purchaseId = purchaseId,
                        status = GoldItemStatus.ACTIVE.name,
                        createdAt = now,
                        updatedAt = now,
                    )
                )

                goldDao.insertPurchaseItem(
                    GoldPurchaseItemEntity(
                        purchaseId = purchaseId,
                        goldItemId = itemId,
                        title = line.title,
                        purity = line.purity.name,
                        purityPercentText = line.purityPercentText,
                        quantity = line.quantity.coerceAtLeast(1),
                        grossWeightMcg = line.grossWeightMcg.coerceAtLeast(line.netWeightMcg),
                        stoneWeightMcg = line.stoneWeightMcg.coerceAtLeast(0L),
                        netWeightMcg = line.netWeightMcg,
                        fineGoldMcg = fineMcg,
                        ratePerGramPaise = line.ratePerGramPaise,
                        makingChargesPaise = line.makingChargesPaise,
                        wastageChargesPaise = line.wastageChargesPaise,
                        gstPaise = line.gstPaise,
                        otherChargesPaise = line.otherChargesPaise,
                        totalCostPaise = line.totalCostPaise,
                    )
                )
            }

            recordValuationSnapshotInternal(draft.purchaseDateEpochDay, now)
            purchaseId
        }
    }

    // --- Sell / Dispose Gold ---
    suspend fun recordSale(draft: GoldSaleDraft): Long {
        require(draft.lines.isNotEmpty()) { "Select at least one gold holding to sell." }

        return db.withTransaction {
            val now = System.currentTimeMillis()
            var totalSoldNet = 0L
            var totalSoldFine = 0L
            var totalGrossProceeds = 0L
            var totalDeductions = 0L
            var totalAllocatedCost = 0L

            data class ResolvedSaleLine(
                val item: GoldItemEntity,
                val soldQty: Int,
                val soldNetMcg: Long,
                val soldFineMcg: Long,
                val netProceedsPaise: Long,
                val allocatedCostPaise: Long,
            )

            val resolvedLines = mutableListOf<ResolvedSaleLine>()

            for (line in draft.lines) {
                val item = goldDao.getItemById(line.goldItemId)
                    ?: error("Gold item not found.")
                require(item.status != GoldItemStatus.SOLD.name && item.status != GoldItemStatus.ARCHIVED.name) {
                    "${item.title} is no longer available for sale."
                }
                require(line.soldNetWeightMcg > 0L && line.soldNetWeightMcg <= item.netWeightMcg) {
                    "Sold weight for ${item.title} exceeds available ${WeightFormatter.formatGrams(item.netWeightMcg)}."
                }

                val purity = runCatching { GoldPurity.valueOf(item.purity) }.getOrDefault(GoldPurity.K22)
                val soldFineMcg = GoldCalculator.calculateFineGoldMicrograms(
                    line.soldNetWeightMcg,
                    purity,
                    item.purityPercentText,
                )
                val allocatedCost = GoldCalculator.allocateAcquisitionCostPaise(
                    remainingAcquisitionCostPaise = item.acquisitionCostPaise,
                    remainingNetWeightMcg = item.netWeightMcg,
                    soldNetWeightMcg = line.soldNetWeightMcg,
                )
                val netProceeds = (line.grossProceedsPaise - line.deductionsPaise).coerceAtLeast(0L)

                totalSoldNet += line.soldNetWeightMcg
                totalSoldFine += soldFineMcg
                totalGrossProceeds += line.grossProceedsPaise
                totalDeductions += line.deductionsPaise
                totalAllocatedCost += allocatedCost

                resolvedLines.add(
                    ResolvedSaleLine(
                        item = item,
                        soldQty = line.soldQuantity.coerceIn(0, item.quantity),
                        soldNetMcg = line.soldNetWeightMcg,
                        soldFineMcg = soldFineMcg,
                        netProceedsPaise = netProceeds,
                        allocatedCostPaise = allocatedCost,
                    )
                )
            }

            val totalNetProceeds = (totalGrossProceeds - totalDeductions).coerceAtLeast(0L)
            val totalRealizedPnl = totalNetProceeds - totalAllocatedCost

            val linkedTxId = if (draft.depositAccountId != null && totalNetProceeds > 0L) {
                transactionDao.insert(
                    TransactionEntity(
                        type = TransactionType.ASSET_SALE.name,
                        amountPaise = totalNetProceeds,
                        timestamp = now,
                        accountId = draft.depositAccountId,
                        merchant = draft.buyerName.ifBlank { "Gold Sale" },
                        description = "Gold sale (${WeightFormatter.formatGrams(totalSoldNet)})",
                        notes = draft.notes,
                        createdAt = now,
                        updatedAt = now,
                    )
                )
            } else null

            val saleId = goldDao.insertSale(
                GoldSaleEntity(
                    saleDateEpochDay = draft.saleDateEpochDay,
                    timestamp = now,
                    buyerName = draft.buyerName,
                    totalSoldNetWeightMcg = totalSoldNet,
                    totalSoldFineGoldMcg = totalSoldFine,
                    saleRatePerGramPaise = draft.saleRatePerGramPaise,
                    grossSaleAmountPaise = totalGrossProceeds,
                    deductionsPaise = totalDeductions,
                    netProceedsPaise = totalNetProceeds,
                    allocatedCostBasisPaise = totalAllocatedCost,
                    realizedGainLossPaise = totalRealizedPnl,
                    depositAccountId = draft.depositAccountId,
                    linkedTransactionId = linkedTxId,
                    notes = draft.notes,
                    createdAt = now,
                )
            )

            for (res in resolvedLines) {
                val item = res.item
                val remainingNet = (item.netWeightMcg - res.soldNetMcg).coerceAtLeast(0L)
                val remainingCost = (item.acquisitionCostPaise - res.allocatedCostPaise).coerceAtLeast(0L)
                val remainingQty = if (remainingNet == 0L) 0 else {
                    (item.quantity - res.soldQty).coerceAtLeast(1)
                }
                val newStatus = if (remainingNet == 0L) {
                    GoldItemStatus.SOLD.name
                } else {
                    GoldItemStatus.PARTIALLY_SOLD.name
                }
                val remainingGross = if (remainingNet == 0L) 0L else {
                    (item.grossWeightMcg - res.soldNetMcg).coerceAtLeast(remainingNet)
                }

                // Preserve originalNetWeightMcg, originalAcquisitionCostPaise, and originalQuantity
                goldDao.updateItem(
                    item.copy(
                        quantity = remainingQty,
                        grossWeightMcg = remainingGross,
                        netWeightMcg = remainingNet,
                        acquisitionCostPaise = remainingCost,
                        status = newStatus,
                        updatedAt = now,
                    )
                )

                goldDao.insertSaleItem(
                    GoldSaleItemEntity(
                        saleId = saleId,
                        goldItemId = item.id,
                        itemTitleSnapshot = item.title,
                        puritySnapshot = item.purity,
                        soldQuantity = res.soldQty,
                        soldNetWeightMcg = res.soldNetMcg,
                        soldFineGoldMcg = res.soldFineMcg,
                        proceedsPaise = res.netProceedsPaise,
                        allocatedCostBasisPaise = res.allocatedCostPaise,
                        realizedGainLossPaise = res.netProceedsPaise - res.allocatedCostPaise,
                    )
                )
            }

            recordValuationSnapshotInternal(draft.saleDateEpochDay, now)
            saleId
        }
    }

    // --- Obligations ---
    suspend fun createObligation(
        direction: GoldObligationDirection,
        counterpartyName: String,
        description: String,
        purity: GoldPurity,
        purityPercentText: String,
        promisedWeightMcg: Long,
        agreedValuePaise: Long,
        dueDateEpochDay: Long?,
        notes: String,
    ): Long {
        require(counterpartyName.isNotBlank()) { "Counterparty name is required." }
        require(promisedWeightMcg > 0L) { "Promised weight must be positive." }
        val now = System.currentTimeMillis()
        val today = LocalDate.now().toEpochDay()
        val initialStatus = if (dueDateEpochDay != null && dueDateEpochDay < today) {
            GoldObligationStatus.OVERDUE.name
        } else {
            GoldObligationStatus.OPEN.name
        }
        // IMPORTANT: Creating an outgoing obligation does NOT reduce physical inventory.
        return goldDao.insertObligation(
            GoldObligationEntity(
                direction = direction.name,
                counterpartyName = counterpartyName.trim(),
                description = description.trim(),
                purity = purity.name,
                purityPercentText = purityPercentText,
                totalPromisedWeightMcg = promisedWeightMcg,
                settledWeightMcg = 0L,
                agreedValuePaise = agreedValuePaise,
                createdDateEpochDay = today,
                dueDateEpochDay = dueDateEpochDay,
                status = initialStatus,
                notes = notes,
                createdAt = now,
                updatedAt = now,
            )
        )
    }

    suspend fun cancelObligation(obligation: GoldObligationEntity) {
        goldDao.updateObligation(
            obligation.copy(
                status = GoldObligationStatus.CANCELLED.name,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun settleIncomingObligation(
        obligationId: Long,
        receivedWeightMcg: Long,
        settlementDateEpochDay: Long,
        convertToInventory: Boolean,
        categoryId: Long?,
        itemTitle: String,
        storageLocation: String,
        notes: String,
    ) {
        require(receivedWeightMcg > 0L) { "Received weight must be positive." }
        db.withTransaction {
            val now = System.currentTimeMillis()
            val ob = goldDao.getObligationById(obligationId) ?: error("Obligation not found.")
            require(ob.direction == GoldObligationDirection.INCOMING.name) { "Obligation is not incoming." }
            val remaining = (ob.totalPromisedWeightMcg - ob.settledWeightMcg).coerceAtLeast(0L)
            require(receivedWeightMcg <= remaining) {
                "Received weight exceeds remaining ${WeightFormatter.formatGrams(remaining)}."
            }

            var createdItemId: Long? = null
            if (convertToInventory) {
                val resolvedCategoryId = categoryId ?: goldDao.getCategories().firstOrNull()?.id
                    ?: error("No gold category available.")
                val allocatedCost = if (ob.agreedValuePaise > 0L && ob.totalPromisedWeightMcg > 0L) {
                    GoldCalculator.allocateAcquisitionCostPaise(
                        remainingAcquisitionCostPaise = ob.agreedValuePaise,
                        remainingNetWeightMcg = ob.totalPromisedWeightMcg,
                        soldNetWeightMcg = receivedWeightMcg,
                    )
                } else 0L

                createdItemId = goldDao.insertItem(
                    GoldItemEntity(
                        title = itemTitle.ifBlank { "${ob.counterpartyName} Settlement" },
                        categoryId = resolvedCategoryId,
                        purity = ob.purity,
                        purityPercentText = ob.purityPercentText,
                        isBulkLot = true,
                        quantity = 1,
                        originalQuantity = 1,
                        grossWeightMcg = receivedWeightMcg,
                        stoneWeightMcg = 0L,
                        netWeightMcg = receivedWeightMcg,
                        originalNetWeightMcg = receivedWeightMcg,
                        acquisitionCostPaise = allocatedCost,
                        originalAcquisitionCostPaise = allocatedCost,
                        purchaseDateEpochDay = settlementDateEpochDay,
                        merchantName = ob.counterpartyName,
                        storageLocation = storageLocation,
                        notes = notes.ifBlank { "Converted from incoming obligation" },
                        status = GoldItemStatus.ACTIVE.name,
                        createdAt = now,
                        updatedAt = now,
                    )
                )
            }

            val newSettled = ob.settledWeightMcg + receivedWeightMcg
            val newStatus = resolveObligationStatus(ob, newSettled)
            goldDao.updateObligation(
                ob.copy(
                    settledWeightMcg = newSettled,
                    status = newStatus.name,
                    updatedAt = now,
                )
            )

            goldDao.insertSettlement(
                GoldObligationSettlementEntity(
                    obligationId = ob.id,
                    settlementDateEpochDay = settlementDateEpochDay,
                    timestamp = now,
                    settledWeightMcg = receivedWeightMcg,
                    goldItemId = createdItemId,
                    convertedToInventory = convertToInventory,
                    notes = notes,
                )
            )

            if (convertToInventory) {
                recordValuationSnapshotInternal(settlementDateEpochDay, now)
            }
        }
    }

    suspend fun settleOutgoingObligation(
        obligationId: Long,
        deliveredWeightMcg: Long,
        sourceGoldItemId: Long,
        settlementDateEpochDay: Long,
        notes: String,
    ) {
        require(deliveredWeightMcg > 0L) { "Delivered weight must be positive." }
        db.withTransaction {
            val now = System.currentTimeMillis()
            val ob = goldDao.getObligationById(obligationId) ?: error("Obligation not found.")
            require(ob.direction == GoldObligationDirection.OUTGOING.name) { "Obligation is not outgoing." }
            val remaining = (ob.totalPromisedWeightMcg - ob.settledWeightMcg).coerceAtLeast(0L)
            require(deliveredWeightMcg <= remaining) {
                "Delivered weight exceeds remaining ${WeightFormatter.formatGrams(remaining)}."
            }

            val sourceItem = goldDao.getItemById(sourceGoldItemId)
                ?: error("Select an existing inventory item for outgoing delivery.")
            require(sourceItem.status != GoldItemStatus.SOLD.name && sourceItem.status != GoldItemStatus.ARCHIVED.name) {
                "Selected inventory item is not active."
            }
            require(sourceItem.netWeightMcg >= deliveredWeightMcg) {
                "Selected item only has ${WeightFormatter.formatGrams(sourceItem.netWeightMcg)} available."
            }

            val allocatedCost = GoldCalculator.allocateAcquisitionCostPaise(
                remainingAcquisitionCostPaise = sourceItem.acquisitionCostPaise,
                remainingNetWeightMcg = sourceItem.netWeightMcg,
                soldNetWeightMcg = deliveredWeightMcg,
            )
            val remainingNet = (sourceItem.netWeightMcg - deliveredWeightMcg).coerceAtLeast(0L)
            val remainingCost = (sourceItem.acquisitionCostPaise - allocatedCost).coerceAtLeast(0L)
            val nextItemStatus = if (remainingNet == 0L) {
                GoldItemStatus.SOLD.name
            } else {
                GoldItemStatus.PARTIALLY_SOLD.name
            }
            goldDao.updateItem(
                sourceItem.copy(
                    grossWeightMcg = if (remainingNet == 0L) 0L else (sourceItem.grossWeightMcg - deliveredWeightMcg).coerceAtLeast(remainingNet),
                    netWeightMcg = remainingNet,
                    acquisitionCostPaise = remainingCost,
                    quantity = if (remainingNet == 0L) 0 else sourceItem.quantity,
                    status = nextItemStatus,
                    updatedAt = now,
                )
            )

            val newSettled = ob.settledWeightMcg + deliveredWeightMcg
            val newStatus = resolveObligationStatus(ob, newSettled)
            goldDao.updateObligation(
                ob.copy(
                    settledWeightMcg = newSettled,
                    status = newStatus.name,
                    updatedAt = now,
                )
            )

            goldDao.insertSettlement(
                GoldObligationSettlementEntity(
                    obligationId = ob.id,
                    settlementDateEpochDay = settlementDateEpochDay,
                    timestamp = now,
                    settledWeightMcg = deliveredWeightMcg,
                    goldItemId = sourceGoldItemId,
                    convertedToInventory = false,
                    notes = notes,
                )
            )

            recordValuationSnapshotInternal(settlementDateEpochDay, now)
        }
    }

    private fun resolveObligationStatus(ob: GoldObligationEntity, newSettledWeightMcg: Long): GoldObligationStatus {
        val today = LocalDate.now().toEpochDay()
        return when {
            newSettledWeightMcg >= ob.totalPromisedWeightMcg -> GoldObligationStatus.SETTLED
            newSettledWeightMcg > 0L -> GoldObligationStatus.PARTIAL
            ob.dueDateEpochDay != null && ob.dueDateEpochDay < today -> GoldObligationStatus.OVERDUE
            else -> GoldObligationStatus.OPEN
        }
    }

    // --- Market Rates & Valuation Snapshots ---
    suspend fun refreshMarketRateIfEligible(
        city: String = "Chennai",
        nowIst: ZonedDateTime = ZonedDateTime.now(GoldMarketSchedule.IST_ZONE),
    ): GoldRateRefreshResult {
        val cleanCity = city.ifBlank { "Chennai" }
        val eligibility = GoldMarketSchedule.evaluateFetchWindow(nowIst)
        val cachedForToday = goldDao.findRateSnapshot(
            rateDateIso = eligibility.istDateIso,
            source = GoldRateSource.NEBULA_METALS.name,
            city = cleanCity,
        )
        if (cachedForToday != null) {
            return GoldRateRefreshResult.AlreadyCachedToday(
                snapshot = cachedForToday,
                message = "Market rate for ${eligibility.istDateIso} ($cleanCity) is already cached today.",
            )
        }

        val latest = goldDao.getLatestRateSnapshot()
        if (!eligibility.isEligibleNow) {
            return GoldRateRefreshResult.OutsideMarketWindow(
                cached = latest,
                reason = eligibility.reason,
            )
        }

        val fetchedResult = marketRateProvider.fetchRates(cleanCity)
        return fetchedResult.fold(
            onSuccess = { parsed ->
                val saved = db.withTransaction {
                    val alreadyForApiDate = goldDao.findRateSnapshot(
                        rateDateIso = parsed.rateDateIso,
                        source = GoldRateSource.NEBULA_METALS.name,
                        city = parsed.city,
                    )
                    if (alreadyForApiDate != null) {
                        alreadyForApiDate
                    } else {
                        val id = goldDao.insertRateSnapshot(parsed)
                        recordValuationSnapshotInternal(LocalDate.now().toEpochDay(), System.currentTimeMillis())
                        parsed.copy(id = id)
                    }
                }
                GoldRateRefreshResult.Fetched(saved)
            },
            onFailure = { err ->
                GoldRateRefreshResult.Error(
                    cached = latest,
                    message = err.message ?: "Unable to fetch NebulaMetals rate.",
                )
            },
        )
    }

    suspend fun saveManualRate(
        city: String,
        rateDateIso: String,
        rate24kFinalPaise: Long,
        rate22kFinalPaise: Long,
        rate18kFinalPaise: Long,
        rate14kFinalPaise: Long,
        gstIncluded: Boolean,
    ): Long {
        require(rate24kFinalPaise > 0L) { "24K gold rate must be positive." }
        val now = System.currentTimeMillis()
        val resolved22 = if (rate22kFinalPaise > 0L) rate22kFinalPaise else (rate24kFinalPaise * 22L) / 24L
        val resolved18 = if (rate18kFinalPaise > 0L) rate18kFinalPaise else (rate24kFinalPaise * 18L) / 24L
        val resolved14 = if (rate14kFinalPaise > 0L) rate14kFinalPaise else (rate24kFinalPaise * 14L) / 24L

        fun baseOf(final: Long): Long = if (gstIncluded) (final * 100L) / 103L else final
        fun gstOf(final: Long): Long = if (gstIncluded) final - baseOf(final) else 0L

        return db.withTransaction {
            val id = goldDao.insertRateSnapshot(
                GoldRateSnapshotEntity(
                    city = city.ifBlank { "Manual" },
                    market = "User Entry",
                    rateDateIso = rateDateIso,
                    source = GoldRateSource.MANUAL.name,
                    fetchedAtMillis = now,
                    gstIncluded = gstIncluded,
                    confidence = "manual",
                    rate24kBasePaise = baseOf(rate24kFinalPaise),
                    rate24kGstPaise = gstOf(rate24kFinalPaise),
                    rate24kFinalPaise = rate24kFinalPaise,
                    rate22kBasePaise = baseOf(resolved22),
                    rate22kGstPaise = gstOf(resolved22),
                    rate22kFinalPaise = resolved22,
                    rate18kBasePaise = baseOf(resolved18),
                    rate18kGstPaise = gstOf(resolved18),
                    rate18kFinalPaise = resolved18,
                    rate14kBasePaise = baseOf(resolved14),
                    rate14kGstPaise = gstOf(resolved14),
                    rate14kFinalPaise = resolved14,
                )
            )
            recordValuationSnapshotInternal(LocalDate.now().toEpochDay(), now)
            id
        }
    }

    suspend fun getRateOnOrBefore(dateIso: String): GoldRateSnapshotEntity? {
        return goldDao.getRateSnapshotOnOrBefore(dateIso)
    }

    private suspend fun recordValuationSnapshotInternal(epochDay: Long, nowMillis: Long) {
        val items = goldDao.getItems().map { it.toHoldingInput() }
        val latestRate = goldDao.getLatestRateSnapshot()?.toRateInput()
        val realized = goldDao.getSales().sumOf { it.realizedGainLossPaise }
        val portfolio = GoldCalculator.valuatePortfolio(
            items = items,
            rates = latestRate,
            realizedGainLossPaise = realized,
        )
        goldDao.insertValuationSnapshot(
            GoldValuationSnapshotEntity(
                snapshotDateEpochDay = epochDay,
                recordedAtMillis = nowMillis,
                totalNetWeightMcg = portfolio.holdings.totalPhysicalNetMcg,
                totalFineGoldMcg = portfolio.holdings.totalFineGoldMcg,
                totalAcquisitionCostPaise = portfolio.totalAcquisitionCostPaise,
                benchmark24kValuePaise = portfolio.currentBenchmarkValuePaise,
                correspondingPurityValuePaise = portfolio.currentPurityValuePaise,
                estimatedResaleValuePaise = portfolio.estimatedResaleValuePaise,
                rate24kFinalPaise = latestRate?.rate24kFinalPaise ?: 0L,
            )
        )
    }
}

fun GoldItemEntity.toHoldingInput(): GoldHoldingInput = GoldHoldingInput(
    id = id,
    categoryId = categoryId,
    purity = runCatching { GoldPurity.valueOf(purity) }.getOrDefault(GoldPurity.K22),
    purityPercentText = purityPercentText,
    netWeightMcg = netWeightMcg,
    acquisitionCostPaise = acquisitionCostPaise,
    status = runCatching { GoldItemStatus.valueOf(status) }.getOrDefault(GoldItemStatus.ACTIVE),
)

fun GoldRateSnapshotEntity.toRateInput(): GoldRateInput = GoldRateInput(
    rate24kBasePaise = rate24kBasePaise,
    rate24kFinalPaise = rate24kFinalPaise,
    rate22kFinalPaise = rate22kFinalPaise,
    rate18kFinalPaise = rate18kFinalPaise,
    rate14kFinalPaise = rate14kFinalPaise,
)
