package com.example.ui.screens.gold

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.gold.GoldMarketSchedule
import com.example.data.local.GoldCategoryEntity
import com.example.data.local.GoldItemEntity
import com.example.data.local.GoldObligationEntity
import com.example.data.local.GoldPurchaseEntity
import com.example.data.local.GoldRateSnapshotEntity
import com.example.data.local.GoldSaleEntity
import com.example.data.local.GoldValuationSnapshotEntity
import com.example.data.preferences.PreferencesRepository
import com.example.data.repository.GoldRepository
import com.example.data.repository.toHoldingInput
import com.example.data.repository.toRateInput
import com.example.domain.gold.GoldCalculator
import com.example.domain.gold.GoldHoldingsMetrics
import com.example.domain.gold.GoldItemStatus
import com.example.domain.gold.GoldObligationDirection
import com.example.domain.gold.GoldObligationStatus
import com.example.domain.gold.GoldPortfolioValuation
import com.example.domain.gold.GoldPurity
import com.example.domain.gold.GoldValuationMethod
import com.example.domain.model.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

enum class GoldRangeFilter(val label: String) {
    THIS_MONTH("This Month"),
    LAST_3M("3M"),
    THIS_YEAR("This Year"),
    ALL("All Time"),
}

data class GoldSlice(
    val label: String,
    val weightMcg: Long,
    val costPaise: Long,
    val colorIndex: Int,
)

data class GoldMonthlyFlowPoint(
    val label: String,
    val boughtPaise: Long,
    val soldPaise: Long,
    val boughtMcg: Long,
    val soldMcg: Long,
)

data class GoldDashboardUiState(
    val isLoading: Boolean = true,
    val preferences: AppPreferences = AppPreferences(),
    val valuationMethod: GoldValuationMethod = GoldValuationMethod.FINE_GOLD_24K,
    val rangeFilter: GoldRangeFilter = GoldRangeFilter.ALL,
    val portfolio: GoldPortfolioValuation = GoldPortfolioValuation(
        holdings = GoldHoldingsMetrics(0, 0, 0, 0, 0, 0, 0),
        totalAcquisitionCostPaise = 0L,
        currentBenchmarkValuePaise = 0L,
        currentPurityValuePaise = 0L,
        estimatedResaleValuePaise = 0L,
        unrealizedGainLossPaise = 0L,
        realizedGainLossPaise = 0L,
    ),
    val boughtThisMonthMcg: Long = 0L,
    val boughtThisMonthPaise: Long = 0L,
    val soldThisMonthMcg: Long = 0L,
    val soldThisMonthPaise: Long = 0L,
    val incomingPendingMcg: Long = 0L,
    val incomingOpenCount: Int = 0,
    val outgoingPendingMcg: Long = 0L,
    val outgoingOpenCount: Int = 0,
    val overdueObligationsCount: Int = 0,
    val latestRate: GoldRateSnapshotEntity? = null,
    val isRateStale: Boolean = true,
    val activeItemsCount: Int = 0,
    val puritySlices: List<GoldSlice> = emptyList(),
    val categorySlices: List<GoldSlice> = emptyList(),
    val monthlyFlows: List<GoldMonthlyFlowPoint> = emptyList(),
    val valuationSnapshots: List<GoldValuationSnapshotEntity> = emptyList(),
    val recentPurchases: List<GoldPurchaseEntity> = emptyList(),
    val recentSales: List<GoldSaleEntity> = emptyList(),
)

