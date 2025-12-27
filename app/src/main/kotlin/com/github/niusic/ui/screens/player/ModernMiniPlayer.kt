package com.github.niusic.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import com.github.core.ui.LocalAppearance
import com.github.core.ui.DesignStyle
import com.github.niusic.LocalPlayerServiceBinder
import com.github.niusic.service.PlayerService
import com.github.niusic.R
import com.github.niusic.ui.styling.Dimensions
import com.github.niusic.ui.styling.px
import com.github.niusic.ui.appearance.PLAYER_BACKGROUND_STYLE_KEY
import com.github.niusic.ui.appearance.BackgroundStyles
import com.github.niusic.ui.appearance.extractDominantColor
import com.github.niusic.ui.modifier.glassEffect
import com.github.niusic.utils.rememberPreference
import com.github.niusic.utils.DisposableListener
import com.github.niusic.utils.positionAndDurationState
import com.github.niusic.utils.shouldBePlaying
import com.github.niusic.utils.thumbnail
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.github.niusic.utils.forceSeekToNext
import com.github.niusic.utils.forceSeekToPrevious

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernMiniPlayer(
    openPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player ?: return

    var shouldBePlaying by remember { mutableStateOf(player.shouldBePlaying) }

    var nullableMediaItem by remember {
        mutableStateOf(player.currentMediaItem, neverEqualPolicy())
    }

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
    val positionAndDuration by player.positionAndDurationState()
    val isAzanPlaying by rememberPreference(com.github.niusic.utils.isAzanPlayingKey, false)
    val mediaItem = nullableMediaItem ?: return
    val context = LocalContext.current
    val appearance = LocalAppearance.current
    val colorPalette = appearance.colorPalette
    val backgroundStyle by rememberPreference(PLAYER_BACKGROUND_STYLE_KEY, BackgroundStyles.DYNAMIC)
    val isGlassTheme = appearance.designStyle == DesignStyle.Glass || backgroundStyle == BackgroundStyles.GLASS

    var adaptiveContentColor by remember { mutableStateOf(colorPalette.text) }
    
    LaunchedEffect(mediaItem.mediaMetadata.artworkUri) {
        val dominant = withContext(Dispatchers.IO) {
            extractDominantColor(context, mediaItem.mediaMetadata.artworkUri?.toString(), colorPalette.background1)
        }
        adaptiveContentColor = if (dominant.luminance() > 0.5f) Color.Black else Color.White
    }

    val contentColor = if (isGlassTheme) adaptiveContentColor else colorPalette.text
    val title = if (isAzanPlaying) "AZAN" else mediaItem.mediaMetadata.title?.toString() ?: ""
    val artist = if (isAzanPlaying) "Playing Azan..." else mediaItem.mediaMetadata.artist?.toString() ?: ""

    val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Modern look: Floating card style
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp + navigationBarsPadding) // Slightly taller
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .then(
                if (isGlassTheme) {
                    Modifier.glassEffect(shape = RoundedCornerShape(16.dp), alpha = 0.15f)
                } else {
                    Modifier.clip(RoundedCornerShape(16.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    colorPalette.background2.copy(alpha = 0.95f),
                                    colorPalette.background1.copy(alpha = 0.95f)
                                )
                            )
                        )
                }
            )
            .clickable(onClick = openPlayer)
    ) {
        // Progress Indicator as background or bottom line? 
        // Let's do a bottom line inside the card
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val position = positionAndDuration.first
                    val duration = positionAndDuration.second

                    val fraction = if (duration > 0L) {
                        (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    val barWidth = size.width * fraction

                    if (barWidth > 0f) {
                        drawRect(
                            color = colorPalette.accent, // Use accent color
                            topLeft = Offset(0f, size.height - 2.dp.toPx()),
                            size = Size(width = barWidth, height = 2.dp.toPx())
                        )
                    }
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 2.dp) // Space for progress bar
            ) {
                AsyncImage(
                    model = mediaItem.mediaMetadata.artworkUri.thumbnail(Dimensions.thumbnails.song.px),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .clip(CircleShape) // Circular artwork for Modern look? Or Rounded? Let's go Rounded.
                        .clip(RoundedCornerShape(8.dp))
                        .size(48.dp),
                    placeholder = painterResource(id = R.drawable.app_icon),
                    error = painterResource(id = R.drawable.app_icon),
                    fallback = painterResource(id = R.drawable.app_icon)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            shadow = if (isGlassTheme) Shadow(
                                color = Color.Black.copy(alpha = 0.3f),
                                offset = Offset(1f, 1f),
                                blurRadius = 2f
                            ) else null
                        ),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = contentColor
                    )
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodySmall.copy(
                            shadow = if (isGlassTheme) Shadow(
                                color = if (contentColor == Color.White) Color.Black.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.3f),
                                offset = Offset(1f, 1f),
                                blurRadius = 2f
                            ) else null
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = contentColor.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }

                // Controls Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Previous
                    IconButton(
                        onClick = { player.forceSeekToPrevious() },
                        modifier = Modifier.size(32.dp),
                        enabled = !isAzanPlaying
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.play_skip_back),
                            contentDescription = "Previous",
                            tint = if (isAzanPlaying) contentColor.copy(alpha = 0.5f) else contentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Play/Pause Button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isAzanPlaying) colorPalette.accent.copy(alpha = 0.05f) else colorPalette.accent.copy(alpha = 0.1f))
                            .clickable(enabled = !isAzanPlaying) {
                                if (shouldBePlaying) player.pause()
                                else {
                                    if (player.playbackState == Player.STATE_IDLE) {
                                        player.prepare()
                                    } else if (player.playbackState == Player.STATE_ENDED) {
                                        player.seekToDefaultPosition(0)
                                    }
                                    player.play()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = if (shouldBePlaying) R.drawable.pause else R.drawable.play),
                            contentDescription = if (shouldBePlaying) "Pause" else "Play",
                            tint = if (isAzanPlaying) colorPalette.accent.copy(alpha = 0.5f) else colorPalette.accent,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Next
                    IconButton(
                        onClick = { player.forceSeekToNext() },
                        modifier = Modifier.size(32.dp),
                        enabled = !isAzanPlaying
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.play_skip_forward),
                            contentDescription = "Next",
                            tint = if (isAzanPlaying) contentColor.copy(alpha = 0.5f) else contentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

