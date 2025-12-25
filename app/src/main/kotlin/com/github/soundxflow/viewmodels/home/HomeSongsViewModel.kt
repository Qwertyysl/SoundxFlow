package com.github.soundxflow.viewmodels.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.github.soundxflow.Database
import com.github.soundxflow.enums.SongSortBy
import com.github.soundxflow.enums.SortOrder
import com.github.soundxflow.models.Song

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