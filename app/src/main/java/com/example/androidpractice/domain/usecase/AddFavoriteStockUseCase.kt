package com.example.androidpractice.domain.usecase

import com.example.androidpractice.domain.model.FavoriteStock
import com.example.androidpractice.domain.repository.FavoriteStocksRepository

class AddFavoriteStockUseCase(
    private val repository: FavoriteStocksRepository
) {
    suspend operator fun invoke(stock: FavoriteStock) {
        repository.addFavorite(stock)
    }
}
