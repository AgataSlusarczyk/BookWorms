package com.bookworms.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import com.bookworms.app.ui.theme.PlaceholderGray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// TODO: the user's photo from the server.
// Kept outside the screens, so the photo saved on Edit profile shows up in Profile too.
// It lasts until the app is closed.
internal var profilePhotoUri by mutableStateOf<Uri?>(null)

// The user's photo in a circle, or the gray circle when there's no photo yet.
@Composable
fun ProfilePhoto(uri: Uri?, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // Loads the picture in the background, so the screen doesn't freeze on a big photo.
    val bitmap by produceState<ImageBitmap?>(initialValue = null, uri) {
        value = uri?.let { withContext(Dispatchers.IO) { loadSmallBitmap(context, it) } }
    }

    val picture = bitmap
    if (picture != null) {
        Image(
            bitmap = picture,
            contentDescription = null,
            // Fills the circle and cuts off what sticks out, like a profile photo should.
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape),
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .background(PlaceholderGray, CircleShape),
        )
    }
}

// A phone photo is far bigger than the circle, so we load a smaller copy – it's faster and uses less memory.
private fun loadSmallBitmap(context: Context, uri: Uri): ImageBitmap? = runCatching {
    val resolver = context.contentResolver

    // First only read the size of the photo...
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    // ...then pick how many times smaller to load it (1, 2, 4…), keeping it at least 512 px.
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= 512 && bounds.outHeight / (sample * 2) >= 512) sample *= 2

    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        ?: return@runCatching null

    // Photos taken with the phone held sideways carry a note to turn them – without it they'd lie on their side.
    val orientation = resolver.openInputStream(uri)?.use {
        ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }
    val degrees = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    val upright = if (degrees == 0f) bitmap else
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees) }, true)
    upright.asImageBitmap()
}.getOrNull()
