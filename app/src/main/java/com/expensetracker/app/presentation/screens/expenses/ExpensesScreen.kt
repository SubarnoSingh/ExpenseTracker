package com.expensetracker.app.presentation.screens.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.model.DayTotal
import com.expensetracker.app.domain.model.ExpenseType
import com.expensetracker.app.domain.model.TimePeriod
import com.expensetracker.app.domain.model.dayLabel
import com.expensetracker.app.presentation.components.CategoryIcons
import com.expensetracker.app.presentation.components.EmptyState
import com.expensetracker.app.presentation.components.ExpenseRow
import com.expensetracker.app.presentation.components.GradientFab
import com.expensetracker.app.presentation.components.SegmentedControl
import com.expensetracker.app.presentation.components.SubscriptionCard
import com.expensetracker.app.presentation.theme.LocalAppColors
import com.expensetracker.app.presentation.util.formatMoney
import com.expensetracker.app.presentation.util.formatMoneyCompact
import com.expensetracker.app.presentation.util.subscriptionRateLabel
import java.time.LocalDate

@Composable
fun ExpensesScreen(
    onAddExpense: (ExpenseType?) -> Unit,
    onEditExpense: (Long) -> Unit,
    onAddSubscription: () -> Unit,
    onEditSubscription: (Long) -> Unit,
    viewModel: ExpensesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }
    val appColors = LocalAppColors.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(10.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Expenses",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                Box {
                    IconButton(onClick = { showFilters = true }) {
                        Icon(
                            imageVector = Icons.Rounded.FilterList,
                            contentDescription = "Filters",
                            tint = if (viewModel.filterActive.collectAsStateWithLifecycle().value)
                                appColors.gradientStart else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (viewModel.filterActive.collectAsStateWithLifecycle().value) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 6.dp, end = 6.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(appColors.gradientStart),
                        )
                    }
                }
            }

            // Search
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by description or category") },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setQuery("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )

            Spacer(Modifier.height(12.dp))

            SegmentedControl(
                options = ExpensesSegment.entries,
                selected = state.segment,
                onSelect = viewModel::selectSegment,
                label = { it.name.lowercase().replaceFirstChar(Char::uppercase) },
            )

            Spacer(Modifier.height(12.dp))

            when (state.segment) {
                ExpensesSegment.SUBSCRIPTIONS -> SubscriptionList(
                    state = state,
                    onAdd = onAddSubscription,
                    onEdit = onEditSubscription,
                )
                else -> ExpenseList(
                    state = state,
                    onEditExpense = onEditExpense,
                )
            }
        }

        GradientFab(
            onClick = {
                when (state.segment) {
                    ExpensesSegment.SUBSCRIPTIONS -> onAddSubscription()
                    ExpensesSegment.REGULAR -> onAddExpense(ExpenseType.REGULAR)
                    ExpensesSegment.OCCASIONAL -> onAddExpense(ExpenseType.OCCASIONAL)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 24.dp),
        )
    }

    if (showFilters) {
        FilterSheet(
            state = state,
            onDismiss = { showFilters = false },
            onSelectCategory = viewModel::setCategoryFilter,
            onSelectRange = viewModel::setDateRange,
            onReset = viewModel::resetFilters,
        )
    }
}

@Composable
private fun ExpenseList(
    state: ExpensesUiState,
    onEditExpense: (Long) -> Unit,
) {
    if (state.dateGroups.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.ReceiptLong,
            title = if (state.query.isNotEmpty() || state.filterCategoryId != null || state.dateRange != null)
                "No matches found"
            else "No expenses yet",
            subtitle = if (state.query.isNotEmpty() || state.filterCategoryId != null || state.dateRange != null)
                "Try a different search or clear the filters."
            else "Start tracking your spending to see where your money goes.",
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        state.dateGroups.forEach { group ->
            item(key = "header-${group.date}") {
                DateGroupHeader(
                    date = group.date,
                    total = formatMoneyCompact(group.total, state.currency),
                )
            }
            items(group.items, key = { it.expense.id }) { entry ->
                ExpenseRow(
                    entry = entry,
                    amountText = formatMoney(entry.expense.amount, state.currency),
                    onClick = { onEditExpense(entry.expense.id) },
                )
            }
        }
    }
}

