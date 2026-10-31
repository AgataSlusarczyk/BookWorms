package com.bookworms.app.ui.groups

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.RadioButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.ui.auth.FieldShape
import com.bookworms.app.ui.auth.LabeledField
import com.bookworms.app.ui.auth.UsernamePattern
import com.bookworms.app.ui.auth.authFieldColors
import com.bookworms.app.ui.components.AppTopBar
import com.bookworms.app.ui.components.CardShape
import com.bookworms.app.ui.components.mistedBackground
import com.bookworms.app.ui.theme.BookWormsTheme
import com.bookworms.app.ui.theme.CardBorder

private const val MinNameLength = 3
private const val MaxNameLength = 40

// Who can see the group and how people get in. Each option has its own title,
// description, and hint under the "Create group" button.
enum class GroupVisibility(
    @StringRes val title: Int,
    @StringRes val description: Int,
    @StringRes val hint: Int,
) {
    Private(R.string.visibility_private, R.string.visibility_private_description, R.string.private_group_hint),
    Public(R.string.visibility_public, R.string.visibility_public_description, R.string.public_group_hint),
}

@Composable
fun CreateGroupScreen(
    name: String,
    description: String,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    visibility: GroupVisibility,
    onVisibilityChange: (GroupVisibility) -> Unit,
    invitedUsernames: List<String>,
    onAddInvite: (String) -> Unit,
    onRemoveInvite: (String) -> Unit,
    onCreateGroupClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        // Transparent, so the misted picture behind the whole screen shows through.
        modifier = modifier.mistedBackground(),
        containerColor = Color.Transparent,
        // The same top bar as the other screens opened from a tab (My books, Add book…).
        topBar = { AppTopBar(title = stringResource(R.string.create_group_title), onBackClick = onBackClick) },
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

            Spacer(Modifier.height(16.dp))

            LabeledField(label = stringResource(R.string.group_visibility_label)) {
                // selectableGroup tells screen readers these options belong together – you pick one.
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.selectableGroup(),
                ) {
                    GroupVisibility.entries.forEach { option ->
                        VisibilityOption(
                            option = option,
                            selected = option == visibility,
                            onSelect = { onVisibilityChange(option) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            InviteFriendsField(
                invitedUsernames = invitedUsernames,
                onAddInvite = onAddInvite,
                onRemoveInvite = onRemoveInvite,
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onCreateGroupClick,
                // Spaces alone don't count as a name.
                enabled = name.trim().length >= MinNameLength,
                shape = FieldShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(stringResource(R.string.create_group_button))
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(visibility.hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// One option card: radio button, title and a short description. The whole card is clickable.
@Composable
private fun VisibilityOption(option: GroupVisibility, selected: Boolean, onSelect: () -> Unit) {
    // The chosen card gets a blue border, the other one the light gray border of all cards.
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else CardBorder
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(if (selected) 2.dp else 1.dp, borderColor, CardShape)
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(end = 12.dp, top = 8.dp, bottom = 8.dp),
    ) {
        // null, because the card above already handles the click.
        RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(12.dp))
        Column {
            Text(stringResource(option.title), style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(option.description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Username field with a "+" button, and the added people as chips under it.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InviteFriendsField(
    invitedUsernames: List<String>,
    onAddInvite: (String) -> Unit,
    onRemoveInvite: (String) -> Unit,
) {
    // What's typed right now – only this screen needs it, so it lives here.
    var username by rememberSaveable { mutableStateOf("") }
    // People often type "@ola" – the "@" isn't part of the username, so we drop it.
    val cleaned = username.trim().removePrefix("@")
    val canAdd = UsernamePattern.matches(cleaned) && cleaned !in invitedUsernames

    val add = {
        if (canAdd) {
            onAddInvite(cleaned)
            username = ""
        }
    }

    LabeledField(label = stringResource(R.string.invite_friends_label)) {
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            singleLine = true,
            shape = FieldShape,
            colors = authFieldColors(),
            placeholder = { Text(stringResource(R.string.invite_friends_placeholder)) },
            trailingIcon = {
                IconButton(onClick = add, enabled = canAdd) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = stringResource(R.string.add_invite),
                    )
                }
            },
            // The keyboard's "Done" key adds the username too, so you don't have to reach for "+".
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { add() }),
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (invitedUsernames.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        // Chips go side by side and wrap to the next line when there's no room.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            invitedUsernames.forEach { invited ->
                InputChip(
                    selected = false,
                    onClick = { onRemoveInvite(invited) },
                    label = { Text("@$invited") },
                    trailingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.remove_invite, invited),
                            modifier = Modifier.size(InputChipDefaults.IconSize),
                        )
                    },
                    colors = InputChipDefaults.inputChipColors(containerColor = Color.White.copy(alpha = 0.8f)),
                )
            }
        }
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
            visibility = GroupVisibility.Private,
            onVisibilityChange = {},
            invitedUsernames = listOf("ola", "marek_w"),
            onAddInvite = {},
            onRemoveInvite = {},
            onCreateGroupClick = {},
            onBackClick = {},
        )
    }
}
