package com.ohadsam.tehilim.data.prefs

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ReaderSettings(
    val fontSizeSp: Float = DEFAULT_SIZE,
    val showVerseNumbers: Boolean = true,
) {
    companion object {
        const val MIN_SIZE = 14f
        const val MAX_SIZE = 48f
        const val DEFAULT_SIZE = 22f
        const val STEP = 2f
    }
}

/** Reader display preferences, observable. (The full settings screen arrives in batch 9.) */
class ReaderPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("reader", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(load())
    val settings: StateFlow<ReaderSettings> = state.asStateFlow()

    private fun load() = ReaderSettings(
        fontSizeSp = prefs.getFloat(KEY_SIZE, ReaderSettings.DEFAULT_SIZE).coerceIn(ReaderSettings.MIN_SIZE, ReaderSettings.MAX_SIZE),
        showVerseNumbers = prefs.getBoolean(KEY_NUMBERS, true),
    )

    fun update(change: (ReaderSettings) -> ReaderSettings) {
        val next = change(state.value).let { it.copy(fontSizeSp = it.fontSizeSp.coerceIn(ReaderSettings.MIN_SIZE, ReaderSettings.MAX_SIZE)) }
        state.value = next
        prefs.edit().putFloat(KEY_SIZE, next.fontSizeSp).putBoolean(KEY_NUMBERS, next.showVerseNumbers).apply()
    }

    /** Where the user last read in Browse, for the "continue where I left off" shortcut (0 = none). */
    var lastChapter: Int
        get() = prefs.getInt(KEY_LAST_CHAPTER, 0)
        set(value) = prefs.edit().putInt(KEY_LAST_CHAPTER, value).apply()

    private companion object {
        const val KEY_LAST_CHAPTER = "last_chapter"
        const val KEY_SIZE = "font_size_sp"
        const val KEY_NUMBERS = "verse_numbers"
    }
}
