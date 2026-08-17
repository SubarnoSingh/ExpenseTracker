package com.expensetracker.app.data.repository;

import com.expensetracker.app.data.local.dao.CategoryDao;
import com.expensetracker.app.data.local.dao.ExpenseDao;
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
public final class CategoryRepositoryImpl_Factory implements Factory<CategoryRepositoryImpl> {
  private final Provider<CategoryDao> categoryDaoProvider;

  private final Provider<ExpenseDao> expenseDaoProvider;

  private final Provider<SubscriptionDao> subscriptionDaoProvider;

  private CategoryRepositoryImpl_Factory(Provider<CategoryDao> categoryDaoProvider,
      Provider<ExpenseDao> expenseDaoProvider, Provider<SubscriptionDao> subscriptionDaoProvider) {
    this.categoryDaoProvider = categoryDaoProvider;
    this.expenseDaoProvider = expenseDaoProvider;
    this.subscriptionDaoProvider = subscriptionDaoProvider;
  }

  @Override
  public CategoryRepositoryImpl get() {
    return newInstance(categoryDaoProvider.get(), expenseDaoProvider.get(), subscriptionDaoProvider.get());
  }

  public static CategoryRepositoryImpl_Factory create(Provider<CategoryDao> categoryDaoProvider,
      Provider<ExpenseDao> expenseDaoProvider, Provider<SubscriptionDao> subscriptionDaoProvider) {
    return new CategoryRepositoryImpl_Factory(categoryDaoProvider, expenseDaoProvider, subscriptionDaoProvider);
  }

  public static CategoryRepositoryImpl newInstance(CategoryDao categoryDao, ExpenseDao expenseDao,
      SubscriptionDao subscriptionDao) {
    return new CategoryRepositoryImpl(categoryDao, expenseDao, subscriptionDao);
  }
}
