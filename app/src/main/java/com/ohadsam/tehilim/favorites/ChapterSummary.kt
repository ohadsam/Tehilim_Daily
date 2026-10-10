package com.ohadsam.tehilim.favorites

import com.ohadsam.tehilim.domain.HebrewNumerals
import com.ohadsam.tehilim.domain.PsalmRef
import com.ohadsam.tehilim.domain.shortLabel

/** [20,21,22,23,24,91] → "כ׳–כ״ד, צ״א"; partial-chapter refs are shown individually. */
fun summarizeChapters(refs: List<PsalmRef>): String {
    val parts = mutableListOf<String>()
    var i = 0
    while (i < refs.size) {
        val r = refs[i]
        if (!r.isWhole) {
            parts += r.shortLabel()
            i++
            continue
        }
        var j = i
        while (j + 1 < refs.size && refs[j + 1].isWhole && refs[j + 1].chapter == refs[j].chapter + 1) j++
        parts += when (j - i) {
            0 -> HebrewNumerals.toHebrew(r.chapter)
            1 -> "${HebrewNumerals.toHebrew(r.chapter)}, ${HebrewNumerals.toHebrew(refs[j].chapter)}"
            else -> "${HebrewNumerals.toHebrew(r.chapter)}–${HebrewNumerals.toHebrew(refs[j].chapter)}"
        }
        i = j + 1
    }
    return parts.joinToString(", ")
}
