package com.example.androidpractice.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GlobalQuoteResponseDto(
    @SerializedName("Global Quote") val quote: GlobalQuoteDto? = null,
    @SerializedName("Information") val information: String? = null,
    @SerializedName("Note") val note: String? = null,
    @SerializedName("Error Message") val errorMessage: String? = null
)

data class GlobalQuoteDto(
    @SerializedName("01. symbol") val symbol: String? = null,
    @SerializedName("02. open") val open: String? = null,
    @SerializedName("03. high") val high: String? = null,
    @SerializedName("04. low") val low: String? = null,
    @SerializedName("05. price") val price: String? = null,
    @SerializedName("06. volume") val volume: String? = null,
    @SerializedName("09. change") val change: String? = null,
    @SerializedName("10. change percent") val changePercent: String? = null
)
