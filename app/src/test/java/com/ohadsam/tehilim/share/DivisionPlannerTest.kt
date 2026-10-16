package com.ohadsam.tehilim.share

import com.ohadsam.tehilim.domain.PSALMS_COUNT
import com.ohadsam.tehilim.domain.PsalmRef
import com.ohadsam.tehilim.domain.PsalmVerseCounts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DivisionPlannerTest {
    private val all = (1..PSALMS_COUNT).map { PsalmRef(it) }

    /** Every (chapter, verse) a list of refs covers, in order. */
    private fun versesOf(refs: List<PsalmRef>): List<Pair<Int, Int>> = refs.flatMap { r ->
        val from = r.fromVerse ?: 1
        val to = r.toVerse ?: PsalmVerseCounts.of(r.chapter)
        (from..to).map { r.chapter to it }
    }

    private val everyVerse = versesOf(all)

    @Test
    fun everyDivisionCoversTheWholeTextExactlyOnceInOrder() {
        for (mode in DivisionMode.entries) {
            for (n in listOf(1, 2, 3, 7, 10, 30, 100, 150)) {
                val parts = DivisionPlanner.divide(all, n, mode)
                assertEquals("$mode/$n parts", n, parts.size)
                assertTrue("$mode/$n has an empty part", parts.all { it.isNotEmpty() })
                assertEquals("$mode/$n coverage", everyVerse, versesOf(parts.flatten()))
            }
        }
    }

    @Test
    fun byLengthBalancesVerses() {
        val sums = DivisionPlanner.divide(all, 3, DivisionMode.BY_LENGTH).map(DivisionPlanner::versesOf)
        assertEquals(listOf(852, 843, 832), sums)
        val thirty = DivisionPlanner.divide(all, 30, DivisionMode.BY_LENGTH).map(DivisionPlanner::versesOf)
        assertEquals(PsalmVerseCounts.TOTAL, thirty.sum())
        assertTrue("largest part ${thirty.max()}", thirty.max() <= 130)
    }

    @Test
    fun byCountGivesEqualChapterCounts() {
        val parts = DivisionPlanner.divide(all, 30, DivisionMode.BY_COUNT)
        assertTrue(parts.all { it.size == 5 })
        val seven = DivisionPlanner.divide(all, 7, DivisionMode.BY_COUNT).map { it.size }
        assertEquals(150, seven.sum())
        assertTrue(seven.max() - seven.min() <= 1)
    }

    @Test
    fun psalm119IsSplitOnlyWhenNeededAndJoinedBackWhenTogether() {
        assertEquals(listOf(listOf(PsalmRef(119))), DivisionPlanner.divide(listOf(PsalmRef(119)), 1, DivisionMode.BY_LENGTH))
        assertEquals(
            listOf(listOf(PsalmRef(119, 1, 96)), listOf(PsalmRef(119, 97, 176))),
            DivisionPlanner.divide(listOf(PsalmRef(119)), 2, DivisionMode.BY_LENGTH),
        )
        // by count a chapter is never split
        assertEquals(1, DivisionPlanner.maxPeople(listOf(PsalmRef(119)), DivisionMode.BY_COUNT))
    }

    @Test
    fun maxPeopleAndClamping() {
        assertEquals(151, DivisionPlanner.maxPeople(all, DivisionMode.BY_LENGTH))
        assertEquals(150, DivisionPlanner.maxPeople(all, DivisionMode.BY_COUNT))
        val few = listOf(PsalmRef(1), PsalmRef(2), PsalmRef(3))
        assertEquals(3, DivisionPlanner.divide(few, 50, DivisionMode.BY_COUNT).size)
        assertEquals(1, DivisionPlanner.divide(few, 0, DivisionMode.BY_COUNT).size)
    }

    @Test
    fun keepsPartialRefsAndChapterOrderOfTheInput() {
        val input = listOf(PsalmRef(91), PsalmRef(20), PsalmRef(119, 1, 96), PsalmRef(3))
        val parts = DivisionPlanner.divide(input, 2, DivisionMode.BY_LENGTH)
        assertEquals(input, parts.flatten())
    }
}
