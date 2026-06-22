package com.example.quiniela_virtual_app.di

import com.example.quiniela_virtual_app.BuildConfig
import com.example.quiniela_virtual_app.data.remote.FootballApiInterceptor
import com.example.quiniela_virtual_app.data.remote.api.FootballApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

/** Provee OkHttpClient, Retrofit y FootballApiService como singletons para toda la app. */
@Module
@InstallIn(SingletonComponent::class)
object FootballApiModule {

    private const val BASE_URL = "https://api.football-data.org/v4/"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(FootballApiInterceptor())
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                    else HttpLoggingInterceptor.Level.NONE
        })
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideFootballApiService(retrofit: Retrofit): FootballApiService =
        retrofit.create(FootballApiService::class.java)
}
