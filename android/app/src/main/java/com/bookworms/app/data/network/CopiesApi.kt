package com.bookworms.app.data.network

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

// The logged-in user's own books (copies) on our server. Every call needs the login token.
interface CopiesApi {

    // Newest first.
    @GET("me/copies")
    suspend fun myCopies(): List<CopyDto>

    // A book with this ISBN already in the catalog gets the copy (the typed title is then not used).
    // Answers 400 (code INVALID_ISBN) for an ISBN that doesn't have 10 or 13 digits.
    @POST("me/copies")
    suspend fun addCopy(@Body request: AddCopyRequestDto): CopyDto

    @PATCH("copies/{id}")
    suspend fun changeAvailability(@Path("id") id: String, @Body request: ChangeAvailabilityRequestDto): CopyDto

    // Only for a book the user typed in by hand (canEditBook) – otherwise 403 (code BOOK_NOT_EDITABLE).
    @PUT("copies/{id}/book")
    suspend fun editBook(@Path("id") id: String, @Body request: EditBookRequestDto): CopyDto

    // Answers 409 (code COPY_LENT) while someone has the book.
    // Response<Unit>, because the server answers 204 with no body.
    @DELETE("copies/{id}")
    suspend fun removeCopy(@Path("id") id: String): Response<Unit>
}

@Serializable
data class CopyDto(
    val id: String,
    // AVAILABLE, UNAVAILABLE or LOST. Being lent out isn't here – it comes from the loans.
    val availability: String,
    val book: CopyBookDto,
    // true = the user typed this book in by hand, so they can edit it.
    val canEditBook: Boolean = false,
)

@Serializable
data class CopyBookDto(
    val id: String,
    val isbn: String? = null,
    val title: String,
    val authors: String? = null,
    val publisher: String? = null,
    val publishedYear: Int? = null,
    // A path on our server, e.g. "/covers/9788308075197.jpg".
    val coverUrl: String? = null,
)

@Serializable
data class AddCopyRequestDto(
    val isbn: String? = null,
    val title: String,
    val authors: String,
    val publisher: String? = null,
    val publishedYear: Int? = null,
)

@Serializable
data class EditBookRequestDto(
    val title: String,
    val authors: String,
    val publisher: String? = null,
    val publishedYear: Int? = null,
)

@Serializable
data class ChangeAvailabilityRequestDto(
    val availability: String,
)