@Composable
private fun DateGroupHeader(date: LocalDate, total: String) {
    val label = date.dayLabel()
    val dayNumber = date.format(com.expensetracker.app.domain.model.DAY_LABEL_FORMAT)
    val text = if (label == "Today" || label == "Yesterday") {
        "${label.uppercase()} — $dayNumber"
    } else {
        dayNumber.uppercase()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = total,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SubscriptionList(
    state: ExpensesUiState,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    if (state.subscriptions.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.ReceiptLong,
            title = "No subscriptions",
            subtitle = "Track recurring services like Spotify, Netflix or gym memberships.",
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(state.subscriptions, key = { it.subscription.id }) { entry ->
            SubscriptionCard(
                entry = entry,
                rateLabel = subscriptionRateLabel(entry.subscription, state.currency),
                onClick = { onEdit(entry.subscription.id) },
                showActiveState = true,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    state: ExpensesUiState,
    onDismiss: () -> Unit,
    onSelectCategory: (Long?) -> Unit,
    onSelectRange: (ClosedRange<LocalDate>?) -> Unit,
    onReset: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }
    var customFrom by remember { mutableStateOf<LocalDate?>(state.dateRange?.start) }
    var customTo by remember { mutableStateOf<LocalDate?>(state.dateRange?.endInclusive) }

    val presets = listOf(
        TimePeriod.TODAY to "Today",
        TimePeriod.WEEK to "This Week",
        TimePeriod.MONTH to "This Month",
        TimePeriod.YEAR to "This Year",
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Filters",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = "Date Range",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        label = "All Time",
                        selected = state.dateRange == null,
                        onClick = { onSelectRange(null) },
                    )
                }
                items(presets, key = { it.first }) { (period, label) ->
                    FilterChip(
                        label = label,
                        selected = state.dateRange == period.range(LocalDate.now()),
                        onClick = { onSelectRange(period.range(LocalDate.now())) },
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterDateField(
                    label = "From",
                    value = customFrom?.format(com.expensetracker.app.domain.model.DAY_LABEL_FORMAT) ?: "Any",
                    modifier = Modifier.weight(1f),
                    onClick = { showFromPicker = true },
                )
                FilterDateField(
                    label = "To",
                    value = customTo?.format(com.expensetracker.app.domain.model.DAY_LABEL_FORMAT) ?: "Any",
                    modifier = Modifier.weight(1f),
                    onClick = { showToPicker = true },
                )
            }

            Text(
                text = "Category",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        label = "All",
                        selected = state.filterCategoryId == null,
                        onClick = { onSelectCategory(null) },
                    )
                }
                items(state.categories, key = { it.id }) { category ->
                    FilterChip(
                        label = category.name,
                        iconColor = Color(category.color),
                        selected = state.filterCategoryId == category.id,
                        onClick = { onSelectCategory(category.id) },
                    )
                }
            }

            TextButton(onClick = onReset, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Reset Filters", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    fun applyRange() {
        val from = customFrom ?: return
        val to = customTo ?: return
        if (from <= to) onSelectRange(from..to)
    }

    if (showFromPicker) {
        val pickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis =
                (customFrom ?: LocalDate.now()).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showFromPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        customFrom = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneOffset.UTC).toLocalDate()
                    }
                    applyRange()
                    showFromPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showFromPicker = false }) { Text("Cancel") }
            },
        ) {
            androidx.compose.material3.DatePicker(state = pickerState)
        }
    }

    if (showToPicker) {
        val pickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis =
                (customTo ?: LocalDate.now()).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showToPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        customTo = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneOffset.UTC).toLocalDate()
                    }
                    applyRange()
                    showToPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showToPicker = false }) { Text("Cancel") }
            },
        ) {
            androidx.compose.material3.DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    iconColor: Color? = null,
) {
    val container = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    else MaterialTheme.colorScheme.surfaceContainerHigh
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (iconColor != null) {
            Icon(
                imageVector = CategoryIcons.iconFor("more_horiz"),
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FilterDateField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Start,
        )
    }
}
