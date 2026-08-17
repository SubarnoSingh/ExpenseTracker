package com.expensetracker.app.presentation.screens.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.model.CategoryType
import com.expensetracker.app.presentation.components.CategoryBadge
import com.expensetracker.app.presentation.components.CategoryIcons
import com.expensetracker.app.presentation.components.GradientFab
import com.expensetracker.app.presentation.components.SectionHeader
import com.expensetracker.app.presentation.components.SegmentedControl
import com.expensetracker.app.presentation.theme.LocalAppColors
import com.expensetracker.app.presentation.theme.Amber
import com.expensetracker.app.presentation.theme.Blue
import com.expensetracker.app.presentation.theme.Cyan
import com.expensetracker.app.presentation.theme.Green
import com.expensetracker.app.presentation.theme.Indigo
import com.expensetracker.app.presentation.theme.Lime
import com.expensetracker.app.presentation.theme.Orange
import com.expensetracker.app.presentation.theme.Pink
import com.expensetracker.app.presentation.theme.Purple
import com.expensetracker.app.presentation.theme.Red
import com.expensetracker.app.presentation.theme.Rose
import com.expensetracker.app.presentation.theme.SkyBlue
import com.expensetracker.app.presentation.theme.SlateGray
import com.expensetracker.app.presentation.theme.Teal
import com.expensetracker.app.presentation.theme.Violet

val CategoryPalette: List<Color> = listOf(
    Purple, Violet, Indigo, Blue, SkyBlue, Cyan, Teal, Green, Lime, Amber, Orange, Pink, Rose, Red, SlateGray,
)

@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSheet by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }

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
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 110.dp),
        ) {
            item {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Customize the categories used across the app. Regular, occasional and subscription categories stay separate.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Spacer(Modifier.height(12.dp))
            }

            item { SectionHeader("Regular") }
            items(state.regular, key = { it.id }) { category ->
                CategoryRow(
                    category = category,
                    onEdit = {
                        editingCategory = category
                        showSheet = true
                    },
                    onDelete = { viewModel.deleteCategory(category) },
                )
            }

            item { Spacer(Modifier.height(8.dp)); SectionHeader("Occasional") }
            items(state.occasional, key = { it.id }) { category ->
                CategoryRow(
                    category = category,
                    onEdit = {
                        editingCategory = category
                        showSheet = true
                    },
                    onDelete = { viewModel.deleteCategory(category) },
                )
            }

            item { Spacer(Modifier.height(8.dp)); SectionHeader("Subscription") }
            items(state.subscription, key = { it.id }) { category ->
                CategoryRow(
                    category = category,
                    onEdit = {
                        editingCategory = category
                        showSheet = true
                    },
                    onDelete = { viewModel.deleteCategory(category) },
                )
            }
        }

        GradientFab(
            onClick = {
                editingCategory = null
                showSheet = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 24.dp),
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp),
        )
    }

    if (showSheet) {
        CategorySheet(
            category = editingCategory,
            onDismiss = { showSheet = false },
            onSave = { name, icon, color, type ->
                val existing = editingCategory
                if (existing == null) {
                    viewModel.addCategory(name, icon, color, type) { showSheet = false }
                } else {
                    viewModel.updateCategory(
                        existing.copy(name = name, icon = icon, color = color, type = type)
                    ) { showSheet = false }
                }
            },
        )
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryBadge(
            icon = CategoryIcons.iconFor(category.icon),
            color = Color(category.color),
            size = 38,
        )
        Spacer(Modifier.size(14.dp))
        Text(
            text = category.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onEdit) {
            Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = "Edit ${category.name}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = "Delete ${category.name}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySheet(
    category: Category?,
    onDismiss: () -> Unit,
    onSave: (name: String, icon: String, color: Long, type: CategoryType) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(category) { mutableStateOf(category?.name.orEmpty()) }
    var type by remember(category) { mutableStateOf(category?.type ?: CategoryType.REGULAR) }
    var icon by remember(category) { mutableStateOf(category?.icon ?: "restaurant") }
    var color by remember(category) { mutableStateOf(category?.color ?: Purple.value.toLong()) }
    var error by remember { mutableStateOf<String?>(null) }

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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = if (category == null) "Add Category" else "Edit Category",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp),
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; error = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Category name") },
                placeholder = { Text("e.g. Coffee") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )

            Text(
                text = "Type",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SegmentedControl(
                options = CategoryType.entries,
                selected = type,
                onSelect = { type = it },
                label = { it.name.lowercase().replaceFirstChar(Char::uppercase) },
            )

            Text(
                text = "Icon",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CategoryIcons.all.keys.chunked(8).forEach { rowKeys ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    rowKeys.forEach { key ->
                        val selected = key == icon
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { icon = key },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = CategoryIcons.iconFor(key),
                                contentDescription = null,
                                tint = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }

            Text(
                text = "Color",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CategoryPalette.chunked(8).forEach { rowColors ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    rowColors.forEach { c ->
                        val selected = c.value.toLong() == color
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(c)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { color = c.value.toLong() },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }

            error?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            com.expensetracker.app.presentation.components.GradientButton(
                text = if (category == null) "Add Category" else "Save Changes",
                onClick = {
                    if (name.isBlank()) error = "Enter a category name"
                    else onSave(name.trim(), icon, color, type)
                },
            )
        }
    }
}
