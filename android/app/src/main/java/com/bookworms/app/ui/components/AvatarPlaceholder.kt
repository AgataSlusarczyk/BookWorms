package com.bookworms.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bookworms.app.ui.theme.PlaceholderGray

// Gray circle in place of a profile picture.
// TODO: show the real picture when users can add one.
@Composable
fun AvatarPlaceholder(modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        modifier = modifier
            .size(size)
            .background(PlaceholderGray, CircleShape),
    )
}
