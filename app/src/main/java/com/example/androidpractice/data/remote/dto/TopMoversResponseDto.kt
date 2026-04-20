package com.example.androidpractice.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TopMoversResponseDto(
    @SerializedName("top_gainers") val topGainers: List<TopMoverItemDto>? = null,
    @SerializedName("top_losers") val topLosers: List<TopMoverItemDto>? = null,
    @SerializedName("most_actively_traded") val mostActivelyTraded: List<TopMoverItemDto>? = null,
    @SerializedName("Information") val information: String? = null,
    @SerializedName("Note") val note: String? = null,
    @SerializedName("Error Message") val errorMessage: String? = null
)

data class TopMoverItemDto(
    @SerializedName("ticker") val ticker: String? = null,
    @SerializedName("symbol") val symbol: String? = null,
    @SerializedName("price") val price: String? = null,
    @SerializedName("change_amount") val changeAmount: String? = null,
    @SerializedName("change_percentage") val changePercentage: String? = null,
    @SerializedName("volume") val volume: String? = null
)
