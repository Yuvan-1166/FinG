package com.example.ui.screens.gold

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AccountEntity
import com.example.data.local.GoldCategoryEntity
import com.example.data.local.GoldItemEntity
import com.example.data.local.GoldRateSnapshotEntity
import com.example.data.preferences.PreferencesRepository
import com.example.data.repository.FinanceRepository
import com.example.data.repository.GoldPurchaseDraft
import com.example.data.repository.GoldPurchaseLineDraft
import com.example.data.repository.GoldRepository
import com.example.data.repository.GoldSaleDraft
import com.example.data.repository.GoldSaleLineDraft
import com.example.data.repository.toRateInput
import com.example.domain.finance.MoneyFormatter
import com.example.domain.gold.GoldCalculator
import com.example.domain.gold.GoldItemStatus
import com.example.domain.gold.GoldPurity
import com.example.domain.gold.WeightFormatter
import com.example.domain.model.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class GoldTradeUiState(
    val categories: List<GoldCategoryEntity> = emptyList(),
    val activeHoldings: List<GoldItemEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val latestRate: GoldRateSnapshotEntity? = null,
    val preferences: AppPreferences = AppPreferences(),
    val isSaving: Boolean = false,
    val saveCompleted: Boolean = false,
    val errorMessage: String? = null,
)

data class BuyLineInput(
    val localId: Int,
    val title: String = "",
    val categoryId: Long = 0L,
    val subcategoryId: Long? = null,
    val purity: GoldPurity = GoldPurity.K22,
    val customPurityPercent: String = GoldPurity.K22.defaultPercentText,
    val isBulkLot: Boolean = false,
    val quantityText: String = "1",
    val grossWeightText: String = "",
    val stoneWeightText: String = "0",
    val ratePerGramText: String = "",
    val makingText: String = "0",
    val wastageText: String = "0",
    val gstText: String = "0",
    val otherChargesText: String = "0",
    val hallmarkCode: String = "",
    val storageLocation: String = "",
)

