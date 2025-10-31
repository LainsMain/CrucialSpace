package com.crucialspace.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily

private val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF8A7AE6),
    secondary = Color(0xFF5ED1C1),
    tertiary = Color(0xFFF1C27D),
    background = Color(0xFF131314),
    surface = Color(0xFF18181A),
    surfaceVariant = Color(0xFF1E1E21),
    onBackground = Color(0xFFEDEDED),
    onSurface = Color(0xFFEDEDED),
    onPrimary = Color(0xFF101013),
)

private val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF8A7AE6),
    secondary = Color(0xFF5ED1C1),
    tertiary = Color(0xFFF1C27D),
)

@Composable
fun CrucialTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme || isSystemInDarkTheme()) DarkColors else LightColors
    val type = Typography(
        displayLarge = MaterialTheme.typography.displayLarge.copy(fontFamily = FontFamily.Serif),
        displayMedium = MaterialTheme.typography.displayMedium.copy(fontFamily = FontFamily.Serif),
        displaySmall = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Serif),
        headlineLarge = MaterialTheme.typography.headlineLarge.copy(fontFamily = FontFamily.Serif),
        headlineMedium = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif),
        headlineSmall = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
    )
    MaterialTheme(
        colorScheme = colors,
        typography = type,
        content = content
    )
}


