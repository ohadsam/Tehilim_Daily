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
import kotlinx.serialization.json.jsonObject

/**
 * Turns a Sefaria v3 `texts` reply for "<commentator> on Psalms N" into verse → comments. The reply's `text` is indexed by
 * verse (index + 1); each entry is a list of comments (a plain string for Steinsaltz). Only tags are stripped; the opening
 * words Sefaria marks with `<b>` are kept between [BOLD_START] and [BOLD_END] so the UI can emphasise them.
 */
object CommentaryParser {
    const val BOLD_START = '\u0001'
    const val BOLD_END = '\u0002'

    private val json = Json { ignoreUnknownKeys = true }
    private val hebrewLetter = Regex("[א-ת]")
    private val boldOpen = Regex("<(b|strong)>", RegexOption.IGNORE_CASE)
    private val boldClose = Regex("</(b|strong)>", RegexOption.IGNORE_CASE)

    fun parse(body: String): Map<Int, List<String>> {
        val root = try {
            json.parseToJsonElement(body).jsonObject
        } catch (e: Exception) {
            throw PsalmException(PsalmErrorKind.INVALID_DATA, "commentary response is not a JSON object", e)
        }
        root["error"]?.let { throw PsalmException(PsalmErrorKind.NOT_FOUND, "Sefaria error: ${(it as? JsonPrimitive)?.contentOrNull ?: it}") }
        val versions = root["versions"] as? JsonArray ?: throw PsalmException(PsalmErrorKind.INVALID_DATA, "commentary: no versions")
        val hebrew = versions.map { it as? JsonObject }.firstOrNull { v ->
            v != null && ((v["language"] as? JsonPrimitive)?.contentOrNull == "he" || (v["actualLanguage"] as? JsonPrimitive)?.contentOrNull == "he")
        } ?: throw PsalmException(PsalmErrorKind.INVALID_DATA, "commentary: no Hebrew version")
        val text = hebrew["text"] as? JsonArray ?: return emptyMap()
        val out = linkedMapOf<Int, List<String>>()
        text.forEachIndexed { i, verse ->
            val comments = flatten(verse).map(::clean).filter { it.isNotEmpty() && hebrewLetter.containsMatchIn(it) }
            if (comments.isNotEmpty()) out[i + 1] = comments
        }
        return out
    }

    private fun flatten(e: JsonElement): List<String> = when (e) {
        is JsonArray -> e.flatMap(::flatten)
        is JsonNull -> emptyList()
        is JsonPrimitive -> listOf(e.content)
        else -> emptyList()
    }

    internal fun clean(raw: String): String {
        val marked = boldClose.replace(boldOpen.replace(raw, BOLD_START.toString()), BOLD_END.toString())
        return SefariaParser.clean(marked)
    }
}
