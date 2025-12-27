package com.github.niusic.ui.screens.player

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
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
import com.github.core.ui.collapsedPlayerProgressBar
import com.github.core.ui.DesignStyle
import com.github.niusic.LocalPlayerServiceBinder
import com.github.niusic.service.PlayerService
import com.github.niusic.R
import com.github.niusic.ui.appearance.DynamicBackground
import com.github.niusic.ui.appearance.PLAYER_BACKGROUND_STYLE_KEY
import com.github.niusic.ui.appearance.BackgroundStyles
import com.github.niusic.ui.modifier.glassEffect
import com.github.niusic.ui.appearance.extractDominantColor
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.github.niusic.ui.styling.Dimensions
import com.github.niusic.ui.styling.px
import com.github.niusic.utils.rememberPreference
import com.github.niusic.utils.DisposableListener
import com.github.niusic.utils.positionAndDurationState
import com.github.niusic.utils.shouldBePlaying
import com.github.niusic.utils.thumbnail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewMiniPlayer(
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
    val mediaItem = nullableMediaItem ?: return
    val context = LocalContext.current
    val appearance = LocalAppearance.current
    val colorPalette = appearance.colorPalette
    val backgroundStyle by rememberPreference(PLAYER_BACKGROUND_STYLE_KEY, BackgroundStyles.DYNAMIC)
    val isGlassTheme = appearance.designStyle == DesignStyle.Glass || backgroundStyle == BackgroundStyles.GLASS

    var adaptiveContentColor by remember { mutableStateOf(colorPalette.text) }
    
    LaunchedEffect(mediaItem.mediaMetadata.artworkUri, appearance.designStyle, backgroundStyle) {
        val dominant = withContext(Dispatchers.IO) {
            extractDominantColor(context, mediaItem.mediaMetadata.artworkUri?.toString(), colorPalette.background1)
        }
        
        if (isGlassTheme) {
            adaptiveContentColor = if (colorPalette.isDark) {
                if (dominant.luminance() > 0.8f) Color(0xFFCCCCCC) else Color.White
            } else {
                if (dominant.luminance() < 0.3f) Color.Black.copy(alpha = 0.8f) else Color.Black
            }
        } else {
            adaptiveContentColor = colorPalette.text
        }
    }

    val contentColor = adaptiveContentColor
    val title = mediaItem.mediaMetadata.title?.toString() ?: ""
    val artist = mediaItem.mediaMetadata.artist?.toString() ?: ""
    val album = mediaItem.mediaMetadata.albumTitle?.toString() ?: ""

    val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val seekBarColor = if (isGlassTheme) {
        if (colorPalette.isDark) Color(0xFFCCCCCC) else Color.White
    } else colorPalette.collapsedPlayerProgressBar.copy(alpha = 0.8f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = navigationBarsPadding)
            .height(72.dp)
            .then(
                if (isGlassTheme) {
                    Modifier.glassEffect(shape = RoundedCornerShape(0.dp), alpha = 0.15f)
                } else {
                    Modifier 
                }
            )
    ) {
        if (!isGlassTheme) {
            DynamicBackground(
                thumbnailUrl = mediaItem.mediaMetadata.artworkUri.toString(),
                animate = false,
                useGradient = false,
                modifier = Modifier.fillMaxSize()
            ) {}
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = openPlayer)
                .drawBehind {
                    val position = positionAndDuration.first
                    val duration = positionAndDuration.second

                    val fraction = if (duration > 0L) {
                        (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    val barWidth = size.width * fraction

                    if (barWidth > 0f) {
                        drawRect(
                            color = seekBarColor,
                            topLeft = Offset(0f, 72.dp.toPx() - 2.dp.toPx()),
                            size = Size(width = barWidth, height = 2.dp.toPx())
                        )
                    }
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 2.dp)
            ) {
                AsyncImage(
                    model = mediaItem.mediaMetadata.artworkUri.thumbnail(Dimensions.thumbnails.song.px),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
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
                                color = if (contentColor.luminance() > 0.5f) Color.Black.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.3f),
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
                        text = if (album.isNotEmpty()) "$artist • $album" else artist,
                        style = MaterialTheme.typography.bodySmall.copy(
                            shadow = if (isGlassTheme) Shadow(
                                color = if (contentColor.luminance() > 0.5f) Color.Black.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.3f),
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

                MiniPlayerControl(
                    playing = shouldBePlaying,
                    onClick = {
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
                    tint = contentColor
                )

                IconButton(
                    onClick = openPlayer,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.chevron_up),
                        contentDescription = "Open Player",
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
