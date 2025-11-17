package com.crucialspace.app.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crucialspace.app.settings.SettingsStore

// Material 3 Expressive Color Palette - Dark Theme (Refined)
private val DarkColors: ColorScheme = darkColorScheme(
    // Primary colors - Softer, professional purple
    primary = Color(0xFF9B8FE8),           // Reduced saturation, more elegant
    onPrimary = Color(0xFF2A1B4D),
    primaryContainer = Color(0xFF4A3A7D),  // Less intense, more sophisticated
    onPrimaryContainer = Color(0xFFDDD6F3),
    
    // Secondary colors - Calming teal with personality
    secondary = Color(0xFF5FE3D1),         // Slightly softer teal
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF00574E),
    onSecondaryContainer = Color(0xFF9FFFEB),
    
    // Tertiary colors - Warm golden accents
    tertiary = Color(0xFFFFD392),
    onTertiary = Color(0xFF462A00),
    tertiaryContainer = Color(0xFF644000),
    onTertiaryContainer = Color(0xFFFFE3B8),
    
    // Error colors
    error = Color(0xFFFF8B7A),
    onError = Color(0xFF690010),
    errorContainer = Color(0xFF93001E),
    onErrorContainer = Color(0xFFFFDAD6),
    
    // Background & Surface - Increased contrast
    background = Color(0xFF0A0A0D),         // Darker background
    onBackground = Color(0xFFF0EFF3),
    surface = Color(0xFF121215),            // Darker base surface
    onSurface = Color(0xFFF0EFF3),
    
    // Surface variants with better depth
    surfaceVariant = Color(0xFF1C1C21),
    onSurfaceVariant = Color(0xFFD0C9D6),
    surfaceTint = Color(0xFF9B8FE8),
    
    // Surface containers - More distinct layering
    surfaceContainerLowest = Color(0xFF080809),
    surfaceContainerLow = Color(0xFF121215),
    surfaceContainer = Color(0xFF1A1A1F),
    surfaceContainerHigh = Color(0xFF26262D),   // More contrast
    surfaceContainerHighest = Color(0xFF32323A), // Even more distinct
    
    // Outline colors
    outline = Color(0xFF9A91A0),
    outlineVariant = Color(0xFF4A4551),
    
    // Inverse colors
    inverseSurface = Color(0xFFE8E1E6),
    inverseOnSurface = Color(0xFF322F33),
    inversePrimary = Color(0xFF6B52D6),
    
    // Scrim
    scrim = Color(0xFF000000),
)

// Material 3 Expressive Color Palette - Light Theme
private val LightColors: ColorScheme = lightColorScheme(
    // Primary colors
    primary = Color(0xFF6B52D6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE3DBFF),
    onPrimaryContainer = Color(0xFF21005E),
    
    // Secondary colors
    secondary = Color(0xFF00857A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF9FFFEB),
    onSecondaryContainer = Color(0xFF002019),
    
    // Tertiary colors
    tertiary = Color(0xFF855300),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE3B8),
    onTertiaryContainer = Color(0xFF2A1800),
    
    // Error colors
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    
    // Background & Surface
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF1D1B1E),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF1D1B1E),
    
    // Surface variants
    surfaceVariant = Color(0xFFE8E0EB),
    onSurfaceVariant = Color(0xFF4A4551),
    surfaceTint = Color(0xFF6B52D6),
    
    // Surface containers
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8F5FA),
    surfaceContainer = Color(0xFFF2EFF4),
    surfaceContainerHigh = Color(0xFFECE9EE),
    surfaceContainerHighest = Color(0xFFE6E3E8),
    
    // Outline colors
    outline = Color(0xFF7B7581),
    outlineVariant = Color(0xFFCBC4CF),
    
    // Inverse colors
    inverseSurface = Color(0xFF322F33),
    inverseOnSurface = Color(0xFFF6EFF4),
    inversePrimary = Color(0xFF9D8FFF),
    
    // Scrim
    scrim = Color(0xFF000000),
)

// Material 3 Expressive Shapes - Larger, more personality-driven corner radii
val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun CrucialTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val store = remember { SettingsStore(context) }
    
    // Use remember with a key that changes when we want to recompose
    // This will trigger recomposition when the preference changes
    val themePreference = remember { mutableStateOf(store.getThemePreference()) }
    
    // Update the state whenever this composable recomposes
    LaunchedEffect(Unit) {
        themePreference.value = store.getThemePreference()
    }
    
    // Determine color scheme based on preference and system capabilities
    val colors = when {
        // Material You Dynamic Colors (Android 12+)
        themePreference.value == "dynamic" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            dynamicDarkColorScheme(context)
        }
        // Theme presets
        themePreference.value == "dynamic" && Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> {
            // Fallback to default if dynamic not available
            ThemePresets.PurpleExpressive
        }
        // Apply selected preset
        else -> ThemePresets.getThemeByName(themePreference.value)
    }
    
    // Material 3 Expressive Typography - BOLDER, more personality
    val type = Typography(
        // Display styles - ExtraBold, maximum impact
        displayLarge = MaterialTheme.typography.displayLarge.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp, // Tighter for impact
        ),
        displayMedium = MaterialTheme.typography.displayMedium.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
            letterSpacing = (-0.3).sp,
        ),
        displaySmall = MaterialTheme.typography.displaySmall.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            letterSpacing = (-0.2).sp,
        ),
        
        // Headlines - Bold and confident
        headlineLarge = MaterialTheme.typography.headlineLarge.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            letterSpacing = (-0.2).sp,
        ),
        headlineMedium = MaterialTheme.typography.headlineMedium.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        ),
        headlineSmall = MaterialTheme.typography.headlineSmall.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        ),
        
        // Titles - Strong hierarchy
        titleLarge = MaterialTheme.typography.titleLarge.copy(
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        ),
        titleMedium = MaterialTheme.typography.titleMedium.copy(
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        ),
        titleSmall = MaterialTheme.typography.titleSmall.copy(
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        ),
        
        // Body - Comfortable reading
        bodyLarge = MaterialTheme.typography.bodyLarge.copy(
            fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
        ),
        bodyMedium = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
        ),
        bodySmall = MaterialTheme.typography.bodySmall.copy(
            fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
        ),
        
        // Labels - Clear and legible
        labelLarge = MaterialTheme.typography.labelLarge.copy(
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        ),
        labelMedium = MaterialTheme.typography.labelMedium.copy(
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        ),
        labelSmall = MaterialTheme.typography.labelSmall.copy(
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        ),
    )
    
    MaterialTheme(
        colorScheme = colors,
        typography = type,
        shapes = ExpressiveShapes,
        content = content
    )
}


