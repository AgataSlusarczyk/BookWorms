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
import com.bookworms.app.ui.components.AppCard
import com.bookworms.app.ui.components.AppTopBar
import com.bookworms.app.ui.components.mistedBackground
import com.bookworms.app.ui.theme.BookWormsTheme

// Also the Edit book form: pass the book as editing, and the fields start filled in. Its ISBN can't be changed.
// prefill = a book from the catalog ("I own this book" on Book details): still adding, but the fields start filled in.
@Composable
internal fun AddBookManualScreen( // <--- Słowo 'internal' rozwiązuje konflikt dostępności OwnBook
    onScanIsbnClick: () -> Unit,
    onBackClick: () -> Unit,
    // Gets the book as it is on the form.
    onSaveClick: (BookForm) -> Unit = {},
    editing: BookForm? = null,
    prefill: BookForm? = null,
    // true while the book is on its way to the server – Save waits for it.
    saving: Boolean = false,
) {
    val start = editing ?: prefill ?: BookForm()
    // rememberSaveable, so typed text survives rotating the phone.
    var title by rememberSaveable { mutableStateOf(start.title) }
    var author by rememberSaveable { mutableStateOf(start.author) }
    var isbn by rememberSaveable { mutableStateOf(start.isbn) }
    var publisher by rememberSaveable { mutableStateOf(start.publisher) }
    var year by rememberSaveable { mutableStateOf(start.year?.toString().orEmpty()) }
    // The server takes old 10-digit ISBNs and new 13-digit ones.
    val isbnValid = isbn.isEmpty() || isbn.length == 10 || isbn.length == 13

    Scaffold(
        modifier = Modifier.mistedBackground(),
        containerColor = Color.Transparent,
        topBar = {
            AppTopBar(
                title = stringResource(if (editing == null) R.string.add_book_title else R.string.edit_book_title),
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
            // Only when adding a new book – an edited or prefilled book already has its details.
            if (editing == null && prefill == null) {
                ScanIsbnCard(onClick = onScanIsbnClick)

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
                // ISBN is digits only (plus an X at the end of old 10-digit ones), so we keep just those.
                onValueChange = { isbn = it.filter { c -> c.isDigit() || c == 'X' || c == 'x' }.uppercase().take(13) },
                keyboardType = KeyboardType.Number,
                // The server keeps the ISBN of a saved book for good, so editing can't change it.
                enabled = editing == null,
                error = if (isbnValid) null else stringResource(R.string.isbn_length_hint),
                // The barcode icon at the end opens the scanner too, right where the ISBN goes.
                trailingIcon = if (editing != null) null else {
                    {
                        IconButton(onClick = onScanIsbnClick) {
                            Icon(painterResource(R.drawable.ic_barcode_scan), contentDescription = stringResource(R.string.scan_isbn_title))
                        }
                    }
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
                    onSaveClick(
                        BookForm(
                            title = title.trim(),
                            author = author.trim(),
                            isbn = isbn,
                            publisher = publisher.trim(),
                            year = year.toIntOrNull(),
                        ),
                    )
                    onSaveClick(edited)
                },
                // Title and author are the minimum to tell books apart.
                enabled = title.isNotBlank() && author.isNotBlank() && isbnValid && !saving,
                shape = FieldShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = FieldShape,
                // The title and author are minimum requirements for a book.
                enabled = title.isNotBlank() && author.isNotBlank(),
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Text(stringResource(R.string.save_action))
                }
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
    trailingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    // Shown in red under the field. null = the field is fine.
    error: String? = null,
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
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                trailingIcon = trailingIcon,
                enabled = enabled,
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
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