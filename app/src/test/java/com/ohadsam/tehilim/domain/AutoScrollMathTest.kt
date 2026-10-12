package com.ohadsam.tehilim.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoScrollMathTest {
    @Test
    fun levelsAreClampedAndScaled() {
        assertEquals(1, AutoScrollMath.clampLevel(-5))
        assertEquals(15, AutoScrollMath.clampLevel(99))
        assertEquals(32f, AutoScrollMath.pxPerSec(AutoScrollMath.DEFAULT_LEVEL))
        assertEquals(8f, AutoScrollMath.pxPerSec(1))
        assertEquals(120f, AutoScrollMath.pxPerSec(15))
    }

    @Test
    fun formatTime() {
        assertEquals("7 שנ׳", AutoScrollMath.formatTime(6.2f))
        assertEquals("2 דק׳ 05 שנ׳", AutoScrollMath.formatTime(125f))
        assertEquals("1 ש׳ 02 דק׳", AutoScrollMath.formatTime(3725f))
        assertEquals("0 שנ׳", AutoScrollMath.formatTime(-3f))
    }

    @Test
    fun progressMidwayAndAtEnd() {
        val mid = AutoScrollMath.progress(scrolledPx = 160f, totalScrollablePx = 320f, level = 4)
        assertEquals(50, mid.percent)
        assertEquals(5f, mid.remainingSeconds, 0.001f) // 160 px left at 32 px/s
        assertFalse(mid.atEnd)
        val end = AutoScrollMath.progress(320f, 320f, 4)
        assertTrue(end.atEnd)
        assertEquals(100, end.percent)
        assertEquals(100, AutoScrollMath.progress(0f, 0f, 4).percent)
    }

    @Test
    fun neverClaims100BeforeTheBottomAndCountsTheHold() {
        assertEquals(99, AutoScrollMath.progress(319f, 320f, 4).percent)
        assertEquals(7f, AutoScrollMath.progress(160f, 320f, 4, holdMs = 2000).remainingSeconds, 0.001f)
    }
}
