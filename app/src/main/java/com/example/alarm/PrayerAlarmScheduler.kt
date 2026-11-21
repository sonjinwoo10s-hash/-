package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.receiver.AdhanReceiver

object PrayerAlarmScheduler {

    const val EXTRA_PRAYER_KEY = "extra_prayer_key"
    const val EXTRA_PRAYER_NAME = "extra_prayer_name"
    const val EXTRA_PRAYER_TIME = "extra_prayer_time"

    private val requestCodeMap = mapOf(
        "Fajr" to 1001,
        "Dhuhr" to 1002,
        "Asr" to 1003,
        "Maghrib" to 1004,
        "Isha" to 1005
    )

    /**
     * Schedules all 5 daily prayers using AlarmManager.setExactAndAllowWhileIdle().
     * Safely degrades to setAndAllowWhileIdle() if exact alarm permission is revoked on API 31+.
     */
    fun scheduleAllPrayers(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val upcoming = PrayerScheduleStore.computeUpcomingPrayerTriggers(appContext)

        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        for (prayer in upcoming) {
            val reqCode = requestCodeMap[prayer.key] ?: continue
            val intent = Intent(appContext, AdhanReceiver::class.java).apply {
                action = AdhanReceiver.ACTION_TRIGGER_ADHAN
                putExtra(EXTRA_PRAYER_KEY, prayer.key)
                putExtra(EXTRA_PRAYER_NAME, prayer.arabicName)
                putExtra(EXTRA_PRAYER_TIME, prayer.formatted12)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getBroadcast(appContext, reqCode, intent, flags)

            try {
                if (canExact) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        prayer.triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        prayer.triggerAtMillis,
                        pendingIntent
                    )
                }
            } catch (_: SecurityException) {
                runCatching {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        prayer.triggerAtMillis,
                        pendingIntent
                    )
                }
            }
        }
    }

    fun cancelAllPrayers(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        requestCodeMap.values.forEach { reqCode ->
            val intent = Intent(appContext, AdhanReceiver::class.java).apply {
                action = AdhanReceiver.ACTION_TRIGGER_ADHAN
            }
            val pi = PendingIntent.getBroadcast(appContext, reqCode, intent, flags)
            alarmManager.cancel(pi)
        }
    }
}
