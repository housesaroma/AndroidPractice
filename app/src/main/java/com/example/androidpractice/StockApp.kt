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
import com.example.androidpractice.ui.viewmodel.StocksViewModel

@Composable
fun StockApp() {
    val navController = rememberNavController()
    val viewModel: StocksViewModel = viewModel()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val stocks by viewModel.stocks.collectAsState()

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
                StockListScreen(
                    stocks = stocks,
                    onStockClick = { stock ->
                        navController.navigate(Screen.StockDetail.createRoute(stock.symbol))
                    }
                )
            }
            composable(Screen.Portfolio.route) {
                PlaceholderScreen(
                    title = "Portfolio",
                    description = "Placeholder for the next practices."
                )
            }
            composable(Screen.Settings.route) {
                PlaceholderScreen(
                    title = "Settings",
                    description = "Placeholder for the next practices."
                )
            }
            composable(
                route = Screen.StockDetail.route,
                arguments = listOf(navArgument(Screen.StockDetail.ARG_SYMBOL) { type = NavType.StringType })
            ) { backStackEntry ->
                val symbol = backStackEntry.arguments?.getString(Screen.StockDetail.ARG_SYMBOL)
                val stock = symbol?.let { viewModel.getStock(it) }
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
