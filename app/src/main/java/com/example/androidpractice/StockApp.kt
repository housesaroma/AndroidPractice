package com.example.androidpractice

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.androidpractice.ui.navigation.BottomNavItem
import com.example.androidpractice.ui.navigation.Screen
import com.example.androidpractice.ui.screens.detail.StockDetailScreen
import com.example.androidpractice.ui.screens.favorites.FavoritesScreen
import com.example.androidpractice.ui.screens.list.StockListScreen
import com.example.androidpractice.ui.screens.placeholder.PlaceholderScreen
import com.example.androidpractice.ui.screens.settings.SettingsScreen
import com.example.androidpractice.ui.viewmodel.StockDetailsUiState
import com.example.androidpractice.ui.viewmodel.StocksViewModel

@Composable
fun StockApp() {
    val navController = rememberNavController()
    val viewModel: StocksViewModel = viewModel(factory = StocksViewModel.factory())

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val stocksUiState by viewModel.stocksUiState.collectAsStateWithLifecycle()
    val stockDetailsUiState by viewModel.stockDetailsUiState.collectAsStateWithLifecycle()
    val settingsUiState by viewModel.settingsUiState.collectAsStateWithLifecycle()
    val favoritesUiState by viewModel.favoritesUiState.collectAsStateWithLifecycle()
    val showSettingsBadge by viewModel.showSettingsBadge.collectAsStateWithLifecycle()

    val bottomItems = listOf(
        BottomNavItem.Stocks,
        BottomNavItem.Portfolio,
        BottomNavItem.Settings
    )
    val showBottomBar = bottomItems.any { it.screen.route == currentRoute }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        val selected = currentRoute == item.screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                if (item == BottomNavItem.Settings && showSettingsBadge) {
                                    BadgedBox(badge = { Badge() }) {
                                        Icon(imageVector = item.icon, contentDescription = item.label)
                                    }
                                } else {
                                    Icon(imageVector = item.icon, contentDescription = item.label)
                                }
                            },
                            label = { Text(text = item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Stocks.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Stocks.route) {
                StockListScreen(
                    uiState = stocksUiState,
                    onRetry = viewModel::retryStocks,
                    onStockClick = { symbol ->
                        navController.navigate(Screen.StockDetail.createRoute(symbol))
                    },
                    onToggleFavorite = viewModel::toggleFavoriteFromQuote
                )
            }

            composable(Screen.Portfolio.route) {
                FavoritesScreen(
                    uiState = favoritesUiState,
                    onRemoveFavorite = viewModel::removeFavorite
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    uiState = settingsUiState,
                    onQueryChange = viewModel::onSettingsSearchQueryChange,
                    onRangePointChange = viewModel::onSettingsRangePointChange,
                    onOnlyRisingChange = viewModel::onSettingsOnlyRisingChange,
                    onResetFilters = viewModel::resetFilters,
                    onDone = {
                        val success = viewModel.onSettingsDone()
                        if (success) {
                            navController.navigate(Screen.Stocks.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }

            composable(
                route = Screen.StockDetail.route,
                arguments = listOf(navArgument(Screen.StockDetail.ARG_SYMBOL) { type = NavType.StringType })
            ) { backStackEntry ->
                val symbol = backStackEntry.arguments?.getString(Screen.StockDetail.ARG_SYMBOL)
                if (symbol.isNullOrBlank()) {
                    PlaceholderScreen(
                        title = "Stock not found",
                        description = "No symbol was passed to detail screen."
                    )
                } else {
                    LaunchedEffect(symbol) {
                        viewModel.loadStockDetails(symbol = symbol)
                    }
                    val detailUiState = if (
                        stockDetailsUiState.symbol != symbol &&
                        stockDetailsUiState.stock?.symbol != symbol
                    ) {
                        StockDetailsUiState(symbol = symbol, isLoading = true)
                    } else {
                        stockDetailsUiState
                    }
                    StockDetailScreen(
                        uiState = detailUiState,
                        onBack = { navController.popBackStack() },
                        onRetry = viewModel::retryStockDetails,
                        onToggleFavorite = viewModel::toggleFavoriteFromDetails
                    )
                }
            }
        }
    }
}
