package com.bookworms.app.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.ui.auth.FieldShape
import com.bookworms.app.ui.components.AppTopBar
import com.bookworms.app.ui.components.mistedBackground

@Composable
fun EditProfileScreen(onBackClick: () -> Unit) {
    var name by remember { mutableStateOf("Anna Kowalska") }
    
    Scaffold(
        modifier = Modifier.mistedBackground(),
        containerColor = Color.Transparent,
        topBar = { AppTopBar(title = stringResource(R.string.edit_profile_title), onBackClick = onBackClick) }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp), 
            horizontalAlignment = Alignment.CenterHorizontally, 
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(100.dp)) {}
            
            TextButton(onClick = { /* TODO: Wybierz nowe zdjęcie */ }) { 
                Text(stringResource(R.string.change_photo)) 
            }
            
            OutlinedTextField(
                value = name, 
                onValueChange = { name = it }, 
                label = { Text(stringResource(R.string.name_label)) }, 
                shape = FieldShape,
                modifier = Modifier.fillMaxWidth(), 
                singleLine = true
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = onBackClick, 
                shape = FieldShape, 
                modifier = Modifier.fillMaxWidth()
            ) { 
                Text(stringResource(R.string.action_save)) 
            }
        }
    }
}