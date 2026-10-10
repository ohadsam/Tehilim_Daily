package com.ohadsam.tehilim.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ohadsam.tehilim.MainActivity
import com.ohadsam.tehilim.R
import com.ohadsam.tehilim.said.SaidContext

/** Notification channels (one per sound × vibration combination, since Android fixes those per channel) and builder. */
class NotificationHelper(private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)

    fun createChannels() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        for (sound in listOf(true, false)) for (vibrate in listOf(true, false)) {
            val key = AlertConfig(sound, vibrate).channelKey
            val ch = NotificationChannel(channelId(key), context.getString(channelName(sound, vibrate)), NotificationManager.IMPORTANCE_HIGH)
            ch.description = context.getString(R.string.channel_desc)
            ch.enableVibration(vibrate)
            if (vibrate) ch.vibrationPattern = longArrayOf(0, 300, 150, 300)
            if (sound) {
                ch.setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build(),
                )
            } else {
                ch.setSound(null, null)
            }
            nm.createNotificationChannel(ch)
        }
    }

    fun areEnabled(): Boolean = manager.areNotificationsEnabled()

    @Suppress("MissingPermission") // POST_NOTIFICATIONS is requested in the UI; notify() is a no-op when denied.
    fun show(reminder: Reminder, message: ReminderMessage, occurrence: Occurrence, snoozeCount: Int) {
        if (!areEnabled()) return
        val open = PendingIntent.getActivity(
            context, reminder.id.toInt(),
            Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_ROUTE, message.route)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val nid = notificationId(reminder.id)
        val b = NotificationCompat.Builder(context, channelId(reminder.alert.channelKey))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(message.title)
            .setContentText(message.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            // Names of people (refuah / ilui nishmat) are private: on the lock screen show only a generic line.
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(
                NotificationCompat.Builder(context, channelId(reminder.alert.channelKey))
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(context.getString(R.string.notif_public_title))
                    .setContentText(context.getString(R.string.notif_public_text))
                    .build(),
            )
        message.saidContext?.let { b.addAction(0, context.getString(R.string.notif_said), actionIntent(reminder, occurrence, ReminderActionReceiver.ACTION_SAID, message.saidContext, snoozeCount)) }
        if (reminder.snooze.enabled && snoozeCount < reminder.snooze.maxCount) {
            b.addAction(
                0, context.getString(R.string.notif_snooze, reminder.snooze.minutes),
                actionIntent(reminder, occurrence, ReminderActionReceiver.ACTION_SNOOZE, message.saidContext, snoozeCount),
            )
        }
        manager.notify(nid, b.build())
    }

    fun cancel(reminderId: Long) = manager.cancel(notificationId(reminderId))

    private fun actionIntent(r: Reminder, o: Occurrence, action: String, said: SaidContext?, snoozeCount: Int): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java).setAction(action)
            .putExtra(ReminderReceiver.EXTRA_ID, r.id)
            .putExtra(ReminderReceiver.EXTRA_INTENDED, o.intendedDate.toEpochDay())
            .putExtra(ReminderReceiver.EXTRA_SHIFT, o.shift.ordinal)
            .putExtra(ReminderReceiver.EXTRA_SNOOZE_COUNT, snoozeCount)
            .putExtra(ReminderActionReceiver.EXTRA_SAID_TYPE, said?.type?.name)
            .putExtra(ReminderActionReceiver.EXTRA_SAID_ID, said?.id)
        val code = r.id.toInt() * 4 + if (action == ReminderActionReceiver.ACTION_SAID) 1 else 2
        return PendingIntent.getBroadcast(context, code, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    companion object {
        fun channelId(key: String) = "reminders_$key"

        fun notificationId(reminderId: Long) = (reminderId % Int.MAX_VALUE).toInt() + 1000

        private fun channelName(sound: Boolean, vibrate: Boolean) = when {
            sound && vibrate -> R.string.channel_sound_vibrate
            sound -> R.string.channel_sound
            vibrate -> R.string.channel_vibrate
            else -> R.string.channel_silent
        }
    }
}
