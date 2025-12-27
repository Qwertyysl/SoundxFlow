package com.github.niusic.viewmodels.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.github.niusic.Database
import com.github.niusic.enums.SongSortBy
import com.github.niusic.enums.SortOrder
import com.github.niusic.models.Song

class HomeSongsViewModel : ViewModel() {
    var items: List<Song> by mutableStateOf(emptyList())

    suspend fun loadSongs(
        sortBy: SongSortBy,
        sortOrder: SortOrder
    ) {
        Database
            .songs(sortBy, sortOrder)
            .collect { items = it }
    }
}
