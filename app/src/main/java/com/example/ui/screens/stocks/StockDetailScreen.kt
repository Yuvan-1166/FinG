package com.example.ui.screens.stocks

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.finance.MoneyFormatter
import com.example.domain.stock.ShareFormatter
import com.example.domain.stock.StockExchange
import com.example.domain.stock.StockTxType
import com.example.ui.components.formatDateTimeMillis
import com.example.ui.components.formatEpochDay
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.theme.MoneyTextStyle
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(
    viewModel: StocksViewModel,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.detailUiState.collectAsStateWithLifecycle()
    val financeColors = LocalFinanceColors.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showPriceDialog by remember { mutableStateOf(false) }
    var tradeDialogType by remember { mutableStateOf<StockTxType?>(null) }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val stock = state.stock
    val holding = state.holding

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stock?.let { "${it.symbol} (${it.exchange})" } ?: "Stock Detail", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (stock != null) {
                        IconButton(onClick = { showPriceDialog = true }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Update Manual Price")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        if (stock == null || holding == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Stock holding not found.")
            }
            return@Scaffold
        }

        val prefs = state.preferences
        val unrlColor = if (state.unrealizedPnlPaise >= 0L) financeColors.income else financeColors.expense
        val realColor = if (holding.realizedGainLossPaise >= 0L) financeColors.income else financeColors.expense

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stock.companyName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "${ShareFormatter.formatShares(holding.quantityScale4)} shares held",
                            style = MaterialTheme.typography.headlineSmall.merge(MoneyTextStyle),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        StockDetailRow("Average Buy Cost", MoneyFormatter.format(holding.averageCostPaise))
                        StockDetailRow("Manual Market Price", MoneyFormatter.format(holding.currentManualPricePaise))
                        StockDetailRow("Price Updated", formatDateTimeMillis(holding.priceUpdatedAtMillis, prefs.dateFormat))
                        StockDetailRow("Total Invested", MoneyFormatter.format(holding.totalInvestedPaise))
                        StockDetailRow("Current Market Value", MoneyFormatter.format(state.marketValuePaise))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Unrealized P&L", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(MoneyFormatter.format(state.unrealizedPnlPaise, showPlusSign = true), style = MaterialTheme.typography.titleSmall.merge(MoneyTextStyle), color = unrlColor)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Realized P&L", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(MoneyFormatter.format(holding.realizedGainLossPaise, showPlusSign = true), style = MaterialTheme.typography.titleSmall.merge(MoneyTextStyle), color = realColor)
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { tradeDialogType = StockTxType.BUY },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("Buy More") }
                    OutlinedButton(
                        onClick = { tradeDialogType = StockTxType.SELL },
                        enabled = holding.quantityScale4 > 0L,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("Sell Shares") }
                    OutlinedButton(
                        onClick = { showPriceDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("Set Price") }
                }
            }

            item {
                Text("Trade Ledger", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }

            items(state.transactions, key = { it.id }) { tx ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "${tx.type} • ${ShareFormatter.formatShares(tx.quantityScale4)} @ ${MoneyFormatter.format(tx.pricePerSharePaise)}",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = formatEpochDay(tx.tradeDateEpochDay, prefs.dateFormat),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(MoneyFormatter.format(tx.totalAmountPaise), style = MaterialTheme.typography.titleSmall.merge(MoneyTextStyle))
                            if (tx.type == StockTxType.SELL.name) {
                                val c = if (tx.realizedGainLossPaise >= 0L) financeColors.income else financeColors.expense
                                Text(
                                    text = "P&L ${MoneyFormatter.format(tx.realizedGainLossPaise, showPlusSign = true)}",
                                    style = MaterialTheme.typography.labelSmall.merge(MoneyTextStyle),
                                    color = c,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPriceDialog && stock != null && holding != null) {
        var priceText by remember { mutableStateOf(MoneyFormatter.paiseToPlainInput(holding.currentManualPricePaise)) }
        var noteText by remember { mutableStateOf("") }
        var err by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPriceDialog = false },
            title = { Text("Update Manual Price (${stock.symbol})") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Market Price per Share (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Note (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    err?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val paise = MoneyFormatter.parseRupeesToPaise(priceText)
                    if (paise == null || paise <= 0L) {
                        err = "Enter valid positive share price."
                        return@Button
                    }
                    viewModel.updateManualPrice(stock.id, paise, noteText.trim())
                    showPriceDialog = false
                }) { Text("Save Price") }
            },
            dismissButton = { TextButton(onClick = { showPriceDialog = false }) { Text("Cancel") } },
        )
    }

    tradeDialogType?.let { txType ->
        if (stock != null) {
            val ex = runCatching { StockExchange.valueOf(stock.exchange) }.getOrDefault(StockExchange.NSE)
            StockTradeDialog(
                initialSymbol = stock.symbol,
                initialCompany = stock.companyName,
                initialExchange = ex,
                initialSector = stock.sector,
                initialType = txType,
                accounts = state.accounts,
                onSave = { sym, comp, exchange, sec, t, qty4, price, chg, accId, notes ->
                    viewModel.recordTrade(sym, comp, exchange, sec, t, qty4, price, chg, LocalDate.now().toEpochDay(), accId, notes)
                    tradeDialogType = null
                },
                onDismiss = { tradeDialogType = null },
            )
        }
    }
}

@Composable
private fun StockDetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.bodySmall.merge(MoneyTextStyle))
    }
}
