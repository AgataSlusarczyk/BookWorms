package com.bookworms.app.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.ui.auth.FieldShape
import com.bookworms.app.ui.auth.LabeledField
import com.bookworms.app.ui.auth.authFieldColors
import com.bookworms.app.ui.browse.Book
import com.bookworms.app.ui.components.AppCard
import com.bookworms.app.ui.components.AppTopBar
import com.bookworms.app.ui.components.mistedBackground
import com.bookworms.app.ui.theme.BookWormsTheme

@Composable
internal fun AddBookManualScreen( // <--- Słowo 'internal' rozwiązuje konflikt dostępności OwnBook
    onScanIsbnClick: () -> Unit,
    onBackClick: () -> Unit,
    onSaveClick: (OwnBook) -> Unit,
    // The user's copy, passed in when tapping "Edit book". null when adding a new book.
    book: OwnBook? = null,
    // Prefills the form when tapping "I own this book" or scanning an ISBN.
    prefill: Book? = null,
) {
    val isEdit = book != null
    var title by rememberSaveable(book, prefill) { mutableStateOf(book?.title ?: prefill?.title ?: "") }
    var author by rememberSaveable(book, prefill) { mutableStateOf(book?.author ?: prefill?.author ?: "") }
    var isbn by rememberSaveable(book, prefill) { mutableStateOf(book?.isbn ?: prefill?.isbn ?: "") }
    var publisher by rememberSaveable(book, prefill) { mutableStateOf(book?.publisher ?: "") }
    var year by rememberSaveable(book, prefill) {
        val y = book?.year ?: prefill?.year ?: 0
        mutableStateOf(if (y > 0) y.toString() else "")
    }

    Scaffold(
        modifier = Modifier.mistedBackground(),
        containerColor = Color.Transparent,
        topBar = {
            AppTopBar(
                title = stringResource(if (isEdit) R.string.edit_book_title else R.string.add_book_title),
                onBackClick = onBackClick,
            )
        },
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            // Scanner isn't needed when we're already editing an existing book.
            if (!isEdit) {
                AppCard(onClick = onScanIsbnClick) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                        Icon(painterResource(R.drawable.ic_barcode_scan), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(stringResource(R.string.scan_isbn_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.scan_isbn_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        stringResource(R.string.or_fill_in_yourself),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            BookInputField(
                label = stringResource(R.string.title_label),
                value = title,
                onValueChange = { title = it },
            )
            BookInputField(
                label = stringResource(R.string.author_label),
                value = author,
                onValueChange = { author = it },
            )
            BookInputField(
                label = stringResource(R.string.isbn_label),
                value = isbn,
                onValueChange = { newValue ->
                    isbn = newValue.filter { it.isDigit() || it == '-' || it.equals('X', ignoreCase = true) }.take(17)
                },
                keyboardType = KeyboardType.Number,
                trailingIcon = {
                    if (!isEdit) {
                        IconButton(onClick = onScanIsbnClick) {
                            Icon(
                                painter = painterResource(R.drawable.ic_barcode_scan),
                                contentDescription = stringResource(R.string.scan_isbn_title),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                BookInputField(
                    label = stringResource(R.string.publisher_label),
                    value = publisher,
                    onValueChange = { publisher = it },
                    modifier = Modifier.weight(1f),
                )
                BookInputField(
                    label = stringResource(R.string.year_label),
                    value = year,
                    // The year only has numbers and fits in 4 digits.
                    onValueChange = { year = it.filter(Char::isDigit).take(4) },
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val edited = OwnBook(
                        title = title.trim(),
                        author = author.trim(),
                        year = year.toIntOrNull() ?: 0,
                        isbn = isbn.trim(),
                        publisher = publisher.trim(),
                        // In edit mode we keep the original data (e.g. who borrowed it).
                        groups = book?.groups ?: myGroups,
                        lentTo = book?.lentTo,
                        lentSince = book?.lentSince,
                        returnBy = book?.returnBy,
                    )
                    onSaveClick(edited)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = FieldShape,
                // The title and author are minimum requirements for a book.
                enabled = title.isNotBlank() && author.isNotBlank(),
            ) {
                Text(stringResource(R.string.save_action))
            }
        }
    }
}

@Composable
private fun BookInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    Box(modifier = modifier) {
        LabeledField(label = label) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth(),
                shape = FieldShape,
                colors = authFieldColors(),
                trailingIcon = trailingIcon
            )
        }
    }
}

@Preview(device = "id:pixel_8")
@Composable
private fun AddBookManualScreenPreview() {
    BookWormsTheme {
        AddBookManualScreen(onScanIsbnClick = {}, onBackClick = {}, onSaveClick = {})
    }
}