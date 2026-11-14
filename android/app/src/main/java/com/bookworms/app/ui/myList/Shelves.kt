package com.bookworms.app.ui.lists

import androidx.annotation.StringRes
import com.bookworms.app.R
import java.text.Collator
import java.util.Locale

// The three built-in lists every user has, in the order of the tabs on My lists.
enum class Shelf(@StringRes val label: Int) {
    Favorites(R.string.tab_favorites),
    WantToRead(R.string.tab_want_to_read),
    Read(R.string.tab_read),
}

// One book on a shelf. addedDaysAgo = when it was put there.
internal data class ShelfBook(val title: String, val author: String, val status: ListStatus, val addedDaysAgo: Int)

// TODO: the user's shelves from the server. Empty until then.
internal val shelfBooks: Map<Shelf, List<ShelfBook>> = emptyMap()

internal fun Shelf.books(): List<ShelfBook> = shelfBooks[this].orEmpty()

// Titles on Favorites, so the other shelves can show a full heart for them too.
internal fun favoriteTitles(): Set<String> = Shelf.Favorites.books().map { it.title }.toSet()

// The orders the books on a shelf can be shown in.
internal enum class ShelfSort(@StringRes val label: Int) {
    RecentlyAdded(R.string.sort_recently_added),
    Title(R.string.sort_title),
    Author(R.string.sort_author);

    fun apply(books: List<ShelfBook>): List<ShelfBook> = when (this) {
        RecentlyAdded -> books.sortedBy { it.addedDaysAgo }
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
