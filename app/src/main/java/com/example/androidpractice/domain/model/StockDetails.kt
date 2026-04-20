package com.example.androidpractice.domain.model

data class StockDetails(
    val symbol: String,
    val name: String,
    val exchange: String,
    val currency: String,
    val price: Double?,
    val change: Double?,
    val changePercent: Double?,
    val dayHigh: Double?,
    val dayLow: Double?,
    val week52High: Double?,
    val week52Low: Double?,
    val marketCap: Long?,
    val volume: Long?,
    val peRatio: Double?,
    val eps: Double?,
    val dividendYield: Double?,
    val sector: String,
    val industry: String,
    val ceo: String,
    val headquarters: String,
    val description: String
)
