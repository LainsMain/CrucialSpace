package com.crucialspace.app.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive Motion System
 * 
 * Provides spring-based animations and expressive motion patterns for a fluid, natural feel.
 */
object ExpressiveMotion {
    
    // Duration scales for consistent timing
    object Duration {
        const val Fast = 100
        const val Normal = 300
        const val Slow = 500
        const val ExtraSlow = 700
    }
    
    // Spring configurations for natural, bouncy motion
    object SpringConfig {
        /**
         * High bounce - Playful, expressive interactions
         */
        fun <T> highBounce() = spring<T>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
        
        /**
         * Medium bounce - Balanced, smooth interactions
         */
        fun <T> mediumBounce() = spring<T>(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        )
        
        /**
         * Low bounce - Subtle, refined interactions
         */
        fun <T> lowBounce() = spring<T>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
        
        /**
         * Quick spring - Fast, responsive interactions
         */
        fun <T> quick() = spring<T>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        )
        
        /**
         * Smooth spring - Elegant, flowing interactions
         */
        fun <T> smooth() = spring<T>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        )
    }
    
    // Tween configurations for linear/eased animations
    object TweenConfig {
        fun <T> fast() = tween<T>(
            durationMillis = Duration.Fast,
            easing = FastOutSlowInEasing
        )
        
        fun <T> normal() = tween<T>(
            durationMillis = Duration.Normal,
            easing = FastOutSlowInEasing
        )
        
        fun <T> slow() = tween<T>(
            durationMillis = Duration.Slow,
            easing = FastOutSlowInEasing
        )
        
        fun <T> emphasized() = tween<T>(
            durationMillis = Duration.Normal,
            easing = EaseInOut
        )
        
        fun <T> decelerate() = tween<T>(
            durationMillis = Duration.Normal,
            easing = EaseOut
        )
    }
    
    // Common animation specs for different use cases
    object Specs {
        /**
         * For button presses and taps
         */
        fun <T> pressAnimation(): AnimationSpec<T> = SpringConfig.quick()
        
        /**
         * For card and component entrance
         */
        fun <T> enterAnimation(): AnimationSpec<T> = SpringConfig.mediumBounce()
        
        /**
         * For card and component exit
         */
        fun <T> exitAnimation(): AnimationSpec<T> = TweenConfig.fast()
        
        /**
         * For smooth page transitions
         */
        fun <T> transitionAnimation(): AnimationSpec<T> = SpringConfig.smooth()
        
        /**
         * For hero animations and emphasis
         */
        fun <T> heroAnimation(): AnimationSpec<T> = SpringConfig.lowBounce()
        
        /**
         * For FAB and prominent actions
         */
        fun <T> fabAnimation(): AnimationSpec<T> = SpringConfig.highBounce()
    }
}

/**
 * Elevation tokens for consistent layering
 */
object Elevation {
    val Level0 = 0.dp
    val Level1 = 1.dp
    val Level2 = 3.dp
    val Level3 = 6.dp
    val Level4 = 8.dp
    val Level5 = 12.dp
}

