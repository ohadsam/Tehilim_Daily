package com.ohadsam.tehilim.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TextDisplayTest {
    private val full = "בְּרֵאשִׁ֖ית" // bet+sheva, resh+tsere, alef, shin+hirik+shin-dot, yod+pashta(ta'am), tav
    private fun strip(s: String, from: Char, to: Char) = s.filterNot { it in from..to }

    @Test
    fun keepEverythingIsIdentity() {
        assertEquals(full, TextDisplay.apply(full, showNikud = true, showTaamim = true))
    }

    @Test
    fun removingTaamimKeepsNikud() {
        val r = TextDisplay.apply(full, showNikud = true, showTaamim = false)
        assertEquals(strip(full, '֑', '֯'), r)
        assertEquals(true, r.contains('ְ')) // sheva still there
    }

    @Test
    fun removingNikudKeepsLettersAndTaamim() {
        val r = TextDisplay.apply(full, showNikud = false, showTaamim = true)
        assertEquals(true, r.contains('֖')) // tipcha kept
        assertEquals(false, r.contains('ְ'))
        assertEquals("בראשית", TextDisplay.apply(full, showNikud = false, showTaamim = false))
    }

    @Test
    fun sofPasukAndMaqafSurvive() {
        assertEquals("אֶל־לֹא׃", TextDisplay.apply("אֶל־לֹא׃", true, false))
        assertEquals("אל־לא׃", TextDisplay.apply("אֶל־לֹא׃", false, false))
    }
}
