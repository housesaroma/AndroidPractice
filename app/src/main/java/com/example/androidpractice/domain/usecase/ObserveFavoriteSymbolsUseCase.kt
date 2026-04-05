package com.example.androidpractice.domain.usecase

import com.example.androidpractice.domain.repository.FavoriteStocksRepository
import kotlinx.coroutines.flow.Flow

class ObserveFavoriteSymbolsUseCase(
    private val repository: FavoriteStocksRepository
) {
    operator fun invoke(): Flow<Set<String>> = repository.observeFavoriteSymbols()
}
