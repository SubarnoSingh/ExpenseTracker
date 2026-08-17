package com.expensetracker.app.presentation.screens.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.domain.model.dayLabel
import com.expensetracker.app.presentation.components.CategoryBadge
import com.expensetracker.app.presentation.components.CategoryIcons
import com.expensetracker.app.presentation.components.EmptyState
import com.expensetracker.app.presentation.components.ExpenseRow
import com.expensetracker.app.presentation.components.GradientCard
import com.expensetracker.app.presentation.components.StatCard
import com.expensetracker.app.presentation.util.formatMoney
import com.expensetracker.app.presentation.util.formatMoneyCompact
import com.expensetracker.app.presentation.util.formatTime

@Composable
fun CategoryDetailScreen(
    onBack: () -> Unit,
    onEditExpense: (Long) -> Unit,
    viewModel: CategoryDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val category = state.category

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = category?.name ?: "Category",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp, end = 20.dp, bottom = 40.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (category != null) {
                item {
                    val categoryColor = Color(category.color)
                    GradientCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CategoryBadge(
                                    icon = CategoryIcons.iconFor(category.icon),
                                    color = categoryColor,
                                    size = 44,
                                )
                                Spacer(Modifier.size(14.dp))
                                Column {
                                    Text(
                                        text = "This Month",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White.copy(alpha = 0.7f),
                                    )
                                    Text(
                                        text = formatMoneyCompact(state.monthTotal, state.currency),
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = Color.White,
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(
                            title = "This Week",
                            value = formatMoney(state.weekTotal, state.currency),
                            modifier = Modifier.weight(1f),
                        )
                        StatCard(
                            title = "Today",
                            value = formatMoney(state.todayTotal, state.currency),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                item {
                    Text(
                        text = "Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }

                if (state.entries.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Rounded.ReceiptLong,
                            title = "No spending in this category yet",
                            subtitle = "Expenses tagged with ${category.name} will appear here.",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    items(state.entries, key = { it.expense.id }) { entry ->
                        ExpenseRow(
                            entry = entry,
                            amountText = formatMoney(entry.expense.amount, state.currency),
                            subtitle = "${entry.expense.date.dayLabel()}  •  ${formatTime(entry.expense.time)}",
                            onClick = { onEditExpense(entry.expense.id) },
                        )
                    }
                }
            }
        }
    }
}
