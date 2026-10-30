package com.bookworms.app.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.ui.components.AppBottomBar
import com.bookworms.app.ui.components.AppCard
import com.bookworms.app.ui.components.AppTopBar
import com.bookworms.app.ui.components.mistedBackground

@Composable
fun GroupsScreen(
    onNavigate: (String) -> Unit,
    onCreateGroupClick: () -> Unit
) {
    Scaffold(
        modifier = Modifier.mistedBackground(),
        containerColor = Color.Transparent,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.groups_title),
                actions = {
                    IconButton(onClick = onCreateGroupClick) {
                        Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.create_group_title))
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { /* TODO: Otwórz wyszukiwarkę grup */ },
                icon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
                text = { Text(stringResource(R.string.join_group)) }
            )
        },
        bottomBar = { AppBottomBar("groups", onNavigate) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { GroupCard(name = "Czytelnia 23", members = "14 members", role = "admin") }
            item { GroupCard(name = "Rodzina", members = "5 members", role = "member") }
        }
    }
}

@Composable
fun GroupCard(name: String, members: String, role: String) {
    AppCard(onClick = { }) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(), 
            horizontalArrangement = Arrangement.SpaceBetween, 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(name, style = MaterialTheme.typography.titleMedium)
                    Text(members, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Surface(
                shape = CircleShape, 
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = role, 
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), 
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}