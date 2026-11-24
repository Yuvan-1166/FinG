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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.GoldCategoryEntity
import com.example.data.local.GoldItemEntity
import com.example.data.local.GoldObligationEntity
import com.example.data.local.GoldObligationSettlementEntity
import com.example.domain.finance.MoneyFormatter
import com.example.domain.gold.GoldObligationDirection
import com.example.domain.gold.GoldObligationStatus
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
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldObligationsScreen(
    viewModel: GoldObligationsViewModel,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Incoming, 1 = Outgoing
    var showCreateDialog by remember { mutableStateOf(false) }
    var settlingIncoming by remember { mutableStateOf<GoldObligationEntity?>(null) }
    var settlingOutgoing by remember { mutableStateOf<GoldObligationEntity?>(null) }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    GoldTreasuryTheme {
        val gold = LocalGoldColors.current
        val currentDirection = if (selectedTab == 0) GoldObligationDirection.INCOMING else GoldObligationDirection.OUTGOING
        val displayedList = if (selectedTab == 0) state.incoming else state.outgoing

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Gold Obligations", fontWeight = FontWeight.Bold) },
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
                    onClick = { showCreateDialog = true },
                    containerColor = gold.champagne,
                    contentColor = Charcoal950,
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add Obligation")
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
                item {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            shape = SegmentedButtonDefaults.itemShape(0, 2),
                        ) {
                            Text("Incoming (${WeightFormatter.formatGrams(state.incomingPendingMcg, 2)})")
                        }
                        SegmentedButton(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            shape = SegmentedButtonDefaults.itemShape(1, 2),
                        ) {
                            Text("Outgoing (${WeightFormatter.formatGrams(state.outgoingPendingMcg, 2)})")
                        }
                    }
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = if (selectedTab == 0) {
                                "Incoming gold commitments add to your physical inventory only when you confirm receipt and choose Convert to Inventory."
                            } else {
                                "Outgoing gold obligations do not reduce physical inventory until you confirm delivery from an existing vault lot."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }

                if (displayedList.isEmpty()) {
                    item {
                        GoldEmptyCard(
                            title = "No ${currentDirection.label} Commitments",
                            subtitle = "Track pending gold advances, custom orders, bullion deliveries, or family commitments.",
                            actionLabel = "Add ${currentDirection.label}",
                            onActionClick = { showCreateDialog = true },
                        )
                    }
                } else {
                    items(displayedList, key = { it.id }) { ob ->
                        val settlements = state.settlementsByObligation[ob.id].orEmpty()
                        ObligationCard(
                            obligation = ob,
                            settlements = settlements,
                            datePref = state.preferences.dateFormat,
                            onSettle = {
                                if (ob.direction == GoldObligationDirection.INCOMING.name) {
                                    settlingIncoming = ob
                                } else {
                                    settlingOutgoing = ob
                                }
                            },
                            onCancel = { viewModel.cancelObligation(ob) },
                        )
                    }
                }
            }
        }

        if (showCreateDialog) {
            CreateObligationDialog(
                initialDirection = currentDirection,
                onCreate = { dir, cp, desc, purity, pct, weightMcg, valPaise, due, notes ->
                    viewModel.createObligation(dir, cp, desc, purity, pct, weightMcg, valPaise, due, notes)
                    showCreateDialog = false
                },
                onDismiss = { showCreateDialog = false },
            )
        }

        settlingIncoming?.let { ob ->
            SettleIncomingDialog(
                obligation = ob,
                categories = state.categories,
                onConfirm = { weightMcg, convert, catId, title, loc, notes ->
                    viewModel.settleIncoming(ob.id, weightMcg, convert, catId, title, loc, notes)
                    settlingIncoming = null
                },
                onDismiss = { settlingIncoming = null },
            )
        }

        settlingOutgoing?.let { ob ->
            SettleOutgoingDialog(
                obligation = ob,
                inventoryItems = state.activeInventoryItems,
                onConfirm = { weightMcg, itemId, notes ->
                    viewModel.settleOutgoing(ob.id, weightMcg, itemId, notes)
                    settlingOutgoing = null
                },
                onDismiss = { settlingOutgoing = null },
            )
        }
    }
}

