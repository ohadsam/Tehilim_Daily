package com.ohadsam.tehilim.ui.more

import android.content.Context
import org.json.JSONObject

data class WhatsNew(val version: String, val items: List<String>)

/** Reads `assets/whatsnew.json` (only the current version's bullets) and tracks the last version shown. */
class WhatsNewStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("whatsnew", Context.MODE_PRIVATE)

    fun load(): WhatsNew = runCatching {
        val text = context.assets.open("whatsnew.json").bufferedReader().use { it.readText() }
        val json = JSONObject(text)
        val arr = json.getJSONArray("items")
        WhatsNew(json.getString("version"), List(arr.length()) { arr.getString(it) })
    }.getOrDefault(WhatsNew("", emptyList()))

    fun shouldAutoShow(currentVersion: String): Boolean = prefs.getString(KEY_SEEN, null) != currentVersion

    fun markSeen(currentVersion: String) {
        prefs.edit().putString(KEY_SEEN, currentVersion).apply()
    }

    private companion object {
        const val KEY_SEEN = "seen_version"
    }
}
