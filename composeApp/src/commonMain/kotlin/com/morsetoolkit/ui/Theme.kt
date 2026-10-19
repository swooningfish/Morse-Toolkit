package com.morsetoolkit.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFC53D),
    onPrimary = Color(0xFF231A00),
    primaryContainer = Color(0xFF3A3220),
    onPrimaryContainer = Color(0xFFFFE08A),
    secondary = Color(0xFF6CD3C5),
    onSecondary = Color(0xFF00201C),
    background = Color(0xFF111316),
    onBackground = Color(0xFFE6E6E6),
    surface = Color(0xFF111316),
    onSurface = Color(0xFFE6E6E6),
    surfaceVariant = Color(0xFF23262B),
    onSurfaceVariant = Color(0xFFB9BCC2),
    surfaceContainer = Color(0xFF1A1D21),
    outline = Color(0xFF4A4E55),
    error = Color(0xFFFF8A80),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF00796B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8EEE6),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFFB27C00),
    onSecondary = Color.White,
    background = Color(0xFFF7F8F8),
    onBackground = Color(0xFF191C1C),
    surface = Color(0xFFF7F8F8),
    onSurface = Color(0xFF191C1C),
    surfaceVariant = Color(0xFFE3E8E7),
    onSurfaceVariant = Color(0xFF3F4948),
    surfaceContainer = Color(0xFFEDF1F0),
    outline = Color(0xFF8A9493),
    error = Color(0xFFC62828),
)

/** Colours for right and wrong answers. */
data class ResultColors(val correct: Color, val wrong: Color)

val LocalResultColors = staticCompositionLocalOf { ResultColors(Color.Green, Color.Red) }

@Composable
fun MorseTheme(dark: Boolean, content: @Composable () -> Unit) {
    val results = if (dark) ResultColors(Color(0xFF7BD88F), Color(0xFFFF8A80)) else ResultColors(Color(0xFF2E7D32), Color(0xFFC62828))
    CompositionLocalProvider(LocalResultColors provides results) {
        MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
    }
}
