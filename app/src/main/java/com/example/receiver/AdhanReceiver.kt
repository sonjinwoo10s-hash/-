package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.alarm.PrayerAlarmScheduler
import com.example.service.AdhanPlaybackService
import com.example.service.PrayerCountdownService

/**
 * Triggered by AlarmManager at each daily prayer time.
 * Starts AdhanPlaybackService as a ForegroundService and immediately reschedules
 * all daily prayer alarms (rolling this prayer to tomorrow) and refreshes countdown.
 */
class AdhanReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_ADHAN = "com.example.action.TRIGGER_ADHAN"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val prayerKey = intent?.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_KEY) ?: "Fajr"
        val prayerName = intent?.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_NAME) ?: "الصلاة"
        val prayerTime = intent?.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_TIME).orEmpty()

        val serviceIntent = Intent(context, AdhanPlaybackService::class.java).apply {
            action = AdhanPlaybackService.ACTION_START_ADHAN
            putExtra(PrayerAlarmScheduler.EXTRA_PRAYER_KEY, prayerKey)
            putExtra(PrayerAlarmScheduler.EXTRA_PRAYER_NAME, prayerName)
            putExtra(PrayerAlarmScheduler.EXTRA_PRAYER_TIME, prayerTime)
        }

        runCatching {
            ContextCompat.startForegroundService(context, serviceIntent)
        }

        // Reschedule all 5 daily prayers so tomorrow's alarm is guaranteed to be set
        PrayerAlarmScheduler.scheduleAllPrayers(context)

        // Update the persistent next-prayer countdown notification immediately
        PrayerCountdownService.syncState(context)
    }
}
