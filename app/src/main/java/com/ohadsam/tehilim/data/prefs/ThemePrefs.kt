package com.ohadsam.tehilim.data.prefs

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Background overrides are ARGB ints; null = use the palette's own background. */
data class ThemeSettings(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val paletteKey: String = "judaica",
    val dynamicColor: Boolean = false,
    val lightBackground: Int? = null,
    val darkBackground: Int? = null,
)

class ThemePrefs(context: Context) {
    private val prefs = context.getSharedPreferences("theme", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(load())
    val settings: StateFlow<ThemeSettings> = state.asStateFlow()

    private fun load() = ThemeSettings(
        mode = runCatching { ThemeMode.valueOf(prefs.getString("mode", "") ?: "") }.getOrDefault(ThemeMode.SYSTEM),
        paletteKey = prefs.getString("palette", "judaica") ?: "judaica",
        dynamicColor = prefs.getBoolean("dynamic", false),
        lightBackground = if (prefs.contains("bg_light")) prefs.getInt("bg_light", 0) else null,
        darkBackground = if (prefs.contains("bg_dark")) prefs.getInt("bg_dark", 0) else null,
    )

    fun update(change: (ThemeSettings) -> ThemeSettings) {
        val s = change(state.value)
        state.value = s
        prefs.edit().apply {
            putString("mode", s.mode.name)
            putString("palette", s.paletteKey)
            putBoolean("dynamic", s.dynamicColor)
            if (s.lightBackground == null) remove("bg_light") else putInt("bg_light", s.lightBackground)
            if (s.darkBackground == null) remove("bg_dark") else putInt("bg_dark", s.darkBackground)
        }.apply()
    }
}
