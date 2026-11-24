package com.example.domain.finance

import com.example.domain.model.AccountType
import com.example.domain.model.BudgetPeriod
import com.example.domain.model.BudgetStatus
import com.example.domain.model.DateRangePreset
import com.example.domain.model.RecurrenceFrequency
import com.example.domain.model.TransactionType
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs

data class AccountInput(
    val id: Long,
    val name: String,
    val type: AccountType,
    val openingBalancePaise: Long,
    val isArchived: Boolean,
)

data class TransactionInput(
    val id: Long,
    val type: TransactionType,
    val amountPaise: Long,
    val timestamp: Long,
    val accountId: Long,
    val destinationAccountId: Long?,
    val categoryId: Long?,
    val subcategoryId: Long?,
    val isDebtIncomingRepayment: Boolean = false,
)

data class AccountBalancePoint(
    val timestamp: Long,
    val balancePaise: Long,
)

data class DashboardSummary(
    val totalAvailableBalancePaise: Long,
    val creditCardLiabilityPaise: Long,
    val netWorthPaise: Long,
    val totalIncomePaise: Long,
    val grossExpensePaise: Long,
    val totalRefundPaise: Long,
    val netExpensePaise: Long,
    val netSavingsPaise: Long,
    val savingsRateTenthsPercent: Int, // e.g. 245 = 24.5%
)

data class CategoryBreakdownItem(
    val categoryId: Long,
    val categoryName: String,
    val colorHex: String,
    val iconName: String,
    val amountPaise: Long,
    val shareBasisPoints: Int, // 0..10000
)

data class BudgetUtilizationResult(
    val budgetId: Long,
    val name: String,
    val categoryId: Long?,
    val limitPaise: Long,
    val spentPaise: Long,
    val remainingPaise: Long,
    val utilizationPercent: Int,
    val status: BudgetStatus,
)

data class PeriodIncomeExpensePoint(
    val label: String,
    val yearMonth: YearMonth,
    val incomePaise: Long,
    val expensePaise: Long,
)

data class DailySpendPoint(
    val date: LocalDate,
    val label: String,
    val amountPaise: Long,
)

object AccountBalanceCalculator {

    /**
     * Computes exact current balances for all accounts using opening balance + transactions.
     *
     * Accounting rules:
     * - INCOME / REFUND / MONEY_BORROWED: +amount to source account
     * - EXPENSE / MONEY_LENT: -amount from source account (increases credit card liability if credit card)
     * - TRANSFER: -amount from source account, +amount to destination account (reduces liability if destination is credit card)
     * - DEBT_REPAYMENT:
     *     - if paying back borrowed money (isDebtIncomingRepayment == false): -amount from source account
     *     - if receiving lent money back (isDebtIncomingRepayment == true): +amount to source account
     */
    fun calculateBalances(
        accounts: List<AccountInput>,
        transactions: List<TransactionInput>,
    ): Map<Long, Long> {
        val balances = mutableMapOf<Long, Long>()
        accounts.forEach { balances[it.id] = it.openingBalancePaise }

        for (tx in transactions) {
            when (tx.type) {
                TransactionType.INCOME,
                TransactionType.REFUND,
                TransactionType.MONEY_BORROWED,
                TransactionType.ASSET_SALE -> {
                    balances[tx.accountId] = (balances[tx.accountId] ?: 0L) + tx.amountPaise
                }
                TransactionType.EXPENSE,
                TransactionType.MONEY_LENT,
                TransactionType.ASSET_PURCHASE -> {
                    balances[tx.accountId] = (balances[tx.accountId] ?: 0L) - tx.amountPaise
                }
                TransactionType.TRANSFER -> {
                    balances[tx.accountId] = (balances[tx.accountId] ?: 0L) - tx.amountPaise
                    tx.destinationAccountId?.let { destId ->
                        balances[destId] = (balances[destId] ?: 0L) + tx.amountPaise
                    }
                }
                TransactionType.DEBT_REPAYMENT -> {
                    val delta = if (tx.isDebtIncomingRepayment) tx.amountPaise else -tx.amountPaise
                    balances[tx.accountId] = (balances[tx.accountId] ?: 0L) + delta
                }
            }
        }
        return balances
    }

