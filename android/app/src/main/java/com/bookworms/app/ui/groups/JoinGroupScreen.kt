package com.bookworms.app.ui.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.ui.auth.FieldShape
import com.bookworms.app.ui.auth.LabeledField
import com.bookworms.app.ui.auth.authFieldColors
import com.bookworms.app.ui.components.AppCard
import com.bookworms.app.ui.components.AppTopBar
import com.bookworms.app.ui.components.AvatarPlaceholder
import com.bookworms.app.ui.components.ButtonShape
import com.bookworms.app.ui.components.mistedBackground
import com.bookworms.app.ui.theme.BookWormsTheme
import com.bookworms.app.ui.theme.BrandBlue

// Type the code from a friend, see the group, join it. Anyone with the code gets in right away.
@Composable
internal fun JoinGroupScreen(
    // The group behind the code after "Find group". null = nothing found yet.
    found: InvitePreview?,
    // true = no group has this code.
    notFound: Boolean,
    // true while the server is asked – the buttons wait for it.
    busy: Boolean,
    onCodeChange: () -> Unit,
    onFindClick: (code: String) -> Unit,
    onJoinClick: (code: String) -> Unit,
    onBackClick: () -> Unit,
) {
    var code by rememberSaveable { mutableStateOf("") }

    Scaffold(
        // Transparent, so the misted picture behind the whole screen shows through.
        modifier = Modifier.mistedBackground(),
        containerColor = Color.Transparent,
        topBar = { AppTopBar(title = stringResource(R.string.join_group), onBackClick = onBackClick) },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                // imePadding makes room for the keyboard, so the field you type in moves up above it.
                .imePadding(),
        ) {
            item {
                LabeledField(label = stringResource(R.string.invite_code_label)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = {
                            // Codes are always in capitals, so we write them that way – and a new code means a new search.
                            code = it.uppercase()
                            onCodeChange()
                        },
                        placeholder = { Text(CodeExample) },
                        singleLine = true,
                        shape = FieldShape,
                        colors = authFieldColors(),
                        isError = notFound,
                        supportingText = {
                            Text(stringResource(if (notFound) R.string.error_invite_code_not_found else R.string.invite_code_hint))
                        },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { if (code.isNotBlank() && !busy) onFindClick(code) }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                Button(
                    onClick = { onFindClick(code) },
                    enabled = code.isNotBlank() && !busy,
                    shape = ButtonShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    if (busy && found == null) {
                        CircularProgressIndicator(strokeWidth = 2.dp, color = LocalContentColor.current, modifier = Modifier.size(20.dp))
                    } else {
                        Text(stringResource(R.string.find_group))
                    }
                }
            }

            if (found != null) {
                item {
                    FoundGroupCard(group = found, busy = busy, onJoinClick = { onJoinClick(code) })
                }
            }
        }
    }
}

// How a code looks, in the empty field.
private const val CodeExample = "K7QM-3XTP"

// The group the code belongs to: picture, name, "4 members", description and "Join" (or "Open group").
@Composable
private fun FoundGroupCard(group: InvitePreview, busy: Boolean, onJoinClick: () -> Unit) {
    AppCard(borderColor = BrandBlue) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarPlaceholder()
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(group.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        pluralStringResource(R.plurals.members, group.memberCount, group.memberCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (group.description.isNotBlank()) {
                Text(group.description, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = onJoinClick,
                enabled = !busy,
                shape = ButtonShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                // Joining again does nothing new, so for a member the button just opens the group.
                Text(stringResource(if (group.alreadyMember) R.string.open_group else R.string.action_join))
            }
        }
    }
}

@Preview(device = "id:pixel_8")
@Composable
private fun JoinGroupScreenPreview() {
    BookWormsTheme {
        JoinGroupScreen(
            found = InvitePreview("Czytelnia 23", "Sci-fi fans from work.", memberCount = 4, alreadyMember = false),
            notFound = false,
            busy = false,
            onCodeChange = {},
            onFindClick = {},
            onJoinClick = {},
            onBackClick = {},
        )
    }
}
