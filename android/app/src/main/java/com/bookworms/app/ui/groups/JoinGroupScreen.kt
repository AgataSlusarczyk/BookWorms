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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.painterResource
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
import com.bookworms.app.ui.components.SectionHeader
import com.bookworms.app.ui.components.StatusTag
import com.bookworms.app.ui.components.mistedBackground
import com.bookworms.app.ui.group.sampleInvite
import com.bookworms.app.ui.theme.BookWormsTheme
import com.bookworms.app.ui.theme.BrandBlue
import com.bookworms.app.ui.theme.StatusBlueBg

// Temporary data until groups come from the server.
private data class JoinableGroup(val name: String, val members: Int, val description: String, val isPrivate: Boolean)

// The group behind sampleInvite's code, so typing BKX-7F2Q finds something.
private val invitedGroup = JoinableGroup(
    name = sampleInvite.groupName,
    members = 4,
    description = "Sci-fi fans from work. We meet every first Thursday.",
    isPrivate = true,
)

private val samplePublicGroups = listOf(
    JoinableGroup("Kryminały po godzinach", 18, "Crime novels, one book a month.", isPrivate = false),
    JoinableGroup("Fantasy Kraków", 42, "Swapping fantasy books around Kraków.", isPrivate = false),
    JoinableGroup("Reportaże", 9, "Polish and foreign non-fiction.", isPrivate = false),
)

// What happened after "Find group": nothing yet, a group was found, or the code is wrong.
private enum class CodeResult { None, Found, NotFound }

@Composable
fun JoinGroupScreen(
    // Public groups let you in right away, so the app can open the group.
    onGroupJoined: (groupName: String) -> Unit,
    onBackClick: () -> Unit,
) {
    var code by rememberSaveable { mutableStateOf("") }
    var codeResult by rememberSaveable { mutableStateOf(CodeResult.None) }
    // A private group needs the admin's yes, so after asking we only show that the request is on its way.
    var requestSent by rememberSaveable { mutableStateOf(false) }
    var search by rememberSaveable { mutableStateOf("") }

    // TODO: ask the server for the group with this code.
    val findGroup = {
        codeResult = if (code.trim().equals(sampleInvite.code, ignoreCase = true)) CodeResult.Found else CodeResult.NotFound
    }
    val publicGroups = samplePublicGroups.filter { it.name.contains(search.trim(), ignoreCase = true) }

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
                            codeResult = CodeResult.None
                            requestSent = false
                        },
                        placeholder = { Text(sampleInvite.code) },
                        singleLine = true,
                        shape = FieldShape,
                        colors = authFieldColors(),
                        isError = codeResult == CodeResult.NotFound,
                        supportingText = {
                            Text(
                                stringResource(
                                    if (codeResult == CodeResult.NotFound) R.string.error_invite_code_not_found else R.string.invite_code_hint,
                                ),
                            )
                        },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { if (code.isNotBlank()) findGroup() }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                Button(
                    onClick = findGroup,
                    enabled = code.isNotBlank(),
                    shape = ButtonShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text(stringResource(R.string.find_group))
                }
            }

            if (codeResult == CodeResult.Found) {
                item {
                    FoundGroupCard(
                        group = invitedGroup,
                        requestSent = requestSent,
                        // TODO: send the join request to the server.
                        onAskToJoinClick = { requestSent = true },
                    )
                }
            }

            item { SectionHeader(stringResource(R.string.public_groups), Modifier.padding(top = 12.dp)) }
            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text(stringResource(R.string.search_groups)) },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
                    singleLine = true,
                    shape = FieldShape,
                    colors = authFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (publicGroups.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.no_groups_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(publicGroups, key = { it.name }) { group ->
                // TODO: join on the server first.
                PublicGroupCard(group, onJoinClick = { onGroupJoined(group.name) })
            }
        }
    }
}

// The group the code belongs to: picture, name, "4 members · Private", description and "Ask to join".
@Composable
private fun FoundGroupCard(group: JoinableGroup, requestSent: Boolean, onAskToJoinClick: () -> Unit) {
    AppCard(borderColor = BrandBlue) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
            GroupSummary(group)
            Text(group.description, style = MaterialTheme.typography.bodyMedium)
            if (requestSent) {
                StatusTag(stringResource(R.string.join_request_sent), dotColor = BrandBlue, backgroundColor = StatusBlueBg, textColor = BrandBlue)
                Text(
                    stringResource(R.string.join_request_sent_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Button(
                    onClick = onAskToJoinClick,
                    shape = ButtonShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text(stringResource(R.string.ask_to_join))
                }
            }
        }
    }
}

// Anyone can join a public group, so there's a "Join" button right on the card.
@Composable
private fun PublicGroupCard(group: JoinableGroup, onJoinClick: () -> Unit) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                GroupSummary(group)
                Text(
                    group.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onJoinClick, shape = ButtonShape) {
                Text(stringResource(R.string.action_join))
            }
        }
    }
}

// Picture, name and "4 members · Private" – the same as at the top of Group details.
@Composable
private fun GroupSummary(group: JoinableGroup) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AvatarPlaceholder()
        Spacer(Modifier.width(12.dp))
        Column {
            Text(group.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = stringResource(
                    R.string.group_members_visibility,
                    pluralStringResource(R.plurals.members, group.members, group.members),
                    stringResource(if (group.isPrivate) R.string.visibility_private else R.string.visibility_public),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(device = "id:pixel_8")
@Composable
private fun JoinGroupScreenPreview() {
    BookWormsTheme {
        JoinGroupScreen(onGroupJoined = {}, onBackClick = {})
    }
}
