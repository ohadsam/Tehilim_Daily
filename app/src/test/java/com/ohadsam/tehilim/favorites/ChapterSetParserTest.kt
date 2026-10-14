package com.ohadsam.tehilim.favorites

import com.ohadsam.tehilim.domain.PsalmRef
import com.ohadsam.tehilim.domain.parsePsalmRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChapterSetParserTest {
    @Test
    fun parsesRangesSinglesAndVerseRanges() {
        val r = ChapterSetParser.parse("20-24, 91 119:1-96")
        assertEquals((20..24).map { PsalmRef(it) } + PsalmRef(91) + PsalmRef(119, 1, 96), r.refs)
        assertEquals(emptyList<String>(), r.invalidTokens)
    }

    @Test
    fun acceptsHebrewLettersAndKeepsOrderDroppingDuplicates() {
        val r = ChapterSetParser.parse("כ-כב קל כא")
        assertEquals(listOf(20, 21, 22, 130), r.refs.map { it.chapter })
    }

    @Test
    fun reportsInvalidTokens() {
        val r = ChapterSetParser.parse("151 5-3 abc 7 0 119:90-80")
        assertEquals(listOf(PsalmRef(7)), r.refs)
        assertEquals(listOf("151", "5-3", "abc", "0", "119:90-80"), r.invalidTokens)
    }

    @Test
    fun compactFormRoundTrips() {
        listOf(PsalmRef(1), PsalmRef(119, 97, 176), PsalmRef(150)).forEach { assertEquals(it, parsePsalmRef(it.compact)) }
        assertNull(parsePsalmRef("x"))
        assertNull(parsePsalmRef("119:5"))
        assertNull(parsePsalmRef("151"))
    }

    @Test
    fun commaSeparatedHebrewLettersWithoutSpaces() {
        val r = ChapterSetParser.parse("א,ג,כג,ל,קיח")
        assertEquals(listOf(1, 3, 23, 30, 118), r.refs.map { it.chapter })
        assertEquals(emptyList<String>(), r.invalidTokens)
        val mixed = ChapterSetParser.parse("א, ג-ה,כג , 119:1-96,קמה")
        assertEquals(listOf(1, 3, 4, 5, 23, 119, 145), mixed.refs.map { it.chapter })
    }
}
