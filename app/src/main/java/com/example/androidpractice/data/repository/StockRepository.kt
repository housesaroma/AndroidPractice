package com.example.androidpractice.data.repository

import com.example.androidpractice.data.model.Stock

interface StockRepository {
    fun getStocks(): List<Stock>
    fun getStock(symbol: String): Stock?
}
