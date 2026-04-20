package com.example.androidpractice.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.androidpractice.data.di.AppContainer
import com.example.androidpractice.domain.usecase.ObserveFavoriteStocksUseCase
import com.example.androidpractice.domain.usecase.RemoveFavoriteStockUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val observeFavoriteStocksUseCase: ObserveFavoriteStocksUseCase,
    private val removeFavoriteStockUseCase: RemoveFavoriteStockUseCase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeFavoriteStocksUseCase().collect { favorites ->
                _uiState.value = FavoritesUiState(
                    isLoading = false,
                    favorites = favorites.map { it.toFavoriteStockItemUiModel() }
                )
            }
        }
    }

    fun removeFavorite(symbol: String) {
        viewModelScope.launch(ioDispatcher) {
            removeFavoriteStockUseCase(symbol)
        }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this.requireApplication()
                val favoritesRepository = AppContainer.provideFavoriteStocksRepository(application)

                FavoritesViewModel(
                    observeFavoriteStocksUseCase = ObserveFavoriteStocksUseCase(favoritesRepository),
                    removeFavoriteStockUseCase = RemoveFavoriteStockUseCase(favoritesRepository)
                )
            }
        }

        private fun CreationExtras.requireApplication(): Application {
            return checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) {
                "Application is required to build FavoritesViewModel"
            }
        }
    }
}
