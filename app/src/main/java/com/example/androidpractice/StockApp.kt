package com.example.androidpractice

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.example.androidpractice.ui.screens.profile.ProfileScreen
import com.example.androidpractice.ui.screens.profile.edit.EditProfileScreen
import com.example.androidpractice.ui.screens.settings.SettingsScreen
import com.example.androidpractice.ui.viewmodel.EditProfileViewModel
import com.example.androidpractice.ui.viewmodel.EditProfileViewModelFactory
import com.example.androidpractice.ui.viewmodel.FavoritesViewModel
import com.example.androidpractice.ui.viewmodel.ProfileEvent
import com.example.androidpractice.ui.viewmodel.ProfileViewModel
import com.example.androidpractice.ui.viewmodel.ProfileViewModelFactory
import com.example.androidpractice.ui.viewmodel.SettingsViewModel
import com.example.androidpractice.ui.viewmodel.StockDetailViewModel
import com.example.androidpractice.ui.viewmodel.StockListViewModel
import com.example.androidpractice.ui.viewmodel.StockDetailsUiState

@Composable
fun StockApp() {
    val navController = rememberNavController()
    val stockListViewModel: StockListViewModel = viewModel(factory = StockListViewModel.factory())
    val stockDetailViewModel: StockDetailViewModel = viewModel(factory = StockDetailViewModel.factory())
    val favoritesViewModel: FavoritesViewModel = viewModel(factory = FavoritesViewModel.factory())
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory())
    val appContext = LocalContext.current.applicationContext
    val profileViewModelFactory = remember(appContext) { ProfileViewModelFactory(appContext) }
    val editProfileViewModelFactory = remember(appContext) { EditProfileViewModelFactory(appContext) }
    val profileViewModel: ProfileViewModel = viewModel(factory = profileViewModelFactory)
    val editProfileViewModel: EditProfileViewModel = viewModel(factory = editProfileViewModelFactory)
    val context = LocalContext.current

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val stocksUiState by stockListViewModel.uiState.collectAsStateWithLifecycle()
    val stockDetailsUiState by stockDetailViewModel.uiState.collectAsStateWithLifecycle()
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val favoritesUiState by favoritesViewModel.uiState.collectAsStateWithLifecycle()
    val showSettingsBadge by settingsViewModel.showSettingsBadge.collectAsStateWithLifecycle()

    val profileUiState by profileViewModel.profileUiState.collectAsStateWithLifecycle()
    val editProfileUiState by editProfileViewModel.uiState.collectAsStateWithLifecycle()

    val bottomItems = listOf(
        BottomNavItem.Stocks,
        BottomNavItem.Favorites,
        BottomNavItem.Settings,
        BottomNavItem.Profile
    )
    val showBottomBar = bottomItems.any { it.screen.route == currentRoute }

    LaunchedEffect(profileViewModel) {
        profileViewModel.events.collect { event ->
            when (event) {
                is ProfileEvent.OpenDownloadedFile -> openDownloadedFile(context, event.uri)
                is ProfileEvent.ShowMessage -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

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
                    onRetry = stockListViewModel::retryStocks,
                    onStockClick = { symbol ->
                        navController.navigate(Screen.StockDetail.createRoute(symbol))
                    },
                    onToggleFavorite = stockListViewModel::toggleFavorite
                )
            }

            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    uiState = favoritesUiState,
                    onRemoveFavorite = favoritesViewModel::removeFavorite
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    uiState = settingsUiState,
                    onQueryChange = settingsViewModel::onSettingsSearchQueryChange,
                    onRangePointChange = settingsViewModel::onSettingsRangePointChange,
                    onOnlyRisingChange = settingsViewModel::onSettingsOnlyRisingChange,
                    onResetFilters = settingsViewModel::resetFilters,
                    onDone = {
                        val success = settingsViewModel.onSettingsDone()
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

            composable(Screen.Profile.route) {
                ProfileScreen(
                    uiState = profileUiState,
                    onEditClick = { navController.navigate(Screen.EditProfile.route) },
                    onResumeClick = profileViewModel::downloadAndOpenResume
                )
            }

            composable(Screen.EditProfile.route) {
                LaunchedEffect(Unit) {
                    editProfileViewModel.startEditing()
                }
                EditProfileScreen(
                    uiState = editProfileUiState,
                    onBackClick = { navController.popBackStack() },
                    onFullNameChange = editProfileViewModel::onFullNameChange,
                    onPositionChange = editProfileViewModel::onPositionChange,
                    onResumeUrlChange = editProfileViewModel::onResumeUrlChange,
                    onFavoriteLessonTimeChange = editProfileViewModel::onFavoriteLessonTimeChange,
                    onFavoriteLessonTimeSelected = editProfileViewModel::onFavoriteLessonTimeSelected,
                    onAvatarUriChange = editProfileViewModel::onAvatarUriChange,
                    onDoneClick = {
                        val saved = editProfileViewModel.saveProfile()
                        if (saved) {
                            navController.popBackStack()
                        }
                    },
                    onStoragePermissionDenied = { navController.popBackStack() }
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
                        stockDetailViewModel.loadStockDetails(symbol = symbol)
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
                        onRetry = stockDetailViewModel::retryStockDetails,
                        onToggleFavorite = stockDetailViewModel::toggleFavorite
                    )
                }
            }
        }
    }
}

private fun openDownloadedFile(context: Context, uriString: String) {
    val uri = Uri.parse(uriString)
    val mimeType = context.contentResolver.getType(uri) ?: "*/*"

    val openFileIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    runCatching {
        context.startActivity(openFileIntent)
    }.onFailure {
        val downloadsIntent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(downloadsIntent) }
    }
}