    fun calculateAccountHistory(
        account: AccountInput,
        transactions: List<TransactionInput>,
    ): List<AccountBalancePoint> {
        val relevant = transactions
            .filter { it.accountId == account.id || it.destinationAccountId == account.id }
            .sortedBy { it.timestamp }
        var running = account.openingBalancePaise
        val points = mutableListOf<AccountBalancePoint>()
        for (tx in relevant) {
            if (tx.accountId == account.id) {
                running += when (tx.type) {
                    TransactionType.INCOME,
                    TransactionType.REFUND,
                    TransactionType.MONEY_BORROWED,
                    TransactionType.ASSET_SALE -> tx.amountPaise
                    TransactionType.EXPENSE,
                    TransactionType.MONEY_LENT,
                    TransactionType.TRANSFER,
                    TransactionType.ASSET_PURCHASE -> -tx.amountPaise
                    TransactionType.DEBT_REPAYMENT ->
                        if (tx.isDebtIncomingRepayment) tx.amountPaise else -tx.amountPaise
                }
            }
            if (tx.type == TransactionType.TRANSFER && tx.destinationAccountId == account.id) {
                running += tx.amountPaise
            }
            points.add(AccountBalancePoint(timestamp = tx.timestamp, balancePaise = running))
        }
        return points
    }
}

object DashboardCalculator {

    fun calculateSummary(
        accounts: List<AccountInput>,
        accountBalances: Map<Long, Long>,
        periodTransactions: List<TransactionInput>,
    ): DashboardSummary {
        var totalAvailable = 0L
        var creditCardLiability = 0L
        var netWorth = 0L

        for (account in accounts) {
            if (account.isArchived) continue
            val bal = accountBalances[account.id] ?: account.openingBalancePaise
            netWorth += bal
            if (account.type == AccountType.CREDIT_CARD) {
                if (bal < 0L) creditCardLiability += abs(bal)
            } else {
                totalAvailable += bal
            }
        }

        var totalIncome = 0L
        var grossExpense = 0L
        var totalRefund = 0L

        for (tx in periodTransactions) {
            when (tx.type) {
                TransactionType.INCOME -> totalIncome += tx.amountPaise
                TransactionType.EXPENSE -> grossExpense += tx.amountPaise
                TransactionType.REFUND -> totalRefund += tx.amountPaise
                // Transfers, Debts, and Investment Asset Movements do NOT inflate Income or Expenses
                TransactionType.TRANSFER,
                TransactionType.MONEY_LENT,
                TransactionType.MONEY_BORROWED,
                TransactionType.DEBT_REPAYMENT,
                TransactionType.ASSET_PURCHASE,
                TransactionType.ASSET_SALE -> Unit
            }
        }

        val netExpense = (grossExpense - totalRefund).coerceAtLeast(0L)
        val netSavings = totalIncome - netExpense
        val savingsRateTenths = if (totalIncome > 0L) {
            ((netSavings * 1000L) / totalIncome).toInt().coerceIn(-9990, 1000)
        } else {
            0
        }

        return DashboardSummary(
            totalAvailableBalancePaise = totalAvailable,
            creditCardLiabilityPaise = creditCardLiability,
            netWorthPaise = netWorth,
            totalIncomePaise = totalIncome,
            grossExpensePaise = grossExpense,
            totalRefundPaise = totalRefund,
            netExpensePaise = netExpense,
            netSavingsPaise = netSavings,
            savingsRateTenthsPercent = savingsRateTenths,
        )
    }

