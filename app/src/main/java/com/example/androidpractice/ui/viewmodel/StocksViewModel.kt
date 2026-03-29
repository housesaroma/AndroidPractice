package com.example.androidpractice.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import com.example.androidpractice.data.di.AppContainer
import com.example.androidpractice.domain.model.StockDetails
import com.example.androidpractice.domain.model.StockQuote
import com.example.androidpractice.domain.usecase.GetDefaultStocksUseCase
import com.example.androidpractice.domain.usecase.GetStockDetailsUseCase
import com.example.androidpractice.domain.usecase.SearchStocksUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

data class StocksUiState(
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val stocks: List<StockQuote> = emptyList(),
    val errorMessage: String? = null,
    val fromCache: Boolean = false
)

data class StockDetailsUiState(
    val symbol: String = "",
    val isLoading: Boolean = false,
    val stock: StockDetails? = null,
    val errorMessage: String? = null,
    val fromCache: Boolean = false
)

class StocksViewModel(
    private val getDefaultStocksUseCase: GetDefaultStocksUseCase,
    private val searchStocksUseCase: SearchStocksUseCase,
    private val getStockDetailsUseCase: GetStockDetailsUseCase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val listCache = mutableMapOf<String, List<StockQuote>>()
    private val detailCache = mutableMapOf<String, StockDetails>()

    private val _stocksUiState = MutableStateFlow(StocksUiState(isLoading = true))
    val stocksUiState: StateFlow<StocksUiState> = _stocksUiState.asStateFlow()

    private val _stockDetailsUiState = MutableStateFlow(StockDetailsUiState())
    val stockDetailsUiState: StateFlow<StockDetailsUiState> = _stockDetailsUiState.asStateFlow()

    init {
        loadStocks(forceRefresh = true)
    }

    fun onSearchQueryChange(value: String) {
        _stocksUiState.update { it.copy(searchQuery = value) }
    }

    fun submitSearch() {
        loadStocks(forceRefresh = true)
    }

    fun clearSearchAndReload() {
        _stocksUiState.update { it.copy(searchQuery = "") }
        loadStocks(forceRefresh = true)
    }

    fun retryStocks() {
        loadStocks(forceRefresh = true)
    }

    fun loadStockDetails(symbol: String, forceRefresh: Boolean = false) {
        val normalizedSymbol = symbol.trim().uppercase(Locale.US)
        if (normalizedSymbol.isBlank()) return

        val cached = detailCache[normalizedSymbol]
        if (cached != null && !forceRefresh) {
            _stockDetailsUiState.value = StockDetailsUiState(
                symbol = normalizedSymbol,
                isLoading = false,
                stock = cached,
                errorMessage = null,
                fromCache = true
            )
            return
        }

        _stockDetailsUiState.value = StockDetailsUiState(
            symbol = normalizedSymbol,
            isLoading = true,
            stock = cached,
            errorMessage = null,
            fromCache = cached != null
        )

        viewModelScope.launch(ioDispatcher) {
            runCatching {
                getStockDetailsUseCase(normalizedSymbol)
            }.onSuccess { details ->
                detailCache[normalizedSymbol] = details
                _stockDetailsUiState.value = StockDetailsUiState(
                    symbol = normalizedSymbol,
                    isLoading = false,
                    stock = details,
                    errorMessage = null,
                    fromCache = false
                )
            }.onFailure { throwable ->
                _stockDetailsUiState.value = StockDetailsUiState(
                    symbol = normalizedSymbol,
                    isLoading = false,
                    stock = cached,
                    errorMessage = throwable.toReadableMessage(),
                    fromCache = cached != null
                )
            }
        }
    }

    fun retryStockDetails() {
        val symbol = _stockDetailsUiState.value.symbol
        if (symbol.isBlank()) return
        loadStockDetails(symbol = symbol, forceRefresh = true)
    }

    private fun loadStocks(forceRefresh: Boolean) {
        val query = _stocksUiState.value.searchQuery.trim()
        val cacheKey = query.lowercase(Locale.US)
        val cached = listCache[cacheKey]

        if (cached != null && !forceRefresh) {
            _stocksUiState.update {
                it.copy(
                    isLoading = false,
                    stocks = cached,
                    errorMessage = null,
                    fromCache = true
                )
            }
            return
        }

        _stocksUiState.update {
            it.copy(
                isLoading = true,
                stocks = cached ?: it.stocks,
                errorMessage = null,
                fromCache = cached != null
            )
        }

        viewModelScope.launch(ioDispatcher) {
            runCatching {
                if (query.isBlank()) {
                    getDefaultStocksUseCase()
                } else {
                    searchStocksUseCase(query)
                }
            }.onSuccess { stocks ->
                listCache[cacheKey] = stocks
                _stocksUiState.update {
                    it.copy(
                        isLoading = false,
                        stocks = stocks,
                        errorMessage = null,
                        fromCache = false
                    )
                }
            }.onFailure { throwable ->
                _stocksUiState.update {
                    it.copy(
                        isLoading = false,
                        stocks = cached ?: it.stocks,
                        errorMessage = throwable.toReadableMessage(),
                        fromCache = cached != null
                    )
                }
            }
        }
    }

    private fun Throwable.toReadableMessage(): String {
        val raw = message.orEmpty()
        return when {
            raw.contains("Invalid API call", ignoreCase = true) -> {
                "Alpha Vantage rejected the request. Check symbol or query format."
            }
            raw.contains("API call frequency", ignoreCase = true) || raw.contains("standard API rate limit", ignoreCase = true) -> {
                "API rate limit reached. Wait a bit and retry."
            }
            raw.contains("timeout", ignoreCase = true) -> {
                "Request timed out. Check internet and retry."
            }
            raw.isNotBlank() -> raw
            else -> "Something went wrong while loading data."
        }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this.requireApplication()
                val repository = AppContainer.provideStocksRepository(application)
                StocksViewModel(
                    getDefaultStocksUseCase = GetDefaultStocksUseCase(repository),
                    searchStocksUseCase = SearchStocksUseCase(repository),
                    getStockDetailsUseCase = GetStockDetailsUseCase(repository)
                )
            }
        }

        private fun CreationExtras.requireApplication(): Application {
            return checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) {
                "Application is required to build StocksViewModel"
            }
        }
    }
}
