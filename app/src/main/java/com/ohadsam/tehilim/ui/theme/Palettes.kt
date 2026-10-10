package com.ohadsam.tehilim.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

class Palette(val key: String, val nameHe: String, val light: ColorScheme, val dark: ColorScheme, val swatch: Color)

private fun palette(key: String, nameHe: String, primary: Color, secondary: Color, darkPrimary: Color, darkSecondary: Color): Palette {
    val white = Color.White
    val black = Color.Black
    return Palette(
        key, nameHe,
        lightColorScheme(
            primary = primary, onPrimary = white, primaryContainer = lerp(primary, white, 0.85f), onPrimaryContainer = primary,
            secondary = secondary, onSecondary = white, secondaryContainer = lerp(secondary, white, 0.8f), onSecondaryContainer = lerp(secondary, black, 0.6f),
            background = Color(0xFFFBF6EA), onBackground = Color(0xFF1B1B1F), surface = Color(0xFFFBF6EA), onSurface = Color(0xFF1B1B1F),
            surfaceVariant = Color(0xFFF1E9D2), onSurfaceVariant = Color(0xFF4A4637), outline = Color(0xFF7B7461), error = Color(0xFFB3261E),
        ),
        darkColorScheme(
            primary = darkPrimary, onPrimary = lerp(darkPrimary, black, 0.8f), primaryContainer = lerp(darkPrimary, black, 0.65f), onPrimaryContainer = lerp(darkPrimary, white, 0.7f),
            secondary = darkSecondary, onSecondary = lerp(darkSecondary, black, 0.8f), secondaryContainer = lerp(darkSecondary, black, 0.6f), onSecondaryContainer = lerp(darkSecondary, white, 0.7f),
            background = Color(0xFF0F1420), onBackground = Color(0xFFE6E1D6), surface = Color(0xFF0F1420), onSurface = Color(0xFFE6E1D6),
            surfaceVariant = Color(0xFF1C2335), onSurfaceVariant = Color(0xFFC9C2AE), outline = Color(0xFF948E7C), error = Color(0xFFF2B8B5),
        ),
        primary,
    )
}

object Palettes {
    val ALL: List<Palette> = listOf(
        Palette("judaica", "זהב ופרגמנט", LightColors, DarkColors, Color(0xFFB8860B)),
        palette("sea", "כחול ים", Color(0xFF1F5FA6), Color(0xFF00796B), Color(0xFF9CC3F5), Color(0xFF6FD6C4)),
        palette("olive", "ירוק זית", Color(0xFF3E6B3A), Color(0xFF8D6E00), Color(0xFFA8D5A2), Color(0xFFE6C35C)),
        palette("burgundy", "בורדו", Color(0xFF7B2D3B), Color(0xFFB07A00), Color(0xFFF0A9B6), Color(0xFFF2C14E)),
        Palette(
            "contrast", "ניגודיות גבוהה",
            lightColorScheme(
                primary = Color.Black, onPrimary = Color.White, secondary = Color(0xFF6B4E00), onSecondary = Color.White,
                background = Color.White, onBackground = Color.Black, surface = Color.White, onSurface = Color.Black,
                surfaceVariant = Color(0xFFEDEDED), onSurfaceVariant = Color.Black, outline = Color.Black,
                secondaryContainer = Color(0xFFFFE9A8), onSecondaryContainer = Color.Black, primaryContainer = Color(0xFFDDDDDD), onPrimaryContainer = Color.Black,
            ),
            darkColorScheme(
                primary = Color.White, onPrimary = Color.Black, secondary = Color(0xFFFFD54F), onSecondary = Color.Black,
                background = Color.Black, onBackground = Color.White, surface = Color.Black, onSurface = Color.White,
                surfaceVariant = Color(0xFF1F1F1F), onSurfaceVariant = Color.White, outline = Color.White,
                secondaryContainer = Color(0xFF5A4300), onSecondaryContainer = Color.White, primaryContainer = Color(0xFF333333), onPrimaryContainer = Color.White,
            ),
            Color.Black,
        ),
    )

    fun byKey(key: String?): Palette = ALL.firstOrNull { it.key == key } ?: ALL.first()

    val LIGHT_BACKGROUNDS = listOf(0xFFFBF6EA, 0xFFFFFFFF, 0xFFFFF8E1, 0xFFF5F5F5, 0xFFF4ECD8, 0xFFEAF4EA).map { Color(it) }
    val DARK_BACKGROUNDS = listOf(0xFF0F1420, 0xFF000000, 0xFF121212, 0xFF1E1812, 0xFF0F1A14).map { Color(it) }
}
