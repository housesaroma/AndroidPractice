package com.example.androidpractice.domain.repository

import com.example.androidpractice.domain.model.StockFilters
import kotlinx.coroutines.flow.Flow

interface StockFiltersRepository {
    fun observeFilters(): Flow<StockFilters>
    suspend fun saveFilters(filters: StockFilters)
}
