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
import com.bookworms.app.ui.theme.CardBorder

// Also the Edit book form: pass the book, and the fields start filled in.
@Composable
internal fun AddBookManualScreen(
    onScanIsbnClick: () -> Unit,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit = {},
    book: OwnBook? = null,
) {
    // rememberSaveable, so typed text survives rotating the phone.
    var title by rememberSaveable { mutableStateOf(book?.title.orEmpty()) }
    var author by rememberSaveable { mutableStateOf(book?.author.orEmpty()) }
    var isbn by rememberSaveable { mutableStateOf(book?.isbn.orEmpty()) }
    var publisher by rememberSaveable { mutableStateOf(book?.publisher.orEmpty()) }
    var year by rememberSaveable { mutableStateOf(book?.year?.toString().orEmpty()) }

    Scaffold(
        // Transparent, so the misted picture behind the whole screen shows through.
        modifier = Modifier.mistedBackground(),
        containerColor = Color.Transparent,
        topBar = {
            AppTopBar(
                title = stringResource(if (book == null) R.string.add_book_title else R.string.edit_book_title),
                onBackClick = onBackClick,
            )
        },
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                // imePadding makes room for the keyboard, so the field you type in moves up above it.
                .imePadding()
                // Scrolls on small phones and when the keyboard is open.
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            // Only when adding – a book being edited already has its details.
            if (book == null) {
                ScanIsbnCard(onClick = onScanIsbnClick)

                // "or fill in the details yourself", with a thin line on both sides.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(color = CardBorder, modifier = Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.or_fill_in_yourself),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    HorizontalDivider(color = CardBorder, modifier = Modifier.weight(1f))
                }
            }

            BookField(label = stringResource(R.string.title_label), value = title, onValueChange = { title = it })
            BookField(label = stringResource(R.string.author_label), value = author, onValueChange = { author = it })
            BookField(
                label = stringResource(R.string.isbn_label),
                value = isbn,
                // ISBN is digits only (plus an X at the end of old 10-digit ones), so we keep just those.
                onValueChange = { isbn = it.filter { c -> c.isDigit() || c == 'X' || c == 'x' }.take(13) },
                keyboardType = KeyboardType.Number,
                // The barcode icon at the end opens the scanner too, right where the ISBN goes.
                trailingIcon = {
                    IconButton(onClick = onScanIsbnClick) {
                        Icon(painterResource(R.drawable.ic_barcode_scan), contentDescription = stringResource(R.string.scan_isbn_title))
                    }
                },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                BookField(
                    label = stringResource(R.string.publisher_label),
                    value = publisher,
                    onValueChange = { publisher = it },
                    modifier = Modifier.weight(2f),
                )
                BookField(
                    label = stringResource(R.string.year_label),
                    value = year,
                    onValueChange = { year = it.filter(Char::isDigit).take(4) },
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                // TODO: save the book on the server first (ViewModel + server).
                onClick = onSaveClick,
                // Title and author are the minimum to tell books apart.
                enabled = title.isNotBlank() && author.isNotBlank(),
                shape = FieldShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(stringResource(R.string.save_action))
            }
        }
    }
}

// Label above, white see-through field below – the same as on the sign-up and create group screens.
@Composable
private fun BookField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    Box(modifier = modifier) {
        LabeledField(label = label) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                shape = FieldShape,
                colors = authFieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                trailingIcon = trailingIcon,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// The quickest way to add a book: scan the barcode and the details fill in themselves.
@Composable
private fun ScanIsbnCard(onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Icon(painterResource(R.drawable.ic_barcode_scan), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.scan_isbn_title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.scan_isbn_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(device = "id:pixel_8")
@Composable
private fun AddBookManualScreenPreview() {
    BookWormsTheme {
        AddBookManualScreen(onScanIsbnClick = {}, onBackClick = {})
    }
}