@Composable
private fun ObligationCard(
    obligation: GoldObligationEntity,
    settlements: List<GoldObligationSettlementEntity>,
    datePref: com.example.domain.model.DateFormatPreference,
    onSettle: () -> Unit,
    onCancel: () -> Unit,
) {
    val gold = LocalGoldColors.current
    val purity = runCatching { GoldPurity.valueOf(obligation.purity) }.getOrDefault(GoldPurity.K22)
    val status = runCatching { GoldObligationStatus.valueOf(obligation.status) }.getOrDefault(GoldObligationStatus.OPEN)
    val remaining = (obligation.totalPromisedWeightMcg - obligation.settledWeightMcg).coerceAtLeast(0L)
    val progress = if (obligation.totalPromisedWeightMcg > 0L) {
        (obligation.settledWeightMcg.toDouble() / obligation.totalPromisedWeightMcg.toDouble()).toFloat().coerceIn(0f, 1f)
    } else 0f

    val statusColor = when (status) {
        GoldObligationStatus.SETTLED -> gold.gain
        GoldObligationStatus.OVERDUE -> gold.loss
        GoldObligationStatus.PARTIAL -> gold.champagneBright
        GoldObligationStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
        GoldObligationStatus.OPEN -> gold.mutedGold
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (status == GoldObligationStatus.OVERDUE) gold.loss.copy(alpha = 0.6f) else gold.goldBorder.copy(alpha = 0.6f)),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    Text(obligation.counterpartyName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    GoldPurityBadge(purity, obligation.purityPercentText)
                }
                Text(status.label, style = MaterialTheme.typography.labelSmall, color = statusColor, fontWeight = FontWeight.Bold)
            }
            if (obligation.description.isNotBlank()) {
                Text(obligation.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Promised: ${WeightFormatter.formatGrams(obligation.totalPromisedWeightMcg, 3)}",
                    style = MaterialTheme.typography.labelMedium.merge(MoneyTextStyle),
                )
                Text(
                    text = "Remaining: ${WeightFormatter.formatGrams(remaining, 3)}",
                    style = MaterialTheme.typography.labelMedium.merge(MoneyTextStyle),
                    color = gold.champagneBright,
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            obligation.dueDateEpochDay?.let { due ->
                Text(
                    text = "Due: ${formatEpochDay(due, datePref)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (status == GoldObligationStatus.OVERDUE) gold.loss else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (settlements.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                settlements.take(3).forEach { s ->
                    Text(
                        text = "• ${formatEpochDay(s.settlementDateEpochDay, datePref)}: ${WeightFormatter.formatGrams(s.settledWeightMcg, 3)}${if (s.convertedToInventory) " (Added to Vault)" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (status != GoldObligationStatus.SETTLED && status != GoldObligationStatus.CANCELLED && remaining > 0L) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onSettle,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = gold.champagne, contentColor = Charcoal950),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(
                            text = if (obligation.direction == GoldObligationDirection.INCOMING.name) "Record Receipt" else "Record Delivery",
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    OutlinedButton(onClick = onCancel, shape = RoundedCornerShape(10.dp)) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateObligationDialog(
    initialDirection: GoldObligationDirection,
    onCreate: (GoldObligationDirection, String, String, GoldPurity, String, Long, Long, Long?, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var direction by remember { mutableStateOf(initialDirection) }
    var counterparty by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var purity by remember { mutableStateOf(GoldPurity.K22) }
    var customPct by remember { mutableStateOf(GoldPurity.K22.defaultPercentText) }
    var weightText by remember { mutableStateOf("") }
    var valueText by remember { mutableStateOf("") }
    var dueDateEpochDay by remember { mutableStateOf<Long?>(null) }
    var showDuePicker by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Gold Obligation") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoldObligationDirection.entries.forEach { d ->
                        FilterChip(
                            selected = direction == d,
                            onClick = { direction = d },
                            label = { Text(d.label) },
                        )
                    }
                }
                OutlinedTextField(
                    value = counterparty,
                    onValueChange = { counterparty = it },
                    label = { Text("Counterparty / Jeweller / Person *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GoldPurity.entries.forEach { p ->
                        FilterChip(
                            selected = purity == p,
                            onClick = {
                                purity = p
                                if (p != GoldPurity.CUSTOM) customPct = p.defaultPercentText
                            },
                            label = { Text(p.shortLabel) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        label = { Text("Promised Weight (g) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = valueText,
                        onValueChange = { valueText = it },
                        label = { Text("Agreed Value (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedButton(onClick = { showDuePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(dueDateEpochDay?.let { "Due: ${java.time.LocalDate.ofEpochDay(it)}" } ?: "Set Optional Due Date")
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                )
                err?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val w = WeightFormatter.parseGramsToMicrograms(weightText)
                if (counterparty.isBlank()) {
                    err = "Enter counterparty name."
                    return@Button
                }
                if (w == null || w <= 0L) {
                    err = "Enter positive promised weight."
                    return@Button
                }
                val v = MoneyFormatter.parseRupeesToPaise(valueText.ifBlank { "0" }) ?: 0L
                val pct = if (purity == GoldPurity.CUSTOM) customPct else purity.defaultPercentText
                onCreate(direction, counterparty, description, purity, pct, w, v, dueDateEpochDay, notes)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )

    if (showDuePicker) {
        val dp = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDuePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dp.selectedDateMillis?.let { ms ->
                        dueDateEpochDay = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
                    }
                    showDuePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDuePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = dp) }
    }
}

@Composable
private fun SettleIncomingDialog(
    obligation: GoldObligationEntity,
    categories: List<GoldCategoryEntity>,
    onConfirm: (Long, Boolean, Long?, String, String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val remaining = (obligation.totalPromisedWeightMcg - obligation.settledWeightMcg).coerceAtLeast(0L)
    var weightText by remember { mutableStateOf(WeightFormatter.plainInput(remaining)) }
    var convertToInventory by remember { mutableStateOf(true) }
    var selectedCatId by remember { mutableStateOf(categories.firstOrNull()?.id) }
    var title by remember { mutableStateOf(obligation.description.ifBlank { "${obligation.counterpartyName} Gold" }) }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Incoming Receipt") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Remaining promised: ${WeightFormatter.formatGrams(remaining, 3)}", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Received Weight (g) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Convert to Physical Inventory", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = convertToInventory, onCheckedChange = { convertToInventory = it })
                }
                if (convertToInventory) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Vault Item Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCatId == cat.id,
                                onClick = { selectedCatId = cat.id },
                                label = { Text(cat.name) },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Storage Location") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Settlement Notes") },
                    modifier = Modifier.fillMaxWidth(),
                )
                err?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val w = WeightFormatter.parseGramsToMicrograms(weightText)
                if (w == null || w <= 0L || w > remaining) {
                    err = "Enter weight between 0 and ${WeightFormatter.formatGrams(remaining, 3)}."
                    return@Button
                }
                onConfirm(w, convertToInventory, selectedCatId, title, location, notes)
            }) { Text("Confirm Receipt") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SettleOutgoingDialog(
    obligation: GoldObligationEntity,
    inventoryItems: List<GoldItemEntity>,
    onConfirm: (Long, Long, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val remaining = (obligation.totalPromisedWeightMcg - obligation.settledWeightMcg).coerceAtLeast(0L)
    var weightText by remember { mutableStateOf(WeightFormatter.plainInput(remaining)) }
    var selectedItemId by remember { mutableStateOf(inventoryItems.firstOrNull()?.id) }
    var notes by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Outgoing Delivery") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Select existing inventory lot to deduct upon delivery:", style = MaterialTheme.typography.bodySmall)
                if (inventoryItems.isEmpty()) {
                    Text("No active inventory lots available to deliver.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                } else {
                    inventoryItems.forEach { item ->
                        FilterChip(
                            selected = selectedItemId == item.id,
                            onClick = { selectedItemId = item.id },
                            label = { Text("${item.title} (${WeightFormatter.formatGrams(item.netWeightMcg, 2)})") },
                        )
                    }
                }
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Delivered Weight (g) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Delivery Notes") },
                    modifier = Modifier.fillMaxWidth(),
                )
                err?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                enabled = inventoryItems.isNotEmpty(),
                onClick = {
                    val itemId = selectedItemId ?: return@Button
                    val item = inventoryItems.find { it.id == itemId } ?: return@Button
                    val w = WeightFormatter.parseGramsToMicrograms(weightText)
                    if (w == null || w <= 0L || w > remaining || w > item.netWeightMcg) {
                        err = "Delivered weight must not exceed remaining obligation or selected lot (${WeightFormatter.formatGrams(item.netWeightMcg, 3)})."
                        return@Button
                    }
                    onConfirm(w, itemId, notes)
                },
            ) { Text("Confirm Delivery") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
