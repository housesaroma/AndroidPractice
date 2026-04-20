package com.example.androidpractice.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_stocks")
data class FavoriteStockEntity(
    @PrimaryKey val symbol: String,
    val name: String,
    val exchange: String,
    val currency: String,
    val price: Double?,
    val change: Double?,
    val changePercent: Double?,
    val addedAt: Long
)
