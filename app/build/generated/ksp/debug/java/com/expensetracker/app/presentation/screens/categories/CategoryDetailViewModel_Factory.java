package com.expensetracker.app.presentation.screens.categories;

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
public final class CategoryDetailViewModel_Factory implements Factory<CategoryDetailViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<CategoryRepository> categoryRepositoryProvider;

  private final Provider<ExpenseRepository> expenseRepositoryProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private CategoryDetailViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<CategoryRepository> categoryRepositoryProvider,
      Provider<ExpenseRepository> expenseRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.categoryRepositoryProvider = categoryRepositoryProvider;
    this.expenseRepositoryProvider = expenseRepositoryProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  @Override
  public CategoryDetailViewModel get() {
    return newInstance(savedStateHandleProvider.get(), categoryRepositoryProvider.get(), expenseRepositoryProvider.get(), settingsRepositoryProvider.get());
  }

  public static CategoryDetailViewModel_Factory create(
      Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<CategoryRepository> categoryRepositoryProvider,
      Provider<ExpenseRepository> expenseRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    return new CategoryDetailViewModel_Factory(savedStateHandleProvider, categoryRepositoryProvider, expenseRepositoryProvider, settingsRepositoryProvider);
  }

  public static CategoryDetailViewModel newInstance(SavedStateHandle savedStateHandle,
      CategoryRepository categoryRepository, ExpenseRepository expenseRepository,
      SettingsRepository settingsRepository) {
    return new CategoryDetailViewModel(savedStateHandle, categoryRepository, expenseRepository, settingsRepository);
  }
}
