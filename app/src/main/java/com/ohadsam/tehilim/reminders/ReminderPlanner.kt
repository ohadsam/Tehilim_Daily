package com.ohadsam.tehilim.reminders

import com.ohadsam.tehilim.calendar.HebrewCalendar
import com.ohadsam.tehilim.calendar.HolyTimes
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Pure computation of when a reminder fires. Occurrences that fall on Shabbat / Yom Tov — or within [bufferMinutes] of
 * their start/end — are moved to ≥ [bufferMinutes] before candle lighting and/or ≥ [bufferMinutes] after havdalah.
 */
class ReminderPlanner(
    private val zone: ZoneId,
    private val holy: HolyTimes,
    private val bufferMinutes: Int = DEFAULT_BUFFER_MINUTES,
) {
    private val buffer = Duration.ofMinutes(bufferMinutes.toLong())

    /** Up to [limit] occurrences strictly after [after], earliest first. */
    fun next(reminder: Reminder, after: Instant, limit: Int = 1, horizonDays: Int = DEFAULT_HORIZON_DAYS): List<Occurrence> {
        if (!reminder.enabled) return emptyList()
        val startDay = after.atZone(zone).toLocalDate().minusDays(1)
        val out = sortedSetOf<Occurrence>(compareBy({ it.fireAtMillis }, { it.intendedDate }))
        for (offset in 0..horizonDays) {
            val date = startDay.plusDays(offset.toLong())
            if (!matches(reminder, date)) continue
            for (time in reminder.times) {
                val base = ZonedDateTime.of(date, time, zone).toInstant()
                expand(reminder.holyDayRule, base, date).filter { it.fireAtMillis > after.toEpochMilli() }.forEach { out += it }
            }
            if (out.size >= limit && offset > 4) break // enough found, and shifted ones near the horizon cannot sort earlier
        }
        return out.take(limit)
    }

    private fun expand(rule: HolyDayRule, base: Instant, date: LocalDate): List<Occurrence> {
        val plain = Occurrence(base.toEpochMilli(), date)
        if (rule == HolyDayRule.NORMAL) return listOf(plain)
        val period = holy.periodNear(base, buffer) ?: return listOf(plain)
        val before = Occurrence(period.start.minus(buffer).toEpochMilli(), date, Shift.BEFORE_HOLY)
        val after = Occurrence(period.end.plus(buffer).toEpochMilli(), date, Shift.AFTER_HOLY)
        return when (rule) {
            HolyDayRule.BEFORE -> listOf(before)
            HolyDayRule.AFTER -> listOf(after)
            HolyDayRule.BOTH -> listOf(before, after)
            HolyDayRule.SKIP -> emptyList()
            HolyDayRule.NORMAL -> listOf(plain)
        }
    }

    private fun matches(r: Reminder, date: LocalDate): Boolean {
        if (r.startDate != null && date < r.startDate) return false
        if (r.endDate != null && date > r.endDate) return false
        return when (val rep = r.repeat) {
            is Repeat.Once -> date == rep.date
            Repeat.Daily -> true
            is Repeat.Weekly -> date.dayOfWeek in rep.days
            is Repeat.MonthlyHebrew -> {
                val h = HebrewCalendar.fromLocalDate(date)
                h.day == rep.day.coerceAtMost(h.daysInMonth)
            }
        }
    }

    companion object {
        const val DEFAULT_BUFFER_MINUTES = 30
        const val DEFAULT_HORIZON_DAYS = 400
    }
}
