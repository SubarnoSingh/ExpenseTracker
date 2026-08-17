package com.expensetracker.app.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Periods the user can switch between for totals, charts and breakdowns.
 * [range] always returns an inclusive [ClosedRange] of dates.
 */
enum class TimePeriod(val label: String) {
    TODAY("Today"),
    WEEK("This Week"),
    MONTH("This Month"),
    YEAR("This Year"),
    ALL("All Time");

    fun range(now: LocalDate): ClosedRange<LocalDate> = when (this) {
        TODAY -> now..now
        WEEK -> {
            val start = now.with(DayOfWeek.MONDAY)
            start..start.plusDays(6)
        }
        MONTH -> {
            val start = now.withDayOfMonth(1)
            start..start.plusMonths(1).minusDays(1)
        }
        YEAR -> {
            val start = now.withDayOfYear(1)
            start..start.plusYears(1).minusDays(1)
        }
        ALL -> LocalDate.of(1970, 1, 1)..LocalDate.of(9999, 12, 31)
    }
}

/** A single day's aggregate. */
data class DayTotal(
    val date: LocalDate,
    val total: Double,
)

/** One slice of a chart series. */
data class ChartPoint(
    val label: String,
    val value: Double,
)

/** A category's share of spending within a period. */
data class CategoryBreakdown(
    val category: Category,
    val amount: Double,
    val percentage: Float,
)

/** Expenses grouped by the day they happened on. */
data class DateGroup(
    val date: LocalDate,
    val total: Double,
    val items: List<ExpenseEntry>,
)

// ---------------------------------------------------------------------------
// Date helpers used across screens
// ---------------------------------------------------------------------------

val DAY_LABEL_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
val FULL_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

fun LocalDate.dayLabel(now: LocalDate = LocalDate.now()): String = when (this) {
    now -> "Today"
    now.minusDays(1) -> "Yesterday"
    else -> if (year == now.year) format(DAY_LABEL_FORMAT) else format(FULL_DATE_FORMAT)
}

fun LocalDate.weekdayName(): String =
    dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
