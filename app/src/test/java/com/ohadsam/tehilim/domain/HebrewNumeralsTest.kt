package com.ohadsam.tehilim.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HebrewNumeralsTest {
    @Test
    fun formatsKnownChapterNumbers() {
        val expected = mapOf(
            1 to "א׳", 10 to "י׳", 15 to "ט״ו", 16 to "ט״ז", 20 to "כ׳", 23 to "כ״ג", 30 to "ל׳",
            100 to "ק׳", 113 to "קי״ג", 119 to "קי״ט", 130 to "ק״ל", 134 to "קל״ד", 150 to "ק״נ", 400 to "ת׳", 500 to "תק׳",
        )
        expected.forEach { (n, s) -> assertEquals("n=$n", s, HebrewNumerals.toHebrew(n)) }
    }

    @Test
    fun everyPsalmNumberRoundTrips() {
        for (n in 1..150) assertEquals(n, HebrewNumerals.fromHebrew(HebrewNumerals.toHebrew(n)))
    }

    @Test
    fun parsesUserTypedForms() {
        assertEquals(130, HebrewNumerals.fromHebrew("קל"))
        assertEquals(119, HebrewNumerals.fromHebrew("קי\"ט"))
        assertEquals(119, HebrewNumerals.fromHebrew("קי'ט"))
        assertEquals(20, HebrewNumerals.fromHebrew("ך"))
        assertNull(HebrewNumerals.fromHebrew(""))
        assertNull(HebrewNumerals.fromHebrew("abc"))
    }
}
