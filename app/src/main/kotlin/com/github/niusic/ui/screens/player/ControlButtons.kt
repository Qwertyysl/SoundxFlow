@file:kotlin.OptIn(ExperimentalAnimationApi::class)

package com.github.niusic.ui.screens.player

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.github.core.ui.LocalAppearance
import com.github.core.ui.favoritesIcon
import com.github.core.ui.surface
import com.github.core.ui.DesignStyle
import com.github.niusic.ui.appearance.PLAYER_BACKGROUND_STYLE_KEY
import com.github.niusic.ui.appearance.BackgroundStyles
import com.github.niusic.ui.modifier.glassEffect
import com.github.innertube.models.NavigationEndpoint
import com.github.niusic.Database
import com.github.niusic.LocalPlayerServiceBinder
import com.github.niusic.service.PlayerService
import com.github.niusic.R
import com.github.niusic.models.LocalMenuState
import com.github.niusic.models.Song
import com.github.niusic.query
import com.github.niusic.ui.components.BaseMediaItemMenu
import com.github.niusic.ui.components.SeekBar
import com.github.niusic.ui.styling.Dimensions
import com.github.niusic.utils.forceSeekToNext
import com.github.niusic.utils.forceSeekToPrevious
import com.github.niusic.utils.formatAsDuration
import com.github.niusic.utils.queueLoopEnabledKey
import com.github.niusic.utils.rememberPreference
import com.github.niusic.utils.seamlessPlay
import com.github.niusic.utils.shuffleQueue
import com.github.niusic.utils.toast
import com.github.niusic.utils.trackLoopEnabledKey
import com.github.niusic.utils.volumeBoosterEnabledKey
import com.github.niusic.enums.MusicStylePreset
import com.github.niusic.utils.musicStylePresetKey
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf

@Composable
fun AnimatedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.5f),
        label = "scale"
    )

    IconButton(
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        interactionSource = interactionSource,
        enabled = enabled,
        content = content
    )
}

@Composable
fun PlayPauseButton(
    playing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null
) {
    val appearance = LocalAppearance.current
    val colorPalette = appearance.colorPalette
    val backgroundStyle by rememberPreference(PLAYER_BACKGROUND_STYLE_KEY, BackgroundStyles.DYNAMIC)
    val isGlassTheme = appearance.designStyle == DesignStyle.Glass || backgroundStyle == BackgroundStyles.GLASS
    val isDark = colorPalette.isDark

    val contentColor = tint ?: colorPalette.iconColor
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = 0.4f),
        label = "scale"
    )

    Box(
        modifier = modifier
            .size(64.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .then(
                if (isGlassTheme) {
                    Modifier.glassEffect(
                        shape = CircleShape,
                        alpha = 0.1f,
                        borderColor = (if (isDark) Color.White else Color.Black).copy(alpha = 0.1f)
                    )
                } else {
                    Modifier.background(colorPalette.accent.copy(alpha = 0.15f))
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = if (playing) R.drawable.pause else R.drawable.play),
            contentDescription = if (playing) "Pause" else "Play",
            tint = if (isGlassTheme) contentColor else colorPalette.accent,
            modifier = Modifier.size(32.dp)
        )
    }
}

