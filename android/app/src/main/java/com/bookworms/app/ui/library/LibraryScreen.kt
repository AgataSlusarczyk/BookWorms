package com.bookworms.app.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.data.LoadState
import com.bookworms.app.ui.auth.FieldShape
import com.bookworms.app.ui.auth.authFieldColors
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

    Scaffold(
        // Transparent, so the misted picture behind the whole screen shows through.
        modifier = Modifier.mistedBackground(),
        containerColor = Color.Transparent,
        topBar = { AppTopBar(title = stringResource(R.string.library_title)) },
        bottomBar = { AppBottomBar(currentRoute = "library", onNavigate = onNavigate) },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            item {
                // Same search field as on the Browse screen.
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    shape = FieldShape,
                    colors = authFieldColors(),
                    placeholder = { Text(stringResource(R.string.search_books)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_search),
                            contentDescription = stringResource(R.string.content_desc_search),
                        )
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item { SectionHeader(stringResource(R.string.recently_added), Modifier.padding(top = 4.dp)) }

            when (books) {
                LoadState.Loading -> item { LoadingIndicator() }
                LoadState.Failed -> item { LoadFailedMessage(stringResource(R.string.load_books_failed), onRetryClick) }
                is LoadState.Ready -> items(books.data, key = { it.isbn }) { book ->
                    AppCard(onClick = { onBookClick(book) }) {
                        BookRow(title = book.title, subtitle = book.authorAndYear())
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
