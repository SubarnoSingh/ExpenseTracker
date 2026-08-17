package com.expensetracker.app.domain.usecase

import com.expensetracker.app.domain.model.BillingCycle
import com.expensetracker.app.domain.model.CategoryBreakdown
import com.expensetracker.app.domain.model.ChartPoint
import com.expensetracker.app.domain.model.DateGroup
import com.expensetracker.app.domain.model.DayTotal
import com.expensetracker.app.domain.model.Expense
import com.expensetracker.app.domain.model.ExpenseEntry
import com.expensetracker.app.domain.model.SubscriptionEntry
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.IsoFields
import kotlin.math.roundToLong

/**
 * All statistics are derived from real stored data via these pure functions.
 * Regular, Occasional and Subscription figures are never mixed here.
 */
object StatsCalculator {

    private fun round2(value: Double): Double =
        (value * 100).roundToLong() / 100.0

    fun total(entries: List<ExpenseEntry>): Double =
        round2(entries.sumOf { it.expense.amount })

    fun totalOf(expenses: List<Expense>): Double =
        round2(expenses.sumOf { it.amount })

    /** Amount spent per category, sorted descending, with percentage of [total]. */
    fun categoryBreakdown(
        entries: List<ExpenseEntry>,
        total: Double = total(entries),
    ): List<CategoryBreakdown> {
        val byCategory = entries.groupBy { it.category.id }
        return byCategory.map { (_, items) ->
            val amount = round2(items.sumOf { it.expense.amount })
            CategoryBreakdown(
                category = items.first().category,
                amount = amount,
                percentage = if (total > 0) (amount / total).toFloat() else 0f,
            )
        }.sortedByDescending { it.amount }
    }

    fun dailyAverage(total: Double, dayCount: Long): Double =
        if (dayCount <= 0) 0.0 else round2(total / dayCount)

    fun highestSpendingDay(entries: List<ExpenseEntry>): DayTotal? {
        val byDay = entries.groupBy { it.expense.date }
        return byDay.map { (date, items) -> DayTotal(date, round2(items.sumOf { it.expense.amount })) }
            .maxByOrNull { it.total }
    }

    /** One [DayTotal] per day in [range], zero-filled so charts stay continuous. */
    fun dailyTotals(entries: List<ExpenseEntry>, range: ClosedRange<LocalDate>): List<DayTotal> {
        val totals = entries
            .filter { it.expense.date in range }
            .groupBy { it.expense.date }
            .mapValues { (_, items) -> round2(items.sumOf { it.expense.amount }) }
        val endExclusive = if (range.endInclusive == LocalDate.MAX) LocalDate.MAX
        else range.endInclusive.plusDays(1)
        return range.start.datesUntil(endExclusive).toList().map { day ->
            DayTotal(day, totals[day] ?: 0.0)
        }
    }

    /** Spending summed per ISO week within [range]; the label is the week's Monday. */
    fun weeklyTotals(entries: List<ExpenseEntry>, range: ClosedRange<LocalDate>): List<DayTotal> {
        val inRange = entries.filter { it.expense.date in range }
        val byWeek = inRange.groupBy {
            it.expense.date.with(java.time.DayOfWeek.MONDAY)
        }
        val start = range.start.with(java.time.DayOfWeek.MONDAY)
        val weeks = generateSequence(start) { it.plusWeeks(1) }
            .takeWhile { it <= range.endInclusive }
            .toList()
        return weeks.map { weekStart ->
            DayTotal(weekStart, round2(byWeek[weekStart]?.sumOf { it.expense.amount } ?: 0.0))
        }
    }

    /** Spending summed per calendar month within [range]. */
    fun monthlyTotals(entries: List<ExpenseEntry>, range: ClosedRange<LocalDate>): List<DayTotal> {
        val inRange = entries.filter { it.expense.date in range }
        val byMonth = inRange.groupBy { YearMonth.from(it.expense.date) }
        val start = YearMonth.from(range.start)
        val months = generateSequence(start) { it.plusMonths(1) }
            .takeWhile { it <= YearMonth.from(range.endInclusive) }
            .toList()
        return months.map { month ->
            DayTotal(month.atDay(1), round2(byMonth[month]?.sumOf { it.expense.amount } ?: 0.0))
        }
    }

    /** 24 hourly buckets for "today's spending pattern". */
    fun hourlyTotals(entries: List<ExpenseEntry>, date: LocalDate): List<ChartPoint> {
        val byHour = entries
            .filter { it.expense.date == date }
            .groupBy { it.expense.time.hour }
            .mapValues { (_, items) -> round2(items.sumOf { it.expense.amount }) }
        return (0 until 24).map { hour ->
            ChartPoint(
                label = when (hour) {
                    0 -> "12a"
                    6 -> "6a"
                    12 -> "12p"
                    18 -> "6p"
                    else -> ""
                },
                value = byHour[hour] ?: 0.0,
            )
        }
    }

    /** Expenses grouped by date, newest first. */
    fun groupByDate(entries: List<ExpenseEntry>): List<DateGroup> {
        val sorted = entries.sortedWith(
            compareByDescending<ExpenseEntry> { it.expense.date }
                .thenByDescending { it.expense.time }
        )
        return sorted.groupBy { it.expense.date }.map { (date, items) ->
            DateGroup(date = date, total = round2(items.sumOf { it.expense.amount }), items = items)
        }
    }

    /** Monthly committed cost: monthly subs once, yearly subs split over 12 months. */
    fun subscriptionMonthlyCost(entries: List<SubscriptionEntry>): Double {
        val active = entries.filter { it.subscription.isActive }
        return round2(active.sumOf {
            if (it.subscription.billingCycle == BillingCycle.MONTHLY) it.subscription.amount
            else it.subscription.amount / 12.0
        })
    }

    /** Yearly committed cost: monthly subs x12, yearly subs once. */
    fun subscriptionYearlyCost(entries: List<SubscriptionEntry>): Double {
        val active = entries.filter { it.subscription.isActive }
        return round2(active.sumOf {
            if (it.subscription.billingCycle == BillingCycle.MONTHLY) it.subscription.amount * 12.0
            else it.subscription.amount
        })
    }

    fun count(entries: List<ExpenseEntry>): Int = entries.size

    /** Convenience: ISO week number for grouping labels. */
    fun isoWeekOf(date: LocalDate): Int =
        date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
}
