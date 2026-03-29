package com.example.androidpractice.domain.usecase

import com.example.androidpractice.domain.model.StockQuote
import com.example.androidpractice.domain.repository.StocksRepository

class SearchStocksUseCase(
    private val repository: StocksRepository
) {
    suspend operator fun invoke(query: String): List<StockQuote> {
        return repository.searchStocks(query = query)
    }
}
