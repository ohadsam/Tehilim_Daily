package com.ohadsam.tehilim.share

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.util.UUID

/** The lists and divisions the user created to send out ("רשימות שיתוף"), kept on the device so they can be re-sent or edited. */
class SharedListsStore(context: Context) {
    private val prefs = context.getSharedPreferences("shared_lists", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; encodeDefaults = true }
    private val serializer = ListSerializer(SharePackage.serializer())
    private val _items = MutableStateFlow(load())
    val items: StateFlow<List<SharePackage>> = _items.asStateFlow()

    private fun load(): List<SharePackage> = runCatching {
        json.decodeFromString(serializer, prefs.getString(KEY, null).orEmpty())
    }.getOrDefault(emptyList()).filter { it.validate() == null }

    /** Adds the package (new id) or replaces the one with the same id; newest first. Returns the stored package. */
    fun save(pkg: SharePackage, now: Long = System.currentTimeMillis()): SharePackage {
        val stored = pkg.copy(id = pkg.id.ifEmpty { newId() }, createdAt = if (pkg.createdAt == 0L) now else pkg.createdAt)
        val rest = _items.value.filterNot { it.id == stored.id }
        persist(listOf(stored) + rest)
        return stored
    }

    /** Used by backup restore. */
    fun replaceAll(list: List<SharePackage>) = persist(list)

    fun delete(id: String) {
        persist(_items.value.filterNot { it.id == id })
        val e = prefs.edit()
        prefs.all.keys.filter { it.startsWith("recipient.$id.") || it.startsWith("sent.$id.") }.forEach { e.remove(it) }
        e.apply()
        bump()
    }

    // Who a part is meant for and whether it was already sent: local bookkeeping for the sender, never part of the shared data.
    private val _progress = MutableStateFlow(0)

    /** Changes whenever recipients / sent marks change, so screens showing them refresh. */
    val progress: StateFlow<Int> = _progress.asStateFlow()

    private fun bump() {
        _progress.value++
    }

    fun recipient(id: String, part: Int): String = prefs.getString("recipient.$id.$part", "").orEmpty()

    fun setRecipient(id: String, part: Int, name: String) {
        prefs.edit().putString("recipient.$id.$part", name.trim().take(SharePackage.MAX_NAME)).apply()
        bump()
    }

    fun isSent(id: String, part: Int): Boolean = prefs.getBoolean("sent.$id.$part", false)

    fun setSent(id: String, part: Int, sent: Boolean) {
        prefs.edit().putBoolean("sent.$id.$part", sent).apply()
        bump()
    }

    fun resetSent(id: String) {
        val e = prefs.edit()
        prefs.all.keys.filter { it.startsWith("sent.$id.") }.forEach { e.remove(it) }
        e.apply()
        bump()
    }

    private fun persist(list: List<SharePackage>) {
        _items.value = list
        prefs.edit().putString(KEY, json.encodeToString(serializer, list)).apply()
    }

    private fun newId() = UUID.randomUUID().toString().take(8)

    private companion object {
        const val KEY = "items"
    }
}
