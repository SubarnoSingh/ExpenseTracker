package com.expensetracker.app.presentation.screens.analytics

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.domain.model.ExpenseType
import com.expensetracker.app.domain.model.dayLabel
import com.expensetracker.app.presentation.components.AppCard
import com.expensetracker.app.presentation.components.BarChart
import com.expensetracker.app.presentation.components.CategoryProgressBar
import com.expensetracker.app.presentation.components.DonutChart
import com.expensetracker.app.presentation.components.DonutSegment
import com.expensetracker.app.presentation.components.EmptyState
import com.expensetracker.app.presentation.components.ExpenseRow
import com.expensetracker.app.presentation.components.GradientCard
import com.expensetracker.app.presentation.components.SectionHeader
import com.expensetracker.app.presentation.components.SegmentedControl
import com.expensetracker.app.presentation.components.StatCard
import com.expensetracker.app.presentation.components.SubscriptionCard
import com.expensetracker.app.presentation.screens.home.HomeSegment
import com.expensetracker.app.presentation.theme.LocalAppColors
import com.expensetracker.app.presentation.util.formatMoney
import com.expensetracker.app.presentation.util.formatMoneyCompact
import com.expensetracker.app.presentation.util.formatTime
import com.expensetracker.app.presentation.util.subscriptionRateLabel

@Composable
fun AnalyticsScreen(
    onEditExpense: (Long) -> Unit,
    onEditSubscription: (Long) -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Analytics",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        SegmentedControl(
            options = HomeSegment.entries,
            selected = state.segment,
            onSelect = viewModel::selectSegment,
            label = { segment ->
                when (segment) {
                    HomeSegment.REGULAR -> "Regular"
                    HomeSegment.OCCASIONAL -> "Occasional"
                    HomeSegment.SUBSCRIPTIONS -> "Subscriptions"
                }
            },
        )

        AnimatedContent(
            targetState = state.segment,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "analyticsSegment",
        ) { segment ->
            when (segment) {
                HomeSegment.SUBSCRIPTIONS -> SubscriptionAnalytics(
                    state = state,
                    onEdit = onEditSubscription,
                )
                else -> SpendingAnalytics(
                    state = state,
                    onSelectPeriod = viewModel::selectPeriod,
                    onEditExpense = onEditExpense,
                )
            }
        }

        // Clears the floating bottom bar.
        Spacer(Modifier.height(110.dp))
    }
}

@Composable
private fun SpendingAnalytics(
    state: AnalyticsUiState,
    onSelectPeriod: (AnalyticsPeriod) -> Unit,
    onEditExpense: (Long) -> Unit,
) {
    val appColors = LocalAppColors.current
    val title = if (state.segment == HomeSegment.REGULAR) "Regular Spending" else "Occasional Spending"

    // Period toggle
    SegmentedControl(
        options = AnalyticsPeriod.entries,
        selected = state.period,
        onSelect = onSelectPeriod,
        label = { it.label },
    )

    // Total hero
    GradientCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Total $title",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatMoneyCompact(state.total, state.currency),
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
            )
        }
    }

    if (state.total <= 0.0) {
        EmptyState(
            icon = Icons.Rounded.ReceiptLong,
            title = "No spending this ${state.period.label.lowercase()}",
            subtitle = "Add ${state.segment.name.lowercase()} expenses to see statistics here.",
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }

    // Stats row
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(
            title = "Daily Average",
            value = formatMoney(state.dailyAverage, state.currency),
            icon = Icons.Rounded.CalendarMonth,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            title = "Highest Day",
            value = state.highestDay?.let {
                formatMoney(it.total, state.currency)
            } ?: "—",
            icon = Icons.Rounded.ReceiptLong,
            modifier = Modifier.weight(1f),
        )
    }
    state.highestDay?.let { highest ->
        Text(
            text = "Highest spending day: ${highest.date.dayLabel()}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }

    // Category breakdown with donut
    SectionHeader("Category Breakdown")
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        ) {
            DonutChart(
                segments = state.breakdown.map {
                    DonutSegment(value = it.amount, color = Color(it.category.color))
                },
            ) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatMoneyCompact(state.total, state.currency),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            state.breakdown.forEach { breakdown ->
                CategoryProgressBar(
                    breakdown = breakdown,
                    amountText = formatMoneyCompact(breakdown.amount, state.currency),
                )
            }
        }
    }

    // Daily/weekly/monthly spending chart
    SectionHeader(
        title = if (state.period == AnalyticsPeriod.MONTH) "Daily Spending"
        else "Monthly Spending"
    )
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            BarChart(
                points = state.barPoints,
                highlightIndices = state.barHighlightIndices,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    // Recent transactions
    SectionHeader("Transactions")
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            state.entries.take(10).forEach { entry ->
                ExpenseRow(
                    entry = entry,
                    amountText = formatMoney(entry.expense.amount, state.currency),
                    subtitle = "${entry.category.name}  •  ${entry.expense.date.dayLabel()}  •  ${formatTime(entry.expense.time)}",
                    onClick = { onEditExpense(entry.expense.id) },
                )
            }
        }
    }
}

@Composable
private fun SubscriptionAnalytics(
    state: AnalyticsUiState,
    onEdit: (Long) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(
            title = "Monthly Cost",
            value = formatMoney(state.monthlyCost, state.currency),
            modifier = Modifier.weight(1f),
            valueSize = 20,
        )
        StatCard(
            title = "Yearly Estimate",
            value = formatMoney(state.yearlyCost, state.currency),
            modifier = Modifier.weight(1f),
            valueSize = 20,
        )
    }

    SectionHeader(
        title = "Active Subscriptions",
        trailing = "${state.subscriptions.count { it.subscription.isActive }} active",
    )
    if (state.subscriptions.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.ReceiptLong,
            title = "No subscriptions added",
            subtitle = "Add subscriptions to see how much recurring money is committed.",
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.subscriptions.forEach { entry ->
                SubscriptionCard(
                    entry = entry,
                    rateLabel = subscriptionRateLabel(entry.subscription, state.currency),
                    onClick = { onEdit(entry.subscription.id) },
                    showActiveState = true,
                )
            }
        }
    }
}
