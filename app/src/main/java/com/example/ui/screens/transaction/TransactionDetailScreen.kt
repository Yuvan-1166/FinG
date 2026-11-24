package com.example.ui.screens.transaction

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
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.domain.model.TransactionType
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.MoneyText
import com.example.ui.components.formatDateTimeMillis
import com.example.ui.theme.LocalFinanceColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    viewModel: TransactionViewModel,
    onNavigateBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDuplicate: (Long) -> Unit,
) {
    val state by viewModel.detailUiState.collectAsStateWithLifecycle()
    val financeColors = LocalFinanceColors.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) onNavigateBack()
    }

    val tx = state.transaction

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (tx != null) {
                        IconButton(
                            onClick = { onDuplicate(tx.id) },
                            modifier = Modifier.testTag("tx_detail_duplicate_button"),
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = "Duplicate transaction")
                        }
                        IconButton(
                            onClick = { onEdit(tx.id) },
                            modifier = Modifier.testTag("tx_detail_edit_button"),
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit transaction")
                        }
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.testTag("tx_detail_delete_button"),
                        ) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "Delete transaction",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (tx == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text("Transaction not found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        val txType = runCatching { TransactionType.valueOf(tx.type) }.getOrDefault(TransactionType.EXPENSE)
        val accent = when (txType) {
            TransactionType.INCOME, TransactionType.REFUND, TransactionType.ASSET_SALE -> financeColors.income
            TransactionType.EXPENSE -> financeColors.expense
            TransactionType.TRANSFER -> financeColors.transfer
            TransactionType.ASSET_PURCHASE -> financeColors.warning
            else -> financeColors.debt
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = accent.copy(alpha = 0.14f),
                        ) {
                            Text(
                                text = txType.label,
                                color = accent,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        MoneyText(
                            paise = tx.amountPaise,
                            preferences = state.preferences,
                            style = MaterialTheme.typography.displayMedium,
                            color = accent,
                            forceFull = true,
                        )
                        if (tx.merchant.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(tx.merchant, style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = formatDateTimeMillis(tx.timestamp, state.preferences.dateFormat),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        DetailRow("Account", state.account?.name ?: "—")
                        if (state.destinationAccount != null) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            DetailRow("Destination Account", state.destinationAccount!!.name)
                        }
                        if (state.category != null) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            val catText = buildString {
                                append(state.category!!.name)
                                state.subcategory?.let { append(" • ${it.name}") }
                            }
                            DetailRow("Category", catText)
                        }
                        if (tx.description.isNotBlank()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            DetailRow("Description", tx.description)
                        }
                        if (tx.tags.isNotBlank()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            DetailRow("Tags", tx.tags)
                        }
                        if (tx.notes.isNotBlank()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            DetailRow("Notes", tx.notes)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        DetailRow("Created", formatDateTimeMillis(tx.createdAt, state.preferences.dateFormat))
                        DetailRow("Updated", formatDateTimeMillis(tx.updatedAt, state.preferences.dateFormat))
                    }
                }
            }

            if (tx.receiptUri != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Receipt Attachment", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(10.dp))
                            AsyncImage(
                                model = tx.receiptUri,
                                contentDescription = "Attached receipt",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = { onDuplicate(tx.id) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Duplicate")
                    }
                    OutlinedButton(
                        onClick = { onEdit(tx.id) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Edit")
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            title = "Delete Transaction?",
            message = "This will permanently remove this transaction and recalculate account balances and budgets.",
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteCurrentTransaction()
                onNavigateBack()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
