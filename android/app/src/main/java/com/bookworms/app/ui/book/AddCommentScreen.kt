package com.bookworms.app.ui.book

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.ui.auth.FieldShape
import com.bookworms.app.ui.auth.LabeledField
import com.bookworms.app.ui.auth.authFieldColors
import com.bookworms.app.ui.components.AppTopBar
import com.bookworms.app.ui.components.BookCoverPlaceholder
import com.bookworms.app.ui.components.mistedBackground
import com.bookworms.app.ui.theme.BookWormsTheme

private const val MaxCommentLength = 500

@Composable
fun AddCommentScreen(
    bookTitle: String,
    bookAuthor: String,
    onPostClick: (text: String, hasSpoilers: Boolean) -> Unit,
    onBackClick: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    var hasSpoilers by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        // Transparent, so the misted picture behind the whole screen shows through.
        modifier = Modifier.mistedBackground(),
        containerColor = Color.Transparent,
        topBar = { AppTopBar(title = stringResource(R.string.add_comment_title), onBackClick = onBackClick) },
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
            // Which book the comment is about, so it's clear even after a long time typing.
            Row(verticalAlignment = Alignment.CenterVertically) {
                BookCoverPlaceholder()
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(bookTitle, style = MaterialTheme.typography.titleMedium)
                    Text(bookAuthor, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(20.dp))

            LabeledField(label = stringResource(R.string.your_comment_label)) {
                OutlinedTextField(
                    value = text,
                    // Stops typing at the limit instead of showing an error.
                    onValueChange = { text = it.take(MaxCommentLength) },
                    shape = FieldShape,
                    colors = authFieldColors(),
                    placeholder = { Text(stringResource(R.string.comment_placeholder)) },
                    // Counter on the right, under the field, e.g. "120/500".
                    supportingText = {
                        Text("${text.length}/$MaxCommentLength", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    // A few lines high from the start, so it's clear you can write more here.
                    minLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                )
            }

            Text(
                text = stringResource(R.string.comment_visibility_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            Spacer(Modifier.height(12.dp))

            // The whole row is clickable, not only the small box – easier to hit with a finger.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(value = hasSpoilers, onValueChange = { hasSpoilers = it }, role = Role.Checkbox),
            ) {
                // null, because the row above already handles the click.
                Checkbox(checked = hasSpoilers, onCheckedChange = null)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(stringResource(R.string.contains_spoilers), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.contains_spoilers_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                // TODO: send the comment to the server first.
                onClick = { onPostClick(text.trim(), hasSpoilers) },
                // Spaces alone don't count as a comment.
                enabled = text.isNotBlank(),
                shape = FieldShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(stringResource(R.string.post_comment))
            }
        }
    }
}

@Preview(device = "id:pixel_8")
@Composable
private fun AddCommentScreenPreview() {
    BookWormsTheme {
        AddCommentScreen(bookTitle = "Solaris", bookAuthor = "Stanisław Lem", onPostClick = { _, _ -> }, onBackClick = {})
    }
}
