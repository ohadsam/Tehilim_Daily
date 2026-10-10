package com.ohadsam.tehilim.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import com.ohadsam.tehilim.TehilimApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

object WidgetUpdater {
    suspend fun updateAll(context: Context) {
        runCatching { HebrewDateWidget().updateAll(context) }
        runCatching { WeekdayWidget().updateAll(context) }
        runCatching { ReaderWidget().updateAll(context) }
        runCatching { FavoritesListWidget().updateAll(context) }
        runCatching { RefuahListWidget().updateAll(context) }
        runCatching { IluiListWidget().updateAll(context) }
        runCatching { BookmarkWidget().updateAll(context) }
    }

    /** Fires shortly after local midnight so date-based widgets roll over even if the system broadcast is missed. */
    fun scheduleMidnight(context: Context) {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val next = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + 30_000L
        val pi = PendingIntent.getBroadcast(
            context, 7001, Intent(context, DateChangeReceiver::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        runCatching { alarms.setAndAllowWhileIdle(AlarmManager.RTC, next, pi) }
    }
}

/** Date / time / time-zone changes and the midnight alarm: refresh every widget and re-arm the midnight alarm. */
class DateChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as TehilimApp
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                app.container.logger.i("Widgets", "refresh after ${intent.action ?: "midnight alarm"}")
                WidgetUpdater.updateAll(context.applicationContext)
                WidgetUpdater.scheduleMidnight(context.applicationContext)
            } finally {
                pending.finish()
            }
        }
    }
}
