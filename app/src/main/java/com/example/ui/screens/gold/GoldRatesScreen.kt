package com.example.ui.screens.gold

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.data.gold.GoldMarketSchedule
import com.example.data.local.GoldRateSnapshotEntity
import com.example.data.preferences.PreferencesRepository
import com.example.data.repository.GoldRateRefreshResult
import com.example.data.repository.GoldRepository
import com.example.domain.finance.MoneyFormatter
import com.example.domain.model.AppPreferences
import com.example.ui.theme.Charcoal950
import com.example.ui.theme.GoldEmptyCard
import com.example.ui.theme.GoldSectionHeader
import com.example.ui.theme.GoldTreasuryTheme
import com.example.ui.theme.LocalGoldColors
import com.example.ui.theme.MoneyTextStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class GoldRatesUiState(
    val selectedCity: String = "Chennai",
    val snapshots: List<GoldRateSnapshotEntity> = emptyList(),
    val latest: GoldRateSnapshotEntity? = null,
    val isStale: Boolean = true,
    val isRefreshing: Boolean = false,
    val statusBanner: String? = null,
    val preferences: AppPreferences = AppPreferences(),
)

class GoldRatesViewModel(
    private val goldRepository: GoldRepository,
    preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val cityFlow = MutableStateFlow("Chennai")
    private val refreshingFlow = MutableStateFlow(false)
    private val bannerFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<GoldRatesUiState> = combine(
        goldRepository.rateSnapshotsFlow,
        preferencesRepository.preferencesFlow,
        cityFlow,
        refreshingFlow,
        bannerFlow,
    ) { snapshots, prefs, city, refreshing, banner ->
        val latest = snapshots.firstOrNull()
        GoldRatesUiState(
            selectedCity = city,
            snapshots = snapshots,
            latest = latest,
            isStale = GoldMarketSchedule.isRateStale(latest?.rateDateIso),
            isRefreshing = refreshing,
            statusBanner = banner,
            preferences = prefs,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoldRatesUiState(),
    )

    fun selectCity(city: String) {
        cityFlow.value = city
    }

    fun fetchMarketRate() {
        if (refreshingFlow.value) return
        refreshingFlow.value = true
        val city = cityFlow.value
        viewModelScope.launch {
            val result = goldRepository.refreshMarketRateIfEligible(city)
            refreshingFlow.value = false
            bannerFlow.value = when (result) {
                is GoldRateRefreshResult.Fetched -> "Fetched ${result.snapshot.city} rates for ${result.snapshot.rateDateIso} from NebulaMetals."
                is GoldRateRefreshResult.AlreadyCachedToday -> result.message
                is GoldRateRefreshResult.OutsideMarketWindow -> result.reason
                is GoldRateRefreshResult.Error -> result.message
            }
        }
    }

    fun saveManualRate(
        city: String,
        rate24kPaise: Long,
        rate22kPaise: Long,
        rate18kPaise: Long,
        rate14kPaise: Long,
        gstIncluded: Boolean,
    ) {
        viewModelScope.launch {
            goldRepository.saveManualRate(
                city = city,
                rateDateIso = LocalDate.now().toString(),
                rate24kFinalPaise = rate24kPaise,
                rate22kFinalPaise = rate22kPaise,
                rate18kFinalPaise = rate18kPaise,
                rate14kFinalPaise = rate14kPaise,
                gstIncluded = gstIncluded,
            )
            bannerFlow.value = "Manual market rate saved locally."
        }
    }

    fun clearBanner() {
        bannerFlow.value = null
    }

    class Factory(
        private val goldRepository: GoldRepository,
        private val preferencesRepository: PreferencesRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GoldRatesViewModel(goldRepository, preferencesRepository) as T
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldRatesScreen(
    viewModel: GoldRatesViewModel,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showManualDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.statusBanner) {
        state.statusBanner?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearBanner()
        }
    }

    GoldTreasuryTheme {
        val gold = LocalGoldColors.current
        val cities = listOf("Chennai", "Mumbai", "Delhi", "Bengaluru", "Hyderabad", "Kolkata")

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Gold Market Rates", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showManualDialog = true }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Manual Rate Entry", tint = gold.champagne)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // City Selector & Fetch Action
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, gold.goldBorder),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("NebulaMetals Daily Feed (Mon–Fri after 10 AM IST)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                cities.forEach { city ->
                                    FilterChip(
                                        selected = state.selectedCity == city,
                                        onClick = { viewModel.selectCity(city) },
                                        label = { Text(city) },
                                    )
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = viewModel::fetchMarketRate,
                                    enabled = !state.isRefreshing,
                                    modifier = Modifier.weight(1f).testTag("fetch_gold_rates_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = gold.champagne, contentColor = Charcoal950),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (state.isRefreshing) "Checking..." else "Fetch Daily Rate", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { showManualDialog = true },
                                    modifier = Modifier.weight(1f).testTag("manual_gold_rate_button"),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Text("Manual Entry")
                                }
                            }
                        }
                    }
                }

                val latest = state.latest
                if (latest == null) {
                    item {
                        GoldEmptyCard(
                            title = "No Gold Rates Cached",
                            subtitle = "Fetch today's rate from NebulaMetals during market hours or enter a manual benchmark rate.",
                            actionLabel = "Enter Manual Rate",
                            onActionClick = { showManualDialog = true },
                        )
                    }
                } else {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column {
                                        Text("${latest.city} • ${latest.rateDateIso}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = gold.champagneBright)
                                        Text(
                                            text = "Source: ${latest.source.replace("_", " ")} ${if (latest.market.isNotBlank()) "• ${latest.market}" else ""}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    if (state.isStale) {
                                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.errorContainer) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            ) {
                                                Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = gold.loss, modifier = Modifier.size(14.dp))
                                                Text("Stale / Cached", style = MaterialTheme.typography.labelSmall, color = gold.loss)
                                            }
                                        }
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                RateDetailTableRow("24K (999)", latest.rate24kBasePaise, latest.rate24kGstPaise, latest.rate24kFinalPaise)
                                RateDetailTableRow("22K (916)", latest.rate22kBasePaise, latest.rate22kGstPaise, latest.rate22kFinalPaise)
                                RateDetailTableRow("18K (750)", latest.rate18kBasePaise, latest.rate18kGstPaise, latest.rate18kFinalPaise)
                                RateDetailTableRow("14K (585)", latest.rate14kBasePaise, latest.rate14kGstPaise, latest.rate14kFinalPaise)
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
                                GoldSectionHeader("24K Rate Trend (₹ / g)")
                                GoldTrendLineChart(
                                    points = state.snapshots.reversed().mapNotNull { snap ->
                                        val d = runCatching { LocalDate.parse(snap.rateDateIso).toEpochDay() }.getOrNull() ?: return@mapNotNull null
                                        GoldLinePoint(d, snap.rate24kFinalPaise)
                                    },
                                    valueFormatter = { MoneyFormatter.format(it, showDecimals = false) + "/g" },
                                    preferences = state.preferences,
                                    emptyMessage = "No rate history yet.",
                                )
                            }
                        }
                    }

                    item {
                        Text("Cached Rate History", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                    items(state.snapshots, key = { it.id }) { snap ->
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
                                    Text("${snap.rateDateIso} • ${snap.city}", style = MaterialTheme.typography.titleSmall)
                                    Text(snap.source.replace("_", " "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("24K: ${MoneyFormatter.format(snap.rate24kFinalPaise, false)}/g", style = MaterialTheme.typography.labelLarge.merge(MoneyTextStyle), color = gold.champagneBright)
                                    Text("22K: ${MoneyFormatter.format(snap.rate22kFinalPaise, false)}/g", style = MaterialTheme.typography.labelSmall.merge(MoneyTextStyle), color = gold.mutedGold)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showManualDialog) {
            ManualGoldRateDialog(
                defaultCity = state.selectedCity,
                onSave = { city, r24, r22, r18, r14, gst ->
                    viewModel.saveManualRate(city, r24, r22, r18, r14, gst)
                    showManualDialog = false
                },
                onDismiss = { showManualDialog = false },
            )
        }
    }
}

@Composable
private fun RateDetailTableRow(label: String, basePaise: Long, gstPaise: Long, finalPaise: Long) {
    val gold = LocalGoldColors.current
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                text = if (finalPaise > 0L) "${MoneyFormatter.format(finalPaise, false)} / 1g" else "—",
                style = MaterialTheme.typography.titleSmall.merge(MoneyTextStyle),
                color = gold.champagneBright,
            )
        }
        if (finalPaise > 0L) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Base ${MoneyFormatter.format(basePaise, false)} + GST ${MoneyFormatter.format(gstPaise, false)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "8g: ${MoneyFormatter.format(finalPaise * 8L, false)} • 10g: ${MoneyFormatter.format(finalPaise * 10L, false)}",
                    style = MaterialTheme.typography.labelSmall.merge(MoneyTextStyle),
                    color = gold.mutedGold,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun ManualGoldRateDialog(
    defaultCity: String,
    onSave: (String, Long, Long, Long, Long, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var city by remember { mutableStateOf(defaultCity) }
    var r24Text by remember { mutableStateOf("") }
    var r22Text by remember { mutableStateOf("") }
    var r18Text by remember { mutableStateOf("") }
    var r14Text by remember { mutableStateOf("") }
    var gstIncluded by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Manual Gold Rate") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City / Market") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = r24Text,
                    onValueChange = { r24Text = it },
                    label = { Text("24K Rate per gram (₹) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = r22Text,
                    onValueChange = { r22Text = it },
                    label = { Text("22K Rate per gram (₹, optional auto)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = r18Text,
                    onValueChange = { r18Text = it },
                    label = { Text("18K Rate per gram (₹, optional auto)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = r14Text,
                    onValueChange = { r14Text = it },
                    label = { Text("14K Rate per gram (₹, optional auto)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Prices Include 3% GST", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = gstIncluded, onCheckedChange = { gstIncluded = it })
                }
                err?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val p24 = MoneyFormatter.parseRupeesToPaise(r24Text)
                if (p24 == null || p24 <= 0L) {
                    err = "Enter a valid positive 24K rate per gram."
                    return@Button
                }
                val p22 = MoneyFormatter.parseRupeesToPaise(r22Text) ?: 0L
                val p18 = MoneyFormatter.parseRupeesToPaise(r18Text) ?: 0L
                val p14 = MoneyFormatter.parseRupeesToPaise(r14Text) ?: 0L
                onSave(city, p24, p22, p18, p14, gstIncluded)
            }) { Text("Save Rate") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
