package com.example.ui.screens.gold

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.finance.MoneyFormatter
import com.example.domain.gold.GoldCalculator
import com.example.domain.gold.GoldPurity
import com.example.domain.gold.WeightFormatter
import com.example.ui.components.formatEpochDay
import com.example.ui.theme.Charcoal950
import com.example.ui.theme.GoldEmptyCard
import com.example.ui.theme.GoldPurityBadge
import com.example.ui.theme.GoldTreasuryTheme
import com.example.ui.theme.LocalGoldColors
import com.example.ui.theme.MoneyTextStyle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellGoldScreen(
    viewModel: GoldTradeViewModel,
    preselectedItemId: Long,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.saveCompleted) {
        if (state.saveCompleted) onNavigateBack()
    }

    var selectedItemId by remember { mutableLongStateOf(preselectedItemId) }
    val selectedItem = state.activeHoldings.firstOrNull { it.id == selectedItemId }
        ?: state.activeHoldings.firstOrNull()

    var soldWeightText by remember { mutableStateOf("") }
    var soldQtyText by remember { mutableStateOf("1") }
    var saleRateText by remember { mutableStateOf("") }
    var deductionsText by remember { mutableStateOf("0") }
    var buyerName by remember { mutableStateOf("") }
    var saleDateEpochDay by remember { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var depositAccountId by remember { mutableStateOf<Long?>(null) }
    var notes by remember { mutableStateOf("") }

    LaunchedEffect(selectedItem?.id, state.latestRate) {
        val item = selectedItem ?: return@LaunchedEffect
        selectedItemId = item.id
        if (soldWeightText.isEmpty()) {
            soldWeightText = WeightFormatter.plainInput(item.netWeightMcg)
        }
        if (saleRateText.isEmpty()) {
            val p = runCatching { GoldPurity.valueOf(item.purity) }.getOrDefault(GoldPurity.K22)
            val suggested = viewModel.suggestedRatePerGramPaise(p, item.purityPercentText)
            if (suggested > 0L) saleRateText = MoneyFormatter.paiseToPlainInput(suggested)
        }
    }

    val soldMcg = WeightFormatter.parseGramsToMicrograms(soldWeightText) ?: 0L
    val ratePaise = MoneyFormatter.parseRupeesToPaise(saleRateText) ?: 0L
    val deductionsPaise = MoneyFormatter.parseRupeesToPaise(deductionsText.ifBlank { "0" }) ?: 0L
    val grossSalePaise = GoldCalculator.calculateGoldValuePaise(soldMcg, ratePaise)
    val netProceedsPaise = (grossSalePaise - deductionsPaise).coerceAtLeast(0L)
    val allocatedCostPaise = selectedItem?.let {
        GoldCalculator.allocateAcquisitionCostPaise(it.acquisitionCostPaise, it.netWeightMcg, soldMcg.coerceAtMost(it.netWeightMcg))
    } ?: 0L
    val previewPnlPaise = netProceedsPaise - allocatedCostPaise

    GoldTreasuryTheme {
        val gold = LocalGoldColors.current
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Sell / Dispose Gold", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            },
            bottomBar = {
                if (selectedItem != null) {
                    Surface(tonalElevation = 4.dp, color = MaterialTheme.colorScheme.surface) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            state.errorMessage?.let { err ->
                                Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 6.dp))
                            }
                            Button(
                                onClick = {
                                    viewModel.submitSale(
                                        item = selectedItem,
                                        soldWeightText = soldWeightText,
                                        soldQtyText = soldQtyText,
                                        saleRatePerGramText = saleRateText,
                                        deductionsText = deductionsText,
                                        buyerName = buyerName,
                                        saleDateEpochDay = saleDateEpochDay,
                                        depositAccountId = depositAccountId,
                                        notes = notes,
                                    )
                                },
                                enabled = !state.isSaving,
                                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("sell_gold_confirm_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = gold.champagne, contentColor = Charcoal950),
                            ) {
                                Text(if (state.isSaving) "Saving..." else "Confirm Gold Sale", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            if (state.activeHoldings.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
                    GoldEmptyCard(
                        title = "No Active Gold Holdings",
                        subtitle = "Add or purchase gold items in your vault before recording a sale.",
                    )
                }
                return@Scaffold
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Text("Select Holding to Dispose", style = MaterialTheme.typography.labelLarge, color = gold.champagneBright)
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.activeHoldings.forEach { holding ->
                            val isSelected = holding.id == selectedItem?.id
                            val purity = runCatching { GoldPurity.valueOf(holding.purity) }.getOrDefault(GoldPurity.K22)
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    selectedItemId = holding.id
                                    soldWeightText = WeightFormatter.plainInput(holding.netWeightMcg)
                                    val suggested = viewModel.suggestedRatePerGramPaise(purity, holding.purityPercentText)
                                    if (suggested > 0L) saleRateText = MoneyFormatter.paiseToPlainInput(suggested)
                                },
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                ),
                                border = BorderStroke(1.dp, if (isSelected) gold.champagne else gold.goldBorder.copy(alpha = 0.5f)),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(holding.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                            GoldPurityBadge(purity, holding.purityPercentText)
                                        }
                                        Text(
                                            text = "Available: ${WeightFormatter.formatGrams(holding.netWeightMcg, 3)} • Cost: ${MoneyFormatter.format(holding.acquisitionCostPaise)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedItem != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Disposal Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = gold.champagneBright)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = soldWeightText,
                                        onValueChange = { soldWeightText = it },
                                        label = { Text("Sold Net Weight (g) *") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1.3f),
                                    )
                                    OutlinedTextField(
                                        value = soldQtyText,
                                        onValueChange = { soldQtyText = it },
                                        label = { Text("Qty") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(0.7f),
                                    )
                                }
                                TextButton(
                                    onClick = { soldWeightText = WeightFormatter.plainInput(selectedItem.netWeightMcg) },
                                ) {
                                    Text("Use Full Available Weight (${WeightFormatter.formatGrams(selectedItem.netWeightMcg, 3)})")
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = saleRateText,
                                        onValueChange = { saleRateText = it },
                                        label = { Text("Sale Rate / g (₹) *") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                    )
                                    OutlinedTextField(
                                        value = deductionsText,
                                        onValueChange = { deductionsText = it },
                                        label = { Text("Melting/Fees (₹)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = buyerName,
                                        onValueChange = { buyerName = it },
                                        label = { Text("Buyer / Jeweller") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                    )
                                    OutlinedButton(
                                        onClick = { showDatePicker = true },
                                        modifier = Modifier.weight(1f).height(56.dp),
                                        shape = RoundedCornerShape(12.dp),
                                    ) {
                                        Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(formatEpochDay(saleDateEpochDay, state.preferences.dateFormat), maxLines = 1)
                                    }
                                }
                                Text("Deposit Proceeds To Account (Optional)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    FilterChip(
                                        selected = depositAccountId == null,
                                        onClick = { depositAccountId = null },
                                        label = { Text("No Account Link") },
                                    )
                                    state.accounts.forEach { acc ->
                                        FilterChip(
                                            selected = depositAccountId == acc.id,
                                            onClick = { depositAccountId = acc.id },
                                            label = { Text(acc.name) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = gold.champagne.copy(alpha = 0.2f),
                                                selectedLabelColor = gold.champagneBright,
                                            ),
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = notes,
                                    onValueChange = { notes = it },
                                    label = { Text("Notes") },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, gold.goldBorder),
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Cost Allocation & Realized P&L Preview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Gross Sale Amount", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(MoneyFormatter.format(grossSalePaise), style = MaterialTheme.typography.bodySmall.merge(MoneyTextStyle))
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Net Proceeds", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(MoneyFormatter.format(netProceedsPaise), style = MaterialTheme.typography.bodyMedium.merge(MoneyTextStyle), color = gold.champagneBright)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Allocated Cost Basis", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(MoneyFormatter.format(allocatedCostPaise), style = MaterialTheme.typography.bodySmall.merge(MoneyTextStyle))
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                val pnlColor = if (previewPnlPaise >= 0L) gold.gain else gold.loss
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Realized Gain / Loss", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = MoneyFormatter.format(previewPnlPaise, showPlusSign = true),
                                        style = MaterialTheme.typography.titleSmall.merge(MoneyTextStyle),
                                        color = pnlColor,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showDatePicker) {
            val dpState = rememberDatePickerState(
                initialSelectedDateMillis = LocalDate.ofEpochDay(saleDateEpochDay)
                    .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        dpState.selectedDateMillis?.let { millis ->
                            saleDateEpochDay = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
                        }
                        showDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
            ) { DatePicker(state = dpState) }
        }
    }
}
