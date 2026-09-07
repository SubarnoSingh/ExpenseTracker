package com.expensetracker.app.presentation.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.TableRows
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.domain.model.Currency
import com.expensetracker.app.domain.model.ThemeMode
import com.expensetracker.app.presentation.components.GradientButton
import com.expensetracker.app.presentation.components.SectionHeader
import com.expensetracker.app.presentation.components.SegmentedControl
import com.expensetracker.app.presentation.theme.LocalAppColors

@Composable
fun SettingsScreen(
    onManageCategories: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val appColors = LocalAppColors.current

    var showNameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // SAF launchers
    val exportExpensesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { viewModel.exportExpensesCsv(it) } }

    val exportSubscriptionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { viewModel.exportSubscriptionsCsv(it) } }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportJsonBackup(it) } }

    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importJsonBackup(it) } }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(10.dp))
            }

            item {
                AppSettingsRow(
                    icon = Icons.Rounded.Person,
                    iconTint = appColors.gradientStart,
                    title = "Profile",
                    subtitle = "Name: ${state.userName}",
                    trailingIcon = Icons.Rounded.Edit,
                    onClick = { showNameDialog = true },
                )
            }

            item {
                SectionHeader("Appearance", modifier = Modifier.padding(top = 12.dp))
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Theme",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SegmentedControl(
                        options = ThemeMode.entries,
                        selected = state.themeMode,
                        onSelect = viewModel::setThemeMode,
                        label = {
                            when (it) {
                                ThemeMode.DARK -> "Dark"
                                ThemeMode.LIGHT -> "Light"
                                ThemeMode.SYSTEM -> "System"
                            }
                        },
                    )
                    Text(
                        text = "Currency",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Currency.entries.chunked(4).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            row.forEach { currency ->
                                val selected = currency.code == state.currency
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                            else MaterialTheme.colorScheme.surfaceContainerHigh
                                        )
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                        ) { viewModel.setCurrency(currency.code) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = currency.code,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { SectionHeader("Manage", modifier = Modifier.padding(top = 12.dp)) }

            item {
                AppSettingsRow(
                    icon = Icons.Rounded.TableRows,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    title = "Manage Categories",
                    subtitle = "Add, edit or delete categories",
                    trailingIcon = Icons.Rounded.ChevronRight,
                    onClick = onManageCategories,
                )
            }

            item { SectionHeader("Data", modifier = Modifier.padding(top = 12.dp)) }

            item {
                AppSettingsRow(
                    icon = Icons.Rounded.TableChart,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "Export Expenses (CSV)",
                    subtitle = "All regular & occasional expenses",
                    trailingIcon = Icons.Rounded.FileDownload,
                    onClick = { exportExpensesLauncher.launch("expenses.csv") },
                )
            }
            item {
                AppSettingsRow(
                    icon = Icons.Rounded.TableRows,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "Export Subscriptions (CSV)",
                    subtitle = "All subscriptions as a spreadsheet",
                    trailingIcon = Icons.Rounded.FileDownload,
                    onClick = { exportSubscriptionsLauncher.launch("subscriptions.csv") },
                )
            }
            item {
                AppSettingsRow(
                    icon = Icons.Rounded.FileDownload,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "Export Backup (JSON)",
                    subtitle = "Categories, expenses, subscriptions & settings",
                    trailingIcon = Icons.Rounded.FileDownload,
                    onClick = { exportBackupLauncher.launch("expense-tracker-backup.json") },
                )
            }
            item {
                AppSettingsRow(
                    icon = Icons.Rounded.FileUpload,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    title = "Import Backup (JSON)",
                    subtitle = "Replaces all current data",
                    trailingIcon = Icons.Rounded.FileUpload,
                    onClick = { importBackupLauncher.launch(arrayOf("*/*")) },
                )
            }
            item {
                AppSettingsRow(
                    icon = Icons.Rounded.DeleteForever,
                    iconTint = MaterialTheme.colorScheme.error,
                    title = "Delete All Data",
                    subtitle = "Remove every expense, subscription and category",
                    trailingIcon = Icons.Rounded.DeleteForever,
                    onClick = { showDeleteDialog = true },
                )
            }

            item { SectionHeader("About", modifier = Modifier.padding(top = 12.dp)) }

            item {
                AppSettingsRow(
                    icon = Icons.Rounded.Info,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    title = "Expense Tracker",
                    subtitle = "Version 1.0.0  •  100% offline. Your data never leaves this device.",
                    trailingIcon = null,
                    onClick = null,
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        )
    }

    if (showNameDialog) {
        var name by remember { mutableStateOf(state.userName) }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Your name") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setUserName(name)
                    showNameDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Delete all data?") },
            text = {
                Text(
                    "This permanently removes every expense, subscription and custom " +
                        "category from this device, and lets SMS import offer past messages " +
                        "again. This can't be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAllData()
                    showDeleteDialog = false
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun AppSettingsRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    trailingIcon: ImageVector?,
    onClick: (() -> Unit)?,
) {
    val appColors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconTint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(19.dp),
            )
        }
        Spacer(Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (trailingIcon != null) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = appColors.gradientStart,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
