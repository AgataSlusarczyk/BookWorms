package com.bookworms.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.ui.components.AppBottomBar
import com.bookworms.app.ui.components.AppCard
import com.bookworms.app.ui.components.AppTopBar
import com.bookworms.app.ui.components.ButtonShape
import com.bookworms.app.ui.components.SegmentedTabs
import com.bookworms.app.ui.components.mistedBackground
import com.bookworms.app.ui.theme.BookWormsTheme
import com.bookworms.app.ui.theme.CardBorder
import com.bookworms.app.ui.theme.PlaceholderGray

@Composable
fun ProfileScreen(
    onNavigate: (String) -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToMyBooks: () -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onChangePasswordClick: () -> Unit = {},
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
                    // weight(1f) pushes the pencil to the right edge.
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Anna Kowalska", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "anna.kowalska@mail.com",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onEditProfileClick) {
                        Icon(painterResource(R.drawable.ic_edit), contentDescription = stringResource(R.string.edit_profile_title))
                    }
                }
            }

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
            item { ProfileMenuItem(R.drawable.ic_lock, stringResource(R.string.menu_change_password), onClick = onChangePasswordClick) }
            item { ExportDataItem() }

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
private fun ProfileMenuItem(
    iconResId: Int,
    title: String,
    subText: String? = null,
    trailingText: String? = null,
    onClick: () -> Unit = {},
) {
    AppCard(onClick = onClick) {
        ProfileMenuRow(iconResId, title, subText, trailingText)
    }
}

// Icon, title and an arrow on the right. When the item opens in place (expanded isn't null),
// the arrow points down while it's open.
@Composable
private fun ProfileMenuRow(
    iconResId: Int,
    title: String,
    subText: String? = null,
    trailingText: String? = null,
    expanded: Boolean? = null,
    modifier: Modifier = Modifier,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
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
        Icon(
            painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.rotate(if (expanded == true) 90f else 0f),
        )
    }
}

// "Export data" opens right here in the card: pick JSON or CSV, then download.
@Composable
private fun ExportDataItem() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var formatIndex by rememberSaveable { mutableIntStateOf(0) }
    val formats = listOf(stringResource(R.string.format_json), stringResource(R.string.format_csv))

    // No onClick on the card itself – only the top row opens and closes it,
    // so tapping the buttons inside doesn't close it by accident.
    AppCard {
        ProfileMenuRow(
            iconResId = R.drawable.ic_download,
            title = stringResource(R.string.menu_export_data),
            subText = if (expanded) null else "JSON/CSV",
            expanded = expanded,
            modifier = Modifier.clickable { expanded = !expanded },
        )
        if (expanded) {
            HorizontalDivider(color = CardBorder)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
                SegmentedTabs(tabs = formats, selectedIndex = formatIndex, onSelect = { formatIndex = it })
                Button(
                    // TODO: ask the server for the file in the chosen format.
                    onClick = { expanded = false },
                    shape = ButtonShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text(stringResource(R.string.action_download))
                }
            }
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
