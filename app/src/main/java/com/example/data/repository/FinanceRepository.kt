package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AccountDao
import com.example.data.local.AccountEntity
import com.example.data.local.AppPreferenceDao
import com.example.data.local.BudgetDao
import com.example.data.local.BudgetEntity
import com.example.data.local.CategoryDao
import com.example.data.local.CategoryEntity
import com.example.data.local.DatabaseSeeder
import com.example.data.local.DebtDao
import com.example.data.local.DebtEntity
import com.example.data.local.DebtRepaymentEntity
import com.example.data.local.GoldCategoryEntity
import com.example.data.local.GoldItemEntity
import com.example.data.local.GoldObligationEntity
import com.example.data.local.GoldObligationSettlementEntity
import com.example.data.local.GoldPurchaseEntity
import com.example.data.local.GoldPurchaseItemEntity
import com.example.data.local.GoldRateSnapshotEntity
import com.example.data.local.GoldSaleEntity
import com.example.data.local.GoldSaleItemEntity
import com.example.data.local.GoldValuationSnapshotEntity
import com.example.data.local.GoalContributionEntity
import com.example.data.local.RecurringPaymentDao
import com.example.data.local.RecurringPaymentEntity
import com.example.data.local.RupeeLedgerDatabase
import com.example.data.local.SavingsGoalDao
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.StockEntity
import com.example.data.local.StockHoldingEntity
import com.example.data.local.StockPriceSnapshotEntity
import com.example.data.local.StockTransactionEntity
import com.example.data.local.TransactionDao
import com.example.data.local.TransactionEntity
import com.example.domain.finance.AccountBalanceCalculator
import com.example.domain.finance.AccountInput
import com.example.domain.finance.RecurrenceCalculator
import com.example.domain.finance.TransactionInput
import com.example.domain.model.AccountType
import com.example.domain.model.DebtDirection
import com.example.domain.model.RecurrenceFrequency
import com.example.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import kotlin.math.abs

sealed interface DeleteResult {
    data object Deleted : DeleteResult
    data class BlockedInUse(val reason: String) : DeleteResult
}

data class AccountWithBalance(
    val account: AccountEntity,
    val currentBalancePaise: Long,
)

data class GoalWithProgress(
    val goal: SavingsGoalEntity,
    val savedAmountPaise: Long,
    val progressPercent: Int,
)

data class DebtWithBalance(
    val debt: DebtEntity,
    val repaidAmountPaise: Long,
    val outstandingAmountPaise: Long,
)

