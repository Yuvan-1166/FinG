package com.example.ui.screens.stocks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AccountEntity
import com.example.domain.finance.MoneyFormatter
import com.example.domain.stock.ShareFormatter
import com.example.domain.stock.StockExchange
import com.example.domain.stock.StockTxType
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.theme.MoneyTextStyle
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StocksScreen(
    viewModel: StocksViewModel,
    onNavigateBack: () -> Unit,
    onOpenStockDetail: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val financeColors = LocalFinanceColors.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showTradeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stock Investments", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showTradeDialog = true },
                modifier = Modifier.testTag("add_stock_trade_fab"),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Record Stock Trade")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Manual Prices Disclaimer Banner
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Manual equity tracker — market values are calculated from prices you enter manually (not live exchange quotes).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Portfolio Summary Card
            item {
                val prefs = state.preferences
                val unrlColor = if (state.totalUnrealizedPaise >= 0L) financeColors.income else financeColors.expense
                val realColor = if (state.totalRealizedPaise >= 0L) financeColors.income else financeColors.expense
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Portfolio Valuation (Manual)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = MoneyFormatter.formatWithMode(state.totalMarketValuePaise, prefs.currencyDisplayMode, prefs.showPaiseDecimals),
                            style = MaterialTheme.typography.displayMedium.merge(MoneyTextStyle),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Invested", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    MoneyFormatter.formatWithMode(state.totalInvestedPaise, prefs.currencyDisplayMode, false),
                                    style = MaterialTheme.typography.titleSmall.merge(MoneyTextStyle),
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Unrealized P&L", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    MoneyFormatter.formatWithMode(state.totalUnrealizedPaise, prefs.currencyDisplayMode, false, showPlusSign = true),
                                    style = MaterialTheme.typography.titleSmall.merge(MoneyTextStyle),
                                    color = unrlColor,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Realized P&L", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    MoneyFormatter.formatWithMode(state.totalRealizedPaise, prefs.currencyDisplayMode, false, showPlusSign = true),
                                    style = MaterialTheme.typography.titleSmall.merge(MoneyTextStyle),
                                    color = realColor,
                                )
                            }
                        }
                    }
                }
            }

            // Search & Exchange Filters
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = viewModel::setSearch,
                        placeholder = { Text("Search symbol or company...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearch("") }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.exchangeFilter == null,
                            onClick = { viewModel.setExchangeFilter(null) },
                            label = { Text("All") },
                        )
                        StockExchange.entries.forEach { ex ->
                            FilterChip(
                                selected = state.exchangeFilter == ex,
                                onClick = { viewModel.setExchangeFilter(if (state.exchangeFilter == ex) null else ex) },
                                label = { Text(ex.label) },
                            )
                        }
                    }
                }
            }

            if (state.holdings.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No Stock Holdings Yet",
                        subtitle = "Log manual NSE/BSE buy and sell trades to track average buy cost, share count, and realized/unrealized P&L.",
                        icon = Icons.AutoMirrored.Rounded.TrendingUp,
                        actionLabel = "Add Stock Trade",
                        onActionClick = { showTradeDialog = true },
                    )
                }
            } else {
                val totalPortfolioVal = state.totalMarketValuePaise.coerceAtLeast(1L)
                items(state.holdings, key = { it.stock.id }) { item ->
                    val h = item.holding
                    val pnlColor = if (item.unrealizedPnlPaise >= 0L) financeColors.income else financeColors.expense
                    val allocRatio = if (h.quantityScale4 > 0L) {
                        (item.currentMarketValuePaise.toDouble() / totalPortfolioVal.toDouble()).toFloat().coerceIn(0f, 1f)
                    } else 0f

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenStockDetail(item.stock.id) }
                            .testTag("stock_card_${item.stock.symbol}"),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(item.stock.symbol, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                            Text(item.stock.exchange, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                    Text(
                                        text = item.stock.companyName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = MoneyFormatter.formatWithMode(item.currentMarketValuePaise, state.preferences.currencyDisplayMode, false),
                                        style = MaterialTheme.typography.titleSmall.merge(MoneyTextStyle),
                                    )
                                    Text(
                                        text = MoneyFormatter.formatWithMode(item.unrealizedPnlPaise, state.preferences.currencyDisplayMode, false, showPlusSign = true),
                                        style = MaterialTheme.typography.labelSmall.merge(MoneyTextStyle),
                                        color = pnlColor,
                                    )
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = "${ShareFormatter.formatShares(h.quantityScale4)} shares @ Avg ${MoneyFormatter.format(h.averageCostPaise, true)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "Manual Price: ${MoneyFormatter.format(h.currentManualPricePaise, true)}",
                                    style = MaterialTheme.typography.labelSmall.merge(MoneyTextStyle),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            if (h.quantityScale4 > 0L) {
                                LinearProgressIndicator(
                                    progress = { allocRatio },
                                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTradeDialog) {
        StockTradeDialog(
            initialSymbol = "",
            initialCompany = "",
            initialExchange = StockExchange.NSE,
            initialSector = "",
            initialType = StockTxType.BUY,
            accounts = state.accounts,
            onSave = { sym, comp, ex, sec, type, qty4, price, chg, accId, notes ->
                viewModel.recordTrade(sym, comp, ex, sec, type, qty4, price, chg, LocalDate.now().toEpochDay(), accId, notes)
                showTradeDialog = false
            },
            onDismiss = { showTradeDialog = false },
        )
    }
}

@Composable
fun StockTradeDialog(
    initialSymbol: String,
    initialCompany: String,
    initialExchange: StockExchange,
    initialSector: String,
    initialType: StockTxType,
    accounts: List<AccountEntity>,
    onSave: (String, String, StockExchange, String, StockTxType, Long, Long, Long, Long?, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var type by remember { mutableStateOf(initialType) }
    var symbol by remember { mutableStateOf(initialSymbol) }
    var company by remember { mutableStateOf(initialCompany) }
    var exchange by remember { mutableStateOf(initialExchange) }
    var sector by remember { mutableStateOf(initialSector) }
    var qtyText by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var chargesText by remember { mutableStateOf("0") }
    var accountId by remember { mutableStateOf<Long?>(null) }
    var notes by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Stock Trade") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StockTxType.entries.forEach { t ->
                        FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.label) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = symbol,
                        onValueChange = { symbol = it.uppercase() },
                        label = { Text("Ticker (e.g. TCS) *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        StockExchange.entries.forEach { ex ->
                            FilterChip(selected = exchange == ex, onClick = { exchange = ex }, label = { Text(ex.label) })
                        }
                    }
                }
                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Company Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = sector,
                    onValueChange = { sector = it },
                    label = { Text("Sector (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it },
                        label = { Text("Shares *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Price / Share (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = chargesText,
                    onValueChange = { chargesText = it },
                    label = { Text("Brokerage & Taxes (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Link Financial Account (Optional — records Asset Purchase/Sale)", style = MaterialTheme.typography.labelSmall)
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = accountId == null, onClick = { accountId = null }, label = { Text("No Link") })
                    accounts.forEach { acc ->
                        FilterChip(selected = accountId == acc.id, onClick = { accountId = acc.id }, label = { Text(acc.name) })
                    }
                }
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
                err?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val qty4 = ShareFormatter.parseSharesToScale4(qtyText)
                val price = MoneyFormatter.parseRupeesToPaise(priceText)
                val charges = MoneyFormatter.parseRupeesToPaise(chargesText.ifBlank { "0" }) ?: 0L
                if (symbol.isBlank()) {
                    err = "Enter stock symbol."
                    return@Button
                }
                if (qty4 == null || qty4 <= 0L) {
                    err = "Enter valid positive share quantity."
                    return@Button
                }
                if (price == null || price <= 0L) {
                    err = "Enter valid price per share."
                    return@Button
                }
                onSave(symbol.trim().uppercase(), company.trim(), exchange, sector.trim(), type, qty4, price, charges, accountId, notes.trim())
            }) { Text("Save Trade") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
