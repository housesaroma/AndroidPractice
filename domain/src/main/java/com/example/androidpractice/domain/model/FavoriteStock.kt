package com.example.androidpractice.domain.model

data class FavoriteStock(
    val symbol: String,
    val name: String,
    val exchange: String,
    val currency: String,
    val price: Double?,
    val change: Double?,
    val changePercent: Double?,
    val addedAt: Long
)
