package com.github.niusic.ui.appearance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.github.core.ui.LocalAppearance
import com.github.niusic.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val PLAYER_BACKGROUND_STYLE_KEY = "player_background_style"

// --- NEW CUSTOMIZATION KEYS ---
const val PLAYER_BACKGROUND_CUSTOM_COLOR_1 = "player_bg_color_1" // Primary / Start
const val PLAYER_BACKGROUND_CUSTOM_COLOR_2 = "player_bg_color_2" // Secondary / End (-1 = None)
const val PLAYER_BACKGROUND_IS_ANIMATED = "player_bg_animated"   // True = Breathing, False = Static
const val PLAYER_BACKGROUND_CUSTOM_IMAGE_KEY = "player_background_custom_image"

object BackgroundStyles {
    const val DYNAMIC = 0
    const val ABSTRACT_1 = 1
    const val ABSTRACT_2 = 2
    const val ABSTRACT_3 = 3
    const val ABSTRACT_4 = 4
    const val MESH = 10
    const val GLASS = 11
    const val CUSTOM_IMAGE = 99
}

@Composable
fun PlayerBackground(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val currentStyle by rememberPreference(PLAYER_BACKGROUND_STYLE_KEY, BackgroundStyles.DYNAMIC)
    val customImagePath by rememberPreference(PLAYER_BACKGROUND_CUSTOM_IMAGE_KEY, "")

    // Read new customization prefs
    val isAnimated by rememberPreference(PLAYER_BACKGROUND_IS_ANIMATED, true)

    Box(modifier = modifier.fillMaxSize()) {
        when (currentStyle) {
            BackgroundStyles.DYNAMIC -> {
                // Now passing the animation preference
                DynamicBackground(
                    thumbnailUrl = thumbnailUrl,
                    animate = isAnimated,
                    content = {}
                )
            }
            BackgroundStyles.CUSTOM_IMAGE -> {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    if (customImagePath.isNotEmpty()) {
                        AsyncImage(
                            model = customImagePath,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize().alpha(0.6f)
                        )
                    }
                }
            }
            BackgroundStyles.MESH -> {
                MeshBackground(thumbnailUrl = thumbnailUrl)
            }
            BackgroundStyles.GLASS -> {
                val appearance = LocalAppearance.current
                val isDark = appearance.colorPalette.isDark
                val context = LocalContext.current
                
                var frostedColor by remember { mutableStateOf(Color.White) }
                
                LaunchedEffect(thumbnailUrl) {
                    if (thumbnailUrl != null) {
                        frostedColor = withContext(Dispatchers.IO) {
                            extractDominantColor(context, thumbnailUrl, if (isDark) Color.DarkGray else Color.White)
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize().background(if (isDark) Color.Black else Color.White)) {
                    if (thumbnailUrl != null) {
                        AsyncImage(
                            model = thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(radius = 60.dp)
                                .alpha(if (isDark) 0.6f else 0.8f)
                        )
                    }
                    // Improved iOS-like glass overlay using spread frosted color from album
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        frostedColor.copy(alpha = if (isDark) 0.3f else 0.4f),
                                        if (isDark) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.7f)
                                    )
                                )
                            )
                    )
                }
            }
            else -> {
                ThemedLottieBackground(
                    animationNumber = if (currentStyle in listOf(BackgroundStyles.ABSTRACT_1, BackgroundStyles.ABSTRACT_2, BackgroundStyles.ABSTRACT_3, BackgroundStyles.ABSTRACT_4)) currentStyle else BackgroundStyles.ABSTRACT_1,
                    modifier = Modifier.matchParentSize()
                )
            }
        }
        content()
    }
}
