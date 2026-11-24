package com.example.ui.screens.activity

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.DateRangePreset
import com.example.domain.model.TransactionSortOrder
import com.example.domain.model.TransactionType
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MoneyText
import com.example.ui.components.TransactionRow
import com.example.ui.components.formatDateMillis
import com.example.ui.theme.LocalFinanceColors
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    viewModel: ActivityViewModel,
    onOpenTransactionDetail: (Long) -> Unit,
    onAddTransaction: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val financeColors = LocalFinanceColors.current

    var showSortMenu by remember { mutableStateOf(false) }
    var showTypeMenu by remember { mutableStateOf(false) }
    var showAccountMenu by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showCustomRangePicker by remember { mutableStateOf(false) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Activity",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Row {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier.testTag("activity_sort_button"),
                    ) {
                        Icon(Icons.Rounded.SwapVert, contentDescription = "Sort transactions")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                    ) {
                        TransactionSortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = { Text(order.label) },
                                onClick = {
                                    viewModel.setSortOrder(order)
                                    showSortMenu = false
                                },
                            )
                        }
                    }
                }
            }

            // Search bar
            OutlinedTextField(
                value = state.filter.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("activity_search_input"),
                placeholder = { Text("Search merchant, notes, tags, category...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.filter.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )

            Spacer(Modifier.height(10.dp))

            // Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Date preset chip
                var showDateMenu by remember { mutableStateOf(false) }
                FilterChip(
                    selected = state.filter.datePreset != null,
                    onClick = { showDateMenu = true },
                    label = { Text(state.filter.datePreset?.label ?: "All Dates") },
                    leadingIcon = {
                        Icon(Icons.Rounded.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                )
                DropdownMenu(expanded = showDateMenu, onDismissRequest = { showDateMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("All Dates") },
                        onClick = {
                            viewModel.setDatePreset(null)
                            showDateMenu = false
                        },
                    )
                    DateRangePreset.entries.forEach { preset ->
                        DropdownMenuItem(
                            text = { Text(preset.label) },
                            onClick = {
                                showDateMenu = false
                                if (preset == DateRangePreset.CUSTOM) {
                                    showCustomRangePicker = true
                                } else {
                                    viewModel.setDatePreset(preset)
                                }
                            },
                        )
                    }
                }

                // Type filter
                FilterChip(
                    selected = state.filter.selectedType != null,
                    onClick = { showTypeMenu = true },
                    label = { Text(state.filter.selectedType?.label ?: "All Types") },
                )
                DropdownMenu(expanded = showTypeMenu, onDismissRequest = { showTypeMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("All Types") },
                        onClick = {
                            viewModel.setTypeFilter(null)
                            showTypeMenu = false
                        },
                    )
                    TransactionType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.label) },
                            onClick = {
                                viewModel.setTypeFilter(type)
                                showTypeMenu = false
                            },
                        )
                    }
                }

                // Account filter
                FilterChip(
                    selected = state.filter.selectedAccountId != null,
                    onClick = { showAccountMenu = true },
                    label = {
                        Text(
                            state.filter.selectedAccountId?.let { state.accountMap[it]?.name } ?: "All Accounts",
                        )
                    },
                )
                DropdownMenu(expanded = showAccountMenu, onDismissRequest = { showAccountMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("All Accounts") },
                        onClick = {
                            viewModel.setAccountFilter(null)
                            showAccountMenu = false
                        },
                    )
                    state.accounts.forEach { acc ->
                        DropdownMenuItem(
                            text = { Text(acc.name) },
                            onClick = {
                                viewModel.setAccountFilter(acc.id)
                                showAccountMenu = false
                            },
                        )
                    }
                }

                // Category filter
                FilterChip(
                    selected = state.filter.selectedCategoryId != null,
                    onClick = { showCategoryMenu = true },
                    label = {
                        Text(
                            state.filter.selectedCategoryId?.let { state.categoryMap[it]?.name } ?: "All Categories",
                        )
                    },
                )
                DropdownMenu(expanded = showCategoryMenu, onDismissRequest = { showCategoryMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("All Categories") },
                        onClick = {
                            viewModel.setCategoryFilter(null)
                            showCategoryMenu = false
                        },
                    )
                    state.categories.filter { it.parentCategoryId == null }.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name) },
                            onClick = {
                                viewModel.setCategoryFilter(cat.id)
                                showCategoryMenu = false
                            },
                        )
                    }
                }

                if (state.filter != ActivityFilter()) {
                    TextButton(onClick = viewModel::clearFilters) {
                        Text("Reset")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Totals strip
            if (state.transactions.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "${state.transactions.size} items",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            MoneyText(
                                paise = state.filteredInflowPaise,
                                preferences = state.preferences,
                                style = MaterialTheme.typography.labelLarge,
                                color = financeColors.income,
                                showPlusSign = true,
                            )
                            MoneyText(
                                paise = -state.filteredOutflowPaise,
                                preferences = state.preferences,
                                style = MaterialTheme.typography.labelLarge,
                                color = financeColors.expense,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (state.transactions.isEmpty()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    EmptyStateCard(
                        title = "No matching transactions",
                        subtitle = if (state.filter == ActivityFilter()) {
                            "Your ledger is ready. Tap Add Transaction to record your first entry."
                        } else {
                            "Try clearing or adjusting your search and filter selections."
                        },
                        icon = if (state.filter == ActivityFilter()) Icons.Rounded.Add else Icons.Rounded.ReceiptLong,
                        actionLabel = if (state.filter == ActivityFilter()) "Add Transaction" else "Clear Filters",
                        onActionClick = {
                            if (state.filter == ActivityFilter()) onAddTransaction() else viewModel.clearFilters()
                        },
                    )
                }
            } else {
                val grouped = remember(state.transactions, state.preferences.dateFormat, state.filter.sortOrder) {
                    if (state.filter.sortOrder == TransactionSortOrder.DATE_DESC ||
                        state.filter.sortOrder == TransactionSortOrder.DATE_ASC
                    ) {
                        state.transactions.groupBy { formatDateMillis(it.timestamp, state.preferences.dateFormat) }
                    } else {
                        mapOf("Sorted by Amount" to state.transactions)
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 104.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    grouped.forEach { (header, itemsInGroup) ->
                        item(key = "header_$header") {
                            Text(
                                text = header,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                            )
                        }
                        item(key = "card_$header") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            ) {
                                Column {
                                    itemsInGroup.forEachIndexed { idx, tx ->
                                        TransactionRow(
                                            transaction = tx,
                                            account = state.accountMap[tx.accountId],
                                            destinationAccount = tx.destinationAccountId?.let { state.accountMap[it] },
                                            category = tx.categoryId?.let { state.categoryMap[it] },
                                            preferences = state.preferences,
                                            onClick = { onOpenTransactionDetail(tx.id) },
                                            modifier = Modifier.testTag("tx_row_${tx.id}"),
                                        )
                                        if (idx < itemsInGroup.lastIndex) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 16.dp),
                                                color = MaterialTheme.colorScheme.outlineVariant,
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

    if (showCustomRangePicker) {
        val pickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showCustomRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val s = pickerState.selectedStartDateMillis
                        val e = pickerState.selectedEndDateMillis ?: s
                        if (s != null && e != null) {
                            val zone = ZoneId.systemDefault()
                            viewModel.setCustomDates(
                                Instant.ofEpochMilli(s).atZone(zone).toLocalDate(),
                                Instant.ofEpochMilli(e).atZone(zone).toLocalDate(),
                            )
                        }
                        showCustomRangePicker = false
                    },
                ) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomRangePicker = false }) { Text("Cancel") }
            },
        ) {
            DateRangePicker(state = pickerState, modifier = Modifier.height(420.dp))
        }
    }
}
