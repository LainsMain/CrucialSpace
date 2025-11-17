package com.crucialspace.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive - Gold Accent System
 * 
 * Animated gold shimmer and decorative accent elements
 */

val GoldColors = listOf(
    Color(0xFFFFE082),
    Color(0xFFFFD54F),
    Color(0xFFFFF59D),
    Color(0xFFFFD54F),
    Color(0xFFFFE082)
)

/**
 * Animated gold shimmer background - premium feel
 */
@Composable
fun GoldShimmerBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gold_shimmer")
    val offset by infiniteTransition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )
    
    Box(
        modifier = modifier
            .drawBehind {
                val shimmerWidth = 400f
                val shimmerBrush = Brush.linearGradient(
                    colors = listOf(
                        Color(0x00FFD54F),
                        Color(0x40FFD54F),
                        Color(0x80FFD54F),
                        Color(0x40FFD54F),
                        Color(0x00FFD54F)
                    ),
                    start = Offset(offset, 0f),
                    end = Offset(offset + shimmerWidth, size.height)
                )
                drawRect(brush = shimmerBrush)
            }
    ) {
        content()
    }
}

/**
 * Glowing gold accent - highlights important elements
 */
@Composable
fun GoldGlow(
    modifier: Modifier = Modifier,
    intensity: Float = 0.3f,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glow by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )
    
    Box(
        modifier = modifier
            .drawBehind {
                val glowBrush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFD54F).copy(alpha = intensity * glow),
                        Color(0x00FFD54F)
                    ),
                    center = center,
                    radius = size.maxDimension
                )
                drawRect(brush = glowBrush)
            }
    ) {
        content()
    }
}

/**
 * Animated gold wave divider - section separator
 */
@Composable
fun GoldWaveDivider(
    modifier: Modifier = Modifier
) {
    SquigglyWaveform(
        modifier = modifier.fillMaxWidth(),
        style = SquigglyStyle.SECTION_DIVIDER,
        gradientColors = GoldColors,
        animated = true,
        height = 24.dp
    )
}

/**
 * Gold squiggly underline - emphasizes text
 */
@Composable
fun GoldSquigglyUnderline(
    modifier: Modifier = Modifier
) {
    SquigglyWaveform(
        modifier = modifier.fillMaxWidth(),
        style = SquigglyStyle.DECORATIVE_ACCENT,
        gradientColors = GoldColors,
        animated = true,
        height = 12.dp
    )
}

/**
 * Subtle gold gradient background
 */
@Composable
fun GoldGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0x10FFD54F),
                    Color(0x05FFD54F),
                    Color(0x00FFD54F)
                )
            )
        )
    ) {
        content()
    }
}

/**
 * Animated gold border - flowing around edges
 */
@Composable
fun GoldFlowingBorder(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "flowing_border")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "border_flow"
    )
    
    Box(
        modifier = modifier
            .drawBehind {
                val borderWidth = 3.dp.toPx()
                val gradientBrush = Brush.linearGradient(
                    colors = GoldColors,
                    start = Offset(offset, 0f),
                    end = Offset(offset + 300f, 200f)
                )
                
                // Top
                drawRect(
                    brush = gradientBrush,
                    topLeft = Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(size.width, borderWidth)
                )
                // Right
                drawRect(
                    brush = gradientBrush,
                    topLeft = Offset(size.width - borderWidth, 0f),
                    size = androidx.compose.ui.geometry.Size(borderWidth, size.height)
                )
                // Bottom
                drawRect(
                    brush = gradientBrush,
                    topLeft = Offset(0f, size.height - borderWidth),
                    size = androidx.compose.ui.geometry.Size(size.width, borderWidth)
                )
                // Left
                drawRect(
                    brush = gradientBrush,
                    topLeft = Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(borderWidth, size.height)
                )
            }
    ) {
        content()
    }
}

/**
 * Gold sparkle effect - for important indicators
 */
@Composable
fun GoldSparkle(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sparkle")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkle_alpha"
    )
    
    Box(
        modifier = modifier
            .size(8.dp)
            .background(
                color = Color(0xFFFFD54F).copy(alpha = alpha),
                shape = androidx.compose.foundation.shape.CircleShape
            )
    )
}

