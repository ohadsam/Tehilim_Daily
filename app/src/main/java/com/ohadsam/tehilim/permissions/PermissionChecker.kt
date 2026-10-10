package com.ohadsam.tehilim.permissions

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import com.ohadsam.tehilim.location.DeviceLocation
import com.ohadsam.tehilim.reminders.AlertConfig
import com.ohadsam.tehilim.reminders.NotificationHelper

enum class PermissionId { NOTIFICATIONS, CHANNELS, EXACT_ALARMS, BATTERY, LOCATION }

/** [required]: reminders cannot work properly without it; otherwise it is recommended / optional. */
data class PermissionItem(val id: PermissionId, val granted: Boolean, val required: Boolean)

data class PermissionSummary(val items: List<PermissionItem>) {
    val missingRequired: List<PermissionItem> get() = items.filter { it.required && !it.granted }
    val missingOptional: List<PermissionItem> get() = items.filter { !it.required && !it.granted }
    val allGood: Boolean get() = missingRequired.isEmpty() && missingOptional.isEmpty()
}

/** Checks everything the app needs to deliver reminders reliably; nothing here changes system state. */
object PermissionChecker {
    fun check(context: Context): PermissionSummary {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationsOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
        val channelsOk = listOf(true, false).all { s ->
            listOf(true, false).all { v ->
                val ch = nm.getNotificationChannel(NotificationHelper.channelId(AlertConfig(s, v).channelKey))
                ch == null || ch.importance != NotificationManager.IMPORTANCE_NONE
            }
        }
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val exactOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
        val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val batteryOk = power.isIgnoringBatteryOptimizations(context.packageName)
        return PermissionSummary(
            listOf(
                PermissionItem(PermissionId.NOTIFICATIONS, notificationsOn, required = true),
                PermissionItem(PermissionId.CHANNELS, channelsOk, required = true),
                PermissionItem(PermissionId.EXACT_ALARMS, exactOk, required = true),
                PermissionItem(PermissionId.BATTERY, batteryOk, required = false),
                PermissionItem(PermissionId.LOCATION, DeviceLocation.hasPermission(context), required = false),
            ),
        )
    }
}
