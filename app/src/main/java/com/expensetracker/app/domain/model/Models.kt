package com.expensetracker.app.domain.model

import java.time.LocalDate
import java.time.LocalTime

data class Expense(
    val id: Long = 0,
    val amount: Double,
    val description: String,
    val categoryId: Long,
    val type: ExpenseType,
    val date: LocalDate,
    val time: LocalTime,
    val note: String? = null,
)

/** An expense joined with its category - what most screens actually display. */
data class ExpenseEntry(
    val expense: Expense,
    val category: Category,
)

data class Category(
    val id: Long = 0,
    val name: String,
    val icon: String,
    val color: Long,
    val type: CategoryType,
)

data class Subscription(
    val id: Long = 0,
    val name: String,
    val amount: Double,
    val categoryId: Long,
    val billingCycle: BillingCycle,
    val startDate: LocalDate,
    val nextBillingDate: LocalDate,
    val note: String? = null,
    val isActive: Boolean = true,
)

/** A subscription joined with its category for display. */
data class SubscriptionEntry(
    val subscription: Subscription,
    val category: Category?,
)

data class AppSettings(
    val userName: String,
    val currency: String,
    val themeMode: ThemeMode,
)

/** Supported currencies. `code` matches java.util.Currency codes. */
enum class Currency(
    val code: String,
    val symbol: String,
    val localeTag: String,
) {
    INR("INR", "₹", "en-IN"),
    USD("USD", "$", "en-US"),
    EUR("EUR", "€", "en-IE"),
    GBP("GBP", "£", "en-GB"),
    JPY("JPY", "¥", "ja-JP"),
    AUD("AUD", "A$", "en-AU"),
    AED("AED", "AED ", "en-AE");

    companion object {
        fun fromCode(code: String): Currency =
            entries.firstOrNull { it.code == code } ?: INR
    }
}
