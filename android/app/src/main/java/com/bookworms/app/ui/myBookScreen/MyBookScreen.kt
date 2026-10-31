package com.bookworms.app.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.ui.components.AppCard
import com.bookworms.app.ui.components.AppTopBar
import com.bookworms.app.ui.components.BookRow
import com.bookworms.app.ui.components.SectionHeader
import com.bookworms.app.ui.components.StatusTag
import com.bookworms.app.ui.components.mistedBackground
import com.bookworms.app.ui.theme.BookWormsTheme
import com.bookworms.app.ui.theme.BrandBlue
import com.bookworms.app.ui.theme.StatusBlueBg
import com.bookworms.app.ui.theme.StatusGreen
import com.bookworms.app.ui.theme.StatusGreenBg

// One of your own books. lentTo is the name of the friend who has it now, or null if it's on your shelf.
private data class OwnBook(val title: String, val author: String, val year: Int, val lentTo: String? = null)

// Sample books until your books come from the server.
private val sampleOwnBooks = listOf(
    OwnBook("Wiedźmin", "Andrzej Sapkowski", 1994),
    OwnBook("Problem trzech ciał", "Liu Cixin", 2008, lentTo = "Marek W."),
    OwnBook("Solaris", "Stanisław Lem", 1961),
    OwnBook("Lalka", "Bolesław Prus", 1890, lentTo = "Ola N."),
)

@Composable
fun MyBooksScreen(
    onBackClick: () -> Unit,
    onAddBookClick: () -> Unit,
    // Gets the book's title – the sample books here have no ISBN yet.
    onBookClick: (title: String) -> Unit,
) {
    Scaffold(
        // Transparent, so the misted picture behind the whole screen shows through.
        modifier = Modifier.mistedBackground(),
        containerColor = Color.Transparent,
        topBar = { AppTopBar(title = stringResource(R.string.my_books_title), onBackClick = onBackClick) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddBookClick,
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                text = { Text(stringResource(R.string.content_desc_add)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            item { SectionHeader(pluralStringResource(R.plurals.books_count, sampleOwnBooks.size, sampleOwnBooks.size)) }
            items(sampleOwnBooks, key = { it.title }) { book ->
                OwnBookCard(book, onClick = { onBookClick(book.title) })
            }
        }
    }
}

// Same look as the book cards on the other screens, with a chip that says where the book is.
@Composable
private fun OwnBookCard(book: OwnBook, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        BookRow(title = book.title, subtitle = stringResource(R.string.book_author_year, book.author, book.year)) {
            if (book.lentTo == null) {
                StatusTag(
                    text = stringResource(R.string.status_available),
                    dotColor = StatusGreen,
                    backgroundColor = StatusGreenBg,
                    textColor = StatusGreen,
                )
            } else {
                StatusTag(
                    text = stringResource(R.string.status_lent_to, book.lentTo),
                    dotColor = BrandBlue,
                    backgroundColor = StatusBlueBg,
                    textColor = BrandBlue,
                )
            }
        }
    }
}

@Preview(device = "id:pixel_8")
@Composable
private fun MyBooksScreenPreview() {
    BookWormsTheme {
        MyBooksScreen(onBackClick = {}, onAddBookClick = {}, onBookClick = {})
    }
}
