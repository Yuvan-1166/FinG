package com.example.ui.screens.gold

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.domain.finance.MoneyFormatter
import com.example.domain.gold.GoldItemStatus
import com.example.domain.gold.GoldPurity
import com.example.domain.gold.WeightFormatter
import com.example.ui.components.formatEpochDay
import com.example.ui.theme.Charcoal950
import com.example.ui.theme.GoldPurityBadge
import com.example.ui.theme.GoldTreasuryTheme
import com.example.ui.theme.LocalGoldColors
import com.example.ui.theme.MoneyTextStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldItemDetailScreen(
    viewModel: GoldInventoryViewModel,
    onNavigateBack: () -> Unit,
    onSellItem: (Long) -> Unit,
) {
    val detail by viewModel.detailUiState.collectAsStateWithLifecycle()
    val invState by viewModel.uiState.collectAsStateWithLifecycle()
    var showEditDialog by remember { mutableStateOf(false) }

    GoldTreasuryTheme {
        val gold = LocalGoldColors.current
        val item = detail.item
        val prefs = detail.preferences

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(item?.title ?: "Gold Item", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (item != null) {
                            IconButton(onClick = { showEditDialog = true }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Edit Item", tint = gold.champagne)
                            }
                            IconButton(onClick = { viewModel.toggleArchiveItem(item) }) {
                                Icon(Icons.Rounded.Archive, contentDescription = "Archive Item")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            if (item == null) {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text("Gold item not found.")
                }
                return@Scaffold
            }

            val purity = runCatching { GoldPurity.valueOf(item.purity) }.getOrDefault(GoldPurity.K22)
            val valn = detail.valuation
            val canSell = item.status != GoldItemStatus.SOLD.name &&
                item.status != GoldItemStatus.ARCHIVED.name &&
                item.netWeightMcg > 0L

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (item.itemPhotoUri != null) {
                    item {
                        AsyncImage(
                            model = item.itemPhotoUri,
                            contentDescription = item.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, gold.goldBorder),
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                GoldPurityBadge(purity, item.purityPercentText)
                                Text(item.status.replace("_", " "), style = MaterialTheme.typography.labelMedium, color = gold.mutedGold)
                            }
                            Text(
                                text = "Remaining Net Gold: ${WeightFormatter.formatGrams(item.netWeightMcg, 4)}",
                                style = MaterialTheme.typography.headlineSmall.merge(MoneyTextStyle),
                                color = gold.champagneBright,
                            )
                            Text(
                                text = "Fine Gold Content: ${WeightFormatter.formatFineGrams(valn?.fineGoldMcg ?: 0L)}  (24K Eq.)",
                                style = MaterialTheme.typography.bodyMedium.merge(MoneyTextStyle),
                                color = gold.mutedGold,
                            )
                            if (valn != null && valn.benchmark24kValuePaise > 0L) {
                                HorizontalDivider(color = gold.goldBorder.copy(alpha = 0.5f))
                                GoldDetailRow("24K Benchmark Value", MoneyFormatter.formatWithMode(valn.benchmark24kValuePaise, prefs.currencyDisplayMode, true))
                                GoldDetailRow("Purity Rate Value", MoneyFormatter.formatWithMode(valn.purityBenchmarkValuePaise, prefs.currencyDisplayMode, true))
                                GoldDetailRow("Est. Realizable Resale", MoneyFormatter.formatWithMode(valn.estimatedResaleValuePaise, prefs.currencyDisplayMode, true))
                                val pnlColor = if (valn.unrealizedGainLossPaise >= 0L) gold.gain else gold.loss
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Unrealized Gain / Loss", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = MoneyFormatter.formatWithMode(valn.unrealizedGainLossPaise, prefs.currencyDisplayMode, true, showPlusSign = true),
                                        style = MaterialTheme.typography.titleSmall.merge(MoneyTextStyle),
                                        color = pnlColor,
                                    )
                                }
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
                            Text("Weights & Acquisition Cost", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            GoldDetailRow("Category", buildString {
                                append(detail.categoryName)
                                detail.subcategoryName?.let { append(" • $it") }
                            })
                            GoldDetailRow("Quantity", "${item.quantity} / ${item.originalQuantity}")
                            GoldDetailRow("Gross Weight", WeightFormatter.formatGrams(item.grossWeightMcg, 4))
                            GoldDetailRow("Stone / Non-Gold Weight", WeightFormatter.formatGrams(item.stoneWeightMcg, 4))
                            GoldDetailRow("Original Net Weight", WeightFormatter.formatGrams(item.originalNetWeightMcg, 4))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            GoldDetailRow("Remaining Cost Basis", MoneyFormatter.formatWithMode(item.acquisitionCostPaise, prefs.currencyDisplayMode, true))
                            GoldDetailRow("Original Purchase Cost", MoneyFormatter.formatWithMode(item.originalAcquisitionCostPaise, prefs.currencyDisplayMode, true))
                            if (item.makingChargesPaise > 0L) GoldDetailRow("Making Charges", MoneyFormatter.format(item.makingChargesPaise))
                            if (item.wastageChargesPaise > 0L) GoldDetailRow("Wastage Charges", MoneyFormatter.format(item.wastageChargesPaise))
                            if (item.gstPaise > 0L) GoldDetailRow("GST Paid", MoneyFormatter.format(item.gstPaise))
                            if (item.otherChargesPaise > 0L) GoldDetailRow("Other Charges", MoneyFormatter.format(item.otherChargesPaise))
                            GoldDetailRow("Purchase Date", formatEpochDay(item.purchaseDateEpochDay, prefs.dateFormat))
                            if (item.merchantName.isNotBlank()) GoldDetailRow("Seller", item.merchantName)
                            if (item.hallmarkCode.isNotBlank()) GoldDetailRow("Hallmark / HUID", item.hallmarkCode)
                            if (item.invoiceRef.isNotBlank()) GoldDetailRow("Invoice Ref", item.invoiceRef)
                            if (item.storageLocation.isNotBlank()) GoldDetailRow("Storage Location", item.storageLocation)
                        }
                    }
                }

                if (item.receiptUri != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Receipt / Certificate", style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(10.dp))
                                AsyncImage(
                                    model = item.receiptUri,
                                    contentDescription = "Receipt",
                                    modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        }
                    }
                }

                if (detail.saleHistory.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Sale & Disposal History", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                detail.saleHistory.forEach { sale ->
                                    val pnlColor = if (sale.realizedGainLossPaise >= 0L) gold.gain else gold.loss
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column {
                                            Text("Sold ${WeightFormatter.formatGrams(sale.soldNetWeightMcg, 3)}", style = MaterialTheme.typography.bodyMedium)
                                            Text("Cost Basis ${MoneyFormatter.format(sale.allocatedCostBasisPaise)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(MoneyFormatter.format(sale.proceedsPaise), style = MaterialTheme.typography.titleSmall.merge(MoneyTextStyle))
                                            Text(
                                                text = MoneyFormatter.format(sale.realizedGainLossPaise, showPlusSign = true),
                                                style = MaterialTheme.typography.labelSmall.merge(MoneyTextStyle),
                                                color = pnlColor,
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                }

                if (canSell) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { onSellItem(item.id) },
                                modifier = Modifier.weight(1f).height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = gold.champagne, contentColor = Charcoal950),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(Icons.Rounded.Sell, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Sell / Dispose", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { showEditDialog = true },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text("Edit Details")
                            }
                        }
                    }
                }
            }
        }

        if (showEditDialog && item != null) {
            GoldItemEditorDialog(
                categories = invState.categories.filter { !it.isArchived },
                existing = item,
                onSave = {
                    viewModel.saveItem(it)
                    showEditDialog = false
                },
                onDismiss = { showEditDialog = false },
            )
        }
    }
}

@Composable
private fun GoldDetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.bodySmall.merge(MoneyTextStyle), color = MaterialTheme.colorScheme.onSurface)
    }
}
