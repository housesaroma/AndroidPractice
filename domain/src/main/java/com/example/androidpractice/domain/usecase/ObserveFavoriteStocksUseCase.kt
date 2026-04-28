package com.example.androidpractice.domain.usecase

import com.example.androidpractice.domain.model.FavoriteStock
import com.example.androidpractice.domain.repository.FavoriteStocksRepository
import kotlinx.coroutines.flow.Flow

class ObserveFavoriteStocksUseCase(
    private val repository: FavoriteStocksRepository
) {
    operator fun invoke(): Flow<List<FavoriteStock>> = repository.observeFavorites()
}
