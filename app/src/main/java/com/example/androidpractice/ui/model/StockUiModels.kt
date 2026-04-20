package com.example.androidpractice.ui.model

data class StockListItemUiModel(
    val symbol: String,
    val name: String,
    val exchange: String,
    val priceText: String,
    val changeText: String,
    val isPositiveChange: Boolean
)

data class StockDetailUiModel(
    val symbol: String,
    val name: String,
    val exchangeCurrencyText: String,
    val priceText: String,
    val changeText: String,
    val isPositiveChange: Boolean,
    val dayRangeText: String,
    val week52RangeText: String,
    val volumeText: String,
    val avgVolumeText: String,
    val marketCapText: String,
    val peRatioText: String,
    val epsText: String,
    val dividendYieldText: String,
    val ceo: String,
    val headquarters: String,
    val sector: String,
    val industry: String,
    val description: String
)
