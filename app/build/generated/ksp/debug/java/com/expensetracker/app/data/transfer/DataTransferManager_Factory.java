package com.expensetracker.app.data.transfer;

import android.content.Context;
import com.expensetracker.app.data.local.database.AppDatabase;
import com.expensetracker.app.domain.repository.SettingsRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class DataTransferManager_Factory implements Factory<DataTransferManager> {
  private final Provider<Context> contextProvider;

  private final Provider<AppDatabase> databaseProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private DataTransferManager_Factory(Provider<Context> contextProvider,
      Provider<AppDatabase> databaseProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    this.contextProvider = contextProvider;
    this.databaseProvider = databaseProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  @Override
  public DataTransferManager get() {
    return newInstance(contextProvider.get(), databaseProvider.get(), settingsRepositoryProvider.get());
  }

  public static DataTransferManager_Factory create(Provider<Context> contextProvider,
      Provider<AppDatabase> databaseProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    return new DataTransferManager_Factory(contextProvider, databaseProvider, settingsRepositoryProvider);
  }

  public static DataTransferManager newInstance(Context context, AppDatabase database,
      SettingsRepository settingsRepository) {
    return new DataTransferManager(context, database, settingsRepository);
  }
}
