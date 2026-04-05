package com.example.androidpractice.data.remote

import com.example.androidpractice.data.remote.dto.GlobalQuoteResponseDto
import com.example.androidpractice.data.remote.dto.OverviewResponseDto
import com.example.androidpractice.data.remote.dto.SymbolSearchResponseDto
import com.example.androidpractice.data.remote.dto.TopMoversResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface AlphaVantageApi {
    @GET("query")
    suspend fun getGlobalQuote(
        @Query("function") function: String = "GLOBAL_QUOTE",
        @Query("symbol") symbol: String,
        @Query("apikey") apiKey: String
    ): GlobalQuoteResponseDto

    @GET("query")
    suspend fun getOverview(
        @Query("function") function: String = "OVERVIEW",
        @Query("symbol") symbol: String,
        @Query("apikey") apiKey: String
    ): OverviewResponseDto

    @GET("query")
    suspend fun searchSymbols(
        @Query("function") function: String = "SYMBOL_SEARCH",
        @Query("keywords") keywords: String,
        @Query("apikey") apiKey: String
    ): SymbolSearchResponseDto

    @GET("query")
    suspend fun getTopGainersLosers(
        @Query("function") function: String = "TOP_GAINERS_LOSERS",
        @Query("apikey") apiKey: String
    ): TopMoversResponseDto
}
