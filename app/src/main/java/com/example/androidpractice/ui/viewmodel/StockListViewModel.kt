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
import com.example.androidpractice.domain.model.StockFilters
import com.example.androidpractice.domain.model.StockQuote
import com.example.androidpractice.domain.usecase.AddFavoriteStockUseCase
import com.example.androidpractice.domain.usecase.GetDefaultStocksUseCase
import com.example.androidpractice.domain.usecase.GetStockDetailsUseCase
import com.example.androidpractice.domain.usecase.ObserveFavoriteSymbolsUseCase
import com.example.androidpractice.domain.usecase.ObserveStockFiltersUseCase
import com.example.androidpractice.domain.usecase.RemoveFavoriteStockUseCase
import com.example.androidpractice.domain.usecase.SearchStocksUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

class StockListViewModel(
    private val getDefaultStocksUseCase: GetDefaultStocksUseCase,
    private val searchStocksUseCase: SearchStocksUseCase,
    private val getStockDetailsUseCase: GetStockDetailsUseCase,
    private val observeStockFiltersUseCase: ObserveStockFiltersUseCase,
    private val observeFavoriteSymbolsUseCase: ObserveFavoriteSymbolsUseCase,
    private val addFavoriteStockUseCase: AddFavoriteStockUseCase,
    private val removeFavoriteStockUseCase: RemoveFavoriteStockUseCase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val remoteStocksCache = mutableMapOf<String, List<StockQuote>>()
    private val detailCache = mutableMapOf<String, com.example.androidpractice.domain.model.StockDetails>()
    private var currentVisibleStocks: List<StockQuote> = emptyList()
    private var currentFilters = StockFilters()

    private val _uiState = MutableStateFlow(StocksUiState(isLoading = true))
    val uiState: StateFlow<StocksUiState> = _uiState.asStateFlow()

    init {
        observeFilters()
        observeFavoriteSymbols()
    }

    fun retryStocks() {
        loadStocks(filters = currentFilters, forceRefresh = true)
    }

    fun toggleFavorite(symbol: String) {
        val normalizedSymbol = symbol.trim().uppercase(Locale.US)
        val isFavorite = _uiState.value.favoriteSymbols.contains(normalizedSymbol)
        viewModelScope.launch(ioDispatcher) {
            if (isFavorite) {
                removeFavoriteStockUseCase(normalizedSymbol)
            } else {
                val target = currentVisibleStocks.firstOrNull {
                    it.symbol.equals(normalizedSymbol, ignoreCase = true)
                } ?: return@launch
                addFavoriteStockUseCase(target.toFavoriteStock())
            }
        }
    }

    private fun observeFilters() {
        viewModelScope.launch {
            observeStockFiltersUseCase().collect { filters ->
                currentFilters = filters
                _uiState.update {
                    it.copy(filtersSummaryText = filters.toSummaryText())
                }
                loadStocks(filters = filters, forceRefresh = false)
            }
        }
    }

    private fun observeFavoriteSymbols() {
        viewModelScope.launch {
            observeFavoriteSymbolsUseCase().collect { symbols ->
                _uiState.update { state ->
                    state.copy(
                        favoriteSymbols = symbols,
                        stocks = currentVisibleStocks.toStockListItemUiModels(symbols)
                    )
                }
            }
        }
    }

    private fun loadStocks(filters: StockFilters, forceRefresh: Boolean) {
        val query = filters.searchQuery.trim()
        val cacheKey = query.lowercase(Locale.US)
        val cachedRemote = remoteStocksCache[cacheKey]
        val shouldUseCacheOnly = cachedRemote != null && !forceRefresh

        if (shouldUseCacheOnly && filters.rangePoint == null) {
            val visibleStocks = applyLocalFilters(cachedRemote, filters)
            currentVisibleStocks = visibleStocks
            _uiState.update {
                it.copy(
                    isLoading = false,
                    stocks = visibleStocks.toStockListItemUiModels(it.favoriteSymbols),
                    filtersSummaryText = filters.toSummaryText(),
                    errorMessage = null,
                    fromCache = true
                )
            }
            return
        }

        val loadingStocks = if (cachedRemote != null) {
            applyLocalFilters(cachedRemote, filters)
        } else {
            currentVisibleStocks
        }
        currentVisibleStocks = loadingStocks

        _uiState.update {
            it.copy(
                isLoading = true,
                stocks = loadingStocks.toStockListItemUiModels(it.favoriteSymbols),
                filtersSummaryText = filters.toSummaryText(),
                errorMessage = null,
                fromCache = cachedRemote != null
            )
        }

        viewModelScope.launch(ioDispatcher) {
            val remoteStocksResult = if (shouldUseCacheOnly) {
                runCatching { cachedRemote.orEmpty() }
            } else {
                runCatching {
                    if (query.isBlank()) {
                        getDefaultStocksUseCase()
                    } else {
                        searchStocksUseCase(query)
                    }
                }
            }

            remoteStocksResult.onSuccess { remoteStocks ->
                if (!shouldUseCacheOnly) {
                    remoteStocksCache[cacheKey] = remoteStocks
                }
                val enrichedStocks = enrichWithRangeDataIfNeeded(remoteStocks, filters)
                val visibleStocks = applyLocalFilters(enrichedStocks, filters)
                currentVisibleStocks = visibleStocks
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        stocks = visibleStocks.toStockListItemUiModels(it.favoriteSymbols),
                        filtersSummaryText = filters.toSummaryText(),
                        errorMessage = null,
                        fromCache = shouldUseCacheOnly
                    )
                }
            }.onFailure { throwable ->
                val visibleStocks = if (cachedRemote != null) {
                    applyLocalFilters(cachedRemote, filters)
                } else {
                    currentVisibleStocks
                }
                currentVisibleStocks = visibleStocks
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        stocks = visibleStocks.toStockListItemUiModels(it.favoriteSymbols),
                        filtersSummaryText = filters.toSummaryText(),
                        errorMessage = throwable.toReadableStockMessage(),
                        fromCache = cachedRemote != null
                    )
                }
            }
        }
    }

    private suspend fun enrichWithRangeDataIfNeeded(
        stocks: List<StockQuote>,
        filters: StockFilters
    ): List<StockQuote> {
        if (filters.rangePoint == null) return stocks

        return stocks.map { stock ->
            if (stock.week52High != null && stock.week52Low != null) {
                stock
            } else {
                val details = detailCache[stock.symbol] ?: runCatching {
                    getStockDetailsUseCase(stock.symbol)
                }.getOrNull()?.also { detailCache[stock.symbol] = it }

                stock.copy(
                    week52High = details?.week52High,
                    week52Low = details?.week52Low
                )
            }
        }
    }

    private fun applyLocalFilters(stocks: List<StockQuote>, filters: StockFilters): List<StockQuote> {
        return stocks.filter { stock ->
            val passesRising = if (filters.onlyRising) {
                (stock.change ?: 0.0) > 0
            } else {
                true
            }

            val passesRange = filters.rangePoint?.let { point ->
                val low = stock.week52Low
                val high = stock.week52High
                if (low == null || high == null) {
                    false
                } else {
                    point in low..high
                }
            } ?: true

            passesRising && passesRange
        }
    }

    private fun StockQuote.toFavoriteStock(): FavoriteStock {
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
                val filtersRepository = AppContainer.provideStockFiltersRepository(application)
                val favoritesRepository = AppContainer.provideFavoriteStocksRepository(application)

                StockListViewModel(
                    getDefaultStocksUseCase = GetDefaultStocksUseCase(stocksRepository),
                    searchStocksUseCase = SearchStocksUseCase(stocksRepository),
                    getStockDetailsUseCase = GetStockDetailsUseCase(stocksRepository),
                    observeStockFiltersUseCase = ObserveStockFiltersUseCase(filtersRepository),
                    observeFavoriteSymbolsUseCase = ObserveFavoriteSymbolsUseCase(favoritesRepository),
                    addFavoriteStockUseCase = AddFavoriteStockUseCase(favoritesRepository),
                    removeFavoriteStockUseCase = RemoveFavoriteStockUseCase(favoritesRepository)
                )
            }
        }

        private fun CreationExtras.requireApplication(): Application {
            return checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) {
                "Application is required to build StockListViewModel"
            }
        }
    }
}
