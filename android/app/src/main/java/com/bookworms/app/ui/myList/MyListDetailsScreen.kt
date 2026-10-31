package com.bookworms.app.ui.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.ui.browse.Book
import com.bookworms.app.ui.browse.sampleBooks
import com.bookworms.app.ui.components.AppTopBar
import com.bookworms.app.ui.components.mistedBackground
import com.bookworms.app.ui.theme.BookWormsTheme

// One book on a list, with what's happening to it right now.
internal data class ListBook(val book: Book, val status: ListStatus)

// TODO: the books on each list from the server. For now every list has the same books.
internal val sampleListBooks = listOf(
    ListBook(sampleBooks[0], ListStatus.Available),
    ListBook(sampleBooks[1], ListStatus.Borrowed),
    ListBook(sampleBooks[3], ListStatus.Reserved),
)

// The books on one of the user's own lists, opened from "Custom lists" in My lists.
// The pencil at the top opens Edit list, the button at the bottom adds books.
@Composable
fun MyListDetailsScreen(
    listName: String,
    onBookClick: (isbn: String) -> Unit,
    onEditClick: () -> Unit,
    onAddBooksClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    Scaffold(
        // Transparent, so the misted picture behind the whole screen shows through.
        modifier = Modifier.mistedBackground(),
        containerColor = Color.Transparent,
        topBar = {
            AppTopBar(title = listName, onBackClick = onBackClick) {
                IconButton(onClick = onEditClick) {
                    Icon(painterResource(R.drawable.ic_edit), contentDescription = stringResource(R.string.edit_list_title))
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddBooksClick,
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                text = { Text(stringResource(R.string.add_books)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { padding ->
        LazyColumn(
            // Extra space at the bottom, so the last book isn't hidden under the button.
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            items(sampleListBooks, key = { it.book.isbn }) { (book, status) ->
                ListBookCard(
                    title = book.title,
                    author = book.author,
                    status = status,
                    onClick = { onBookClick(book.isbn) },
                    // Someone has it now, so the user can ask to hear when it's back.
                    extraBadge = if (status == ListStatus.Borrowed) stringResource(R.string.badge_notify_me) else null,
                )
            }
        }
    }
}

@Preview(device = "id:pixel_8")
@Composable
private fun MyListDetailsScreenPreview() {
    BookWormsTheme {
        MyListDetailsScreen(listName = "Sci-fi", onBookClick = {}, onEditClick = {}, onAddBooksClick = {}, onBackClick = {})
    }
}
