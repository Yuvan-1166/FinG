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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AccountEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.RecurringPaymentEntity
import com.example.domain.finance.MoneyFormatter
import com.example.domain.model.RecurrenceFrequency
import com.example.domain.model.TransactionType
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MoneyText
import com.example.ui.components.formatEpochDay
import com.example.ui.theme.LocalFinanceColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    viewModel: RecurringViewModel,
    onNavigateBack: () -> Unit,
    onOpenAccounts: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val financeColors = LocalFinanceColors.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showEditor by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<RecurringPaymentEntity?>(null) }
    var deletingItem by remember { mutableStateOf<RecurringPaymentEntity?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recurring Bills & Income") },
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
                    editingItem = null
                    showEditor = true
                },
                modifier = Modifier.testTag("add_recurring_fab"),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Recurring Payment")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.accounts.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "Account needed",
                        subtitle = "Create at least one account before scheduling recurring bills or income.",
                        actionLabel = "Go to Accounts",
                        onActionClick = onOpenAccounts,
                    )
                }
            } else if (state.items.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No recurring payments",
                        subtitle = "Schedule rent, electricity, internet, insurance, subscriptions, or EMI with reminders.",
                        icon = Icons.Rounded.Repeat,
                        actionLabel = "Schedule Recurring",
                        onActionClick = {
                            editingItem = null
                            showEditor = true
                        },
                    )
                }
            } else {
                val todayEpoch = LocalDate.now().toEpochDay()
                items(state.items, key = { it.id }) { item ->
                    val freq = runCatching { RecurrenceFrequency.valueOf(item.frequency).label }.getOrDefault("Monthly")
                    val accName = state.accountMap[item.accountId]?.name ?: "Account"
                    val isIncome = item.type == TransactionType.INCOME.name
                    val daysDiff = item.nextDueDateEpochDay - todayEpoch
                    val dueStatus = when {
                        !item.isActive -> "Paused"
                        daysDiff < 0 -> "Overdue by ${-daysDiff}d"
                        daysDiff == 0L -> "Due today"
                        else -> "Next: ${formatEpochDay(item.nextDueDateEpochDay, state.preferences.dateFormat)}"
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = if (item.isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "$freq • $accName",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = dueStatus,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (item.isActive && daysDiff <= 0) financeColors.expense else MaterialTheme.colorScheme.primary,
                                    )
                                }
                                MoneyText(
                                    paise = item.amountPaise,
                                    preferences = state.preferences,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isIncome) financeColors.income else financeColors.expense,
                                    showPlusSign = isIncome,
                                )
                            }

                            if (item.isActive) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Button(
                                        onClick = { viewModel.confirmOccurrence(item) },
                                        modifier = Modifier.weight(1f).testTag("recurring_confirm_${item.id}"),
                                        shape = RoundedCornerShape(10.dp),
                                    ) {
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Confirm Payment")
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.skipOccurrence(item) },
                                        shape = RoundedCornerShape(10.dp),
                                    ) {
                                        Icon(Icons.Rounded.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Skip")
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                IconButton(onClick = { viewModel.toggleActive(item) }) {
                                    Icon(
                                        if (item.isActive) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = if (item.isActive) "Pause" else "Resume",
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                IconButton(onClick = {
                                    editingItem = item
                                    showEditor = true
                                }) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { deletingItem = item }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditor && state.accounts.isNotEmpty()) {
        RecurringEditorDialog(
            initial = editingItem,
            accounts = state.accounts,
            categories = state.categories,
            onDismiss = { showEditor = false },
            onSave = { title, type, amt, freq, startDay, endDay, accId, catId, merchant, notes, remOn, remDays ->
                viewModel.saveRecurring(
                    editingId = editingItem?.id,
                    title = title,
                    type = type,
                    amountInput = amt,
                    frequency = freq,
                    startEpochDay = startDay,
                    endEpochDay = endDay,
                    accountId = accId,
                    categoryId = catId,
                    merchant = merchant,
                    notes = notes,
                    reminderEnabled = remOn,
                    reminderDaysBefore = remDays,
                )
                showEditor = false
            },
        )
    }

    deletingItem?.let { item ->
        ConfirmDeleteDialog(
            title = "Delete '${item.title}'?",
            message = "Previously confirmed transactions will stay in your ledger.",
            onConfirm = {
                viewModel.deleteRecurring(item.id)
                deletingItem = null
            },
            onDismiss = { deletingItem = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun RecurringEditorDialog(
    initial: RecurringPaymentEntity?,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (String, TransactionType, String, RecurrenceFrequency, Long, Long?, Long?, Long?, String, String, Boolean, Int) -> Unit,
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var type by remember {
        mutableStateOf(
            if (initial?.type == TransactionType.INCOME.name) TransactionType.INCOME else TransactionType.EXPENSE,
        )
    }
    var amount by remember { mutableStateOf(initial?.let { MoneyFormatter.paiseToPlainInput(it.amountPaise) } ?: "") }
    var frequency by remember {
        mutableStateOf(
            initial?.frequency?.let { runCatching { RecurrenceFrequency.valueOf(it) }.getOrNull() }
                ?: RecurrenceFrequency.MONTHLY,
        )
    }
    var startEpochDay by remember { mutableStateOf(initial?.startDateEpochDay ?: LocalDate.now().toEpochDay()) }
    var endEpochDay by remember { mutableStateOf(initial?.endDateEpochDay) }
    var accountId by remember { mutableStateOf(initial?.accountId ?: accounts.firstOrNull()?.id) }
    var categoryId by remember { mutableStateOf(initial?.categoryId) }
    var merchant by remember { mutableStateOf(initial?.merchant ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var reminderEnabled by remember { mutableStateOf(initial?.reminderEnabled ?: true) }
    var reminderDays by remember { mutableIntStateOf(initial?.reminderDaysBefore ?: 2) }

    var pickStartDate by remember { mutableStateOf(false) }
    var pickEndDate by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Schedule Recurring" else "Edit Recurring") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (e.g. House Rent, Netflix)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("recurring_title_input"),
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("recurring_amount_input"),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == TransactionType.EXPENSE,
                        onClick = { type = TransactionType.EXPENSE; categoryId = null },
                        label = { Text("Expense") },
                    )
                    FilterChip(
                        selected = type == TransactionType.INCOME,
                        onClick = { type = TransactionType.INCOME; categoryId = null },
                        label = { Text("Income") },
                    )
                }
                Text("Frequency", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RecurrenceFrequency.entries.forEach { f ->
                        FilterChip(
                            selected = frequency == f,
                            onClick = { frequency = f },
                            label = { Text(f.label) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pickStartDate = true }, modifier = Modifier.weight(1f)) {
                        Text("Start: ${formatEpochDay(startEpochDay, com.example.domain.model.DateFormatPreference.DD_MMM_YYYY)}", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(onClick = { pickEndDate = true }, modifier = Modifier.weight(1f)) {
                        Text(
                            endEpochDay?.let { "End: ${formatEpochDay(it, com.example.domain.model.DateFormatPreference.DD_MMM_YYYY)}" }
                                ?: "End: None",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                Text("Account", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    accounts.forEach { acc ->
                        FilterChip(
                            selected = accountId == acc.id,
                            onClick = { accountId = acc.id },
                            label = { Text(acc.name, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
                Text("Category", style = MaterialTheme.typography.labelMedium)
                val catType = if (type == TransactionType.INCOME) "INCOME" else "EXPENSE"
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.filter { it.type == catType }.take(8).forEach { cat ->
                        FilterChip(
                            selected = categoryId == cat.id,
                            onClick = { categoryId = cat.id },
                            label = { Text(cat.name, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Local Reminder (${reminderDays}d before)", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        title, type, amount, frequency, startEpochDay, endEpochDay,
                        accountId, categoryId, merchant, notes, reminderEnabled, reminderDays,
                    )
                },
                modifier = Modifier.testTag("recurring_save_button"),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )

    if (pickStartDate) {
        val st = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { pickStartDate = false },
            confirmButton = {
                TextButton(onClick = {
                    st.selectedDateMillis?.let {
                        startEpochDay = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
                    }
                    pickStartDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickStartDate = false }) { Text("Cancel") } },
        ) { DatePicker(state = st) }
    }

    if (pickEndDate) {
        val st = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { pickEndDate = false },
            confirmButton = {
                TextButton(onClick = {
                    st.selectedDateMillis?.let {
                        endEpochDay = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
                    }
                    pickEndDate = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = {
                    endEpochDay = null
                    pickEndDate = false
                }) { Text("Clear") }
            },
        ) { DatePicker(state = st) }
    }
}
