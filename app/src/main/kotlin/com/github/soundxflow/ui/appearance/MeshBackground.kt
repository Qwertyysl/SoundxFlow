package com.github.soundxflow.ui.appearance

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.github.core.ui.LocalAppearance
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MeshBackground(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val (colorPalette) = LocalAppearance.current
    
    var dominantColor by remember { mutableStateOf(colorPalette.background1) }
    
    LaunchedEffect(thumbnailUrl) {
        if (thumbnailUrl != null) {
            dominantColor = extractDominantColor(context, thumbnailUrl, colorPalette.background1)
        }
    }
    
    val animatedColor by animateColorAsState(dominantColor, tween(1000), label = "MeshColor")
    
    val infiniteTransition = rememberInfiniteTransition(label = "MeshAnimation")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Box(modifier = modifier.fillMaxSize().background(colorPalette.background3)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            // Draw multiple overlapping radial gradients to simulate a mesh
            val centers = listOf(
                Offset(
                    width * (0.5f + 0.3f * sin(time)),
                    height * (0.5f + 0.3f * cos(time))
                ),
                Offset(
                    width * (0.2f + 0.2f * sin(time + 2f)),
                    height * (0.8f + 0.2f * cos(time + 2f))
                ),
                Offset(
                    width * (0.8f + 0.2f * sin(time + 4f)),
                    height * (0.2f + 0.2f * cos(time + 4f))
                )
            )
            
            centers.forEachIndexed { index, center ->
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            animatedColor.copy(alpha = 0.4f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = maxOf(width, height) * 0.8f
                    ),
                    center = center,
                    radius = maxOf(width, height) * 0.8f
                )
            }
        }
    }
}
