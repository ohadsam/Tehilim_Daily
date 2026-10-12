package com.ohadsam.tehilim.data.prefs

import android.content.Context
import com.ohadsam.tehilim.domain.AutoScrollMath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Defaults for the reader's auto-scroll; speed changed from the on-screen toolbar is session-only (see AutoScrollController). */
data class AutoScrollSettings(
    val speed: Int = AutoScrollMath.DEFAULT_LEVEL,
    val keepAwake: Boolean = true,
    val pauseOnTouch: Boolean = true,
    val sectionDelaySec: Int = 2,
    /** At the end of a chapter keep going: next chapter of the day, then the next day. */
    val autoAdvance: Boolean = true,
)

class AutoScrollPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("autoscroll", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(load())
    val settings: StateFlow<AutoScrollSettings> = state.asStateFlow()

    private fun load() = AutoScrollSettings(
        speed = AutoScrollMath.clampLevel(prefs.getInt("speed", AutoScrollMath.DEFAULT_LEVEL)),
        keepAwake = prefs.getBoolean("keep_awake", true),
        pauseOnTouch = prefs.getBoolean("pause_on_touch", true),
        sectionDelaySec = prefs.getInt("section_delay", 2).coerceIn(0, 10),
        autoAdvance = prefs.getBoolean("auto_advance", true),
    )

    fun update(change: (AutoScrollSettings) -> AutoScrollSettings) {
        val next = change(state.value).let { it.copy(speed = AutoScrollMath.clampLevel(it.speed), sectionDelaySec = it.sectionDelaySec.coerceIn(0, 10)) }
        state.value = next
        prefs.edit().putInt("speed", next.speed).putBoolean("keep_awake", next.keepAwake).putBoolean("pause_on_touch", next.pauseOnTouch)
            .putInt("section_delay", next.sectionDelaySec).putBoolean("auto_advance", next.autoAdvance).apply()
    }
}
