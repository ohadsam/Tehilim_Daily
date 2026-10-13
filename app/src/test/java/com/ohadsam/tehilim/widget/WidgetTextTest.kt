package com.ohadsam.tehilim.widget

import com.ohadsam.tehilim.domain.PsalmRef
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetTextTest {
    private val day2 = (10..17).map { PsalmRef(it) }

    @Test
    fun showsCurrentChapterToTheLastOneWithCounter() {
        assertEquals("י׳ – יז׳  (1/8)", progressLabel(day2, 0))
        assertEquals("יא׳ – יז׳  (2/8)", progressLabel(day2, 1))
        assertEquals("יז׳  (8/8)", progressLabel(day2, 7))
    }

    @Test
    fun singleChapterAndOutOfRangeIndex() {
        assertEquals("כ״ג", progressLabel(listOf(PsalmRef(23)), 0))
        assertEquals("יז׳  (8/8)", progressLabel(day2, 99))
        assertEquals("", progressLabel(emptyList(), 0))
    }
}
