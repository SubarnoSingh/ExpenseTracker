package com.expensetracker.app.presentation.screens.subscriptions;

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
public final class SubscriptionsViewModel_Factory implements Factory<SubscriptionsViewModel> {
  private final Provider<SubscriptionRepository> subscriptionRepositoryProvider;

  private final Provider<CategoryRepository> categoryRepositoryProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private SubscriptionsViewModel_Factory(
      Provider<SubscriptionRepository> subscriptionRepositoryProvider,
      Provider<CategoryRepository> categoryRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    this.subscriptionRepositoryProvider = subscriptionRepositoryProvider;
    this.categoryRepositoryProvider = categoryRepositoryProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  @Override
  public SubscriptionsViewModel get() {
    return newInstance(subscriptionRepositoryProvider.get(), categoryRepositoryProvider.get(), settingsRepositoryProvider.get());
  }

  public static SubscriptionsViewModel_Factory create(
      Provider<SubscriptionRepository> subscriptionRepositoryProvider,
      Provider<CategoryRepository> categoryRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    return new SubscriptionsViewModel_Factory(subscriptionRepositoryProvider, categoryRepositoryProvider, settingsRepositoryProvider);
  }

  public static SubscriptionsViewModel newInstance(SubscriptionRepository subscriptionRepository,
      CategoryRepository categoryRepository, SettingsRepository settingsRepository) {
    return new SubscriptionsViewModel(subscriptionRepository, categoryRepository, settingsRepository);
  }
}
