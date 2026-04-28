package com.example.androidpractice.domain.usecase

import com.example.androidpractice.domain.model.StockFilters
import com.example.androidpractice.domain.repository.StockFiltersRepository
import kotlinx.coroutines.flow.Flow

class ObserveStockFiltersUseCase(
    private val repository: StockFiltersRepository
) {
    operator fun invoke(): Flow<StockFilters> = repository.observeFilters()
}
