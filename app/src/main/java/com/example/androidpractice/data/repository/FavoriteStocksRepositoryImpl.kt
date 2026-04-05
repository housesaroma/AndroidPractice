package com.example.androidpractice.data.repository

import com.example.androidpractice.data.local.room.dao.FavoriteStocksDao
import com.example.androidpractice.data.local.room.entity.FavoriteStockEntity
import com.example.androidpractice.domain.model.FavoriteStock
import com.example.androidpractice.domain.repository.FavoriteStocksRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavoriteStocksRepositoryImpl(
    private val dao: FavoriteStocksDao
) : FavoriteStocksRepository {

    override fun observeFavorites(): Flow<List<FavoriteStock>> {
        return dao.observeFavorites().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeFavoriteSymbols(): Flow<Set<String>> {
        return dao.observeFavoriteSymbols().map { symbols ->
            symbols.toSet()
        }
    }

    override suspend fun addFavorite(stock: FavoriteStock) {
        dao.upsertFavorite(stock.toEntity())
    }

    override suspend fun removeFavorite(symbol: String) {
        dao.deleteFavoriteBySymbol(symbol)
    }

    private fun FavoriteStockEntity.toDomain(): FavoriteStock {
        return FavoriteStock(
            symbol = symbol,
            name = name,
            exchange = exchange,
            currency = currency,
            price = price,
            change = change,
            changePercent = changePercent,
            addedAt = addedAt
        )
    }

    private fun FavoriteStock.toEntity(): FavoriteStockEntity {
        return FavoriteStockEntity(
            symbol = symbol,
            name = name,
            exchange = exchange,
            currency = currency,
            price = price,
            change = change,
            changePercent = changePercent,
            addedAt = addedAt
        )
    }
}
