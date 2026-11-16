package com.bookworms.app.data

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow

// What a screen shows while it waits for data from the server.
sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>

    // No internet, the server is off, or it answered with an error. The screen offers "Try again".
    data object Failed : LoadState<Nothing>

    // offline = the server couldn't be reached, so this is the copy saved on the phone (SavedData).
    // checking = the copy saved on the phone, shown while the server is still being asked.
    // Either way it's only to look at: the screen grays out everything that would change it.
    data class Ready<T>(val data: T, val offline: Boolean = false, val checking: Boolean = false) : LoadState<T>
}

// true when the server couldn't be reached and the screen shows the copy saved on the phone.
val LoadState<*>.isOffline: Boolean get() = this is LoadState.Ready && offline

// true while the screen shows the copy saved on the phone – the server is being asked or can't be reached.
val LoadState<*>.isSavedCopy: Boolean get() = this is LoadState.Ready && (offline || checking)

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

// Shows the copy saved on the phone right away (checking), so the user doesn't wait for the server,
// then the fresh data from the server – or the saved copy marked offline when it can't be reached.
// Fresh data already on the screen stays there while the server is asked again.
suspend fun <T> MutableStateFlow<LoadState<T>>.refreshWithSaved(saved: suspend () -> T?, block: suspend () -> T) {
    val shown = value
    if (shown !is LoadState.Ready || shown.offline) {
        val copy = saved()
        value = if (copy != null) LoadState.Ready(copy, checking = true) else LoadState.Loading
    }
    value = loadOrSaved(saved, block)
}

// Like load, but when the server can't be reached it falls back on the copy saved on the phone.
// saved() gives null when there's none – then it's Failed, as with load.
suspend fun <T> loadOrSaved(saved: suspend () -> T?, block: suspend () -> T): LoadState<T> {
    val fresh = load(block)
    if (fresh !is LoadState.Failed) return fresh
    val copy = saved() ?: return fresh
    return LoadState.Ready(copy, offline = true)
}
