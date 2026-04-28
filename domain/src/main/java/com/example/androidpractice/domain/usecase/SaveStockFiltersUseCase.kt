package com.example.androidpractice.domain.usecase

import com.example.androidpractice.domain.model.StockFilters
import com.example.androidpractice.domain.repository.StockFiltersRepository

class SaveStockFiltersUseCase(
    private val repository: StockFiltersRepository
) {
    suspend operator fun invoke(filters: StockFilters) {
        repository.saveFilters(filters)
    }
}
