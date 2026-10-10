package com.ohadsam.tehilim.bookmarks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarkTextTest {
    @Test
    fun defaultLabelUsesHebrewNumerals() {
        assertEquals("תהילים כ״ג · פסוק א׳", BookmarkText.defaultLabel(23, 1))
    }

    @Test
    fun shortTextIsKeptVerbatim() {
        assertEquals("מִזְמוֹר לְדָוִד", BookmarkText.snippet("  מִזְמוֹר לְדָוִד  "))
    }

    @Test
    fun longTextIsCutAtAWordBoundaryWithEllipsis() {
        val text = (1..30).joinToString(" ") { "מילה$it" }
        val s = BookmarkText.snippet(text, 40)
        assertTrue(s.endsWith("…"))
        assertTrue(s.length <= 41)
        assertTrue(text.startsWith(s.removeSuffix("…")))
        assertTrue(!s.removeSuffix("…").endsWith(" "))
    }

    @Test
    fun sortingPinnedFirstThenNewest() {
        val a = Bookmark(1, 1, 1, "a", "", pinned = false, createdAt = 10)
        val b = Bookmark(2, 1, 2, "b", "", pinned = true, createdAt = 5)
        val c = Bookmark(3, 1, 3, "c", "", pinned = false, createdAt = 20)
        assertEquals(listOf(2L, 3L, 1L), BookmarkText.sorted(listOf(a, b, c)).map { it.id })
    }

    @Test
    fun filterMatchesLabelSnippetAndNote() {
        val a = Bookmark(1, 1, 1, "הרים", "אשא עיני", note = "לשבת")
        assertEquals(1, BookmarkText.filter(listOf(a), "הרים").size)
        assertEquals(1, BookmarkText.filter(listOf(a), "עיני").size)
        assertEquals(1, BookmarkText.filter(listOf(a), "שבת").size)
        assertEquals(0, BookmarkText.filter(listOf(a), "xyz").size)
        assertEquals(1, BookmarkText.filter(listOf(a), " ").size)
    }
}
