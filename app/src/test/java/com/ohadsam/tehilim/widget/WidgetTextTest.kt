package com.ohadsam.tehilim.widget

import com.ohadsam.tehilim.domain.PsalmRef
import com.ohadsam.tehilim.domain.shortLabel
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetTextTest {
    private val day2 = (10..17).map { PsalmRef(it) }
    private fun l(ch: Int) = PsalmRef(ch).shortLabel()

    @Test
    fun showsCurrentChapterToTheLastOneWithCounter() {
        assertEquals("${l(10)} – ${l(17)}  (1/8)", progressLabel(day2, 0))
        assertEquals("${l(11)} – ${l(17)}  (2/8)", progressLabel(day2, 1))
        assertEquals("${l(17)}  (8/8)", progressLabel(day2, 7))
    }

    @Test
    fun singleChapterAndOutOfRangeIndex() {
        assertEquals(l(23), progressLabel(listOf(PsalmRef(23)), 0))
        assertEquals("${l(17)}  (8/8)", progressLabel(day2, 99))
        assertEquals("", progressLabel(emptyList(), 0))
    }

    @Test
    fun landingScreenFlow() {
        // landing → next starts reading at the saved place; previous on the landing does nothing
        assertEquals(ReaderPos(true, 0), stepReader(true, false, 0, 8, 1))
        assertEquals(ReaderPos(true, 3), stepReader(true, false, 3, 8, 1))
        assertEquals(ReaderPos(false, 2), stepReader(true, false, 2, 8, -1))
        // reading: normal paging, previous on the first chapter returns to the landing
        assertEquals(ReaderPos(true, 1), stepReader(true, true, 0, 8, 1))
        assertEquals(ReaderPos(false, 0), stepReader(true, true, 0, 8, -1))
        assertEquals(ReaderPos(true, 7), stepReader(true, true, 7, 8, 1))
        // landing disabled: plain clamped paging, never a landing
        assertEquals(ReaderPos(true, 0), stepReader(false, false, 0, 8, -1))
        assertEquals(ReaderPos(true, 1), stepReader(false, false, 0, 8, 1))
    }
}
