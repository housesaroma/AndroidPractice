package com.example.androidpractice.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OverviewResponseDto(
    @SerializedName("Symbol") val symbol: String? = null,
    @SerializedName("Name") val name: String? = null,
    @SerializedName("Exchange") val exchange: String? = null,
    @SerializedName("Currency") val currency: String? = null,
    @SerializedName("Description") val description: String? = null,
    @SerializedName("Sector") val sector: String? = null,
    @SerializedName("Industry") val industry: String? = null,
    @SerializedName("Address") val address: String? = null,
    @SerializedName("MarketCapitalization") val marketCapitalization: String? = null,
    @SerializedName("PERatio") val peRatio: String? = null,
    @SerializedName("EPS") val eps: String? = null,
    @SerializedName("DividendYield") val dividendYield: String? = null,
    @SerializedName("52WeekHigh") val week52High: String? = null,
    @SerializedName("52WeekLow") val week52Low: String? = null,
    @SerializedName("Error Message") val errorMessage: String? = null,
    @SerializedName("Information") val information: String? = null,
    @SerializedName("Note") val note: String? = null
)
