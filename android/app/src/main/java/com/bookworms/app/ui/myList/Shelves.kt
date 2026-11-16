package com.bookworms.app.ui.lists

import androidx.annotation.StringRes
import com.bookworms.app.R
import java.text.Collator
import java.time.Duration
import java.time.Instant
import java.util.Locale

// The built-in lists every user has, in the order of the tabs on My lists.
// Favorites = the heart on Book details; the others come from the reading status there.
enum class Shelf(@StringRes val label: Int) {
    Favorites(R.string.tab_favorites),
    WantToRead(R.string.tab_want_to_read),
    Reading(R.string.tab_reading),
    Read(R.string.tab_read),
}

// One book on a shelf. addedAt = when it was put there (the heart tapped, the status picked).
internal data class ShelfBook(
    val isbn: String,
    val title: String,
    val author: String,
    val coverUrl: String?,
    val addedAt: Instant,
    // A full heart on every shelf, not only on Favorites.
    val isFavorite: Boolean,
) {
    // 0 for today.
    val addedDaysAgo: Int get() = Duration.between(addedAt, Instant.now()).toDays().toInt().coerceAtLeast(0)
}

// The orders the books on a shelf can be shown in.
internal enum class ShelfSort(@StringRes val label: Int) {
    RecentlyAdded(R.string.sort_recently_added),
    Title(R.string.sort_title),
    Author(R.string.sort_author);

    fun apply(books: List<ShelfBook>): List<ShelfBook> = when (this) {
        RecentlyAdded -> books.sortedByDescending { it.addedAt }
        Title -> books.sortedWith(compareBy(polishOrder) { it.title })
        // By surname (the last word), e.g. "Stanisław Lem" goes under L. Same author: by title.
        Author -> books.sortedWith(
            compareBy<ShelfBook, String>(polishOrder) { it.author.substringAfterLast(' ') }
                .thenBy(polishOrder) { it.title }
        )
    }
}

// Sorts like a Polish dictionary: "Ł" comes right after "L", not after "Z".
private val polishOrder = Collator.getInstance(Locale.forLanguageTag("pl"))
