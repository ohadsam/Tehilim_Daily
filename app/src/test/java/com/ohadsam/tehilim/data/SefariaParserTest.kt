package com.ohadsam.tehilim.data

import com.ohadsam.tehilim.data.sefaria.SefariaParser
import com.ohadsam.tehilim.domain.PsalmErrorKind
import com.ohadsam.tehilim.domain.PsalmException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SefariaParserTest {
    private fun kind(body: String): PsalmErrorKind? = try {
        SefariaParser.parseChapter(body)
        null
    } catch (e: PsalmException) {
        e.kind
    }

    @Test
    fun parsesFlatList() {
        val r = SefariaParser.parseChapter("""{"he":["אשרי האיש","לא כן הרשעים"],"length":2,"heVersionTitle":"Miqra"}""")
        assertEquals(listOf("אשרי האיש", "לא כן הרשעים"), r.verses)
        assertEquals("Miqra", r.versionTitle)
    }

    @Test
    fun parsesNestedListsAndSingleString() {
        assertEquals(3, SefariaParser.parseChapter("""{"he":[["א א","ב ב"],["ג ג"]]}""").verses.size)
        assertEquals(listOf("שלום"), SefariaParser.parseChapter("""{"he":"שלום"}""").verses)
    }

    @Test
    fun parsesV3Versions() {
        val r = SefariaParser.parseChapter("""{"versions":[{"language":"en","text":["x"]},{"language":"he","versionTitle":"V","text":["אבג"]}]}""")
        assertEquals(listOf("אבג"), r.verses)
        assertEquals("V", r.versionTitle)
    }

    @Test
    fun stripsMarkupButKeepsLetters() {
        val r = SefariaParser.parseChapter("""{"he":["<b>אשרי</b>&nbsp;האיש&thinsp;<br>טוב &amp; יפה<sup class=\"footnote-marker\">1</sup><i class=\"footnote\">note</i>"]}""")
        assertEquals("אשרי האיש טוב & יפה", r.verses.single())
    }

    @Test
    fun doesNotTurnEscapedAngleBracketsIntoTags() {
        assertEquals("א <b> ב", SefariaParser.clean("א &lt;b&gt; ב"))
        assertEquals("א &lt; ב", SefariaParser.clean("א &amp;lt; ב"))
    }

    @Test
    fun preservesNikudAndTaamim() {
        val text = "אַשְׁרֵי־הָאִ֗ישׁ"
        assertEquals(text, SefariaParser.parseChapter("""{"he":["$text"]}""").verses.single())
    }

    @Test
    fun rejectsLengthMismatch() {
        assertEquals(PsalmErrorKind.INVALID_DATA, kind("""{"he":["אב","גד"],"length":3}"""))
    }

    @Test
    fun rejectsBlankOrNonHebrewVerseAndEmptyText() {
        assertEquals(PsalmErrorKind.INVALID_DATA, kind("""{"he":["אב",""]}"""))
        assertEquals(PsalmErrorKind.INVALID_DATA, kind("""{"he":["abc"]}"""))
        assertEquals(PsalmErrorKind.INVALID_DATA, kind("""{"he":[]}"""))
        assertEquals(PsalmErrorKind.INVALID_DATA, kind("not json"))
    }

    @Test
    fun sefariaErrorFieldMeansNotFound() {
        assertEquals(PsalmErrorKind.NOT_FOUND, kind("""{"error":"No text found"}"""))
    }

    @Test
    fun validResponseHasNoKind() {
        assertTrue(kind("""{"he":["אב"],"length":1}""") == null)
        assertFalse(kind("""{"he":["אב"]}""") != null)
        try {
            SefariaParser.parseChapter("""{"he":null}""")
            fail("expected failure")
        } catch (_: PsalmException) {
        }
    }
}
