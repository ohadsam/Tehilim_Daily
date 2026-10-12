package com.ohadsam.tehilim.ui.reader

import com.ohadsam.tehilim.data.prefs.AutoScrollSettings
import com.ohadsam.tehilim.domain.AutoScrollMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoScrollControllerTest {
    private fun controller(s: AutoScrollSettings = AutoScrollSettings()) = AutoScrollController { s }

    @Test
    fun playPauseResumeStop() {
        val c = controller()
        assertEquals(ScrollPhase.IDLE, c.phase)
        c.start()
        assertEquals(ScrollPhase.PLAYING, c.phase)
        c.pause()
        assertEquals(ScrollPhase.PAUSED, c.phase)
        c.toggle()
        assertEquals(ScrollPhase.PLAYING, c.phase)
        c.stop()
        assertEquals(ScrollPhase.IDLE, c.phase)
        assertFalse(c.active)
    }

    @Test
    fun speedStartsAtTheSettingsDefaultAndSessionChangesAreClamped() {
        val c = controller(AutoScrollSettings(speed = 14))
        assertEquals(14, c.level)
        c.faster()
        c.faster()
        assertEquals(AutoScrollMath.MAX_LEVEL, c.level)
        repeat(30) { c.slower() }
        assertEquals(AutoScrollMath.MIN_LEVEL, c.level)
    }

    @Test
    fun touchPausesOnlyWhenEnabled() {
        val on = controller().apply { start() }
        on.onUserDrag()
        assertEquals(ScrollPhase.PAUSED, on.phase)
        val off = controller(AutoScrollSettings(pauseOnTouch = false)).apply { start() }
        off.onUserDrag()
        assertEquals(ScrollPhase.PLAYING, off.phase)
    }

    @Test
    fun endOfSectionHandsOverToTheNextOneOrEnds() {
        val c = controller().apply { start() }
        var asked = 0
        c.advance = { asked++; true }
        c.reachedEnd()
        assertEquals(1, asked)
        assertEquals(ScrollPhase.PLAYING, c.phase)
        assertNull(c.list)
        assertTrue(c.holdUntilMs > 0)

        val last = controller().apply { start(); advance = { false } }
        last.reachedEnd()
        assertEquals(ScrollPhase.ENDED, last.phase)

        val manual = controller(AutoScrollSettings(autoAdvance = false)).apply { start(); advance = { error("must not be asked") } }
        manual.reachedEnd()
        assertEquals(ScrollPhase.ENDED, manual.phase)
        manual.resume()
        assertEquals(ScrollPhase.PLAYING, manual.phase)
    }
}
