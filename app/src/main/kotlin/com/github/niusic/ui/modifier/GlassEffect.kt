package com.github.niusic.ui.modifier

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.core.ui.LocalAppearance

@Composable
fun Modifier.glassEffect(
    shape: Shape,
    blur: Dp = 20.dp,
    alpha: Float = 0.1f,
    borderColor: Color = Color.White.copy(alpha = 0.2f),
    borderWidth: Dp = 0.5.dp,
    useLiquidHighlight: Boolean = false
): Modifier = composed {
    val appearance = LocalAppearance.current
    val isDark = appearance.colorPalette.isDark
    
    val baseModifier = this
        .clip(shape)
        .background(
            Color.White.copy(alpha = if (isDark) alpha * 1.5f else alpha)
        )
        .border(
            width = borderWidth,
            brush = Brush.linearGradient(
                listOf(
                    borderColor,
                    borderColor.copy(alpha = 0.05f)
                )
            ),
            shape = shape
        )
    
    if (useLiquidHighlight) {
        baseModifier.drawBehind {
            // Liquid lens highlight simulation
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(size.width * 0.25f, size.height * 0.25f),
                    radius = size.minDimension
                ),
                size = size
            )
        }
    } else {
        baseModifier
    }
}
