package com.bookworms.app.data.network

import com.bookworms.app.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

// One HTTP client for the whole app, talking only to our server.
object ApiClient {

    // Skips fields the app doesn't know yet, so a newer server doesn't break an older app.
    private val json = Json { ignoreUnknownKeys = true }

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val books: BooksApi = retrofit.create(BooksApi::class.java)
}
