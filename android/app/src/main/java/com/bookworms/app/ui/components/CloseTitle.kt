package com.bookworms.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bookworms.app.R

// X button with the screen title next to it. Like BackTitle, but for screens
// that open on top of another one for a single task (scanning, inviting).
@Composable
fun CloseTitle(
    title: String,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
    // Small gray text under the title, e.g. the group name.
    subtitle: String? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        IconButton(
            onClick = onCloseClick,
            // Same as in BackTitle: lines the X up with the rest of the screen.
            modifier = Modifier.offset(x = (-12).dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.close),
            )
        }
        Column(modifier = Modifier.offset(x = (-8).dp)) {
            Text(
                text = title,
                // Same size as BackTitle and the app's top bars.
                fontSize = ScreenTitleSize,
                fontWeight = FontWeight.Bold,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
