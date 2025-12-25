package com.github.soundxflow.ui.screens.player

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
import com.github.soundxflow.LocalPlayerServiceBinder
import com.github.soundxflow.R
import com.github.soundxflow.ui.appearance.DynamicBackground
import com.github.soundxflow.ui.styling.Dimensions
import com.github.soundxflow.ui.styling.px
import com.github.soundxflow.utils.rememberPreference
import com.github.soundxflow.utils.DisposableListener
import com.github.soundxflow.utils.positionAndDurationState
import com.github.soundxflow.utils.shouldBePlaying
import com.github.soundxflow.utils.thumbnail

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
    val (colorPalette) = LocalAppearance.current

    val title = mediaItem.mediaMetadata.title?.toString() ?: ""
    val artist = mediaItem.mediaMetadata.artist?.toString() ?: ""
    val album = mediaItem.mediaMetadata.albumTitle?.toString() ?: ""

    val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    DynamicBackground(
        thumbnailUrl = mediaItem.mediaMetadata.artworkUri.toString(),
        animate = false,
        useGradient = false,
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp + navigationBarsPadding)
            .clickable(onClick = openPlayer)
    ) {
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
                        // Draw progress bar at the bottom of the 72dp content area
                        drawRect(
                            color = colorPalette.collapsedPlayerProgressBar.copy(alpha = 0.8f),
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
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = colorPalette.text
                    )
                    Text(
                        text = if (album.isNotEmpty()) "$artist • $album" else artist,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = colorPalette.text.copy(alpha = 0.7f),
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
                    }
                )

                IconButton(
                    onClick = openPlayer,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.chevron_up),
                        contentDescription = "Open Player",
                        tint = colorPalette.text,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
