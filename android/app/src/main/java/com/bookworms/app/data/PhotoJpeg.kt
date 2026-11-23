package com.bookworms.app.data

import android.content.Context
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

// A photo from the phone, made small and upright, as JPEG bytes to send to the server.
// A phone photo is far bigger than a profile circle, so sending it whole would be slow and waste data.
// Throws InvalidPhotoException when the photo can't be read.
internal fun photoJpeg(resolver: ContentResolver, uri: Uri): ByteArray {
    val upright = uprightPhoto(resolver, uri, MinSize)
    return ByteArrayOutputStream().use { out ->
        upright.compress(Bitmap.CompressFormat.JPEG, JpegQuality, out)
        out.toByteArray()
    }
}

// The photo, turned the right way up and loaded only as big as needed: at least minSize px on its shorter side
// (or the whole photo, when it's smaller). Throws InvalidPhotoException when it can't be read.
internal fun uprightPhoto(resolver: ContentResolver, uri: Uri, minSize: Int): Bitmap {
    // First only read the size of the photo...
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    // ...then pick how many times smaller to load it (1, 2, 4…), keeping it at least minSize px.
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= minSize && bounds.outHeight / (sample * 2) >= minSize) sample *= 2

    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        ?: throw InvalidPhotoException()

    // Photos taken with the phone held sideways carry a note to turn them – without it they'd lie on their side.
    // The server doesn't keep that note, so the photo is turned here.
    val orientation = resolver.openInputStream(uri)?.use {
        ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }
    val degrees = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    return if (degrees == 0f) bitmap else
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees) }, true)
}

// Keeps a cropped photo in the app's cache until Save sends it, and gives its address.
// The cache is the phone's to clear, so old crops don't pile up.
internal fun saveCroppedPhoto(context: Context, photo: Bitmap): Uri {
    val folder = File(context.cacheDir, "cropped").apply { mkdirs() }
    // Only the newest crop is needed – the older ones were replaced or dropped.
    folder.listFiles()?.forEach { it.delete() }
    val file = File(folder, "${UUID.randomUUID()}.jpg")
    file.outputStream().use { photo.compress(Bitmap.CompressFormat.JPEG, JpegQuality, it) }
    return Uri.fromFile(file)
}

class InvalidPhotoException : Exception("The photo can't be read or the server didn't accept it")

private const val MinSize = 512
private const val JpegQuality = 90
