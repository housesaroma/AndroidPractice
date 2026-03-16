package com.example.androidpractice.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.androidpractice.data.model.Stock
import com.example.androidpractice.data.repository.MockStockRepository
import com.example.androidpractice.data.repository.StockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StocksViewModel(
    private val repository: StockRepository = MockStockRepository()
) : ViewModel() {
    private val _stocks = MutableStateFlow(repository.getStocks())
    val stocks: StateFlow<List<Stock>> = _stocks.asStateFlow()

    fun getStock(symbol: String): Stock? = repository.getStock(symbol)
}
