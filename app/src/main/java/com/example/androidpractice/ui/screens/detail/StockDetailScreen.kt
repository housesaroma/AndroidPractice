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
import com.example.androidpractice.ui.model.StockDetailUiModel

private val UpGreen = Color(0xFF137333)
private val DownRed = Color(0xFFB3261E)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun StockDetailScreen(
    stock: StockDetailUiModel,
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
private fun StockHeader(stock: StockDetailUiModel) {
    val changeColor = if (stock.isPositiveChange) UpGreen else DownRed

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
                text = stock.priceText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.constrainAs(price) {
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                }
            )
            Text(
                text = stock.changeText,
                style = MaterialTheme.typography.bodyMedium,
                color = changeColor,
                modifier = Modifier.constrainAs(change) {
                    end.linkTo(price.end)
                    top.linkTo(price.bottom, margin = 6.dp)
                }
            )
            Text(
                text = stock.exchangeCurrencyText,
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
private fun TradingStatsCard(stock: StockDetailUiModel) {
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
            StatRow(label = "Day range", value = stock.dayRangeText)
            StatRow(label = "52w range", value = stock.week52RangeText)
            StatRow(label = "Volume", value = stock.volumeText)
            StatRow(label = "Avg volume", value = stock.avgVolumeText)
            StatRow(label = "Market cap", value = stock.marketCapText)
            StatRow(label = "P/E ratio", value = stock.peRatioText)
            StatRow(label = "EPS", value = stock.epsText)
            StatRow(label = "Dividend yield", value = stock.dividendYieldText)
        }
    }
}

@Composable
private fun CompanyCard(stock: StockDetailUiModel) {
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
