package com.example.ui.screens.planning

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AccountEntity
import com.example.data.repository.DebtWithBalance
import com.example.domain.finance.MoneyFormatter
import com.example.domain.model.DebtDirection
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MoneyText
import com.example.ui.components.SectionHeader
import com.example.ui.components.formatDateMillis
import com.example.ui.components.formatEpochDay
import com.example.ui.theme.LocalFinanceColors
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(
    viewModel: DebtsViewModel,
    onNavigateBack: () -> Unit,
    onOpenDebtDetail: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val financeColors = LocalFinanceColors.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debts & Liabilities") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.testTag("add_debt_fab"),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Debt")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("To Receive (Lent)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            MoneyText(state.totalReceivablePaise, state.preferences, style = MaterialTheme.typography.titleLarge, color = financeColors.income)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("To Pay (Borrowed)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            MoneyText(state.totalPayablePaise, state.preferences, style = MaterialTheme.typography.titleLarge, color = financeColors.expense)
                        }
                    }
                }
            }

            if (state.debts.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No debt records",
                        subtitle = "Track money lent to friends or borrowed from others separately from your income & expenses.",
                        icon = Icons.Rounded.Handshake,
                        actionLabel = "Add Debt Record",
                        onActionClick = { showCreateDialog = true },
                    )
                }
            } else {
                items(state.debts, key = { it.debt.id }) { item ->
                    val d = item.debt
                    val isLent = d.direction == DebtDirection.LENT.name
                    val accent = if (isLent) financeColors.income else financeColors.expense

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenDebtDetail(d.id) },
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(d.personName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = accent.copy(alpha = 0.14f),
                                    ) {
                                        Text(
                                            text = if (d.isSettled) "Settled" else if (isLent) "Lent" else "Borrowed",
                                            color = accent,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Principal ${MoneyFormatter.formatWithMode(d.principalAmountPaise, state.preferences.currencyDisplayMode, false)}" +
                                        (d.dueDateEpochDay?.let { " • Due ${formatEpochDay(it, state.preferences.dateFormat)}" } ?: ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Outstanding", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                MoneyText(item.outstandingAmountPaise, state.preferences, style = MaterialTheme.typography.titleMedium, color = accent)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateDebtDialog(
            accounts = state.accounts,
            onDismiss = { showCreateDialog = false },
            onCreate = { person, dir, amount, accId, logTx, dueDay, notes ->
                viewModel.createDebt(person, dir, amount, accId, logTx, dueDay, notes)
                showCreateDialog = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtDetailScreen(
    viewModel: DebtsViewModel,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.detailUiState.collectAsStateWithLifecycle()
    val financeColors = LocalFinanceColors.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showRepaymentDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val item = state.debtWithBalance

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item?.debt?.personName ?: "Debt Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (item != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete debt", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (item == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Debt record not found")
            }
            return@Scaffold
        }

        val debt = item.debt
        val isLent = debt.direction == DebtDirection.LENT.name
        val accent = if (isLent) financeColors.income else financeColors.expense

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
                        Text(
                            text = if (isLent) "Receivable from ${debt.personName}" else "Payable to ${debt.personName}",
                            style = MaterialTheme.typography.labelLarge,
                            color = accent,
                        )
                        MoneyText(item.outstandingAmountPaise, state.preferences, style = MaterialTheme.typography.displayMedium, color = accent, forceFull = true)
                        Text(
                            text = "Principal: ${MoneyFormatter.format(debt.principalAmountPaise, state.preferences.showPaiseDecimals)} • Repaid: ${MoneyFormatter.format(item.repaidAmountPaise, state.preferences.showPaiseDecimals)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (debt.notes.isNotBlank()) {
                            Text(debt.notes, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (!debt.isSettled && item.outstandingAmountPaise > 0L) {
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = { showRepaymentDialog = true },
                                modifier = Modifier.fillMaxWidth().testTag("debt_add_repayment_button"),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text("Record Repayment")
                            }
                        }
                    }
                }
            }

            item { SectionHeader(title = "Repayment History") }

            if (state.repayments.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No repayments yet",
                        subtitle = "Log partial or full repayments as they happen.",
                    )
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column {
                            state.repayments.forEachIndexed { idx, rep ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column {
                                        Text(rep.note.ifBlank { "Repayment" }, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = formatDateMillis(rep.timestamp, state.preferences.dateFormat),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    MoneyText(rep.amountPaise, state.preferences, style = MaterialTheme.typography.titleSmall, color = accent)
                                }
                                if (idx < state.repayments.lastIndex) {
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRepaymentDialog && item != null) {
        DebtRepaymentDialog(
            item = item,
            accounts = state.accounts,
            onDismiss = { showRepaymentDialog = false },
            onConfirm = { amount, accId, logTx, note ->
                viewModel.recordRepayment(item, amount, accId, logTx, note)
                showRepaymentDialog = false
            },
        )
    }

    if (showDeleteDialog && item != null) {
        ConfirmDeleteDialog(
            title = "Delete debt with ${item.debt.personName}?",
            message = "This removes the debt and its repayment history.",
            onConfirm = {
                viewModel.deleteDebt(item.debt.id)
                showDeleteDialog = false
                onNavigateBack()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CreateDebtDialog(
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onCreate: (String, DebtDirection, String, Long?, Boolean, Long?, String) -> Unit,
) {
    var person by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf(DebtDirection.LENT) }
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var logTx by remember { mutableStateOf(accounts.isNotEmpty()) }
    var accountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Debt Record") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = person,
                    onValueChange = { person = it },
                    label = { Text("Person / Counterparty") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("debt_person_input"),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DebtDirection.entries.forEach { d ->
                        FilterChip(
                            selected = direction == d,
                            onClick = { direction = d },
                            label = { Text(if (d == DebtDirection.LENT) "Lent" else "Borrowed") },
                        )
                    }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("debt_amount_input"),
                )
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        dueDate?.let { "Due: ${formatEpochDay(it, com.example.domain.model.DateFormatPreference.DD_MMM_YYYY)}" }
                            ?: "Set Due Date (Optional)",
                    )
                }
                if (accounts.isNotEmpty()) {
                    FilterChip(
                        selected = logTx,
                        onClick = { logTx = !logTx },
                        label = { Text("Update account balance via ledger entry") },
                    )
                    if (logTx) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            accounts.forEach { acc ->
                                FilterChip(
                                    selected = accountId == acc.id,
                                    onClick = { accountId = acc.id },
                                    label = { Text(acc.name, style = MaterialTheme.typography.labelSmall) },
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(person, direction, amount, accountId, logTx, dueDate, notes) },
                modifier = Modifier.testTag("debt_save_button"),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )

    if (showDatePicker) {
        val st = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    st.selectedDateMillis?.let {
                        dueDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = st) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DebtRepaymentDialog(
    item: DebtWithBalance,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, Long?, Boolean, String) -> Unit,
) {
    var amount by remember { mutableStateOf(MoneyFormatter.paiseToPlainInput(item.outstandingAmountPaise)) }
    var note by remember { mutableStateOf("") }
    var logTx by remember { mutableStateOf(accounts.isNotEmpty()) }
    var accountId by remember { mutableStateOf(item.debt.accountId ?: accounts.firstOrNull()?.id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Repayment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Repayment Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("repayment_amount_input"),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (accounts.isNotEmpty()) {
                    FilterChip(
                        selected = logTx,
                        onClick = { logTx = !logTx },
                        label = { Text("Log account balance update") },
                    )
                    if (logTx) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            accounts.forEach { acc ->
                                FilterChip(
                                    selected = accountId == acc.id,
                                    onClick = { accountId = acc.id },
                                    label = { Text(acc.name, style = MaterialTheme.typography.labelSmall) },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(amount, accountId, logTx, note) },
                modifier = Modifier.testTag("repayment_confirm_button"),
            ) { Text("Record") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
