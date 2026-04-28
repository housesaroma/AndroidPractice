package com.example.androidpractice.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

class FavoriteLessonReminderScheduler(
    private val context: Context
) : com.example.androidpractice.domain.notifications.FavoriteLessonReminderScheduler {
    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun schedule(ownerName: String, favoriteLessonTime: String) {
        val triggerAtMillis = parseTimeToTodayMillis(favoriteLessonTime) ?: return

        val reminderIntent = Intent(context, FavoriteLessonReminderReceiver::class.java).apply {
            putExtra(FavoriteLessonReminderReceiver.EXTRA_OWNER_NAME, ownerName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REMINDER_REQUEST_CODE,
            reminderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    private fun parseTimeToTodayMillis(value: String): Long? {
        val parts = value.split(":")
        if (parts.size != 2) {
            return null
        }

        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) {
            return null
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private companion object {
        const val REMINDER_REQUEST_CODE = 2050
    }
}
