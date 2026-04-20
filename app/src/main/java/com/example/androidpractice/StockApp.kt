package com.example.androidpractice

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.navigation.compose.rememberNavController
import com.example.androidpractice.ui.navigation.BottomNavItem
import com.example.androidpractice.ui.navigation.Screen
import com.example.androidpractice.ui.screens.detail.StockDetailScreen
import com.example.androidpractice.ui.screens.list.StockListScreen
import com.example.androidpractice.ui.screens.placeholder.PlaceholderScreen
import com.example.androidpractice.ui.viewmodel.PortfolioViewModel
import com.example.androidpractice.ui.viewmodel.SettingsViewModel
import com.example.androidpractice.ui.viewmodel.StockDetailViewModel
import com.example.androidpractice.ui.viewmodel.StockListViewModel

@Composable
fun StockApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

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
                            icon = { item.Icon() },
                            label = { item.Label() }
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
                val stockListViewModel: StockListViewModel = viewModel()
                val stocks by stockListViewModel.stocks.collectAsState()
                StockListScreen(
                    stocks = stocks,
                    onStockClick = { symbol ->
                        navController.navigate(Screen.StockDetail.createRoute(symbol))
                    }
                )
            }
            composable(Screen.Portfolio.route) {
                val portfolioViewModel: PortfolioViewModel = viewModel()
                PlaceholderScreen(
                    title = portfolioViewModel.title,
                    description = portfolioViewModel.description
                )
            }
            composable(Screen.Settings.route) {
                val settingsViewModel: SettingsViewModel = viewModel()
                PlaceholderScreen(
                    title = settingsViewModel.title,
                    description = settingsViewModel.description
                )
            }
            composable(
                route = Screen.StockDetail.route,
                arguments = listOf(navArgument(Screen.StockDetail.ARG_SYMBOL) { type = NavType.StringType })
            ) { backStackEntry ->
                val stockDetailViewModel: StockDetailViewModel = viewModel()
                val symbol = backStackEntry.arguments?.getString(Screen.StockDetail.ARG_SYMBOL)
                val stock = symbol?.let { stockDetailViewModel.getStockDetail(it) }
                if (stock == null) {
                    PlaceholderScreen(
                        title = "Stock not found",
                        description = "No data for the selected symbol."
                    )
                } else {
                    StockDetailScreen(
                        stock = stock,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
