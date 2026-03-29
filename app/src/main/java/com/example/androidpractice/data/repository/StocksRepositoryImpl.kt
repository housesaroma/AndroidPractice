package com.example.androidpractice.data.repository

import com.example.androidpractice.data.remote.AlphaVantageApi
import com.example.androidpractice.data.remote.dto.GlobalQuoteDto
import com.example.androidpractice.data.remote.dto.GlobalQuoteResponseDto
import com.example.androidpractice.data.remote.dto.OverviewResponseDto
import com.example.androidpractice.data.remote.dto.SymbolMatchDto
import com.example.androidpractice.data.remote.dto.SymbolSearchResponseDto
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

    override suspend fun getStocksBySymbols(symbols: List<String>): List<StockQuote> = coroutineScope {
        val stocks = symbols.map { symbol ->
            async {
                runCatching { fetchQuote(symbol) }.getOrNull()
            }
        }.awaitAll().filterNotNull()

        if (stocks.isEmpty()) {
            throw IllegalStateException("Could not load stocks. Check API key, limits, or internet connection.")
        }

        stocks
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

    private suspend fun fetchQuote(symbol: String): StockQuote? {
        val normalized = symbol.trim().uppercase(Locale.US)
        val response = api.getGlobalQuote(symbol = normalized, apiKey = apiKey)

        if (response.quote?.symbol.cleanValue().isNullOrBlank()) {
            return null
        }

        val quote = response.quote
        return StockQuote(
            symbol = quote?.symbol.cleanValue() ?: normalized,
            name = FALLBACK_NAMES[normalized] ?: normalized,
            exchange = FALLBACK_EXCHANGES[normalized] ?: "Unknown",
            currency = "USD",
            price = quote?.price.toDoubleSafe(),
            change = quote?.change.toDoubleSafe(),
            changePercent = quote?.changePercent.toPercentDouble()
        )
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
            changePercent = null
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

        val FALLBACK_NAMES = mapOf(
            "AAPL" to "Apple Inc.",
            "MSFT" to "Microsoft Corporation",
            "NVDA" to "NVIDIA Corporation",
            "AMZN" to "Amazon.com, Inc.",
            "GOOGL" to "Alphabet Inc.",
            "TSLA" to "Tesla, Inc.",
            "JPM" to "JPMorgan Chase & Co.",
            "KO" to "The Coca-Cola Company"
        )

        val FALLBACK_EXCHANGES = mapOf(
            "AAPL" to "NASDAQ",
            "MSFT" to "NASDAQ",
            "NVDA" to "NASDAQ",
            "AMZN" to "NASDAQ",
            "GOOGL" to "NASDAQ",
            "TSLA" to "NASDAQ",
            "JPM" to "NYSE",
            "KO" to "NYSE"
        )
    }
}
