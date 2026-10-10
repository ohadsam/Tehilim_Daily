package com.ohadsam.tehilim.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ohadsam.tehilim.calendar.ZmanimHolyTimes
import com.ohadsam.tehilim.data.RemindersRepository
import com.ohadsam.tehilim.location.LocationStore
import com.ohadsam.tehilim.log.AppLogger
import java.time.Instant

/** Keeps exactly one pending alarm per enabled reminder: the next occurrence computed by [ReminderPlanner]. */
class ReminderScheduler(
    private val context: Context,
    private val repo: RemindersRepository,
    private val locations: LocationStore,
    private val logger: AppLogger,
) {
    private val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExact(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()

    fun planner(): ReminderPlanner {
        val place = locations.resolved().place
        return ReminderPlanner(place.zone, ZmanimHolyTimes(place, locations.tzeit))
    }

    suspend fun rescheduleAll(now: Instant = Instant.now()) {
        val all = repo.snapshot()
        val plan = planner()
        all.forEach { schedule(it, plan, now) }
        logger.i(TAG, "rescheduled ${all.size} reminders (exact=${canScheduleExact()})")
    }

    /** Schedules the next occurrence after [after]; cancels the alarm when there is none. Returns it for UI previews. */
    fun schedule(r: Reminder, plan: ReminderPlanner = planner(), after: Instant = Instant.now()): Occurrence? {
        val next = plan.next(r, after).firstOrNull()
        if (next == null) {
            cancel(r.id)
            return null
        }
        setAlarm(pending(r.id, next, snoozeCount = 0, isSnooze = false), next.fireAtMillis)
        return next
    }

    /** One-shot alarm [minutes] from now for the same occurrence content. */
    fun snooze(r: Reminder, o: Occurrence, minutes: Int, snoozeCount: Int) {
        val at = System.currentTimeMillis() + minutes * 60_000L
        setAlarm(pending(r.id, o.copy(fireAtMillis = at), snoozeCount, isSnooze = true), at)
    }

    fun cancel(id: Long) {
        alarms.cancel(pending(id, Occurrence(0, java.time.LocalDate.now()), 0, false))
    }

    private fun pending(id: Long, o: Occurrence, snoozeCount: Int, isSnooze: Boolean): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra(ReminderReceiver.EXTRA_ID, id)
            .putExtra(ReminderReceiver.EXTRA_FIRE_AT, o.fireAtMillis)
            .putExtra(ReminderReceiver.EXTRA_INTENDED, o.intendedDate.toEpochDay())
            .putExtra(ReminderReceiver.EXTRA_SHIFT, o.shift.ordinal)
            .putExtra(ReminderReceiver.EXTRA_SNOOZE_COUNT, snoozeCount)
            .putExtra(ReminderReceiver.EXTRA_IS_SNOOZE, isSnooze)
        // Snooze alarms use their own request code so they never replace the reminder's regular alarm.
        val code = id.toInt() * 4 + if (isSnooze) 3 else 0
        return PendingIntent.getBroadcast(context, code, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun setAlarm(pi: PendingIntent, atMillis: Long) {
        try {
            if (canScheduleExact()) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
            } else {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi) // inexact: user has not granted exact alarms
            }
        } catch (e: SecurityException) {
            logger.w(TAG, "exact alarm denied, using inexact", e)
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        }
    }

    private companion object {
        const val TAG = "Scheduler"
    }
}
