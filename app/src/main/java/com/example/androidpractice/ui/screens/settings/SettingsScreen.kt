package com.example.androidpractice.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.androidpractice.ui.viewmodel.SettingsUiState

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen(
    uiState: SettingsUiState,
    onQueryChange: (String) -> Unit,
    onRangePointChange: (String) -> Unit,
    onOnlyRisingChange: (Boolean) -> Unit,
    onResetFilters: () -> Unit,
    onDone: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(title = { Text(text = "Filters") })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onQueryChange,
                label = { Text("Search query") },
                placeholder = { Text("AAPL, Tesla, Microsoft...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.rangePointInput,
                onValueChange = onRangePointChange,
                label = { Text("52w range point") },
                placeholder = { Text("Example: 200") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                supportingText = {
                    val error = uiState.rangeInputError
                    if (error != null) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text("Stock must include this value in its 52-week range")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Only rising stocks",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = uiState.onlyRising,
                    onCheckedChange = onOnlyRisingChange
                )
            }

            Text(
                text = if (uiState.hasActiveFilters) {
                    "Non-default filters are active"
                } else {
                    "Filters are in default state"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onResetFilters) {
                    Text("Reset")
                }
                Button(onClick = onDone) {
                    Text("Done")
                }
            }
        }
    }
}
