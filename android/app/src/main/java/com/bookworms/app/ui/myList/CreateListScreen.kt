package com.bookworms.app.ui.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.ui.auth.FieldShape
import com.bookworms.app.ui.auth.LabeledField
import com.bookworms.app.ui.auth.authFieldColors
import com.bookworms.app.ui.browse.Book
import com.bookworms.app.ui.browse.filterBooks
import com.bookworms.app.ui.browse.sampleBooks
import com.bookworms.app.ui.components.AppCard
import com.bookworms.app.ui.components.AppTopBar
import com.bookworms.app.ui.components.BookRow
import com.bookworms.app.ui.components.SectionHeader
import com.bookworms.app.ui.components.mistedBackground
import com.bookworms.app.ui.theme.BookWormsTheme

private const val MaxListNameLength = 30

// Enough results to find the book, without pushing the rest of the form far down.
private const val MaxSearchResults = 5

@Composable
fun CreateListScreen(
    // Gets the new list's name, so the app can open it.
    onCreateClick: (listName: String) -> Unit,
    onBackClick: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var search by rememberSaveable { mutableStateOf("") }
    // Only the ISBNs, because a plain list of texts survives turning the phone.
    var addedIsbns by rememberSaveable { mutableStateOf(listOf<String>()) }

    // Two lists with the same name would be easy to mix up, so we don't allow it (big and small letters count as the same).
    val trimmedName = name.trim()
    val nameTaken = sampleCustomLists.any { it.equals(trimmedName, ignoreCase = true) }

    // TODO: search all books on the server, not only the sample ones.
    val addedBooks = addedIsbns.mapNotNull { isbn -> sampleBooks.find { it.isbn == isbn } }
    val results = if (search.isBlank()) emptyList() else filterBooks(sampleBooks, search).filter { it.isbn !in addedIsbns }.take(MaxSearchResults)

    Scaffold(
        // Transparent, so the misted picture behind the whole screen shows through.
        modifier = Modifier.mistedBackground(),
        containerColor = Color.Transparent,
        topBar = { AppTopBar(title = stringResource(R.string.create_list), onBackClick = onBackClick) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                // imePadding makes room for the keyboard, so the field you type in moves up above it.
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            LabeledField(label = stringResource(R.string.list_name_label)) {
                OutlinedTextField(
                    value = name,
                    // Stops typing at the limit instead of showing an error.
                    onValueChange = { name = it.take(MaxListNameLength) },
                    singleLine = true,
                    shape = FieldShape,
                    colors = authFieldColors(),
                    placeholder = { Text(stringResource(R.string.list_name_placeholder)) },
                    isError = nameTaken,
                    supportingText = {
                        if (nameTaken) {
                            Text(stringResource(R.string.error_list_name_taken))
                        } else {
                            // Counter on the right, under the field, e.g. "12/30".
                            Text("${name.length}/$MaxListNameLength", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(8.dp))

            LabeledField(label = stringResource(R.string.group_description_label)) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    shape = FieldShape,
                    colors = authFieldColors(),
                    placeholder = { Text(stringResource(R.string.list_description_placeholder)) },
                    // A few lines high from the start, so it's clear you can write more here.
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp),
                )
            }

            Spacer(Modifier.height(16.dp))

            LabeledField(label = stringResource(R.string.list_books_label)) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    singleLine = true,
                    shape = FieldShape,
                    colors = authFieldColors(),
                    placeholder = { Text(stringResource(R.string.list_books_search_placeholder)) },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Results while typing. "+" adds the book and hides it from the results.
            if (search.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                if (results.isEmpty()) {
                    Text(
                        stringResource(R.string.no_results),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        results.forEach { book ->
                            ListFormBookCard(book, iconRes = R.drawable.ic_add, iconDescription = stringResource(R.string.add_book_to_list, book.title)) {
                                addedIsbns = addedIsbns + book.isbn
                            }
                        }
                    }
                }
            }

            // The books already picked. "×" takes a book off again.
            if (addedBooks.isNotEmpty()) {
                SectionHeader(stringResource(R.string.list_books_added, addedBooks.size), Modifier.padding(top = 16.dp, bottom = 8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    addedBooks.forEach { book ->
                        ListFormBookCard(book, iconRes = R.drawable.ic_close, iconDescription = stringResource(R.string.remove_book_from_list, book.title)) {
                            addedIsbns = addedIsbns - book.isbn
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                // TODO: create the list with its books on the server first.
                onClick = { onCreateClick(trimmedName) },
                // Spaces alone don't count as a name. Books are optional – they can be added later too.
                enabled = trimmedName.isNotEmpty() && !nameTaken,
                shape = FieldShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(stringResource(R.string.create_list))
            }
        }
    }
}

// A book in the form: cover, title, author and one button on the right ("+" to add or "×" to remove).
@Composable
private fun ListFormBookCard(book: Book, iconRes: Int, iconDescription: String, onIconClick: () -> Unit) {
    AppCard {
        BookRow(
            title = book.title,
            subtitle = book.author,
            trailing = {
                IconButton(onClick = onIconClick) {
                    Icon(painterResource(iconRes), contentDescription = iconDescription, tint = MaterialTheme.colorScheme.primary)
                }
            },
        )
    }
}

@Preview(device = "id:pixel_8")
@Composable
private fun CreateListScreenPreview() {
    BookWormsTheme {
        CreateListScreen(onCreateClick = {}, onBackClick = {})
    }
}
