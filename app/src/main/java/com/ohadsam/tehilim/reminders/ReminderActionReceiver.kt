package com.ohadsam.tehilim.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ohadsam.tehilim.TehilimApp
import com.ohadsam.tehilim.said.SaidContext
import com.ohadsam.tehilim.said.SaidType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Handles the notification buttons "נאמר" and "נודניק". */
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as TehilimApp
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val c = app.container
                val id = intent.getLongExtra(ReminderReceiver.EXTRA_ID, 0)
                c.notifications.cancel(id)
                when (intent.action) {
                    ACTION_SAID -> {
                        val type = intent.getStringExtra(EXTRA_SAID_TYPE)?.let { runCatching { SaidType.valueOf(it) }.getOrNull() }
                        val sid = intent.getStringExtra(EXTRA_SAID_ID)
                        if (type != null && sid != null) c.said.mark(LocalDate.now(), SaidContext(type, sid))
                    }
                    ACTION_SNOOZE -> {
                        val r = c.reminders.get(id) ?: return@launch
                        val o = Occurrence(
                            System.currentTimeMillis(),
                            LocalDate.ofEpochDay(intent.getLongExtra(ReminderReceiver.EXTRA_INTENDED, LocalDate.now().toEpochDay())),
                            Shift.entries.getOrElse(intent.getIntExtra(ReminderReceiver.EXTRA_SHIFT, 0)) { Shift.NONE },
                        )
                        c.reminderScheduler.snooze(r, o, r.snooze.minutes, intent.getIntExtra(ReminderReceiver.EXTRA_SNOOZE_COUNT, 0) + 1)
                    }
                }
            } catch (e: Exception) {
                app.container.logger.e("ReminderAction", "failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_SAID = "com.ohadsam.tehilim.action.SAID"
        const val ACTION_SNOOZE = "com.ohadsam.tehilim.action.SNOOZE"
        const val EXTRA_SAID_TYPE = "said_type"
        const val EXTRA_SAID_ID = "said_id"
    }
}
