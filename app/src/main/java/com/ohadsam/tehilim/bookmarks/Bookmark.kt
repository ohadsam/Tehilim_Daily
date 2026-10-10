package com.ohadsam.tehilim.bookmarks

import com.ohadsam.tehilim.domain.HebrewNumerals

data class Bookmark(
    val id: Long = 0,
    val chapter: Int,
    val verse: Int,
    val label: String,
    /** First words of the verse, copied from the locally stored text at creation time. */
    val snippet: String,
    val colorIndex: Int = 0,
    val note: String? = null,
    val pinned: Boolean = false,
    val createdAt: Long = 0,
)

object BookmarkText {
    private const val SNIPPET_MAX = 70

    fun defaultLabel(chapter: Int, verse: Int): String =
        "תהילים ${HebrewNumerals.toHebrew(chapter)} · פסוק ${HebrewNumerals.toHebrew(verse)}"

    /** Cuts at a word boundary and appends an ellipsis; the characters themselves are never altered. */
    fun snippet(text: String, max: Int = SNIPPET_MAX): String {
        val t = text.trim()
        if (t.length <= max) return t
        val cut = t.lastIndexOf(' ', max).takeIf { it > max / 2 } ?: max
        return t.substring(0, cut).trimEnd() + "…"
    }

    /** Pinned first, then newest first. */
    fun sorted(items: List<Bookmark>): List<Bookmark> = items.sortedWith(compareByDescending<Bookmark> { it.pinned }.thenByDescending { it.createdAt })

    fun filter(items: List<Bookmark>, query: String): List<Bookmark> {
        val q = query.trim()
        return if (q.isEmpty()) items else items.filter { it.label.contains(q) || it.snippet.contains(q) || (it.note?.contains(q) == true) }
    }
}
