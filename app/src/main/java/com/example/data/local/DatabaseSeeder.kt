package com.example.data.local

object DatabaseSeeder {

    suspend fun seedInitialCategoriesIfEmpty(categoryDao: CategoryDao) {
        if (categoryDao.countAll() > 0) return

        val incomeCategories = listOf(
            CategoryEntity(name = "Salary", type = "INCOME", iconName = "work", colorHex = "#10B981", isDefault = true),
            CategoryEntity(name = "Freelance", type = "INCOME", iconName = "laptop", colorHex = "#06B6D4", isDefault = true),
            CategoryEntity(name = "Business", type = "INCOME", iconName = "storefront", colorHex = "#3B82F6", isDefault = true),
            CategoryEntity(name = "Interest", type = "INCOME", iconName = "percent", colorHex = "#14B8A6", isDefault = true),
            CategoryEntity(name = "Gifts", type = "INCOME", iconName = "card_giftcard", colorHex = "#8B5CF6", isDefault = true),
            CategoryEntity(name = "Other Income", type = "INCOME", iconName = "payments", colorHex = "#84CC16", isDefault = true),
        )

        val expenseCategories = listOf(
            CategoryEntity(name = "Food", type = "EXPENSE", iconName = "restaurant", colorHex = "#F97316", isDefault = true),
            CategoryEntity(name = "Groceries", type = "EXPENSE", iconName = "shopping_basket", colorHex = "#10B981", isDefault = true),
            CategoryEntity(name = "Transportation", type = "EXPENSE", iconName = "directions_car", colorHex = "#3B82F6", isDefault = true),
            CategoryEntity(name = "Shopping", type = "EXPENSE", iconName = "shopping_bag", colorHex = "#EC4899", isDefault = true),
            CategoryEntity(name = "Entertainment", type = "EXPENSE", iconName = "movie", colorHex = "#8B5CF6", isDefault = true),
            CategoryEntity(name = "Healthcare", type = "EXPENSE", iconName = "medical_services", colorHex = "#EF4444", isDefault = true),
            CategoryEntity(name = "Education", type = "EXPENSE", iconName = "school", colorHex = "#6366F1", isDefault = true),
            CategoryEntity(name = "Rent", type = "EXPENSE", iconName = "home", colorHex = "#06B6D4", isDefault = true),
            CategoryEntity(name = "Utilities", type = "EXPENSE", iconName = "bolt", colorHex = "#F59E0B", isDefault = true),
            CategoryEntity(name = "Travel", type = "EXPENSE", iconName = "flight", colorHex = "#14B8A6", isDefault = true),
            CategoryEntity(name = "Personal Care", type = "EXPENSE", iconName = "spa", colorHex = "#EC4899", isDefault = true),
            CategoryEntity(name = "Subscriptions", type = "EXPENSE", iconName = "subscriptions", colorHex = "#6366F1", isDefault = true),
            CategoryEntity(name = "EMI", type = "EXPENSE", iconName = "credit_score", colorHex = "#EF4444", isDefault = true),
            CategoryEntity(name = "Other Expenses", type = "EXPENSE", iconName = "receipt_long", colorHex = "#64748B", isDefault = true),
        )

        categoryDao.insertAll(incomeCategories + expenseCategories)
    }

    suspend fun seedGoldCategoriesIfEmpty(goldDao: GoldDao) {
        if (goldDao.countCategories() > 0) return

        val predefined = listOf(
            GoldCategoryEntity(name = "Ornaments", iconName = "ornaments", isPredefined = true),
            GoldCategoryEntity(name = "Rings", iconName = "ring", isPredefined = true),
            GoldCategoryEntity(name = "Chains", iconName = "chain", isPredefined = true),
            GoldCategoryEntity(name = "Necklaces", iconName = "necklace", isPredefined = true),
            GoldCategoryEntity(name = "Bangles", iconName = "bangle", isPredefined = true),
            GoldCategoryEntity(name = "Earrings", iconName = "earring", isPredefined = true),
            GoldCategoryEntity(name = "Coins", iconName = "coin", isPredefined = true),
            GoldCategoryEntity(name = "Gold Bars", iconName = "bar", isPredefined = true),
            GoldCategoryEntity(name = "Bullion", iconName = "bullion", isPredefined = true),
            GoldCategoryEntity(name = "Other", iconName = "other", isPredefined = true),
        )
        goldDao.insertAllCategories(predefined)
    }
}
