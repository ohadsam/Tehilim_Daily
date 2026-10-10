package com.ohadsam.tehilim.search

import com.ohadsam.tehilim.domain.PsalmChapter

data class SearchHit(val chapter: Int, val verse: Int, val text: String, val matches: List<IntRange>)

/**
 * Full-text search over locally stored chapters. Matching ignores nikud, ta'amim, sof-pasuk, maqaf and final-letter forms;
 * [SearchHit.matches] are ranges into the ORIGINAL text so the UI can highlight without altering it.
 */
object PsalmSearch {
    private val finals = mapOf('ך' to 'כ', 'ם' to 'מ', 'ן' to 'נ', 'ף' to 'פ', 'ץ' to 'צ')

    private fun isMark(c: Char) = c in '֑'..'ֽ' || c == 'ֿ' || c == 'ׁ' || c == 'ׂ' || c == 'ׄ' ||
        c == 'ׅ' || c == 'ׇ' || c == '׃'

    /** Normalised text plus, for each normalised char, its index in the original. */
    class Normalized(val text: String, val origin: IntArray)

    fun normalize(s: String): Normalized {
        val sb = StringBuilder()
        val idx = ArrayList<Int>(s.length)
        s.forEachIndexed { i, ch ->
            when {
                isMark(ch) -> Unit
                ch == '־' -> { // maqaf joins words: treat as a space
                    sb.append(' ')
                    idx += i
                }
                else -> {
                    sb.append(finals[ch] ?: ch)
                    idx += i
                }
            }
        }
        return Normalized(sb.toString(), idx.toIntArray())
    }

    /** Words of the query must all occur in the verse (any order). Empty or 1-letter queries return nothing. */
    fun search(chapters: List<PsalmChapter>, query: String, limit: Int = 300): List<SearchHit> {
        val words = normalize(query).text.split(' ').filter { it.isNotBlank() }
        if (words.isEmpty() || words.sumOf { it.length } < 2) return emptyList()
        val hits = mutableListOf<SearchHit>()
        for (ch in chapters.sortedBy { it.chapter }) {
            for (v in ch.verses) {
                val n = normalize(v.text)
                val ranges = mutableListOf<IntRange>()
                var all = true
                for (w in words) {
                    var from = 0
                    var found = false
                    while (true) {
                        val i = n.text.indexOf(w, from)
                        if (i < 0) break
                        found = true
                        ranges += (n.origin[i]..n.origin[i + w.length - 1])
                        from = i + w.length
                    }
                    if (!found) {
                        all = false
                        break
                    }
                }
                if (all) {
                    hits += SearchHit(ch.chapter, v.number, v.text, mergeRanges(ranges))
                    if (hits.size >= limit) return hits
                }
            }
        }
        return hits
    }

    /** Extends each range over trailing combining marks so the highlight covers the nikud of the last letter. */
    private fun mergeRanges(ranges: List<IntRange>): List<IntRange> =
        ranges.sortedBy { it.first }.fold(mutableListOf<IntRange>()) { acc, r ->
            val last = acc.lastOrNull()
            if (last != null && r.first <= last.last + 1) acc[acc.lastIndex] = last.first..maxOf(last.last, r.last) else acc += r
            acc
        }
}
