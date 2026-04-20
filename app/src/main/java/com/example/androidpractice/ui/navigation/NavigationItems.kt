package com.example.androidpractice.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Stocks : Screen("stocks")
    data object Portfolio : Screen("portfolio")
    data object Settings : Screen("settings")

    data object StockDetail : Screen("stock/{symbol}") {
        const val ARG_SYMBOL = "symbol"
        fun createRoute(symbol: String): String = "stock/$symbol"
    }
}

enum class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
) {
    Stocks(
        screen = Screen.Stocks,
        label = "Stocks",
        icon = Icons.AutoMirrored.Filled.ShowChart
    ),
    Portfolio(
        screen = Screen.Portfolio,
        label = "Portfolio",
        icon = Icons.Filled.AccountBalanceWallet
    ),
    Settings(
        screen = Screen.Settings,
        label = "Settings",
        icon = Icons.Filled.Settings
    )
}
