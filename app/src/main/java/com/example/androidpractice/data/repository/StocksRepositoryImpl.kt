package com.example.androidpractice.data.repository

import com.example.androidpractice.data.remote.AlphaVantageApi
import com.example.androidpractice.data.remote.dto.GlobalQuoteDto
import com.example.androidpractice.data.remote.dto.OverviewResponseDto
import com.example.androidpractice.data.remote.dto.SymbolMatchDto
import com.example.androidpractice.data.remote.dto.SymbolSearchResponseDto
import com.example.androidpractice.data.remote.dto.TopMoverItemDto
import com.example.androidpractice.data.remote.dto.TopMoversResponseDto
import com.example.androidpractice.domain.model.StockDetails
import com.example.androidpractice.domain.model.StockQuote
import com.example.androidpractice.domain.repository.StocksRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.Locale

class StocksRepositoryImpl(
    private val api: AlphaVantageApi,
    private val apiKey: String
) : StocksRepository {

    override suspend fun getDefaultStocks(): List<StockQuote> {
        val response = api.getTopGainersLosers(apiKey = apiKey)
        response.throwIfApiError()

        val defaultList = response.mostActivelyTraded
            .orEmpty()
            .ifEmpty { response.topGainers.orEmpty() }
            .ifEmpty { response.topLosers.orEmpty() }
            .mapNotNull { it.toDomainQuote() }
            .take(MAX_DEFAULT_RESULTS)

        if (defaultList.isEmpty()) {
            throw IllegalStateException("Could not load stocks list. Check API key, limits, or internet connection.")
        }

        return defaultList
    }

    override suspend fun searchStocks(query: String): List<StockQuote> = coroutineScope {
        val response = api.searchSymbols(keywords = query, apiKey = apiKey)
        response.throwIfApiError()

        val matches = response.bestMatches.orEmpty().take(MAX_SEARCH_RESULTS)
        matches.mapNotNull { match ->
            val symbol = match.symbol.cleanValue() ?: return@mapNotNull null
            async {
                val baseQuote = match.toDomainQuote()
                val quoteDto = runCatching {
                    api.getGlobalQuote(symbol = symbol, apiKey = apiKey).quote
                }.getOrNull()

                if (quoteDto == null) {
                    baseQuote
                } else {
                    baseQuote.copy(
                        price = quoteDto.price.toDoubleSafe(),
                        change = quoteDto.change.toDoubleSafe(),
                        changePercent = quoteDto.changePercent.toPercentDouble()
                    )
                }
            }
        }.awaitAll()
    }

    override suspend fun getStockDetails(symbol: String): StockDetails {
        val normalized = symbol.trim().uppercase(Locale.US)
        val overview = api.getOverview(symbol = normalized, apiKey = apiKey)
        overview.throwIfApiError(normalized)

        val quoteDto = runCatching {
            api.getGlobalQuote(symbol = normalized, apiKey = apiKey).quote
        }.getOrNull()

        return overview.toDomain(quoteDto, normalized)
    }

    private fun SymbolMatchDto.toDomainQuote(): StockQuote {
        val symbolValue = symbol.cleanValue().orEmpty()
        return StockQuote(
            symbol = symbolValue,
            name = name.cleanValue() ?: symbolValue,
            exchange = region.cleanValue() ?: "Unknown",
            currency = currency.cleanValue() ?: "USD",
            price = null,
            change = null,
            changePercent = null,
            week52High = null,
            week52Low = null
        )
    }

    private fun TopMoverItemDto.toDomainQuote(): StockQuote? {
        val resolvedSymbol = ticker.cleanValue() ?: symbol.cleanValue() ?: return null
        return StockQuote(
            symbol = resolvedSymbol.uppercase(Locale.US),
            name = resolvedSymbol.uppercase(Locale.US),
            exchange = "US Market",
            currency = "USD",
            price = price.toDoubleSafe(),
            change = changeAmount.toDoubleSafe(),
            changePercent = changePercentage.toPercentDouble(),
            week52High = null,
            week52Low = null
        )
    }

    private fun OverviewResponseDto.toDomain(quote: GlobalQuoteDto?, symbolFallback: String): StockDetails {
        return StockDetails(
            symbol = symbol.cleanValue() ?: symbolFallback,
            name = name.cleanValue() ?: symbolFallback,
            exchange = exchange.cleanValue() ?: "Unknown",
            currency = currency.cleanValue() ?: "USD",
            price = quote?.price.toDoubleSafe(),
            change = quote?.change.toDoubleSafe(),
            changePercent = quote?.changePercent.toPercentDouble(),
            dayHigh = quote?.high.toDoubleSafe(),
            dayLow = quote?.low.toDoubleSafe(),
            week52High = week52High.toDoubleSafe(),
            week52Low = week52Low.toDoubleSafe(),
            marketCap = marketCapitalization.toLongSafe(),
            volume = quote?.volume.toLongSafe(),
            peRatio = peRatio.toDoubleSafe(),
            eps = eps.toDoubleSafe(),
            dividendYield = dividendYield.toPercentDouble(),
            sector = sector.cleanValue() ?: "Unknown",
            industry = industry.cleanValue() ?: "Unknown",
            ceo = "N/A",
            headquarters = address.cleanValue() ?: "Unknown",
            description = description.cleanValue() ?: "No description"
        )
    }

    private fun SymbolSearchResponseDto.throwIfApiError() {
        val apiError = errorMessage.cleanValue() ?: information.cleanValue() ?: note.cleanValue()
        if (apiError != null) {
            throw IllegalStateException(apiError)
        }
    }

    private fun TopMoversResponseDto.throwIfApiError() {
        val apiError = errorMessage.cleanValue() ?: information.cleanValue() ?: note.cleanValue()
        if (apiError != null) {
            throw IllegalStateException(apiError)
        }
    }

    private fun OverviewResponseDto.throwIfApiError(symbol: String) {
        val apiError = errorMessage.cleanValue() ?: information.cleanValue() ?: note.cleanValue()
        if (apiError != null) {
            throw IllegalStateException(apiError)
        }
        if (this.symbol.cleanValue().isNullOrBlank()) {
            throw IllegalStateException("No overview data for $symbol")
        }
    }

    private fun String?.cleanValue(): String? {
        val value = this?.trim().orEmpty()
        return value.takeIf { it.isNotBlank() && !it.equals("None", ignoreCase = true) }
    }

    private fun String?.toDoubleSafe(): Double? {
        val normalized = this.cleanValue()?.replace(",", "") ?: return null
        return normalized.toDoubleOrNull()
    }

    private fun String?.toLongSafe(): Long? {
        val normalized = this.cleanValue()?.replace(",", "") ?: return null
        return normalized.toLongOrNull()
    }

    private fun String?.toPercentDouble(): Double? {
        val normalized = this.cleanValue()?.replace("%", "") ?: return null
        return normalized.toDoubleOrNull()
    }

    private companion object {
        const val MAX_SEARCH_RESULTS = 8
        const val MAX_DEFAULT_RESULTS = 20
    }
}
