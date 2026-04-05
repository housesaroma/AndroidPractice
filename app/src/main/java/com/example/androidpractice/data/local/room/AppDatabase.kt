package com.example.androidpractice.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.androidpractice.data.local.room.dao.FavoriteStocksDao
import com.example.androidpractice.data.local.room.entity.FavoriteStockEntity

@Database(
    entities = [FavoriteStockEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteStocksDao(): FavoriteStocksDao
}
