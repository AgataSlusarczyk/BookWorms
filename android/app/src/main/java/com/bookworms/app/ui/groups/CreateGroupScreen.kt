package com.bookworms.app.ui.groups

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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

private const val MinNameLength = 3
private const val MaxNameLength = 40

// Also the Edit group form: pass onDeleteClick, and the form has "Save" instead of "Create group",
// the "Members can invite" switch and a "Delete group" button.
@Composable
internal fun CreateGroupScreen(
    name: String,
    description: String,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCreateGroupClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    // true while the group is on its way to the server – the buttons wait for it.
    saving: Boolean = false,
    membersCanInvite: Boolean = false,
    onMembersCanInviteChange: (Boolean) -> Unit = {},
    onDeleteClick: (() -> Unit)? = null,
) {
    val isEditing = onDeleteClick != null
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        // Transparent, so the misted picture behind the whole screen shows through.
        modifier = modifier.mistedBackground(),
        containerColor = Color.Transparent,
        // The same top bar as the other screens opened from a tab (My books, Add book…).
        topBar = {
            AppTopBar(
                title = stringResource(if (isEditing) R.string.edit_group_title else R.string.create_group_title),
                onBackClick = onBackClick,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                // imePadding makes room for the keyboard, so the field you type in moves up above it.
                .imePadding()
                // Scrolls on small phones and when the keyboard is open.
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            LabeledField(label = stringResource(R.string.group_name_label)) {
                OutlinedTextField(
                    value = name,
                    // Stops typing at the limit instead of showing an error.
                    onValueChange = { onNameChange(it.take(MaxNameLength)) },
                    singleLine = true,
                    shape = FieldShape,
                    colors = authFieldColors(),
                    placeholder = { Text(stringResource(R.string.group_name_placeholder)) },
                    // Counter on the right, under the field, e.g. "12/40".
                    supportingText = {
                        Text(
                            text = "${name.length}/$MaxNameLength",
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(8.dp))

            LabeledField(label = stringResource(R.string.group_description_label)) {
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    shape = FieldShape,
                    colors = authFieldColors(),
                    placeholder = { Text(stringResource(R.string.group_description_placeholder)) },
                    // A few lines high from the start, so it's clear you can write more here.
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp),
                )
            }

            if (isEditing) {
                Spacer(Modifier.height(16.dp))
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.members_can_invite), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                stringResource(R.string.members_can_invite_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = membersCanInvite, onCheckedChange = onMembersCanInviteChange)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onCreateGroupClick,
                // Spaces alone don't count as a name.
                enabled = name.trim().length >= MinNameLength && !saving,
                shape = FieldShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Text(stringResource(if (isEditing) R.string.save_action else R.string.create_group_button))
                }
            }

            if (!isEditing) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.create_group_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (isEditing) {
                Spacer(Modifier.height(16.dp))
                TextButton(
                    onClick = { showDeleteDialog = true },
                    enabled = !saving,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.delete_group))
                }
            }
        }
    }

    // Asks first, because a deleted group can't be brought back.
    if (showDeleteDialog && onDeleteClick != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_group_title, name)) },
            text = { Text(stringResource(R.string.delete_group_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
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
private fun CreateGroupScreenPreview() {
    BookWormsTheme {
        CreateGroupScreen(
            name = "Book club 23",
            description = "",
            onNameChange = {},
            onDescriptionChange = {},
            onCreateGroupClick = {},
            onBackClick = {},
        )
    }
}
