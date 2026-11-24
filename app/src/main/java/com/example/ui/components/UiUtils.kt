package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.CreditScore
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingBasket
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.domain.model.AccountType
import com.example.domain.model.DateFormatPreference
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun parseHexColor(hex: String, fallback: Color = Color(0xFF10B981)): Color {
    return runCatching {
        val clean = hex.removePrefix("#")
        val value = clean.toLong(16)
        when (clean.length) {
            6 -> Color(0xFF000000L or value)
            8 -> Color(value)
            else -> fallback
        }
    }.getOrDefault(fallback)
}

fun formatDateMillis(epochMillis: Long, pref: DateFormatPreference): String {
    val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
    return dt.format(DateTimeFormatter.ofPattern(pref.pattern))
}

fun formatDateTimeMillis(epochMillis: Long, pref: DateFormatPreference): String {
    val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
    return dt.format(DateTimeFormatter.ofPattern("${pref.pattern}, hh:mm a"))
}

fun formatEpochDay(epochDay: Long, pref: DateFormatPreference): String {
    val d = LocalDate.ofEpochDay(epochDay)
    return d.format(DateTimeFormatter.ofPattern(pref.pattern))
}

object CategoryIconMapper {
    val selectableIcons = listOf(
        "restaurant" to "Food",
        "shopping_basket" to "Groceries",
        "directions_car" to "Transit",
        "shopping_bag" to "Shopping",
        "movie" to "Leisure",
        "medical_services" to "Health",
        "school" to "Education",
        "home" to "Home",
        "bolt" to "Bills",
        "flight" to "Travel",
        "spa" to "Care",
        "subscriptions" to "Subs",
        "credit_score" to "EMI",
        "work" to "Salary",
        "laptop" to "Freelance",
        "storefront" to "Business",
        "percent" to "Interest",
        "card_giftcard" to "Gifts",
        "payments" to "Income",
        "savings" to "Savings",
        "receipt_long" to "Other",
    )

    fun iconFor(key: String?): ImageVector = when (key) {
        "work" -> Icons.Rounded.Work
        "laptop" -> Icons.Rounded.Laptop
        "storefront" -> Icons.Rounded.Storefront
        "percent" -> Icons.Rounded.Percent
        "card_giftcard" -> Icons.Rounded.CardGiftcard
        "payments" -> Icons.Rounded.Payments
        "restaurant" -> Icons.Rounded.Restaurant
        "shopping_basket" -> Icons.Rounded.ShoppingBasket
        "directions_car" -> Icons.Rounded.DirectionsCar
        "shopping_bag" -> Icons.Rounded.ShoppingBag
        "movie" -> Icons.Rounded.Movie
        "medical_services" -> Icons.Rounded.MedicalServices
        "school" -> Icons.Rounded.School
        "home" -> Icons.Rounded.Home
        "bolt" -> Icons.Rounded.Bolt
        "flight" -> Icons.Rounded.Flight
        "spa" -> Icons.Rounded.Spa
        "subscriptions" -> Icons.Rounded.Subscriptions
        "credit_score" -> Icons.Rounded.CreditScore
        "receipt_long" -> Icons.Rounded.ReceiptLong
        "savings" -> Icons.Rounded.Savings
        "account_balance" -> Icons.Rounded.AccountBalance
        "wallet" -> Icons.Rounded.AccountBalanceWallet
        "upi" -> Icons.Rounded.QrCode2
        "credit_card" -> Icons.Rounded.CreditCard
        "transfer" -> Icons.Rounded.SwapHoriz
        else -> Icons.Rounded.Category
    }

    fun iconForAccountType(type: AccountType): ImageVector = when (type) {
        AccountType.SAVINGS, AccountType.CURRENT -> Icons.Rounded.AccountBalance
        AccountType.CASH -> Icons.Rounded.AccountBalanceWallet
        AccountType.UPI -> Icons.Rounded.QrCode2
        AccountType.CREDIT_CARD -> Icons.Rounded.CreditCard
        AccountType.CUSTOM -> Icons.Rounded.Savings
    }
}
