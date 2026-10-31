package com.bookworms.app.ui.lists

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.ui.theme.BookWormsTheme

// The Create list form, filled in with the list's current name and books,
// plus a "Delete list" button at the bottom.
@Composable
fun EditListScreen(
    listName: String,
    // Gets the name after the changes, so the app can open the list under its new name.
    onSaveClick: (listName: String) -> Unit,
    onDeleteClick: () -> Unit,
    onBookClick: (isbn: String) -> Unit,
    onBackClick: () -> Unit,
) {
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    ListFormScreen(
        title = stringResource(R.string.edit_list_title),
        buttonText = stringResource(R.string.save_action),
        searchLabel = R.string.add_books,
        addedHeader = R.string.list_books_on_list,
        // TODO: save the changes on the server first.
        onSubmit = onSaveClick,
        onBookClick = onBookClick,
        onBackClick = onBackClick,
        initialName = listName,
        // TODO: the list's books (and description) from the server.
        initialIsbns = sampleListBooks.map { it.book.isbn },
    ) {
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = { showDeleteDialog = true },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.delete_list))
        }
    }

    // Asks first, because a deleted list can't be brought back.
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_list_title, listName)) },
            text = { Text(stringResource(R.string.delete_list_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        // TODO: delete the list on the server first.
                        onDeleteClick()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.delete_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Preview(device = "id:pixel_8")
@Composable
private fun EditListScreenPreview() {
    BookWormsTheme {
        EditListScreen(listName = "Sci-fi", onSaveClick = {}, onDeleteClick = {}, onBookClick = {}, onBackClick = {})
    }
}
