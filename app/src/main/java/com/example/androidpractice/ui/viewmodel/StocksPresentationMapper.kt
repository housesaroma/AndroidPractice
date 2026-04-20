package com.example.androidpractice.ui.viewmodel

import com.example.androidpractice.domain.model.FavoriteStock
import com.example.androidpractice.domain.model.StockDetails
import com.example.androidpractice.domain.model.StockFilters
import com.example.androidpractice.domain.model.StockQuote
import com.example.androidpractice.ui.model.FavoriteStockItemUiModel
import com.example.androidpractice.ui.model.StockChangeTrend
import com.example.androidpractice.ui.model.StockDetailsUiModel
import com.example.androidpractice.ui.model.StockListItemUiModel
import java.util.Locale

internal fun StockFilters.toSummaryText(): String {
    val queryPart = searchQuery.takeIf { it.isNotBlank() }?.let { "Query: $it" }
    val rangePart = rangePoint?.let { "52w point: ${formatFilterPoint(it)}" }
    val risingPart = if (onlyRising) "Only rising" else null

    return listOfNotNull(queryPart, rangePart, risingPart).joinToString(separator = "  |  ")
}

internal fun List<StockQuote>.toStockListItemUiModels(
    favoriteSymbols: Set<String>
): List<StockListItemUiModel> {
    return map { stock ->
        StockListItemUiModel(
            symbol = stock.symbol,
            name = stock.name,
            exchange = stock.exchange,
            priceText = formatPrice(stock.price, stock.currency),
            changeText = formatChange(stock.change, stock.changePercent),
            changeTrend = toChangeTrend(stock.change),
            isFavorite = favoriteSymbols.contains(stock.symbol.uppercase(Locale.US))
        )
    }
}

internal fun StockDetails.toStockDetailsUiModel(): StockDetailsUiModel {
    return StockDetailsUiModel(
        symbol = symbol,
        name = name,
        exchangeCurrencyText = "$exchange • $currency",
        priceText = formatPrice(price, currency),
        changeText = formatChange(change, changePercent),
        changeTrend = toChangeTrend(change),
        dayRangeText = formatRange(dayLow, dayHigh),
        week52RangeText = formatRange(week52Low, week52High),
        volumeText = formatNumber(volume),
        marketCapText = formatMarketCap(marketCap),
        peRatioText = formatDecimal(peRatio),
        epsText = formatDecimal(eps),
        dividendYieldText = formatPercent(dividendYield),
        headquarters = headquarters,
        sector = sector,
        industry = industry,
        description = description
    )
}

internal fun FavoriteStock.toFavoriteStockItemUiModel(): FavoriteStockItemUiModel {
    return FavoriteStockItemUiModel(
        symbol = symbol,
        name = name,
        exchange = exchange,
        priceText = formatPrice(price, currency),
        changeText = formatChange(change, changePercent),
        changeTrend = toChangeTrend(change)
    )
}

internal fun Throwable.toReadableStockMessage(): String {
    val raw = message.orEmpty()
    return when {
        raw.contains("Invalid API call", ignoreCase = true) -> {
            "Alpha Vantage rejected the request. Check symbol or query format."
        }

        raw.contains("API call frequency", ignoreCase = true) ||
            raw.contains("standard API rate limit", ignoreCase = true) -> {
            "API rate limit reached. Wait a bit and retry."
        }

        raw.contains("timeout", ignoreCase = true) -> {
            "Request timed out. Check internet and retry."
        }

        raw.isNotBlank() -> raw
        else -> "Something went wrong while loading data."
    }
}

private fun toChangeTrend(change: Double?): StockChangeTrend {
    return when {
        change == null || change == 0.0 -> StockChangeTrend.NEUTRAL
        change > 0 -> StockChangeTrend.UP
        else -> StockChangeTrend.DOWN
    }
}

private fun formatPrice(price: Double?, currency: String): String {
    if (price == null) return "--"
    return String.format(Locale.US, "%.2f %s", price, currency)
}

private fun formatChange(change: Double?, changePercent: Double?): String {
    if (change == null || changePercent == null) return "--"
    return String.format(Locale.US, "%+.2f (%.2f%%)", change, changePercent)
}

private fun formatDecimal(value: Double?): String {
    return value?.let { String.format(Locale.US, "%.2f", it) } ?: "--"
}

private fun formatRange(low: Double?, high: Double?): String {
    if (low == null || high == null) return "--"
    return String.format(Locale.US, "%.2f - %.2f", low, high)
}

private fun formatNumber(value: Long?): String {
    return value?.let { String.format(Locale.US, "%,d", it) } ?: "--"
}

private fun formatMarketCap(value: Long?): String {
    if (value == null) return "--"
    return when {
        value >= 1_000_000_000_000L -> String.format(Locale.US, "%.2f T", value / 1_000_000_000_000.0)
        value >= 1_000_000_000L -> String.format(Locale.US, "%.2f B", value / 1_000_000_000.0)
        value >= 1_000_000L -> String.format(Locale.US, "%.2f M", value / 1_000_000.0)
        else -> value.toString()
    }
}

private fun formatPercent(value: Double?): String {
    return value?.let { String.format(Locale.US, "%.2f%%", it) } ?: "--"
}

private fun formatFilterPoint(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toLong().toString()
    } else {
        String.format(Locale.US, "%.2f", value)
    }
}
