package com.ohadsam.tehilim.reminders

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** What a reminder is about. [MonthDayToday]/[WeekdayToday] always mean the chapters of the day the reminder is *for*. */
sealed interface ReminderTarget {
    data class Favorite(val id: Long) : ReminderTarget

    data class Person(val id: Long) : ReminderTarget

    /** A favorite together with a person it is read for (the combined "link" the user can remind about). */
    data class Group(val favoriteId: Long, val personId: Long) : ReminderTarget

    data object MonthDayToday : ReminderTarget

    data object WeekdayToday : ReminderTarget

    data class BookmarkTarget(val id: Long) : ReminderTarget
}

sealed interface Repeat {
    data class Once(val date: LocalDate) : Repeat

    data object Daily : Repeat

    data class Weekly(val days: Set<DayOfWeek>) : Repeat

    /** Same Hebrew day of every month; day 30 falls on the 29th in 29-day months. */
    data class MonthlyHebrew(val day: Int) : Repeat

    /** Every year on a Hebrew date (yahrzeit). [month] uses HebrewCalendar numbering; Adar (12) moves to Adar II (13) in leap years. */
    data class YearlyHebrew(val month: Int, val day: Int) : Repeat
}

/** What to do when a reminder would fire on Shabbat / Yom Tov (or within the buffer around them). */
enum class HolyDayRule { NORMAL, BEFORE, AFTER, BOTH, SKIP }

data class AlertConfig(val sound: Boolean = true, val vibrate: Boolean = true) {
    /** A notification is always shown; sound/vibration are optional extras (they decide the notification channel). */
    val channelKey: String get() = "s${if (sound) 1 else 0}v${if (vibrate) 1 else 0}"
}

data class SnoozeConfig(val enabled: Boolean = true, val minutes: Int = 10, val maxCount: Int = 3)

data class Reminder(
    val id: Long = 0,
    val name: String? = null,
    val target: ReminderTarget,
    val times: List<LocalTime>,
    val repeat: Repeat = Repeat.Daily,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val alert: AlertConfig = AlertConfig(),
    val snooze: SnoozeConfig = SnoozeConfig(),
    val holyDayRule: HolyDayRule = HolyDayRule.BOTH,
    val enabled: Boolean = true,
) {
    init {
        require(times.isNotEmpty()) { "a reminder needs at least one time" }
    }
}

enum class Shift { NONE, BEFORE_HOLY, AFTER_HOLY }

/** One concrete notification: when it fires and which day's content it is *for* (differs from the fire date when shifted). */
data class Occurrence(val fireAtMillis: Long, val intendedDate: LocalDate, val shift: Shift = Shift.NONE)
