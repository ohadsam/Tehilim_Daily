package com.ohadsam.tehilim.location

import java.time.ZoneId

data class GeoFix(val latitude: Double, val longitude: Double, val elevationMeters: Double = 0.0, val timeMillis: Long = 0L)

/** What the user chose in Settings → מיקום. */
sealed interface LocationChoice {
    /** Use the device location (needs permission); falls back to the last good fix, then Jerusalem. */
    data object Auto : LocationChoice

    data class Manual(val placeId: String) : LocationChoice

    data class Custom(val name: String, val latitude: Double, val longitude: Double, val elevationMeters: Double) : LocationChoice
}

enum class LocationSource { MANUAL, CUSTOM, AUTO, LAST_AUTO, DEFAULT }

data class ResolvedLocation(val place: GeoPlace, val source: LocationSource)

/** Pure resolution chain: explicit choice → live auto fix → last good auto fix → Jerusalem. */
object LocationResolver {
    private const val NEAR_JERUSALEM_KM = 15.0
    private const val JERUSALEM_CANDLES = 40
    private const val DEFAULT_CANDLES = 18
    private val israelZones = setOf("Asia/Jerusalem", "Asia/Tel_Aviv", "Israel")

    fun resolve(
        choice: LocationChoice,
        liveFix: GeoFix?,
        lastAutoFix: GeoFix?,
        deviceZone: ZoneId = ZoneId.systemDefault(),
    ): ResolvedLocation = when (choice) {
        is LocationChoice.Manual -> Places.byId(choice.placeId)?.let { ResolvedLocation(it, LocationSource.MANUAL) }
            ?: autoOrDefault(liveFix, lastAutoFix, deviceZone)
        is LocationChoice.Custom -> ResolvedLocation(customPlace(choice, deviceZone), LocationSource.CUSTOM)
        LocationChoice.Auto -> autoOrDefault(liveFix, lastAutoFix, deviceZone)
    }

    private fun autoOrDefault(live: GeoFix?, last: GeoFix?, zone: ZoneId): ResolvedLocation = when {
        live != null -> ResolvedLocation(placeFromFix(live, zone), LocationSource.AUTO)
        last != null -> ResolvedLocation(placeFromFix(last, zone), LocationSource.LAST_AUTO)
        else -> ResolvedLocation(Places.JERUSALEM, LocationSource.DEFAULT)
    }

    private fun placeFromFix(fix: GeoFix, zone: ZoneId): GeoPlace {
        val nearest = Places.ALL.minBy { Places.distanceKm(fix.latitude, fix.longitude, it.latitude, it.longitude) }
        val km = Places.distanceKm(fix.latitude, fix.longitude, nearest.latitude, nearest.longitude)
        val inIsrael = isInIsrael(fix.latitude, fix.longitude, zone)
        val nearJerusalem = Places.distanceKm(fix.latitude, fix.longitude, Places.JERUSALEM.latitude, Places.JERUSALEM.longitude) <= NEAR_JERUSALEM_KM
        return GeoPlace(
            id = "auto",
            nameHe = if (km <= NAME_RADIUS_KM) "ליד ${nearest.nameHe}" else "המיקום שלי",
            latitude = fix.latitude,
            longitude = fix.longitude,
            elevationMeters = fix.elevationMeters,
            zoneId = zone.id,
            inIsrael = inIsrael,
            candleLightingMinutes = if (nearJerusalem) JERUSALEM_CANDLES else DEFAULT_CANDLES,
        )
    }

    private fun customPlace(c: LocationChoice.Custom, zone: ZoneId) = GeoPlace(
        id = "custom",
        nameHe = c.name.ifBlank { "מיקום מותאם" },
        latitude = c.latitude,
        longitude = c.longitude,
        elevationMeters = c.elevationMeters,
        zoneId = zone.id,
        inIsrael = isInIsrael(c.latitude, c.longitude, zone),
        candleLightingMinutes = DEFAULT_CANDLES,
    )

    /** Rough Israel bounding box AND an Israeli device time zone; avoids misclassifying nearby countries. */
    fun isInIsrael(lat: Double, lon: Double, zone: ZoneId): Boolean =
        zone.id in israelZones && lat in 29.4..33.4 && lon in 34.2..35.95

    private const val NAME_RADIUS_KM = 12.0
}
