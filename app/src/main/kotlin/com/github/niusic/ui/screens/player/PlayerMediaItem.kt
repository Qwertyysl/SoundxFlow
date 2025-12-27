package com.github.niusic.ui.screens.player

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.github.core.ui.LocalAppearance
import com.github.niusic.Database
import com.github.niusic.LocalPlayerServiceBinder
import com.github.niusic.service.PlayerService
import com.github.niusic.ui.modifier.fadingEdge
import com.github.niusic.utils.DisposableListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PlayerMediaItem(
    onGoToArtist: (() -> Unit)?,
) {
    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player ?: return

    var currentItem by remember {
        mutableStateOf(player.currentMediaItem, neverEqualPolicy())
    }

    var albumYear by remember { mutableStateOf<String?>(null) }

    // Update when player changes
    player.DisposableListener {
        object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentItem = mediaItem
            }
        }
    }

    val fadingEdge = Brush.horizontalGradient(
        0f to Color.Transparent,
        0.1f to Color.Black,
        0.9f to Color.Black,
        1f to Color.Transparent
    )

    val mediaItem = currentItem ?: return
    val (colorPalette) = LocalAppearance.current

    LaunchedEffect(mediaItem.mediaId) {
        withContext(Dispatchers.IO) {
            Database.song(mediaItem.mediaId).collect { song ->
                val albumInfo = Database.songAlbumInfo(mediaItem.mediaId)
                if (albumInfo?.id != null) {
                    Database.album(albumInfo.id).collect { album ->
                        albumYear = album?.year
                    }
                } else {
                    albumYear = null
                }
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .padding(bottom = 8.dp) // Added padding to avoid overlap
    ) {

        // TITLE
        Text(
            text = mediaItem.mediaMetadata.title?.toString().orEmpty(),
            color = colorPalette.text,
            modifier = Modifier.basicMarquee(),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.3f),
                    offset = Offset(2f, 2f),
                    blurRadius = 4f
                )
            ),
            maxLines = 1
        )

        if (!albumYear.isNullOrEmpty()) {
            Text(
                text = albumYear!!,
                color = colorPalette.textSecondary,
                style = MaterialTheme.typography.labelMedium.copy(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.3f),
                        offset = Offset(1f, 1f),
                        blurRadius = 2f
                    )
                ),
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ARTIST
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(
                    enabled = onGoToArtist != null,
                    onClick = onGoToArtist ?: {}
                )
                .fadingEdge(fadingEdge)
                .basicMarquee()
                .padding(horizontal = 8.dp, vertical = 4.dp)   // bigger hitbox
        ) {
            Text(
                text = mediaItem.mediaMetadata.artist?.toString().orEmpty(),
                color = colorPalette.text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.3f),
                        offset = Offset(2f, 2f),
                        blurRadius = 4f
                    )
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

    }
}

