package com.bookworms.app.ui.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.data.Friend
import com.bookworms.app.data.LoadState
import com.bookworms.app.ui.auth.FieldShape
import com.bookworms.app.ui.auth.authFieldColors
import com.bookworms.app.ui.components.AppCard
import com.bookworms.app.ui.components.ProfilePhoto
import com.bookworms.app.ui.theme.BookWormsTheme

// "Invite by username" on Invite: a search field and the people found, each with an Invite button.
// results = null while there's nothing to show (see InviteSearchViewModel).
@Composable
internal fun GroupInviteSearchSection(
    query: String,
    results: LoadState<List<Friend>>?,
    invited: Set<String>,
    onQueryChange: (String) -> Unit,
    onInviteClick: (Friend) -> Unit,
    modifier: Modifier = Modifier,
    // true while an invitation is on its way – the buttons wait for it.
    saving: Boolean = false,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.invite_by_username),
            style = MaterialTheme.typography.titleMedium,
        )

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(stringResource(R.string.search_by_username)) },
            singleLine = true,
            shape = FieldShape,
            colors = authFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )

        when (results) {
            null, LoadState.Loading -> Unit
            LoadState.Failed -> SearchHint(stringResource(R.string.error_server_unreachable))
            is LoadState.Ready -> if (results.data.isEmpty()) {
                SearchHint(stringResource(R.string.no_users_found))
            } else {
                results.data.forEach { user ->
                    InvitableUserCard(
                        user = user,
                        invited = user.id in invited,
                        enabled = !saving,
                        onInviteClick = { onInviteClick(user) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// invited = the button turns into a gray "Invited", so nobody gets two invitations.
@Composable
private fun InvitableUserCard(user: Friend, invited: Boolean, enabled: Boolean, onInviteClick: () -> Unit) {
    AppCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            ProfilePhoto(photo = user.photoUrl, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.username_handle, user.username),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = onInviteClick, enabled = enabled && !invited, shape = FieldShape) {
                Text(stringResource(if (invited) R.string.invited else R.string.invite))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GroupInviteSearchSectionPreview() {
    BookWormsTheme {
        GroupInviteSearchSection(
            query = "an",
            results = LoadState.Ready(
                listOf(
                    Friend("1", "Anna Kowalska", "anna_k"),
                    Friend("2", "Jan Nowak", "janek"),
                ),
            ),
            invited = setOf("2"),
            onQueryChange = {},
            onInviteClick = {},
        )
    }
}
