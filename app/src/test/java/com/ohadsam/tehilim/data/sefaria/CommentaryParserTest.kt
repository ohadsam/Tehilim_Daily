package com.ohadsam.tehilim.data.sefaria

import com.ohadsam.tehilim.domain.PsalmErrorKind
import com.ohadsam.tehilim.domain.PsalmException
import com.ohadsam.tehilim.ui.reader.commentaryAnnotated
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CommentaryParserTest {
    private val b = CommentaryParser.BOLD_START
    private val e = CommentaryParser.BOLD_END

    private fun body(text: String, language: String = "he") =
        """{"versions":[{"versionTitle":"x","language":"$language","text":$text}]}"""

    @Test
    fun commentsAreAlignedToVerseNumbersAndEmptyVersesAreSkipped() {
        val r = CommentaryParser.parse(body("""[["<b>אשרי האיש.</b> פירוש א","עוד"],[],["פסוק ג"]]"""))
        assertEquals(setOf(1, 3), r.keys)
        assertEquals(listOf("${b}אשרי האיש.$e פירוש א", "עוד"), r.getValue(1))
        assertEquals(listOf("פסוק ג"), r.getValue(3))
    }

    @Test
    fun flatTextPerVerseSteinsaltzShapeAndEntitiesAndTags() {
        val r = CommentaryParser.parse(body("""["<b>אַשְׁרֵי</b>, מאושר&nbsp;<i>x</i>&amp;","שני"]"""))
        assertEquals("${b}אַשְׁרֵי$e, מאושר x&", r.getValue(1).single())
        assertEquals(listOf("שני"), r.getValue(2))
    }

    @Test
    fun nonHebrewCommentsAreDropped() {
        assertTrue(CommentaryParser.parse(body("""[["english only"],[]]""")).isEmpty())
    }

    @Test
    fun wrongShapesAreInvalidOrNotFound() {
        for ((input, kind) in listOf(
            "not json" to PsalmErrorKind.INVALID_DATA,
            """{"error":"Could not find title"}""" to PsalmErrorKind.NOT_FOUND,
            """{"versions":[]}""" to PsalmErrorKind.INVALID_DATA,
            body("""[["אבג"]]""", language = "en") to PsalmErrorKind.INVALID_DATA,
        )) {
            try {
                CommentaryParser.parse(input)
                fail("expected failure for $input")
            } catch (ex: PsalmException) {
                assertEquals(kind, ex.kind)
            }
        }
    }

    @Test
    fun annotatedTextBoldsTheMarkedOpeningWordsOnly() {
        val a = commentaryAnnotated("${b}אשרי.$e פירוש")
        assertEquals("אשרי. פירוש", a.text)
        assertEquals(1, a.spanStyles.size)
        assertEquals(0, a.spanStyles[0].start)
        assertEquals(5, a.spanStyles[0].end)
    }
}
