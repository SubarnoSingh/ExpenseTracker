package com.expensetracker.app.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.model.CategoryBreakdown
import com.expensetracker.app.domain.model.ChartPoint
import com.expensetracker.app.domain.model.Currency
import com.expensetracker.app.domain.model.ExpenseEntry
import com.expensetracker.app.domain.model.ExpenseType
import com.expensetracker.app.domain.model.SubscriptionEntry
import com.expensetracker.app.domain.model.TimePeriod
import com.expensetracker.app.domain.model.weekdayName
import com.expensetracker.app.domain.repository.CategoryRepository
import com.expensetracker.app.domain.repository.ExpenseRepository
import com.expensetracker.app.domain.repository.SettingsRepository
import com.expensetracker.app.domain.repository.SubscriptionRepository
import com.expensetracker.app.domain.usecase.StatsCalculator
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

enum class HomeSegment { REGULAR, OCCASIONAL, SUBSCRIPTIONS }

data class HomeUiState(
    val userName: String = "There",
    val currency: Currency = Currency.INR,
    val segment: HomeSegment = HomeSegment.REGULAR,
    val period: TimePeriod = TimePeriod.MONTH,
    // Regular
    val periodTotal: Double = 0.0,
    val chartPoints: List<ChartPoint> = emptyList(),
    val categoryBreakdown: List<CategoryBreakdown> = emptyList(),
    val todayEntries: List<ExpenseEntry> = emptyList(),
    val todayTotal: Double = 0.0,
    // Occasional
    val occasionalEntries: List<ExpenseEntry> = emptyList(),
    val occasionalTotal: Double = 0.0,
    // Subscriptions
    val subscriptions: List<SubscriptionEntry> = emptyList(),
    val subscriptionMonthlyCost: Double = 0.0,
    val categories: List<Category> = emptyList(),
    val loading: Boolean = true,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val subscriptionRepository: SubscriptionRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val segment = MutableStateFlow(HomeSegment.REGULAR)
    private val period = MutableStateFlow(TimePeriod.MONTH)

    fun selectSegment(value: HomeSegment) { segment.value = value }
    fun selectPeriod(value: TimePeriod) { period.value = value }

    private data class Data(
        val categories: List<Category>,
        val entries: List<ExpenseEntry>,
        val subscriptions: List<SubscriptionEntry>,
    )

    private val dataFlow = combine(
        categoryRepository.observeCategories(),
        expenseRepository.observeEntries(),
        subscriptionRepository.observeSubscriptions(),
    ) { categories, entries, subscriptions ->
        Data(categories, entries, subscriptions)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        dataFlow,
        settingsRepository.settings,
        segment,
        period,
    ) { data, settings, segment, period ->
        val now = LocalDate.now()
        val range = period.range(now)
        val entries = data.entries
        val regular = entries.filter { it.expense.type == ExpenseType.REGULAR }
        val occasional = entries.filter { it.expense.type == ExpenseType.OCCASIONAL }

        val periodRegular = regular.filter { it.expense.date in range }
        val periodOccasional = occasional.filter { it.expense.date in range }
        val today = regular.filter { it.expense.date == now }
        val subscriptions = data.subscriptions

        HomeUiState(
            userName = settings.userName,
            currency = Currency.fromCode(settings.currency),
            segment = segment,
            period = period,
            periodTotal = StatsCalculator.total(periodRegular),
            chartPoints = chartPointsFor(period, periodRegular, now),
            categoryBreakdown = StatsCalculator.categoryBreakdown(periodRegular),
            todayEntries = today,
            todayTotal = StatsCalculator.total(today),
            occasionalEntries = periodOccasional,
            occasionalTotal = StatsCalculator.total(periodOccasional),
            subscriptions = subscriptions,
            subscriptionMonthlyCost = StatsCalculator.subscriptionMonthlyCost(subscriptions),
            categories = data.categories,
            loading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    private fun chartPointsFor(
        period: TimePeriod,
        entries: List<ExpenseEntry>,
        now: LocalDate,
    ): List<ChartPoint> = when (period) {
        TimePeriod.TODAY -> StatsCalculator.hourlyTotals(entries, now)
        TimePeriod.WEEK -> StatsCalculator.dailyTotals(entries, period.range(now))
            .map { ChartPoint(it.date.weekdayName(), it.total) }
        TimePeriod.MONTH -> StatsCalculator.dailyTotals(entries, period.range(now))
            .map { ChartPoint(it.date.dayOfMonth.toString(), it.total) }
        TimePeriod.YEAR -> StatsCalculator.monthlyTotals(entries, period.range(now))
            .map {
                ChartPoint(
                    it.date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    it.total,
                )
            }
        TimePeriod.ALL -> emptyList()
    }
}
