package com.example.androidpractice.ui.model

import com.example.androidpractice.data.model.Stock
import java.util.Locale

fun Stock.toListItemUiModel(): StockListItemUiModel {
    return StockListItemUiModel(
        symbol = symbol,
        name = name,
        exchange = exchange,
        priceText = formatPrice(price, currency),
        changeText = formatChange(change, changePercent),
        isPositiveChange = change >= 0
    )
}

fun Stock.toDetailUiModel(): StockDetailUiModel {
    return StockDetailUiModel(
        symbol = symbol,
        name = name,
        exchangeCurrencyText = "$exchange • $currency",
        priceText = formatPrice(price, currency),
        changeText = formatChange(change, changePercent),
        isPositiveChange = change >= 0,
        dayRangeText = formatRange(dayLow, dayHigh),
        week52RangeText = formatRange(week52Low, week52High),
        volumeText = formatNumber(volume),
        avgVolumeText = formatNumber(avgVolume),
        marketCapText = formatMarketCap(marketCap),
        peRatioText = formatDecimal(peRatio),
        epsText = formatDecimal(eps),
        dividendYieldText = formatPercent(dividendYield),
        ceo = ceo,
        headquarters = headquarters,
        sector = sector,
        industry = industry,
        description = description
    )
}

private fun formatPrice(price: Double, currency: String): String {
    return String.format(Locale.US, "%.2f %s", price, currency)
}

private fun formatChange(change: Double, changePercent: Double): String {
    return String.format(Locale.US, "%+.2f (%.2f%%)", change, changePercent)
}

private fun formatDecimal(value: Double): String {
    return String.format(Locale.US, "%.2f", value)
}

private fun formatRange(low: Double, high: Double): String {
    return String.format(Locale.US, "%.2f - %.2f", low, high)
}

private fun formatNumber(value: Long): String {
    return String.format(Locale.US, "%,d", value)
}

private fun formatMarketCap(value: Long): String {
    return when {
        value >= 1_000_000_000_000L -> String.format(Locale.US, "%.2f T", value / 1_000_000_000_000.0)
        value >= 1_000_000_000L -> String.format(Locale.US, "%.2f B", value / 1_000_000_000.0)
        value >= 1_000_000L -> String.format(Locale.US, "%.2f M", value / 1_000_000.0)
        else -> value.toString()
    }
}

private fun formatPercent(value: Double): String {
    return String.format(Locale.US, "%.2f%%", value)
}
