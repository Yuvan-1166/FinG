package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AccountEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionEntity
import com.example.domain.finance.MoneyFormatter
import com.example.domain.model.AppPreferences
import com.example.domain.model.TransactionType
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.theme.MoneyTextStyle

@Composable
fun MoneyText(
    paise: Long,
    preferences: AppPreferences,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    showPlusSign: Boolean = false,
    forceFull: Boolean = false,
) {
    val text = if (forceFull) {
        MoneyFormatter.format(
            paise = paise,
            showDecimals = preferences.showPaiseDecimals,
            compact = false,
            showPlusSign = showPlusSign,
        )
    } else {
        MoneyFormatter.formatWithMode(
            paise = paise,
            mode = preferences.currencyDisplayMode,
            showDecimals = preferences.showPaiseDecimals,
            showPlusSign = showPlusSign,
        )
    }
    Text(
        text = text,
        modifier = modifier,
        style = style.merge(MoneyTextStyle),
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (actionLabel != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(actionLabel, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.AccountBalanceWallet,
    actionLabel: String? = null,
    actionTestTag: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (actionLabel != null && onActionClick != null) {
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = onActionClick,
                    modifier = if (actionTestTag != null) Modifier.testTag(actionTestTag) else Modifier,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun TransactionRow(
    transaction: TransactionEntity,
    account: AccountEntity?,
    destinationAccount: AccountEntity?,
    category: CategoryEntity?,
    preferences: AppPreferences,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val financeColors = LocalFinanceColors.current
    val txType = runCatching { TransactionType.valueOf(transaction.type) }
        .getOrDefault(TransactionType.EXPENSE)

    val (accentColor, bgColor, prefix) = when (txType) {
        TransactionType.INCOME -> Triple(financeColors.income, financeColors.incomeContainer, "+")
        TransactionType.REFUND -> Triple(financeColors.income, financeColors.incomeContainer, "+")
        TransactionType.EXPENSE -> Triple(financeColors.expense, financeColors.expenseContainer, "-")
        TransactionType.TRANSFER -> Triple(financeColors.transfer, financeColors.transferContainer, "")
        TransactionType.MONEY_LENT -> Triple(financeColors.debt, financeColors.debtContainer, "-")
        TransactionType.MONEY_BORROWED -> Triple(financeColors.debt, financeColors.debtContainer, "+")
        TransactionType.DEBT_REPAYMENT -> {
            if (transaction.isDebtIncomingRepayment) {
                Triple(financeColors.income, financeColors.incomeContainer, "+")
            } else {
                Triple(financeColors.debt, financeColors.debtContainer, "-")
            }
        }
        TransactionType.ASSET_PURCHASE -> Triple(financeColors.warning, financeColors.warningContainer, "-")
        TransactionType.ASSET_SALE -> Triple(financeColors.income, financeColors.incomeContainer, "+")
    }

    val iconVector = when (txType) {
        TransactionType.TRANSFER -> Icons.Rounded.SwapHoriz
        TransactionType.REFUND -> Icons.Rounded.Replay
        TransactionType.ASSET_PURCHASE, TransactionType.ASSET_SALE -> Icons.Rounded.AccountBalanceWallet
        else -> CategoryIconMapper.iconFor(category?.iconName)
    }

    val catColor = category?.colorHex?.let { parseHexColor(it, accentColor) } ?: accentColor
    val primaryLabel = transaction.merchant.ifBlank {
        transaction.description.ifBlank {
            when (txType) {
                TransactionType.TRANSFER -> "${account?.name ?: "Account"} → ${destinationAccount?.name ?: "Account"}"
                else -> category?.name ?: txType.label
            }
        }
    }
    val secondaryLabel = buildString {
        if (txType == TransactionType.TRANSFER) {
            append("${account?.name ?: "From"} → ${destinationAccount?.name ?: "To"}")
        } else {
            append(category?.name ?: txType.label)
            account?.let { append(" • ${it.name}") }
        }
        append(" • ${formatDateMillis(transaction.timestamp, preferences.dateFormat)}")
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (category != null) catColor.copy(alpha = 0.14f) else bgColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = txType.label,
                tint = if (category != null) catColor else accentColor,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = primaryLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = secondaryLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            val formatted = MoneyFormatter.formatWithMode(
                paise = transaction.amountPaise,
                mode = preferences.currencyDisplayMode,
                showDecimals = preferences.showPaiseDecimals,
            )
            Text(
                text = "$prefix$formatted",
                style = MaterialTheme.typography.titleSmall.merge(MoneyTextStyle),
                color = accentColor,
                maxLines = 1,
            )
            if (txType != TransactionType.EXPENSE && txType != TransactionType.INCOME) {
                Text(
                    text = txType.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    confirmLabel: String = "Delete",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
