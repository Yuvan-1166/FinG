package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object ActivityRoute

@Serializable
data object GoldDashboardRoute

@Serializable
data object MoreRoute

@Serializable
data object InsightsRoute

@Serializable
data object PlanningRoute

@Serializable
data class TransactionEntryRoute(
    val transactionId: Long = -1L,
    val duplicateFromId: Long = -1L,
    val initialType: String = "",
    val initialAccountId: Long = -1L,
)

@Serializable
data class TransactionDetailRoute(val transactionId: Long)

@Serializable
data object AccountsRoute

@Serializable
data class AccountDetailRoute(val accountId: Long)

@Serializable
data object CategoriesRoute

@Serializable
data object BudgetsRoute

@Serializable
data object GoalsRoute

@Serializable
data class GoalDetailRoute(val goalId: Long)

@Serializable
data object RecurringRoute

@Serializable
data object DebtsRoute

@Serializable
data class DebtDetailRoute(val debtId: Long)

@Serializable
data object SettingsRoute

// Gold Treasury Routes
@Serializable
data object GoldInventoryRoute

@Serializable
data class GoldItemDetailRoute(val itemId: Long)

@Serializable
data object BuyGoldRoute

@Serializable
data class SellGoldRoute(val preselectedItemId: Long = -1L)

@Serializable
data object GoldObligationsRoute

@Serializable
data object GoldRatesRoute

@Serializable
data object GoldAnalyticsRoute

// Stocks Routes
@Serializable
data object StocksRoute

@Serializable
data class StockDetailRoute(val stockId: Long)
