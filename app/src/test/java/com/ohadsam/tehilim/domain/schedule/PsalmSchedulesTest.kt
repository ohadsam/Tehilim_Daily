package com.ohadsam.tehilim.domain.schedule

import com.ohadsam.tehilim.domain.PsalmRef
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek

/** Golden tests: content accuracy is critical, so every schedule must cover Tehilim exactly once. */
class PsalmSchedulesTest {
    private fun coverage(days: List<List<PsalmRef>>): Map<Int, Int> {
        // chapter → number of verses covered (whole chapter counted as -1 marker once)
        val seen = mutableMapOf<Int, MutableList<PsalmRef>>()
        days.flatten().forEach { seen.getOrPut(it.chapter) { mutableListOf() }.add(it) }
        return seen.mapValues { it.value.size }
    }

    private fun assertCoversAllOnce(days: List<List<PsalmRef>>) {
        val c = coverage(days)
        assertEquals((1..150).toList(), c.keys.sorted())
        (1..150).filter { it != 119 }.forEach { assertEquals("chapter $it", 1, c.getValue(it)) }
        val p119 = days.flatten().filter { it.chapter == 119 }
        assertEquals(listOf(PsalmRef(119, 1, 96), PsalmRef(119, 97, 176)).sortedBy { it.fromVerse }, p119.sortedBy { it.fromVerse })
    }

    @Test
    fun monthlyHasThirtyDaysAndCoversEverythingOnce() {
        assertEquals(30, PsalmSchedules.MONTHLY.size)
        assertCoversAllOnce(PsalmSchedules.MONTHLY)
    }

    @Test
    fun monthlyChaptersAreInOrder() {
        val flat = PsalmSchedules.MONTHLY.flatten().map { it.chapter }
        assertEquals(flat.sorted(), flat)
    }

    @Test
    fun weeklyHasSevenDaysAndCoversEverythingOnce() {
        assertEquals(7, PsalmSchedules.WEEKLY.size)
        val c = coverage(PsalmSchedules.WEEKLY)
        assertEquals((1..150).toList(), c.keys.sorted())
        assertEquals(true, c.values.all { it == 1 })
        assertEquals(150, PsalmSchedules.WEEKLY.sumOf { it.size })
    }

    @Test
    fun weeklyBoundaries() {
        fun r(d: DayOfWeek) = PsalmSchedules.weekly(d).let { it.first().chapter..it.last().chapter }
        assertEquals(1..29, r(DayOfWeek.SUNDAY))
        assertEquals(30..50, r(DayOfWeek.MONDAY))
        assertEquals(51..72, r(DayOfWeek.TUESDAY))
        assertEquals(73..89, r(DayOfWeek.WEDNESDAY))
        assertEquals(90..106, r(DayOfWeek.THURSDAY))
        assertEquals(107..119, r(DayOfWeek.FRIDAY))
        assertEquals(120..150, r(DayOfWeek.SATURDAY))
    }

    @Test
    fun twentyNineDayMonthMergesDayThirtyIntoTwentyNine() {
        assertEquals((140..150).toList(), PsalmSchedules.monthly(29, 29).map { it.chapter })
        assertEquals((140..144).toList(), PsalmSchedules.monthly(29, 30).map { it.chapter })
        assertEquals((145..150).toList(), PsalmSchedules.monthly(30, 30).map { it.chapter })
        assertEquals(PsalmSchedules.MONTHLY[27], PsalmSchedules.monthly(28, 29))
    }

    @Test
    fun day25And26AreTheTwoHalvesOf119() {
        assertEquals(listOf(PsalmRef(119, 1, 96)), PsalmSchedules.monthly(25, 30))
        assertEquals(listOf(PsalmRef(119, 97, 176)), PsalmSchedules.monthly(26, 30))
    }
}
