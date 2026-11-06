package com.bookworms.app.ui.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookworms.app.data.BookRepository
import com.bookworms.app.data.LoadState
import com.bookworms.app.data.load
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest

// Search in the public catalog on the server. Kept here, so turning the phone doesn't search again.
@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BrowseViewModel(private val repository: BookRepository = BookRepository()) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    // Goes up on "Try again", so the same query is sent once more.
    private val retries = MutableStateFlow(0)

    val results: StateFlow<LoadState<List<Book>>> =
        // Waits until the user stops typing for a moment, so we don't ask the server on every letter.
        combine(_query.debounce { if (it.isBlank()) 0L else SearchDelayMillis }, retries) { query, _ -> query }
            // A new query cancels the request still running for the old one.
            .transformLatest { query ->
                emit(LoadState.Loading)
                emit(load { repository.search(query) })
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoadState.Loading)

    fun onQueryChange(query: String) {
        _query.value = query
    }

    fun retry() {
        retries.value++
    }

    private companion object {
        const val SearchDelayMillis = 300L
    }
}
