package com.ohadsam.tehilim.widget

import com.ohadsam.tehilim.data.prefs.ReaderSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetDisplayTest {
    private val app = ReaderSettings(fontSizeSp = 28f, showVerseNumbers = false, showNikud = false, showTaamim = true)

    @Test
    fun followAppMirrorsAppReadingSettingsScaledForTheWidget() {
        val r = WidgetDisplay.resolve(WidgetDisplayPrefs(followApp = true, fontSp = 12f, nikud = true), app)
        assertEquals(21f, r.fontSp, 0.01f)
        assertEquals(false, r.nikud)
        assertEquals(true, r.taamim)
        assertEquals(false, r.numbers)
    }

    @Test
    fun customSettingsOverrideAndAreClamped() {
        val r = WidgetDisplay.resolve(WidgetDisplayPrefs(followApp = false, fontSp = 99f, nikud = true, taamim = false, numbers = true, background = 0xFF112233.toInt()), app)
        assertEquals(WidgetDisplay.MAX_SP, r.fontSp, 0.01f)
        assertEquals(true, r.nikud)
        assertEquals(0xFF112233.toInt(), r.background)
        assertNull(r.textColor)
    }
}
