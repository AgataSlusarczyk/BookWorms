package com.bookworms.app.ui.groups

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bookworms.app.data.network.ApiClient
import com.bookworms.app.data.network.FriendDto
import com.bookworms.app.data.network.InviteUserRequestDto
import com.bookworms.app.ui.auth.FieldShape
import com.bookworms.app.ui.auth.authFieldColors
import com.bookworms.app.ui.components.AppCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun GroupInviteSearchSection(
    groupId: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<FriendDto>>(emptyList()) }
    var busyUserId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(groupId, query) {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            results = emptyList()
            return@LaunchedEffect
        }
        delay(300)
        results = runCatching {
            ApiClient.groups.searchInvitableUsers(groupId, trimmed)
        }.getOrDefault(emptyList())
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Invite by username",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search users by username") },
            singleLine = true,
            shape = FieldShape,
            colors = authFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )

        if (query.trim().length >= 2) {
            if (results.isEmpty()) {
                Text(
                    text = "No matching users found.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                results.forEach { user ->
                    AppCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "@${user.username}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Button(
                                enabled = busyUserId == null,
                                shape = FieldShape,
                                onClick = {
                                    busyUserId = user.id
                                    scope.launch {
                                        val ok = runCatching {
                                            ApiClient.groups
                                                .inviteUser(groupId, InviteUserRequestDto(user.id))
                                                .isSuccessful
                                        }.getOrDefault(false)

                                        if (ok) {
                                            results = results.filterNot { it.id == user.id }
                                            Toast.makeText(context, "Invitation sent to @${user.username}", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Could not send invitation", Toast.LENGTH_SHORT).show()
                                        }
                                        busyUserId = null
                                    }
                                },
                            ) {
                                Text("Invite")
                            }
                        }
                    }
                }
            }
        }
    }
}