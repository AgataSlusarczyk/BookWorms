package com.bookworms.app.data.network

import com.bookworms.app.BuildConfig
import com.bookworms.app.data.auth.Session
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

// One HTTP client for the whole app, talking only to our server.
object ApiClient {

    // Skips fields the app doesn't know yet, so a newer server doesn't break an older app.
    private val json = Json { ignoreUnknownKeys = true }

    private val http = OkHttpClient.Builder()
        .addInterceptor(::withToken)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(http)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val auth: AuthApi = retrofit.create(AuthApi::class.java)
    val books: BooksApi = retrofit.create(BooksApi::class.java)
    val copies: CopiesApi = retrofit.create(CopiesApi::class.java)
    val loans: LoansApi = retrofit.create(LoansApi::class.java)

    // Sends the login token with every request, so the server knows who asks.
    private fun withToken(chain: Interceptor.Chain): Response {
        val token = Session.token ?: return chain.proceed(chain.request())
        val response = chain.proceed(
            chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        )
        if (response.code != 401) return response

        // The server no longer accepts the token (it ran out, or the user was deleted).
        // We log out, which takes the app back to the login screen (see AppNavigation), and ask once
        // more without the token – the public catalog answers anyway, and the rest answers 401 again.
        response.close()
        if (Session.token == token) Session.end()
        return chain.proceed(chain.request())
    }
}
