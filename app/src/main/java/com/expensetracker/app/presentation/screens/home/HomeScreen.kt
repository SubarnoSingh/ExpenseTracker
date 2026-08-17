package com.expensetracker.app.presentation.screens.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.domain.model.ExpenseType
import com.expensetracker.app.domain.model.TimePeriod
import com.expensetracker.app.presentation.components.AppCard
import com.expensetracker.app.presentation.components.CategoryProgressBar
import com.expensetracker.app.presentation.components.EmptyState
import com.expensetracker.app.presentation.components.ExpenseRow
import com.expensetracker.app.presentation.components.GradientButton
import com.expensetracker.app.presentation.components.GradientCard
import com.expensetracker.app.presentation.components.SectionHeader
import com.expensetracker.app.presentation.components.SegmentedControl
import com.expensetracker.app.presentation.components.SpendingChart
import com.expensetracker.app.presentation.components.SubscriptionCard
import com.expensetracker.app.presentation.theme.LocalAppColors
import com.expensetracker.app.presentation.util.formatMoney
import com.expensetracker.app.presentation.util.formatMoneyCompact
import com.expensetracker.app.presentation.util.formatTime
import com.expensetracker.app.presentation.util.subscriptionRateLabel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val headerDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMM")

@Composable
fun HomeScreen(
    onOpenAddExpense: (ExpenseType?) -> Unit,
    onOpenSubscriptionAdd: () -> Unit,
    onOpenExpenses: () -> Unit,
    onOpenCategory: (Long) -> Unit,
    onEditExpense: (Long) -> Unit,
    onEditSubscription: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
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
        HomeHeader(userName = state.userName)

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
            label = "homeSegment",
        ) { segment ->
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (segment) {
                    HomeSegment.REGULAR -> RegularView(
                        state = state,
                        onSelectPeriod = viewModel::selectPeriod,
                        onOpenCategory = onOpenCategory,
                        onEditExpense = onEditExpense,
                        onViewAll = onOpenExpenses,
                    )
                    HomeSegment.OCCASIONAL -> OccasionalView(
                        state = state,
                        onSelectPeriod = viewModel::selectPeriod,
                        onEditExpense = onEditExpense,
                        onAddOccasional = { onOpenAddExpense(ExpenseType.OCCASIONAL) },
                    )
                    HomeSegment.SUBSCRIPTIONS -> SubscriptionsHomeView(
                        state = state,
                        onAddSubscription = onOpenSubscriptionAdd,
                        onEditSubscription = onEditSubscription,
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun HomeHeader(userName: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Hello,",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = userName,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = LocalDate.now().format(headerDateFormatter),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Avatar with the user's initial
        val appColors = LocalAppColors.current
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(appColors.gradientStart, appColors.gradientEnd))
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = userName.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun RegularView(
    state: HomeUiState,
    onSelectPeriod: (TimePeriod) -> Unit,
    onOpenCategory: (Long) -> Unit,
    onEditExpense: (Long) -> Unit,
    onViewAll: () -> Unit,
) {
    // Period selector
    SegmentedControl(
        options = listOf(TimePeriod.TODAY, TimePeriod.WEEK, TimePeriod.MONTH, TimePeriod.YEAR),
        selected = state.period,
        onSelect = onSelectPeriod,
        label = { it.label.removePrefix("This ").ifEmpty { "Today" } },
    )

    // Main spending card
    GradientCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Spent ${state.period.label.lowercase()}",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatMoneyCompact(state.periodTotal, state.currency),
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
            )
            Spacer(Modifier.height(18.dp))
            SpendingChart(
                points = state.chartPoints,
                accent = Color(0xFFC4B5FD),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    // Category summary
    SectionHeader("Category Summary")
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            if (state.categoryBreakdown.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.ReceiptLong,
                    title = "No spending yet",
                    subtitle = "Add an expense to see where your money goes ${state.period.label.lowercase()}.",
                )
            } else {
                state.categoryBreakdown.forEach { breakdown ->
                    CategoryProgressBar(
                        breakdown = breakdown,
                        amountText = formatMoneyCompact(breakdown.amount, state.currency),
                        onClick = { onOpenCategory(breakdown.category.id) },
                    )
                }
            }
        }
    }

    // Today's expenses
    SectionHeader(
        title = "Today's Expenses",
        trailing = formatMoneyCompact(state.todayTotal, state.currency),
    )
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            if (state.todayEntries.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.ReceiptLong,
                    title = "Nothing spent today",
                    subtitle = "Your purchases today will show up here.",
                )
            } else {
                state.todayEntries.take(5).forEach { entry ->
                    ExpenseRow(
                        entry = entry,
                        amountText = formatMoney(entry.expense.amount, state.currency),
                        onClick = { onEditExpense(entry.expense.id) },
                    )
                }
                if (state.todayEntries.size > 5) {
                    TextButton(
                        onClick = onViewAll,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text("View All (${state.todayEntries.size})")
                    }
                }
            }
        }
    }
}

@Composable
private fun OccasionalView(
    state: HomeUiState,
    onSelectPeriod: (TimePeriod) -> Unit,
    onEditExpense: (Long) -> Unit,
    onAddOccasional: () -> Unit,
) {
    SegmentedControl(
        options = listOf(TimePeriod.TODAY, TimePeriod.WEEK, TimePeriod.MONTH, TimePeriod.YEAR),
        selected = state.period,
        onSelect = onSelectPeriod,
        label = { it.label.removePrefix("This ").ifEmpty { "Today" } },
    )

    GradientCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Occasional Spending · ${state.period.label}",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatMoneyCompact(state.occasionalTotal, state.currency),
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "One-time purchases, kept separate from regular spending.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
            )
        }
    }

    if (state.occasionalEntries.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.Add,
            title = "No occasional expenses",
            subtitle = "Clothes, electronics, gifts and other one-time purchases stay here — never mixed into your regular totals.",
            action = {
                GradientButton(text = "Add Occasional Expense", onClick = onAddOccasional)
            },
        )
    } else {
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                state.occasionalEntries.take(8).forEach { entry ->
                    ExpenseRow(
                        entry = entry,
                        amountText = formatMoney(entry.expense.amount, state.currency),
                        subtitle = "${entry.category.name}  •  ${entry.expense.date.format(com.expensetracker.app.domain.model.DAY_LABEL_FORMAT)}  •  ${formatTime(entry.expense.time)}",
                        onClick = { onEditExpense(entry.expense.id) },
                    )
                }
            }
        }
        GradientButton(text = "Add Occasional Expense", onClick = onAddOccasional)
    }
}

@Composable
private fun SubscriptionsHomeView(
    state: HomeUiState,
    onAddSubscription: () -> Unit,
    onEditSubscription: (Long) -> Unit,
) {
    GradientCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Subscriptions · Monthly Cost",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatMoneyCompact(state.subscriptionMonthlyCost, state.currency),
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Recurring payments, kept separate from regular spending.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
            )
        }
    }

    if (state.subscriptions.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.Add,
            title = "No subscriptions added",
            subtitle = "Track recurring services like Spotify, Netflix or gym memberships.",
            action = {
                GradientButton(text = "Add Subscription", onClick = onAddSubscription)
            },
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.subscriptions.forEach { entry ->
                SubscriptionCard(
                    entry = entry,
                    rateLabel = subscriptionRateLabel(entry.subscription, state.currency),
                    onClick = { onEditSubscription(entry.subscription.id) },
                )
            }
        }
        GradientButton(text = "Add Subscription", onClick = onAddSubscription)
    }
}
