package com.ohadsam.tehilim.favorites

import com.ohadsam.tehilim.domain.HebrewNumerals
import com.ohadsam.tehilim.domain.PSALMS_COUNT
import com.ohadsam.tehilim.domain.PsalmRef

data class ParsedChapters(val refs: List<PsalmRef>, val invalidTokens: List<String>)

/**
 * Parses free-form chapter lists: "20-24, 91", "כ-כד, צא", "119:1-96". Order is kept, exact duplicates dropped.
 * Tokens that cannot be understood are reported instead of silently ignored.
 */
object ChapterSetParser {
    private val separators = Regex("[,;\\n،\\s]+")
    private val dashes = Regex("[-–—־]")

    fun parse(input: String): ParsedChapters {
        val refs = LinkedHashSet<PsalmRef>()
        val bad = mutableListOf<String>()
        input.split(separators).filter { it.isNotBlank() }.forEach { token ->
            val parsed = parseToken(token)
            if (parsed == null) bad += token else refs += parsed
        }
        return ParsedChapters(refs.toList(), bad)
    }

    private fun number(s: String): Int? = s.toIntOrNull() ?: HebrewNumerals.fromHebrew(s)

    private fun parseToken(token: String): List<PsalmRef>? {
        if (':' in token) {
            val (chapterPart, versePart) = token.split(":", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
            val chapter = number(chapterPart)?.takeIf { it in 1..PSALMS_COUNT } ?: return null
            val bounds = versePart.split(dashes).map { number(it) }
            val from = bounds.getOrNull(0) ?: return null
            val to = if (bounds.size == 1) from else bounds.getOrNull(1) ?: return null
            if (bounds.size > 2 || from < 1 || to < from) return null
            return listOf(PsalmRef(chapter, from, to))
        }
        val bounds = token.split(dashes).map { number(it) }
        if (bounds.isEmpty() || bounds.size > 2 || bounds.any { it == null }) return null
        val from = bounds[0]!!
        val to = bounds.getOrNull(1) ?: from
        if (from !in 1..PSALMS_COUNT || to !in 1..PSALMS_COUNT || to < from) return null
        return (from..to).map { PsalmRef(it) }
    }
}
