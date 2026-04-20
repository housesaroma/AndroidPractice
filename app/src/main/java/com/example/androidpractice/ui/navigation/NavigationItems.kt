package com.example.androidpractice.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

sealed class Screen(val route: String) {
    data object Stocks : Screen("stocks")
    data object Portfolio : Screen("portfolio")
    data object Settings : Screen("settings")

    data object StockDetail : Screen("stock/{symbol}") {
        const val ARG_SYMBOL = "symbol"
        fun createRoute(symbol: String): String = "stock/$symbol"
    }
}

sealed class BottomNavItem(
    val screen: Screen,
    private val label: String,
    private val icon: @Composable () -> Unit
) {
    data object Stocks : BottomNavItem(
        screen = Screen.Stocks,
        label = "Stocks",
        icon = { Icon(imageVector = Icons.AutoMirrored.Filled.ShowChart, contentDescription = null) }
    )

    data object Portfolio : BottomNavItem(
        screen = Screen.Portfolio,
        label = "Portfolio",
        icon = { Icon(imageVector = Icons.Filled.AccountBalanceWallet, contentDescription = null) }
    )

    data object Settings : BottomNavItem(
        screen = Screen.Settings,
        label = "Settings",
        icon = { Icon(imageVector = Icons.Filled.Settings, contentDescription = null) }
    )

    @Composable
    fun Label() {
        Text(text = label)
    }

    @Composable
    fun Icon() {
        icon()
    }
}
