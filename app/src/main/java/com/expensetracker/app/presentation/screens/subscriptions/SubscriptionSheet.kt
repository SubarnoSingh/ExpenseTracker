package com.expensetracker.app.presentation.screens.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.domain.model.BillingCycle
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.presentation.components.CategoryIcons
import com.expensetracker.app.presentation.components.GradientButton
import com.expensetracker.app.presentation.components.SegmentedControl
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val sheetDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionSheetContent(
    viewModel: SubscriptionSheetViewModel,
    onDone: () -> Unit,
) {
    val name by viewModel.name.collectAsStateWithLifecycle()
    val amount by viewModel.amount.collectAsStateWithLifecycle()
    val cycle by viewModel.billingCycle.collectAsStateWithLifecycle()
    val startDate by viewModel.startDate.collectAsStateWithLifecycle()
    val nextBillingDate by viewModel.nextBillingDate.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val note by viewModel.note.collectAsStateWithLifecycle()
    val isActive by viewModel.isActive.collectAsStateWithLifecycle()
    val editingId by viewModel.editingId.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()

    var showStartPicker by remember { mutableStateOf(false) }
    var showNextPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = if (editingId != null) "Edit Subscription" else "Add Subscription",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp),
        )

        OutlinedTextField(
            value = name,
            onValueChange = viewModel::setName,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Subscription name") },
            placeholder = { Text("Spotify, Netflix, Gym…") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
        )

        // Amount
        val shape = RoundedCornerShape(20.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            OutlinedTextField(
                value = amount,
                onValueChange = viewModel::setAmount,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "0.00",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                },
                textStyle = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                prefix = {
                    Text(
                        text = currency.symbol,
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }

        // Billing cycle
        Text(
            text = "Billing Frequency",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SegmentedControl(
            options = BillingCycle.entries,
            selected = cycle,
            onSelect = viewModel::setCycle,
            label = { it.name.lowercase().replaceFirstChar(Char::uppercase) },
        )

        // Category
        Text(
            text = "Category",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val categoryRowState = rememberLazyListState()
        LaunchedEffect(selectedCategoryId, categories) {
            val index = categories.indexOfFirst { it.id == selectedCategoryId }
            if (index >= 0) categoryRowState.animateScrollToItem(index)
        }
        LazyRow(
            state = categoryRowState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(categories, key = { it.id }) { category ->
                SubscriptionCategoryChip(
                    category = category,
                    selected = category.id == selectedCategoryId,
                    onClick = { viewModel.setCategory(category.id) },
                )
            }
        }

        // Dates
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SubscriptionDateField(
                label = "Start Date",
                value = startDate.format(sheetDateFormatter),
                modifier = Modifier.weight(1f),
                onClick = { showStartPicker = true },
            )
            SubscriptionDateField(
                label = "Next Billing",
                value = nextBillingDate.format(sheetDateFormatter),
                modifier = Modifier.weight(1f),
                onClick = { showNextPicker = true },
            )
        }

        OutlinedTextField(
            value = note,
            onValueChange = viewModel::setNote,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Note (optional)") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
        )

        if (editingId != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Active",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Paused subscriptions don't count toward totals",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = isActive,
                    onCheckedChange = viewModel::setActive,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    ),
                )
            }
        }

        errorMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        GradientButton(
            text = if (editingId != null) "Update Subscription" else "Save Subscription",
            onClick = { viewModel.save(onDone) },
            enabled = amount.isNotBlank(),
        )

        if (editingId != null) {
            TextButton(
                onClick = { viewModel.delete(onDone) },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text("Delete Subscription", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showStartPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        viewModel.setStartDate(
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        )
                    }
                    showStartPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showNextPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = nextBillingDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showNextPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        viewModel.setNextBillingDate(
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        )
                    }
                    showNextPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showNextPicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun SubscriptionCategoryChip(
    category: Category,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = Color(category.color)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) color.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = CategoryIcons.iconFor(category.icon),
            contentDescription = null,
            tint = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SubscriptionDateField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}
