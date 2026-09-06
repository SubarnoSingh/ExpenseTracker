package com.expensetracker.app.presentation.screens.smsimport

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Sms
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.domain.model.BillingCycle
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.model.CategoryType
import com.expensetracker.app.domain.model.Currency
import com.expensetracker.app.presentation.components.CategoryBadge
import com.expensetracker.app.presentation.components.CategoryIcons
import com.expensetracker.app.presentation.components.EmptyState
import com.expensetracker.app.presentation.components.GradientButton
import com.expensetracker.app.presentation.theme.LocalAppColors
import com.expensetracker.app.presentation.util.formatMoney
import com.expensetracker.app.presentation.util.formatMoneyCompact
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val shortDate = DateTimeFormatter.ofPattern("d MMM")
private val longDate = DateTimeFormatter.ofPattern("d MMM yyyy")

private val sections = listOf(
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

    LaunchedEffect(granted) {
        if (granted) viewModel.prepare()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        TitleBar(onBack = onBack)

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

            else -> {
                ControlBar(
                    from = scanFrom,
                    allSelected = rows.isNotEmpty() && rows.all { it.selected },
                    showSelectAll = rows.isNotEmpty(),
                    onChangeRange = { showDatePicker = true },
                    onSelectAll = viewModel::setAllSelected,
                )

                when {
                    state is ScanState.Scanning -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }

                    rows.isEmpty() -> EmptyState(
                        icon = Icons.Rounded.Sms,
                        title = "Nothing to import",
                        subtitle = "No bank or UPI debit alerts since ${scanFrom.format(longDate)}.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        action = {
                            TextButton(onClick = { showDatePicker = true }) {
                                Text("Look further back")
                            }
                        },
                    )

                    else -> ReviewList(
                        rows = rows,
                        currency = currency,
                        onToggle = viewModel::toggle,
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
            merchant = row.sms.merchant,
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
private fun TitleBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
        }
        Text(
            text = "Import from SMS",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/** Date range on the left, bulk selection on the right - the two things you change. */
@Composable
private fun ControlBar(
    from: LocalDate,
    allSelected: Boolean,
    showSelectAll: Boolean,
    onChangeRange: () -> Unit,
    onSelectAll: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(onClick = onChangeRange)
                .padding(start = 12.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "From ${from.format(shortDate)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (showSelectAll) {
            TextButton(onClick = { onSelectAll(!allSelected) }) {
                Text(if (allSelected) "Clear all" else "Select all")
            }
        }
    }
}

@Composable
private fun ReviewList(
    rows: List<SmsImportRow>,
    currency: Currency,
    onToggle: (Long) -> Unit,
    onEditCategory: (SmsImportRow) -> Unit,
    onImport: () -> Unit,
) {
    val selected = rows.filter { it.selected }
    val total = selected.sumOf { it.sms.amount }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            sections.forEach { (type, label) ->
                val ofType = rows.filter { it.type == type }
                if (ofType.isEmpty()) return@forEach
                item(key = "header_$type") {
                    SectionHeader(
                        label = label,
                        count = ofType.size,
                        total = formatMoneyCompact(ofType.sumOf { it.sms.amount }, currency),
                    )
                }
                items(ofType, key = { it.sms.smsId }) { row ->
                    MessageRow(
                        row = row,
                        amountText = formatMoney(row.sms.amount, currency),
                        onClick = { onToggle(row.sms.smsId) },
                        onEditCategory = { onEditCategory(row) },
                    )
                }
            }
        }

        ImportBar(
            count = selected.size,
            total = formatMoney(total, currency),
            onImport = onImport,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun SectionHeader(label: String, count: Int, total: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "$count · $total",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Same anatomy as [com.expensetracker.app.presentation.components.ExpenseRow] so the
 * list reads like the rest of the app. Tapping the row keeps or drops it; tapping the
 * badge moves it to another category.
 */
@Composable
private fun MessageRow(
    row: SmsImportRow,
    amountText: String,
    onClick: () -> Unit,
    onEditCategory: () -> Unit,
) {
    val category = row.category
    val color = category?.let { Color(it.color) } ?: MaterialTheme.colorScheme.onSurfaceVariant
    val contentAlpha by animateFloatAsState(if (row.selected) 1f else 0.4f, label = "rowAlpha")
    val rowColor by animateColorAsState(
        if (row.selected) MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
        else Color.Transparent,
        label = "rowColor",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(rowColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SelectionBadge(
            selected = row.selected,
            icon = category?.icon,
            color = color,
            onClick = onEditCategory,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .alpha(contentAlpha),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = row.sms.merchant,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = row.subtitle(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = amountText,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.alpha(contentAlpha),
        )
    }
}

private fun SmsImportRow.subtitle(): String {
    val name = category?.name ?: "Uncategorised"
    return if (type == CategoryType.SUBSCRIPTION && charges > 1) {
        val cycle = if (billingCycle == BillingCycle.YEARLY) "Yearly" else "Monthly"
        "$name  •  $cycle, $charges charges since ${firstCharge.format(shortDate)}"
    } else {
        "$name  •  ${sms.date.format(shortDate)}"
    }
}

/**
 * The category badge doubles as the selection indicator: a tick replaces the icon
 * when the row is going in, so one glance down the column shows what's included.
 */
@Composable
private fun SelectionBadge(
    selected: Boolean,
    icon: String?,
    color: Color,
    onClick: () -> Unit,
) {
    Box(modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick)) {
        CategoryBadge(
            icon = if (selected) Icons.Rounded.Check else CategoryIcons.iconFor(icon.orEmpty()),
            color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
            size = 40,
        )
    }
}

@Composable
private fun ImportBar(
    count: Int,
    total: String,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(appColors.cardBrush)
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp)
            .navigationBarsPadding()
            .padding(bottom = 12.dp),
    ) {
        GradientButton(
            text = when (count) {
                0 -> "Select entries to import"
                1 -> "Import 1 entry  ·  $total"
                else -> "Import $count entries  ·  $total"
            },
            onClick = onImport,
            enabled = count > 0,
            modifier = Modifier.fillMaxWidth(),
        )
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
    merchant: String,
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
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item(key = "sheet_title") {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    Text(
                        text = merchant,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Moving this to a subscription category tracks it as recurring.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            sections.forEach { (type, label) ->
                val ofType = categories.filter { it.type == type }
                if (ofType.isEmpty()) return@forEach
                item(key = "sheet_header_$type") {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            start = 20.dp,
                            end = 20.dp,
                            top = 18.dp,
                            bottom = 4.dp,
                        ),
                    )
                }
                items(ofType, key = { it.id }) { category ->
                    CategoryOption(
                        category = category,
                        selected = category.id == selectedId,
                        onClick = { onPick(category) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryOption(category: Category, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CategoryBadge(
            icon = CategoryIcons.iconFor(category.icon),
            color = Color(category.color),
            size = 36,
        )
        Text(
            text = category.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Current category",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Turn bank alerts into entries",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Expense Tracker reads the debit alerts your bank sends you and drafts " +
                "matching entries. You review the list and choose what gets saved.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Assurance("Messages never leave this device.")
        Assurance("Nothing is saved until you tap Import.")
        Assurance("Only debit alerts are read - everything else is ignored.")

        Spacer(Modifier.weight(1f))
        if (denied) {
            Text(
                text = "SMS access is off. Turn it on in system settings, then come back.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(12.dp))
            GradientButton(
                text = "Open app settings",
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            GradientButton(
                text = "Allow SMS access",
                onClick = onGrant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun Assurance(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyState(
            icon = Icons.Rounded.Check,
            title = if (parts.isEmpty()) "Nothing imported" else "Added ${parts.joinToString(" and ")}",
            subtitle = "Edit any of them from your expenses and subscriptions lists.",
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onScanAgain) { Text("Scan again") }
            Spacer(Modifier.width(4.dp))
            TextButton(onClick = onDone) { Text("Done") }
        }
    }
}
