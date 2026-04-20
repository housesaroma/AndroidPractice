package com.example.androidpractice.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SymbolSearchResponseDto(
    @SerializedName("bestMatches") val bestMatches: List<SymbolMatchDto>? = null,
    @SerializedName("Information") val information: String? = null,
    @SerializedName("Note") val note: String? = null,
    @SerializedName("Error Message") val errorMessage: String? = null
)

data class SymbolMatchDto(
    @SerializedName("1. symbol") val symbol: String? = null,
    @SerializedName("2. name") val name: String? = null,
    @SerializedName("4. region") val region: String? = null,
    @SerializedName("8. currency") val currency: String? = null
)
