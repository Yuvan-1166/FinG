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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Savings
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AccountEntity
import com.example.data.local.SavingsGoalEntity
import com.example.domain.finance.MoneyFormatter
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MoneyText
import com.example.ui.components.SectionHeader
import com.example.ui.components.formatDateMillis
import com.example.ui.components.formatEpochDay
import com.example.ui.components.parseHexColor
import com.example.ui.theme.CategoryPaletteHex
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.theme.MoneyTextStyle
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    viewModel: GoalsViewModel,
    onNavigateBack: () -> Unit,
    onOpenGoalDetail: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showEditor by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Savings Goals") },
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
                    editingGoal = null
                    showEditor = true
                },
                modifier = Modifier.testTag("add_goal_fab"),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Goal")
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
            if (state.goals.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No savings goals yet",
                        subtitle = "Create targets for your emergency fund, vacation, new laptop, or education.",
                        icon = Icons.Rounded.Savings,
                        actionLabel = "Create Goal",
                        onActionClick = {
                            editingGoal = null
                            showEditor = true
                        },
                    )
                }
            } else {
                items(state.goals, key = { it.goal.id }) { item ->
                    val goal = item.goal
                    val color = parseHexColor(goal.colorHex)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenGoalDetail(goal.id) },
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
                                    Text(goal.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    goal.targetDateEpochDay?.let { day ->
                                        Text(
                                            text = "Target: ${formatEpochDay(day, state.preferences.dateFormat)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${item.progressPercent}%",
                                        style = MaterialTheme.typography.titleMedium.merge(MoneyTextStyle),
                                        color = color,
                                    )
                                    IconButton(onClick = {
                                        editingGoal = goal
                                        showEditor = true
                                    }) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "Edit goal", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            LinearProgressIndicator(
                                progress = { (item.progressPercent / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(9.dp).clip(CircleShape),
                                color = color,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeCap = StrokeCap.Round,
                            )
                            Text(
                                text = "${MoneyFormatter.formatWithMode(item.savedAmountPaise, state.preferences.currencyDisplayMode, false)} saved of ${MoneyFormatter.formatWithMode(goal.targetAmountPaise, state.preferences.currencyDisplayMode, false)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        GoalEditorDialog(
            initial = editingGoal,
            accounts = state.accounts,
            onDismiss = { showEditor = false },
            onSave = { name, target, desc, targetDay, linkedAccId, colorHex ->
                viewModel.saveGoal(editingGoal?.id, name, target, desc, targetDay, linkedAccId, colorHex)
                showEditor = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    viewModel: GoalsViewModel,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.detailUiState.collectAsStateWithLifecycle()
    val financeColors = LocalFinanceColors.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showContribDialog by remember { mutableStateOf(false) }
    var isWithdrawalMode by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val item = state.goalWithProgress

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item?.goal?.name ?: "Goal Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (item != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete goal", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (item == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Goal not found")
            }
            return@Scaffold
        }

        val goal = item.goal
        val color = parseHexColor(goal.colorHex)
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
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("${item.progressPercent}% Complete", style = MaterialTheme.typography.labelLarge, color = color)
                        MoneyText(item.savedAmountPaise, state.preferences, style = MaterialTheme.typography.displayMedium, forceFull = true)
                        LinearProgressIndicator(
                            progress = { (item.progressPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                            color = color,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        val remaining = (goal.targetAmountPaise - item.savedAmountPaise).coerceAtLeast(0L)
                        Text(
                            text = "Target: ${MoneyFormatter.format(goal.targetAmountPaise, state.preferences.showPaiseDecimals)} • Remaining: ${MoneyFormatter.format(remaining, state.preferences.showPaiseDecimals)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (goal.description.isNotBlank()) {
                            Text(goal.description, style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    isWithdrawalMode = false
                                    showContribDialog = true
                                },
                                modifier = Modifier.weight(1f).testTag("goal_add_contrib_button"),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Contribute")
                            }
                            OutlinedButton(
                                onClick = {
                                    isWithdrawalMode = true
                                    showContribDialog = true
                                },
                                modifier = Modifier.weight(1f).testTag("goal_withdraw_button"),
                                enabled = item.savedAmountPaise > 0L,
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(Icons.Rounded.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Withdraw")
                            }
                        }
                    }
                }
            }

            item { SectionHeader(title = "Contribution History") }

            if (state.contributions.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No contributions yet",
                        subtitle = "Add your first contribution to start building toward this goal.",
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
                            state.contributions.forEachIndexed { idx, c ->
                                val isDeposit = c.amountPaise > 0L
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = c.note.ifBlank { if (isDeposit) "Goal Contribution" else "Goal Withdrawal" },
                                            style = MaterialTheme.typography.titleSmall,
                                        )
                                        Text(
                                            text = formatDateMillis(c.timestamp, state.preferences.dateFormat),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    MoneyText(
                                        paise = c.amountPaise,
                                        preferences = state.preferences,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (isDeposit) financeColors.income else financeColors.expense,
                                        showPlusSign = true,
                                    )
                                }
                                if (idx < state.contributions.lastIndex) {
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showContribDialog && item != null) {
        GoalContributionDialog(
            isWithdrawal = isWithdrawalMode,
            accounts = state.accounts,
            defaultDestAccountId = item.goal.linkedAccountId,
            onDismiss = { showContribDialog = false },
            onConfirm = { amount, srcId, destId, note ->
                viewModel.contributeOrWithdraw(item.goal, amount, isWithdrawalMode, srcId, destId, note)
                showContribDialog = false
            },
        )
    }

    if (showDeleteDialog && item != null) {
        ConfirmDeleteDialog(
            title = "Delete '${item.goal.name}'?",
            message = "This will delete the goal and its contribution log.",
            onConfirm = {
                viewModel.deleteGoal(item.goal.id)
                showDeleteDialog = false
                onNavigateBack()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun GoalEditorDialog(
    initial: SavingsGoalEntity?,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Long?, Long?, String) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var target by remember { mutableStateOf(initial?.let { MoneyFormatter.paiseToPlainInput(it.targetAmountPaise) } ?: "") }
    var desc by remember { mutableStateOf(initial?.description ?: "") }
    var targetDay by remember { mutableStateOf(initial?.targetDateEpochDay) }
    var linkedAccountId by remember { mutableStateOf(initial?.linkedAccountId) }
    var colorHex by remember { mutableStateOf(initial?.colorHex ?: CategoryPaletteHex.first()) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Savings Goal" else "Edit Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Goal Name (e.g. Emergency Fund)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("goal_name_input"),
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("Target Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("goal_target_input"),
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        targetDay?.let { "Target: ${formatEpochDay(it, com.example.domain.model.DateFormatPreference.DD_MMM_YYYY)}" }
                            ?: "Set Target Date (Optional)",
                    )
                }
                if (accounts.isNotEmpty()) {
                    Text("Dedicated Goal Account (Optional)", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = linkedAccountId == null,
                            onClick = { linkedAccountId = null },
                            label = { Text("None", style = MaterialTheme.typography.labelSmall) },
                        )
                        accounts.take(4).forEach { acc ->
                            FilterChip(
                                selected = linkedAccountId == acc.id,
                                onClick = { linkedAccountId = acc.id },
                                label = { Text(acc.name, style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryPaletteHex.take(6).forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(hex))
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
                onClick = { onSave(name, target, desc, targetDay, linkedAccountId, colorHex) },
                modifier = Modifier.testTag("goal_save_button"),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )

    if (showDatePicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        targetDay = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoalContributionDialog(
    isWithdrawal: Boolean,
    accounts: List<AccountEntity>,
    defaultDestAccountId: Long?,
    onDismiss: () -> Unit,
    onConfirm: (String, Long?, Long?, String) -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var transferBetweenAccounts by remember { mutableStateOf(false) }
    var sourceAccId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    var destAccId by remember { mutableStateOf(defaultDestAccountId ?: accounts.getOrNull(1)?.id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isWithdrawal) "Withdraw from Goal" else "Add Goal Contribution") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("goal_contrib_amount_input"),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (accounts.size >= 2) {
                    FilterChip(
                        selected = transferBetweenAccounts,
                        onClick = { transferBetweenAccounts = !transferBetweenAccounts },
                        label = { Text("Also log transfer between accounts") },
                    )
                    if (transferBetweenAccounts) {
                        Text("From Account", style = MaterialTheme.typography.labelSmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            accounts.forEach { acc ->
                                FilterChip(
                                    selected = sourceAccId == acc.id,
                                    onClick = { sourceAccId = acc.id },
                                    label = { Text(acc.name, style = MaterialTheme.typography.labelSmall) },
                                )
                            }
                        }
                        Text("To Goal Account", style = MaterialTheme.typography.labelSmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            accounts.filter { it.id != sourceAccId }.forEach { acc ->
                                FilterChip(
                                    selected = destAccId == acc.id,
                                    onClick = { destAccId = acc.id },
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
                onClick = {
                    onConfirm(
                        amount,
                        if (transferBetweenAccounts) sourceAccId else null,
                        if (transferBetweenAccounts) destAccId else null,
                        note,
                    )
                },
                modifier = Modifier.testTag("goal_contrib_confirm_button"),
            ) { Text(if (isWithdrawal) "Withdraw" else "Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
