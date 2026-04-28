package com.example.androidpractice.domain.usecase

import com.example.androidpractice.domain.repository.FavoriteStocksRepository

class RemoveFavoriteStockUseCase(
    private val repository: FavoriteStocksRepository
) {
    suspend operator fun invoke(symbol: String) {
        repository.removeFavorite(symbol)
    }
}