    fun calculateExpenseByCategory(
        periodTransactions: List<TransactionInput>,
        categoryLookup: Map<Long, Triple<String, String, String>>, // id -> (name, colorHex, iconName)
    ): List<CategoryBreakdownItem> {
        val totals = mutableMapOf<Long, Long>()
        for (tx in periodTransactions) {
            val catId = tx.categoryId ?: -1L
            when (tx.type) {
                TransactionType.EXPENSE -> {
                    totals[catId] = (totals[catId] ?: 0L) + tx.amountPaise
                }
                TransactionType.REFUND -> {
                    if (totals.containsKey(catId)) {
                        totals[catId] = ((totals[catId] ?: 0L) - tx.amountPaise).coerceAtLeast(0L)
                    }
                }
                else -> Unit
            }
        }

        val positiveEntries = totals.filterValues { it > 0L }
        val grandTotal = positiveEntries.values.sum()
        if (grandTotal <= 0L) return emptyList()

        return positiveEntries.entries
            .sortedByDescending { it.value }
            .map { (catId, amount) ->
                val meta = categoryLookup[catId] ?: Triple("Uncategorized", "#64748B", "category")
                CategoryBreakdownItem(
                    categoryId = catId,
                    categoryName = meta.first,
                    colorHex = meta.second,
                    iconName = meta.third,
                    amountPaise = amount,
                    shareBasisPoints = ((amount * 10000L) / grandTotal).toInt(),
                )
            }
    }

    fun calculateBudgetUtilization(
        budgetId: Long,
        name: String,
        categoryId: Long?,
        limitPaise: Long,
        alertThresholdPercent: Int,
        periodTransactions: List<TransactionInput>,
    ): BudgetUtilizationResult {
        var gross = 0L
        var refunds = 0L
        for (tx in periodTransactions) {
            val matchesCategory = categoryId == null || tx.categoryId == categoryId || tx.subcategoryId == categoryId
            if (!matchesCategory) continue
            when (tx.type) {
                TransactionType.EXPENSE -> gross += tx.amountPaise
                TransactionType.REFUND -> refunds += tx.amountPaise
                else -> Unit
            }
        }
        val spent = (gross - refunds).coerceAtLeast(0L)
        val remaining = limitPaise - spent
        val percent = if (limitPaise > 0L) ((spent * 100L) / limitPaise).toInt() else 0
        val status = when {
            spent > limitPaise -> BudgetStatus.EXCEEDED
            percent >= alertThresholdPercent -> BudgetStatus.APPROACHING
            else -> BudgetStatus.NORMAL
        }
        return BudgetUtilizationResult(
            budgetId = budgetId,
            name = name,
            categoryId = categoryId,
            limitPaise = limitPaise,
            spentPaise = spent,
            remainingPaise = remaining,
            utilizationPercent = percent,
            status = status,
        )
    }

