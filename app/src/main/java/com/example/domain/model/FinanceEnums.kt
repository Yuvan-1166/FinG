package com.example.domain.model

enum class AccountType(val label: String, val isLiabilityByDefault: Boolean = false) {
    SAVINGS("Savings Account"),
    CURRENT("Current Account"),
    CASH("Cash"),
    UPI("UPI Linked"),
    CREDIT_CARD("Credit Card", isLiabilityByDefault = true),
    CUSTOM("Custom Account"),
}

enum class TransactionType(val label: String) {
    INCOME("Income"),
    EXPENSE("Expense"),
    TRANSFER("Transfer"),
    REFUND("Refund"),
    MONEY_LENT("Money Lent"),
    MONEY_BORROWED("Money Borrowed"),
    DEBT_REPAYMENT("Debt Repayment"),
    ASSET_PURCHASE("Asset Purchase"),
    ASSET_SALE("Asset Sale"),
}

enum class RecurrenceFrequency(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly"),
}

enum class BudgetPeriod(val label: String) {
    MONTHLY("Monthly"),
    WEEKLY("Weekly"),
    CUSTOM("Custom"),
}

enum class BudgetStatus {
    NORMAL,
    APPROACHING,
    EXCEEDED,
}

enum class DebtDirection(val label: String) {
    LENT("Money Lent (Receivable)"),
    BORROWED("Money Borrowed (Payable)"),
}

enum class ThemeMode(val label: String) {
    SYSTEM("Follow System"),
    DARK("Dark"),
    LIGHT("Light"),
}

enum class CurrencyDisplayMode(val label: String) {
    STANDARD_INR("Indian Standard (₹1,23,456.00)"),
    COMPACT_INR("Indian Compact (₹1.23L)"),
}

enum class DateFormatPreference(val pattern: String, val label: String) {
    DD_MMM_YYYY("dd MMM yyyy", "24 Nov 2026"),
    DD_MM_YYYY("dd/MM/yyyy", "24/11/2026"),
    YYYY_MM_DD("yyyy-MM-dd", "2026-11-24"),
}

enum class DateRangePreset(val label: String) {
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    PREVIOUS_MONTH("Previous Month"),
    LAST_THREE_MONTHS("Last 3 Months"),
    THIS_YEAR("This Year"),
    CUSTOM("Custom"),
}

enum class TransactionSortOrder(val label: String) {
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    AMOUNT_DESC("Highest Amount"),
    AMOUNT_ASC("Lowest Amount"),
}

data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val currencyDisplayMode: CurrencyDisplayMode = CurrencyDisplayMode.STANDARD_INR,
    val showPaiseDecimals: Boolean = true,
    val dateFormat: DateFormatPreference = DateFormatPreference.DD_MMM_YYYY,
    val notificationsEnabled: Boolean = true,
    val budgetAlertsEnabled: Boolean = true,
    val appLockEnabled: Boolean = false,
)
