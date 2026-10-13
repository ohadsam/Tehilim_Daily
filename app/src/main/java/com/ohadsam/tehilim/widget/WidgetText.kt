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

/** Where a reading widget is: [landing] screen shown or not, and the chapter index. */
data class ReaderPos(val landingSeen: Boolean, val index: Int)

/**
 * One tap on a paging arrow. With the landing screen enabled: landing --next--> first/saved chapter; first chapter --previous-->
 * landing (the landing is shown again on a new day because the "seen" marker is per day); otherwise plain paging, clamped.
 */
fun stepReader(landingEnabled: Boolean, landingSeen: Boolean, index: Int, size: Int, step: Int): ReaderPos {
    val cur = index.coerceIn(0, (size - 1).coerceAtLeast(0))
    return when {
        landingEnabled && !landingSeen -> ReaderPos(step > 0, cur)
        landingEnabled && step < 0 && cur == 0 -> ReaderPos(false, 0)
        else -> ReaderPos(true, (cur + step).coerceIn(0, (size - 1).coerceAtLeast(0)))
    }
}
