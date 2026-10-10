package com.ohadsam.tehilim.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ohadsam.tehilim.TehilimApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

/** Fires when a reminder's alarm goes off: shows the notification (unless already said) and schedules the next one. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as TehilimApp
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                handle(app, intent)
            } catch (e: Exception) {
                app.container.logger.e("ReminderReceiver", "failed to handle alarm", e)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handle(app: TehilimApp, intent: Intent) {
        val c = app.container
        val id = intent.getLongExtra(EXTRA_ID, 0)
        val reminder = c.reminders.get(id) ?: return
        val shift = Shift.entries.getOrElse(intent.getIntExtra(EXTRA_SHIFT, 0)) { Shift.NONE }
        val scheduledDate = LocalDate.ofEpochDay(intent.getLongExtra(EXTRA_INTENDED, LocalDate.now().toEpochDay()))
        // For "today's psalms" reminders fired after sunset/nightfall (when the user chose that), the content is the next day's.
        val dayBased = reminder.target is ReminderTarget.MonthDayToday || reminder.target is ReminderTarget.WeekdayToday
        val intended = if (dayBased && shift == Shift.NONE) c.locations.logicalToday(Instant.ofEpochMilli(intent.getLongExtra(EXTRA_FIRE_AT, System.currentTimeMillis()))) else scheduledDate
        val snoozeCount = intent.getIntExtra(EXTRA_SNOOZE_COUNT, 0)
        val occurrence = Occurrence(intent.getLongExtra(EXTRA_FIRE_AT, System.currentTimeMillis()), intended, shift)
        val today = LocalDate.now()

        val message = ReminderContent.build(
            reminder, intended, today, shift,
            c.favorites.state.first(), c.bookmarks.bookmarks.first(),
        )
        val alreadySaid = message?.saidContext?.let { c.said.isSaid(today, it) } == true
        if (message != null && !alreadySaid) {
            c.notifications.show(reminder, message, occurrence, snoozeCount)
            c.logger.i("ReminderReceiver", "notified reminder=$id intended=$intended shift=$shift")
        } else {
            c.logger.i("ReminderReceiver", "suppressed reminder=$id (said=$alreadySaid, target missing=${message == null})")
        }
        if (!intent.getBooleanExtra(EXTRA_IS_SNOOZE, false)) {
            c.reminderScheduler.schedule(reminder, after = Instant.ofEpochMilli(maxOf(occurrence.fireAtMillis, System.currentTimeMillis())))
        }
    }

    companion object {
        const val EXTRA_ID = "reminder_id"
        const val EXTRA_FIRE_AT = "fire_at"
        const val EXTRA_INTENDED = "intended_epoch_day"
        const val EXTRA_SHIFT = "shift"
        const val EXTRA_SNOOZE_COUNT = "snooze_count"
        const val EXTRA_IS_SNOOZE = "is_snooze"
    }
}
