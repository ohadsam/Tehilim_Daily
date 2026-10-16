package com.ohadsam.tehilim.share

import com.ohadsam.tehilim.domain.PsalmRef
import com.ohadsam.tehilim.domain.PsalmVerseCounts

enum class DivisionMode {
    /** Parts of about the same number of verses (a long chapter counts for more). */
    BY_LENGTH,

    /** Parts with the same number of chapters, regardless of length. */
    BY_COUNT,
}

/**
 * Splits a list of chapters into consecutive parts for several people. Parts are always consecutive ranges (easy to read out:
 * "כ–כד"), never empty, and together contain exactly the input — nothing is dropped or repeated.
 */
object DivisionPlanner {
    private const val LONG_CHAPTER = 119

    fun verses(r: PsalmRef): Int = if (r.isWhole) PsalmVerseCounts.of(r.chapter) else r.toVerse!! - r.fromVerse!! + 1

    /** The most people the list can be divided between. */
    fun maxPeople(refs: List<PsalmRef>, mode: DivisionMode): Int = units(refs, mode).size

    /** By length, Psalm 119 (176 verses, far longer than any other) may be split at verse 96, the boundary the monthly schedule uses. */
    private fun units(refs: List<PsalmRef>, mode: DivisionMode): List<PsalmRef> =
        if (mode == DivisionMode.BY_LENGTH) {
            refs.flatMap {
                if (it.chapter == LONG_CHAPTER && it.isWhole) listOf(PsalmRef(LONG_CHAPTER, 1, 96), PsalmRef(LONG_CHAPTER, 97, 176)) else listOf(it)
            }
        } else {
            refs
        }

    fun divide(refs: List<PsalmRef>, people: Int, mode: DivisionMode): List<List<PsalmRef>> {
        val units = units(refs, mode)
        require(units.isNotEmpty()) { "nothing to divide" }
        val n = people.coerceIn(1, units.size)
        val weight = { r: PsalmRef -> if (mode == DivisionMode.BY_LENGTH) verses(r) else 1 }
        val cumulative = IntArray(units.size + 1)
        units.forEachIndexed { i, r -> cumulative[i + 1] = cumulative[i] + weight(r) }
        val total = cumulative.last()
        // cuts[k] = how many units the first k parts hold; each cut is the boundary nearest to k/n of the total weight,
        // kept strictly increasing and leaving at least one unit for every remaining part.
        val cuts = IntArray(n + 1)
        cuts[n] = units.size
        for (k in 1 until n) {
            val target = total.toDouble() * k / n
            val lo = cuts[k - 1] + 1
            val hi = units.size - (n - k)
            var best = lo
            for (c in lo..hi) {
                if (kotlin.math.abs(cumulative[c] - target) < kotlin.math.abs(cumulative[best] - target)) best = c
            }
            cuts[k] = best
        }
        return (0 until n).map { k -> mergeAdjacent(units.subList(cuts[k], cuts[k + 1])) }
    }

    /** Joins consecutive verse ranges of one chapter back together (e.g. the two halves of 119 in one part become the whole chapter). */
    fun mergeAdjacent(refs: List<PsalmRef>): List<PsalmRef> {
        val out = ArrayList<PsalmRef>()
        for (r in refs) {
            val last = out.lastOrNull()
            if (last != null && !last.isWhole && !r.isWhole && last.chapter == r.chapter && last.toVerse!! + 1 == r.fromVerse) {
                val merged = PsalmRef(r.chapter, last.fromVerse, r.toVerse)
                out[out.lastIndex] = if (merged.fromVerse == 1 && merged.toVerse == PsalmVerseCounts.of(r.chapter)) PsalmRef(r.chapter) else merged
            } else {
                out += r
            }
        }
        return out
    }

    fun versesOf(part: List<PsalmRef>): Int = part.sumOf(::verses)
}
