package com.bookworms.app.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.ui.components.AppBottomBar
import com.bookworms.app.ui.components.AppCard
import com.bookworms.app.ui.components.AppTopBar
import com.bookworms.app.ui.components.SectionHeader
import com.bookworms.app.ui.components.mistedBackground
import com.bookworms.app.ui.theme.BookWormsTheme
import com.bookworms.app.ui.theme.PlaceholderGray

@Composable
fun GroupsScreen(
    onNavigate: (String) -> Unit = {},
    onCreateGroupClick: () -> Unit = {},
    onJoinGroupClick: () -> Unit = {},
    onGroupClick: (String) -> Unit = {},
) {
    Scaffold(
        // Transparent, so the misted picture behind the whole screen shows through.
        modifier = Modifier.mistedBackground(),
        containerColor = Color.Transparent,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.groups_title),
                actions = {
                    IconButton(onClick = onCreateGroupClick) {
                        Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.create_group_title))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onJoinGroupClick,
                icon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
                text = { Text(stringResource(R.string.join_group)) },
            )
        },
        bottomBar = { AppBottomBar(currentRoute = "groups", onNavigate = onNavigate) },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            item { SectionHeader(stringResource(R.string.profile_my_groups_count, 2)) }
            item {
                GroupCard(
                    name = "Czytelnia 23",
                    members = pluralStringResource(R.plurals.members, 14, 14),
                    role = stringResource(R.string.role_admin),
                    onClick = { onGroupClick("Czytelnia 23") },
                )
            }
            item {
                GroupCard(
                    name = "Rodzina",
                    members = pluralStringResource(R.plurals.members, 5, 5),
                    role = stringResource(R.string.role_member),
                    onClick = { onGroupClick("Rodzina") },
                )
            }
        }
    }
}

// Gray circle in place of the group picture, name, member count and the user's role on the right.
@Composable
private fun GroupCard(name: String, members: String, role: String, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
           Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Box(modifier = Modifier.size(40.dp).background(PlaceholderGray, CircleShape))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(members, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = role,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .background(PlaceholderGray, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@Preview(device = "id:pixel_8")
@Composable
private fun GroupsScreenPreview() {
    BookWormsTheme {
        GroupsScreen()
    }
}
