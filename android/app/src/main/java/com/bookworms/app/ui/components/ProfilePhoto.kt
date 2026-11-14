package com.bookworms.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import com.bookworms.app.ui.theme.PlaceholderGray

// The user's photo in a circle, or the gray circle when there's no photo (or while it loads).
// photo = the full address of a photo on the server, or a Uri of one just picked on the phone, or null.
@Composable
fun ProfilePhoto(photo: Any?, size: Dp, modifier: Modifier = Modifier) {
    AsyncImage(
        model = photo,
        contentDescription = null,
        // Fills the circle and cuts off what sticks out, like a profile photo should.
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(PlaceholderGray, CircleShape),
    )
}
