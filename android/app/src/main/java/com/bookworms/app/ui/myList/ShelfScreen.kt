package com.bookworms.app.ui.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.data.LoadState
import com.bookworms.app.ui.components.LoadFailedMessage
import com.bookworms.app.ui.components.LoadingIndicator
import com.bookworms.app.ui.components.AppTopBar
import com.bookworms.app.ui.components.SectionHeader
import com.bookworms.app.ui.components.mistedBackground
import com.bookworms.app.ui.theme.BookWormsTheme

// All books on one shelf (Favorites, Want to read, Reading or Read), opened with "See all" on My lists.
@Composable
internal fun ShelfScreen(
    shelf: Shelf,
    onBookClick: (isbn: String) -> Unit,
    onBackClick: () -> Unit,
    // The books on each shelf, from the server.
    shelves: LoadState<Map<Shelf, List<ShelfBook>>> = LoadState.Ready(emptyMap()),
    onRetryClick: () -> Unit = {},
) {
    var sort by rememberSaveable { mutableStateOf(ShelfSort.RecentlyAdded) }
    val books = sort.apply((shelves as? LoadState.Ready)?.data?.get(shelf).orEmpty())

    Scaffold(
        // Transparent, so the misted picture behind the whole screen shows through.
        modifier = Modifier.mistedBackground(),
        containerColor = Color.Transparent,
        topBar = { AppTopBar(title = stringResource(shelf.label), onBackClick = onBackClick) },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            when (shelves) {
                LoadState.Loading -> item { LoadingIndicator() }
                LoadState.Failed -> item {
                    LoadFailedMessage(stringResource(R.string.load_shelves_failed), onRetryClick = onRetryClick)
                }
                is LoadState.Ready -> {
                    item { ShelfHeader(bookCount = books.size, sort = sort, onSortChange = { sort = it }) }
                    if (books.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.shelf_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            items(books, key = { it.isbn }) { book ->
                ShelfBookCard(book, onClick = { onBookClick(book.isbn) })
            }
        }
    }
}

// A book on a shelf – used both on My lists and here.
// TODO: whether the book can be borrowed in the user's groups (Available / Borrowed) – not shown yet.
@Composable
internal fun ShelfBookCard(book: ShelfBook, onClick: () -> Unit) {
    ListBookCard(
        title = book.title,
        author = book.author,
        status = null,
        onClick = onClick,
        isFavorite = book.isFavorite,
        addedDaysAgo = book.addedDaysAgo,
        coverUrl = book.coverUrl,
    )
}

// "5 books" on the left, and the order on the right ("Recently added ▾"), which opens a menu.
@Composable
private fun ShelfHeader(bookCount: Int, sort: ShelfSort, onSortChange: (ShelfSort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            pluralStringResource(R.plurals.books_count, bookCount, bookCount),
            Modifier.weight(1f),
        )
        // Box, so the menu opens right under the button.
        Box {
            TextButton(
                onClick = { expanded = true },
                contentPadding = PaddingValues(start = 8.dp, end = 4.dp),
            ) {
                Text(stringResource(sort.label), style = MaterialTheme.typography.labelLarge)
                Icon(
                    painterResource(R.drawable.ic_expand_more),
                    contentDescription = stringResource(R.string.content_desc_sort),
                    modifier = Modifier.size(18.dp),
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                ShelfSort.entries.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(option.label),
                                fontWeight = if (option == sort) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        onClick = {
                            onSortChange(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Preview(device = "id:pixel_8")
@Composable
private fun ShelfScreenPreview() {
    BookWormsTheme {
        ShelfScreen(shelf = Shelf.Favorites, onBookClick = {}, onBackClick = {})
    }
}
