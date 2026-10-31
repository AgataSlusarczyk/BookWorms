package com.bookworms.app.ui.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.ui.auth.FieldShape
import com.bookworms.app.ui.auth.authFieldColors
import com.bookworms.app.ui.browse.Book
import com.bookworms.app.ui.browse.filterBooks
import com.bookworms.app.ui.browse.sampleBooks
import com.bookworms.app.ui.components.AppCard
import com.bookworms.app.ui.components.AppTopBar
import com.bookworms.app.ui.components.BookRow
import com.bookworms.app.ui.components.mistedBackground
import com.bookworms.app.ui.theme.BookWormsTheme
import com.bookworms.app.ui.theme.BrandBlue
import com.bookworms.app.ui.theme.CardBorder

// Opened with "Add books" on a list. Pick books with "+", then add them all at once with the button at the bottom.
@Composable
fun AddBooksToListScreen(
    listName: String,
    // Books already on the list – they show "On list" instead of "+".
    isbnsOnList: List<String>,
    // Gets the ISBNs of the picked books.
    onAddClick: (isbns: List<String>) -> Unit,
    onBookClick: (isbn: String) -> Unit,
    onBackClick: () -> Unit,
) {
    var search by rememberSaveable { mutableStateOf("") }
    var pickedIsbns by rememberSaveable { mutableStateOf(listOf<String>()) }

    // TODO: search all books on the server, not only the sample ones.
    // An empty search shows every book, so there's something to pick from right away.
    val results = filterBooks(sampleBooks, search)

    Scaffold(
        // Transparent, so the misted picture behind the whole screen shows through.
        modifier = Modifier
            .mistedBackground()
            // Keeps the button at the bottom above the keyboard.
            .imePadding(),
        containerColor = Color.Transparent,
        topBar = { AppTopBar(title = stringResource(R.string.add_books_to_list_title, listName), onBackClick = onBackClick) },
        bottomBar = {
            Button(
                onClick = { onAddClick(pickedIsbns) },
                enabled = pickedIsbns.isNotEmpty(),
                shape = FieldShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .height(48.dp),
            ) {
                Text(
                    if (pickedIsbns.isEmpty()) stringResource(R.string.add_books)
                    else pluralStringResource(R.plurals.add_x_books, pickedIsbns.size, pickedIsbns.size)
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                singleLine = true,
                shape = FieldShape,
                colors = authFieldColors(),
                placeholder = { Text(stringResource(R.string.list_books_search_placeholder)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp),
            )

            if (results.isEmpty()) {
                Text(
                    stringResource(R.string.no_results),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(results, key = { it.isbn }) { book ->
                        PickBookCard(
                            book = book,
                            isOnList = book.isbn in isbnsOnList,
                            isPicked = book.isbn in pickedIsbns,
                            onClick = { onBookClick(book.isbn) },
                            onPickClick = {
                                pickedIsbns = if (book.isbn in pickedIsbns) pickedIsbns - book.isbn else pickedIsbns + book.isbn
                            },
                        )
                    }
                }
            }
        }
    }
}

// A book in the results. On the right: "On list" if it's there already, otherwise "+" to pick it
// or "×" to unpick it. A picked book also gets a blue border.
@Composable
private fun PickBookCard(book: Book, isOnList: Boolean, isPicked: Boolean, onClick: () -> Unit, onPickClick: () -> Unit) {
    AppCard(onClick = onClick, borderColor = if (isPicked) BrandBlue else CardBorder) {
        BookRow(
            title = book.title,
            subtitle = book.author,
            trailing = {
                if (isOnList) {
                    Text(
                        stringResource(R.string.book_on_list),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                } else {
                    IconButton(onClick = onPickClick) {
                        Icon(
                            painterResource(if (isPicked) R.drawable.ic_close else R.drawable.ic_add),
                            contentDescription = stringResource(
                                if (isPicked) R.string.remove_book_from_list else R.string.add_book_to_list,
                                book.title,
                            ),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            },
        )
    }
}

@Preview(device = "id:pixel_8")
@Composable
private fun AddBooksToListScreenPreview() {
    BookWormsTheme {
        AddBooksToListScreen(
            listName = "Sci-fi",
            isbnsOnList = sampleListBooks.map { it.book.isbn },
            onAddClick = {},
            onBookClick = {},
            onBackClick = {},
        )
    }
}
