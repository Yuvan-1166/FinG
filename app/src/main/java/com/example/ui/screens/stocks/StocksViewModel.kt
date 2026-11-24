package com.example.ui.screens.stocks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AccountEntity
import com.example.data.local.StockEntity
import com.example.data.local.StockHoldingEntity
import com.example.data.local.StockPriceSnapshotEntity
import com.example.data.local.StockTransactionEntity
import com.example.data.preferences.PreferencesRepository
import com.example.data.repository.FinanceRepository
import com.example.data.repository.StockRepository
import com.example.domain.model.AppPreferences
import com.example.domain.stock.StockCalculator
import com.example.domain.stock.StockExchange
import com.example.domain.stock.StockTxType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StockHoldingSummary(
    val stock: StockEntity,
    val holding: StockHoldingEntity,
    val currentMarketValuePaise: Long,
    val unrealizedPnlPaise: Long,
)

data class StocksUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val exchangeFilter: StockExchange? = null,
    val holdings: List<StockHoldingSummary> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val totalInvestedPaise: Long = 0L,
    val totalMarketValuePaise: Long = 0L,
    val totalUnrealizedPaise: Long = 0L,
    val totalRealizedPaise: Long = 0L,
    val preferences: AppPreferences = AppPreferences(),
    val statusMessage: String? = null,
)

data class StockDetailUiState(
    val stock: StockEntity? = null,
    val holding: StockHoldingEntity? = null,
    val marketValuePaise: Long = 0L,
    val unrealizedPnlPaise: Long = 0L,
    val transactions: List<StockTransactionEntity> = emptyList(),
    val priceHistory: List<StockPriceSnapshotEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val preferences: AppPreferences = AppPreferences(),
    val statusMessage: String? = null,
)

class StocksViewModel(
    private val stockRepository: StockRepository,
    financeRepository: FinanceRepository,
    preferencesRepository: PreferencesRepository,
    private val detailStockId: Long = -1L,
) : ViewModel() {

    private val searchFlow = MutableStateFlow("")
    private val exchangeFlow = MutableStateFlow<StockExchange?>(null)
    private val messageFlow = MutableStateFlow<String?>(null)

    private val repoFlow = combine(
        stockRepository.stocksFlow,
        stockRepository.holdingsFlow,
        stockRepository.transactionsFlow,
        stockRepository.priceSnapshotsFlow,
        financeRepository.accountsFlow,
    ) { stocks, holdings, txs, snapshots, accounts ->
        StockRepoBundle(stocks, holdings, txs, snapshots, accounts)
    }

    val uiState: StateFlow<StocksUiState> = combine(
        repoFlow,
        preferencesRepository.preferencesFlow,
        searchFlow,
        exchangeFlow,
        messageFlow,
    ) { bundle, prefs, q, exFilter, msg ->
        val stockMap = bundle.stocks.associateBy { it.id }
        val allSummaries = bundle.holdings.mapNotNull { h ->
            val st = stockMap[h.stockId] ?: return@mapNotNull null
            val mv = StockCalculator.calculateMarketValuePaise(h.quantityScale4, h.currentManualPricePaise)
            val unrl = if (h.quantityScale4 > 0L) mv - h.totalInvestedPaise else 0L
            StockHoldingSummary(st, h, mv, unrl)
        }

        val cleanQ = q.trim().lowercase()
        val filtered = allSummaries.filter { s ->
            if (exFilter != null && s.stock.exchange != exFilter.name) return@filter false
            if (cleanQ.isNotEmpty()) {
                val matches = s.stock.symbol.lowercase().contains(cleanQ) ||
                    s.stock.companyName.lowercase().contains(cleanQ) ||
                    s.stock.sector.lowercase().contains(cleanQ)
                if (!matches) return@filter false
            }
            true
        }

        val activeHoldings = allSummaries.filter { it.holding.quantityScale4 > 0L }
        StocksUiState(
            isLoading = false,
            searchQuery = q,
            exchangeFilter = exFilter,
            holdings = filtered.sortedByDescending { it.currentMarketValuePaise },
            accounts = bundle.accounts.filter { !it.isArchived },
            totalInvestedPaise = activeHoldings.sumOf { it.holding.totalInvestedPaise },
            totalMarketValuePaise = activeHoldings.sumOf { it.currentMarketValuePaise },
            totalUnrealizedPaise = activeHoldings.sumOf { it.unrealizedPnlPaise },
            totalRealizedPaise = allSummaries.sumOf { it.holding.realizedGainLossPaise },
            preferences = prefs,
            statusMessage = msg,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StocksUiState(),
    )

    val detailUiState: StateFlow<StockDetailUiState> = combine(
        repoFlow,
        preferencesRepository.preferencesFlow,
        messageFlow,
    ) { bundle, prefs, msg ->
        val stock = bundle.stocks.firstOrNull { it.id == detailStockId }
        val holding = bundle.holdings.firstOrNull { it.stockId == detailStockId }
        val mv = holding?.let { StockCalculator.calculateMarketValuePaise(it.quantityScale4, it.currentManualPricePaise) } ?: 0L
        val unrl = if (holding != null && holding.quantityScale4 > 0L) mv - holding.totalInvestedPaise else 0L
        StockDetailUiState(
            stock = stock,
            holding = holding,
            marketValuePaise = mv,
            unrealizedPnlPaise = unrl,
            transactions = bundle.txs.filter { it.stockId == detailStockId },
            priceHistory = bundle.snapshots.filter { it.stockId == detailStockId },
            accounts = bundle.accounts.filter { !it.isArchived },
            preferences = prefs,
            statusMessage = msg,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StockDetailUiState(),
    )

    fun setSearch(q: String) {
        searchFlow.value = q
    }

    fun setExchangeFilter(ex: StockExchange?) {
        exchangeFlow.value = ex
    }

    fun recordTrade(
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
    ) {
        viewModelScope.launch {
            runCatching {
                stockRepository.recordStockTrade(
                    symbol = symbol,
                    companyName = companyName,
                    exchange = exchange,
                    sector = sector,
                    type = type,
                    quantityScale4 = quantityScale4,
                    pricePerSharePaise = pricePerSharePaise,
                    chargesPaise = chargesPaise,
                    tradeDateEpochDay = tradeDateEpochDay,
                    accountId = accountId,
                    notes = notes,
                )
            }.onSuccess {
                messageFlow.value = "${type.label} trade for ${symbol.uppercase()} recorded."
            }.onFailure {
                messageFlow.value = it.message ?: "Failed to record trade."
            }
        }
    }

    fun updateManualPrice(stockId: Long, pricePaise: Long, note: String = "") {
        viewModelScope.launch {
            runCatching { stockRepository.updateManualPrice(stockId, pricePaise, note) }
                .onSuccess { messageFlow.value = "Manual stock price updated." }
                .onFailure { messageFlow.value = it.message ?: "Failed to update price." }
        }
    }

    fun clearMessage() {
        messageFlow.value = null
    }

    private data class StockRepoBundle(
        val stocks: List<StockEntity>,
        val holdings: List<StockHoldingEntity>,
        val txs: List<StockTransactionEntity>,
        val snapshots: List<StockPriceSnapshotEntity>,
        val accounts: List<AccountEntity>,
    )

    class Factory(
        private val stockRepository: StockRepository,
        private val financeRepository: FinanceRepository,
        private val preferencesRepository: PreferencesRepository,
        private val detailStockId: Long = -1L,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StocksViewModel(stockRepository, financeRepository, preferencesRepository, detailStockId) as T
        }
    }
}
