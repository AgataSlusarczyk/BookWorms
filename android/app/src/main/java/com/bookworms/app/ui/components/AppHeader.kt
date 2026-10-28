package com.bookworms.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bookworms.app.R

// Logo and app name shown at the top of the screens.
@Composable
fun AppHeader(modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        // TODO: replace with the real logo.
        Icon(
            painter = painterResource(R.drawable.ic_logo_placeholder),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
