package com.example.androidpractice.data.di

import android.content.Context
import androidx.room.Room
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.example.androidpractice.BuildConfig
import com.example.androidpractice.data.files.DownloadManagerResumeDownloader
import com.example.androidpractice.data.local.preferences.StockFiltersDataStoreRepository
import com.example.androidpractice.data.local.preferences.UserProfileDataStoreRepository
import com.example.androidpractice.data.local.room.AppDatabase
import com.example.androidpractice.data.remote.AlphaVantageApi
import com.example.androidpractice.data.repository.FavoriteStocksRepositoryImpl
import com.example.androidpractice.data.repository.StocksRepositoryImpl
import com.example.androidpractice.domain.repository.FavoriteStocksRepository
import com.example.androidpractice.domain.repository.ResumeDownloader
import com.example.androidpractice.domain.repository.StockFiltersRepository
import com.example.androidpractice.domain.repository.StocksRepository
import com.example.androidpractice.domain.repository.UserProfileRepository
import com.example.androidpractice.notifications.FavoriteLessonReminderScheduler
import com.example.androidpractice.ui.cache.SettingsBadgeCache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object AppContainer {
    @Volatile
    private var stocksRepository: StocksRepository? = null

    @Volatile
    private var stockFiltersRepository: StockFiltersRepository? = null

    @Volatile
    private var favoriteStocksRepository: FavoriteStocksRepository? = null

    @Volatile
    private var appDatabase: AppDatabase? = null

    @Volatile
    private var settingsBadgeCache: SettingsBadgeCache? = null

    @Volatile
    private var userProfileRepository: UserProfileRepository? = null

    @Volatile
    private var resumeDownloader: ResumeDownloader? = null

    @Volatile
    private var favoriteLessonReminderScheduler: FavoriteLessonReminderScheduler? = null

    fun provideStocksRepository(context: Context): StocksRepository {
        return stocksRepository ?: synchronized(this) {
            stocksRepository ?: StocksRepositoryImpl(
                api = createApi(context),
                apiKey = BuildConfig.ALPHA_VANTAGE_API_KEY
            ).also { stocksRepository = it }
        }
    }

    fun provideStockFiltersRepository(context: Context): StockFiltersRepository {
        return stockFiltersRepository ?: synchronized(this) {
            stockFiltersRepository ?: StockFiltersDataStoreRepository(
                context = context.applicationContext
            ).also { stockFiltersRepository = it }
        }
    }

    fun provideFavoriteStocksRepository(context: Context): FavoriteStocksRepository {
        return favoriteStocksRepository ?: synchronized(this) {
            favoriteStocksRepository ?: FavoriteStocksRepositoryImpl(
                dao = provideDatabase(context).favoriteStocksDao()
            ).also { favoriteStocksRepository = it }
        }
    }

    fun provideSettingsBadgeCache(): SettingsBadgeCache {
        return settingsBadgeCache ?: synchronized(this) {
            settingsBadgeCache ?: SettingsBadgeCache().also { settingsBadgeCache = it }
        }
    }

    fun provideUserProfileRepository(context: Context): UserProfileRepository {
        return userProfileRepository ?: synchronized(this) {
            userProfileRepository ?: UserProfileDataStoreRepository(
                context = context.applicationContext
            ).also { userProfileRepository = it }
        }
    }

    fun provideResumeDownloader(context: Context): ResumeDownloader {
        return resumeDownloader ?: synchronized(this) {
            resumeDownloader ?: DownloadManagerResumeDownloader(
                context = context.applicationContext
            ).also { resumeDownloader = it }
        }
    }

    fun provideFavoriteLessonReminderScheduler(context: Context): FavoriteLessonReminderScheduler {
        return favoriteLessonReminderScheduler ?: synchronized(this) {
            favoriteLessonReminderScheduler ?: FavoriteLessonReminderScheduler(
                context = context.applicationContext
            ).also { favoriteLessonReminderScheduler = it }
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

    private fun provideDatabase(context: Context): AppDatabase {
        return appDatabase ?: synchronized(this) {
            appDatabase ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "stocks_database"
            ).build().also { appDatabase = it }
        }
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
