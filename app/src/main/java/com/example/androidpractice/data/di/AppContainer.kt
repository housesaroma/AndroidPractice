package com.example.androidpractice.data.di

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.example.androidpractice.BuildConfig
import com.example.androidpractice.data.remote.AlphaVantageApi
import com.example.androidpractice.data.repository.StocksRepositoryImpl
import com.example.androidpractice.domain.repository.StocksRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object AppContainer {
    @Volatile
    private var stocksRepository: StocksRepository? = null

    fun provideStocksRepository(context: Context): StocksRepository {
        return stocksRepository ?: synchronized(this) {
            stocksRepository ?: StocksRepositoryImpl(
                api = createApi(context),
                apiKey = BuildConfig.ALPHA_VANTAGE_API_KEY
            ).also { stocksRepository = it }
        }
    }

    private fun createApi(context: Context): AlphaVantageApi {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.ALPHA_VANTAGE_BASE_URL)
            .client(createOkHttp(context))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AlphaVantageApi::class.java)
    }

    private fun createOkHttp(context: Context): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }

        return OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .addInterceptor(ChuckerInterceptor.Builder(context).build())
            .build()
    }
}
