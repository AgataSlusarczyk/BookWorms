package com.bookworms.app.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.ui.components.BackTitle
import com.bookworms.app.ui.components.mistedBackground

@Composable
fun AddBookManualScreen(onBackClick: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var isbn by remember { mutableStateOf("") }
    var publisher by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.mistedBackground(),
        containerColor = Color.Transparent,
        topBar = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BackTitle(title = stringResource(R.string.add_book_title), onBackClick = onBackClick)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BookInputField(value = title, label = stringResource(R.string.title_label), onValueChange = { title = it })
            BookInputField(value = author, label = stringResource(R.string.author_label), onValueChange = { author = it })
            BookInputField(value = isbn, label = stringResource(R.string.isbn_label), onValueChange = { isbn = it })
            
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                BookInputField(value = publisher, label = stringResource(R.string.publisher_label), onValueChange = { publisher = it }, modifier = Modifier.weight(1f))
                BookInputField(value = year, label = stringResource(R.string.year_label), onValueChange = { year = it }, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { /* TODO: Zapisz książkę w ViewModelu */ },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp),
                enabled = title.isNotBlank() && author.isNotBlank()
            ) {
                Text(stringResource(R.string.save_action))
            }
        }
    }
}

@Composable
fun BookInputField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}