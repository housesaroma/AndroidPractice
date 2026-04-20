package com.example.androidpractice.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.androidpractice.data.di.AppContainer
import com.example.androidpractice.domain.model.StockFilters
import com.example.androidpractice.domain.usecase.ObserveStockFiltersUseCase
import com.example.androidpractice.domain.usecase.SaveStockFiltersUseCase
import com.example.androidpractice.ui.cache.SettingsBadgeCache
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val observeStockFiltersUseCase: ObserveStockFiltersUseCase,
    private val saveStockFiltersUseCase: SaveStockFiltersUseCase,
    private val settingsBadgeCache: SettingsBadgeCache,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val showSettingsBadge: StateFlow<Boolean> = settingsBadgeCache.showBadge

    init {
        observeFilters()
    }

    fun onSettingsSearchQueryChange(value: String) {
        _uiState.update {
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
        _uiState.update {
            it.copy(
                rangePointInput = sanitized,
                rangeInputError = null
            )
        }
    }

    fun onSettingsOnlyRisingChange(value: Boolean) {
        _uiState.update { it.copy(onlyRising = value) }
    }

    fun onSettingsDone(): Boolean {
        val state = _uiState.value
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
        _uiState.value = defaults.toSettingsUiState()
        viewModelScope.launch(ioDispatcher) {
            saveStockFiltersUseCase(defaults)
        }
    }

    private fun observeFilters() {
        viewModelScope.launch {
            observeStockFiltersUseCase().collect { filters ->
                settingsBadgeCache.setShowBadge(!filters.isDefault())
                _uiState.value = filters.toSettingsUiState()
            }
        }
    }

    private fun parseRangePoint(value: String): Pair<Boolean, Double?> {
        val text = value.trim()
        if (text.isBlank()) {
            _uiState.update { it.copy(rangeInputError = null) }
            return true to null
        }

        val parsed = text.replace(',', '.').toDoubleOrNull()
        if (parsed == null || parsed < 0) {
            _uiState.update {
                it.copy(rangeInputError = "Enter a valid positive number")
            }
            return false to null
        }

        _uiState.update { it.copy(rangeInputError = null) }
        return true to parsed
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

    companion object {
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this.requireApplication()
                val filtersRepository = AppContainer.provideStockFiltersRepository(application)
                val badgeCache = AppContainer.provideSettingsBadgeCache()

                SettingsViewModel(
                    observeStockFiltersUseCase = ObserveStockFiltersUseCase(filtersRepository),
                    saveStockFiltersUseCase = SaveStockFiltersUseCase(filtersRepository),
                    settingsBadgeCache = badgeCache
                )
            }
        }

        private fun CreationExtras.requireApplication(): Application {
            return checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) {
                "Application is required to build SettingsViewModel"
            }
        }
    }
}
