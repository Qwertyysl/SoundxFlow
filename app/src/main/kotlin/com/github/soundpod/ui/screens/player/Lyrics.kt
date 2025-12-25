package com.github.soundpod.ui.screens.player

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import com.valentinilk.shimmer.shimmer
import com.github.innertube.Innertube
import com.github.innertube.requests.lyrics
import com.github.kugou.KuGou
import com.github.soundpod.Database
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.models.LocalMenuState
import com.github.soundpod.models.Lyrics
import com.github.soundpod.query
import com.github.soundpod.ui.components.Menu
import com.github.soundpod.ui.components.MenuEntry
import com.github.soundpod.ui.components.TextFieldDialog
import com.github.soundpod.ui.components.TextPlaceholder
import com.github.soundpod.ui.styling.Dimensions
import com.github.soundpod.ui.styling.onOverlay
import com.github.soundpod.utils.BetterLyrics
import com.github.soundpod.utils.SynchronizedLyrics
import com.github.soundpod.utils.isLandscape
import com.github.soundpod.utils.isShowingSynchronizedLyricsKey
import com.github.soundpod.utils.rememberPreference
import com.github.soundpod.utils.toast
import com.github.soundpod.utils.verticalFadingEdge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

@Composable
fun Lyrics(
    mediaId: String,
    isDisplayed: Boolean,
    onDismiss: () -> Unit,
    size: Dp,
    mediaMetadataProvider: () -> MediaMetadata,
    durationProvider: () -> Long,
    ensureSongInserted: () -> Unit,
    fullScreenLyrics: Boolean,
    toggleFullScreenLyrics: () -> Unit
) {
    AnimatedVisibility(
        visible = isDisplayed,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val context = LocalContext.current
        val menuState = LocalMenuState.current
        val currentView = LocalView.current

        var isShowingSynchronizedLyrics by rememberPreference(isShowingSynchronizedLyricsKey, true)

        var isEditing by remember(mediaId, isShowingSynchronizedLyrics) {
            mutableStateOf(false)
        }

        var lyricsModel by remember {
            mutableStateOf<com.github.soundpod.models.Lyrics?>(null)
        }

        val text = if (isShowingSynchronizedLyrics) lyricsModel?.synced else lyricsModel?.fixed

        var isError by remember(mediaId, isShowingSynchronizedLyrics) {
            mutableStateOf(false)
        }

        val isFloatingLyricsEnabled by rememberPreference(com.github.soundpod.utils.isFloatingLyricsEnabledKey, false)

        LaunchedEffect(mediaId, isShowingSynchronizedLyrics, isFloatingLyricsEnabled) {
            withContext(Dispatchers.IO) {
                Database.lyrics(mediaId).collect { dbLyrics ->
                    lyricsModel = dbLyrics
                    if ((isShowingSynchronizedLyrics || isFloatingLyricsEnabled) && dbLyrics?.synced == null) {
                        // --- IMPROVED LYRICS FETCHING CHAIN ---
                        val mediaMetadata = mediaMetadataProvider()
                        val artist = mediaMetadata.artist?.toString() ?: ""
                        val title = mediaMetadata.title?.toString() ?: ""
                        val album = mediaMetadata.albumTitle?.toString()
                        var duration = withContext(Dispatchers.Main) { durationProvider() }

                        while (duration == C.TIME_UNSET) {
                            delay(100)
                            duration = withContext(Dispatchers.Main) { durationProvider() }
                        }
                        val songDurationSec = duration / 1000

                        var fetchedSynced: String? = null
                        var fetchedFixed: String? = null

                        // 1. BetterLyrics (Video ID match)
                        BetterLyrics.fetchLyrics(mediaId).onSuccess {
                            if (!it.isNullOrBlank()) fetchedSynced = it
                        }

                        // 2. LRCLIB Get (Metadata match)
                        if (fetchedSynced == null) {
                            com.github.soundpod.utils.LrcLib.fetchLyrics(artist, title, album, songDurationSec).onSuccess { response ->
                                val lrcDuration = response?.duration?.toLong() ?: 0L
                                if (response?.syncedLyrics != null && (lrcDuration == 0L || Math.abs(lrcDuration - songDurationSec) <= 3)) {
                                    fetchedSynced = response.syncedLyrics
                                    fetchedFixed = response.plainLyrics
                                }
                            }
                        }

                        // 3. NetEase (Search match)
                        if (fetchedSynced == null) {
                            com.github.soundpod.utils.NetEase.fetchLyrics(artist, title, duration).onSuccess {
                                if (!it.isNullOrBlank()) fetchedSynced = it
                            }
                        }

                        // 4. LRCLIB Search (Keyword match)
                        if (fetchedSynced == null) {
                            com.github.soundpod.utils.LrcLib.searchLyrics("$artist $title").onSuccess { results ->
                                val best = results.find { Math.abs((it.duration ?: 0.0).toLong() - songDurationSec) <= 3 }
                                if (best?.syncedLyrics != null) {
                                    fetchedSynced = best.syncedLyrics
                                    fetchedFixed = best.plainLyrics
                                }
                            }
                        }

                        // 5. KuGou (Legacy fallback)
                        if (fetchedSynced == null) {
                            KuGou.lyrics(artist, title, songDurationSec)?.onSuccess {
                                if (!it?.value.isNullOrBlank()) fetchedSynced = it?.value
                            }
                        }

                        if (fetchedSynced != null) {
                            Database.upsert(
                                com.github.soundpod.models.Lyrics(
                                    songId = mediaId,
                                    fixed = fetchedFixed ?: dbLyrics?.fixed,
                                    synced = fetchedSynced
                                )
                            )
                        } else {
                            isError = true
                        }
                    } else if (!isShowingSynchronizedLyrics && !isFloatingLyricsEnabled && dbLyrics?.fixed == null) {
                        Innertube.lyrics(videoId = mediaId)?.onSuccess { fixedLyrics ->
                            Database.upsert(
                                com.github.soundpod.models.Lyrics(
                                    songId = mediaId,
                                    fixed = fixedLyrics ?: "",
                                    synced = dbLyrics?.synced
                                )
                            )
                        }?.onFailure {
                            isError = true
                        }
                    }
                }
            }
        }

        if (isEditing) {
            TextFieldDialog(
                title = stringResource(id = R.string.edit_lyrics),
                hintText = stringResource(id = R.string.enter_lyrics),
                initialTextInput = text ?: "",
                singleLine = false,
                maxLines = 10,
                isTextInputValid = { true },
                onDismiss = { isEditing = false },
                onDone = {
                    query {
                        ensureSongInserted()
                        Database.upsert(
                            com.github.soundpod.models.Lyrics(
                                songId = mediaId,
                                fixed = if (isShowingSynchronizedLyrics) lyricsModel?.fixed else it,
                                synced = if (isShowingSynchronizedLyrics) it else lyricsModel?.synced,
                            )
                        )
                    }
                }
            )
        }

        if (isShowingSynchronizedLyrics) {
            DisposableEffect(Unit) {
                currentView.keepScreenOn = true
                onDispose {
                    currentView.keepScreenOn = false
                }
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clickable(onClick = onDismiss)
                .fillMaxSize()
                .background(Color.Black.copy(0.45f))
        ) {
            AnimatedVisibility(
                visible = isError && text == null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text(
                    text = if (isShowingSynchronizedLyrics) {
                        stringResource(id = R.string.error_fetching_synchronized_lyrics)
                    } else {
                        stringResource(id = R.string.error_fetching_lyrics)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(all = 16.dp)
                        .fillMaxWidth()
                )
            }

            AnimatedVisibility(
                visible = text?.let(String::isEmpty) == true,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text(
                    text = if (isShowingSynchronizedLyrics) {
                        stringResource(id = R.string.synchronized_lyrics_not_available)
                    } else {
                        stringResource(id = R.string.lyrics_not_available)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(all = 16.dp)
                        .fillMaxWidth()
                )
            }

            if (text?.isNotEmpty() == true) {
                if (isShowingSynchronizedLyrics) {
                    val density = LocalDensity.current
                    val player = LocalPlayerServiceBinder.current?.player
                        ?: return@AnimatedVisibility

                    val synchronizedLyrics = remember(text) {
                        val sentences = com.github.soundpod.utils.LrcParser.parse(text)
                        SynchronizedLyrics(sentences) {
                            player.currentPosition
                        }
                    }

                    val lazyListState = rememberLazyListState(
                        if (synchronizedLyrics.index == -1) 0 else synchronizedLyrics.index,
                        with(density) { size.roundToPx() } / 6)

                    LaunchedEffect(synchronizedLyrics) {
                        val center = with(density) { size.roundToPx() } / 6

                        while (isActive) {
                            delay(50)
                            if (synchronizedLyrics.update()) {
                                if (synchronizedLyrics.index != -1) {
                                    lazyListState.animateScrollToItem(
                                        synchronizedLyrics.index,
                                        center
                                    )
                                } else {
                                    lazyListState.animateScrollToItem(0, 0)
                                }
                            }
                        }
                    }

                    LazyColumn(
                        state = lazyListState,
                        userScrollEnabled = true, // Changed to true for Apple Music feel
                        contentPadding = PaddingValues(vertical = size / 2),
                        horizontalAlignment = Alignment.Start, // Changed to start
                        modifier = Modifier.verticalFadingEdge()
                    ) {
                        itemsIndexed(items = synchronizedLyrics.sentences) { index, sentence ->
                            Text(
                                text = sentence.second,
                                style = if (index == synchronizedLyrics.index) 
                                    MaterialTheme.typography.headlineMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                    else MaterialTheme.typography.headlineSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
                                color = Color.White,
                                textAlign = TextAlign.Start,
                                modifier = Modifier
                                    .padding(vertical = 12.dp, horizontal = 24.dp)
                                    .alpha(if (index == synchronizedLyrics.index) 1F else 0.4f)
                                    .clickable {
                                        player.seekTo(sentence.first)
                                    }
                            )
                        }
                    }
                } else {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .verticalFadingEdge()
                            .verticalScroll(rememberScrollState())
                            .fillMaxWidth()
                            .padding(vertical = size / 4, horizontal = 32.dp)
                    )
                }
            }

            if (text == null && !isError) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.shimmer()
                ) {
                    repeat(4) {
                        TextPlaceholder(
                            modifier = Modifier.alpha(1f - it * 0.2f)
                        )
                    }
                }
            }

            if (!isLandscape) {
                IconButton(
                    onClick = toggleFullScreenLyrics,
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Icon(
                        imageVector = if (fullScreenLyrics) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onOverlay
                    )
                }
            }

            IconButton(
                onClick = {
                    menuState.display {
                        Menu {
                            MenuEntry(
                                icon = Icons.Outlined.Schedule,
                                text = if (isShowingSynchronizedLyrics) {
                                    stringResource(id = R.string.show_unsynchronized_lyrics)
                                } else {
                                    stringResource(id = R.string.show_synchronized_lyrics)
                                },
                                secondaryText = if (isShowingSynchronizedLyrics) null else {
                                    stringResource(id = R.string.provided_by_kugou)
                                },
                                onClick = {
                                    menuState.hide()
                                    isShowingSynchronizedLyrics =
                                        !isShowingSynchronizedLyrics
                                }
                            )

                            MenuEntry(
                                icon = Icons.Outlined.Edit,
                                text = stringResource(id = R.string.edit_lyrics),
                                onClick = {
                                    menuState.hide()
                                    isEditing = true
                                }
                            )

                            MenuEntry(
                                icon = Icons.Outlined.Search,
                                text = stringResource(id = R.string.search_lyrics_online),
                                onClick = {
                                    menuState.hide()
                                    val mediaMetadata = mediaMetadataProvider()

                                    try {
                                        context.startActivity(
                                            Intent(Intent.ACTION_WEB_SEARCH).apply {
                                                putExtra(
                                                    SearchManager.QUERY,
                                                    "${mediaMetadata.title} ${mediaMetadata.artist} lyrics"
                                                )
                                            }
                                        )
                                    } catch (_: ActivityNotFoundException) {
                                        context.toast("Couldn't find an application to browse the Internet")
                                    }
                                }
                            )

                            MenuEntry(
                                icon = Icons.Outlined.Download,
                                text = stringResource(id = R.string.fetch_lyrics_again),
                                enabled = lyricsModel != null,
                                onClick = {
                                    menuState.hide()
                                    query {
                                        Database.upsert(
                                            com.github.soundpod.models.Lyrics(
                                                songId = mediaId,
                                                fixed = if (isShowingSynchronizedLyrics) lyricsModel?.fixed else null,
                                                synced = if (isShowingSynchronizedLyrics) null else lyricsModel?.synced,
                                            )
                                        )
                                    }
                                }
                            )
                        }
                    }
                },
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Icon(
                    imageVector = Icons.Outlined.MoreHoriz,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onOverlay
                )
            }
        }
    }
}