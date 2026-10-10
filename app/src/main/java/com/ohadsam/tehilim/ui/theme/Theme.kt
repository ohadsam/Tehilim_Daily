package com.ohadsam.tehilim.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// "Judaica" palette: parchment + navy + gold.
private val Gold = Color(0xFFB8860B)
private val GoldLight = Color(0xFFF2C14E)
private val Navy = Color(0xFF14213D)
private val Parchment = Color(0xFFFBF6EA)
private val ParchmentDim = Color(0xFFF1E9D2)

val LightColors: ColorScheme = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE3F3),
    onPrimaryContainer = Navy,
    secondary = Gold,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF7E7B5),
    onSecondaryContainer = Color(0xFF3A2A00),
    background = Parchment,
    onBackground = Color(0xFF1B1B1F),
    surface = Parchment,
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = ParchmentDim,
    onSurfaceVariant = Color(0xFF4A4637),
    outline = Color(0xFF7B7461),
    error = Color(0xFFB3261E),
)

val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFFB9C7F0),
    onPrimary = Navy,
    primaryContainer = Color(0xFF2A3A63),
    onPrimaryContainer = Color(0xFFDDE3F3),
    secondary = GoldLight,
    onSecondary = Color(0xFF3A2A00),
    secondaryContainer = Color(0xFF5A4300),
    onSecondaryContainer = Color(0xFFF7E7B5),
    background = Color(0xFF0F1420),
    onBackground = Color(0xFFE6E1D6),
    surface = Color(0xFF0F1420),
    onSurface = Color(0xFFE6E1D6),
    surfaceVariant = Color(0xFF1C2335),
    onSurfaceVariant = Color(0xFFC9C2AE),
    outline = Color(0xFF948E7C),
    error = Color(0xFFF2B8B5),
)

@Composable
fun TehilimTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = TehilimTypography,
        content = content,
    )
}
