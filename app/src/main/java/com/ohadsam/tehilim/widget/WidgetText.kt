package com.ohadsam.tehilim.widget

import com.ohadsam.tehilim.domain.PsalmRef
import com.ohadsam.tehilim.domain.shortLabel

/**
 * Where the reading widget is within the day: "current – last (n/total)", e.g. "י׳ – יז׳  (1/8)", then "יא׳ – יז׳  (2/8)" …
 * and just the chapter once the last one is reached. The numbers are digits on purpose (how many of how many).
 */
fun progressLabel(chapters: List<PsalmRef>, index: Int): String {
    if (chapters.isEmpty()) return ""
    val i = index.coerceIn(0, chapters.lastIndex)
    val current = chapters[i].shortLabel()
    val last = chapters.last().shortLabel()
    val range = if (i == chapters.lastIndex) current else "$current – $last"
    return if (chapters.size == 1) range else "$range  (${i + 1}/${chapters.size})"
}
