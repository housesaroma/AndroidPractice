package com.example.androidpractice.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.androidpractice.data.model.Stock
import com.example.androidpractice.data.repository.MockStockRepository
import com.example.androidpractice.data.repository.StockRepository
import com.example.androidpractice.ui.model.StockDetailUiModel
import com.example.androidpractice.ui.model.StockListItemUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class StocksViewModel(
    private val repository: StockRepository = MockStockRepository()
) : ViewModel() {
    private val _stocks = MutableStateFlow(repository.getStocks().map(::toListItemUiModel))
    val stocks: StateFlow<List<StockListItemUiModel>> = _stocks.asStateFlow()

    fun getStockDetail(symbol: String): StockDetailUiModel? {
        return repository.getStock(symbol)?.let(::toDetailUiModel)
    }

    private fun toListItemUiModel(stock: Stock): StockListItemUiModel {
        return StockListItemUiModel(
            symbol = stock.symbol,
            name = stock.name,
            exchange = stock.exchange,
            priceText = formatPrice(stock.price, stock.currency),
            changeText = formatChange(stock.change, stock.changePercent),
            isPositiveChange = stock.change >= 0
        )
    }

    private fun toDetailUiModel(stock: Stock): StockDetailUiModel {
        return StockDetailUiModel(
            symbol = stock.symbol,
            name = stock.name,
            exchangeCurrencyText = "${stock.exchange} • ${stock.currency}",
            priceText = formatPrice(stock.price, stock.currency),
            changeText = formatChange(stock.change, stock.changePercent),
            isPositiveChange = stock.change >= 0,
            dayRangeText = formatRange(stock.dayLow, stock.dayHigh),
            week52RangeText = formatRange(stock.week52Low, stock.week52High),
            volumeText = formatNumber(stock.volume),
            avgVolumeText = formatNumber(stock.avgVolume),
            marketCapText = formatMarketCap(stock.marketCap),
            peRatioText = formatDecimal(stock.peRatio),
            epsText = formatDecimal(stock.eps),
            dividendYieldText = formatPercent(stock.dividendYield),
            ceo = stock.ceo,
            headquarters = stock.headquarters,
            sector = stock.sector,
            industry = stock.industry,
            description = stock.description
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
}