class FinanceRepository(
    private val db: RupeeLedgerDatabase,
    private val accountDao: AccountDao = db.accountDao(),
    private val categoryDao: CategoryDao = db.categoryDao(),
    private val transactionDao: TransactionDao = db.transactionDao(),
    private val budgetDao: BudgetDao = db.budgetDao(),
    private val savingsGoalDao: SavingsGoalDao = db.savingsGoalDao(),
    private val recurringPaymentDao: RecurringPaymentDao = db.recurringPaymentDao(),
    private val debtDao: DebtDao = db.debtDao(),
    private val appPreferenceDao: AppPreferenceDao = db.appPreferenceDao(),
) {
    val accountsFlow: Flow<List<AccountEntity>> = accountDao.observeAll()
    val categoriesFlow: Flow<List<CategoryEntity>> = categoryDao.observeAll()
    val transactionsFlow: Flow<List<TransactionEntity>> = transactionDao.observeAll()
    val budgetsFlow: Flow<List<BudgetEntity>> = budgetDao.observeAll()
    val goalsFlow: Flow<List<SavingsGoalEntity>> = savingsGoalDao.observeAllGoals()
    val goalContributionsFlow: Flow<List<GoalContributionEntity>> = savingsGoalDao.observeAllContributions()
    val recurringPaymentsFlow: Flow<List<RecurringPaymentEntity>> = recurringPaymentDao.observeAll()
    val debtsFlow: Flow<List<DebtEntity>> = debtDao.observeAllDebts()
    val debtRepaymentsFlow: Flow<List<DebtRepaymentEntity>> = debtDao.observeAllRepayments()

    val accountsWithBalanceFlow: Flow<List<AccountWithBalance>> =
        combine(accountsFlow, transactionsFlow) { accounts, txs ->
            val balances = AccountBalanceCalculator.calculateBalances(
                accounts = accounts.map { it.toInput() },
                transactions = txs.map { it.toInput() },
            )
            accounts.map { account ->
                AccountWithBalance(
                    account = account,
                    currentBalancePaise = balances[account.id] ?: account.openingBalancePaise,
                )
            }
        }

    val goalsWithProgressFlow: Flow<List<GoalWithProgress>> =
        combine(goalsFlow, goalContributionsFlow) { goals, contributions ->
            val byGoal = contributions.groupBy { it.goalId }
            goals.map { goal ->
                val saved = (byGoal[goal.id]?.sumOf { it.amountPaise } ?: 0L).coerceAtLeast(0L)
                val pct = if (goal.targetAmountPaise > 0L) {
                    ((saved * 100L) / goal.targetAmountPaise).toInt().coerceIn(0, 100)
                } else 0
                GoalWithProgress(goal = goal, savedAmountPaise = saved, progressPercent = pct)
            }
        }

    val debtsWithBalanceFlow: Flow<List<DebtWithBalance>> =
        combine(debtsFlow, debtRepaymentsFlow) { debts, repayments ->
            val byDebt = repayments.groupBy { it.debtId }
            debts.map { debt ->
                val repaid = byDebt[debt.id]?.sumOf { it.amountPaise } ?: 0L
                val outstanding = (debt.principalAmountPaise - repaid).coerceAtLeast(0L)
                DebtWithBalance(
                    debt = debt,
                    repaidAmountPaise = repaid,
                    outstandingAmountPaise = outstanding,
                )
            }
        }

    suspend fun ensureSeeded() {
        DatabaseSeeder.seedInitialCategoriesIfEmpty(categoryDao)
    }

    // --- Accounts ---
    suspend fun saveAccount(account: AccountEntity): Long {
        return if (account.id == 0L) {
            accountDao.insert(account)
        } else {
            accountDao.update(account)
            account.id
        }
    }

    suspend fun toggleAccountArchive(account: AccountEntity) {
        accountDao.update(account.copy(isArchived = !account.isArchived))
    }

    suspend fun deleteAccount(accountId: Long): DeleteResult {
        val txCount = accountDao.countTransactionsForAccount(accountId)
        return if (txCount > 0) {
            DeleteResult.BlockedInUse("This account has $txCount historical transaction(s). Archive it instead to keep your ledger accurate.")
        } else {
            accountDao.deleteById(accountId)
            DeleteResult.Deleted
        }
    }

    // --- Categories ---
    suspend fun saveCategory(category: CategoryEntity): Long {
        return if (category.id == 0L) {
            categoryDao.insert(category)
        } else {
            categoryDao.update(category)
            category.id
        }
    }

    suspend fun toggleCategoryArchive(category: CategoryEntity) {
        categoryDao.update(category.copy(isArchived = !category.isArchived))
    }

    suspend fun deleteCategory(categoryId: Long): DeleteResult {
        val txCount = categoryDao.countTransactionsUsingCategory(categoryId)
        val subCount = categoryDao.countSubcategories(categoryId)
        return when {
            txCount > 0 -> DeleteResult.BlockedInUse("This category is used by $txCount transaction(s). Archive it instead to preserve history.")
            subCount > 0 -> DeleteResult.BlockedInUse("Delete or reassign subcategories first.")
            else -> {
                categoryDao.deleteById(categoryId)
                DeleteResult.Deleted
            }
        }
    }

    // --- Transactions ---
    suspend fun getTransactionById(id: Long): TransactionEntity? = transactionDao.getById(id)

    suspend fun saveTransaction(tx: TransactionEntity): Long {
        require(tx.amountPaise > 0L) { "Transaction amount must be positive." }
        if (tx.type == TransactionType.TRANSFER.name) {
            require(tx.destinationAccountId != null && tx.destinationAccountId != tx.accountId) {
                "Transfer requires a distinct destination account."
            }
        }
        val now = System.currentTimeMillis()
        return if (tx.id == 0L) {
            transactionDao.insert(tx.copy(createdAt = now, updatedAt = now))
        } else {
            transactionDao.update(tx.copy(updatedAt = now))
            tx.id
        }
    }

    suspend fun deleteTransaction(transactionId: Long) {
        transactionDao.deleteById(transactionId)
    }

    // --- Budgets ---
    suspend fun saveBudget(budget: BudgetEntity): Long {
        require(budget.limitAmountPaise > 0L) { "Budget limit must be positive." }
        return if (budget.id == 0L) {
            budgetDao.insert(budget)
        } else {
            budgetDao.update(budget)
            budget.id
        }
    }

    suspend fun deleteBudget(budgetId: Long) {
        budgetDao.deleteById(budgetId)
    }

    // --- Savings Goals ---
    fun observeContributionsForGoal(goalId: Long): Flow<List<GoalContributionEntity>> =
        savingsGoalDao.observeContributionsForGoal(goalId)

    suspend fun saveGoal(goal: SavingsGoalEntity): Long {
        require(goal.targetAmountPaise > 0L) { "Goal target must be positive." }
        return if (goal.id == 0L) {
            savingsGoalDao.insertGoal(goal)
        } else {
            savingsGoalDao.updateGoal(goal)
            goal.id
        }
    }

    suspend fun deleteGoal(goalId: Long) {
        savingsGoalDao.deleteGoalById(goalId)
    }

    suspend fun addGoalContribution(
        goal: SavingsGoalEntity,
        amountPaise: Long, // positive = deposit, negative = withdrawal
        sourceAccountId: Long?,
        destinationAccountId: Long?,
        note: String,
    ) {
        require(amountPaise != 0L) { "Contribution amount cannot be zero." }
        db.withTransaction {
            val now = System.currentTimeMillis()
            var linkedTxId: Long? = null
            if (sourceAccountId != null && destinationAccountId != null && sourceAccountId != destinationAccountId) {
                val isDeposit = amountPaise > 0L
                val tx = TransactionEntity(
                    type = TransactionType.TRANSFER.name,
                    amountPaise = abs(amountPaise),
                    timestamp = now,
                    accountId = if (isDeposit) sourceAccountId else destinationAccountId,
                    destinationAccountId = if (isDeposit) destinationAccountId else sourceAccountId,
                    merchant = goal.name,
                    description = if (isDeposit) "Goal contribution: ${goal.name}" else "Goal withdrawal: ${goal.name}",
                    notes = note,
                    createdAt = now,
                    updatedAt = now,
                )
                linkedTxId = transactionDao.insert(tx)
            }

            savingsGoalDao.insertContribution(
                GoalContributionEntity(
                    goalId = goal.id,
                    amountPaise = amountPaise,
                    sourceAccountId = sourceAccountId,
                    destinationAccountId = destinationAccountId,
                    linkedTransactionId = linkedTxId,
                    note = note,
                    timestamp = now,
                )
            )

            val allContribs = savingsGoalDao.getAllContributions().filter { it.goalId == goal.id }
            val totalSaved = allContribs.sumOf { it.amountPaise }.coerceAtLeast(0L)
            val completed = totalSaved >= goal.targetAmountPaise
            if (goal.isCompleted != completed) {
                savingsGoalDao.updateGoal(goal.copy(isCompleted = completed))
            }
        }
    }

    // --- Recurring Payments ---
    suspend fun saveRecurringPayment(item: RecurringPaymentEntity): Long {
        require(item.amountPaise > 0L) { "Recurring amount must be positive." }
        return if (item.id == 0L) {
            recurringPaymentDao.insert(item)
        } else {
            recurringPaymentDao.update(item)
            item.id
        }
    }

    suspend fun deleteRecurringPayment(id: Long) {
        recurringPaymentDao.deleteById(id)
    }

    suspend fun toggleRecurringActive(item: RecurringPaymentEntity) {
        recurringPaymentDao.update(item.copy(isActive = !item.isActive))
    }

    /**
     * Confirms a recurring occurrence atomically and prevents duplicate generation
     * via both an explicit lookup and the DB unique index on (recurringPaymentId, recurringOccurrenceEpochDay).
     */
    suspend fun confirmRecurringOccurrence(item: RecurringPaymentEntity): Boolean {
        return db.withTransaction {
            val dueEpochDay = item.nextDueDateEpochDay
            val existing = transactionDao.findByRecurringOccurrence(item.id, dueEpochDay)
            if (existing != null) {
                return@withTransaction false
            }

            val now = System.currentTimeMillis()
            transactionDao.insert(
                TransactionEntity(
                    type = item.type,
                    amountPaise = item.amountPaise,
                    timestamp = now,
                    accountId = item.accountId,
                    categoryId = item.categoryId,
                    merchant = item.merchant.ifBlank { item.title },
                    description = "${item.title} (Recurring)",
                    notes = item.notes,
                    recurringPaymentId = item.id,
                    recurringOccurrenceEpochDay = dueEpochDay,
                    createdAt = now,
                    updatedAt = now,
                )
            )

            val freq = runCatching { RecurrenceFrequency.valueOf(item.frequency) }
                .getOrDefault(RecurrenceFrequency.MONTHLY)
            val nextDate = RecurrenceCalculator.nextOccurrence(LocalDate.ofEpochDay(dueEpochDay), freq)
            val nextEpochDay = nextDate.toEpochDay()
            val stillActive = item.endDateEpochDay == null || nextEpochDay <= item.endDateEpochDay

            recurringPaymentDao.update(
                item.copy(
                    lastConfirmedEpochDay = dueEpochDay,
                    nextDueDateEpochDay = nextEpochDay,
                    isActive = stillActive,
                )
            )
            true
        }
    }

    suspend fun skipRecurringOccurrence(item: RecurringPaymentEntity) {
        db.withTransaction {
            val freq = runCatching { RecurrenceFrequency.valueOf(item.frequency) }
                .getOrDefault(RecurrenceFrequency.MONTHLY)
            val nextDate = RecurrenceCalculator.nextOccurrence(
                LocalDate.ofEpochDay(item.nextDueDateEpochDay),
                freq,
            )
            val nextEpochDay = nextDate.toEpochDay()
            val stillActive = item.endDateEpochDay == null || nextEpochDay <= item.endDateEpochDay
            recurringPaymentDao.update(
                item.copy(
                    nextDueDateEpochDay = nextEpochDay,
                    isActive = stillActive,
                )
            )
        }
    }

    // --- Debts ---
    fun observeRepaymentsForDebt(debtId: Long): Flow<List<DebtRepaymentEntity>> =
        debtDao.observeRepaymentsForDebt(debtId)

    suspend fun createDebt(
        personName: String,
        direction: DebtDirection,
        principalAmountPaise: Long,
        accountId: Long?,
        logAccountTransaction: Boolean,
        dueDateEpochDay: Long?,
        notes: String,
    ): Long {
        require(principalAmountPaise > 0L) { "Debt amount must be positive." }
        return db.withTransaction {
            val now = System.currentTimeMillis()
            val initialTxId = if (logAccountTransaction && accountId != null) {
                val txType = if (direction == DebtDirection.LENT) {
                    TransactionType.MONEY_LENT.name
                } else {
                    TransactionType.MONEY_BORROWED.name
                }
                transactionDao.insert(
                    TransactionEntity(
                        type = txType,
                        amountPaise = principalAmountPaise,
                        timestamp = now,
                        accountId = accountId,
                        merchant = personName,
                        description = if (direction == DebtDirection.LENT) "Lent to $personName" else "Borrowed from $personName",
                        notes = notes,
                        createdAt = now,
                        updatedAt = now,
                    )
                )
            } else null

            val debtId = debtDao.insertDebt(
                DebtEntity(
                    personName = personName,
                    direction = direction.name,
                    principalAmountPaise = principalAmountPaise,
                    accountId = accountId,
                    initialTransactionId = initialTxId,
                    dueDateEpochDay = dueDateEpochDay,
                    notes = notes,
                    isSettled = false,
                    createdAt = now,
                )
            )
            if (initialTxId != null) {
                transactionDao.getById(initialTxId)?.let { tx ->
                    transactionDao.update(tx.copy(debtId = debtId))
                }
            }
            debtId
        }
    }

    suspend fun addDebtRepayment(
        debt: DebtEntity,
        amountPaise: Long,
        accountId: Long?,
        logAccountTransaction: Boolean,
        note: String,
    ) {
        require(amountPaise > 0L) { "Repayment amount must be positive." }
        db.withTransaction {
            val now = System.currentTimeMillis()
            val isIncoming = debt.direction == DebtDirection.LENT.name
            val linkedTxId = if (logAccountTransaction && accountId != null) {
                transactionDao.insert(
                    TransactionEntity(
                        type = TransactionType.DEBT_REPAYMENT.name,
                        amountPaise = amountPaise,
                        timestamp = now,
                        accountId = accountId,
                        merchant = debt.personName,
                        description = if (isIncoming) {
                            "Repayment received from ${debt.personName}"
                        } else {
                            "Repayment paid to ${debt.personName}"
                        },
                        notes = note,
                        debtId = debt.id,
                        isDebtIncomingRepayment = isIncoming,
                        createdAt = now,
                        updatedAt = now,
                    )
                )
            } else null

            debtDao.insertRepayment(
                DebtRepaymentEntity(
                    debtId = debt.id,
                    amountPaise = amountPaise,
                    accountId = accountId,
                    linkedTransactionId = linkedTxId,
                    note = note,
                    timestamp = now,
                )
            )

            val totalRepaid = debtDao.getAllRepayments()
                .filter { it.debtId == debt.id }
                .sumOf { it.amountPaise }
            val settled = totalRepaid >= debt.principalAmountPaise
            if (debt.isSettled != settled) {
                debtDao.updateDebt(debt.copy(isSettled = settled))
            }
        }
    }

    suspend fun deleteDebt(debtId: Long) {
        debtDao.deleteDebtById(debtId)
    }

    suspend fun getSnapshot() = db.withTransaction {
        val goldDao = db.goldDao()
        val stockDao = db.stockDao()
        DatabaseSnapshot(
            schemaVersion = 2,
            accounts = accountDao.getAll(),
            categories = categoryDao.getAll(),
            transactions = transactionDao.getAll(),
            budgets = budgetDao.getAll(),
            goals = savingsGoalDao.getAllGoals(),
            goalContributions = savingsGoalDao.getAllContributions(),
            recurringPayments = recurringPaymentDao.getAll(),
            debts = debtDao.getAllDebts(),
            debtRepayments = debtDao.getAllRepayments(),
            preferences = appPreferenceDao.getAll(),
            goldCategories = goldDao.getCategories(),
            goldItems = goldDao.getItems(),
            goldPurchases = goldDao.getPurchases(),
            goldPurchaseItems = goldDao.getPurchaseItems(),
            goldSales = goldDao.getSales(),
            goldSaleItems = goldDao.getSaleItems(),
            goldObligations = goldDao.getObligations(),
            goldSettlements = goldDao.getSettlements(),
            goldRateSnapshots = goldDao.getRateSnapshots(),
            goldValuationSnapshots = goldDao.getValuationSnapshots(),
            stocks = stockDao.getStocks(),
            stockHoldings = stockDao.getHoldings(),
            stockTransactions = stockDao.getTransactions(),
            stockPriceSnapshots = stockDao.getPriceSnapshots(),
        )
    }

    suspend fun restoreSnapshot(snapshot: DatabaseSnapshot) {
        db.withTransaction {
            val goldDao = db.goldDao()
            val stockDao = db.stockDao()
            // Clear children before parents to respect FK constraints
            stockDao.deleteAllPriceSnapshots()
            stockDao.deleteAllTransactions()
            stockDao.deleteAllHoldings()
            stockDao.deleteAllStocks()
            goldDao.deleteAllSettlements()
            goldDao.deleteAllObligations()
            goldDao.deleteAllSaleItems()
            goldDao.deleteAllSales()
            goldDao.deleteAllPurchaseItems()
            goldDao.deleteAllPurchases()
            goldDao.deleteAllItems()
            goldDao.deleteAllCategories()
            goldDao.deleteAllRateSnapshots()
            goldDao.deleteAllValuationSnapshots()

            debtDao.deleteAllRepayments()
            debtDao.deleteAllDebts()
            savingsGoalDao.deleteAllContributions()
            savingsGoalDao.deleteAllGoals()
            recurringPaymentDao.deleteAll()
            budgetDao.deleteAll()
            transactionDao.deleteAll()
            categoryDao.deleteAll()
            accountDao.deleteAll()
            appPreferenceDao.deleteAll()

            accountDao.insertAll(snapshot.accounts)
            // Insert parent categories before subcategories to satisfy self-referencing FK
            val parents = snapshot.categories.filter { it.parentCategoryId == null }
            val children = snapshot.categories.filter { it.parentCategoryId != null }
            categoryDao.insertAll(parents)
            categoryDao.insertAll(children)
            transactionDao.insertAll(snapshot.transactions)
            budgetDao.insertAll(snapshot.budgets)
            savingsGoalDao.insertAllGoals(snapshot.goals)
            savingsGoalDao.insertAllContributions(snapshot.goalContributions)
            recurringPaymentDao.insertAll(snapshot.recurringPayments)
            debtDao.insertAllDebts(snapshot.debts)
            debtDao.insertAllRepayments(snapshot.debtRepayments)
            appPreferenceDao.insertAll(snapshot.preferences)

            val goldParents = snapshot.goldCategories.filter { it.parentCategoryId == null }
            val goldChildren = snapshot.goldCategories.filter { it.parentCategoryId != null }
            goldDao.insertAllCategories(goldParents)
            goldDao.insertAllCategories(goldChildren)
            goldDao.insertAllPurchases(snapshot.goldPurchases)
            goldDao.insertAllItems(snapshot.goldItems)
            goldDao.insertAllPurchaseItems(snapshot.goldPurchaseItems)
            goldDao.insertAllSales(snapshot.goldSales)
            goldDao.insertAllSaleItems(snapshot.goldSaleItems)
            goldDao.insertAllObligations(snapshot.goldObligations)
            goldDao.insertAllSettlements(snapshot.goldSettlements)
            goldDao.insertAllRateSnapshots(snapshot.goldRateSnapshots)
            goldDao.insertAllValuationSnapshots(snapshot.goldValuationSnapshots)

            stockDao.insertAllStocks(snapshot.stocks)
            stockDao.insertAllHoldings(snapshot.stockHoldings)
            stockDao.insertAllTransactions(snapshot.stockTransactions)
            stockDao.insertAllPriceSnapshots(snapshot.stockPriceSnapshots)
        }
        ensureSeeded()
        DatabaseSeeder.seedGoldCategoriesIfEmpty(db.goldDao())
    }
}

