package com.expensetracker.app.data.repository;

import com.expensetracker.app.data.local.dao.SubscriptionDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class SubscriptionRepositoryImpl_Factory implements Factory<SubscriptionRepositoryImpl> {
  private final Provider<SubscriptionDao> daoProvider;

  private SubscriptionRepositoryImpl_Factory(Provider<SubscriptionDao> daoProvider) {
    this.daoProvider = daoProvider;
  }

  @Override
  public SubscriptionRepositoryImpl get() {
    return newInstance(daoProvider.get());
  }

  public static SubscriptionRepositoryImpl_Factory create(Provider<SubscriptionDao> daoProvider) {
    return new SubscriptionRepositoryImpl_Factory(daoProvider);
  }

  public static SubscriptionRepositoryImpl newInstance(SubscriptionDao dao) {
    return new SubscriptionRepositoryImpl(dao);
  }
}
