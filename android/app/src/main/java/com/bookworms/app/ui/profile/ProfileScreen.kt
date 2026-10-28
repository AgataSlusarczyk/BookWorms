package com.bookworms.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookworms.app.R
import com.bookworms.app.ui.components.AppBackground
import com.bookworms.app.ui.components.AppBottomBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen() {
    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { 
                TopAppBar(
                    title = { Text(stringResource(R.string.profile_title), fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                ) 
            },
            bottomBar = { AppBottomBar("profile") }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                            Box(modifier = Modifier.size(64.dp).background(Color(0xFFE3E8FA), CircleShape))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Anna Kowalska", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                Text("anna.kowalska@mail.com", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            }
                        }
                    }
                }
                
                item { 
                    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), shape = RoundedCornerShape(4.dp)) {
                        Text(stringResource(R.string.profile_my_groups_count, 2), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp) 
                    }
                }
                
                item { GroupCard("Czytelnia 23", "14 ${stringResource(R.string.members)}", stringResource(R.string.role_admin)) }
                item { GroupCard("Rodzina", "5 ${stringResource(R.string.members)}", stringResource(R.string.role_member)) }
                
                item {
                    TextButton(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Icon(painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_add_group))
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
                
                item { ProfileMenuItem(R.drawable.ic_book, stringResource(R.string.menu_my_books), trailingText = "24") }
                item { ProfileMenuItem(R.drawable.ic_notifications, stringResource(R.string.notifications_title)) }
                item { ProfileMenuItem(R.drawable.ic_lock, stringResource(R.string.menu_change_password)) }
                item { ProfileMenuItem(R.drawable.ic_download, stringResource(R.string.menu_export_data), subText = "JSON/CSV") }

                item { Spacer(modifier = Modifier.height(24.dp)) }
                item {
                    TextButton(
                        onClick = { }, 
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                    ) {
                        Icon(painterResource(R.drawable.ic_logout), contentDescription = null, tint = Color.Red)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_logout), color = Color.Red, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun GroupCard(name: String, members: String, role: String) {
    OutlinedCard(colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).background(Color(0xFFE3E8FA), CircleShape))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(members, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(role, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun ProfileMenuItem(iconResId: Int, title: String, subText: String? = null, trailingText: String? = null) {
    OutlinedCard(colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth().clickable { }) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(id = iconResId), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    if (subText != null) Text(subText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (trailingText != null) Text(trailingText, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 8.dp))
                Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}