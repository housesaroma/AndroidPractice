package com.example.androidpractice.ui.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.example.androidpractice.domain.model.StockDetails
import com.example.androidpractice.ui.viewmodel.StockDetailsUiState
import java.util.Locale

private val UpGreen = Color(0xFF137333)
private val DownRed = Color(0xFFB3261E)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun StockDetailScreen(
    uiState: StockDetailsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val title = uiState.stock?.symbol ?: uiState.symbol.ifBlank { "Stock details" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (uiState.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (uiState.isFavorite) "Remove favorite" else "Add favorite",
                            tint = if (uiState.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading && uiState.stock == null -> {
                LoadingState(modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding))
            }

            uiState.errorMessage != null && uiState.stock == null -> {
                ErrorState(
                    message = uiState.errorMessage,
                    onRetry = onRetry,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            uiState.stock != null -> {
                StockContent(
                    stock = uiState.stock,
                    errorMessage = uiState.errorMessage,
                    onRetry = onRetry,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            else -> {
                ErrorState(
                    message = "No details available",
                    onRetry = onRetry,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun StockContent(
    stock: StockDetails,
    errorMessage: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        if (errorMessage != null) {
            item {
                ErrorBanner(message = errorMessage, onRetry = onRetry)
            }
        }
        item { StockHeader(stock = stock) }
        item { TradingStatsCard(stock = stock) }
        item { CompanyCard(stock = stock) }
        item { DescriptionCard(description = stock.description) }
    }
}

@Composable
private fun StockHeader(stock: StockDetails) {
    val changeColor = when {
        (stock.change ?: 0.0) > 0 -> UpGreen
        (stock.change ?: 0.0) < 0 -> DownRed
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

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
private fun TradingStatsCard(stock: StockDetails) {
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
            StatRow(label = "Market cap", value = formatMarketCap(stock.marketCap))
            StatRow(label = "P/E ratio", value = formatDecimal(stock.peRatio))
            StatRow(label = "EPS", value = formatDecimal(stock.eps))
            StatRow(label = "Dividend yield", value = formatPercent(stock.dividendYield))
        }
    }
}

@Composable
private fun CompanyCard(stock: StockDetails) {
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
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(12.dp))
        Text("Loading details...")
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onRetry: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onRetry) {
            Text("Retry")
        }
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
