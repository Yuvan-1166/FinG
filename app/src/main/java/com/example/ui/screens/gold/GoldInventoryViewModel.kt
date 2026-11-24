package com.example.ui.screens.gold

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.GoldCategoryEntity
import com.example.data.local.GoldItemEntity
import com.example.data.local.GoldRateSnapshotEntity
import com.example.data.local.GoldSaleItemEntity
import com.example.data.preferences.PreferencesRepository
import com.example.data.repository.DeleteResult
import com.example.data.repository.GoldRepository
import com.example.data.repository.toHoldingInput
import com.example.data.repository.toRateInput
import com.example.domain.gold.GoldCalculator
import com.example.domain.gold.GoldItemStatus
import com.example.domain.gold.GoldPurity
import com.example.domain.gold.GoldValuationBreakdown
import com.example.domain.model.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GoldInventoryFilter(
    val searchQuery: String = "",
    val categoryId: Long? = null,
    val purity: GoldPurity? = null,
    val statusFilter: GoldItemStatus? = null, // null = Active + Partially Sold
    val includeClosed: Boolean = false,
)

data class GoldItemWithValuation(
    val item: GoldItemEntity,
    val categoryName: String,
    val subcategoryName: String?,
    val valuation: GoldValuationBreakdown,
)

data class GoldInventoryUiState(
    val isLoading: Boolean = true,
    val filter: GoldInventoryFilter = GoldInventoryFilter(),
    val preferences: AppPreferences = AppPreferences(),
    val categories: List<GoldCategoryEntity> = emptyList(),
    val items: List<GoldItemWithValuation> = emptyList(),
    val latestRate: GoldRateSnapshotEntity? = null,
    val statusMessage: String? = null,
)

data class GoldItemDetailUiState(
    val item: GoldItemEntity? = null,
    val categoryName: String = "",
    val subcategoryName: String? = null,
    val valuation: GoldValuationBreakdown? = null,
    val saleHistory: List<GoldSaleItemEntity> = emptyList(),
    val preferences: AppPreferences = AppPreferences(),
)

