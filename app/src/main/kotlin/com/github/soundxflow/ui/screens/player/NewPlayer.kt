package com.github.soundxflow.ui.screens.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImage
import com.github.innertube.Innertube
import com.github.innertube.requests.lyrics
import com.github.soundxflow.Database
import com.github.soundxflow.LocalPlayerServiceBinder
import com.github.soundxflow.service.PlayerService
import com.github.soundxflow.ui.styling.Dimensions
import com.github.soundxflow.utils.DisposableListener
import com.github.soundxflow.utils.isLandscape
import com.github.soundxflow.utils.positionAndDurationState
import com.github.soundxflow.utils.rememberPreference
import com.github.soundxflow.utils.shouldBePlaying
import com.github.soundxflow.utils.thumbnail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.github.soundxflow.models.Lyrics
import com.github.soundxflow.models.Song

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
)
@Composable
fun NewPlayer(
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit,
    onMinimize: () -> Unit
) {
    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player ?: return
    var nullableMediaItem by remember {
        mutableStateOf<MediaItem?>(
            player.currentMediaItem,
            neverEqualPolicy()
        )
    }

    val mediaItem = nullableMediaItem ?: return

    var artistId: String? by remember(mediaItem) {
        mutableStateOf(
            mediaItem.mediaMetadata.extras?.getStringArrayList("artistIds")?.let { artists ->
                if (artists.size == 1) artists.first()
                else null
            }
        )
    }

    var shouldBePlaying by remember { mutableStateOf(player.shouldBePlaying) }

    player.DisposableListener {
        object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                nullableMediaItem = mediaItem
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                shouldBePlaying = player.shouldBePlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                shouldBePlaying = player.shouldBePlaying
            }
        }
    }

    // --- LYRICS FETCHING LOGIC ---
    val isFloatingLyricsEnabled by rememberPreference(com.github.soundxflow.utils.isFloatingLyricsEnabledKey, false)

    LaunchedEffect(mediaItem.mediaId, isFloatingLyricsEnabled) {
        withContext(Dispatchers.IO) {
            Database.lyrics(mediaItem.mediaId).collect { dbLyrics: Lyrics? ->
                if (dbLyrics?.synced == null) {
                    val artist = mediaItem.mediaMetadata.artist?.toString() ?: ""
                    val title = mediaItem.mediaMetadata.title?.toString() ?: ""
                    val album = mediaItem.mediaMetadata.albumTitle?.toString()
                    var duration = withContext(Dispatchers.Main) { player.duration }

                    while (duration == C.TIME_UNSET) {
                        delay(100)
                        duration = withContext(Dispatchers.Main) { player.duration }
                    }
                    val songDurationSec = duration / 1000

                    var fetchedSynced: String? = null
                    var fetchedFixed: String? = null

                    // 1. BetterLyrics
                    com.github.soundxflow.utils.BetterLyrics.fetchLyrics(mediaItem.mediaId).onSuccess {
                        if (!it.isNullOrBlank()) fetchedSynced = it
                    }

                    // 2. LRCLIB Get
                    if (fetchedSynced == null) {
                        com.github.soundxflow.utils.LrcLib.fetchLyrics(artist, title, album, songDurationSec).onSuccess { response ->
                            val lrcDuration = response?.duration?.toLong() ?: 0L
                            if (response?.syncedLyrics != null && (lrcDuration == 0L || (lrcDuration - songDurationSec).let { if (it < 0) -it else it } <= 3L)) {
                                fetchedSynced = response.syncedLyrics
                                fetchedFixed = response.plainLyrics
                            }
                        }
                    }

                    // 3. NetEase
                    if (fetchedSynced == null) {
                        com.github.soundxflow.utils.NetEase.fetchLyrics(artist, title, duration).onSuccess {
                            if (!it.isNullOrBlank()) fetchedSynced = it
                        }
                    }

                    // 4. LRCLIB Search
                    if (fetchedSynced == null) {
                        com.github.soundxflow.utils.LrcLib.searchLyrics("$artist $title").onSuccess { results ->
                            val best = results.find { (it.duration?.toLong()?.minus(songDurationSec))?.let { if (it < 0) -it else it }?.let { it <= 3L } ?: false }
                            if (best?.syncedLyrics != null) {
                                fetchedSynced = best.syncedLyrics
                                fetchedFixed = best.plainLyrics
                            }
                        }
                    }

                    // 5. KuGou
                    if (fetchedSynced == null) {
                        com.github.kugou.KuGou.lyrics(artist, title, songDurationSec)?.onSuccess {
                            if (!it?.value.isNullOrBlank()) fetchedSynced = it?.value
                        }
                    }

                    if (fetchedSynced != null) {
                        Database.upsert(
                            Lyrics(
                                songId = mediaItem.mediaId,
                                fixed = fetchedFixed ?: dbLyrics?.fixed,
                                synced = fetchedSynced
                            )
                        )
                    }
                }

                if (dbLyrics?.fixed == null) {
                    com.github.innertube.Innertube.lyrics(videoId = mediaItem.mediaId)?.onSuccess { fixedLyrics: String? ->
                        Database.upsert(
                            Lyrics(
                                songId = mediaItem.mediaId,
                                fixed = fixedLyrics ?: "",
                                synced = dbLyrics?.synced
                            )
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(mediaItem) {
        withContext(Dispatchers.IO) {
            if (artistId == null) {
                val artistsInfo = Database.songArtistInfo(mediaItem.mediaId)
                if (artistsInfo.size == 1) artistId = artistsInfo.first().id
            }
        }
    }

    var showPlaylist by remember { mutableStateOf(false) }
    var showLyrics by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = showPlaylist || showLyrics) {
        if (showPlaylist) showPlaylist = false
        else if (showLyrics) showLyrics = false
    }

    val positionAndDuration by player.positionAndDurationState()

    var fullScreenLyrics by remember { mutableStateOf(false) }
    var isShowingStatsForNerds by rememberSaveable { mutableStateOf(false) }

    if (isLandscape) {
        //todo
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    WindowInsets.systemBars
                        .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                        .asPaddingValues()
                )
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
        ) {

            Spacer(modifier = Modifier.height(Dimensions.spacer))

            PlayerTopControl(
                onGoToAlbum = onGoToAlbum,
                onGoToArtist = onGoToArtist,
                onBack = onMinimize,
            )

            Box(Modifier.weight(1f)) {
                androidx.compose.animation.AnimatedContent(
                    targetState = if (showPlaylist) 1 else if (showLyrics) 2 else 0,
                    transitionSpec = {
                        fadeIn(tween(400)) togetherWith fadeOut(tween(400))
                    },
                    label = "playerContent"
                ) { targetState ->
                    when (targetState) {
                        1 -> { // Playlist
                            Column {
                                PlaylistOverlay(
                                    modifier = Modifier.weight(1f),
                                    onGoToAlbum = onGoToAlbum,
                                    onGoToArtist = onGoToArtist
                                )
                                Spacer(modifier = Modifier.height(26.dp))
                            }
                        }
                        2 -> { // Full Lyrics View
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // Mini Thumbnail at the top
                                Box(
                                    modifier = Modifier
                                        .padding(top = 16.dp, bottom = 16.dp)
                                        .size(120.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { showLyrics = false }
                                ) {
                                    AsyncImage(
                                        model = mediaItem.mediaMetadata.artworkUri.thumbnail(240),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                
                                Box(modifier = Modifier.weight(1f)) {
                                    Lyrics(
                                        mediaId = mediaItem.mediaId,
                                        isDisplayed = true,
                                        onDismiss = { showLyrics = false },
                                        ensureSongInserted = { Database.insert(mediaItem) },
                                        size = 400.dp, // Dummy size, Lyrics uses it for padding
                                        mediaMetadataProvider = mediaItem::mediaMetadata,
                                        durationProvider = player::getDuration,
                                        fullScreenLyrics = true,
                                        toggleFullScreenLyrics = { /* already full screen */ }
                                    )
                                }
                            }
                        }
                        else -> { // Default Thumbnail View
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Spacer(modifier = Modifier.height(30.dp))

                                NewThumbnail(
                                    isShowingLyrics = false,
                                    onShowLyrics = { showLyrics = it },
                                    fullScreenLyrics = fullScreenLyrics,
                                    toggleFullScreenLyrics = { fullScreenLyrics = !fullScreenLyrics },
                                    isShowingStatsForNerds = isShowingStatsForNerds,
                                    onShowStatsForNerds = { isShowingStatsForNerds = it },
                                    modifier = Modifier
                                        .padding(horizontal = 24.dp)
                                        .fillMaxWidth(),
                                    mediaId = mediaItem.mediaId
                                )

                                Spacer(modifier = Modifier.padding(vertical = 5.dp))

                                PlayerMediaItem(
                                    onGoToArtist = artistId?.let {
                                        { onGoToArtist(it) }
                                    }
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                PlayerMiddleControl(
                                    showPlaylist = showPlaylist,
                                    onTogglePlaylist = { showPlaylist = it },
                                    mediaId = mediaItem.mediaId,
                                    onGoToAlbum = onGoToAlbum,
                                    onGoToArtist = onGoToArtist
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.spacer))

            PlayerSeekBar(
                mediaId = mediaItem.mediaId,
                position = positionAndDuration.first,
                duration = positionAndDuration.second
            )

            Spacer(modifier = Modifier.height(Dimensions.spacer))

            PlayerControlBottom(
                shouldBePlaying = shouldBePlaying,
                onPlayPauseClick = {
                    if (shouldBePlaying) {
                        player.pause()
                    } else {
                        if (player.playbackState == Player.STATE_IDLE) {
                            player.prepare()
                        } else if (player.playbackState == Player.STATE_ENDED) {
                            player.seekToDefaultPosition(0)
                        }
                        player.play()
                    }
                }
            )
        }

    }
}