class GoldDashboardViewModel(
    private val goldRepository: GoldRepository,
    preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val valuationMethodFlow = MutableStateFlow(GoldValuationMethod.FINE_GOLD_24K)
    private val rangeFilterFlow = MutableStateFlow(GoldRangeFilter.ALL)

    init {
        viewModelScope.launch {
            goldRepository.ensureSeeded()
            goldRepository.refreshMarketRateIfEligible(city = "Chennai")
        }
    }

    private val goldCoreFlow = combine(
        goldRepository.categoriesFlow,
        goldRepository.itemsFlow,
        goldRepository.purchasesFlow,
        goldRepository.salesFlow,
        goldRepository.obligationsFlow,
    ) { categories, items, purchases, sales, obligations ->
        GoldCoreBundle(categories, items, purchases, sales, obligations)
    }

    val uiState: StateFlow<GoldDashboardUiState> = combine(
        goldCoreFlow,
        goldRepository.rateSnapshotsFlow,
        goldRepository.valuationSnapshotsFlow,
        preferencesRepository.preferencesFlow,
        valuationMethodFlow,
        rangeFilterFlow,
    ) { args ->
        val core = args[0] as GoldCoreBundle
        @Suppress("UNCHECKED_CAST")
        val rates = args[1] as List<GoldRateSnapshotEntity>
        @Suppress("UNCHECKED_CAST")
        val snapshots = args[2] as List<GoldValuationSnapshotEntity>
        val prefs = args[3] as AppPreferences
        val method = args[4] as GoldValuationMethod
        val rangeFilter = args[5] as GoldRangeFilter

        val latestRate = rates.firstOrNull()
        val isStale = GoldMarketSchedule.isRateStale(latestRate?.rateDateIso)
        val today = LocalDate.now()
        val currentYm = YearMonth.from(today)

        val filterStartEpochDay = when (rangeFilter) {
            GoldRangeFilter.THIS_MONTH -> today.withDayOfMonth(1).toEpochDay()
            GoldRangeFilter.LAST_3M -> today.minusMonths(3).toEpochDay()
            GoldRangeFilter.THIS_YEAR -> today.withDayOfYear(1).toEpochDay()
            GoldRangeFilter.ALL -> Long.MIN_VALUE
        }

        val activeItems = core.items.filter {
            it.status != GoldItemStatus.SOLD.name &&
                it.status != GoldItemStatus.ARCHIVED.name &&
                it.netWeightMcg > 0L
        }
        val filteredSales = core.sales.filter { it.saleDateEpochDay >= filterStartEpochDay }
        val filteredPurchases = core.purchases.filter { it.purchaseDateEpochDay >= filterStartEpochDay }

        val realizedPnl = filteredSales.sumOf { it.realizedGainLossPaise }
        val portfolio = GoldCalculator.valuatePortfolio(
            items = activeItems.map { it.toHoldingInput() },
            rates = latestRate?.toRateInput(),
            realizedGainLossPaise = realizedPnl,
            preferredMethod = method,
        )

        val monthStartEpochDay = currentYm.atDay(1).toEpochDay()
        val monthEndEpochDay = currentYm.atEndOfMonth().toEpochDay()
        val monthPurchases = core.purchases.filter { it.purchaseDateEpochDay in monthStartEpochDay..monthEndEpochDay }
        val monthSales = core.sales.filter { it.saleDateEpochDay in monthStartEpochDay..monthEndEpochDay }

        // Obligations summary
        var inMcg = 0L
        var inCount = 0
        var outMcg = 0L
        var outCount = 0
        var overdueCount = 0
        for (ob in core.obligations) {
            val status = runCatching { GoldObligationStatus.valueOf(ob.status) }.getOrDefault(GoldObligationStatus.OPEN)
            if (status == GoldObligationStatus.SETTLED || status == GoldObligationStatus.CANCELLED) continue
            val pending = (ob.totalPromisedWeightMcg - ob.settledWeightMcg).coerceAtLeast(0L)
            if (pending <= 0L) continue
            val isOverdue = ob.dueDateEpochDay != null && ob.dueDateEpochDay < today.toEpochDay()
            if (isOverdue || status == GoldObligationStatus.OVERDUE) overdueCount++
            if (ob.direction == GoldObligationDirection.INCOMING.name) {
                inMcg += pending
                inCount++
            } else {
                outMcg += pending
                outCount++
            }
        }

        // Purity slices
        val puritySlices = buildList {
            if (portfolio.holdings.weight24kMcg > 0L) {
                add(GoldSlice("24K / 999", portfolio.holdings.weight24kMcg, activeItems.filter { it.purity == GoldPurity.K24.name }.sumOf { it.acquisitionCostPaise }, 0))
            }
            if (portfolio.holdings.weight22kMcg > 0L) {
                add(GoldSlice("22K / 916", portfolio.holdings.weight22kMcg, activeItems.filter { it.purity == GoldPurity.K22.name }.sumOf { it.acquisitionCostPaise }, 1))
            }
            if (portfolio.holdings.weight18kMcg > 0L) {
                add(GoldSlice("18K / 750", portfolio.holdings.weight18kMcg, activeItems.filter { it.purity == GoldPurity.K18.name }.sumOf { it.acquisitionCostPaise }, 2))
            }
            if (portfolio.holdings.weight14kAndOtherMcg > 0L) {
                add(GoldSlice("14K / Custom", portfolio.holdings.weight14kAndOtherMcg, activeItems.filter { it.purity != GoldPurity.K24.name && it.purity != GoldPurity.K22.name && it.purity != GoldPurity.K18.name }.sumOf { it.acquisitionCostPaise }, 3))
            }
        }

        val catMap = core.categories.associateBy { it.id }
        val categorySlices = activeItems
            .groupBy { it.categoryId }
            .entries
            .mapIndexed { idx, (catId, group) ->
                GoldSlice(
                    label = catMap[catId]?.name ?: "Other",
                    weightMcg = group.sumOf { it.netWeightMcg },
                    costPaise = group.sumOf { it.acquisitionCostPaise },
                    colorIndex = idx,
                )
            }
            .sortedByDescending { it.weightMcg }

        // Last 6 months buy vs sell
        val ymFmt = DateTimeFormatter.ofPattern("MMM")
        val monthlyFlows = (5 downTo 0).map { offset ->
            val ym = currentYm.minusMonths(offset.toLong())
            val sDay = ym.atDay(1).toEpochDay()
            val eDay = ym.atEndOfMonth().toEpochDay()
            val buys = core.purchases.filter { it.purchaseDateEpochDay in sDay..eDay }
            val sells = core.sales.filter { it.saleDateEpochDay in sDay..eDay }
            GoldMonthlyFlowPoint(
                label = ym.format(ymFmt),
                boughtPaise = buys.sumOf { it.totalPaidPaise },
                soldPaise = sells.sumOf { it.netProceedsPaise },
                boughtMcg = buys.sumOf { it.totalNetWeightMcg },
                soldMcg = sells.sumOf { it.totalSoldNetWeightMcg },
            )
        }

        GoldDashboardUiState(
            isLoading = false,
            preferences = prefs,
            valuationMethod = method,
            rangeFilter = rangeFilter,
            portfolio = portfolio,
            boughtThisMonthMcg = monthPurchases.sumOf { it.totalNetWeightMcg },
            boughtThisMonthPaise = monthPurchases.sumOf { it.totalPaidPaise },
            soldThisMonthMcg = monthSales.sumOf { it.totalSoldNetWeightMcg },
            soldThisMonthPaise = monthSales.sumOf { it.netProceedsPaise },
            incomingPendingMcg = inMcg,
            incomingOpenCount = inCount,
            outgoingPendingMcg = outMcg,
            outgoingOpenCount = outCount,
            overdueObligationsCount = overdueCount,
            latestRate = latestRate,
            isRateStale = isStale,
            activeItemsCount = activeItems.size,
            puritySlices = puritySlices,
            categorySlices = categorySlices,
            monthlyFlows = monthlyFlows,
            valuationSnapshots = snapshots.filter { it.snapshotDateEpochDay >= filterStartEpochDay },
            recentPurchases = filteredPurchases.take(5),
            recentSales = filteredSales.take(5),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoldDashboardUiState(),
    )

    fun setValuationMethod(method: GoldValuationMethod) {
        valuationMethodFlow.value = method
    }

    fun setRangeFilter(filter: GoldRangeFilter) {
        rangeFilterFlow.value = filter
    }

    private data class GoldCoreBundle(
        val categories: List<GoldCategoryEntity>,
        val items: List<GoldItemEntity>,
        val purchases: List<GoldPurchaseEntity>,
        val sales: List<GoldSaleEntity>,
        val obligations: List<GoldObligationEntity>,
    )

    class Factory(
        private val goldRepository: GoldRepository,
        private val preferencesRepository: PreferencesRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GoldDashboardViewModel(goldRepository, preferencesRepository) as T
        }
    }
}
