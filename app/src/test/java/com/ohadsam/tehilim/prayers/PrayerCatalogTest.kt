package com.ohadsam.tehilim.prayers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrayerCatalogTest {
    private val texts = listOf(
        PrayerText("a", "before", "general", "ashkenaz", "A", "טקסט א", verified = true),
        PrayerText("b", "before", "general", "sfard", "B", "טקסט ב", verified = true),
        PrayerText("c", "before", "refuah", "sfard", "C", "טקסט ג", verified = true),
        PrayerText("d", "after", "general", "ashkenaz", "D", "טקסט ד", verified = false),
        PrayerText("e", "after", "general", "ashkenaz", "E", "", verified = true),
    )

    @Test
    fun unverifiedOrEmptyTextsNeverShow() {
        assertNull(PrayerCatalog.pick(texts, "after", "general", "ashkenaz"))
        assertEquals(listOf("ashkenaz", "sfard"), PrayerCatalog.nusachim(texts))
    }

    @Test
    fun prefersAudienceThenNusach() {
        assertEquals("c", PrayerCatalog.pick(texts, "before", "refuah", "ashkenaz")?.id) // audience beats nusach
        assertEquals("b", PrayerCatalog.pick(texts, "before", "general", "sfard")?.id)
        assertEquals("a", PrayerCatalog.pick(texts, "before", "ilui", "ashkenaz")?.id) // falls back to general
    }

    @Test
    fun parsesAssetShapeAndToleratesGarbage() {
        assertEquals(1, PrayerCatalog.parse("""{"texts":[{"id":"x","kind":"before","nusach":"n","title":"t","text":"x","extra":1}]}""").size)
        assertEquals(0, PrayerCatalog.parse("garbage").size)
    }
}
