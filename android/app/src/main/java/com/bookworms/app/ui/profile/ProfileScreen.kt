package com.bookworms.app.ui.profile

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.ui.components.AppBottomBar
import com.bookworms.app.ui.components.AppCard
import com.bookworms.app.ui.components.AppTopBar
import com.bookworms.app.ui.components.ButtonShape
import com.bookworms.app.ui.components.SectionHeader
import com.bookworms.app.ui.components.mistedBackground
import com.bookworms.app.ui.theme.BookWormsTheme
import com.bookworms.app.ui.theme.CardBorder
import com.bookworms.app.ui.theme.PlaceholderGray

@Composable
fun ProfileScreen(
    onNavigate: (String) -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToMyBooks: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
) {
    Scaffold(
        // Transparent, so the misted picture behind the whole screen shows through.
        modifier = Modifier.mistedBackground(),
        containerColor = Color.Transparent,
        topBar = { AppTopBar(title = stringResource(R.string.profile_title)) },
        bottomBar = { AppBottomBar(currentRoute = "profile", onNavigate = onNavigate) },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                    Box(modifier = Modifier.size(56.dp).background(PlaceholderGray, CircleShape))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Anna Kowalska", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "anna.kowalska@mail.com",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item { SectionHeader(stringResource(R.string.profile_my_groups_count, 2)) }
            item {
                GroupCard(
                    name = "Czytelnia 23",
                    members = pluralStringResource(R.plurals.members, 14, 14),
                    role = stringResource(R.string.role_admin),
                )
            }
            item {
                GroupCard(
                    name = "Rodzina",
                    members = pluralStringResource(R.plurals.members, 5, 5),
                    role = stringResource(R.string.role_member),
                )
            }
            item {
                // Same white look and border as the cards, so it reads as part of the list.
                OutlinedButton(
                    onClick = { },
                    shape = ButtonShape,
                    border = BorderStroke(1.dp, CardBorder),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_add_group))
                }
            }

            item { Spacer(Modifier.height(4.dp)) }

            item {
                ProfileMenuItem(
                    iconResId = R.drawable.ic_book,
                    title = stringResource(R.string.menu_my_books),
                    trailingText = "24",
                    onClick = onNavigateToMyBooks,
                )
            }
            item {
                ProfileMenuItem(
                    iconResId = R.drawable.ic_notifications,
                    title = stringResource(R.string.notifications_title),
                    onClick = onNavigateToNotifications,
                )
            }
            item { ProfileMenuItem(R.drawable.ic_lock, stringResource(R.string.menu_change_password)) }
            item { ProfileMenuItem(R.drawable.ic_download, stringResource(R.string.menu_export_data), subText = "JSON/CSV") }

            item {
                TextButton(
                    onClick = onLogoutClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_logout), contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_logout), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun GroupCard(name: String, members: String, role: String) {
    AppCard {
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

@Composable
private fun ProfileMenuItem(
    iconResId: Int,
    title: String,
    subText: String? = null,
    trailingText: String? = null,
    onClick: () -> Unit = {},
) {
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Icon(painterResource(iconResId), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (subText != null) {
                    Text(subText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (trailingText != null) {
                Text(
                    trailingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(device = "id:pixel_8")
@Composable
private fun ProfileScreenPreview() {
    BookWormsTheme {
        ProfileScreen()
    }
}
