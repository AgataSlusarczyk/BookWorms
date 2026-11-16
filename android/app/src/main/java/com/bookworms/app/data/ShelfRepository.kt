package com.bookworms.app.data

import com.bookworms.app.data.network.ApiClient
import com.bookworms.app.data.network.SetReadingStatusRequestDto
import com.bookworms.app.data.network.ShelfApi
import com.bookworms.app.data.network.ShelfStateDto
import com.bookworms.app.ui.book.ReadingStatus
import com.bookworms.app.ui.lists.Shelf
import com.bookworms.app.ui.lists.ShelfBook
import java.time.Instant

// The heart and the reading status of one book.
data class ShelfState(val status: ReadingStatus? = null, val favorite: Boolean = false)

// Favorites and the reading status, kept on the server.
class ShelfRepository(private val api: ShelfApi = ApiClient.shelf) {

    // The books on each shelf. A book can be on two: Favorites and the one of its status.
    internal suspend fun shelves(): Map<Shelf, List<ShelfBook>> {
        val books = api.shelf()
        val shelves = mutableMapOf<Shelf, MutableList<ShelfBook>>()
        for (dto in books) {
            // The app opens a book by its ISBN, so a book without one couldn't be opened.
            val isbn = dto.book.isbn ?: continue
            fun add(shelf: Shelf, at: String?) {
                shelves.getOrPut(shelf) { mutableListOf() } += ShelfBook(
                    isbn = isbn,
                    title = dto.book.title,
                    author = dto.book.authors.orEmpty(),
                    coverUrl = serverUrl(dto.book.coverUrl),
                    addedAt = at?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: Instant.now(),
                    isFavorite = dto.favorite,
                )
            }
            if (dto.favorite) add(Shelf.Favorites, dto.favoritedAt)
            dto.status?.toReadingStatus()?.shelf()?.let { add(it, dto.statusChangedAt) }
        }
        return shelves
    }

    suspend fun state(bookId: String): ShelfState = api.state(bookId).toShelfState()

    // null takes the status back.
    suspend fun setStatus(bookId: String, status: ReadingStatus?): ShelfState =
        if (status == null) api.removeStatus(bookId).toShelfState()
        else api.setStatus(bookId, SetReadingStatusRequestDto(status.serverName())).toShelfState()

    suspend fun setFavorite(bookId: String, favorite: Boolean): ShelfState =
        if (favorite) api.addFavorite(bookId).toShelfState() else api.removeFavorite(bookId).toShelfState()
}

private fun ShelfStateDto.toShelfState() = ShelfState(status = status?.toReadingStatus(), favorite = favorite)

// The server's names are different from the app's – the app's match the buttons on Book details.
private fun ReadingStatus.serverName() = when (this) {
    ReadingStatus.TO_READ -> "TO_READ"
    ReadingStatus.IN_PROGRESS -> "READING"
    ReadingStatus.FINISHED -> "READ"
}

// null for a status the app doesn't show (ABANDONED).
private fun String.toReadingStatus(): ReadingStatus? = when (this) {
    "TO_READ" -> ReadingStatus.TO_READ
    "READING" -> ReadingStatus.IN_PROGRESS
    "READ" -> ReadingStatus.FINISHED
    else -> null
}

private fun ReadingStatus.shelf() = when (this) {
    ReadingStatus.TO_READ -> Shelf.WantToRead
    ReadingStatus.IN_PROGRESS -> Shelf.Reading
    ReadingStatus.FINISHED -> Shelf.Read
}