class GoldTradeViewModel(
    private val goldRepository: GoldRepository,
    financeRepository: FinanceRepository,
    preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val isSavingFlow = MutableStateFlow(false)
    private val saveCompletedFlow = MutableStateFlow(false)
    private val errorFlow = MutableStateFlow<String?>(null)

    private val repoBundleFlow = combine(
        goldRepository.categoriesFlow,
        goldRepository.itemsFlow,
        financeRepository.accountsFlow,
        goldRepository.rateSnapshotsFlow,
        preferencesRepository.preferencesFlow,
    ) { cats, items, accounts, rates, prefs ->
        TradeRepoBundle(cats, items, accounts, rates.firstOrNull(), prefs)
    }

    val uiState: StateFlow<GoldTradeUiState> = combine(
        repoBundleFlow,
        isSavingFlow,
        saveCompletedFlow,
        errorFlow,
    ) { bundle, saving, completed, err ->
        GoldTradeUiState(
            categories = bundle.categories.filter { !it.isArchived },
            activeHoldings = bundle.items.filter {
                it.status != GoldItemStatus.SOLD.name &&
                    it.status != GoldItemStatus.ARCHIVED.name &&
                    it.netWeightMcg > 0L
            },
            accounts = bundle.accounts.filter { !it.isArchived },
            latestRate = bundle.latestRate,
            preferences = bundle.prefs,
            isSaving = saving,
            saveCompleted = completed,
            errorMessage = err,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoldTradeUiState(),
    )

    fun submitPurchase(
        purchaseDateEpochDay: Long,
        sellerName: String,
        invoiceRef: String,
        paymentAccountId: Long?,
        receiptUri: String?,
        notes: String,
        lines: List<BuyLineInput>,
        defaultCategoryId: Long,
    ) {
        if (isSavingFlow.value) return
        errorFlow.value = null

        val drafts = mutableListOf<GoldPurchaseLineDraft>()
        for ((idx, line) in lines.withIndex()) {
            val cleanTitle = line.title.trim()
            if (cleanTitle.isEmpty()) {
                errorFlow.value = "Item #${idx + 1}: Title is required."
                return
            }
            val grossMcg = WeightFormatter.parseGramsToMicrograms(line.grossWeightText)
            val stoneMcg = WeightFormatter.parseGramsToMicrograms(line.stoneWeightText.ifBlank { "0" }) ?: 0L
            if (grossMcg == null || grossMcg <= 0L) {
                errorFlow.value = "Item #${idx + 1}: Enter valid positive gross weight."
                return
            }
            val netMcg = grossMcg - stoneMcg
            if (netMcg <= 0L) {
                errorFlow.value = "Item #${idx + 1}: Net gold weight (gross - stone) must be positive."
                return
            }
            val ratePaise = MoneyFormatter.parseRupeesToPaise(line.ratePerGramText)
            if (ratePaise == null || ratePaise <= 0L) {
                errorFlow.value = "Item #${idx + 1}: Enter a valid gold rate per gram."
                return
            }
            val making = MoneyFormatter.parseRupeesToPaise(line.makingText.ifBlank { "0" }) ?: 0L
            val wastage = MoneyFormatter.parseRupeesToPaise(line.wastageText.ifBlank { "0" }) ?: 0L
            val gst = MoneyFormatter.parseRupeesToPaise(line.gstText.ifBlank { "0" }) ?: 0L
            val other = MoneyFormatter.parseRupeesToPaise(line.otherChargesText.ifBlank { "0" }) ?: 0L
            val baseGoldCost = GoldCalculator.calculateGoldValuePaise(netMcg, ratePaise)
            val lineTotal = baseGoldCost + making + wastage + gst + other
            val catId = if (line.categoryId > 0L) line.categoryId else defaultCategoryId
            if (catId <= 0L) {
                errorFlow.value = "Item #${idx + 1}: Select a category."
                return
            }
            val pctText = if (line.purity == GoldPurity.CUSTOM) line.customPurityPercent.trim() else line.purity.defaultPercentText
            drafts.add(
                GoldPurchaseLineDraft(
                    title = cleanTitle,
                    categoryId = catId,
                    subcategoryId = line.subcategoryId,
                    purity = line.purity,
                    purityPercentText = pctText,
                    isBulkLot = line.isBulkLot,
                    quantity = line.quantityText.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                    grossWeightMcg = grossMcg,
                    stoneWeightMcg = stoneMcg,
                    netWeightMcg = netMcg,
                    ratePerGramPaise = ratePaise,
                    makingChargesPaise = making,
                    wastageChargesPaise = wastage,
                    gstPaise = gst,
                    otherChargesPaise = other,
                    totalCostPaise = lineTotal,
                    hallmarkCode = line.hallmarkCode.trim(),
                    storageLocation = line.storageLocation.trim(),
                )
            )
        }

        isSavingFlow.value = true
        viewModelScope.launch {
            runCatching {
                goldRepository.recordPurchase(
                    GoldPurchaseDraft(
                        purchaseDateEpochDay = purchaseDateEpochDay,
                        sellerName = sellerName.trim(),
                        invoiceRef = invoiceRef.trim(),
                        paymentAccountId = paymentAccountId,
                        receiptUri = receiptUri,
                        notes = notes.trim(),
                        items = drafts,
                    )
                )
            }.onSuccess {
                isSavingFlow.value = false
                saveCompletedFlow.value = true
            }.onFailure { err ->
                isSavingFlow.value = false
                errorFlow.value = err.message ?: "Failed to record gold purchase."
            }
        }
    }

    fun submitSale(
        item: GoldItemEntity?,
        soldWeightText: String,
        soldQtyText: String,
        saleRatePerGramText: String,
        deductionsText: String,
        buyerName: String,
        saleDateEpochDay: Long,
        depositAccountId: Long?,
        notes: String,
    ) {
        if (isSavingFlow.value) return
        errorFlow.value = null
        if (item == null) {
            errorFlow.value = "Select a gold item to sell."
            return
        }
        val soldMcg = WeightFormatter.parseGramsToMicrograms(soldWeightText)
        if (soldMcg == null || soldMcg <= 0L) {
            errorFlow.value = "Enter a valid positive weight to sell."
            return
        }
        if (soldMcg > item.netWeightMcg) {
            errorFlow.value = "Cannot sell more than available ${WeightFormatter.formatGrams(item.netWeightMcg)}."
            return
        }
        val ratePaise = MoneyFormatter.parseRupeesToPaise(saleRatePerGramText)
        if (ratePaise == null || ratePaise <= 0L) {
            errorFlow.value = "Enter a valid sale rate per gram."
            return
        }
        val deductions = MoneyFormatter.parseRupeesToPaise(deductionsText.ifBlank { "0" }) ?: 0L
        val grossProceeds = GoldCalculator.calculateGoldValuePaise(soldMcg, ratePaise)
        val soldQty = soldQtyText.toIntOrNull()?.coerceIn(1, item.quantity.coerceAtLeast(1)) ?: 1

        isSavingFlow.value = true
        viewModelScope.launch {
            runCatching {
                goldRepository.recordSale(
                    GoldSaleDraft(
                        saleDateEpochDay = saleDateEpochDay,
                        buyerName = buyerName.trim(),
                        saleRatePerGramPaise = ratePaise,
                        depositAccountId = depositAccountId,
                        notes = notes.trim(),
                        lines = listOf(
                            GoldSaleLineDraft(
                                goldItemId = item.id,
                                soldQuantity = soldQty,
                                soldNetWeightMcg = soldMcg,
                                grossProceedsPaise = grossProceeds,
                                deductionsPaise = deductions,
                            )
                        ),
                    )
                )
            }.onSuccess {
                isSavingFlow.value = false
                saveCompletedFlow.value = true
            }.onFailure { err ->
                isSavingFlow.value = false
                errorFlow.value = err.message ?: "Failed to record gold sale."
            }
        }
    }

    fun suggestedRatePerGramPaise(purity: GoldPurity, customPct: String): Long {
        val r = uiState.value.latestRate?.toRateInput() ?: return 0L
        return GoldCalculator.rateForPurity(r, purity, customPct)
    }

    private data class TradeRepoBundle(
        val categories: List<GoldCategoryEntity>,
        val items: List<GoldItemEntity>,
        val accounts: List<AccountEntity>,
        val latestRate: GoldRateSnapshotEntity?,
        val prefs: AppPreferences,
    )

    class Factory(
        private val goldRepository: GoldRepository,
        private val financeRepository: FinanceRepository,
        private val preferencesRepository: PreferencesRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GoldTradeViewModel(goldRepository, financeRepository, preferencesRepository) as T
        }
    }
}
