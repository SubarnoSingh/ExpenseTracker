package com.expensetracker.app.presentation.screens.add;

import androidx.lifecycle.SavedStateHandle;
import com.expensetracker.app.domain.repository.CategoryRepository;
import com.expensetracker.app.domain.repository.ExpenseRepository;
import com.expensetracker.app.domain.repository.SettingsRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class AddExpenseViewModel_Factory implements Factory<AddExpenseViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<ExpenseRepository> expenseRepositoryProvider;

  private final Provider<CategoryRepository> categoryRepositoryProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private AddExpenseViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<ExpenseRepository> expenseRepositoryProvider,
      Provider<CategoryRepository> categoryRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.expenseRepositoryProvider = expenseRepositoryProvider;
    this.categoryRepositoryProvider = categoryRepositoryProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  @Override
  public AddExpenseViewModel get() {
    return newInstance(savedStateHandleProvider.get(), expenseRepositoryProvider.get(), categoryRepositoryProvider.get(), settingsRepositoryProvider.get());
  }

  public static AddExpenseViewModel_Factory create(
      Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<ExpenseRepository> expenseRepositoryProvider,
      Provider<CategoryRepository> categoryRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    return new AddExpenseViewModel_Factory(savedStateHandleProvider, expenseRepositoryProvider, categoryRepositoryProvider, settingsRepositoryProvider);
  }

  public static AddExpenseViewModel newInstance(SavedStateHandle savedStateHandle,
      ExpenseRepository expenseRepository, CategoryRepository categoryRepository,
      SettingsRepository settingsRepository) {
    return new AddExpenseViewModel(savedStateHandle, expenseRepository, categoryRepository, settingsRepository);
  }
}
