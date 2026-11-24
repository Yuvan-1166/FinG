package com.example

import com.example.domain.finance.AccountBalanceCalculator
import com.example.domain.finance.AccountInput
import com.example.domain.finance.DashboardCalculator
import com.example.domain.finance.MoneyFormatter
import com.example.domain.finance.RecurrenceCalculator
import com.example.domain.finance.TransactionInput
import com.example.domain.model.AccountType
import com.example.domain.model.BudgetStatus
import com.example.domain.model.RecurrenceFrequency
import com.example.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class FinanceCalculatorsTest {

    @Test
    fun `indian rupee formatting handles lakhs and crores accurately`() {
        assertEquals("₹0.00", MoneyFormatter.format(0L))
        assertEquals("₹1,234.50", MoneyFormatter.format(123_450L))
        assertEquals("₹1,23,456.75", MoneyFormatter.format(12_345_675L))
        assertEquals("₹12,34,56,789.00", MoneyFormatter.format(12_345_678_900L))
        assertEquals("-₹5,000.00", MoneyFormatter.format(-500_000L))
        assertEquals("₹1.50L", MoneyFormatter.format(15_000_000L, compact = true))
        assertEquals("₹2.50Cr", MoneyFormatter.format(2_500_000_000L, compact = true))
    }

    @Test
    fun `parseRupeesToPaise parses valid INR strings without floating point loss`() {
        assertEquals(125050L, MoneyFormatter.parseRupeesToPaise("1,250.50"))
        assertEquals(50000L, MoneyFormatter.parseRupeesToPaise("₹500"))
        assertEquals(1050L, MoneyFormatter.parseRupeesToPaise("10.5"))
        assertEquals(1999L, MoneyFormatter.parseRupeesToPaise("19.99"))
        assertNull(MoneyFormatter.parseRupeesToPaise("12.345"))
        assertNull(MoneyFormatter.parseRupeesToPaise("abc"))
        assertNull(MoneyFormatter.parseRupeesToPaise(""))
    }

    @Test
    fun `transfers preserve net worth and do not count as income or expense`() {
        val accounts = listOf(
            AccountInput(1L, "HDFC Savings", AccountType.SAVINGS, 1_000_000L, false), // ₹10,000
            AccountInput(2L, "Cash", AccountType.CASH, 200_000L, false),           // ₹2,000
        )
        val txs = listOf(
            TransactionInput(
                id = 1L,
                type = TransactionType.TRANSFER,
                amountPaise = 500_000L, // ₹5,000
                timestamp = 1000L,
                accountId = 1L,
                destinationAccountId = 2L,
                categoryId = null,
                subcategoryId = null,
            )
        )
        val balances = AccountBalanceCalculator.calculateBalances(accounts, txs)
        assertEquals(500_000L, balances[1L])
        assertEquals(700_000L, balances[2L])

        val summary = DashboardCalculator.calculateSummary(accounts, balances, txs)
        assertEquals(1_200_000L, summary.totalAvailableBalancePaise)
        assertEquals(1_200_000L, summary.netWorthPaise)
        assertEquals(0L, summary.totalIncomePaise)
        assertEquals(0L, summary.netExpensePaise)
    }

    @Test
    fun `credit card purchases increase expense and liability while repayments reduce liability without double expense`() {
        val accounts = listOf(
            AccountInput(1L, "Bank", AccountType.SAVINGS, 2_000_000L, false),      // ₹20,000
            AccountInput(2L, "Credit Card", AccountType.CREDIT_CARD, 0L, false),   // ₹0
        )
        val txs = listOf(
            // ₹3,000 purchase on credit card
            TransactionInput(1L, TransactionType.EXPENSE, 300_000L, 1000L, 2L, null, 10L, null),
            // ₹1,000 refund on credit card
            TransactionInput(2L, TransactionType.REFUND, 100_000L, 2000L, 2L, null, 10L, null),
            // ₹2,000 repayment from Bank to Credit Card
            TransactionInput(3L, TransactionType.TRANSFER, 200_000L, 3000L, 1L, 2L, null, null),
        )
        val balances = AccountBalanceCalculator.calculateBalances(accounts, txs)
        assertEquals(1_800_000L, balances[1L]) // Bank reduced by ₹2,000
        assertEquals(0L, balances[2L])         // Credit card settled back to ₹0

        val summary = DashboardCalculator.calculateSummary(accounts, balances, txs)
        assertEquals(300_000L, summary.grossExpensePaise)
        assertEquals(100_000L, summary.totalRefundPaise)
        assertEquals(200_000L, summary.netExpensePaise)
        assertEquals(0L, summary.creditCardLiabilityPaise)
        assertEquals(1_800_000L, summary.netWorthPaise)
    }

    @Test
    fun `budget utilization detects normal approaching and exceeded thresholds`() {
        val txs = listOf(
            TransactionInput(1L, TransactionType.EXPENSE, 850_000L, 1000L, 1L, null, 5L, null),
            TransactionInput(2L, TransactionType.REFUND, 50_000L, 2000L, 1L, null, 5L, null),
        )
        val approaching = DashboardCalculator.calculateBudgetUtilization(
            budgetId = 1L,
            name = "Food",
            categoryId = 5L,
            limitPaise = 1_000_000L,
            alertThresholdPercent = 80,
            periodTransactions = txs,
        )
        assertEquals(800_000L, approaching.spentPaise)
        assertEquals(80, approaching.utilizationPercent)
        assertEquals(BudgetStatus.APPROACHING, approaching.status)

        val exceeded = DashboardCalculator.calculateBudgetUtilization(
            budgetId = 1L,
            name = "Food",
            categoryId = 5L,
            limitPaise = 700_000L,
            alertThresholdPercent = 80,
            periodTransactions = txs,
        )
        assertEquals(BudgetStatus.EXCEEDED, exceeded.status)
        assertEquals(-100_000L, exceeded.remainingPaise)
    }

    @Test
    fun `recurrence calculator advances monthly and leap year dates safely`() {
        val jan31 = LocalDate.of(2026, 1, 31)
        assertEquals(LocalDate.of(2026, 2, 28), RecurrenceCalculator.nextOccurrence(jan31, RecurrenceFrequency.MONTHLY))
        assertEquals(LocalDate.of(2026, 2, 7), RecurrenceCalculator.nextOccurrence(jan31, RecurrenceFrequency.WEEKLY))
        assertEquals(LocalDate.of(2027, 1, 31), RecurrenceCalculator.nextOccurrence(jan31, RecurrenceFrequency.YEARLY))
    }
}
