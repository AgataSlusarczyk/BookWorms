package com.bookworms.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.bookworms.app.R

// The login picture covered with thick paper-colored mist, so only a hint of it shows through.
@Composable
fun Modifier.mistedBackground(): Modifier = this
    .paint(
        painter = painterResource(R.drawable.bg_login),
        contentScale = ContentScale.Crop,
        sizeToIntrinsics = false,
    )
    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.89f))
