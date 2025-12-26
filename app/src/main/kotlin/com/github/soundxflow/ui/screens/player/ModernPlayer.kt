package com.github.soundxflow.ui.screens.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImage
import com.github.core.ui.LocalAppearance
import com.github.soundxflow.Database
import com.github.soundxflow.LocalPlayerServiceBinder
import com.github.soundxflow.service.PlayerService
import com.github.soundxflow.ui.styling.Dimensions
import com.github.soundxflow.utils.*
import android.media.audiofx.AudioEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.outlined.Timer
import com.github.soundxflow.query
import com.github.core.ui.favoritesIcon
import com.github.soundxflow.utils.shuffleQueue
import com.github.soundxflow.utils.seamlessPlay
import com.github.innertube.models.NavigationEndpoint
import com.github.soundxflow.models.Song
import android.content.ActivityNotFoundException
import android.content.Intent
import com.github.soundxflow.utils.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.github.soundxflow.ui.components.BaseMediaItemMenu
import kotlinx.coroutines.flow.flowOf
import com.github.soundxflow.models.Lyrics as LyricsModel
import com.github.soundxflow.models.LocalMenuState
import com.github.soundxflow.ui.components.TooltipIconButton
import com.github.soundxflow.R
import com.github.innertube.requests.lyrics
import androidx.compose.ui.res.painterResource
import com.github.soundxflow.utils.forceSeekToNext
import com.github.soundxflow.utils.forceSeekToPrevious

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
)
@Composable
fun ModernPlayer(
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit,
    onMinimize: () -> Unit
) {
    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player ?: return
    val menuState = LocalMenuState.current
    var nullableMediaItem by remember {
        mutableStateOf(player.currentMediaItem, neverEqualPolicy())
    }
    val mediaItem = nullableMediaItem ?: return
    val (colorPalette) = LocalAppearance.current

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

    // --- LYRICS FETCHING LOGIC (Same as NewPlayer) ---
    val isFloatingLyricsEnabled by rememberPreference(com.github.soundxflow.utils.isFloatingLyricsEnabledKey, false)

    LaunchedEffect(mediaItem.mediaId, isFloatingLyricsEnabled) {
        withContext(Dispatchers.IO) {
            Database.lyrics(mediaItem.mediaId).collect { dbLyrics: LyricsModel? ->
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
                            LyricsModel(
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
                            LyricsModel(
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
    
    val positionAndDuration by player.positionAndDurationState()
    val isAzanPlaying by rememberPreference(isAzanPlayingKey, false)
    
    var showPlaylist by remember { mutableStateOf(false) }
    var showLyrics by rememberSaveable { mutableStateOf(false) }

    val activityResultLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    val context = LocalContext.current
    val sleepTimerMillisLeft by (binder.sleepTimerMillisLeft ?: flowOf(null)).collectAsState(initial = null)
    var isShowingSleepTimerDialog by rememberSaveable { mutableStateOf(false) }
    val volumeBoosterEnabled by rememberPreference(volumeBoosterEnabledKey, false)

    var trackLoopEnabled by rememberPreference(trackLoopEnabledKey, defaultValue = false)
    var queueLoopEnabled by rememberPreference(queueLoopEnabledKey, defaultValue = false)
    var likedAt by rememberSaveable { mutableStateOf<Long?>(null) }
    var isShowingVolumeBoosterDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(mediaItem.mediaId) {
        Database.likedAt(mediaItem.mediaId).collect { likedAt = it }
    }

    BackHandler(enabled = showPlaylist || showLyrics) {
        if (showPlaylist) showPlaylist = false
        else if (showLyrics) showLyrics = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues())
            .padding(horizontal = 24.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = {
                if (showLyrics) showLyrics = false
                else if (showPlaylist) showPlaylist = false
                else onMinimize()
            }) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Minimize",
                    tint = colorPalette.text,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (sleepTimerMillisLeft != null) {
                    Text(
                        text = formatTime(sleepTimerMillisLeft ?: 0L),
                        color = colorPalette.text,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { isShowingSleepTimerDialog = true }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                } else {
                    IconButton(onClick = { isShowingSleepTimerDialog = true }) {
                        Icon(Icons.Outlined.Timer, tint = colorPalette.text, contentDescription = null, modifier = Modifier.size(22.dp))
                    }
                }

                IconButton(onClick = {
                    val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                        putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
                        putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                        putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                    }
                    try { activityResultLauncher.launch(intent) } catch (_: ActivityNotFoundException) { context.toast("Couldn't find an application to equalize audio") }
                }) {
                    Icon(painter = painterResource(id = R.drawable.equalizer), tint = colorPalette.text, contentDescription = null, modifier = Modifier.size(18.dp))
                }

                IconButton(onClick = { isShowingVolumeBoosterDialog = true }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.VolumeUp, tint = if (volumeBoosterEnabled) colorPalette.accent else colorPalette.text, contentDescription = null, modifier = Modifier.size(22.dp))
                }
            }

            IconButton(onClick = {
                menuState.display {
                    BaseMediaItemMenu(
                        onDismiss = menuState::hide,
                        mediaItem = mediaItem,
                        onStartRadio = {
                            binder.stopRadio()
                            binder.player.seamlessPlay(mediaItem)
                            binder.setupRadio(NavigationEndpoint.Endpoint.Watch(videoId = mediaItem.mediaId))
                        },
                        onGoToAlbum = onGoToAlbum,
                        onGoToArtist = onGoToArtist
                    )
                }
            }) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "More",
                    tint = colorPalette.text
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (isAzanPlaying) "AZAN" else "NOW PLAYING",
            style = MaterialTheme.typography.labelMedium,
            color = if (isAzanPlaying) colorPalette.accent else colorPalette.textSecondary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Artwork / Lyrics / Playlist
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AnimatedContent(
                targetState = if (showPlaylist) 1 else if (showLyrics) 2 else 0,
                label = "middleContent"
            ) { targetState ->
                when (targetState) {
                    1 -> { // Playlist
                        Column(modifier = Modifier.fillMaxSize()) {
                            PlaylistOverlay(
                                modifier = Modifier.weight(1f),
                                onGoToAlbum = onGoToAlbum,
                                onGoToArtist = onGoToArtist
                            )
                            IconButton(
                                onClick = { showPlaylist = false },
                                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp)
                            ) {
                                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Close Playlist", tint = colorPalette.text, modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                    2 -> { // Lyrics
                        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.weight(1f)) {
                                Lyrics(
                                    mediaId = mediaItem.mediaId,
                                    isDisplayed = true,
                                    onDismiss = { showLyrics = false },
                                    ensureSongInserted = { Database.insert(mediaItem) },
                                    size = 400.dp,
                                    mediaMetadataProvider = mediaItem::mediaMetadata,
                                    durationProvider = player::getDuration,
                                    fullScreenLyrics = true,
                                    toggleFullScreenLyrics = {}
                                )
                            }
                            IconButton(
                                onClick = { showLyrics = false },
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Close Lyrics", tint = colorPalette.text, modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                    else -> { // Artwork
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(24.dp))
                                    .clickable { showLyrics = true }
                            ) {
                                AsyncImage(
                                    model = mediaItem.mediaMetadata.artworkUri.thumbnail(1000),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                FloatingLyrics(
                                    mediaId = mediaItem.mediaId,
                                    player = player,
                                    modifier = Modifier.align(Alignment.BottomCenter)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            PlayerMediaItem(onGoToArtist = { 
                                mediaItem.mediaMetadata.extras?.getStringArrayList("artistIds")?.firstOrNull()?.let { 
                                    onGoToArtist(it) 
                                }
                            })
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Middle Controls (Playlist, Like, Add)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { showPlaylist = !showPlaylist }) {
                Icon(
                    painter = painterResource(id = R.drawable.playlist),
                    contentDescription = "Playlist",
                    tint = if (showPlaylist) colorPalette.accent else colorPalette.textSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            IconButton(onClick = {
                query {
                    if (Database.like(mediaItem.mediaId, if (likedAt == null) System.currentTimeMillis() else null) == 0) {
                        Database.insert(mediaItem, Song::toggleLike)
                    }
                }
            }) {
                Icon(
                    painter = painterResource(id = if (likedAt == null) R.drawable.heart_outline else R.drawable.heart),
                    contentDescription = "Like",
                    tint = if (likedAt == null) colorPalette.textSecondary else colorPalette.favoritesIcon,
                    modifier = Modifier.size(28.dp)
                )
            }

            IconButton(onClick = {
                menuState.display {
                    BaseMediaItemMenu(
                        onDismiss = menuState::hide,
                        mediaItem = mediaItem,
                        onGoToAlbum = onGoToAlbum,
                        onGoToArtist = onGoToArtist
                    )
                }
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.add),
                    contentDescription = "Add",
                    tint = colorPalette.textSecondary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Seek Bar
        PlayerSeekBar(
            mediaId = mediaItem.mediaId,
            position = positionAndDuration.first,
            duration = positionAndDuration.second
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Main Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { 
                player.shuffleModeEnabled = !player.shuffleModeEnabled
                player.shuffleQueue()
            }, enabled = !isAzanPlaying) {
                Icon(
                    painter = painterResource(id = R.drawable.shuffle),
                    contentDescription = "Shuffle",
                    tint = if (isAzanPlaying) colorPalette.textSecondary.copy(alpha = 0.5f) 
                           else if (player.shuffleModeEnabled) colorPalette.accent else colorPalette.textSecondary
                )
            }

            IconButton(onClick = { player.forceSeekToPrevious() }, modifier = Modifier.size(48.dp), enabled = !isAzanPlaying) {
                Icon(
                    painter = painterResource(id = R.drawable.play_skip_back),
                    contentDescription = "Previous",
                    tint = if (isAzanPlaying) colorPalette.text.copy(alpha = 0.5f) else colorPalette.text,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Play/Pause
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(if (isAzanPlaying) colorPalette.accent.copy(alpha = 0.5f) else colorPalette.accent)
                    .clickable(enabled = !isAzanPlaying) {
                        if (shouldBePlaying) player.pause() else player.play()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = if (shouldBePlaying) R.drawable.pause else R.drawable.play),
                    contentDescription = if (shouldBePlaying) "Pause" else "Play",
                    tint = colorPalette.onAccent,
                    modifier = Modifier.size(36.dp)
                )
            }

            IconButton(onClick = { player.forceSeekToNext() }, modifier = Modifier.size(48.dp), enabled = !isAzanPlaying) {
                Icon(
                    painter = painterResource(id = R.drawable.play_skip_forward),
                    contentDescription = "Next",
                    tint = if (isAzanPlaying) colorPalette.text.copy(alpha = 0.5f) else colorPalette.text,
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(onClick = { 
                if (trackLoopEnabled) {
                    trackLoopEnabled = false
                } else if (queueLoopEnabled) {
                    queueLoopEnabled = false
                    trackLoopEnabled = true
                } else {
                    queueLoopEnabled = true
                }
            }, enabled = !isAzanPlaying) {
                val repeatMode = when {
                    trackLoopEnabled -> Player.REPEAT_MODE_ONE
                    queueLoopEnabled -> Player.REPEAT_MODE_ALL
                    else -> Player.REPEAT_MODE_OFF
                }
                val icon = when (repeatMode) {
                    Player.REPEAT_MODE_ONE -> painterResource(R.drawable.repeat_one)
                    Player.REPEAT_MODE_ALL -> painterResource(R.drawable.repeat)
                    else -> painterResource(R.drawable.repeat_off)
                }
                Icon(
                    painter = icon,
                    contentDescription = "Repeat",
                    tint = if (isAzanPlaying) colorPalette.textSecondary.copy(alpha = 0.5f) 
                           else if (repeatMode != Player.REPEAT_MODE_OFF) colorPalette.accent else colorPalette.textSecondary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (isShowingSleepTimerDialog) {
        SleepTimer(
            sleepTimerMillisLeft = sleepTimerMillisLeft,
            onDismiss = { isShowingSleepTimerDialog = false }
        )
    }
    if (isShowingVolumeBoosterDialog) {
        VolumeBooster(
            onDismiss = { isShowingVolumeBoosterDialog = false }
        )
    }
}
