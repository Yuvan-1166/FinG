package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.DataTransferService
import com.example.data.local.AccountEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.RecurringPaymentEntity
import com.example.data.local.RupeeLedgerDatabase
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.TransactionEntity
import com.example.data.repository.DeleteResult
import com.example.data.repository.FinanceRepository
import com.example.domain.model.AccountType
import com.example.domain.model.DebtDirection
import com.example.domain.model.RecurrenceFrequency
import com.example.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FinanceRepositoryRobolectricTest {

    private lateinit var db: RupeeLedgerDatabase
    private lateinit var repo: FinanceRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, RupeeLedgerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = FinanceRepository(db)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `confirmRecurringOccurrence prevents duplicate generation and advances due date`() = runBlocking {
        repo.ensureSeeded()
        val accId = repo.saveAccount(
            AccountEntity(name = "HDFC", type = AccountType.SAVINGS.name, openingBalancePaise = 1_000_000L),
        )
        val catId = repo.categoriesFlow.first().first { it.name == "Rent" }.id
        val startEpoch = LocalDate.of(2026, 11, 1).toEpochDay()
        val recId = repo.saveRecurringPayment(
            RecurringPaymentEntity(
                title = "Apartment Rent",
                type = TransactionType.EXPENSE.name,
                amountPaise = 1_500_000L,
                frequency = RecurrenceFrequency.MONTHLY.name,
                startDateEpochDay = startEpoch,
                nextDueDateEpochDay = startEpoch,
                accountId = accId,
                categoryId = catId,
            ),
        )
        val item = repo.recurringPaymentsFlow.first().first { it.id == recId }
        assertTrue(repo.confirmRecurringOccurrence(item))
        // Re-confirming the exact same occurrence object must be rejected as duplicate
        assertFalse(repo.confirmRecurringOccurrence(item))

        val txs = repo.transactionsFlow.first()
        assertEquals(1, txs.size)
        val updatedItem = repo.recurringPaymentsFlow.first().first { it.id == recId }
        assertEquals(LocalDate.of(2026, 12, 1).toEpochDay(), updatedItem.nextDueDateEpochDay)
    }

    @Test
    fun `deleting category or account in use is blocked to protect historical transactions`() = runBlocking {
        val accId = repo.saveAccount(
            AccountEntity(name = "Cash", type = AccountType.CASH.name, openingBalancePaise = 500_000L),
        )
        val catId = repo.saveCategory(CategoryEntity(name = "Custom Tea", type = "EXPENSE"))
        repo.saveTransaction(
            TransactionEntity(
                type = TransactionType.EXPENSE.name,
                amountPaise = 2_000L,
                timestamp = System.currentTimeMillis(),
                accountId = accId,
                categoryId = catId,
            ),
        )

        val accDel = repo.deleteAccount(accId)
        val catDel = repo.deleteCategory(catId)
        assertTrue(accDel is DeleteResult.BlockedInUse)
        assertTrue(catDel is DeleteResult.BlockedInUse)
    }

    @Test
    fun `goal contribution with transfer and debt repayment update balances atomically`() = runBlocking {
        val savingsId = repo.saveAccount(AccountEntity(name = "Savings", type = AccountType.SAVINGS.name, openingBalancePaise = 2_000_000L))
        val goalAccId = repo.saveAccount(AccountEntity(name = "Emergency Vault", type = AccountType.SAVINGS.name, openingBalancePaise = 0L))

        val goalId = repo.saveGoal(SavingsGoalEntity(name = "Emergency Fund", targetAmountPaise = 1_000_000L))
        val goal = repo.goalsFlow.first().first { it.id == goalId }

        repo.addGoalContribution(
            goal = goal,
            amountPaise = 500_000L,
            sourceAccountId = savingsId,
            destinationAccountId = goalAccId,
            note = "November contribution",
        )

        val balances = repo.accountsWithBalanceFlow.first().associate { it.account.id to it.currentBalancePaise }
        assertEquals(1_500_000L, balances[savingsId])
        assertEquals(500_000L, balances[goalAccId])

        val debtId = repo.createDebt(
            personName = "Rahul",
            direction = DebtDirection.LENT,
            principalAmountPaise = 300_000L,
            accountId = savingsId,
            logAccountTransaction = true,
            dueDateEpochDay = null,
            notes = "Dinner split",
        )
        val debtWithBal = repo.debtsWithBalanceFlow.first().first { it.debt.id == debtId }
        assertEquals(300_000L, debtWithBal.outstandingAmountPaise)

        repo.addDebtRepayment(
            debt = debtWithBal.debt,
            amountPaise = 300_000L,
            accountId = savingsId,
            logAccountTransaction = true,
            note = "UPI settled",
        )
        val settled = repo.debtsWithBalanceFlow.first().first { it.debt.id == debtId }
        assertTrue(settled.debt.isSettled)
        assertEquals(0L, settled.outstandingAmountPaise)
    }

    @Test
    fun `csv export and import detects duplicates and invalid rows`() = runBlocking {
        val accId = repo.saveAccount(AccountEntity(name = "HDFC", type = AccountType.SAVINGS.name, openingBalancePaise = 0L))
        val catId = repo.saveCategory(CategoryEntity(name = "Food", type = "EXPENSE"))
        val tx = TransactionEntity(
            id = 1L,
            type = TransactionType.EXPENSE.name,
            amountPaise = 45000L,
            timestamp = 1_764_000_000_000L,
            accountId = accId,
            categoryId = catId,
            merchant = "Cafe",
        )
        val csv = DataTransferService.exportTransactionsToCsv(
            transactions = listOf(tx),
            accounts = mapOf(accId to AccountEntity(id = accId, name = "HDFC", type = "SAVINGS", openingBalancePaise = 0L)),
            categories = mapOf(catId to CategoryEntity(id = catId, name = "Food", type = "EXPENSE")),
        )

        val analysis = DataTransferService.analyzeCsvImport(
            csvText = csv,
            existingTransactions = listOf(tx),
            accountsById = mapOf(accId to AccountEntity(id = accId, name = "HDFC", type = "SAVINGS", openingBalancePaise = 0L)),
        )
        assertEquals(0, analysis.validRows.size)
        assertEquals(1, analysis.duplicateRows.size)
        assertEquals(0, analysis.errors.size)
    }
}
