package com.github.niusic.viewmodels.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.github.niusic.Database
import com.github.niusic.enums.AlbumSortBy
import com.github.niusic.enums.SortOrder
import com.github.niusic.models.Album

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
