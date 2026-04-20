package com.example.androidpractice.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.androidpractice.data.repository.MockStockRepository
import com.example.androidpractice.data.repository.StockRepository
import com.example.androidpractice.ui.model.StockDetailUiModel
import com.example.androidpractice.ui.model.toDetailUiModel

class StockDetailViewModel(
    private val repository: StockRepository = MockStockRepository()
) : ViewModel() {
    fun getStockDetail(symbol: String): StockDetailUiModel? {
        return repository.getStock(symbol)?.toDetailUiModel()
    }
}
