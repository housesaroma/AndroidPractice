package com.example.androidpractice.domain.usecase

import com.example.androidpractice.domain.model.StockQuote
import com.example.androidpractice.domain.repository.StocksRepository

class GetDefaultStocksUseCase(
    private val repository: StocksRepository
) {
    suspend operator fun invoke(): List<StockQuote> {
        return repository.getStocksBySymbols(DEFAULT_SYMBOLS)
    }

    private companion object {
        val DEFAULT_SYMBOLS = listOf("AAPL", "MSFT", "NVDA", "AMZN", "GOOGL", "TSLA", "JPM", "KO")
    }
}
