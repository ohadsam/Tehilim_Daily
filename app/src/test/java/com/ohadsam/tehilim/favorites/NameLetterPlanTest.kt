package com.ohadsam.tehilim.favorites

import com.ohadsam.tehilim.domain.PsalmRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NameLetterPlanTest {
    @Test
    fun nameLettersInOrderThenFullNeshama() {
        val plan = NameLetterPlan.build(NameSpec(listOf("דוד"), neshama = true))
        // ד, ו (the repeated ד is shown once), then נ ש מ ה
        assertEquals(listOf('ד', 'ו', 'נ', 'ש', 'מ', 'ה').map { NameLetters.stanza(it)!! }, plan.map { it.ref })
        assertEquals(setOf(0, 2), plan[0].highlight) // both ד in "דוד"
        assertEquals(setOf(1), plan[1].highlight)
        assertEquals(NameSection.NESHAMA, plan[2].section)
        assertEquals("נשמה", plan[2].label)
        assertEquals(setOf(0), plan[2].highlight)
    }

    @Test
    fun severalNamesAreOneFullNameAndFinalLettersMap() {
        val plan = NameLetterPlan.build(NameSpec(listOf("יוסף", "חיים"), neshama = false))
        assertEquals("יוסף חיים", plan.first().label)
        // letters: י ו ס פ(ף) ח י י ם(מ): י appears at 0,6,7 — distinct stanzas only
        assertEquals(listOf('י', 'ו', 'ס', 'פ', 'ח', 'מ').map { NameLetters.stanza(it)!! }, plan.map { it.ref })
        assertEquals(setOf(0, 6, 7), plan[0].highlight)
        assertEquals(setOf(3), plan[3].highlight) // ף
    }

    @Test
    fun neshamaStaysCompleteEvenIfNameSharesLetters() {
        val plan = NameLetterPlan.build(NameSpec(listOf("נעמי"), neshama = true))
        assertEquals(2, plan.count { it.ref == NameLetters.stanza('מ') } .coerceAtLeast(2)) // מ in name and in נשמה
        assertEquals(4, plan.count { it.section == NameSection.NESHAMA })
    }

    @Test
    fun nonLettersAreIgnoredAndEmptyNameGivesOnlyNeshama() {
        assertEquals(1, NameLetterPlan.build(NameSpec(listOf("א-1"), false)).size)
        assertEquals(4, NameLetterPlan.build(NameSpec(emptyList(), true)).size)
    }

    @Test
    fun specEncodingRoundTrips() {
        val s = NameSpec(listOf("אברהם", "יצחק"), true)
        assertEquals(s, NameSpec.decode(s.encode()))
        assertEquals(NameSpec(listOf("א"), false), NameSpec.decode("א#0"))
        assertNull(NameSpec.decode("garbage"))
        assertNull(NameSpec.decode(null))
    }

    @Test
    fun refsAreWholeStanzas() {
        NameLetterPlan.refs(NameSpec(listOf("אבגד"), false)).forEach { assertEquals(8, it.toVerse!! - it.fromVerse!! + 1) }
        assertEquals(PsalmRef(119, 1, 8), NameLetterPlan.refs(NameSpec(listOf("א"), false)).single())
    }
}
