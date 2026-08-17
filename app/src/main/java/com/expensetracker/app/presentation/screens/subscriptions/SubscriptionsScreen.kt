package com.expensetracker.app.presentation.screens.subscriptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ReceiptLong
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
import com.expensetracker.app.presentation.components.EmptyState
import com.expensetracker.app.presentation.components.GradientCard
import com.expensetracker.app.presentation.components.GradientFab
import com.expensetracker.app.presentation.components.StatCard
import com.expensetracker.app.presentation.components.SubscriptionCard
import com.expensetracker.app.presentation.util.formatMoney
import com.expensetracker.app.presentation.util.formatMoneyCompact
import com.expensetracker.app.presentation.util.subscriptionRateLabel

@Composable
fun SubscriptionsScreen(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: SubscriptionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Subscriptions",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(16.dp))

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
            Spacer(Modifier.height(14.dp))

            if (state.subscriptions.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.ReceiptLong,
                    title = "No subscriptions added",
                    subtitle = "Track recurring services like Spotify, Netflix or gym memberships.\nMonthly and yearly plans are kept separate from your other spending.",
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
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
        }

        GradientFab(
            onClick = onAdd,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 24.dp),
        )
    }
}
