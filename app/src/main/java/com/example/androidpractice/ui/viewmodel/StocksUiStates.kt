package com.example.androidpractice.ui.viewmodel

import com.example.androidpractice.ui.model.FavoriteStockItemUiModel
import com.example.androidpractice.ui.model.StockDetailsUiModel
import com.example.androidpractice.ui.model.StockListItemUiModel

data class StocksUiState(
    val isLoading: Boolean = false,
    val stocks: List<StockListItemUiModel> = emptyList(),
    val favoriteSymbols: Set<String> = emptySet(),
    val filtersSummaryText: String = "",
    val errorMessage: String? = null,
    val fromCache: Boolean = false
)

data class StockDetailsUiState(
    val symbol: String = "",
    val isLoading: Boolean = false,
    val stock: StockDetailsUiModel? = null,
    val isFavorite: Boolean = false,
    val errorMessage: String? = null,
    val fromCache: Boolean = false
)

data class SettingsUiState(
    val searchQuery: String = "",
    val rangePointInput: String = "",
    val onlyRising: Boolean = false,
    val rangeInputError: String? = null,
    val hasActiveFilters: Boolean = false
)

data class FavoritesUiState(
    val isLoading: Boolean = true,
    val favorites: List<FavoriteStockItemUiModel> = emptyList()
)
