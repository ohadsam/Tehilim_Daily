package com.ohadsam.tehilim.data.prefs

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PrayerSettings(val showBefore: Boolean = false, val showAfter: Boolean = false, val nusach: String = "ashkenaz")

class PrayerPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("prayers", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(
        PrayerSettings(prefs.getBoolean("before", false), prefs.getBoolean("after", false), prefs.getString("nusach", "ashkenaz") ?: "ashkenaz"),
    )
    val settings: StateFlow<PrayerSettings> = state.asStateFlow()

    fun update(change: (PrayerSettings) -> PrayerSettings) {
        val s = change(state.value)
        state.value = s
        prefs.edit().putBoolean("before", s.showBefore).putBoolean("after", s.showAfter).putString("nusach", s.nusach).apply()
    }
}
