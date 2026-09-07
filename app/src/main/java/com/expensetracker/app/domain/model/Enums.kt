package com.expensetracker.app.domain.model

/** The three spending types are kept strictly separate everywhere in the app. */
enum class ExpenseType {
    REGULAR,
    OCCASIONAL,
}

/** What a category is used for. Subscriptions have their own category space. */
enum class CategoryType {
    REGULAR,
    OCCASIONAL,
    SUBSCRIPTION,
}

enum class BillingCycle {
    MONTHLY,
    YEARLY,
}

/** Theme preference persisted in settings. */
enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM,
}

fun ExpenseType.toCategoryType(): CategoryType = when (this) {
    ExpenseType.REGULAR -> CategoryType.REGULAR
    ExpenseType.OCCASIONAL -> CategoryType.OCCASIONAL
}

/** Null for [CategoryType.SUBSCRIPTION] - subscriptions aren't expenses. */
fun CategoryType.toExpenseType(): ExpenseType? = when (this) {
    CategoryType.REGULAR -> ExpenseType.REGULAR
    CategoryType.OCCASIONAL -> ExpenseType.OCCASIONAL
    CategoryType.SUBSCRIPTION -> null
}
