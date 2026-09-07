package com.expensetracker.app.presentation.util

import com.expensetracker.app.domain.model.Currency
import com.expensetracker.app.domain.model.Subscription
import java.text.NumberFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

/** Formats an amount like ₹8,450.00 using the user's currency. */
fun formatMoney(amount: Double, currency: Currency): String {
    val nf = NumberFormat.getNumberInstance(Locale.forLanguageTag(currency.localeTag))
    nf.minimumFractionDigits = 2
    nf.maximumFractionDigits = 2
    return currency.symbol + nf.format(amount)
}

/** Formats a whole amount like ₹8,450 (no decimals) - used for big dashboard numbers. */
fun formatMoneyCompact(amount: Double, currency: Currency): String {
    val nf = NumberFormat.getNumberInstance(Locale.forLanguageTag(currency.localeTag))
    nf.maximumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
    return currency.symbol + nf.format(amount)
}

/** Formats a percentage like 38%. */
fun formatPercent(fraction: Float): String = "${(fraction * 100).toInt()}%"

fun formatTime(time: LocalTime): String = time.format(timeFormat)

/** Seeds the amount field when editing: 39.0 shows as "39", 39.5 as "39.5". */
fun formatAmountForInput(amount: Double): String =
    if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()

fun formatAmountInput(raw: String): String {
    val cleaned = raw.filter { it.isDigit() || it == '.' }
    val parts = cleaned.split('.')
    val whole = parts.firstOrNull().orEmpty().take(7)
    val decimal = parts.getOrNull(1).orEmpty().take(2)
    return when {
        cleaned.contains('.') && parts.size > 1 -> "$whole.$decimal"
        else -> whole
    }
}

/** "₹119 / month" or "₹999 / year" style label for subscription cards. */
fun subscriptionRateLabel(subscription: Subscription, currency: Currency): String {
    val nf = NumberFormat.getNumberInstance(Locale.forLanguageTag(currency.localeTag))
    nf.maximumFractionDigits = if (subscription.amount % 1.0 == 0.0) 0 else 2
    val value = currency.symbol + nf.format(subscription.amount)
    return when (subscription.billingCycle) {
        com.expensetracker.app.domain.model.BillingCycle.MONTHLY -> "$value / month"
        com.expensetracker.app.domain.model.BillingCycle.YEARLY -> "$value / year"
    }
}
