package com.ohadsam.tehilim.domain

import com.ohadsam.tehilim.calendar.HebrewDate
import org.junit.Assert.assertEquals
import org.junit.Test

class RefLabelsTest {
    @Test
    fun chapterLabels() {
        assertEquals("כ״ג", PsalmRef(23).shortLabel())
        assertEquals("קי״ט (א–צו)", PsalmRef(119, 1, 96).shortLabel())
        assertEquals("קי״ט (צז–קעו)", PsalmRef(119, 97, 176).shortLabel())
        assertEquals("פרק קי״ט · פסוקים א׳–צ״ו", PsalmRef(119, 1, 96).title())
        assertEquals("פרק א׳", PsalmRef(1).title())
    }

    @Test
    fun monthNavigationCrossesYearBoundaries() {
        // Elul 5786 → Tishrei 5787 (year number increments), and back.
        assertEquals(HebrewDate(5787, 7, 1), HebrewDate(5786, 6, 29).firstOfNextMonth())
        assertEquals(HebrewDate(5786, 6, 1), HebrewDate(5787, 7, 5).firstOfPreviousMonth())
        // Tishrei → Cheshvan is the same year number; Shevat → Adar I in a leap year.
        assertEquals(HebrewDate(5787, 8, 1), HebrewDate(5787, 7, 10).firstOfNextMonth())
        assertEquals(HebrewDate(5784, 12, 1), HebrewDate(5784, 11, 3).firstOfNextMonth())
        // Adar II → Nisan in a leap year, and back.
        assertEquals(HebrewDate(5784, 1, 1), HebrewDate(5784, 13, 9).firstOfNextMonth())
        assertEquals(HebrewDate(5784, 13, 1), HebrewDate(5784, 1, 9).firstOfPreviousMonth())
        // 30 Shevat → Adar clamps to 29 in a non-leap year.
        assertEquals(HebrewDate(5785, 12, 29), HebrewDate(5785, 11, 30).sameDayInAdjacentMonth(1))
    }
}
