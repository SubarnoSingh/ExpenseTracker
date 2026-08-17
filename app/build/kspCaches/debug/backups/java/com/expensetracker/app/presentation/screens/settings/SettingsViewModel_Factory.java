package com.expensetracker.app.presentation.screens.settings;

import com.expensetracker.app.data.transfer.DataTransferManager;
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<DataTransferManager> dataTransferManagerProvider;

  private SettingsViewModel_Factory(Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<DataTransferManager> dataTransferManagerProvider) {
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.dataTransferManagerProvider = dataTransferManagerProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(settingsRepositoryProvider.get(), dataTransferManagerProvider.get());
  }

  public static SettingsViewModel_Factory create(
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<DataTransferManager> dataTransferManagerProvider) {
    return new SettingsViewModel_Factory(settingsRepositoryProvider, dataTransferManagerProvider);
  }

  public static SettingsViewModel newInstance(SettingsRepository settingsRepository,
      DataTransferManager dataTransferManager) {
    return new SettingsViewModel(settingsRepository, dataTransferManager);
  }
}
