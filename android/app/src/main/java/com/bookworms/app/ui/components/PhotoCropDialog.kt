package com.bookworms.app.ui.components

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bookworms.app.R
import com.bookworms.app.data.saveCroppedPhoto
import com.bookworms.app.data.uprightPhoto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt

// Loaded this big, so zooming in still looks sharp. The server keeps 512 px anyway.
private const val LoadSize = 1024

// How far in the user can zoom, compared with the photo just filling the circle.
private const val MaxZoom = 5f

// The cropped square is made at most this big before it's saved.
private const val CroppedSize = 1024

// Space on each side of the circle.
private val CircleMargin = 24.dp

// Moving and zooming a just-picked photo in a circle: two fingers zoom, one moves it.
// The photo always fills the circle, so the result has no empty corners.
// Done gives a square JPEG of what's in the circle (its address in the app's cache), Cancel gives nothing.
@Composable
fun PhotoCropDialog(photo: Uri, onDone: (Uri) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // null while it loads. failed = it couldn't be read.
    var bitmap by remember(photo) { mutableStateOf<Bitmap?>(null) }
    var failed by remember(photo) { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(photo) {
        try {
            bitmap = withContext(Dispatchers.IO) { uprightPhoto(context.contentResolver, photo, LoadSize) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("BookWorms", "Opening the photo to crop failed", e)
            failed = true
        }
    }

    // How the photo lies under the circle. scale = screen px per photo px. offset = how far its middle is
    // from the circle's middle, in screen px.
    var area by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember(photo) { mutableFloatStateOf(0f) }
    var offset by remember(photo) { mutableStateOf(Offset.Zero) }

    val margin = with(LocalDensity.current) { CircleMargin.toPx() }
    val diameter = (min(area.width, area.height) - 2 * margin).coerceAtLeast(1f)
    val minScale = bitmap?.let { diameter / min(it.width, it.height) } ?: 1f

    // The photo can't be moved so far that an edge comes inside the circle.
    fun limited(wanted: Offset, scale: Float): Offset {
        val image = bitmap ?: return Offset.Zero
        val maxX = (image.width * scale - diameter) / 2
        val maxY = (image.height * scale - diameter) / 2
        return Offset(wanted.x.coerceIn(-maxX, maxX), wanted.y.coerceIn(-maxY, maxY))
    }

    // At the start: the whole circle filled, the middle of the photo in the middle.
    if (bitmap != null && area != IntSize.Zero && scale == 0f) scale = minScale

    Dialog(
        onDismissRequest = onCancel,
        // The whole screen, not a small window in the middle.
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .safeDrawingPadding(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel), color = Color.White) }
                Text(
                    stringResource(R.string.crop_photo_title),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = {
                        val image = bitmap ?: return@TextButton
                        saving = true
                        scope.launch {
                            val cropped = try {
                                withContext(Dispatchers.IO) { saveCroppedPhoto(context, crop(image, scale, offset, diameter)) }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                // E.g. no space left on the phone. The user can try again or cancel.
                                Log.w("BookWorms", "Cropping the photo failed", e)
                                null
                            }
                            saving = false
                            if (cropped != null) onDone(cropped)
                        }
                    },
                    enabled = bitmap != null && !saving,
                ) {
                    Text(stringResource(R.string.done_action), color = if (bitmap != null) Color.White else Color.Gray)
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    // The photo is bigger than this space - without it, it would cover the buttons above.
                    .clipToBounds()
                    .onSizeChanged { area = it },
            ) {
                val image = bitmap
                when {
                    failed -> Text(
                        stringResource(R.string.crop_photo_failed),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp),
                    )
                    image == null || scale == 0f -> CircularProgressIndicator(color = Color.White)
                    else -> {
                        val picture = remember(image) { image.asImageBitmap() }
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(image, minScale) {
                                    detectTransformGestures { centroid, pan, zoom, _ ->
                                        val newScale = (scale * zoom).coerceIn(minScale, minScale * MaxZoom)
                                        // Zooms around the point between the fingers, not around the middle.
                                        val fromMiddle = centroid - Offset(size.width / 2f, size.height / 2f)
                                        val moved = (offset - fromMiddle) * (newScale / scale) + fromMiddle + pan
                                        scale = newScale
                                        offset = limited(moved, newScale)
                                    }
                                },
                        ) {
                            val middle = Offset(size.width / 2, size.height / 2)
                            val shown = Size(image.width * scale, image.height * scale)
                            val topLeft = middle + offset - Offset(shown.width / 2, shown.height / 2)
                            drawImage(
                                picture,
                                dstOffset = IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()),
                                dstSize = IntSize(shown.width.roundToInt(), shown.height.roundToInt()),
                            )
                            // Dark outside the circle, so it's clear what stays.
                            val circle = Rect(middle, diameter / 2)
                            val outside = Path().apply {
                                fillType = PathFillType.EvenOdd
                                addRect(Rect(Offset.Zero, size))
                                addOval(circle)
                            }
                            drawPath(outside, Color.Black.copy(alpha = 0.6f))
                            drawCircle(Color.White, radius = diameter / 2, center = middle, style = Stroke(width = 2.dp.toPx()))
                        }
                    }
                }
            }

            Text(
                stringResource(R.string.crop_photo_hint),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
    }
}

// The part of the photo inside the circle, as a square. The square's middle is the circle's middle,
// which on the photo is -offset away from the photo's own middle (in screen px, so divided by scale).
private fun crop(image: Bitmap, scale: Float, offset: Offset, diameter: Float): Bitmap {
    val side = (diameter / scale).roundToInt().coerceIn(1, min(image.width, image.height))
    val centerX = image.width / 2f - offset.x / scale
    val centerY = image.height / 2f - offset.y / scale
    val left = (centerX - side / 2f).roundToInt().coerceIn(0, image.width - side)
    val top = (centerY - side / 2f).roundToInt().coerceIn(0, image.height - side)
    val square = Bitmap.createBitmap(image, left, top, side, side)
    return if (side <= CroppedSize) square else Bitmap.createScaledBitmap(square, CroppedSize, CroppedSize, true)
}
