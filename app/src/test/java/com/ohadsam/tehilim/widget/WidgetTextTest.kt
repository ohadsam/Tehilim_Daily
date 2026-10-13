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
}
