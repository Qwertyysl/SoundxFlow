package com.github.niusic.ui.screens.player

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.github.niusic.Database
import com.github.niusic.utils.rememberPreference
import com.github.niusic.utils.isFloatingLyricsEnabledKey
import com.github.niusic.utils.positionAndDurationState
import com.github.kugou.KuGou

@Composable
fun FloatingLyrics(
    mediaId: String,
    player: Player,
    modifier: Modifier = Modifier
) {
    val isEnabled by rememberPreference(isFloatingLyricsEnabledKey, true) // Default to true for now
    if (!isEnabled) return

    val lyricsFlow = remember(mediaId) { Database.lyrics(mediaId) }
    val lyrics by lyricsFlow.collectAsState(initial = null)
    val syncedLyricsText = lyrics?.synced
    
    val positionAndDuration by player.positionAndDurationState()
    val currentPosition = positionAndDuration.first
    
    val sentences = remember(syncedLyricsText) {
        if (syncedLyricsText.isNullOrBlank()) emptyList()
        else com.github.niusic.utils.LrcParser.parse(syncedLyricsText)
    }
    
    if (sentences.isEmpty()) return
    
    val currentSentenceIndex = sentences.indexOfLast { it.first <= currentPosition }
    if (currentSentenceIndex == -1) return
    val currentSentence = sentences.getOrNull(currentSentenceIndex)?.second ?: ""
    
    if (currentSentence.isBlank()) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp, start = 20.dp, end = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.6f)
                    )
                )
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = currentSentence,
            transitionSpec = {
                (fadeIn(tween(600))).togetherWith(fadeOut(tween(600)))
            },
            label = "floatingLyrics"
        ) { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                    letterSpacing = (-0.5).sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

