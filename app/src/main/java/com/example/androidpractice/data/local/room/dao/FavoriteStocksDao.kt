package com.example.androidpractice.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.androidpractice.data.local.room.entity.FavoriteStockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteStocksDao {
    @Query("SELECT * FROM favorite_stocks ORDER BY addedAt DESC")
    fun observeFavorites(): Flow<List<FavoriteStockEntity>>

    @Query("SELECT symbol FROM favorite_stocks")
    fun observeFavoriteSymbols(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFavorite(stock: FavoriteStockEntity)

    @Query("DELETE FROM favorite_stocks WHERE symbol = :symbol")
    suspend fun deleteFavoriteBySymbol(symbol: String)
}
