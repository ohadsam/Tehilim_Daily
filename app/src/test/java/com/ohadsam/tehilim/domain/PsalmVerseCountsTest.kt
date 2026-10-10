package com.ohadsam.tehilim.domain

import com.ohadsam.tehilim.domain.schedule.PsalmSchedules
import org.junit.Assert.assertEquals
import org.junit.Test

class PsalmVerseCountsTest {
    @Test
    fun totalsMatchTheMasoreticCount() {
        assertEquals(150, PsalmVerseCounts.all.size)
        assertEquals(PsalmVerseCounts.TOTAL, PsalmVerseCounts.all.sum())
        assertEquals(2527, PsalmVerseCounts.TOTAL)
    }

    @Test
    fun knownChapters() {
        assertEquals(176, PsalmVerseCounts.of(119))
        assertEquals(2, PsalmVerseCounts.of(117))
        assertEquals(6, PsalmVerseCounts.of(23))
        assertEquals(51, PsalmVerseCounts.of(18))
    }

    @Test
    fun scheduleVerseRangesFitTheirChapters() {
        (PsalmSchedules.MONTHLY.flatten() + PsalmSchedules.WEEKLY.flatten()).forEach { ref ->
            if (!ref.isWhole) assertEquals(true, ref.toVerse!! <= PsalmVerseCounts.of(ref.chapter))
        }
        val halves = PsalmSchedules.MONTHLY.flatten().filter { it.chapter == 119 }
        assertEquals(176, halves.sumOf { it.toVerse!! - it.fromVerse!! + 1 })
    }
}
