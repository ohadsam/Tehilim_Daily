package com.ohadsam.tehilim.data.prefs

import android.content.Context

/** How long logs are kept before the automatic clean-up removes them (0 = keep until cleared manually). */
class LogPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("logs", Context.MODE_PRIVATE)

    var retentionDays: Int
        get() = prefs.getInt("retention_days", DEFAULT_DAYS)
        set(value) = prefs.edit().putInt("retention_days", value).apply()

    companion object {
        const val DEFAULT_DAYS = 7
        val OPTIONS = listOf(1, 3, 7, 14, 30, 0)
    }
}
