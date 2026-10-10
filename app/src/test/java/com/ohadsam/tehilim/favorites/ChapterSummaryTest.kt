package com.ohadsam.tehilim.favorites

import com.ohadsam.tehilim.domain.PsalmRef
import org.junit.Assert.assertEquals
import org.junit.Test

class ChapterSummaryTest {
    @Test
    fun compressesConsecutiveRuns() {
        val refs = (20..24).map { PsalmRef(it) } + PsalmRef(91)
        assertEquals("כ׳–כ״ד, צ״א", summarizeChapters(refs))
    }

    @Test
    fun pairsAreListedAndPartialsKept() {
        assertEquals("א׳, ב׳, ד׳", summarizeChapters(listOf(PsalmRef(1), PsalmRef(2), PsalmRef(4))))
        assertEquals("קי״ט (א–ח)", summarizeChapters(listOf(PsalmRef(119, 1, 8))))
        assertEquals("", summarizeChapters(emptyList()))
    }
}
