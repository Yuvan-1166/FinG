package com.example.ui.screens.planning

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AccountEntity
import com.example.domain.finance.MoneyFormatter
import com.example.domain.model.AccountType
import com.example.ui.components.CategoryIconMapper
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MoneyText
import com.example.ui.components.SectionHeader
import com.example.ui.components.SpendingTrendChart
import com.example.ui.components.TransactionRow
import com.example.ui.components.parseHexColor
import com.example.ui.theme.CategoryPaletteHex
import com.example.ui.theme.LocalFinanceColors
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel,
    onNavigateBack: () -> Unit,
    onOpenAccountDetail: (Long) -> Unit,
    onTransferMoney: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val financeColors = LocalFinanceColors.current
    val snackbarHostState = remember { SnackbarHostState() }

    var editingAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var deletingAccount by remember { mutableStateOf<AccountEntity?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accounts & Wallets") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.accounts.count { !it.account.isArchived } >= 2) {
                        TextButton(
                            onClick = onTransferMoney,
                            modifier = Modifier.testTag("accounts_transfer_button"),
                        ) {
                            Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Transfer")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingAccount = null
                    showAccountDialog = true
                },
                modifier = Modifier.testTag("add_account_fab"),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Account")
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
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("Liquid Cash & Bank", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            MoneyText(state.totalLiquidPaise, state.preferences, style = MaterialTheme.typography.titleLarge, color = financeColors.income)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Card Liabilities", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            MoneyText(
                                state.totalCreditDuePaise,
                                state.preferences,
                                style = MaterialTheme.typography.titleLarge,
                                color = if (state.totalCreditDuePaise > 0) financeColors.expense else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            if (state.accounts.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No accounts yet",
                        subtitle = "Add your Savings, Current, Cash, UPI, or Credit Card accounts to track balances accurately.",
                        icon = Icons.Rounded.AccountBalance,
                        actionLabel = "Add Account",
                        onActionClick = {
                            editingAccount = null
                            showAccountDialog = true
                        },
                    )
                }
            } else {
                val active = state.accounts.filter { !it.account.isArchived }
                val archived = state.accounts.filter { it.account.isArchived }

                items(active, key = { it.account.id }) { item ->
                    val acc = item.account
                    val accType = runCatching { AccountType.valueOf(acc.type) }.getOrDefault(AccountType.CUSTOM)
                    val color = parseHexColor(acc.colorHex)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenAccountDetail(acc.id) },
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(color.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = CategoryIconMapper.iconForAccountType(accType),
                                    contentDescription = null,
                                    tint = color,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(acc.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = buildString {
                                        append(accType.label)
                                        if (acc.institutionName.isNotBlank()) append(" • ${acc.institutionName}")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                MoneyText(
                                    paise = item.currentBalancePaise,
                                    preferences = state.preferences,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (item.currentBalancePaise < 0) financeColors.expense else MaterialTheme.colorScheme.onSurface,
                                )
                                if (accType == AccountType.CREDIT_CARD && item.currentBalancePaise < 0L) {
                                    Text("Outstanding Due", style = MaterialTheme.typography.labelSmall, color = financeColors.expense)
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            IconButton(onClick = {
                                editingAccount = acc
                                showAccountDialog = true
                            }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Edit account", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { viewModel.toggleArchive(acc) }) {
                                Icon(Icons.Rounded.Archive, contentDescription = "Archive account", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { deletingAccount = acc }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Delete account", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                if (archived.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Archived Accounts")
                    }
                    items(archived, key = { "arch_${it.account.id}" }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.account.name, style = MaterialTheme.typography.titleSmall)
                                    MoneyText(item.currentBalancePaise, state.preferences, style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { viewModel.toggleArchive(item.account) }) {
                                    Icon(Icons.Rounded.Unarchive, contentDescription = "Restore account")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAccountDialog) {
        AccountEditorDialog(
            initial = editingAccount,
            onDismiss = { showAccountDialog = false },
            onSave = { name, type, opening, inst, color ->
                viewModel.saveAccount(editingAccount?.id, name, type, opening, inst, color)
                showAccountDialog = false
            },
        )
    }

    deletingAccount?.let { acc ->
        ConfirmDeleteDialog(
            title = "Delete '${acc.name}'?",
            message = "Accounts with existing transactions cannot be deleted to preserve ledger accuracy; archive them instead.",
            onConfirm = {
                viewModel.deleteAccount(acc.id)
                deletingAccount = null
            },
            onDismiss = { deletingAccount = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    viewModel: AccountsViewModel,
    onNavigateBack: () -> Unit,
    onOpenTransactionDetail: (Long) -> Unit,
    onAddTransactionForAccount: (Long) -> Unit,
) {
    val state by viewModel.detailUiState.collectAsStateWithLifecycle()
    val financeColors = LocalFinanceColors.current
    val item = state.accountWithBalance

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item?.account?.name ?: "Account Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (item != null) {
                        IconButton(onClick = { onAddTransactionForAccount(item.account.id) }) {
                            Icon(Icons.Rounded.Add, contentDescription = "Add transaction")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (item == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Account not found")
            }
            return@Scaffold
        }

        val acc = item.account
        val accType = runCatching { AccountType.valueOf(acc.type) }.getOrDefault(AccountType.CUSTOM)
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
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(accType.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        MoneyText(
                            paise = item.currentBalancePaise,
                            preferences = state.preferences,
                            style = MaterialTheme.typography.displayMedium,
                            color = if (item.currentBalancePaise < 0) financeColors.expense else financeColors.income,
                            forceFull = true,
                        )
                        Text(
                            text = "Opening: ${MoneyFormatter.format(acc.openingBalancePaise, state.preferences.showPaiseDecimals)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (state.balanceHistoryPoints.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SectionHeader(title = "Balance History")
                            Spacer(Modifier.height(10.dp))
                            SpendingTrendChart(
                                points = state.balanceHistoryPoints,
                                preferences = state.preferences,
                                lineColor = parseHexColor(acc.colorHex),
                            )
                        }
                    }
                }
            }

            item {
                SectionHeader(title = "Transactions (${state.transactions.size})")
            }

            if (state.transactions.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No transactions in this account",
                        subtitle = "Log income, expenses, or transfers using this account.",
                        actionLabel = "Add Transaction",
                        onActionClick = { onAddTransactionForAccount(acc.id) },
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
                            state.transactions.forEachIndexed { idx, tx ->
                                TransactionRow(
                                    transaction = tx,
                                    account = state.accountMap[tx.accountId],
                                    destinationAccount = tx.destinationAccountId?.let { state.accountMap[it] },
                                    category = tx.categoryId?.let { state.categoryMap[it] },
                                    preferences = state.preferences,
                                    onClick = { onOpenTransactionDetail(tx.id) },
                                )
                                if (idx < state.transactions.lastIndex) {
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountEditorDialog(
    initial: AccountEntity?,
    onDismiss: () -> Unit,
    onSave: (String, AccountType, String, String, String) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var type by remember {
        mutableStateOf(
            initial?.type?.let { runCatching { AccountType.valueOf(it) }.getOrNull() } ?: AccountType.SAVINGS,
        )
    }
    var opening by remember {
        mutableStateOf(
            initial?.let { MoneyFormatter.paiseToPlainInput(abs(it.openingBalancePaise)) } ?: "0",
        )
    }
    var institution by remember { mutableStateOf(initial?.institutionName ?: "") }
    var colorHex by remember { mutableStateOf(initial?.colorHex ?: CategoryPaletteHex.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Account" else "Edit Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("account_name_input"),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                    label = { Text(if (type == AccountType.CREDIT_CARD) "Outstanding Due (₹)" else "Opening Balance (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("account_opening_input"),
                )
                OutlinedTextField(
                    value = institution,
                    onValueChange = { institution = it },
                    label = { Text("Bank / Provider (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryPaletteHex.take(6).forEach { hex ->
                        val c = parseHexColor(hex)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(c)
                                .then(
                                    if (colorHex == hex) Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier,
                                )
                                .clickable { colorHex = hex },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, type, opening, institution, colorHex) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("account_save_button"),
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
