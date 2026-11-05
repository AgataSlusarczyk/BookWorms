package com.bookworms.app.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.ui.auth.FieldShape
import com.bookworms.app.ui.auth.authFieldColors
import com.bookworms.app.ui.components.AppTopBar
import com.bookworms.app.ui.components.mistedBackground
import com.bookworms.app.ui.theme.BookWormsTheme

@Composable
fun AddBookManualScreen(
    onSaveBook: (title: String, author: String, isbn: String, publisher: String, year: Int) -> Unit,
    onBackClick: () -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var author by rememberSaveable { mutableStateOf("") }
    var isbn by rememberSaveable { mutableStateOf("") }
    var publisher by rememberSaveable { mutableStateOf("") }
    var year by rememberSaveable { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.mistedBackground(),
        containerColor = Color.Transparent,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.add_book_title),
                onBackClick = onBackClick,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BookInputField(
                value = title,
                label = stringResource(R.string.title_label),
                onValueChange = { title = it },
            )
            BookInputField(
                value = author,
                label = stringResource(R.string.author_label),
                onValueChange = { author = it },
            )
            BookInputField(
                value = isbn,
                label = stringResource(R.string.isbn_label),
                onValueChange = { isbn = it },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                BookInputField(
                    value = publisher,
                    label = stringResource(R.string.publisher_label),
                    onValueChange = { publisher = it },
                    modifier = Modifier.weight(1f),
                )
                BookInputField(
                    value = year,
                    label = stringResource(R.string.year_label),
                    onValueChange = { year = it.filter { ch -> ch.isDigit() }.take(4) },
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val parsedYear = year.toIntOrNull() ?: 0
                    val finalIsbn = isbn.trim().ifEmpty { "9780000000000" }
                    onSaveBook(title.trim(), author.trim(), finalIsbn, publisher.trim(), parsedYear)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = FieldShape,
                enabled = title.isNotBlank() && author.isNotBlank(),
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}

@Composable
private fun BookInputField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth(),
        shape = FieldShape,
        colors = authFieldColors(),
    )
}

@Preview(device = "id:pixel_8")
@Composable
private fun AddBookManualScreenPreview() {
    BookWormsTheme {
        AddBookManualScreen(onSaveBook = { _, _, _, _, _ -> }, onBackClick = {})
    }
}