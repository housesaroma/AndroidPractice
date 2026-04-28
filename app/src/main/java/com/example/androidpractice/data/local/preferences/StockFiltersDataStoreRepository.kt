package com.example.androidpractice.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.androidpractice.domain.model.StockFilters
import com.example.androidpractice.domain.repository.StockFiltersRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.stockFiltersDataStore by preferencesDataStore(name = "stock_filters")

class StockFiltersDataStoreRepository(
    private val context: Context
) : StockFiltersRepository {

    override fun observeFilters(): Flow<StockFilters> {
        return context.stockFiltersDataStore.data.map { preferences ->
            preferences.toStockFilters()
        }
    }

    override suspend fun saveFilters(filters: StockFilters) {
        context.stockFiltersDataStore.edit { preferences ->
            preferences[SEARCH_QUERY_KEY] = filters.searchQuery
            val rangePoint = filters.rangePoint
            if (rangePoint == null) {
                preferences.remove(RANGE_POINT_KEY)
            } else {
                preferences[RANGE_POINT_KEY] = rangePoint
            }
            preferences[ONLY_RISING_KEY] = filters.onlyRising
        }
    }

    private fun Preferences.toStockFilters(): StockFilters {
        return StockFilters(
            searchQuery = this[SEARCH_QUERY_KEY].orEmpty(),
            rangePoint = this[RANGE_POINT_KEY],
            onlyRising = this[ONLY_RISING_KEY] ?: false
        )
    }

    private companion object {
        val SEARCH_QUERY_KEY = stringPreferencesKey("search_query")
        val RANGE_POINT_KEY = doublePreferencesKey("range_point")
        val ONLY_RISING_KEY = booleanPreferencesKey("only_rising")
    }
}
