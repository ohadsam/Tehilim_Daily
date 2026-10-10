package com.ohadsam.tehilim.search

import com.ohadsam.tehilim.domain.PsalmChapter
import com.ohadsam.tehilim.domain.Verse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PsalmSearchTest {
    private val ps1 = PsalmChapter(
        1,
        listOf(
            Verse(1, "אַ֥שְֽׁרֵי־הָאִ֗ישׁ אֲשֶׁ֤ר לֹ֪א הָלַךְ֮ בַּעֲצַ֪ת רְשָׁעִ֡ים"),
            Verse(2, "כִּ֤י אִ֥ם בְּתוֹרַ֣ת יְהוָ֣ה חֶפְצ֑וֹ"),
        ),
        null, 0,
    )
    private val ps23 = PsalmChapter(23, listOf(Verse(1, "מִזְמ֗וֹר לְדָוִ֥ד יְהוָ֥ה רֹ֝עִ֗י לֹ֣א אֶחְסָֽר׃")), null, 0)

    @Test
    fun ignoresNikudTaamimAndFinalForms() {
        val hits = PsalmSearch.search(listOf(ps23, ps1), "לא אחסר")
        assertEquals(listOf(23 to 1), hits.map { it.chapter to it.verse })
        assertEquals(listOf(1 to 1), PsalmSearch.search(listOf(ps1), "הלך").map { it.chapter to it.verse }) // final ך vs כ
        assertEquals(listOf(1 to 1), PsalmSearch.search(listOf(ps1), "הלכ").map { it.chapter to it.verse })
    }

    @Test
    fun maqafActsAsWordSeparator() {
        assertEquals(1, PsalmSearch.search(listOf(ps1), "אשרי האיש").size)
    }

    @Test
    fun highlightRangesPointIntoTheOriginalText() {
        val hit = PsalmSearch.search(listOf(ps23), "רעי").single()
        val shown = hit.matches.map { hit.text.substring(it) }
        assertEquals(1, shown.size)
        assertTrue(PsalmSearch.normalize(shown[0]).text == "רעי")
    }

    @Test
    fun allWordsMustMatchAndTinyQueriesReturnNothing() {
        assertTrue(PsalmSearch.search(listOf(ps1, ps23), "לא נשמע").isEmpty())
        assertTrue(PsalmSearch.search(listOf(ps1), "א").isEmpty())
        assertTrue(PsalmSearch.search(listOf(ps1), "  ").isEmpty())
    }

    @Test
    fun resultsAreOrderedByChapterAndLimited() {
        val many = (1..10).map { PsalmChapter(it, listOf(Verse(1, "יהוה אלהי")), null, 0) }
        assertEquals((1..10).toList(), PsalmSearch.search(many.reversed(), "יהוה").map { it.chapter })
        assertEquals(3, PsalmSearch.search(many, "יהוה", limit = 3).size)
    }
}
