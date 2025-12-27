package com.github.niusic.viewmodels.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.github.innertube.Innertube
import com.github.innertube.requests.relatedPage
import com.github.niusic.Database
import com.github.niusic.enums.QuickPicksSource
import com.github.niusic.models.Song
import kotlinx.coroutines.flow.distinctUntilChanged

import com.github.niusic.utils.lastPlayedPlaylistIdKey
import com.github.niusic.utils.preferences
import android.app.Application
import androidx.lifecycle.AndroidViewModel

class QuickPicksViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    var trending: Song? by mutableStateOf(null)
    var history: List<Song> by mutableStateOf(emptyList())
    var relatedPageResult: Result<Innertube.RelatedPage?>? by mutableStateOf(null)

    suspend fun loadHistory() {
        Database.history().distinctUntilChanged().collect {
            history = it
        }
    }

    suspend fun loadQuickPicks(quickPicksSource: QuickPicksSource) {
        val flow = when (quickPicksSource) {
            QuickPicksSource.Trending -> Database.trending()
            QuickPicksSource.LastPlayed -> Database.lastPlayed()
            QuickPicksSource.Random -> Database.randomSong()
        }

        flow.distinctUntilChanged().collect { song ->
            if (quickPicksSource == QuickPicksSource.Random && song != null && trending != null) return@collect

            if ((song == null && relatedPageResult == null) || trending?.id != song?.id || relatedPageResult?.isSuccess != true) {
                val lastPlaylistId = context.preferences.getString(lastPlayedPlaylistIdKey, null)
                relatedPageResult = Innertube.relatedPage(
                    videoId = (song?.id ?: "fJ9rUzIMcZQ"),
                    playlistId = lastPlaylistId
                )
            }

            trending = song
        }
    }
}
