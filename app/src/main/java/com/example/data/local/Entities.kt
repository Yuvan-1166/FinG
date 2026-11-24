package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "accounts",
    indices = [Index(value = ["isArchived"])],
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val type: String,
    val openingBalancePaise: Long,
    val institutionName: String = "",
    val colorHex: String = "#10B981",
    val iconName: String = "account_balance",
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentCategoryId"],
            onDelete = ForeignKey.RESTRICT,
        )
    ],
    indices = [
        Index(value = ["parentCategoryId"]),
        Index(value = ["type"]),
    ],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val type: String, // "INCOME" or "EXPENSE"
    val parentCategoryId: Long? = null,
    val iconName: String = "category",
    val colorHex: String = "#10B981",
    val isDefault: Boolean = false,
    val isArchived: Boolean = false,
)

@Serializable
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["destinationAccountId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["subcategoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["accountId"]),
        Index(value = ["destinationAccountId"]),
        Index(value = ["categoryId"]),
        Index(value = ["subcategoryId"]),
        Index(value = ["type"]),
        Index(
            value = ["recurringPaymentId", "recurringOccurrenceEpochDay"],
            unique = true,
        ),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: String,
    val amountPaise: Long,
    val timestamp: Long,
    val accountId: Long,
    val destinationAccountId: Long? = null,
    val categoryId: Long? = null,
    val subcategoryId: Long? = null,
    val merchant: String = "",
    val description: String = "",
    val notes: String = "",
    val tags: String = "",
    val receiptUri: String? = null,
    val recurringPaymentId: Long? = null,
    val recurringOccurrenceEpochDay: Long? = null,
    val debtId: Long? = null,
    val isDebtIncomingRepayment: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        )
    ],
    indices = [Index(value = ["categoryId"])],
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val categoryId: Long? = null,
    val limitAmountPaise: Long,
    val periodType: String = "MONTHLY",
    val customStartEpochDay: Long? = null,
    val customEndEpochDay: Long? = null,
    val alertThresholdPercent: Int = 80,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val targetAmountPaise: Long,
    val targetDateEpochDay: Long? = null,
    val description: String = "",
    val iconName: String = "savings",
    val colorHex: String = "#10B981",
    val linkedAccountId: Long? = null,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "goal_contributions",
    foreignKeys = [
        ForeignKey(
            entity = SavingsGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["goalId"]), Index(value = ["timestamp"])],
)
data class GoalContributionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val goalId: Long,
    val amountPaise: Long, // positive = contribution, negative = withdrawal
    val sourceAccountId: Long? = null,
    val destinationAccountId: Long? = null,
    val linkedTransactionId: Long? = null,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "recurring_payments",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["categoryId"]),
        Index(value = ["nextDueDateEpochDay"]),
    ],
)
data class RecurringPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val type: String = "EXPENSE",
    val amountPaise: Long,
    val frequency: String = "MONTHLY",
    val startDateEpochDay: Long,
    val endDateEpochDay: Long? = null,
    val nextDueDateEpochDay: Long,
    val lastConfirmedEpochDay: Long? = null,
    val accountId: Long,
    val categoryId: Long? = null,
    val merchant: String = "",
    val notes: String = "",
    val reminderEnabled: Boolean = true,
    val reminderDaysBefore: Int = 2,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val personName: String,
    val direction: String, // "LENT" or "BORROWED"
    val principalAmountPaise: Long,
    val accountId: Long? = null,
    val initialTransactionId: Long? = null,
    val dueDateEpochDay: Long? = null,
    val notes: String = "",
    val isSettled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "debt_repayments",
    foreignKeys = [
        ForeignKey(
            entity = DebtEntity::class,
            parentColumns = ["id"],
            childColumns = ["debtId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["debtId"]), Index(value = ["timestamp"])],
)
data class DebtRepaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val debtId: Long,
    val amountPaise: Long,
    val accountId: Long? = null,
    val linkedTransactionId: Long? = null,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(tableName = "app_preferences")
data class AppPreferenceEntity(
    @PrimaryKey val prefKey: String,
    val prefValue: String,
    val updatedAt: Long = System.currentTimeMillis(),
)
