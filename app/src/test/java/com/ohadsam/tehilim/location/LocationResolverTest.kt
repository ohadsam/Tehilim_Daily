package com.ohadsam.tehilim.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class LocationResolverTest {
    private val il = ZoneId.of("Asia/Jerusalem")
    private val ny = ZoneId.of("America/New_York")

    @Test
    fun manualChoiceWinsOverFixes() {
        val r = LocationResolver.resolve(LocationChoice.Manual("haifa"), GeoFix(40.0, -74.0), null, il)
        assertEquals(LocationSource.MANUAL, r.source)
        assertEquals("haifa", r.place.id)
    }

    @Test
    fun autoFallbackChain() {
        val live = GeoFix(32.084, 34.887)
        val last = GeoFix(31.77, 35.21)
        assertEquals(LocationSource.AUTO, LocationResolver.resolve(LocationChoice.Auto, live, last, il).source)
        assertEquals(LocationSource.LAST_AUTO, LocationResolver.resolve(LocationChoice.Auto, null, last, il).source)
        val d = LocationResolver.resolve(LocationChoice.Auto, null, null, il)
        assertEquals(LocationSource.DEFAULT, d.source)
        assertEquals("jerusalem", d.place.id)
    }

    @Test
    fun unknownManualIdFallsBackToAuto() {
        assertEquals(LocationSource.DEFAULT, LocationResolver.resolve(LocationChoice.Manual("nowhere"), null, null, il).source)
    }

    @Test
    fun autoNearJerusalemUsesFortyMinutesElsewhereEighteen() {
        assertEquals(40, LocationResolver.resolve(LocationChoice.Auto, GeoFix(31.78, 35.22), null, il).place.candleLightingMinutes)
        assertEquals(18, LocationResolver.resolve(LocationChoice.Auto, GeoFix(32.08, 34.78), null, il).place.candleLightingMinutes)
    }

    @Test
    fun israelDetectionNeedsBothBoxAndZone() {
        assertTrue(LocationResolver.isInIsrael(32.08, 34.78, il))
        assertFalse(LocationResolver.isInIsrael(32.08, 34.78, ny))
        assertFalse(LocationResolver.isInIsrael(40.7, -74.0, il))
    }

    @Test
    fun placesAreWellFormed() {
        assertEquals(Places.ALL.size, Places.ALL.map { it.id }.toSet().size)
        Places.ALL.forEach {
            assertTrue(it.id, it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0)
            assertTrue(it.id, runCatching { it.zone }.isSuccess)
        }
        assertTrue(Places.search("ירו").any { it.id == "jerusalem" })
    }
}
