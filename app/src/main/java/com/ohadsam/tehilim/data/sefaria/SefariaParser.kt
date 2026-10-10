package com.ohadsam.tehilim.data.sefaria

import com.ohadsam.tehilim.domain.PsalmErrorKind
import com.ohadsam.tehilim.domain.PsalmException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject

data class RemoteChapter(val verses: List<String>, val versionTitle: String?)

/**
 * Turns a Sefaria `texts` API response into verses and enforces the integrity rules in docs/SPEC.md A3.
 * Shapes handled (see .claude/skills/sefaria-fetch): `he` as string / flat list / nested lists, and v3 `versions[]`.
 * The text itself is never altered beyond tag stripping, entity decoding and whitespace normalisation.
 */
object SefariaParser {
    private val json = Json { ignoreUnknownKeys = true }
    private val hebrewLetter = Regex("[א-ת]")
    private val tag = Regex("<[^>]*>")
    private val footnoteMarker = Regex("<sup[^>]*footnote-marker[^>]*>.*?</sup>", RegexOption.DOT_MATCHES_ALL)
    private val footnoteBody = Regex("<i[^>]*class=\"footnote\"[^>]*>.*?</i>", RegexOption.DOT_MATCHES_ALL)
    private val lineBreak = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
    private val numericEntity = Regex("&#(x?)([0-9a-fA-F]+);")
    private val spaces = Regex("[\\s    ]+")

    fun parseChapter(body: String): RemoteChapter {
        val root = try {
            json.parseToJsonElement(body).jsonObject
        } catch (e: Exception) {
            throw invalid("response is not a JSON object", e)
        }
        root["error"]?.let { throw PsalmException(PsalmErrorKind.NOT_FOUND, "Sefaria error: ${it.asText()}") }

        val (rawHebrew, versionTitle) = hebrewSource(root)
        val verses = flatten(rawHebrew).map(::clean)
        if (verses.isEmpty()) throw invalid("no Hebrew text")
        verses.forEachIndexed { i, v ->
            if (v.isEmpty() || !hebrewLetter.containsMatchIn(v)) throw invalid("verse ${i + 1} is empty or has no Hebrew letters")
        }
        val expected = root["length"].asIntOrNull()
        if (expected != null && expected != verses.size) throw invalid("verse count ${verses.size} != Sefaria length $expected")
        return RemoteChapter(verses, versionTitle)
    }

    private fun hebrewSource(root: JsonObject): Pair<JsonElement, String?> {
        val he = root["he"]
        if (he != null && he !is JsonNull && !(he is JsonArray && he.isEmpty())) {
            return he to (root["heVersionTitle"] as? JsonPrimitive)?.contentOrNull
        }
        val versions = root["versions"] as? JsonArray
        val hebrew = versions?.map { it.jsonObject }?.firstOrNull {
            (it["actualLanguage"] as? JsonPrimitive)?.contentOrNull == "he" || (it["language"] as? JsonPrimitive)?.contentOrNull == "he"
        }
        val text = hebrew?.get("text") ?: throw invalid("no Hebrew text")
        return text to (hebrew["versionTitle"] as? JsonPrimitive)?.contentOrNull
    }

    private fun flatten(e: JsonElement): List<String> = when (e) {
        is JsonArray -> e.flatMap(::flatten)
        is JsonNull -> listOf("")
        is JsonPrimitive -> listOf(e.content)
        else -> throw invalid("unexpected text shape")
    }

    private fun JsonElement?.asIntOrNull(): Int? = (this as? JsonPrimitive)?.intOrNull

    private fun JsonElement.asText(): String = (this as? JsonPrimitive)?.contentOrNull ?: toString()

    fun clean(raw: String): String {
        var s = raw
        s = footnoteMarker.replace(s, "")
        s = footnoteBody.replace(s, "")
        s = lineBreak.replace(s, " ")
        s = tag.replace(s, "")
        s = decodeEntities(s)
        return spaces.replace(s, " ").trim()
    }

    private fun decodeEntities(s: String): String {
        var out = numericEntity.replace(s) { m ->
            val code = m.groupValues[2].toIntOrNull(if (m.groupValues[1].isEmpty()) 10 else 16)
            if (code == null || code !in 1..0x10FFFF) m.value else String(Character.toChars(code))
        }
        // &amp; last so that "&amp;lt;" does not become "<".
        for ((entity, ch) in NAMED) out = out.replace(entity, ch)
        return out.replace("&amp;", "&")
    }

    private val NAMED = listOf(
        "&nbsp;" to " ", "&thinsp;" to " ", "&ensp;" to " ", "&emsp;" to " ",
        "&lt;" to "<", "&gt;" to ">", "&quot;" to "\"", "&apos;" to "'",
        "&mdash;" to "—", "&ndash;" to "–", "&hellip;" to "…",
    )

    private fun invalid(msg: String, cause: Throwable? = null) = PsalmException(PsalmErrorKind.INVALID_DATA, msg, cause)
}
