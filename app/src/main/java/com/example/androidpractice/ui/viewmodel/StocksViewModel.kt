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
import com.example.androidpractice.domain.model.StockFilters
import com.example.androidpractice.domain.model.StockQuote
import com.example.androidpractice.domain.usecase.AddFavoriteStockUseCase
import com.example.androidpractice.domain.usecase.GetDefaultStocksUseCase
import com.example.androidpractice.domain.usecase.GetStockDetailsUseCase
import com.example.androidpractice.domain.usecase.ObserveFavoriteStocksUseCase
import com.example.androidpractice.domain.usecase.ObserveFavoriteSymbolsUseCase
import com.example.androidpractice.domain.usecase.ObserveStockFiltersUseCase
import com.example.androidpractice.domain.usecase.RemoveFavoriteStockUseCase
import com.example.androidpractice.domain.usecase.SaveStockFiltersUseCase
import com.example.androidpractice.domain.usecase.SearchStocksUseCase
import com.example.androidpractice.ui.cache.SettingsBadgeCache
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class StocksUiState(
    val isLoading: Boolean = false,
    val stocks: List<StockQuote> = emptyList(),
    val favoriteSymbols: Set<String> = emptySet(),
    val activeFilters: StockFilters = StockFilters(),
    val errorMessage: String? = null,
    val fromCache: Boolean = false
)

data class StockDetailsUiState(
    val symbol: String = "",
    val isLoading: Boolean = false,
    val stock: StockDetails? = null,
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
    val favorites: List<FavoriteStock> = emptyList()
)

