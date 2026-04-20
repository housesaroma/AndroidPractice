package com.example.androidpractice.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.androidpractice.data.di.AppContainer
import com.example.androidpractice.domain.model.FavoriteStock
import com.example.androidpractice.domain.model.StockDetails
import com.example.androidpractice.domain.usecase.AddFavoriteStockUseCase
import com.example.androidpractice.domain.usecase.GetStockDetailsUseCase
import com.example.androidpractice.domain.usecase.ObserveFavoriteSymbolsUseCase
import com.example.androidpractice.domain.usecase.RemoveFavoriteStockUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

class StockDetailViewModel(
    private val getStockDetailsUseCase: GetStockDetailsUseCase,
    private val observeFavoriteSymbolsUseCase: ObserveFavoriteSymbolsUseCase,
    private val addFavoriteStockUseCase: AddFavoriteStockUseCase,
    private val removeFavoriteStockUseCase: RemoveFavoriteStockUseCase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val detailCache = mutableMapOf<String, StockDetails>()
    private var currentDetailsStock: StockDetails? = null

    private val _uiState = MutableStateFlow(StockDetailsUiState())
    val uiState: StateFlow<StockDetailsUiState> = _uiState.asStateFlow()

    init {
        observeFavoriteSymbols()
    }

    fun retryStockDetails() {
        val symbol = _uiState.value.symbol
        if (symbol.isBlank()) return
        loadStockDetails(symbol = symbol, forceRefresh = true)
    }

    fun loadStockDetails(symbol: String, forceRefresh: Boolean = false) {
        val normalizedSymbol = symbol.trim().uppercase(Locale.US)
        if (normalizedSymbol.isBlank()) return

        val cached = detailCache[normalizedSymbol]
        if (cached != null && !forceRefresh) {
            currentDetailsStock = cached
            _uiState.value = StockDetailsUiState(
                symbol = normalizedSymbol,
                isLoading = false,
                stock = cached.toStockDetailsUiModel(),
                isFavorite = _uiState.value.isFavorite,
                errorMessage = null,
                fromCache = true
            )
            return
        }

        currentDetailsStock = cached
        _uiState.value = StockDetailsUiState(
            symbol = normalizedSymbol,
            isLoading = true,
            stock = cached?.toStockDetailsUiModel(),
            isFavorite = _uiState.value.isFavorite,
            errorMessage = null,
            fromCache = cached != null
        )

        viewModelScope.launch(ioDispatcher) {
            runCatching {
                getStockDetailsUseCase(normalizedSymbol)
            }.onSuccess { details ->
                detailCache[normalizedSymbol] = details
                currentDetailsStock = details
                _uiState.value = StockDetailsUiState(
                    symbol = normalizedSymbol,
                    isLoading = false,
                    stock = details.toStockDetailsUiModel(),
                    isFavorite = _uiState.value.isFavorite,
                    errorMessage = null,
                    fromCache = false
                )
            }.onFailure { throwable ->
                currentDetailsStock = cached
                _uiState.value = StockDetailsUiState(
                    symbol = normalizedSymbol,
                    isLoading = false,
                    stock = cached?.toStockDetailsUiModel(),
                    isFavorite = _uiState.value.isFavorite,
                    errorMessage = throwable.toReadableStockMessage(),
                    fromCache = cached != null
                )
            }
        }
    }

    fun toggleFavorite() {
        val details = currentDetailsStock ?: return
        val symbol = details.symbol
        val isFavorite = _uiState.value.isFavorite

        viewModelScope.launch(ioDispatcher) {
            if (isFavorite) {
                removeFavoriteStockUseCase(symbol)
            } else {
                addFavoriteStockUseCase(details.toFavoriteStock())
            }
        }
    }

    private fun observeFavoriteSymbols() {
        viewModelScope.launch {
            observeFavoriteSymbolsUseCase().collect { symbols ->
                _uiState.update { state ->
                    state.copy(isFavorite = symbols.contains(state.symbol))
                }
            }
        }
    }

    private fun StockDetails.toFavoriteStock(): FavoriteStock {
        return FavoriteStock(
            symbol = symbol,
            name = name,
            exchange = exchange,
            currency = currency,
            price = price,
            change = change,
            changePercent = changePercent,
            addedAt = System.currentTimeMillis()
        )
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this.requireApplication()
                val stocksRepository = AppContainer.provideStocksRepository(application)
                val favoritesRepository = AppContainer.provideFavoriteStocksRepository(application)

                StockDetailViewModel(
                    getStockDetailsUseCase = GetStockDetailsUseCase(stocksRepository),
                    observeFavoriteSymbolsUseCase = ObserveFavoriteSymbolsUseCase(favoritesRepository),
                    addFavoriteStockUseCase = AddFavoriteStockUseCase(favoritesRepository),
                    removeFavoriteStockUseCase = RemoveFavoriteStockUseCase(favoritesRepository)
                )
            }
        }

        private fun CreationExtras.requireApplication(): Application {
            return checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) {
                "Application is required to build StockDetailViewModel"
            }
        }
    }
}
