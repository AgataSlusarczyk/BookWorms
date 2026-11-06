package com.bookworms.app.data.network

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// The public book catalog on our server. Works without logging in.
interface BooksApi {

    // No query = the newest books. Otherwise books whose title, author or ISBN contains it.
    @GET("books")
    suspend fun getBooks(
        @Query("query") query: String? = null,
        @Query("limit") limit: Int? = null,
    ): List<BookSummaryDto>

    // Everything about one book. The ISBN may have dashes.
    @GET("books/isbn/{isbn}")
    suspend fun getBookByIsbn(@Path("isbn") isbn: String): BookDetailsDto
}

// A book as the server sends it in a list.
@Serializable
data class BookSummaryDto(
    val id: String,
    val isbn: String? = null,
    val title: String,
    val authors: String? = null,
    val publishedYear: Int? = null,
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
