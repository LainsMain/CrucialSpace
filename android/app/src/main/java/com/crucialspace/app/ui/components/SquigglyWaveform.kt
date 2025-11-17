package com.crucialspace.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Material 3 Expressive - Squiggly Waveform Component
 * 
 * Draws organic, flowing wavy lines with animation support
 */

enum class SquigglyStyle {
    AUDIO_WAVEFORM,      // For voice recording visualization
    DECORATIVE_ACCENT,   // Subtle accent line
    SECTION_DIVIDER,     // Between content sections
    FLOWING_WAVE         // Animated flowing effect
}

@Composable
fun SquigglyWaveform(
    modifier: Modifier = Modifier,
    style: SquigglyStyle = SquigglyStyle.AUDIO_WAVEFORM,
    color: Color = Color.White,
    gradientColors: List<Color>? = null,
    animated: Boolean = true,
    amplitudes: List<Float>? = null, // For audio waveform
    height: Dp = 80.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "squiggly")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val width = size.width
        val centerY = size.height / 2f
        val path = Path()
        
        when (style) {
            SquigglyStyle.AUDIO_WAVEFORM -> {
                // Organic audio waveform with curves
                val bars = amplitudes?.size ?: 40
                val barWidth = width / bars
                
                for (i in 0 until bars) {
                    val amplitude = amplitudes?.getOrNull(i) ?: (0.3f + 0.4f * sin(i * 0.5f + if (animated) phase else 0f))
                    val barHeight = size.height * amplitude * (if (animated) (0.8f + 0.2f * pulsePhase) else 1f)
                    val x = i * barWidth + barWidth / 2
                    val startY = centerY - barHeight / 2
                    val endY = centerY + barHeight / 2
                    
                    // Draw organic curved bars
                    if (i == 0) {
                        path.moveTo(x, startY)
                    } else {
                        val prevX = (i - 1) * barWidth + barWidth / 2
                        val prevAmplitude = amplitudes?.getOrNull(i - 1) ?: (0.3f + 0.4f * sin((i - 1) * 0.5f + if (animated) phase else 0f))
                        val prevBarHeight = size.height * prevAmplitude * (if (animated) (0.8f + 0.2f * pulsePhase) else 1f)
                        val prevStartY = centerY - prevBarHeight / 2
                        
                        // Curve from previous point to current
                        val controlX = (prevX + x) / 2
                        path.quadraticBezierTo(controlX, prevStartY, x, startY)
                    }
                }
                
                // Draw bottom curve
                for (i in (bars - 1) downTo 0) {
                    val amplitude = amplitudes?.getOrNull(i) ?: (0.3f + 0.4f * sin(i * 0.5f + if (animated) phase else 0f))
                    val barHeight = size.height * amplitude * (if (animated) (0.8f + 0.2f * pulsePhase) else 1f)
                    val x = i * barWidth + barWidth / 2
                    val endY = centerY + barHeight / 2
                    
                    if (i == bars - 1) {
                        path.lineTo(x, endY)
                    } else {
                        val nextX = (i + 1) * barWidth + barWidth / 2
                        val controlX = (x + nextX) / 2
                        path.quadraticBezierTo(controlX, endY, x, endY)
                    }
                }
                
                path.close()
                
                if (gradientColors != null) {
                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(gradientColors)
                    )
                } else {
                    drawPath(path = path, color = color)
                }
            }
            
            SquigglyStyle.DECORATIVE_ACCENT -> {
                // Single flowing squiggly line
                val segments = 50
                val wavelength = width / 3f
                val waveAmplitude = size.height * 0.3f
                
                path.moveTo(0f, centerY)
                
                for (i in 1..segments) {
                    val x = (i.toFloat() / segments) * width
                    val wave = sin(2 * PI * x / wavelength + if (animated) phase else 0f).toFloat()
                    val y = centerY + wave * waveAmplitude
                    
                    val prevX = ((i - 1).toFloat() / segments) * width
                    val controlX = (prevX + x) / 2
                    val prevWave = sin(2 * PI * prevX / wavelength + if (animated) phase else 0f).toFloat()
                    val controlY = centerY + prevWave * waveAmplitude
                    
                    path.quadraticBezierTo(controlX, controlY, x, y)
                }
                
                if (gradientColors != null) {
                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(gradientColors),
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                } else {
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
            
            SquigglyStyle.SECTION_DIVIDER -> {
                // Subtle wavy divider
                val segments = 30
                val wavelength = width / 2f
                val waveAmplitude = size.height * 0.4f
                
                path.moveTo(0f, centerY)
                
                for (i in 1..segments) {
                    val x = (i.toFloat() / segments) * width
                    val wave = sin(2 * PI * x / wavelength).toFloat()
                    val y = centerY + wave * waveAmplitude
                    
                    val prevX = ((i - 1).toFloat() / segments) * width
                    val controlX = (prevX + x) / 2
                    val prevWave = sin(2 * PI * prevX / wavelength).toFloat()
                    val controlY = centerY + prevWave * waveAmplitude
                    
                    path.quadraticBezierTo(controlX, controlY, x, y)
                }
                
                if (gradientColors != null) {
                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(gradientColors),
                        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                    )
                } else {
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
            
            SquigglyStyle.FLOWING_WAVE -> {
                // Multiple flowing waves
                val waves = 3
                for (w in 0 until waves) {
                    val wavePath = Path()
                    val segments = 40
                    val wavelength = width / 2.5f
                    val waveAmplitude = size.height * 0.15f
                    val yOffset = centerY + (w - 1) * size.height * 0.25f
                    
                    wavePath.moveTo(0f, yOffset)
                    
                    for (i in 1..segments) {
                        val x = (i.toFloat() / segments) * width
                        val wave = sin(2 * PI * x / wavelength + if (animated) (phase + w * PI.toFloat() / 3) else 0f).toFloat()
                        val y = yOffset + wave * waveAmplitude
                        
                        val prevX = ((i - 1).toFloat() / segments) * width
                        val controlX = (prevX + x) / 2
                        val prevWave = sin(2 * PI * prevX / wavelength + if (animated) (phase + w * PI.toFloat() / 3) else 0f).toFloat()
                        val controlY = yOffset + prevWave * waveAmplitude
                        
                        wavePath.quadraticBezierTo(controlX, controlY, x, y)
                    }
                    
                    val alpha = 0.6f - w * 0.15f
                    drawPath(
                        path = wavePath,
                        color = color.copy(alpha = alpha),
                        style = Stroke(width = (3 - w).dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

/**
 * Squiggly line for underlining or accents
 */
@Composable
fun SquigglyUnderline(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFFFD54F),
    animated: Boolean = true,
    thickness: Dp = 3.dp
) {
    SquigglyWaveform(
        modifier = modifier,
        style = SquigglyStyle.DECORATIVE_ACCENT,
        color = color,
        animated = animated,
        height = 12.dp
    )
}

