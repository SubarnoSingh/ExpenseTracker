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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.expensetracker.app.domain.model.ExpenseType
import com.expensetracker.app.presentation.components.AppCard
import com.expensetracker.app.presentation.components.CategoryIcons
import com.expensetracker.app.presentation.components.EmptyState
import com.expensetracker.app.presentation.components.GradientButton
import com.expensetracker.app.presentation.components.SegmentedControl
import com.expensetracker.app.presentation.util.formatMoney
import com.expensetracker.app.presentation.util.formatTime
import java.time.format.DateTimeFormatter

private val dayFormat = DateTimeFormatter.ofPattern("d MMM")

@Composable
fun SmsImportScreen(
    onBack: () -> Unit,
    viewModel: SmsImportViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val type by viewModel.type.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()

    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var denied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { result ->
        granted = result
        denied = !result
    }

    // Scan as soon as we are allowed to, so the screen is never a dead end.
    LaunchedEffect(granted) {
        if (granted) viewModel.scan()
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

            state is ScanState.Scanning -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state is ScanState.Done -> ImportedConfirmation(
                count = state.imported,
                onScanAgain = {
                    viewModel.clearResult()
                    viewModel.scan()
                },
                onDone = onBack,
            )

            rows.isEmpty() -> EmptyState(
                icon = Icons.Rounded.Sms,
                title = "No new spending messages",
                subtitle = "Nothing new since your last import. Bank, card and UPI debit alerts show up here automatically.",
                modifier = Modifier.fillMaxWidth(),
                action = { TextButton(onClick = { viewModel.scan() }) { Text("Scan again") } },
            )

            else -> ReviewList(
                rows = rows,
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                type = type,
                amountLabel = { amount -> formatMoney(amount, currency) },
                onToggle = viewModel::toggle,
                onSelectAll = viewModel::setAllSelected,
                onTypeChange = viewModel::setType,
                onCategoryChange = viewModel::setCategory,
                onImport = viewModel::importSelected,
            )
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
private fun ImportedConfirmation(count: Int, onScanAgain: () -> Unit, onDone: () -> Unit) {
    EmptyState(
        icon = Icons.Rounded.Sms,
        title = "Imported $count ${if (count == 1) "expense" else "expenses"}",
        subtitle = "They are in your expense list now - edit any of them like a normal entry.",
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
    categories: List<Category>,
    selectedCategoryId: Long?,
    type: ExpenseType,
    amountLabel: (Double) -> String,
    onToggle: (Long) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onTypeChange: (ExpenseType) -> Unit,
    onCategoryChange: (Long) -> Unit,
    onImport: () -> Unit,
) {
    val selectedCount = rows.count { it.selected }

    Column(modifier = Modifier.fillMaxSize()) {
        SegmentedControl(
            options = ExpenseType.entries,
            selected = type,
            onSelect = onTypeChange,
            label = { if (it == ExpenseType.REGULAR) "Regular" else "Occasional" },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))

        Text(
            text = "Category for these imports",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories, key = { it.id }) { category ->
                CategoryChip(
                    category = category,
                    selected = category.id == selectedCategoryId,
                    onClick = { onCategoryChange(category.id) },
                )
            }
        }
        Spacer(Modifier.height(14.dp))

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
            items(rows, key = { it.sms.smsId }) { row ->
                MessageRow(
                    row = row,
                    amountLabel = amountLabel(row.sms.amount),
                    onClick = { onToggle(row.sms.smsId) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        GradientButton(
            text = if (selectedCount == 1) "Import 1 expense" else "Import $selectedCount expenses",
            onClick = onImport,
            enabled = selectedCount > 0 && selectedCategoryId != null,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun MessageRow(row: SmsImportRow, amountLabel: String, onClick: () -> Unit) {
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
                    text = "${row.sms.date.format(dayFormat)} · ${formatTime(row.sms.time)}" +
                        row.sms.sender.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
private fun CategoryChip(category: Category, selected: Boolean, onClick: () -> Unit) {
    val color = Color(category.color)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) color.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .clickable(onClick = onClick)
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
