package com.expensetracker.app.presentation.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.model.CategoryType
import com.expensetracker.app.domain.repository.CategoryDeleteResult
import com.expensetracker.app.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoriesUiState(
    val regular: List<Category> = emptyList(),
    val occasional: List<Category> = emptyList(),
    val subscription: List<Category> = emptyList(),
    val loading: Boolean = true,
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    val uiState: StateFlow<CategoriesUiState> = categoryRepository.observeCategories()
        .map { list ->
            CategoriesUiState(
                regular = list.filter { it.type == CategoryType.REGULAR },
                occasional = list.filter { it.type == CategoryType.OCCASIONAL },
                subscription = list.filter { it.type == CategoryType.SUBSCRIPTION },
                loading = false,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState())

    /** One-shot messages shown as a snackbar. */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun consumeMessage() { _message.value = null }

    fun addCategory(
        name: String,
        icon: String,
        color: Long,
        type: CategoryType,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            categoryRepository.addCategory(
                Category(name = name, icon = icon, color = color, type = type)
            )
            onDone()
        }
    }

    fun updateCategory(
        category: Category,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            categoryRepository.updateCategory(category)
            onDone()
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            when (val result = categoryRepository.deleteCategory(category)) {
                is CategoryDeleteResult.Deleted -> {
                    _message.value = "Category deleted"
                }
                is CategoryDeleteResult.InUse -> {
                    val detail = buildList {
                        if (result.expenseCount > 0) add("${result.expenseCount} expense(s)")
                        if (result.subscriptionCount > 0) add("${result.subscriptionCount} subscription(s)")
                    }.joinToString(" and ")
                    _message.value = "Can't delete - it's used by $detail"
                }
            }
        }
    }
}
