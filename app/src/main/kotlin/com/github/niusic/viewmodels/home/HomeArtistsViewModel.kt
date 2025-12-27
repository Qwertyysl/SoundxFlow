package com.github.niusic.viewmodels.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.github.niusic.Database
import com.github.niusic.enums.ArtistSortBy
import com.github.niusic.enums.SortOrder
import com.github.niusic.models.Artist

class HomeArtistsViewModel : ViewModel() {
    var items: List<Artist> by mutableStateOf(emptyList())

    suspend fun loadArtists(
        sortBy: ArtistSortBy,
        sortOrder: SortOrder
    ) {
        Database
            .artists(sortBy, sortOrder)
            .collect { items = it }
    }
}
