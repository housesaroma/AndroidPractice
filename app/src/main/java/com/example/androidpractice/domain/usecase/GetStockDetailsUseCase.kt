package com.example.androidpractice.domain.usecase

import com.example.androidpractice.domain.model.StockDetails
import com.example.androidpractice.domain.repository.StocksRepository

class GetStockDetailsUseCase(
    private val repository: StocksRepository
) {
    suspend operator fun invoke(symbol: String): StockDetails {
        return repository.getStockDetails(symbol = symbol)
    }
}