@kotlinx.serialization.Serializable
data class DatabaseSnapshot(
    val schemaVersion: Int = 2,
    val exportedAt: Long = System.currentTimeMillis(),
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val goals: List<SavingsGoalEntity> = emptyList(),
    val goalContributions: List<GoalContributionEntity> = emptyList(),
    val recurringPayments: List<RecurringPaymentEntity> = emptyList(),
    val debts: List<DebtEntity> = emptyList(),
    val debtRepayments: List<DebtRepaymentEntity> = emptyList(),
    val preferences: List<com.example.data.local.AppPreferenceEntity> = emptyList(),
    val goldCategories: List<GoldCategoryEntity> = emptyList(),
    val goldItems: List<GoldItemEntity> = emptyList(),
    val goldPurchases: List<GoldPurchaseEntity> = emptyList(),
    val goldPurchaseItems: List<GoldPurchaseItemEntity> = emptyList(),
    val goldSales: List<GoldSaleEntity> = emptyList(),
    val goldSaleItems: List<GoldSaleItemEntity> = emptyList(),
    val goldObligations: List<GoldObligationEntity> = emptyList(),
    val goldSettlements: List<GoldObligationSettlementEntity> = emptyList(),
    val goldRateSnapshots: List<GoldRateSnapshotEntity> = emptyList(),
    val goldValuationSnapshots: List<GoldValuationSnapshotEntity> = emptyList(),
    val stocks: List<StockEntity> = emptyList(),
    val stockHoldings: List<StockHoldingEntity> = emptyList(),
    val stockTransactions: List<StockTransactionEntity> = emptyList(),
    val stockPriceSnapshots: List<StockPriceSnapshotEntity> = emptyList(),
)

fun AccountEntity.toInput(): AccountInput = AccountInput(
    id = id,
    name = name,
    type = runCatching { AccountType.valueOf(type) }.getOrDefault(AccountType.CUSTOM),
    openingBalancePaise = openingBalancePaise,
    isArchived = isArchived,
)

fun TransactionEntity.toInput(): TransactionInput = TransactionInput(
    id = id,
    type = runCatching { TransactionType.valueOf(type) }.getOrDefault(TransactionType.EXPENSE),
    amountPaise = amountPaise,
    timestamp = timestamp,
    accountId = accountId,
    destinationAccountId = destinationAccountId,
    categoryId = categoryId,
    subcategoryId = subcategoryId,
    isDebtIncomingRepayment = isDebtIncomingRepayment,
)
