package com.bookworms.app.ui.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.ui.auth.FieldShape
import com.bookworms.app.ui.auth.authFieldColors
import com.bookworms.app.ui.components.AppCard
import com.bookworms.app.ui.components.AppHeader
import com.bookworms.app.ui.components.BookRow
import com.bookworms.app.ui.components.BackTitle
import com.bookworms.app.ui.components.Pill
import com.bookworms.app.ui.components.mistedBackground
import com.bookworms.app.ui.theme.BookWormsTheme

@Composable
fun BrowseScreen(
    query: String,
    results: List<Book>,
    onQueryChange: (String) -> Unit,
    onBookClick: (Book) -> Unit,
    onLoginClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .mistedBackground()
            .safeDrawingPadding()
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp),
    ) {
        AppHeader()

        Spacer(Modifier.height(16.dp))

        BackTitle(title = stringResource(R.string.browse_title), onBackClick = onBackClick)

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            shape = FieldShape,
            colors = authFieldColors(),
            // Gray hint shown while the field is empty.
            placeholder = { Text(stringResource(R.string.search_placeholder)) },
            leadingIcon = {
                // null, because the placeholder already says what the field is for.
                Icon(painter = painterResource(R.drawable.ic_search), contentDescription = null)
            },
            // The Enter key on the keyboard shows a magnifier instead of an arrow.
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))

        GuestNotice()

        Spacer(Modifier.height(16.dp))

        if (query.isNotBlank()) {
            Text(
                text = pluralStringResource(R.plurals.search_results, results.size, query.trim(), results.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        if (results.isEmpty()) {
            Text(
                text = stringResource(R.string.no_results),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        // LazyColumn draws only the cards that fit on the screen, so it stays fast with long lists.
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            // weight(1f) = take the space that's left, so the button below stays at the bottom.
            modifier = Modifier.weight(1f),
        ) {
            items(results, key = { it.isbn }) { book ->
                BookCard(book, onClick = { onBookClick(book) })
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onLoginClick,
            shape = FieldShape,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text(stringResource(R.string.login_to_borrow))
        }
    }
}

@Composable
private fun BookCard(book: Book, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // The same card as on the other screens. TODO: show the real cover when books come from the server.
    AppCard(modifier = modifier, onClick = onClick) {
        BookRow(title = book.title, subtitle = stringResource(R.string.book_author_year, book.author, book.year)) {
            AvailabilityChip(book.groupsAvailable)
        }
    }
}

// Small gray pill: "available in 2 groups" or "no copies".
@Composable
private fun AvailabilityChip(groupsAvailable: Int) {
    val text = if (groupsAvailable > 0) {
        pluralStringResource(R.plurals.available_in_groups, groupsAvailable, groupsAvailable)
    } else {
        stringResource(R.string.no_copies)
    }
    Pill(text = text)
}

// Light blue box telling a guest what they can't do without an account.
@Composable
private fun GuestNotice(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), FieldShape)
            .padding(12.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_person),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.guest_notice),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Preview(device = "id:pixel_8")
@Composable
private fun BrowseScreenPreview() {
    BookWormsTheme {
        val query = "lem"
        BrowseScreen(
            query = query,
            results = filterBooks(sampleBooks, query),
            onQueryChange = {},
            onBookClick = {},
            onLoginClick = {},
            onBackClick = {},
        )
    }
}
