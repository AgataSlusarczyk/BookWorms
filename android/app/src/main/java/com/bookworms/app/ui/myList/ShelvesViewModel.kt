package com.bookworms.app.ui.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookworms.app.data.LoadState
import com.bookworms.app.data.ShelfRepository
import com.bookworms.app.data.load
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// The user's shelves (Favorites, Want to read, Reading, Read) for My lists and "See all".
class ShelvesViewModel(
    private val repository: ShelfRepository = ShelfRepository(),
) : ViewModel() {

    private val _shelves = MutableStateFlow<LoadState<Map<Shelf, List<ShelfBook>>>>(LoadState.Loading)
    internal val shelves: StateFlow<LoadState<Map<Shelf, List<ShelfBook>>>> = _shelves.asStateFlow()

    // Called each time the screen shows up, e.g. after a heart was tapped on Book details.
    // The books already on the screen stay there while the new ones load, so it doesn't blink.
    fun refresh() {
        if (_shelves.value !is LoadState.Ready) _shelves.value = LoadState.Loading
        viewModelScope.launch {
            val fresh = load { repository.shelves() }
            // A failed refresh keeps the books already shown.
            if (fresh is LoadState.Ready || _shelves.value !is LoadState.Ready) _shelves.value = fresh
        }
    }
}
