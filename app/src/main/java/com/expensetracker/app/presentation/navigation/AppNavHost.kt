package com.expensetracker.app.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.expensetracker.app.presentation.components.BottomTab
import com.expensetracker.app.presentation.components.PremiumBottomBar
import com.expensetracker.app.presentation.screens.add.AddExpenseSheetContent
import com.expensetracker.app.presentation.screens.add.AddExpenseViewModel
import com.expensetracker.app.presentation.screens.analytics.AnalyticsScreen
import com.expensetracker.app.presentation.screens.categories.CategoriesScreen
import com.expensetracker.app.presentation.screens.categories.CategoryDetailScreen
import com.expensetracker.app.presentation.screens.expenses.ExpensesScreen
import com.expensetracker.app.presentation.screens.home.HomeScreen
import com.expensetracker.app.presentation.screens.more.MoreScreen
import com.expensetracker.app.presentation.screens.settings.SettingsScreen
import com.expensetracker.app.presentation.screens.smsimport.SmsImportScreen
import com.expensetracker.app.presentation.screens.subscriptions.SubscriptionSheetContent
import com.expensetracker.app.presentation.screens.subscriptions.SubscriptionSheetViewModel
import com.expensetracker.app.presentation.screens.subscriptions.SubscriptionsScreen

private val tabRoutes = setOf("home", "expenses", "analytics", "more")

private val bottomTabs = listOf(
    BottomTab("home", "Home", Icons.Rounded.Home),
    BottomTab("expenses", "Expenses", Icons.Rounded.ReceiptLong),
    BottomTab("analytics", "Analytics", Icons.Rounded.Insights),
    BottomTab("more", "More", Icons.Rounded.MoreHoriz),
)

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "home"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (currentRoute in tabRoutes) {
                PremiumBottomBar(
                    tabs = bottomTabs,
                    currentRoute = currentRoute,
                    onTabSelected = { route -> navController.navigateToTab(route) },
                    onAddClick = { navController.navigate("add") },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 24 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(260)) + slideInVertically(tween(260)) { -it / 24 } },
            popExitTransition = { fadeOut(tween(200)) },
        ) {
            composable("home") {
                HomeScreen(
                    onOpenAddExpense = { type ->
                        navController.navigate("add?type=${type?.name?.lowercase() ?: ""}")
                    },
                    onOpenSubscriptionAdd = { navController.navigate("subscription_sheet") },
                    onOpenExpenses = { navController.navigateToTab("expenses") },
                    onOpenCategory = { id -> navController.navigate("category/$id") },
                    onEditExpense = { id -> navController.navigate("add?expenseId=$id") },
                    onEditSubscription = { id ->
                        navController.navigate("subscription_sheet?subscriptionId=$id")
                    },
                )
            }

            composable("expenses") {
                ExpensesScreen(
                    onAddExpense = { type ->
                        navController.navigate("add?type=${type?.name?.lowercase() ?: ""}")
                    },
                    onEditExpense = { id -> navController.navigate("add?expenseId=$id") },
                    onAddSubscription = { navController.navigate("subscription_sheet") },
                    onEditSubscription = { id ->
                        navController.navigate("subscription_sheet?subscriptionId=$id")
                    },
                )
            }

            composable("analytics") {
                AnalyticsScreen(
                    onEditExpense = { id -> navController.navigate("add?expenseId=$id") },
                    onEditSubscription = { id ->
                        navController.navigate("subscription_sheet?subscriptionId=$id")
                    },
                )
            }

            composable("more") {
                MoreScreen(
                    onOpenSubscriptions = { navController.navigate("subscriptions") },
                    onOpenCategories = { navController.navigate("categories") },
                    onOpenSettings = { navController.navigate("settings") },
                    onOpenSmsImport = { navController.navigate("sms_import") },
                )
            }

            composable(
                route = "add?type={type}&expenseId={expenseId}",
                arguments = listOf(
                    navArgument("type") { defaultValue = "" },
                    navArgument("expenseId") { defaultValue = "" },
                ),
            ) {
                AddExpenseRoute(navController)
            }

            composable(
                route = "subscription_sheet?subscriptionId={subscriptionId}",
                arguments = listOf(
                    navArgument("subscriptionId") { defaultValue = "" },
                ),
            ) {
                SubscriptionSheetRoute(navController)
            }

            composable(
                route = "category/{categoryId}",
                arguments = listOf(navArgument("categoryId") { type = NavType.StringType }),
            ) {
                CategoryDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEditExpense = { id -> navController.navigate("add?expenseId=$id") },
                )
            }

            composable("categories") {
                CategoriesScreen()
            }

            composable("sms_import") {
                SmsImportScreen(onBack = { navController.popBackStack() })
            }

            composable("settings") {
                SettingsScreen(
                    onManageCategories = { navController.navigate("categories") },
                )
            }

            composable("subscriptions") {
                SubscriptionsScreen(
                    onAdd = { navController.navigate("subscription_sheet") },
                    onEdit = { id ->
                        navController.navigate("subscription_sheet?subscriptionId=$id")
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseRoute(navController: NavController) {
    val viewModel: AddExpenseViewModel = hiltViewModel()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { navController.popBackStack() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        AddExpenseSheetContent(
            viewModel = viewModel,
            onDone = { navController.popBackStack() },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubscriptionSheetRoute(navController: NavController) {
    val viewModel: SubscriptionSheetViewModel = hiltViewModel()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { navController.popBackStack() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        SubscriptionSheetContent(
            viewModel = viewModel,
            onDone = { navController.popBackStack() },
        )
    }
}

private fun NavController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
