package com.ohadsam.tehilim.ui.more

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
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

@androidx.compose.runtime.Composable
fun WhatsNewDialog(store: WhatsNewStore, onDismiss: () -> Unit) {
    val info = androidx.compose.runtime.remember { store.load() }
    val version = com.ohadsam.tehilim.BuildConfig.VERSION_NAME
    val close = {
        store.markSeen(version)
        onDismiss()
    }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = close,
        title = { androidx.compose.material3.Text(androidx.compose.ui.res.stringResource(com.ohadsam.tehilim.R.string.whats_new_title, info.version.ifEmpty { version })) },
        text = {
            androidx.compose.foundation.layout.Column {
                if (info.items.isEmpty()) androidx.compose.material3.Text(androidx.compose.ui.res.stringResource(com.ohadsam.tehilim.R.string.whats_new_empty))
                info.items.forEach {
                    androidx.compose.material3.Text("• $it", modifier = androidx.compose.ui.Modifier.padding(vertical = 2.dp))
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = close) {
                androidx.compose.material3.Text(androidx.compose.ui.res.stringResource(com.ohadsam.tehilim.R.string.whats_new_close))
            }
        },
    )
}
