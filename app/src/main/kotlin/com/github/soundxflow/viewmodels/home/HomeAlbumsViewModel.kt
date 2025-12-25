package com.github.soundxflow.viewmodels.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.github.soundxflow.Database
import com.github.soundxflow.enums.AlbumSortBy
import com.github.soundxflow.enums.SortOrder
import com.github.soundxflow.models.Album

class HomeAlbumsViewModel : ViewModel() {
    var items: List<Album> by mutableStateOf(emptyList())

    suspend fun loadAlbums(
        sortBy: AlbumSortBy,
        sortOrder: SortOrder
    ) {
        Database
            .albums(sortBy, sortOrder)
            .collect { items = it }
    }
}