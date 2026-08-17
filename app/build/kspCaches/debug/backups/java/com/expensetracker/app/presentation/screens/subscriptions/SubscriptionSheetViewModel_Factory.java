package com.expensetracker.app.presentation.screens.subscriptions;

import androidx.lifecycle.SavedStateHandle;
import com.expensetracker.app.domain.repository.CategoryRepository;
import com.expensetracker.app.domain.repository.SettingsRepository;
import com.expensetracker.app.domain.repository.SubscriptionRepository;
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
public final class SubscriptionSheetViewModel_Factory implements Factory<SubscriptionSheetViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<SubscriptionRepository> subscriptionRepositoryProvider;

  private final Provider<CategoryRepository> categoryRepositoryProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private SubscriptionSheetViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<SubscriptionRepository> subscriptionRepositoryProvider,
      Provider<CategoryRepository> categoryRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.subscriptionRepositoryProvider = subscriptionRepositoryProvider;
    this.categoryRepositoryProvider = categoryRepositoryProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  @Override
  public SubscriptionSheetViewModel get() {
    return newInstance(savedStateHandleProvider.get(), subscriptionRepositoryProvider.get(), categoryRepositoryProvider.get(), settingsRepositoryProvider.get());
  }

  public static SubscriptionSheetViewModel_Factory create(
      Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<SubscriptionRepository> subscriptionRepositoryProvider,
      Provider<CategoryRepository> categoryRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    return new SubscriptionSheetViewModel_Factory(savedStateHandleProvider, subscriptionRepositoryProvider, categoryRepositoryProvider, settingsRepositoryProvider);
  }

  public static SubscriptionSheetViewModel newInstance(SavedStateHandle savedStateHandle,
      SubscriptionRepository subscriptionRepository, CategoryRepository categoryRepository,
      SettingsRepository settingsRepository) {
    return new SubscriptionSheetViewModel(savedStateHandle, subscriptionRepository, categoryRepository, settingsRepository);
  }
}
