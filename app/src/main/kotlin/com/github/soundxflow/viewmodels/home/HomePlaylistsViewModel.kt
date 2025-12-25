package com.github.soundxflow.viewmodels.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.github.soundxflow.Database
import com.github.soundxflow.enums.PlaylistSortBy
import com.github.soundxflow.enums.SortOrder
import com.github.soundxflow.models.PlaylistPreview

class HomePlaylistsViewModel : ViewModel() {
    var items: List<PlaylistPreview> by mutableStateOf(emptyList())

    suspend fun loadArtists(
        sortBy: PlaylistSortBy,
        sortOrder: SortOrder
    ) {
        Database
            .playlistPreviews(sortBy, sortOrder)
            .collect { items = it }
    }
}