package com.example.androidpractice.domain.repository

import com.example.androidpractice.domain.model.StockDetails
import com.example.androidpractice.domain.model.StockQuote

interface StocksRepository {
    suspend fun getDefaultStocks(): List<StockQuote>
    suspend fun searchStocks(query: String): List<StockQuote>
    suspend fun getStockDetails(symbol: String): StockDetails
}
