package com.example.fitnesstracker.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// Singleton-объект для создания и хранения экземпляра Retrofit
object RetrofitInstance {

    // Базовый URL API OpenFoodFacts
    private const val BASE_URL = "https://world.openfoodfacts.org/"

    // Настройка OkHttpClient с таймаутами
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Создание и ленивое инициализирование API-интерфейса с помощью Retrofit
    val api: OpenFoodFactsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL) // Установка базового URL
            .client(client) // Использование кастомного клиента
            .addConverterFactory(GsonConverterFactory.create()) // Конвертер JSON -> Kotlin
            .build()
            .create(OpenFoodFactsApiService::class.java) // Создание реализации API-интерфейса
    }
}