class GoldInventoryViewModel(
    private val goldRepository: GoldRepository,
    preferencesRepository: PreferencesRepository,
    private val detailItemId: Long = -1L,
) : ViewModel() {

    private val filterFlow = MutableStateFlow(GoldInventoryFilter())
    private val statusMessageFlow = MutableStateFlow<String?>(null)

    private val baseFlow = combine(
        goldRepository.categoriesFlow,
        goldRepository.itemsFlow,
        goldRepository.rateSnapshotsFlow,
        preferencesRepository.preferencesFlow,
        filterFlow,
    ) { categories, items, rates, prefs, filter ->
        InventoryBundle(categories, items, rates.firstOrNull(), prefs, filter)
    }

    val uiState: StateFlow<GoldInventoryUiState> = combine(
        baseFlow,
        statusMessageFlow,
    ) { bundle, msg ->
        val catMap = bundle.categories.associateBy { it.id }
        val rateInput = bundle.latestRate?.toRateInput()
        val q = bundle.filter.searchQuery.trim().lowercase()

        val filtered = bundle.items.filter { item ->
            val st = runCatching { GoldItemStatus.valueOf(item.status) }.getOrDefault(GoldItemStatus.ACTIVE)
            if (bundle.filter.statusFilter != null) {
                if (st != bundle.filter.statusFilter) return@filter false
            } else if (!bundle.filter.includeClosed) {
                if (st == GoldItemStatus.SOLD || st == GoldItemStatus.ARCHIVED) return@filter false
            }
            if (bundle.filter.categoryId != null &&
                item.categoryId != bundle.filter.categoryId &&
                item.subcategoryId != bundle.filter.categoryId
            ) return@filter false
            if (bundle.filter.purity != null && item.purity != bundle.filter.purity.name) return@filter false

            if (q.isNotEmpty()) {
                val catName = catMap[item.categoryId]?.name?.lowercase() ?: ""
                val matches = item.title.lowercase().contains(q) ||
                    item.merchantName.lowercase().contains(q) ||
                    item.hallmarkCode.lowercase().contains(q) ||
                    item.storageLocation.lowercase().contains(q) ||
                    catName.contains(q)
                if (!matches) return@filter false
            }
            true
        }

        val enriched = filtered.map { item ->
            val valn = GoldCalculator.valuateItem(item.toHoldingInput(), rateInput)
            GoldItemWithValuation(
                item = item,
                categoryName = catMap[item.categoryId]?.name ?: "Other",
                subcategoryName = item.subcategoryId?.let { catMap[it]?.name },
                valuation = valn,
            )
        }

        GoldInventoryUiState(
            isLoading = false,
            filter = bundle.filter,
            preferences = bundle.prefs,
            categories = bundle.categories,
            items = enriched,
            latestRate = bundle.latestRate,
            statusMessage = msg,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoldInventoryUiState(),
    )

    val detailUiState: StateFlow<GoldItemDetailUiState> = combine(
        goldRepository.itemsFlow,
        goldRepository.categoriesFlow,
        goldRepository.rateSnapshotsFlow,
        goldRepository.saleItemsFlow,
        preferencesRepository.preferencesFlow,
    ) { items, categories, rates, saleItems, prefs ->
        val item = items.firstOrNull { it.id == detailItemId }
        val catMap = categories.associateBy { it.id }
        val latestRate = rates.firstOrNull()?.toRateInput()
        val valn = item?.let { GoldCalculator.valuateItem(it.toHoldingInput(), latestRate) }
        GoldItemDetailUiState(
            item = item,
            categoryName = item?.let { catMap[it.categoryId]?.name } ?: "",
            subcategoryName = item?.subcategoryId?.let { catMap[it]?.name },
            valuation = valn,
            saleHistory = saleItems.filter { it.goldItemId == detailItemId },
            preferences = prefs,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoldItemDetailUiState(),
    )

    fun setSearch(query: String) {
        filterFlow.update { it.copy(searchQuery = query) }
    }

    fun setCategoryFilter(categoryId: Long?) {
        filterFlow.update { it.copy(categoryId = categoryId) }
    }

    fun setPurityFilter(purity: GoldPurity?) {
        filterFlow.update { it.copy(purity = purity) }
    }

    fun setStatusFilter(status: GoldItemStatus?) {
        filterFlow.update { it.copy(statusFilter = status) }
    }

    fun toggleIncludeClosed() {
        filterFlow.update { it.copy(includeClosed = !it.includeClosed, statusFilter = null) }
    }

    fun saveCategory(name: String, parentCategoryId: Long?) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            goldRepository.saveCategory(
                GoldCategoryEntity(
                    name = clean,
                    parentCategoryId = parentCategoryId,
                    iconName = "diamond",
                    isPredefined = false,
                )
            )
            statusMessageFlow.value = "Gold category '$clean' saved."
        }
    }

    fun deleteOrArchiveCategory(cat: GoldCategoryEntity) {
        viewModelScope.launch {
            when (val res = goldRepository.deleteCategory(cat.id)) {
                is DeleteResult.Deleted -> statusMessageFlow.value = "Category '${cat.name}' deleted."
                is DeleteResult.BlockedInUse -> {
                    goldRepository.toggleCategoryArchive(cat)
                    statusMessageFlow.value = "${res.reason} Toggled archive instead."
                }
            }
        }
    }

    fun saveItem(item: GoldItemEntity) {
        viewModelScope.launch {
            runCatching { goldRepository.saveItem(item) }
                .onSuccess { statusMessageFlow.value = "Saved '${item.title}'." }
                .onFailure { statusMessageFlow.value = it.message ?: "Unable to save item." }
        }
    }

    fun toggleArchiveItem(item: GoldItemEntity) {
        viewModelScope.launch {
            goldRepository.toggleArchiveItem(item)
            statusMessageFlow.value = "Updated status for '${item.title}'."
        }
    }

    fun clearStatusMessage() {
        statusMessageFlow.value = null
    }

    private data class InventoryBundle(
        val categories: List<GoldCategoryEntity>,
        val items: List<GoldItemEntity>,
        val latestRate: GoldRateSnapshotEntity?,
        val prefs: AppPreferences,
        val filter: GoldInventoryFilter,
    )

    class Factory(
        private val goldRepository: GoldRepository,
        private val preferencesRepository: PreferencesRepository,
        private val detailItemId: Long = -1L,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GoldInventoryViewModel(goldRepository, preferencesRepository, detailItemId) as T
        }
    }
}
