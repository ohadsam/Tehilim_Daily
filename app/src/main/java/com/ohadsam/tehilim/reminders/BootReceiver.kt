package com.ohadsam.tehilim.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ohadsam.tehilim.TehilimApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Alarms do not survive a reboot, an app update or a clock/time-zone change; this puts them back. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as TehilimApp
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                app.container.logger.i("BootReceiver", "reschedule after ${intent.action}")
                app.container.reminderScheduler.rescheduleAll()
                com.ohadsam.tehilim.widget.WidgetUpdater.scheduleMidnight(context.applicationContext)
                com.ohadsam.tehilim.widget.WidgetUpdater.updateAll(context.applicationContext)
            } catch (e: Exception) {
                app.container.logger.e("BootReceiver", "reschedule failed", e)
            } finally {
                pending.finish()
            }
        }
    }
}
