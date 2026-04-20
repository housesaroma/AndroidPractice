package com.example.androidpractice.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.androidpractice.data.repository.MockStockRepository
import com.example.androidpractice.data.repository.StockRepository
import com.example.androidpractice.ui.model.StockListItemUiModel
import com.example.androidpractice.ui.model.toListItemUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StockListViewModel(
    private val repository: StockRepository = MockStockRepository()
) : ViewModel() {
    private val _stocks = MutableStateFlow(repository.getStocks().map { it.toListItemUiModel() })
    val stocks: StateFlow<List<StockListItemUiModel>> = _stocks.asStateFlow()
}
