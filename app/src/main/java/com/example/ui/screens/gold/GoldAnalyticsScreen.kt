package com.example.ui.screens.gold

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.GoldRateSnapshotEntity
import com.example.data.repository.GoldRepository
import com.example.data.repository.toHoldingInput
import com.example.data.repository.toRateInput
import com.example.domain.finance.MoneyFormatter
import com.example.domain.gold.GoldCalculator
import com.example.domain.gold.WeightFormatter
import com.example.ui.components.formatEpochDay
import com.example.ui.theme.GoldSectionHeader
import com.example.ui.theme.GoldTreasuryTheme
import com.example.ui.theme.LocalGoldColors
import com.example.ui.theme.MoneyTextStyle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldAnalyticsScreen(
    dashboardViewModel: GoldDashboardViewModel,
    goldRepository: GoldRepository,
    onNavigateBack: () -> Unit,
) {
    val dashState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val allItems by goldRepository.itemsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val allRates by goldRepository.rateSnapshotsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    var selectedHistoryEpochDay by remember { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var historicalRate by remember { mutableStateOf<GoldRateSnapshotEntity?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(selectedHistoryEpochDay, allRates) {
        val dateIso = LocalDate.ofEpochDay(selectedHistoryEpochDay).toString()
        historicalRate = goldRepository.getRateOnOrBefore(dateIso)
    }

    val historicalHoldings = remember(allItems, selectedHistoryEpochDay) {
        allItems.filter { it.purchaseDateEpochDay <= selectedHistoryEpochDay && it.originalNetWeightMcg > 0L }
            .map {
                it.toHoldingInput().copy(
                    netWeightMcg = it.originalNetWeightMcg,
                    acquisitionCostPaise = it.originalAcquisitionCostPaise,
                )
            }
    }
    val historicalValuation = remember(historicalHoldings, historicalRate) {
        GoldCalculator.valuatePortfolio(
            items = historicalHoldings,
            rates = historicalRate?.toRateInput(),
            realizedGainLossPaise = 0L,
        )
    }

    GoldTreasuryTheme {
        val gold = LocalGoldColors.current
        val prefs = dashState.preferences

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Gold Analytics & Valuation", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Historical Date Valuation Inspector
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, gold.goldBorder),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            GoldSectionHeader("Historical Gold Valuation Inspector")
                            Text(
                                text = "Select any date to inspect your acquired holdings valued using the nearest cached market rate on or before that date.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedButton(
                                onClick = { showDatePicker = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Date: ${formatEpochDay(selectedHistoryEpochDay, prefs.dateFormat)}")
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Fine Gold Held", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(WeightFormatter.formatFineGrams(historicalValuation.holdings.totalFineGoldMcg), style = MaterialTheme.typography.labelLarge.merge(MoneyTextStyle), color = gold.champagneBright)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Rate Used (${historicalRate?.rateDateIso ?: "None"})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = historicalRate?.let { "${MoneyFormatter.format(it.rate24kFinalPaise, false)}/g" } ?: "No rate snapshot",
                                    style = MaterialTheme.typography.labelLarge.merge(MoneyTextStyle),
                                )
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Historical 24K Benchmark", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (historicalRate != null) MoneyFormatter.formatWithMode(historicalValuation.currentBenchmarkValuePaise, prefs.currencyDisplayMode, false) else "—",
                                    style = MaterialTheme.typography.titleSmall.merge(MoneyTextStyle),
                                    color = gold.champagneBright,
                                )
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            GoldSectionHeader("Fine Gold Weight Trend (g)")
                            GoldTrendLineChart(
                                points = dashState.valuationSnapshots.map {
                                    GoldLinePoint(it.snapshotDateEpochDay, it.totalFineGoldMcg)
                                },
                                valueFormatter = { WeightFormatter.formatFineGrams(it) },
                                preferences = prefs,
                                emptyMessage = "Snapshots record automatically as you buy, sell, or settle gold.",
                            )
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            GoldSectionHeader("Historical Gold Price (24K ₹/g)")
                            GoldTrendLineChart(
                                points = allRates.reversed().mapNotNull { r ->
                                    val d = runCatching { LocalDate.parse(r.rateDateIso).toEpochDay() }.getOrNull() ?: return@mapNotNull null
                                    GoldLinePoint(d, r.rate24kFinalPaise)
                                },
                                valueFormatter = { MoneyFormatter.format(it, false) + "/g" },
                                preferences = prefs,
                                emptyMessage = "Fetch or enter market rates to chart gold price history.",
                            )
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            GoldSectionHeader("Monthly Acquisitions vs Disposals")
                            GoldMonthlyBuySellChart(points = dashState.monthlyFlows, preferences = prefs)
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            GoldSectionHeader("Purity Distribution")
                            GoldWeightDonutChart(slices = dashState.puritySlices, emptyMessage = "No active holdings.")
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            GoldSectionHeader("Category Distribution")
                            GoldWeightDonutChart(slices = dashState.categorySlices, emptyMessage = "No active holdings.")
                        }
                    }
                }
            }
        }

        if (showDatePicker) {
            val dpState = rememberDatePickerState(
                initialSelectedDateMillis = LocalDate.ofEpochDay(selectedHistoryEpochDay)
                    .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        dpState.selectedDateMillis?.let { ms ->
                            selectedHistoryEpochDay = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
                        }
                        showDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
            ) { DatePicker(state = dpState) }
        }
    }
}
