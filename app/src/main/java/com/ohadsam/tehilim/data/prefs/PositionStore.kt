package com.ohadsam.tehilim.data.prefs

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/**
 * Remembers which chapter of a day's list the user is on, per source and per date, shared by the app and the widgets so
 * navigation done in one shows up in the other. Entries older than a few days are dropped.
 */
class PositionStore(context: Context) {
    private val prefs = context.getSharedPreferences("positions", Context.MODE_PRIVATE)
    private val _version = MutableStateFlow(0)

    /** Bumped on every change so observers (widget refresh) can react. */
    val version: StateFlow<Int> = _version.asStateFlow()

    fun get(key: String): Int = prefs.getInt(key, 0)

    fun set(key: String, index: Int) {
        if (prefs.getInt(key, -1) == index) return
        val e = prefs.edit().putInt(key, index)
        val cutoff = LocalDate.now().toEpochDay() - KEEP_DAYS
        prefs.all.keys.filter { dayOf(it)?.let { d -> d < cutoff } == true }.forEach { e.remove(it) }
        e.apply()
        _version.value++
    }

    companion object {
        private const val KEEP_DAYS = 4

        /** type: "m" month-day, "w" weekday, "f" favorite, "p" person; id: row id or 0; date: the day the list is for. */
        fun key(type: String, id: Long, date: LocalDate) = "$type:$id:${date.toEpochDay()}"

        private fun dayOf(key: String): Long? = key.substringAfterLast(':', "").toLongOrNull()
    }
}
