package com.github.niusic.ui.components

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.github.core.ui.LocalAppearance
import com.github.core.ui.DesignStyle
import com.github.niusic.ui.modifier.glassEffect
import androidx.compose.foundation.border
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.toSize
import kotlin.math.roundToLong

@Composable
fun SeekBar(
    value: Long,
    minimumValue: Long,
    maximumValue: Long,
    onDragStart: (Long) -> Unit,
    onDrag: (Long) -> Unit,
    onDragEnd: () -> Unit,
    color: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    barHeight: Dp = 4.dp, // Increased default height for iOS look
    scrubberColor: Color = color,
    scrubberRadius: Dp = 8.dp, // Slightly larger scrubber
    shape: Shape = RoundedCornerShape(2.dp),
    drawSteps: Boolean = false,
) {
    val appearance = LocalAppearance.current
    val colorPalette = appearance.colorPalette
    val isGlassTheme = appearance.designStyle == DesignStyle.Glass
    
    val isDragging = remember {
        MutableTransitionState(false)
    }

    val transition = rememberTransition(transitionState = isDragging, label = null)

    val currentBarHeight by transition.animateDp(label = "") { if (it) scrubberRadius * 1.5f else barHeight }
    val currentScrubberRadius by transition.animateDp(label = "") { if (it) 0.dp else scrubberRadius }

    val trackShape = RoundedCornerShape(12.dp) // iOS sliders are pill-shaped
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .onSizeChanged { containerSize = it }
            .pointerInput(minimumValue, maximumValue) {
                if (maximumValue < minimumValue) return@pointerInput

                var acc = 0f

                detectHorizontalDragGestures(
                    onDragStart = {
                        isDragging.targetState = true
                    },
                    onHorizontalDrag = { _, delta ->
                        acc += delta / size.width * (maximumValue - minimumValue)

                        if (acc !in -1f..1f) {
                            onDrag(acc.toLong())
                            acc -= acc.toLong()
                        }
                    },
                    onDragEnd = {
                        isDragging.targetState = false
                        acc = 0f
                        onDragEnd()
                    },
                    onDragCancel = {
                        isDragging.targetState = false
                        acc = 0f
                        onDragEnd()
                    }
                )
            }
            .pointerInput(minimumValue, maximumValue) {
                if (maximumValue < minimumValue) return@pointerInput

                detectTapGestures(
                    onPress = { offset ->
                        onDragStart((offset.x / size.width * (maximumValue - minimumValue) + minimumValue).roundToLong())
                    },
                    onTap = {
                        onDragEnd()
                    }
                )
            }
            .height(if (isGlassTheme) 24.dp else scrubberRadius * 2) // More touch area for glass
            .fillMaxWidth()
    ) {
        // Track Background
        Box(
            modifier = Modifier
                .height(if (isGlassTheme) 8.dp else currentBarHeight)
                .fillMaxWidth()
                .then(
                    if (isGlassTheme) {
                        Modifier.glassEffect(shape = trackShape, alpha = 0.15f)
                    } else {
                        Modifier.background(color = backgroundColor, shape = shape)
                    }
                )
                .align(Alignment.Center)
        )

        // Progress
        val progressFraction = if (maximumValue > minimumValue) (value.toFloat() - minimumValue) / (maximumValue - minimumValue) else 0f
        Box(
            modifier = Modifier
                .height(if (isGlassTheme) 8.dp else currentBarHeight)
                .fillMaxWidth(progressFraction)
                .background(
                    color = if (isGlassTheme) colorPalette.accent.copy(alpha = 0.6f) else color, 
                    shape = if (isGlassTheme) trackShape else shape
                )
                .align(Alignment.CenterStart)
        )

        // Thumb positioning
        if (isGlassTheme) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .graphicsLayer {
                        translationX = (progressFraction * containerSize.width) - 14.dp.toPx()
                    }
                    .size(28.dp)
                    .glassEffect(shape = CircleShape, alpha = 0.2f, useLiquidHighlight = true)
            ) {
                // Specular highlight (secondary layer for more liquid feel)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.4f), Color.Transparent),
                                center = Offset(8f, 8f),
                                radius = 35f
                            )
                        )
                )
            }
        } else {
            // Standard Scrubber
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = scrubberRadius)
                    .align(Alignment.Center)
            ) {
                val scrubberPosition = if (maximumValue < minimumValue) 0f else progressFraction
                
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            val trackWidth = containerSize.width.toFloat() - (scrubberRadius.toPx() * 2)
                            translationX = (scrubberPosition * trackWidth) + scrubberRadius.toPx() - currentScrubberRadius.toPx()
                        }
                        .size(currentScrubberRadius * 2)
                        .background(color = scrubberColor, shape = CircleShape)
                )
            }
        }
    }
}


