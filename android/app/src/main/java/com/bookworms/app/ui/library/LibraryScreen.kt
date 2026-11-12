package com.bookworms.app.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.data.LoadState
import com.bookworms.app.ui.auth.FieldShape
import com.bookworms.app.ui.auth.authFieldColors
import com.bookworms.app.ui.browse.Book
import com.bookworms.app.ui.browse.filterBooks
import com.bookworms.app.ui.browse.sampleBooks
import com.bookworms.app.ui.components.AppBottomBar
import com.bookworms.app.ui.components.AppCard
import com.bookworms.app.ui.components.AppTopBar
import com.bookworms.app.ui.browse.Book
import com.bookworms.app.ui.browse.authorAndYear
import com.bookworms.app.ui.browse.sampleBooks
import com.bookworms.app.ui.components.BookRow
import com.bookworms.app.ui.components.LoadFailedMessage
import com.bookworms.app.ui.components.LoadingIndicator
import com.bookworms.app.ui.components.SectionHeader
import com.bookworms.app.ui.components.mistedBackground
import com.bookworms.app.ui.theme.BookWormsTheme

@Composable
fun LibraryScreen(
    // The newest books from the server (LibraryViewModel).
    books: LoadState<List<Book>>,
    onRetryClick: () -> Unit,
    onNavigate: (String) -> Unit = {},
    onBookClick: (Book) -> Unit = {},
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredBooks = remember(searchQuery, books) { filterBooks(books, searchQuery) }

    Scaffold(
        modifier = Modifier.mistedBackground(),
        containerColor = Color.Transparent,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.library_title),
                actions = {
                    IconButton(onClick = onScanIsbnClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_barcode_scan),
                            contentDescription = stringResource(R.string.scan_isbn_action),
                        )
                    }
                },
            )
        },
        bottomBar = { AppBottomBar(currentRoute = "library", onNavigate = onNavigate) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search_books)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_search),
                            contentDescription = stringResource(R.string.content_desc_search),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = FieldShape,
                    singleLine = true,
                    colors = authFieldColors(),
                )
            }

            item {
                Text(
                    text = stringResource(R.string.recently_added),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
            }

            when (books) {
                LoadState.Loading -> item { LoadingIndicator() }
                LoadState.Failed -> item { LoadFailedMessage(stringResource(R.string.load_books_failed), onRetryClick) }
                is LoadState.Ready -> items(books.data, key = { it.isbn }) { book ->
                    AppCard(onClick = { onBookClick(book) }) {
                        BookRow(title = book.title, subtitle = book.authorAndYear(), coverUrl = book.coverUrl)
                    }
                }
            }
        }
    }
}

@Preview(device = "id:pixel_8")
@Composable
private fun LibraryScreenPreview() {
    BookWormsTheme {
        LibraryScreen(books = LoadState.Ready(sampleBooks), onRetryClick = {})
    }
}