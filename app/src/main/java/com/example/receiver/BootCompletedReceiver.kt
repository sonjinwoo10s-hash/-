package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.alarm.PrayerAlarmScheduler
import com.example.service.PrayerCountdownService

/**
 * Reschedules all 5 daily prayer alarms and restores the persistent countdown notification
 * automatically after device reboot, app update, or system time/timezone changes.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            PrayerAlarmScheduler.scheduleAllPrayers(context)
            PrayerCountdownService.syncState(context)
        }
    }
}
