package com.expensetracker.app.presentation.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.domain.model.CategoryBreakdown
import com.expensetracker.app.domain.model.ChartPoint
import com.expensetracker.app.domain.model.Currency
import com.expensetracker.app.domain.model.DayTotal
import com.expensetracker.app.domain.model.ExpenseEntry
import com.expensetracker.app.domain.model.ExpenseType
import com.expensetracker.app.domain.model.SubscriptionEntry
import com.expensetracker.app.domain.model.TimePeriod
import com.expensetracker.app.domain.repository.CategoryRepository
import com.expensetracker.app.domain.repository.ExpenseRepository
import com.expensetracker.app.domain.repository.SettingsRepository
import com.expensetracker.app.domain.repository.SubscriptionRepository
import com.expensetracker.app.domain.usecase.StatsCalculator
import com.expensetracker.app.presentation.screens.home.HomeSegment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

enum class AnalyticsPeriod(val label: String) {
    MONTH("Month"),
    YEAR("Year"),
}

data class AnalyticsUiState(
    val segment: HomeSegment = HomeSegment.REGULAR,
    val period: AnalyticsPeriod = AnalyticsPeriod.MONTH,
    // Spending (regular & occasional)
    val total: Double = 0.0,
    val dayCount: Int = 0,
    val dailyAverage: Double = 0.0,
    val highestDay: DayTotal? = null,
    val breakdown: List<CategoryBreakdown> = emptyList(),
    val barPoints: List<ChartPoint> = emptyList(),
    val barHighlightIndices: Set<Int> = emptySet(),
    val entries: List<ExpenseEntry> = emptyList(),
    // Subscriptions
    val subscriptions: List<SubscriptionEntry> = emptyList(),
    val monthlyCost: Double = 0.0,
    val yearlyCost: Double = 0.0,
    val currency: Currency = Currency.INR,
    val loading: Boolean = true,
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val subscriptionRepository: SubscriptionRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val segment = MutableStateFlow(HomeSegment.REGULAR)
    private val period = MutableStateFlow(AnalyticsPeriod.MONTH)

    fun selectSegment(value: HomeSegment) { segment.value = value }
    fun selectPeriod(value: AnalyticsPeriod) { period.value = value }

    private data class Data(
        val entries: List<ExpenseEntry>,
        val subscriptions: List<SubscriptionEntry>,
    )

    private val dataFlow = combine(
        expenseRepository.observeEntries(),
        subscriptionRepository.observeSubscriptions(),
    ) { entries, subscriptions -> Data(entries, subscriptions) }

    val uiState: StateFlow<AnalyticsUiState> = combine(
        dataFlow,
        settingsRepository.settings,
        segment,
        period,
    ) { data, settings, segment, period ->
        val now = LocalDate.now()
        val entries = data.entries
        val subscriptions = data.subscriptions
        val periodRange = when (period) {
            AnalyticsPeriod.MONTH -> TimePeriod.MONTH.range(now)
            AnalyticsPeriod.YEAR -> TimePeriod.YEAR.range(now)
        }

        if (segment == HomeSegment.SUBSCRIPTIONS) {
            AnalyticsUiState(
                segment = segment,
                period = period,
                subscriptions = subscriptions,
                monthlyCost = StatsCalculator.subscriptionMonthlyCost(subscriptions),
                yearlyCost = StatsCalculator.subscriptionYearlyCost(subscriptions),
                currency = Currency.fromCode(settings.currency),
                loading = false,
            )
        } else {
            val type = if (segment == HomeSegment.REGULAR) ExpenseType.REGULAR else ExpenseType.OCCASIONAL
            val periodEntries = entries.filter { it.expense.type == type && it.expense.date in periodRange }
            val total = StatsCalculator.total(periodEntries)
            val breakdown = StatsCalculator.categoryBreakdown(periodEntries)
            val barSeries = when (period) {
                AnalyticsPeriod.MONTH -> StatsCalculator.dailyTotals(periodEntries, periodRange)
                    .map { ChartPoint(it.date.dayOfMonth.toString(), it.total) }
                AnalyticsPeriod.YEAR -> StatsCalculator.monthlyTotals(periodEntries, periodRange)
                    .map {
                        ChartPoint(
                            it.date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                            it.total,
                        )
                    }
            }
            val dayCount = periodRange.start.datesUntil(periodRange.endInclusive.plusDays(1)).count().toInt()
            val maxValue = barSeries.maxOfOrNull { it.value } ?: 0.0
            val highlightIndices = if (period == AnalyticsPeriod.MONTH && maxValue > 0) {
                barSeries.mapIndexedNotNull { index, point ->
                    if (point.value == maxValue) index else null
                }.toSet()
            } else emptySet()

            AnalyticsUiState(
                segment = segment,
                period = period,
                total = total,
                dayCount = dayCount,
                dailyAverage = StatsCalculator.dailyAverage(total, dayCount.toLong()),
                highestDay = StatsCalculator.highestSpendingDay(periodEntries),
                breakdown = breakdown,
                barPoints = barSeries,
                barHighlightIndices = highlightIndices,
                entries = periodEntries,
                currency = Currency.fromCode(settings.currency),
                loading = false,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AnalyticsUiState(),
    )
}
