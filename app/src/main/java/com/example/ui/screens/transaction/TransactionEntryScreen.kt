package com.example.ui.screens.transaction

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.domain.model.AccountType
import com.example.domain.model.TransactionType
import com.example.ui.components.CategoryIconMapper
import com.example.ui.components.formatDateMillis
import com.example.ui.components.parseHexColor
import com.example.ui.theme.MoneyTextStyle
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionEntryScreen(
    viewModel: TransactionViewModel,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.entryUiState.collectAsStateWithLifecycle()
    val form = state.form
    val context = LocalContext.current

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showQuickAccountDialog by remember { mutableStateOf(false) }

    LaunchedEffect(form.saveSuccess) {
        if (form.saveSuccess) onNavigateBack()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            viewModel.setReceiptUri(uri.toString())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (form.editingId != null) "Edit Transaction" else "New Transaction",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(end = 12.dp)) {
                        SegmentedButton(
                            selected = !form.isDetailedMode,
                            onClick = { viewModel.setDetailedMode(false) },
                            shape = SegmentedButtonDefaults.itemShape(0, 2),
                        ) {
                            Text("Quick", style = MaterialTheme.typography.labelMedium)
                        }
                        SegmentedButton(
                            selected = form.isDetailedMode,
                            onClick = { viewModel.setDetailedMode(true) },
                            shape = SegmentedButtonDefaults.itemShape(1, 2),
                        ) {
                            Text("Detailed", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    form.errorMessage?.let { err ->
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    Button(
                        onClick = viewModel::saveTransaction,
                        enabled = !form.isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_transaction_button"),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (form.editingId != null) "Update Transaction" else "Save Transaction",
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Amount Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Amount (INR)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = form.amountInput,
                            onValueChange = viewModel::setAmountInput,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tx_amount_input"),
                            prefix = {
                                Text(
                                    text = "₹ ",
                                    style = MaterialTheme.typography.headlineMedium.merge(MoneyTextStyle),
                                )
                            },
                            placeholder = { Text("0.00") },
                            textStyle = MaterialTheme.typography.headlineMedium.merge(MoneyTextStyle),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf(100L, 500L, 1000L, 5000L).forEach { delta ->
                                AssistChip(
                                    onClick = { viewModel.addQuickAmount(delta) },
                                    label = { Text("+₹$delta") },
                                )
                            }
                        }
                    }
                }
            }

            // Transaction Type Selector
            item {
                val visibleTypes = if (form.isDetailedMode) {
                    TransactionType.entries.filter {
                        (it != TransactionType.ASSET_PURCHASE && it != TransactionType.ASSET_SALE) || it == form.type
                    }
                } else {
                    listOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Transaction Type",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        visibleTypes.forEach { type ->
                            FilterChip(
                                selected = form.type == type,
                                onClick = { viewModel.setType(type) },
                                label = { Text(type.label) },
                                modifier = Modifier.testTag("tx_type_${type.name.lowercase()}"),
                            )
                        }
                    }
                    if (form.type == TransactionType.DEBT_REPAYMENT) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !form.isDebtIncomingRepayment,
                                onClick = { viewModel.setDebtIncomingRepayment(false) },
                                label = { Text("Paying Back Borrowed") },
                            )
                            FilterChip(
                                selected = form.isDebtIncomingRepayment,
                                onClick = { viewModel.setDebtIncomingRepayment(true) },
                                label = { Text("Receiving Lent Back") },
                            )
                        }
                    }
                }
            }

            // Account Selection (plus Destination for Transfer)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (form.type == TransactionType.TRANSFER) "From Account" else "Account",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        TextButton(onClick = { showQuickAccountDialog = true }) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("New Account")
                        }
                    }
                    if (state.accounts.isEmpty()) {
                        OutlinedButton(
                            onClick = { showQuickAccountDialog = true },
                            modifier = Modifier.fillMaxWidth().testTag("entry_create_account_button"),
                        ) {
                            Text("Create your first account (Cash / Bank / UPI / Card)")
                        }
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            state.accounts.forEach { acc ->
                                FilterChip(
                                    selected = form.accountId == acc.id,
                                    onClick = { viewModel.setAccountId(acc.id) },
                                    label = { Text(acc.name) },
                                )
                            }
                        }
                    }

                    if (form.type == TransactionType.TRANSFER && state.accounts.size > 1) {
                        Spacer(Modifier.height(6.dp))
                        Text("To Account", style = MaterialTheme.typography.labelLarge)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            state.accounts.filter { it.id != form.accountId }.forEach { acc ->
                                FilterChip(
                                    selected = form.destinationAccountId == acc.id,
                                    onClick = { viewModel.setDestinationAccountId(acc.id) },
                                    label = { Text(acc.name) },
                                )
                            }
                        }
                    }
                }
            }

            // Category & Subcategory Selection (for Income, Expense, Refund)
            if (form.type in setOf(TransactionType.INCOME, TransactionType.EXPENSE, TransactionType.REFUND)) {
                item {
                    val catType = if (form.type == TransactionType.INCOME) "INCOME" else "EXPENSE"
                    val parentCategories = state.categories.filter { it.parentCategoryId == null && it.type == catType }
                    val subcategories = state.categories.filter { it.parentCategoryId == form.categoryId }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Category", style = MaterialTheme.typography.labelLarge)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            parentCategories.forEach { cat ->
                                val catColor = parseHexColor(cat.colorHex)
                                FilterChip(
                                    selected = form.categoryId == cat.id,
                                    onClick = { viewModel.setCategoryId(cat.id) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = CategoryIconMapper.iconFor(cat.iconName),
                                            contentDescription = null,
                                            tint = catColor,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    },
                                    label = { Text(cat.name) },
                                )
                            }
                        }

                        if (form.isDetailedMode && subcategories.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text("Subcategory", style = MaterialTheme.typography.labelMedium)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                FilterChip(
                                    selected = form.subcategoryId == null,
                                    onClick = { viewModel.setSubcategoryId(null) },
                                    label = { Text("None") },
                                )
                                subcategories.forEach { sub ->
                                    FilterChip(
                                        selected = form.subcategoryId == sub.id,
                                        onClick = { viewModel.setSubcategoryId(sub.id) },
                                        label = { Text(sub.name) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Date & Time
            item {
                val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(form.timestamp), ZoneId.systemDefault())
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(formatDateMillis(form.timestamp, state.preferences.dateFormat))
                    }
                    if (form.isDetailedMode) {
                        OutlinedButton(
                            onClick = { showTimePicker = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(dt.format(DateTimeFormatter.ofPattern("hh:mm a")))
                        }
                    }
                }
            }

            // Quick Mode optional Merchant/Note OR Full Detailed Fields
            if (!form.isDetailedMode) {
                item {
                    OutlinedTextField(
                        value = form.merchant,
                        onValueChange = viewModel::setMerchant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tx_quick_merchant_input"),
                        label = { Text("Merchant / Payee (Optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                    )
                }
            } else {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = form.merchant,
                            onValueChange = viewModel::setMerchant,
                            modifier = Modifier.fillMaxWidth().testTag("tx_merchant_input"),
                            label = { Text("Merchant or Recipient") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                        )
                        OutlinedTextField(
                            value = form.description,
                            onValueChange = viewModel::setDescription,
                            modifier = Modifier.fillMaxWidth().testTag("tx_description_input"),
                            label = { Text("Description") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                        )
                        OutlinedTextField(
                            value = form.tags,
                            onValueChange = viewModel::setTags,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Tags (comma separated, e.g. office, reimbursable)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                        )
                        OutlinedTextField(
                            value = form.notes,
                            onValueChange = viewModel::setNotes,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Notes") },
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(14.dp),
                        )

                        // Optional Receipt Attachment via Android Photo Picker
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(Icons.Rounded.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (form.receiptUri == null) "Attach Receipt" else "Change Receipt")
                            }
                            if (form.receiptUri != null) {
                                IconButton(onClick = { viewModel.setReceiptUri(null) }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Remove receipt")
                                }
                            }
                        }

                        if (form.receiptUri != null) {
                            AsyncImage(
                                model = form.receiptUri,
                                contentDescription = "Receipt attachment",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = form.timestamp)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { picked ->
                            val zone = ZoneId.systemDefault()
                            val newDate = Instant.ofEpochMilli(picked).atZone(zone).toLocalDate()
                            val currentTime = Instant.ofEpochMilli(form.timestamp).atZone(zone).toLocalTime()
                            val updated = LocalDateTime.of(newDate, currentTime).atZone(zone).toInstant().toEpochMilli()
                            viewModel.setTimestamp(updated)
                        }
                        showDatePicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val zone = ZoneId.systemDefault()
        val currentDt = Instant.ofEpochMilli(form.timestamp).atZone(zone).toLocalDateTime()
        val timePickerState = rememberTimePickerState(
            initialHour = currentDt.hour,
            initialMinute = currentDt.minute,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updated = LocalDateTime.of(
                            currentDt.toLocalDate(),
                            LocalTime.of(timePickerState.hour, timePickerState.minute),
                        ).atZone(zone).toInstant().toEpochMilli()
                        viewModel.setTimestamp(updated)
                        showTimePicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
        )
    }

    if (showQuickAccountDialog) {
        QuickCreateAccountDialog(
            onDismiss = { showQuickAccountDialog = false },
            onCreate = { name, type, opening ->
                viewModel.createQuickAccount(name, type, opening)
                showQuickAccountDialog = false
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickCreateAccountDialog(
    onDismiss: () -> Unit,
    onCreate: (String, AccountType, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(AccountType.SAVINGS) }
    var opening by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Create Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name (e.g. HDFC Savings, Cash)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("quick_account_name_input"),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    AccountType.entries.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.label, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
                OutlinedTextField(
                    value = opening,
                    onValueChange = { opening = it },
                    label = {
                        Text(if (type == AccountType.CREDIT_CARD) "Current Outstanding Due (₹)" else "Opening Balance (₹)")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("quick_account_balance_input"),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onCreate(name, type, opening) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("quick_account_confirm_button"),
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
