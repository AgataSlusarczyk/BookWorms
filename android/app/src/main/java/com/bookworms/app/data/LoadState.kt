package com.bookworms.app.data

import android.util.Log
import kotlinx.coroutines.CancellationException

// What a screen shows while it waits for data from the server.
sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>

    // No internet, the server is off, or it answered with an error. The screen offers "Try again".
    data object Failed : LoadState<Nothing>

    data class Ready<T>(val data: T) : LoadState<T>
}

// Runs block and turns its result or its error into a LoadState.
suspend fun <T> load(block: suspend () -> T): LoadState<T> =
    try {
        LoadState.Ready(block())
    } catch (e: CancellationException) {
        // The screen was closed or a newer request replaced this one – not an error to show.
        throw e
    } catch (e: Exception) {
        // In Logcat (tag BookWorms), so we can see why it failed.
        Log.w("BookWorms", "Loading from the server failed", e)
        LoadState.Failed
    }
