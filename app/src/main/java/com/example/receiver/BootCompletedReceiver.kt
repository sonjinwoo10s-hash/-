package com.example.receiver

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.alarm.PrayerAlarmScheduler
import com.example.alarm.PrayerScheduleStore
import com.example.service.PrayerCountdownService
import com.example.widget.PrayerTimesWidgetProvider

/**
 * Reschedules all 5 daily prayer alarms, restores the persistent countdown notification,
 * and updates Home Screen Widgets automatically after device reboot, app update,
 * exact alarm permission state change, or system time/date/timezone changes.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    companion object {
        private const val ACTION_EXACT_ALARM_PERMISSION_CHANGED =
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val isTimeOrZoneChange = action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_DATE_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED

        val isExactAlarmPermChanged = action == ACTION_EXACT_ALARM_PERMISSION_CHANGED ||
            (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
                action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            isTimeOrZoneChange ||
            isExactAlarmPermChanged
        ) {
            runCatching {
                PrayerScheduleStore.ensureTodayTimingsFresh(
                    context = context,
                    nowMillis = System.currentTimeMillis(),
                    forceRecalculate = isTimeOrZoneChange
                )
            }
            runCatching { PrayerAlarmScheduler.scheduleAllPrayers(context) }
            runCatching { PrayerCountdownService.syncState(context) }
            runCatching { PrayerTimesWidgetProvider.updateAllWidgets(context) }
        }
    }
}
