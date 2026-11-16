package com.bookworms.app.data.network

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

// The heart and the reading status of books – the shelves on My lists. Only for the logged-in user.
// Every change answers with the book's heart and status after it.
interface ShelfApi {

    // Every book with a heart or a status, each once.
    @GET("me/shelf")
    suspend fun shelf(): List<ShelfBookDto>

    @GET("me/shelf/{bookId}")
    suspend fun state(@Path("bookId") bookId: String): ShelfStateDto

    @PUT("me/shelf/{bookId}/status")
    suspend fun setStatus(@Path("bookId") bookId: String, @Body request: SetReadingStatusRequestDto): ShelfStateDto

    @DELETE("me/shelf/{bookId}/status")
    suspend fun removeStatus(@Path("bookId") bookId: String): ShelfStateDto

    @PUT("me/shelf/{bookId}/favorite")
    suspend fun addFavorite(@Path("bookId") bookId: String): ShelfStateDto

    @DELETE("me/shelf/{bookId}/favorite")
    suspend fun removeFavorite(@Path("bookId") bookId: String): ShelfStateDto
}

// status: TO_READ, READING, READ (or ABANDONED, not used in the app yet). null = none picked.
@Serializable
data class ShelfStateDto(
    val status: String? = null,
    val favorite: Boolean = false,
)

@Serializable
data class ShelfBookDto(
    val book: BookSummaryDto,
    val status: String? = null,
    // When the status was picked / the heart tapped, e.g. "2026-11-16T15:03:00Z".
    val statusChangedAt: String? = null,
    val favorite: Boolean = false,
    val favoritedAt: String? = null,
)

@Serializable
data class SetReadingStatusRequestDto(
    val status: String,
)
