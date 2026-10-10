package com.ohadsam.tehilim.prayers

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A "יהי רצון" style text read before/after Tehilim. Texts live in assets/prayers.json and are shown only when
 * `verified` is true — the owner of the app must confirm each wording and its source before it can appear.
 */
@Serializable
data class PrayerText(
    val id: String,
    val kind: String, // "before" | "after"
    val audience: String = "general", // "general" | "refuah" | "ilui"
    val nusach: String, // e.g. "ashkenaz", "sfard", "edot", "chabad"
    val title: String,
    val text: String,
    val source: String = "",
    val verified: Boolean = false,
)

@Serializable
data class PrayerFile(val texts: List<PrayerText> = emptyList())

object PrayerCatalog {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): List<PrayerText> = runCatching { json.decodeFromString(PrayerFile.serializer(), text).texts }.getOrDefault(emptyList())

    fun load(context: Context): List<PrayerText> =
        runCatching { context.assets.open("prayers.json").bufferedReader().use { parse(it.readText()) } }.getOrDefault(emptyList())

    fun available(all: List<PrayerText>): List<PrayerText> = all.filter { it.verified && it.text.isNotBlank() }

    fun nusachim(all: List<PrayerText>): List<String> = available(all).map { it.nusach }.distinct()

    /** Best verified text for the request: exact audience first, then the general one; chosen nusach first, then any. */
    fun pick(all: List<PrayerText>, kind: String, audience: String, nusach: String): PrayerText? {
        val pool = available(all).filter { it.kind == kind }
        return listOf(audience, "general").distinct().firstNotNullOfOrNull { a ->
            pool.firstOrNull { it.audience == a && it.nusach == nusach } ?: pool.firstOrNull { it.audience == a }
        }
    }
}
