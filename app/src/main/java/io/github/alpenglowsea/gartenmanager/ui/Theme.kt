package io.github.alpenglowsea.gartenmanager.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Feste Gruen-Farben (Material Design 3). Hell und dunkel folgen der Handy-Einstellung.
private val LightColors = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB7F0B1),
    onPrimaryContainer = Color(0xFF00210A),
    secondary = Color(0xFF52634F),
    background = Color(0xFFFCFDF6),
    surface = Color(0xFFFCFDF6),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CD497),
    onPrimary = Color(0xFF003914),
    primaryContainer = Color(0xFF0F5223),
    onPrimaryContainer = Color(0xFFB7F0B1),
    secondary = Color(0xFFB9CCB4),
    background = Color(0xFF1A1C19),
    surface = Color(0xFF1A1C19),
)

@Composable
fun GartenManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
