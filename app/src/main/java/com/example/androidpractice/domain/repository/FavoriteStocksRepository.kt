package com.example.androidpractice.domain.repository

import com.example.androidpractice.domain.model.FavoriteStock
import kotlinx.coroutines.flow.Flow

interface FavoriteStocksRepository {
    fun observeFavorites(): Flow<List<FavoriteStock>>
    fun observeFavoriteSymbols(): Flow<Set<String>>
    suspend fun addFavorite(stock: FavoriteStock)
    suspend fun removeFavorite(symbol: String)
}
