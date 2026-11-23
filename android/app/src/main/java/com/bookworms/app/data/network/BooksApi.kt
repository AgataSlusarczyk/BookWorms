package com.bookworms.app.data.network

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

// The public book catalog on our server. Reading works without logging in.
interface BooksApi {

    // No query = the newest books. Otherwise books whose title, author or ISBN contains it.
    @GET("books")
    suspend fun getBooks(
        @Query("query") query: String? = null,
        @Query("limit") limit: Int? = null,
    ): List<BookSummaryDto>

    // The Library: books the user or people in their groups have, newest first. Needs logging in.
    @GET("me/library")
    suspend fun library(@Query("limit") limit: Int? = null): List<BookSummaryDto>

    // Everything about one book. The ISBN may have dashes.
    @GET("books/isbn/{isbn}")
    suspend fun getBookByIsbn(@Path("isbn") isbn: String): BookDetailsDto

    // The book's ratings. Works without logging in – then myRating is always null.
    @GET("books/{bookId}/reviews")
    suspend fun getReviews(@Path("bookId") bookId: String): BookReviewsDto

    // Needs logging in. Rating again changes the old rating. Answers with the ratings after the change.
    @PUT("books/{bookId}/review")
    suspend fun rate(@Path("bookId") bookId: String, @Body request: RateBookRequestDto): BookReviewsDto

    @DELETE("books/{bookId}/review")
    suspend fun removeRating(@Path("bookId") bookId: String): BookReviewsDto
}

@Serializable
data class RateBookRequestDto(
    // Whole stars, 1–5.
    val rating: Int,
)

@Serializable
data class BookReviewsDto(
    // In stars, e.g. 4.5. null when nobody rated the book yet.
    val average: Double? = null,
    val count: Int = 0,
    val myRating: Int? = null,
    // The newest first.
    val reviews: List<ReviewDto> = emptyList(),
)

@Serializable
data class ReviewDto(
    val id: String,
    val authorName: String,
    val authorPhotoUrl: String? = null,
    val rating: Int,
    // null = only stars, no words.
    val content: String? = null,
    val hasSpoilers: Boolean = false,
    // e.g. "2026-11-14T20:15:00Z".
    val updatedAt: String,
)

// A book as the server sends it in a list.
@Serializable
data class BookSummaryDto(
    val id: String,
    val isbn: String? = null,
    val title: String,
    val authors: String? = null,
    val publishedYear: Int? = null,
    // A path on our server, e.g. "/covers/9788308075197.jpg".
    val coverUrl: String? = null,
)

// One book with everything the details screen needs.
@Serializable
data class BookDetailsDto(
    val id: String,
    val isbn: String? = null,
    val title: String,
    val authors: String? = null,
    val publisher: String? = null,
    val publishedYear: Int? = null,
    val pageCount: Int? = null,
    val description: String? = null,
    val coverUrl: String? = null,
    // Genre codes, e.g. ["SCI_FI"].
    val genres: List<String> = emptyList(),
)
