package com.example.androidpractice.ui.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import com.example.androidpractice.data.model.Stock
import java.util.Locale

private val UpGreen = Color(0xFF137333)
private val DownRed = Color(0xFFB3261E)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun StockDetailScreen(
    stock: Stock,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stock.symbol) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
        ) {
            item { StockHeader(stock = stock) }
            item { TradingStatsCard(stock = stock) }
            item { CompanyCard(stock = stock) }
            item { DescriptionCard(description = stock.description) }
        }
    }
}

@Composable
private fun StockHeader(stock: Stock) {
    val changeColor = if (stock.change >= 0) UpGreen else DownRed

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            val (symbol, name, price, change, exchange) = createRefs()

            Text(
                text = stock.symbol,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.constrainAs(symbol) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top)
                }
            )
            Text(
                text = stock.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.constrainAs(name) {
                    start.linkTo(symbol.start)
                    top.linkTo(symbol.bottom, margin = 6.dp)
                }
            )
            Text(
                text = formatPrice(stock.price, stock.currency),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.constrainAs(price) {
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                }
            )
            Text(
                text = formatChange(stock.change, stock.changePercent),
                style = MaterialTheme.typography.bodyMedium,
                color = changeColor,
                modifier = Modifier.constrainAs(change) {
                    end.linkTo(price.end)
                    top.linkTo(price.bottom, margin = 6.dp)
                }
            )
            Text(
                text = "${stock.exchange} • ${stock.currency}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.constrainAs(exchange) {
                    start.linkTo(symbol.start)
                    top.linkTo(name.bottom, margin = 8.dp)
                }
            )
        }
    }
}

@Composable
private fun TradingStatsCard(stock: Stock) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Trading stats",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.size(12.dp))
            StatRow(label = "Day range", value = formatRange(stock.dayLow, stock.dayHigh))
            StatRow(label = "52w range", value = formatRange(stock.week52Low, stock.week52High))
            StatRow(label = "Volume", value = formatNumber(stock.volume))
            StatRow(label = "Avg volume", value = formatNumber(stock.avgVolume))
            StatRow(label = "Market cap", value = formatMarketCap(stock.marketCap))
            StatRow(label = "P/E ratio", value = formatDecimal(stock.peRatio))
            StatRow(label = "EPS", value = formatDecimal(stock.eps))
            StatRow(label = "Dividend yield", value = formatPercent(stock.dividendYield))
        }
    }
}

@Composable
private fun CompanyCard(stock: Stock) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Company",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.size(12.dp))
            StatRow(label = "CEO", value = stock.ceo)
            StatRow(label = "Headquarters", value = stock.headquarters)
            StatRow(label = "Sector", value = stock.sector)
            StatRow(label = "Industry", value = stock.industry)
        }
    }
}

@Composable
private fun DescriptionCard(description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
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
