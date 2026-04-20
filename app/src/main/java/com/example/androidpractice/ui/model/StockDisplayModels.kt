package com.example.androidpractice.ui.model

enum class StockChangeTrend {
    UP,
    DOWN,
    NEUTRAL
}

data class StockListItemUiModel(
    val symbol: String,
    val name: String,
    val exchange: String,
    val priceText: String,
    val changeText: String,
    val changeTrend: StockChangeTrend,
    val isFavorite: Boolean
)

data class StockDetailsUiModel(
    val symbol: String,
    val name: String,
    val exchangeCurrencyText: String,
    val priceText: String,
    val changeText: String,
    val changeTrend: StockChangeTrend,
    val dayRangeText: String,
    val week52RangeText: String,
    val volumeText: String,
    val marketCapText: String,
    val peRatioText: String,
    val epsText: String,
    val dividendYieldText: String,
    val headquarters: String,
    val sector: String,
    val industry: String,
    val description: String
)

data class FavoriteStockItemUiModel(
    val symbol: String,
    val name: String,
    val exchange: String,
    val priceText: String,
    val changeText: String,
    val changeTrend: StockChangeTrend
)