class StocksViewModel(
    private val getDefaultStocksUseCase: GetDefaultStocksUseCase,
    private val searchStocksUseCase: SearchStocksUseCase,
    private val getStockDetailsUseCase: GetStockDetailsUseCase,
    private val observeStockFiltersUseCase: ObserveStockFiltersUseCase,
    private val saveStockFiltersUseCase: SaveStockFiltersUseCase,
    private val observeFavoriteStocksUseCase: ObserveFavoriteStocksUseCase,
    private val observeFavoriteSymbolsUseCase: ObserveFavoriteSymbolsUseCase,
    private val addFavoriteStockUseCase: AddFavoriteStockUseCase,
    private val removeFavoriteStockUseCase: RemoveFavoriteStockUseCase,
    private val settingsBadgeCache: SettingsBadgeCache,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val remoteStocksCache = mutableMapOf<String, List<StockQuote>>()
    private val detailCache = mutableMapOf<String, StockDetails>()
    private var currentFilters = StockFilters()

    private val _stocksUiState = MutableStateFlow(StocksUiState(isLoading = true))
    val stocksUiState: StateFlow<StocksUiState> = _stocksUiState.asStateFlow()

    private val _stockDetailsUiState = MutableStateFlow(StockDetailsUiState())
    val stockDetailsUiState: StateFlow<StockDetailsUiState> = _stockDetailsUiState.asStateFlow()

    private val _settingsUiState = MutableStateFlow(SettingsUiState())
    val settingsUiState: StateFlow<SettingsUiState> = _settingsUiState.asStateFlow()

    private val _favoritesUiState = MutableStateFlow(FavoritesUiState())
    val favoritesUiState: StateFlow<FavoritesUiState> = _favoritesUiState.asStateFlow()

    val showSettingsBadge: StateFlow<Boolean> = settingsBadgeCache.showBadge

    init {
        observeFilters()
        observeFavoriteStocks()
        observeFavoriteSymbols()
    }

    fun onSettingsSearchQueryChange(value: String) {
        _settingsUiState.update {
            it.copy(
                searchQuery = value,
                rangeInputError = null
            )
        }
    }

    fun onSettingsRangePointChange(value: String) {
        val sanitized = value.filter { char ->
            char.isDigit() || char == '.' || char == ','
        }
        _settingsUiState.update {
            it.copy(
                rangePointInput = sanitized,
                rangeInputError = null
            )
        }
    }

    fun onSettingsOnlyRisingChange(value: Boolean) {
        _settingsUiState.update { it.copy(onlyRising = value) }
    }

    fun onSettingsDone(): Boolean {
        val state = _settingsUiState.value
        val (isValid, rangePoint) = parseRangePoint(state.rangePointInput)
        if (!isValid) return false

        val filters = StockFilters(
            searchQuery = state.searchQuery.trim(),
            rangePoint = rangePoint,
            onlyRising = state.onlyRising
        )

        viewModelScope.launch(ioDispatcher) {
            saveStockFiltersUseCase(filters)
        }
        return true
    }

    fun resetFilters() {
        val defaults = StockFilters()
        _settingsUiState.value = defaults.toSettingsUiState()
        viewModelScope.launch(ioDispatcher) {
            saveStockFiltersUseCase(defaults)
        }
    }

    fun retryStocks() {
        loadStocks(filters = currentFilters, forceRefresh = true)
    }

    fun retryStockDetails() {
        val symbol = _stockDetailsUiState.value.symbol
        if (symbol.isBlank()) return
        loadStockDetails(symbol = symbol, forceRefresh = true)
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
                isFavorite = _stocksUiState.value.favoriteSymbols.contains(normalizedSymbol),
                errorMessage = null,
                fromCache = true
            )
            return
        }

        _stockDetailsUiState.value = StockDetailsUiState(
            symbol = normalizedSymbol,
            isLoading = true,
            stock = cached,
            isFavorite = _stocksUiState.value.favoriteSymbols.contains(normalizedSymbol),
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
                    isFavorite = _stocksUiState.value.favoriteSymbols.contains(normalizedSymbol),
                    errorMessage = null,
                    fromCache = false
                )
            }.onFailure { throwable ->
                _stockDetailsUiState.value = StockDetailsUiState(
                    symbol = normalizedSymbol,
                    isLoading = false,
                    stock = cached,
                    isFavorite = _stocksUiState.value.favoriteSymbols.contains(normalizedSymbol),
                    errorMessage = throwable.toReadableMessage(),
                    fromCache = cached != null
                )
            }
        }
    }

    fun toggleFavoriteFromQuote(stock: StockQuote) {
        val isFavorite = _stocksUiState.value.favoriteSymbols.contains(stock.symbol)
        viewModelScope.launch(ioDispatcher) {
            if (isFavorite) {
                removeFavoriteStockUseCase(stock.symbol)
            } else {
                addFavoriteStockUseCase(stock.toFavoriteStock())
            }
        }
    }

    fun toggleFavoriteFromDetails() {
        val details = _stockDetailsUiState.value.stock ?: return
        val symbol = details.symbol
        val isFavorite = _stocksUiState.value.favoriteSymbols.contains(symbol)

        viewModelScope.launch(ioDispatcher) {
            if (isFavorite) {
                removeFavoriteStockUseCase(symbol)
            } else {
                addFavoriteStockUseCase(details.toFavoriteStock())
            }
        }
    }

    fun removeFavorite(symbol: String) {
        viewModelScope.launch(ioDispatcher) {
            removeFavoriteStockUseCase(symbol)
        }
    }

    private fun observeFilters() {
        viewModelScope.launch {
            observeStockFiltersUseCase().collect { filters ->
                currentFilters = filters
                settingsBadgeCache.setShowBadge(!filters.isDefault())

                _settingsUiState.value = filters.toSettingsUiState()
                _stocksUiState.update {
                    it.copy(activeFilters = filters)
                }

                loadStocks(filters = filters, forceRefresh = false)
            }
        }
    }

    private fun observeFavoriteStocks() {
        viewModelScope.launch {
            observeFavoriteStocksUseCase().collect { favorites ->
                _favoritesUiState.value = FavoritesUiState(
                    isLoading = false,
                    favorites = favorites
                )
            }
        }
    }

    private fun observeFavoriteSymbols() {
        viewModelScope.launch {
            observeFavoriteSymbolsUseCase().collect { symbols ->
                _stocksUiState.update { state ->
                    state.copy(favoriteSymbols = symbols)
                }
                _stockDetailsUiState.update { state ->
                    state.copy(isFavorite = symbols.contains(state.symbol))
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
            _stocksUiState.update {
                it.copy(
                    isLoading = false,
                    stocks = applyLocalFilters(cachedRemote, filters),
                    activeFilters = filters,
                    errorMessage = null,
                    fromCache = true
                )
            }
            return
        }

        _stocksUiState.update {
            it.copy(
                isLoading = true,
                stocks = if (cachedRemote != null) applyLocalFilters(cachedRemote, filters) else it.stocks,
                activeFilters = filters,
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
                _stocksUiState.update {
                    it.copy(
                        isLoading = false,
                        stocks = applyLocalFilters(enrichedStocks, filters),
                        activeFilters = filters,
                        errorMessage = null,
                        fromCache = shouldUseCacheOnly
                    )
                }
            }.onFailure { throwable ->
                _stocksUiState.update {
                    it.copy(
                        isLoading = false,
                        stocks = if (cachedRemote != null) applyLocalFilters(cachedRemote, filters) else it.stocks,
                        activeFilters = filters,
                        errorMessage = throwable.toReadableMessage(),
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

    private fun parseRangePoint(value: String): Pair<Boolean, Double?> {
        val text = value.trim()
        if (text.isBlank()) {
            _settingsUiState.update { it.copy(rangeInputError = null) }
            return true to null
        }

        val parsed = text.replace(',', '.').toDoubleOrNull()
        if (parsed == null || parsed < 0) {
            _settingsUiState.update {
                it.copy(rangeInputError = "Enter a valid positive number")
            }
            return false to null
        }

        _settingsUiState.update { it.copy(rangeInputError = null) }
        return true to parsed
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

    private fun StockFilters.toSettingsUiState(): SettingsUiState {
        return SettingsUiState(
            searchQuery = searchQuery,
            rangePointInput = rangePoint?.let { toInputString(it) }.orEmpty(),
            onlyRising = onlyRising,
            rangeInputError = null,
            hasActiveFilters = !isDefault()
        )
    }

    private fun toInputString(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            value.toString()
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
                val filtersRepository = AppContainer.provideStockFiltersRepository(application)
                val favoritesRepository = AppContainer.provideFavoriteStocksRepository(application)
                val badgeCache = AppContainer.provideSettingsBadgeCache()

                StocksViewModel(
                    getDefaultStocksUseCase = GetDefaultStocksUseCase(stocksRepository),
                    searchStocksUseCase = SearchStocksUseCase(stocksRepository),
                    getStockDetailsUseCase = GetStockDetailsUseCase(stocksRepository),
                    observeStockFiltersUseCase = ObserveStockFiltersUseCase(filtersRepository),
                    saveStockFiltersUseCase = SaveStockFiltersUseCase(filtersRepository),
                    observeFavoriteStocksUseCase = ObserveFavoriteStocksUseCase(favoritesRepository),
                    observeFavoriteSymbolsUseCase = ObserveFavoriteSymbolsUseCase(favoritesRepository),
                    addFavoriteStockUseCase = AddFavoriteStockUseCase(favoritesRepository),
                    removeFavoriteStockUseCase = RemoveFavoriteStockUseCase(favoritesRepository),
                    settingsBadgeCache = badgeCache
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
