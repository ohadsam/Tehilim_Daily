package com.ohadsam.tehilim.location

import android.content.Context
import com.ohadsam.tehilim.calendar.TzeitMethod

/** Persists the user's location choice, the last good automatic fix and the havdalah method. */
class LocationStore(context: Context) {
    private val prefs = context.getSharedPreferences("location", Context.MODE_PRIVATE)

    var choice: LocationChoice
        get() = decodeChoice(prefs.getString(KEY_CHOICE, null))
        set(value) = prefs.edit().putString(KEY_CHOICE, encodeChoice(value)).apply()

    var lastAutoFix: GeoFix?
        get() = if (prefs.contains(KEY_LAT)) {
            GeoFix(
                Double.fromBits(prefs.getLong(KEY_LAT, 0)), Double.fromBits(prefs.getLong(KEY_LON, 0)),
                Double.fromBits(prefs.getLong(KEY_ELEV, 0)), prefs.getLong(KEY_TIME, 0),
            )
        } else {
            null
        }
        set(v) {
            val e = prefs.edit()
            if (v == null) {
                e.remove(KEY_LAT).remove(KEY_LON).remove(KEY_ELEV).remove(KEY_TIME)
            } else {
                e.putLong(KEY_LAT, v.latitude.toRawBits()).putLong(KEY_LON, v.longitude.toRawBits())
                    .putLong(KEY_ELEV, v.elevationMeters.toRawBits()).putLong(KEY_TIME, v.timeMillis)
            }
            e.apply()
        }

    var dayRollover: com.ohadsam.tehilim.calendar.DayRollover
        get() = runCatching { com.ohadsam.tehilim.calendar.DayRollover.valueOf(prefs.getString(KEY_ROLLOVER, "") ?: "") }
            .getOrDefault(com.ohadsam.tehilim.calendar.DayRollover.MIDNIGHT)
        set(value) = prefs.edit().putString(KEY_ROLLOVER, value.name).apply()

    /** The date whose psalms are "today" right now, honouring the user's choice of when the Hebrew day turns over. */
    fun logicalToday(now: java.time.Instant = java.time.Instant.now()): java.time.LocalDate {
        val place = resolved().place
        val holy = com.ohadsam.tehilim.calendar.ZmanimHolyTimes(place, tzeit)
        val mode = dayRollover
        return com.ohadsam.tehilim.calendar.HebrewDay.logicalDate(now, place.zone, mode) { d ->
            if (mode == com.ohadsam.tehilim.calendar.DayRollover.SUNSET) holy.sunset(d) else holy.havdalah(d)
        }
    }

    var tzeit: TzeitMethod
        get() = runCatching { TzeitMethod.valueOf(prefs.getString(KEY_TZEIT, null) ?: "") }.getOrDefault(TzeitMethod.DEGREES_8_5)
        set(value) = prefs.edit().putString(KEY_TZEIT, value.name).apply()

    /** User override of the candle-lighting offset in minutes (null = the place's default: 40 in Jerusalem, else 18). */
    var candleMinutesOverride: Int?
        get() = if (prefs.contains(KEY_CANDLE)) prefs.getInt(KEY_CANDLE, 18) else null
        set(value) = prefs.edit().apply { if (value == null) remove(KEY_CANDLE) else putInt(KEY_CANDLE, value) }.apply()

    /** The place used for all zmanim; never fails (falls back to Jerusalem). */
    fun resolved(liveFix: GeoFix? = null): ResolvedLocation {
        val r = LocationResolver.resolve(choice, liveFix, lastAutoFix)
        val override = candleMinutesOverride ?: return r
        return r.copy(place = r.place.copy(candleLightingMinutes = override))
    }

    private companion object {
        const val KEY_CHOICE = "choice"
        const val KEY_LAT = "lat"
        const val KEY_LON = "lon"
        const val KEY_ELEV = "elev"
        const val KEY_TIME = "time"
        const val KEY_TZEIT = "tzeit"
        const val KEY_ROLLOVER = "day_rollover"
        const val KEY_CANDLE = "candle_minutes"

        fun encodeChoice(c: LocationChoice): String = when (c) {
            LocationChoice.Auto -> "auto"
            is LocationChoice.Manual -> "manual:${c.placeId}"
            is LocationChoice.Custom -> "custom:${c.latitude}|${c.longitude}|${c.elevationMeters}|${c.name.replace("|", " ")}"
        }

        fun decodeChoice(s: String?): LocationChoice = runCatching {
            when {
                s == null || s == "auto" -> LocationChoice.Auto
                s.startsWith("manual:") -> LocationChoice.Manual(s.removePrefix("manual:"))
                s.startsWith("custom:") -> {
                    val p = s.removePrefix("custom:").split("|", limit = 4)
                    LocationChoice.Custom(p[3], p[0].toDouble(), p[1].toDouble(), p[2].toDouble())
                }
                else -> LocationChoice.Auto
            }
        }.getOrDefault(LocationChoice.Auto)
    }
}
