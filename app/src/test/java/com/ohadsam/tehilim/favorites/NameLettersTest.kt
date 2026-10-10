package com.ohadsam.tehilim.favorites

import com.ohadsam.tehilim.domain.PsalmRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NameLettersTest {
    @Test
    fun stanzaBoundaries() {
        assertEquals(PsalmRef(119, 1, 8), NameLetters.stanza('א'))
        assertEquals(PsalmRef(119, 9, 16), NameLetters.stanza('ב'))
        assertEquals(PsalmRef(119, 169, 176), NameLetters.stanza('ת'))
        assertNull(NameLetters.stanza('a'))
    }

    @Test
    fun finalLettersMapToRegularOnes() {
        assertEquals(NameLetters.stanza('מ'), NameLetters.stanza('ם'))
        assertEquals(NameLetters.stanza('צ'), NameLetters.stanza('ץ'))
    }

    @Test
    fun neshamaIsFourStanzasInNameOrder() {
        val s = NameLetters.stanzasFor(NameLetters.NESHAMA)
        assertEquals(listOf('נ', 'ש', 'מ', 'ה').map { NameLetters.stanza(it) }, s)
    }

    @Test
    fun repeatedLettersAndNonLettersAreDropped() {
        assertEquals(2, NameLetters.stanzasFor("אבא-1 א").size)
    }

    @Test
    fun alphabetCoversAll176VersesExactlyOnce() {
        val all = "אבגדהוזחטיכלמנסעפצקרשת".map { NameLetters.stanza(it)!! }
        assertEquals((1..176).toList(), all.flatMap { (it.fromVerse!!..it.toVerse!!).toList() })
    }
}
