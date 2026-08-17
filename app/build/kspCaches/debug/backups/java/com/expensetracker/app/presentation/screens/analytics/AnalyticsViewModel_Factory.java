package com.expensetracker.app.presentation.screens.analytics;

import com.expensetracker.app.domain.repository.CategoryRepository;
import com.expensetracker.app.domain.repository.ExpenseRepository;
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
public final class AnalyticsViewModel_Factory implements Factory<AnalyticsViewModel> {
  private final Provider<ExpenseRepository> expenseRepositoryProvider;

  private final Provider<CategoryRepository> categoryRepositoryProvider;

  private final Provider<SubscriptionRepository> subscriptionRepositoryProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private AnalyticsViewModel_Factory(Provider<ExpenseRepository> expenseRepositoryProvider,
      Provider<CategoryRepository> categoryRepositoryProvider,
      Provider<SubscriptionRepository> subscriptionRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    this.expenseRepositoryProvider = expenseRepositoryProvider;
    this.categoryRepositoryProvider = categoryRepositoryProvider;
    this.subscriptionRepositoryProvider = subscriptionRepositoryProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  @Override
  public AnalyticsViewModel get() {
    return newInstance(expenseRepositoryProvider.get(), categoryRepositoryProvider.get(), subscriptionRepositoryProvider.get(), settingsRepositoryProvider.get());
  }

  public static AnalyticsViewModel_Factory create(
      Provider<ExpenseRepository> expenseRepositoryProvider,
      Provider<CategoryRepository> categoryRepositoryProvider,
      Provider<SubscriptionRepository> subscriptionRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    return new AnalyticsViewModel_Factory(expenseRepositoryProvider, categoryRepositoryProvider, subscriptionRepositoryProvider, settingsRepositoryProvider);
  }

  public static AnalyticsViewModel newInstance(ExpenseRepository expenseRepository,
      CategoryRepository categoryRepository, SubscriptionRepository subscriptionRepository,
      SettingsRepository settingsRepository) {
    return new AnalyticsViewModel(expenseRepository, categoryRepository, subscriptionRepository, settingsRepository);
  }
}