@SuppressLint("SuspiciousIndentation")
@Composable
fun MiniPlayerControl(
    playing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null
) {
    val binder = LocalPlayerServiceBinder.current
    val (colorPalette) = LocalAppearance.current
    val isAzanPlaying by rememberPreference(com.github.niusic.utils.isAzanPlayingKey, false)
    
    val contentColor = tint ?: colorPalette.iconColor
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
    ) {
        //skip back button
        AnimatedIconButton(
            onClick = { binder?.player?.forceSeekToPrevious() }, 
            modifier = modifier.size(36.dp),
            enabled = !isAzanPlaying
        ) {
            Icon(
                painter = painterResource(id = R.drawable.fast_backward),
                contentDescription = null,
                tint = if (isAzanPlaying) contentColor.copy(alpha = 0.5f) else contentColor,
                modifier = Modifier.size(14.dp)
            )
        }
        //play or pause button
        AnimatedIconButton(
            onClick = onClick,
            modifier = Modifier
                .size(36.dp)
                .semantics { contentDescription = if (playing) "Pause" else "Play" },
            enabled = !isAzanPlaying
        ) {
            Icon(
                painter = painterResource(id = if (playing) R.drawable.pause else R.drawable.play),
                contentDescription = null,
                tint = if (isAzanPlaying) contentColor.copy(alpha = 0.5f) else contentColor,
                modifier = Modifier.size(24.dp)
            )
        }
        //skip next button
        AnimatedIconButton(
            onClick = { binder?.player?.forceSeekToNext() },
            modifier = modifier.size(36.dp),
            enabled = !isAzanPlaying
        ) {
            Icon(
                painter = painterResource(id = R.drawable.fast_forward),
                contentDescription = null,
                tint = if (isAzanPlaying) contentColor.copy(alpha = 0.5f) else contentColor,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun PlayerMiddleControl(
    mediaId: String,
    likedAt: Long?,
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit,
    showPlaylist: Boolean,
    onTogglePlaylist: (Boolean) -> Unit,
    textColor: Color? = null
) {
    val binder = LocalPlayerServiceBinder.current
    val appearance = LocalAppearance.current
    val colorPalette = appearance.colorPalette
    val menuState = LocalMenuState.current
    val backgroundStyle by rememberPreference(PLAYER_BACKGROUND_STYLE_KEY, BackgroundStyles.DYNAMIC)
    val isGlassTheme = appearance.designStyle == DesignStyle.Glass || backgroundStyle == BackgroundStyles.GLASS

    val contentColor = textColor ?: colorPalette.iconColor

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .then(
                if (isGlassTheme) {
                    Modifier.glassEffect(shape = RoundedCornerShape(24.dp), alpha = 0.1f)
                        .padding(vertical = 4.dp)
                } else Modifier
            )
    ) {
        AnimatedIconButton(
            onClick = { onTogglePlaylist(!showPlaylist) }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.playlist),
                contentDescription = "Playlist",
                tint = if (showPlaylist) colorPalette.accent else contentColor,
                modifier = Modifier.size(24.dp)
            )
        }

        AnimatedIconButton(
            onClick = {
                val currentMediaItem = binder?.player?.currentMediaItem
                query {
                    if (Database.like(
                            mediaId,
                            if (likedAt == null) System.currentTimeMillis() else null
                        ) == 0
                    ) {
                        currentMediaItem
                            ?.takeIf { it.mediaId == mediaId }
                            ?.let {
                                Database.insert(currentMediaItem, Song::toggleLike)
                            }
                    }
                }
            }
        ) {
            Icon(
                painter = painterResource(id = if (likedAt == null) R.drawable.heart_outline else R.drawable.heart),
                contentDescription = "Like",
                tint = (if (likedAt == null) contentColor else colorPalette.favoritesIcon),
                modifier = Modifier.size(24.dp)
            )
        }

        AnimatedIconButton(
            onClick = {
                val currentMediaItem = binder?.player?.currentMediaItem ?: return@AnimatedIconButton
                menuState.display {
                    BaseMediaItemMenu(
                        onDismiss = menuState::hide,
                        mediaItem = currentMediaItem,
                        onGoToAlbum = onGoToAlbum,
                        onGoToArtist = onGoToArtist
                    )
                }
            },
        ) {
            Icon(
                painter = painterResource(id = R.drawable.add),
                contentDescription = "Add",
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun PlayerControlBottom(
    shouldBePlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    textColor: Color? = null
) {
    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player ?: return
    val appearance = LocalAppearance.current
    val colorPalette = appearance.colorPalette
    val backgroundStyle by rememberPreference(PLAYER_BACKGROUND_STYLE_KEY, BackgroundStyles.DYNAMIC)
    val isGlassTheme = appearance.designStyle == DesignStyle.Glass || backgroundStyle == BackgroundStyles.GLASS

    val contentColor = textColor ?: colorPalette.iconColor

    var trackLoopEnabled by rememberPreference(trackLoopEnabledKey, defaultValue = false)
    var queueLoopEnabled by rememberPreference(queueLoopEnabledKey, defaultValue = false)
    val isAzanPlaying by rememberPreference(com.github.niusic.utils.isAzanPlayingKey, false)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .then(
                if (isGlassTheme) {
                    Modifier.glassEffect(shape = RoundedCornerShape(32.dp), alpha = 0.15f)
                        .padding(vertical = 8.dp, horizontal = 12.dp)
                } else Modifier
            )
    ) {
        // Shuffle
        AnimatedIconButton(
            onClick = { player.shuffleQueue() },
            enabled = !isAzanPlaying
        ) {
            Icon(
                painter = painterResource(id = R.drawable.shuffle),
                contentDescription = "Shuffle",
                tint = if (isAzanPlaying) contentColor.copy(alpha = 0.5f) else if (player.shuffleModeEnabled) colorPalette.accent else contentColor,
                modifier = Modifier.size(24.dp)
            )
        }

        // Previous
        AnimatedIconButton(
            onClick = { binder.player.forceSeekToPrevious() },
            enabled = !isAzanPlaying
        ) {
            Icon(
                painter = painterResource(id = R.drawable.fast_backward),
                contentDescription = "Previous",
                tint = if (isAzanPlaying) contentColor.copy(alpha = 0.5f) else contentColor,
                modifier = Modifier.size(18.dp)
            )
        }

        // Play / Pause
        PlayPauseButton(
            playing = shouldBePlaying,
            onClick = onPlayPauseClick,
            modifier = Modifier.alpha(if (isAzanPlaying) 0.5f else 1f),
            tint = contentColor
        )

        // Next
        AnimatedIconButton(
            onClick = { binder.player.forceSeekToNext() },
            enabled = !isAzanPlaying
        ) {
            Icon(
                painter = painterResource(id = R.drawable.fast_forward),
                contentDescription = "Next",
                tint = if (isAzanPlaying) contentColor.copy(alpha = 0.5f) else contentColor,
                modifier = Modifier.size(18.dp)
            )
        }

        // Repeat
        AnimatedIconButton(
            onClick = {
                if (trackLoopEnabled) {
                    trackLoopEnabled = false
                } else if (queueLoopEnabled) {
                    queueLoopEnabled = false
                    trackLoopEnabled = true
                } else {
                    queueLoopEnabled = true
                }
            },
            enabled = !isAzanPlaying
        ) {
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
            val alpha = if (repeatMode == Player.REPEAT_MODE_OFF) Dimensions.lowOpacity else 1f

            Icon(
                painter = icon,
                tint = if (isAzanPlaying) contentColor.copy(alpha = 0.5f) else if (repeatMode != Player.REPEAT_MODE_OFF) colorPalette.accent else contentColor,
                contentDescription = null,
                modifier = Modifier
                    .alpha(if (isAzanPlaying) 0.5f else alpha)
                    .size(28.dp)
            )
        }
    }
}

@OptIn(UnstableApi::class)
@UnstableApi
@Composable
fun PlayerTopControl(
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit,
    onBack: () -> Unit
) {
    val menuState = LocalMenuState.current
    val (colorPalette) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    binder?.player ?: return

    var nullableMediaItem by remember {
        mutableStateOf(
            binder.player.currentMediaItem,
            neverEqualPolicy()
        )
    }
    val context = LocalContext.current

    val activityResultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    val mediaItem = nullableMediaItem ?: return

    var isShowingSleepTimerDialog by rememberSaveable { mutableStateOf(false) }
    var isShowingVolumeBoosterDialog by rememberSaveable { mutableStateOf(false) }
    var isShowingMusicStyleDialog by rememberSaveable { mutableStateOf(false) }
    val sleepTimerMillisLeft by (binder.sleepTimerMillisLeft
        ?: flowOf(null))
        .collectAsState(initial = null)

    val volumeBoosterEnabled by rememberPreference(volumeBoosterEnabledKey, false)
    var musicStylePreset by rememberPreference(musicStylePresetKey, MusicStylePreset.None)

    val backgroundStyle by rememberPreference(PLAYER_BACKGROUND_STYLE_KEY, BackgroundStyles.DYNAMIC)
    val isGlassTheme = LocalAppearance.current.designStyle == DesignStyle.Glass || backgroundStyle == BackgroundStyles.GLASS
    val isDark = colorPalette.isDark

    val buttonModifier = if (isGlassTheme) {
        Modifier.glassEffect(
            shape = CircleShape,
            alpha = 0.1f,
            borderColor = (if (isDark) Color.White else Color.Black).copy(alpha = 0.1f)
        )
    } else Modifier

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp, vertical = 16.dp)
    ) {

        IconButton(
            onClick = onBack,
        ){
            Icon(
                painter = painterResource(id = R.drawable.arrow_down),
                tint = colorPalette.iconColor,
                contentDescription = null,
                modifier = Modifier
                    .size(22.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (sleepTimerMillisLeft != null) {
                Text(
                    text = formatTime(sleepTimerMillisLeft ?: 0L),
                    color = colorPalette.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = buttonModifier
                        .clip(CircleShape)
                        .clickable {
                            isShowingSleepTimerDialog = true
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            } else {
                IconButton(
                    onClick = { isShowingSleepTimerDialog = true },
                    modifier = buttonModifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Timer,
                        tint = colorPalette.iconColor,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            IconButton(
                onClick = { isShowingMusicStyleDialog = true },
                modifier = buttonModifier.size(36.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.equalizer),
                    tint = if (musicStylePreset != MusicStylePreset.None) colorPalette.accent else colorPalette.iconColor,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = { isShowingVolumeBoosterDialog = true },
                modifier = buttonModifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    tint = if (volumeBoosterEnabled) colorPalette.accent else colorPalette.iconColor,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = {
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
                },
                modifier = buttonModifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    tint = colorPalette.iconColor,
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                )
            }
        }
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
    if (isShowingMusicStyleDialog) {
        MusicStyleDialog(
            onDismiss = { isShowingMusicStyleDialog = false }
        )
    }
}

@Composable
fun MusicStyleDialog(
    onDismiss: () -> Unit
) {
    var musicStylePreset by rememberPreference(musicStylePresetKey, MusicStylePreset.None)
    val appearance = LocalAppearance.current
    val colorPalette = appearance.colorPalette

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.music_style)) },
        text = {
            Column {
                Text(
                    text = stringResource(id = R.string.music_style_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorPalette.text.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MusicStylePreset.entries.chunked(2).forEach { rowPresets ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowPresets.forEach { preset ->
                                FilterChip(
                                    selected = musicStylePreset == preset,
                                    onClick = { musicStylePreset = preset },
                                    modifier = Modifier.weight(1f),
                                    label = {
                                        Text(
                                            text = when (preset) {
                                                MusicStylePreset.None -> stringResource(id = R.string.none)
                                                MusicStylePreset.VocalBoost -> stringResource(id = R.string.vocal_boost)
                                                MusicStylePreset.MusicBoost -> stringResource(id = R.string.music_boost)
                                                MusicStylePreset.DolbyAtmos -> stringResource(id = R.string.dolby_atmos)
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.done))
            }
        }
    )
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}


@Composable
fun PlayerSeekBar(
    mediaId: String,
    position: Long,
    duration: Long
) {
    val binder = LocalPlayerServiceBinder.current
    binder?.player ?: return

    PlayerSeekBarDefault(
        mediaId = mediaId,
        position = position,
        duration = duration
    )
}

@Composable
private fun PlayerSeekBarDefault(
    mediaId: String,
    position: Long,
    duration: Long
) {
    val binder = LocalPlayerServiceBinder.current
    binder?.player ?: return

    var scrubbingPosition by remember(mediaId) { mutableStateOf<Long?>(null) }

    val appearance = LocalAppearance.current
    val colorPalette = appearance.colorPalette
    val backgroundStyle by rememberPreference(PLAYER_BACKGROUND_STYLE_KEY, BackgroundStyles.DYNAMIC)
    val isGlassTheme = appearance.designStyle == DesignStyle.Glass || backgroundStyle == BackgroundStyles.GLASS

    val seekBarColor = if (isGlassTheme) {
        if (colorPalette.isDark) Color(0xFFCCCCCC) else Color.White
    } else colorPalette.accent

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isGlassTheme) 24.dp else 18.dp)
            .then(
                if (isGlassTheme) {
                    Modifier.glassEffect(shape = RoundedCornerShape(16.dp), alpha = 0.1f)
                        .padding(vertical = 8.dp, horizontal = 16.dp)
                } else Modifier
            )
    ) {

        SeekBar(
            value = scrubbingPosition ?: position,
            minimumValue = 0L,
            maximumValue = duration,
            onDragStart = { scrubbingPosition = it },
            onDrag = { delta -> scrubbingPosition = ((scrubbingPosition ?: position) + delta).coerceIn(0L, duration) },
            onDragEnd = {
                scrubbingPosition?.let {
                    binder.player.seekTo(it)
                    scrubbingPosition = null
                }
            },
            color = seekBarColor,
            backgroundColor = (if (colorPalette.isDark) Color.White else Color.Black).copy(alpha = 0.1f)
        )

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = formatAsDuration(scrubbingPosition ?: position),
                style = MaterialTheme.typography.labelSmall,
                color = if (isGlassTheme) contentColor(colorPalette.isDark, (scrubbingPosition ?: position).toFloat() / duration.toFloat()) else colorPalette.textSecondary
            )

            Text(
                text = formatAsDuration(duration),
                style = MaterialTheme.typography.labelSmall,
                color = if (isGlassTheme) contentColor(colorPalette.isDark, 1f) else colorPalette.textSecondary
            )
        }
    }
}

private fun contentColor(isDark: Boolean, fraction: Float): Color {
    return if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f)
}
