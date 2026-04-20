package com.example.androidpractice.domain.model

data class StockFilters(
    val searchQuery: String = "",
    val rangePoint: Double? = null,
    val onlyRising: Boolean = false
) {
    fun isDefault(): Boolean {
        return searchQuery.isBlank() && rangePoint == null && !onlyRising
    }
}
