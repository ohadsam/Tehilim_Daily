package com.ohadsam.tehilim.data

import com.ohadsam.tehilim.data.db.ReminderEntity
import com.ohadsam.tehilim.reminders.AlertConfig
import com.ohadsam.tehilim.reminders.HolyDayRule
import com.ohadsam.tehilim.reminders.Reminder
import com.ohadsam.tehilim.reminders.ReminderTarget
import com.ohadsam.tehilim.reminders.Repeat
import com.ohadsam.tehilim.reminders.SnoozeConfig
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** Reminder ⇄ database row. Unknown/corrupt rows decode to null so one bad row never breaks scheduling. */
object ReminderCodec {
    fun encode(r: Reminder): ReminderEntity {
        val (tType, id1, id2) = when (val t = r.target) {
            is ReminderTarget.Favorite -> Triple("FAVORITE", t.id, 0L)
            is ReminderTarget.Person -> Triple("PERSON", t.id, 0L)
            is ReminderTarget.Group -> Triple("GROUP", t.favoriteId, t.personId)
            ReminderTarget.MonthDayToday -> Triple("MONTH_DAY", 0L, 0L)
            ReminderTarget.WeekdayToday -> Triple("WEEKDAY", 0L, 0L)
            is ReminderTarget.BookmarkTarget -> Triple("BOOKMARK", t.id, 0L)
        }
        val (rType, rArg) = when (val rep = r.repeat) {
            is Repeat.Once -> "ONCE" to rep.date.toEpochDay()
            Repeat.Daily -> "DAILY" to 0L
            is Repeat.Weekly -> "WEEKLY" to rep.days.fold(0L) { acc, d -> acc or (1L shl (d.value - 1)) }
            is Repeat.MonthlyHebrew -> "MONTHLY_HEBREW" to rep.day.toLong()
            is Repeat.YearlyHebrew -> "YEARLY_HEBREW" to (rep.month * 100L + rep.day)
        }
        return ReminderEntity(
            id = r.id, name = r.name, targetType = tType, targetId1 = id1, targetId2 = id2,
            timesCsv = r.times.sorted().joinToString(",") { "%02d:%02d".format(it.hour, it.minute) },
            repeatType = rType, repeatArg = rArg,
            startEpochDay = r.startDate?.toEpochDay(), endEpochDay = r.endDate?.toEpochDay(),
            sound = r.alert.sound, vibrate = r.alert.vibrate,
            snoozeEnabled = r.snooze.enabled, snoozeMinutes = r.snooze.minutes, snoozeMax = r.snooze.maxCount,
            holyRule = r.holyDayRule.name, enabled = r.enabled,
        )
    }

    fun decode(e: ReminderEntity): Reminder? = runCatching {
        val target = when (e.targetType) {
            "FAVORITE" -> ReminderTarget.Favorite(e.targetId1)
            "PERSON" -> ReminderTarget.Person(e.targetId1)
            "GROUP" -> ReminderTarget.Group(e.targetId1, e.targetId2)
            "MONTH_DAY" -> ReminderTarget.MonthDayToday
            "WEEKDAY" -> ReminderTarget.WeekdayToday
            "BOOKMARK" -> ReminderTarget.BookmarkTarget(e.targetId1)
            else -> return null
        }
        val repeat = when (e.repeatType) {
            "ONCE" -> Repeat.Once(LocalDate.ofEpochDay(e.repeatArg))
            "DAILY" -> Repeat.Daily
            "WEEKLY" -> Repeat.Weekly(DayOfWeek.entries.filter { e.repeatArg and (1L shl (it.value - 1)) != 0L }.toSet())
            "MONTHLY_HEBREW" -> Repeat.MonthlyHebrew(e.repeatArg.toInt())
            "YEARLY_HEBREW" -> Repeat.YearlyHebrew((e.repeatArg / 100).toInt(), (e.repeatArg % 100).toInt())
            else -> return null
        }
        Reminder(
            id = e.id, name = e.name, target = target,
            times = e.timesCsv.split(",").filter { it.isNotBlank() }.map { LocalTime.parse(it) },
            repeat = repeat,
            startDate = e.startEpochDay?.let(LocalDate::ofEpochDay), endDate = e.endEpochDay?.let(LocalDate::ofEpochDay),
            alert = AlertConfig(e.sound, e.vibrate),
            snooze = SnoozeConfig(e.snoozeEnabled, e.snoozeMinutes, e.snoozeMax),
            holyDayRule = HolyDayRule.valueOf(e.holyRule), enabled = e.enabled,
        )
    }.getOrNull()
}
