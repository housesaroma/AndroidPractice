package com.example.androidpractice.domain.model

data class StockQuote(
    val symbol: String,
    val name: String,
    val exchange: String,
    val currency: String,
    val price: Double?,
    val change: Double?,
    val changePercent: Double?,
    val week52High: Double?,
    val week52Low: Double?
)
