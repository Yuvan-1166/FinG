package com.example.ui.screens.planning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.BudgetEntity
import com.example.data.local.CategoryEntity
import com.example.domain.finance.MoneyFormatter
import com.example.domain.model.BudgetPeriod
import com.example.domain.model.BudgetStatus
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.formatEpochDay
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.theme.MoneyTextStyle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    viewModel: BudgetsViewModel,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val financeColors = LocalFinanceColors.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showEditor by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<BudgetEntity?>(null) }
    var deletingBudget by remember { mutableStateOf<BudgetEntity?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budgets") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingBudget = null
                    showEditor = true
                },
                modifier = Modifier.testTag("add_budget_fab"),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Budget")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.alerts.isNotEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = financeColors.warningContainer,
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Warning, contentDescription = null, tint = financeColors.warning, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Budget Alerts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            state.alerts.forEach { alert ->
                                val msg = if (alert.status == BudgetStatus.EXCEEDED) {
                                    "${alert.name} exceeded (${alert.utilizationPercent}% used)"
                                } else {
                                    "${alert.name} approaching limit (${alert.utilizationPercent}% used)"
                                }
                                Text(msg, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            if (state.items.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No budgets yet",
                        subtitle = "Create an overall monthly budget, weekly limit, or category-specific spending cap.",
                        icon = Icons.Rounded.PieChart,
                        actionLabel = "Create Budget",
                        onActionClick = {
                            editingBudget = null
                            showEditor = true
                        },
                    )
                }
            } else {
                items(state.items, key = { it.entity.id }) { item ->
                    val b = item.entity
                    val u = item.utilization
                    val barColor = when (u.status) {
                        BudgetStatus.EXCEEDED -> financeColors.expense
                        BudgetStatus.APPROACHING -> financeColors.warning
                        BudgetStatus.NORMAL -> financeColors.income
                    }
                    val periodLabel = runCatching { BudgetPeriod.valueOf(b.periodType).label }.getOrDefault("Monthly")

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(b.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "${item.categoryName} • $periodLabel",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${u.utilizationPercent}%",
                                        style = MaterialTheme.typography.titleMedium.merge(MoneyTextStyle),
                                        color = barColor,
                                    )
                                    IconButton(onClick = {
                                        editingBudget = b
                                        showEditor = true
                                    }) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "Edit budget", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { deletingBudget = b }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Delete budget", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            LinearProgressIndicator(
                                progress = { (u.utilizationPercent / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(9.dp).clip(CircleShape),
                                color = barColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeCap = StrokeCap.Round,
                            )

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = "Spent ${MoneyFormatter.formatWithMode(u.spentPaise, state.preferences.currencyDisplayMode, false)} of ${MoneyFormatter.formatWithMode(u.limitPaise, state.preferences.currencyDisplayMode, false)}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(
                                    text = if (u.remainingPaise >= 0) {
                                        "Left ${MoneyFormatter.formatWithMode(u.remainingPaise, state.preferences.currencyDisplayMode, false)}"
                                    } else {
                                        "Over ${MoneyFormatter.formatWithMode(-u.remainingPaise, state.preferences.currencyDisplayMode, false)}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (u.remainingPaise < 0) financeColors.expense else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Text("Recent History", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                item.history.forEach { h ->
                                    Column {
                                        Text(h.periodLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${h.percent}%", style = MaterialTheme.typography.labelMedium.merge(MoneyTextStyle))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        BudgetEditorDialog(
            initial = editingBudget,
            categories = state.expenseCategories,
            onDismiss = { showEditor = false },
            onSave = { name, catId, limit, period, startDay, endDay, threshold ->
                viewModel.saveBudget(editingBudget?.id, name, catId, limit, period, startDay, endDay, threshold)
                showEditor = false
            },
        )
    }

    deletingBudget?.let { b ->
        ConfirmDeleteDialog(
            title = "Delete '${b.name}'?",
            message = "This removes the budget limit. Your transactions will not be affected.",
            onConfirm = {
                viewModel.deleteBudget(b.id)
                deletingBudget = null
            },
            onDismiss = { deletingBudget = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun BudgetEditorDialog(
    initial: BudgetEntity?,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (String, Long?, String, BudgetPeriod, Long?, Long?, Int) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var selectedCategoryId by remember { mutableStateOf(initial?.categoryId) }
    var limitInput by remember {
        mutableStateOf(initial?.let { MoneyFormatter.paiseToPlainInput(it.limitAmountPaise) } ?: "")
    }
    var period by remember {
        mutableStateOf(
            initial?.periodType?.let { runCatching { BudgetPeriod.valueOf(it) }.getOrNull() } ?: BudgetPeriod.MONTHLY,
        )
    }
    var customStartDay by remember {
        mutableStateOf(initial?.customStartEpochDay ?: LocalDate.now().withDayOfMonth(1).toEpochDay())
    }
    var customEndDay by remember {
        mutableStateOf(initial?.customEndEpochDay ?: LocalDate.now().plusDays(14).toEpochDay())
    }
    var threshold by remember { mutableIntStateOf(initial?.alertThresholdPercent ?: 80) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Create Budget" else "Edit Budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Budget Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("budget_name_input"),
                )
                OutlinedTextField(
                    value = limitInput,
                    onValueChange = { limitInput = it },
                    label = { Text("Limit Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("budget_limit_input"),
                )
                Text("Period", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BudgetPeriod.entries.forEach { p ->
                        FilterChip(
                            selected = period == p,
                            onClick = { period = p },
                            label = { Text(p.label) },
                        )
                    }
                }
                if (period == BudgetPeriod.CUSTOM) {
                    OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "${formatEpochDay(customStartDay, com.example.domain.model.DateFormatPreference.DD_MMM_YYYY)} - ${formatEpochDay(customEndDay, com.example.domain.model.DateFormatPreference.DD_MMM_YYYY)}",
                        )
                    }
                }
                Text("Scope", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = selectedCategoryId == null,
                        onClick = { selectedCategoryId = null },
                        label = { Text("Overall") },
                    )
                    categories.take(8).forEach { cat ->
                        FilterChip(
                            selected = selectedCategoryId == cat.id,
                            onClick = { selectedCategoryId = cat.id },
                            label = { Text(cat.name, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
                Text("Alert Threshold: $threshold%", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = threshold.toFloat(),
                    onValueChange = { threshold = it.toInt() },
                    valueRange = 50f..95f,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name, selectedCategoryId, limitInput, period, customStartDay, customEndDay, threshold)
                },
                modifier = Modifier.testTag("budget_save_button"),
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )

    if (showDatePicker) {
        val pickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val s = pickerState.selectedStartDateMillis
                        val e = pickerState.selectedEndDateMillis ?: s
                        if (s != null && e != null) {
                            val zone = ZoneId.systemDefault()
                            customStartDay = Instant.ofEpochMilli(s).atZone(zone).toLocalDate().toEpochDay()
                            customEndDay = Instant.ofEpochMilli(e).atZone(zone).toLocalDate().toEpochDay()
                        }
                        showDatePicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DateRangePicker(state = pickerState, modifier = Modifier.height(420.dp))
        }
    }
}
