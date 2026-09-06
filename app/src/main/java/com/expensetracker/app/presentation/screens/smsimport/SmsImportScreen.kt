package com.expensetracker.app.presentation.screens.smsimport

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.model.CategoryType
import com.expensetracker.app.presentation.components.AppCard
import com.expensetracker.app.presentation.components.CategoryIcons
import com.expensetracker.app.presentation.components.EmptyState
import com.expensetracker.app.presentation.components.GradientButton
import com.expensetracker.app.presentation.util.formatMoney
import com.expensetracker.app.presentation.util.formatTime
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val dayFormat = DateTimeFormatter.ofPattern("d MMM")
private val fullDayFormat = DateTimeFormatter.ofPattern("d MMM yyyy")

private val sectionOrder = listOf(
    CategoryType.REGULAR to "Regular",
    CategoryType.OCCASIONAL to "Occasional",
    CategoryType.SUBSCRIPTION to "Subscriptions",
)

@Composable
fun SmsImportScreen(
    onBack: () -> Unit,
    viewModel: SmsImportViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val scanFrom by viewModel.scanFrom.collectAsStateWithLifecycle()

    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var denied by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var editingRow by remember { mutableStateOf<SmsImportRow?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { result ->
        granted = result
        denied = !result
    }

    // Scan as soon as we are allowed to, so the screen is never a dead end.
    LaunchedEffect(granted) {
        if (granted) viewModel.prepare()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Import from SMS",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(8.dp))

        val state = scanState
        when {
            !granted -> PermissionPrompt(
                denied = denied,
                onGrant = { permissionLauncher.launch(Manifest.permission.READ_SMS) },
                onOpenSettings = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        )
                    )
                },
            )

            state is ScanState.Done -> ImportedConfirmation(
                expenses = state.expenses,
                subscriptions = state.subscriptions,
                onScanAgain = {
                    viewModel.clearResult()
                    viewModel.scan()
                },
                onDone = onBack,
            )

            else -> Column(modifier = Modifier.fillMaxSize()) {
                ScanRangeRow(
                    from = scanFrom,
                    onClick = { showDatePicker = true },
                )
                Spacer(Modifier.height(12.dp))

                when {
                    state is ScanState.Scanning -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }

                    rows.isEmpty() -> EmptyState(
                        icon = Icons.Rounded.Sms,
                        title = "No spending messages found",
                        subtitle = "Nothing new in this range. Pick an earlier date to look further back.",
                        modifier = Modifier.fillMaxWidth(),
                    )

                    else -> ReviewList(
                        rows = rows,
                        amountLabel = { amount -> formatMoney(amount, currency) },
                        onToggle = viewModel::toggle,
                        onSelectAll = viewModel::setAllSelected,
                        onEditCategory = { editingRow = it },
                        onImport = viewModel::importSelected,
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        ScanFromDatePicker(
            current = scanFrom,
            onDismiss = { showDatePicker = false },
            onPick = {
                viewModel.setScanFrom(it)
                showDatePicker = false
            },
        )
    }

    editingRow?.let { row ->
        CategoryPickerSheet(
            categories = categories,
            selectedId = row.category?.id,
            onPick = {
                viewModel.setRowCategory(row.sms.smsId, it)
                editingRow = null
            },
            onDismiss = { editingRow = null },
        )
    }
}

@Composable
private fun ScanRangeRow(from: LocalDate, onClick: () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Scanning from ${from.format(fullDayFormat)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Tap to look further back",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScanFromDatePicker(
    current: LocalDate,
    onDismiss: () -> Unit,
    onPick: (LocalDate) -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = current.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { millis ->
                    onPick(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                }
            }) { Text("Scan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = state, title = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPickerSheet(
    categories: List<Category>,
    selectedId: Long?,
    onPick: (Category) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            sectionOrder.forEach { (type, label) ->
                val ofType = categories.filter { it.type == type }
                if (ofType.isEmpty()) return@forEach
                item(key = "header_$type") {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 10.dp),
                    )
                }
                items(ofType, key = { it.id }) { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onPick(category) }
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = CategoryIcons.iconFor(category.icon),
                            contentDescription = null,
                            tint = Color(category.color),
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (category.id == selectedId) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionPrompt(
    denied: Boolean,
    onGrant: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Read your bank SMS",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Expense Tracker scans your inbox for bank, card and UPI debit alerts " +
                        "and turns them into entries you approve before they are saved.\n\n" +
                        "Messages are read on this device only. Nothing is uploaded, and nothing " +
                        "is saved until you tap Import.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (denied) {
            Text(
                text = "Permission denied. Enable SMS access in system settings to use this.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            TextButton(onClick = onOpenSettings) { Text("Open app settings") }
        } else {
            GradientButton(
                text = "Allow SMS access",
                onClick = onGrant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ImportedConfirmation(
    expenses: Int,
    subscriptions: Int,
    onScanAgain: () -> Unit,
    onDone: () -> Unit,
) {
    val parts = buildList {
        if (expenses > 0) add("$expenses ${if (expenses == 1) "expense" else "expenses"}")
        if (subscriptions > 0) {
            add("$subscriptions ${if (subscriptions == 1) "subscription" else "subscriptions"}")
        }
    }
    EmptyState(
        icon = Icons.Rounded.Sms,
        title = if (parts.isEmpty()) "Nothing imported" else "Imported ${parts.joinToString(" and ")}",
        subtitle = "They are in your lists now - edit any of them like a normal entry.",
        modifier = Modifier.fillMaxWidth(),
        action = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onScanAgain) { Text("Scan again") }
                TextButton(onClick = onDone) { Text("Done") }
            }
        },
    )
}

@Composable
private fun ReviewList(
    rows: List<SmsImportRow>,
    amountLabel: (Double) -> String,
    onToggle: (Long) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onEditCategory: (SmsImportRow) -> Unit,
    onImport: () -> Unit,
) {
    val selectedCount = rows.count { it.selected }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "$selectedCount of ${rows.size} selected",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { onSelectAll(selectedCount != rows.size) }) {
                Text(if (selectedCount == rows.size) "Clear all" else "Select all")
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            sectionOrder.forEach { (type, label) ->
                val ofType = rows.filter { it.type == type }
                if (ofType.isEmpty()) return@forEach
                item(key = "header_$type") {
                    Text(
                        text = "$label · ${ofType.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                    )
                }
                items(ofType, key = { it.sms.smsId }) { row ->
                    MessageRow(
                        row = row,
                        amountLabel = amountLabel(row.sms.amount),
                        onClick = { onToggle(row.sms.smsId) },
                        onEditCategory = { onEditCategory(row) },
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        GradientButton(
            text = if (selectedCount == 1) "Import 1 entry" else "Import $selectedCount entries",
            onClick = onImport,
            enabled = selectedCount > 0,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun MessageRow(
    row: SmsImportRow,
    amountLabel: String,
    onClick: () -> Unit,
    onEditCategory: () -> Unit,
) {
    AppCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = row.selected, onCheckedChange = { onClick() })
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.sms.merchant,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${row.sms.date.format(dayFormat)} · ${formatTime(row.sms.time)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Spacer(Modifier.height(6.dp))
                row.category?.let { CategoryTag(it, onEditCategory) }
            }
            Spacer(Modifier.size(8.dp))
            Text(
                text = amountLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun CategoryTag(category: Category, onClick: () -> Unit) {
    val color = Color(category.color)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.16f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = CategoryIcons.iconFor(category.icon),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
