package com.example.ui.screens.gold

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.finance.MoneyFormatter
import com.example.domain.gold.GoldValuationMethod
import com.example.domain.gold.WeightFormatter
import com.example.ui.components.formatEpochDay
import com.example.ui.theme.Charcoal950
import com.example.ui.theme.GoldEmptyCard
import com.example.ui.theme.GoldSectionHeader
import com.example.ui.theme.GoldTreasuryTheme
import com.example.ui.theme.LocalGoldColors
import com.example.ui.theme.MoneyTextStyle

@Composable
fun GoldDashboardScreen(
    viewModel: GoldDashboardViewModel,
    onBuyGold: () -> Unit,
    onSellGold: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenObligations: () -> Unit,
    onOpenRates: () -> Unit,
    onOpenAnalytics: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    GoldTreasuryTheme {
        val gold = LocalGoldColors.current
        val prefs = state.preferences
        val p = state.portfolio
        val h = p.holdings

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.statusBars,
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Gold Treasury",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = gold.champagneBright,
                            )
                            Text(
                                text = "${state.activeItemsCount} active vault lot(s) • 24K benchmark",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = onOpenRates)
                                .testTag("gold_header_rates_chip"),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, gold.goldBorder),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(Icons.Rounded.ShowChart, contentDescription = null, tint = gold.champagne, modifier = Modifier.size(16.dp))
                                Text(
                                    text = state.latestRate?.let {
                                        "24K ${MoneyFormatter.format(it.rate24kFinalPaise, showDecimals = false)}/g"
                                    } ?: "Set Rate",
                                    style = MaterialTheme.typography.labelMedium.merge(MoneyTextStyle),
                                    color = gold.champagneBright,
                                )
                            }
                        }
                    }
                }

                // Hero Vault Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        border = BorderStroke(1.dp, gold.goldBorder),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Brush.linearGradient(listOf(gold.heroStart, gold.heroEnd)))
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        text = "Total Fine Gold Content",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = WeightFormatter.formatFineGrams(h.totalFineGoldMcg),
                                        style = MaterialTheme.typography.displayMedium.merge(MoneyTextStyle),
                                        color = gold.champagneBright,
                                    )
                                    Text(
                                        text = "24K-Eq: ${WeightFormatter.formatGrams(h.total24kEquivalentMcg, 4)}  •  Physical Net: ${WeightFormatter.formatGrams(h.totalPhysicalNetMcg, 3)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = gold.mutedGold,
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(gold.champagne.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Rounded.Diamond, contentDescription = null, tint = gold.champagne)
                                }
                            }

                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                GoldValuationMethod.entries.forEachIndexed { idx, method ->
                                    SegmentedButton(
                                        selected = state.valuationMethod == method,
                                        onClick = { viewModel.setValuationMethod(method) },
                                        shape = SegmentedButtonDefaults.itemShape(idx, GoldValuationMethod.entries.size),
                                    ) {
                                        Text(
                                            text = if (method == GoldValuationMethod.FINE_GOLD_24K) "Fine × 24K" else "Purity Rate",
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = gold.goldBorder.copy(alpha = 0.6f))

                            val activeValuation = if (state.valuationMethod == GoldValuationMethod.FINE_GOLD_24K) {
                                p.currentBenchmarkValuePaise
                            } else {
                                p.currentPurityValuePaise
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text("Acquisition Cost", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = MoneyFormatter.formatWithMode(p.totalAcquisitionCostPaise, prefs.currencyDisplayMode, false),
                                        style = MaterialTheme.typography.titleMedium.merge(MoneyTextStyle),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Current Valuation", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = if (state.latestRate != null) {
                                            MoneyFormatter.formatWithMode(activeValuation, prefs.currencyDisplayMode, false)
                                        } else "—",
                                        style = MaterialTheme.typography.titleMedium.merge(MoneyTextStyle),
                                        color = gold.champagneBright,
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                val unrealizedColor = if (p.unrealizedGainLossPaise >= 0L) gold.gain else gold.loss
                                val realizedColor = if (p.realizedGainLossPaise >= 0L) gold.gain else gold.loss
                                Column {
                                    Text("Unrealized P&L", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = if (state.latestRate != null) {
                                            MoneyFormatter.formatWithMode(p.unrealizedGainLossPaise, prefs.currencyDisplayMode, false, showPlusSign = true)
                                        } else "Needs rate",
                                        style = MaterialTheme.typography.labelLarge.merge(MoneyTextStyle),
                                        color = if (state.latestRate != null) unrealizedColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Realized P&L", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = MoneyFormatter.formatWithMode(p.realizedGainLossPaise, prefs.currencyDisplayMode, false, showPlusSign = true),
                                        style = MaterialTheme.typography.labelLarge.merge(MoneyTextStyle),
                                        color = realizedColor,
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Est. Realizable Resale (ex-GST & making)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = if (state.latestRate != null) {
                                            MoneyFormatter.formatWithMode(p.estimatedResaleValuePaise, prefs.currencyDisplayMode, false)
                                        } else "—",
                                        style = MaterialTheme.typography.labelMedium.merge(MoneyTextStyle),
                                        color = gold.gain,
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick Actions Bar
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(
                                onClick = onBuyGold,
                                modifier = Modifier.weight(1f).height(48.dp).testTag("gold_buy_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = gold.champagne,
                                    contentColor = Charcoal950,
                                ),
                            ) {
                                Icon(Icons.Rounded.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Buy Gold", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = onSellGold,
                                modifier = Modifier.weight(1f).height(48.dp).testTag("gold_sell_button"),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, gold.goldBorder),
                            ) {
                                Icon(Icons.Rounded.Sell, contentDescription = null, tint = gold.champagne, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Sell Gold", color = gold.champagneBright, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            GoldQuickChip("Inventory", Icons.Rounded.Inventory2, Modifier.weight(1f).testTag("gold_inventory_chip"), onOpenInventory)
                            GoldQuickChip("Obligations", Icons.Rounded.Handshake, Modifier.weight(1f).testTag("gold_obligations_chip"), onOpenObligations)
                            GoldQuickChip("Rates", Icons.Rounded.CurrencyRupee, Modifier.weight(1f).testTag("gold_rates_chip"), onOpenRates)
                            GoldQuickChip("Analytics", Icons.Rounded.Analytics, Modifier.weight(1f).testTag("gold_analytics_chip"), onOpenAnalytics)
                        }
                    }
                }

                // Market Rate Banner
                item {
                    val rate = state.latestRate
                    Card(
                        modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp).clickable(onClick = onOpenRates),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, gold.goldBorder),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Market Gold Rates", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (state.isRateStale) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.errorContainer,
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                            ) {
                                                Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = gold.loss, modifier = Modifier.size(12.dp))
                                                Text("Cached / Stale", style = MaterialTheme.typography.labelSmall, color = gold.loss)
                                            }
                                        }
                                    }
                                    Text(
                                        text = rate?.rateDateIso ?: "Not Set",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (rate == null) {
                                Text(
                                    text = "No cached rate yet. Tap to fetch daily NebulaMetals rates or enter a manual market rate.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    RateMiniPill("24K (1g / 8g)", rate.rate24kFinalPaise, Modifier.weight(1f))
                                    RateMiniPill("22K (1g / 8g)", rate.rate22kFinalPaise, Modifier.weight(1f))
                                }
                                Text(
                                    text = "${rate.city} • ${rate.source.replace("_", " ")} • ${if (rate.gstIncluded) "Incl. GST" else "Ex-GST"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = gold.mutedGold,
                                )
                            }
                        }
                    }
                }

                // Purity Breakdown Grid & Monthly Movement
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            GoldSectionHeader("Physical Holdings by Purity", actionLabel = "Inventory", onActionClick = onOpenInventory)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                PurityMetricBox("24K", h.weight24kMcg, Modifier.weight(1f))
                                PurityMetricBox("22K", h.weight22kMcg, Modifier.weight(1f))
                                PurityMetricBox("18K", h.weight18kMcg, Modifier.weight(1f))
                                PurityMetricBox("Other", h.weight14kAndOtherMcg, Modifier.weight(1f))
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Bought This Month", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "${WeightFormatter.formatGrams(state.boughtThisMonthMcg, 2)} • ${MoneyFormatter.formatWithMode(state.boughtThisMonthPaise, prefs.currencyDisplayMode, false)}",
                                        style = MaterialTheme.typography.labelLarge.merge(MoneyTextStyle),
                                        color = gold.champagneBright,
                                    )
                                }
                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                    Text("Sold This Month", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "${WeightFormatter.formatGrams(state.soldThisMonthMcg, 2)} • ${MoneyFormatter.formatWithMode(state.soldThisMonthPaise, prefs.currencyDisplayMode, false)}",
                                        style = MaterialTheme.typography.labelLarge.merge(MoneyTextStyle),
                                        color = gold.gain,
                                    )
                                }
                            }
                        }
                    }
                }

                // Obligations Summary
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp).clickable(onClick = onOpenObligations),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            GoldSectionHeader(
                                title = "Gold Obligations",
                                actionLabel = if (state.overdueObligationsCount > 0) "${state.overdueObligationsCount} Overdue" else "Manage",
                                onActionClick = onOpenObligations,
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(Icons.Rounded.ArrowDownward, contentDescription = null, tint = gold.gain, modifier = Modifier.size(16.dp))
                                            Text("Incoming", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = WeightFormatter.formatGrams(state.incomingPendingMcg, 3),
                                            style = MaterialTheme.typography.titleMedium.merge(MoneyTextStyle),
                                            color = gold.gain,
                                        )
                                        Text("${state.incomingOpenCount} pending", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(Icons.Rounded.ArrowUpward, contentDescription = null, tint = gold.loss, modifier = Modifier.size(16.dp))
                                            Text("Outgoing", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = WeightFormatter.formatGrams(state.outgoingPendingMcg, 3),
                                            style = MaterialTheme.typography.titleMedium.merge(MoneyTextStyle),
                                            color = gold.loss,
                                        )
                                        Text("${state.outgoingOpenCount} pending", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                // Filter Chips for Charts & Recent Activity
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp).horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        GoldRangeFilter.entries.forEach { rf ->
                            FilterChip(
                                selected = state.rangeFilter == rf,
                                onClick = { viewModel.setRangeFilter(rf) },
                                label = { Text(rf.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = gold.champagne.copy(alpha = 0.20f),
                                    selectedLabelColor = gold.champagneBright,
                                ),
                            )
                        }
                    }
                }

                // Visual Charts Section
                if (state.activeItemsCount == 0 && state.recentPurchases.isEmpty() && state.recentSales.isEmpty()) {
                    item {
                        GoldEmptyCard(
                            title = "Your Gold Vault Is Empty",
                            subtitle = "Record physical jewellery, coins, bars, or bullion to track fine-gold weight, market valuation, and P&L.",
                            actionLabel = "Record First Gold Purchase",
                            onActionClick = onBuyGold,
                            modifier = Modifier.widthIn(max = 680.dp),
                        )
                    }
                } else {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                GoldSectionHeader("Purity & Category Distribution", actionLabel = "Analytics", onActionClick = onOpenAnalytics)
                                GoldWeightDonutChart(
                                    slices = state.puritySlices,
                                    emptyMessage = "No active gold holdings to visualize.",
                                )
                                if (state.categorySlices.isNotEmpty()) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Text("By Category", style = MaterialTheme.typography.labelLarge, color = gold.champagneBright)
                                    GoldWeightDonutChart(
                                        slices = state.categorySlices,
                                        emptyMessage = "",
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                GoldSectionHeader("Cost vs Market Valuation")
                                GoldValueComparisonBars(
                                    acquisitionCostPaise = p.totalAcquisitionCostPaise,
                                    benchmarkValuePaise = p.currentBenchmarkValuePaise,
                                    resaleValuePaise = p.estimatedResaleValuePaise,
                                    preferences = prefs,
                                )
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                GoldSectionHeader("Historical Portfolio Valuation")
                                GoldTrendLineChart(
                                    points = state.valuationSnapshots.map {
                                        GoldLinePoint(
                                            epochDay = it.snapshotDateEpochDay,
                                            primaryValue = if (it.benchmark24kValuePaise > 0L) it.benchmark24kValuePaise else it.totalAcquisitionCostPaise,
                                        )
                                    },
                                    valueFormatter = { MoneyFormatter.formatWithMode(it, prefs.currencyDisplayMode, false) },
                                    preferences = prefs,
                                    emptyMessage = "Valuation snapshots are recorded automatically with trades & rate updates.",
                                )
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                GoldSectionHeader("Monthly Gold Purchases vs Sales")
                                GoldMonthlyBuySellChart(points = state.monthlyFlows, preferences = prefs)
                            }
                        }
                    }
                }

                // Recent Gold Transactions
                if (state.recentPurchases.isNotEmpty() || state.recentSales.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                GoldSectionHeader("Recent Gold Ledger", actionLabel = "Analytics", onActionClick = onOpenAnalytics)
                                state.recentPurchases.take(3).forEach { purchase ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Bought • ${purchase.sellerName.ifBlank { "Gold Purchase" }}",
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = "${WeightFormatter.formatGrams(purchase.totalNetWeightMcg, 3)} • ${formatEpochDay(purchase.purchaseDateEpochDay, prefs.dateFormat)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Text(
                                            text = MoneyFormatter.formatWithMode(purchase.totalPaidPaise, prefs.currencyDisplayMode, false),
                                            style = MaterialTheme.typography.titleSmall.merge(MoneyTextStyle),
                                            color = gold.champagneBright,
                                        )
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                                state.recentSales.take(3).forEach { sale ->
                                    val pnlColor = if (sale.realizedGainLossPaise >= 0L) gold.gain else gold.loss
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Sold • ${sale.buyerName.ifBlank { "Gold Sale" }}",
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = "${WeightFormatter.formatGrams(sale.totalSoldNetWeightMcg, 3)} • ${formatEpochDay(sale.saleDateEpochDay, prefs.dateFormat)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = MoneyFormatter.formatWithMode(sale.netProceedsPaise, prefs.currencyDisplayMode, false),
                                                style = MaterialTheme.typography.titleSmall.merge(MoneyTextStyle),
                                                color = gold.gain,
                                            )
                                            Text(
                                                text = "P&L ${MoneyFormatter.formatWithMode(sale.realizedGainLossPaise, prefs.currencyDisplayMode, false, showPlusSign = true)}",
                                                style = MaterialTheme.typography.labelSmall.merge(MoneyTextStyle),
                                                color = pnlColor,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoldQuickChip(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val gold = LocalGoldColors.current
    Surface(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, gold.goldBorder.copy(alpha = 0.7f)),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, contentDescription = label, tint = gold.champagne, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun RateMiniPill(title: String, perGramPaise: Long, modifier: Modifier = Modifier) {
    val gold = LocalGoldColors.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = if (perGramPaise > 0L) {
                    "${MoneyFormatter.format(perGramPaise, showDecimals = false)} / ${MoneyFormatter.format(perGramPaise * 8L, showDecimals = false)}"
                } else "—",
                style = MaterialTheme.typography.labelMedium.merge(MoneyTextStyle),
                color = gold.champagneBright,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PurityMetricBox(label: String, weightMcg: Long, modifier: Modifier = Modifier) {
    val gold = LocalGoldColors.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = gold.mutedGold)
            Spacer(Modifier.height(2.dp))
            Text(
                text = WeightFormatter.formatGrams(weightMcg, decimals = 2),
                style = MaterialTheme.typography.labelMedium.merge(MoneyTextStyle),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
