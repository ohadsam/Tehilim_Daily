package com.ohadsam.tehilim.ui.common

import com.ohadsam.tehilim.calendar.HebrewCalendar
import com.ohadsam.tehilim.domain.schedule.hebrewName
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val hm: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** "שבת קודש · י״ד בתשרי · 17:07" — used in previews of when something will happen. */
fun formatMoment(instant: Instant, zone: ZoneId): String {
    val z = instant.atZone(zone)
    val h = HebrewCalendar.fromLocalDate(z.toLocalDate())
    return "${z.dayOfWeek.hebrewName} · ${h.format(withYear = false)} · ${hm.format(z)}"
}

fun formatTime(instant: Instant, zone: ZoneId): String = hm.format(instant.atZone(zone))