    fun calculateMonthlySeries(
        allTransactions: List<TransactionInput>,
        months: List<YearMonth>,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<PeriodIncomeExpensePoint> {
        return months.map { ym ->
            val (startMs, endMs) = DateRangeUtils.monthRange(ym, zoneId)
            var income = 0L
            var expense = 0L
            var refund = 0L
            for (tx in allTransactions) {
                if (tx.timestamp in startMs..endMs) {
                    when (tx.type) {
                        TransactionType.INCOME -> income += tx.amountPaise
                        TransactionType.EXPENSE -> expense += tx.amountPaise
                        TransactionType.REFUND -> refund += tx.amountPaise
                        else -> Unit
                    }
                }
            }
            val label = ym.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
            PeriodIncomeExpensePoint(
                label = label,
                yearMonth = ym,
                incomePaise = income,
                expensePaise = (expense - refund).coerceAtLeast(0L),
            )
        }
    }

    fun calculateDailySpendingSeries(
        periodTransactions: List<TransactionInput>,
        startDate: LocalDate,
        endDate: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<DailySpendPoint> {
        val byDate = mutableMapOf<LocalDate, Long>()
        for (tx in periodTransactions) {
            val date = Instant.ofEpochMilli(tx.timestamp).atZone(zoneId).toLocalDate()
            when (tx.type) {
                TransactionType.EXPENSE -> byDate[date] = (byDate[date] ?: 0L) + tx.amountPaise
                TransactionType.REFUND -> byDate[date] = ((byDate[date] ?: 0L) - tx.amountPaise).coerceAtLeast(0L)
                else -> Unit
            }
        }
        val points = mutableListOf<DailySpendPoint>()
        var cursor = startDate
        while (!cursor.isAfter(endDate)) {
            points.add(
                DailySpendPoint(
                    date = cursor,
                    label = "${cursor.dayOfMonth}",
                    amountPaise = byDate[cursor] ?: 0L,
                )
            )
            cursor = cursor.plusDays(1)
        }
        return points
    }
}

object RecurrenceCalculator {

    fun nextOccurrence(currentDueDate: LocalDate, frequency: RecurrenceFrequency): LocalDate {
        return when (frequency) {
            RecurrenceFrequency.DAILY -> currentDueDate.plusDays(1)
            RecurrenceFrequency.WEEKLY -> currentDueDate.plusWeeks(1)
            RecurrenceFrequency.MONTHLY -> currentDueDate.plusMonths(1)
            RecurrenceFrequency.YEARLY -> currentDueDate.plusYears(1)
        }
    }
}

object DateRangeUtils {

    fun monthRange(yearMonth: YearMonth, zoneId: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
        val start = yearMonth.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = yearMonth.atEndOfMonth().atTime(LocalTime.MAX).atZone(zoneId).toInstant().toEpochMilli()
        return start to end
    }

    fun weekRange(referenceDate: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
        val monday = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sunday = monday.plusDays(6)
        val start = monday.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = sunday.atTime(LocalTime.MAX).atZone(zoneId).toInstant().toEpochMilli()
        return start to end
    }

    fun presetDates(
        preset: DateRangePreset,
        today: LocalDate = LocalDate.now(),
        customStart: LocalDate = today.withDayOfMonth(1),
        customEnd: LocalDate = today,
    ): Pair<LocalDate, LocalDate> {
        return when (preset) {
            DateRangePreset.THIS_WEEK -> {
                val mon = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                mon to mon.plusDays(6)
            }
            DateRangePreset.THIS_MONTH -> today.withDayOfMonth(1) to YearMonth.from(today).atEndOfMonth()
            DateRangePreset.PREVIOUS_MONTH -> {
                val prev = YearMonth.from(today).minusMonths(1)
                prev.atDay(1) to prev.atEndOfMonth()
            }
            DateRangePreset.LAST_THREE_MONTHS -> {
                val startMonth = YearMonth.from(today).minusMonths(2)
                startMonth.atDay(1) to YearMonth.from(today).atEndOfMonth()
            }
            DateRangePreset.THIS_YEAR -> LocalDate.of(today.year, 1, 1) to LocalDate.of(today.year, 12, 31)
            DateRangePreset.CUSTOM -> {
                if (customStart.isAfter(customEnd)) customEnd to customStart else customStart to customEnd
            }
        }
    }

    fun toEpochMillisRange(
        startDate: LocalDate,
        endDate: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Pair<Long, Long> {
        val start = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = endDate.atTime(LocalTime.MAX).atZone(zoneId).toInstant().toEpochMilli()
        return start to end
    }

    fun budgetPeriodDates(
        period: BudgetPeriod,
        today: LocalDate = LocalDate.now(),
        customStartEpochDay: Long? = null,
        customEndEpochDay: Long? = null,
    ): Pair<LocalDate, LocalDate> {
        return when (period) {
            BudgetPeriod.MONTHLY -> today.withDayOfMonth(1) to YearMonth.from(today).atEndOfMonth()
            BudgetPeriod.WEEKLY -> {
                val mon = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                mon to mon.plusDays(6)
            }
            BudgetPeriod.CUSTOM -> {
                val start = customStartEpochDay?.let { LocalDate.ofEpochDay(it) } ?: today.withDayOfMonth(1)
                val end = customEndEpochDay?.let { LocalDate.ofEpochDay(it) } ?: YearMonth.from(today).atEndOfMonth()
                if (start.isAfter(end)) end to start else start to end
            }
        }
    }
}
