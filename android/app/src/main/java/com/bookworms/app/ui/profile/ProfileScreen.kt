package com.bookworms.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.ui.auth.FieldShape
import com.bookworms.app.ui.components.AppBottomBar
import com.bookworms.app.ui.components.AppCard
import com.bookworms.app.ui.components.AppTopBar
import com.bookworms.app.ui.components.mistedBackground
import com.bookworms.app.ui.theme.BookWormsTheme
import com.bookworms.app.ui.theme.CardBorder

@Composable
fun ProfileScreen(
    onNavigate: (String) -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToMyBooks: () -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    var passwordExpanded by rememberSaveable { mutableStateOf(false) }
    var exportExpanded by rememberSaveable { mutableStateOf(false) }

    var currentPassword by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var repeatPassword by rememberSaveable { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.mistedBackground(),
        containerColor = Color.Transparent,
        topBar = { AppTopBar(title = stringResource(R.string.profile_title)) },
        bottomBar = { AppBottomBar("profile", onNavigate) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(64.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Anna Kowalska", style = MaterialTheme.typography.titleLarge)
                            Text("anna.kowalska@mail.com", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = onEditProfileClick) {
                        Icon(painterResource(R.drawable.ic_edit), contentDescription = stringResource(R.string.content_desc_edit))
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
            
            item { ProfileSimpleMenuItem(R.drawable.ic_book, stringResource(R.string.menu_my_books), trailingText = "24", onClick = onNavigateToMyBooks) }
            item { ProfileSimpleMenuItem(R.drawable.ic_notifications, stringResource(R.string.notifications_title), onClick = onNavigateToNotifications) }
            
            item {
                ExpandableProfileMenu(
                    iconResId = R.drawable.ic_lock,
                    title = stringResource(R.string.menu_change_password),
                    isExpanded = passwordExpanded,
                    onClick = { passwordExpanded = !passwordExpanded }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = currentPassword, 
                            onValueChange = { currentPassword = it }, 
                            label = { Text(stringResource(R.string.current_password)) }, 
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            shape = FieldShape,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newPassword, 
                            onValueChange = { newPassword = it }, 
                            label = { Text(stringResource(R.string.new_password)) }, 
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            shape = FieldShape,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = repeatPassword, 
                            onValueChange = { repeatPassword = it }, 
                            label = { Text(stringResource(R.string.repeat_new_password)) }, 
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            shape = FieldShape,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = { passwordExpanded = false }, 
                            enabled = currentPassword.isNotBlank() && newPassword.isNotBlank(),
                            modifier = Modifier.align(Alignment.End)
                        ) { Text(stringResource(R.string.action_save)) }
                    }
                }
            }

            item {
                ExpandableProfileMenu(
                    iconResId = R.drawable.ic_download,
                    title = stringResource(R.string.menu_export_data),
                    isExpanded = exportExpanded,
                    onClick = { exportExpanded = !exportExpanded }
                ) {
                    var format by rememberSaveable { mutableStateOf("JSON") }
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedButton(
                                onClick = { format = "JSON" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = if (format == "JSON") MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            ) { Text(stringResource(R.string.format_json)) }
                            OutlinedButton(
                                onClick = { format = "CSV" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = if (format == "CSV") MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            ) { Text(stringResource(R.string.format_csv)) }
                        }
                        Button(onClick = { exportExpanded = false }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_download)) }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
            item {
                TextButton(onClick = onLogoutClick, modifier = Modifier.fillMaxWidth()) {
                    Icon(painterResource(R.drawable.ic_logout), contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.action_logout), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun ExpandableProfileMenu(iconResId: Int, title: String, isExpanded: Boolean, onClick: () -> Unit, expandedContent: @Composable () -> Unit) {
    AppCard(onClick = onClick) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(iconResId), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(title, style = MaterialTheme.typography.titleMedium)
                }
            }
            if (isExpanded) {
                HorizontalDivider(color = CardBorder)
                Box(modifier = Modifier.padding(16.dp)) { expandedContent() }
            }
        }
    }
}

@Composable
fun ProfileSimpleMenuItem(iconResId: Int, title: String, trailingText: String? = null, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(iconResId), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(16.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            if (trailingText != null) Text(trailingText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview
@Composable
private fun ProfileScreenPreview() {
    BookWormsTheme {
        ProfileScreen()
    }
}