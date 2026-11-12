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
import com.bookworms.app.data.LoadState
import com.bookworms.app.ui.components.AppCard
import com.bookworms.app.ui.components.AppTopBar
import com.bookworms.app.ui.components.BookRow
import com.bookworms.app.ui.components.LoadFailedMessage
import com.bookworms.app.ui.components.LoadingIndicator
import com.bookworms.app.ui.components.SectionHeader
import com.bookworms.app.ui.components.StatusTag
import com.bookworms.app.ui.components.mistedBackground
import com.bookworms.app.ui.theme.BookWormsTheme
import com.bookworms.app.ui.theme.BrandBlue
import com.bookworms.app.ui.theme.StatusBlueBg
import com.bookworms.app.ui.theme.StatusGreen
import com.bookworms.app.ui.theme.StatusGreenBg

@Composable
internal fun MyBooksScreen(
    books: LoadState<List<OwnBook>>,
    onRetryClick: () -> Unit,
    onBackClick: () -> Unit,
    onAddBookClick: () -> Unit,
    onBookClick: (OwnBook) -> Unit,
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
            when (books) {
                LoadState.Loading -> item { LoadingIndicator() }
                LoadState.Failed -> item { LoadFailedMessage(stringResource(R.string.load_books_failed), onRetryClick) }
                is LoadState.Ready -> {
                    val list = books.data
                    item { SectionHeader(pluralStringResource(R.plurals.books_count, list.size, list.size)) }
                    if (list.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.my_books_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    items(list, key = { it.id }) { book ->
                        OwnBookCard(book, onClick = { onBookClick(book) })
                    }
                }
            }
        }
    }
}

// Same look as the book cards on the other screens, with a chip that says where the book is.
// "Lent to" wins over "Not available": either way, right now the book is with a friend.
@Composable
private fun OwnBookCard(book: OwnBook, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        BookRow(title = book.title, subtitle = book.authorAndYear(), coverUrl = book.coverUrl) {
            when {
                book.lentTo != null -> StatusTag(
                    text = stringResource(R.string.status_lent_to, book.lentTo),
                    dotColor = BrandBlue,
                    backgroundColor = StatusBlueBg,
                    textColor = BrandBlue,
                )
                book.availableToBorrow -> StatusTag(
                    text = stringResource(R.string.status_available),
                    dotColor = StatusGreen,
                    backgroundColor = StatusGreenBg,
                    textColor = StatusGreen,
                )
                // Gray: on the shelf, but the owner doesn't lend it now.
                else -> StatusTag(text = stringResource(R.string.status_not_available))
            }
        }
    }
}

@Preview(device = "id:pixel_8")
@Composable
private fun MyBooksScreenPreview() {
    BookWormsTheme {
        MyBooksScreen(
            books = LoadState.Ready(previewOwnBooks),
            onRetryClick = {},
            onBackClick = {},
            onAddBookClick = {},
            onBookClick = {},
        )
    }
}
