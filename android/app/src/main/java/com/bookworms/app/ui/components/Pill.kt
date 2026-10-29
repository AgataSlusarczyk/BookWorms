package com.bookworms.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bookworms.app.ui.theme.PlaceholderGray

// Small rounded label, e.g. "available in 2 groups" or "science fiction".
@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = PlaceholderGray,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    // A small dot before the text, in the text color. Used for statuses like "Available".
    showDot: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(containerColor, CircleShape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        if (showDot) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(contentColor, CircleShape),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
        )
    }
}
