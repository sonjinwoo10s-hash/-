package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.alarm.PermissionsHelper
import com.example.alarm.PrayerAlarmScheduler
import com.example.alarm.PrayerScheduleStore
import com.example.service.AdhanPlaybackService
import com.example.service.PrayerCountdownService
import com.example.widget.PrayerTimesWidgetProvider

/**
 * Triggered by AlarmManager at each daily prayer time, Pre-Adhan reminder, Iqama alert,
 * Auto-Silent DND start/restore window, and weekly Friday Surah Al-Kahf reminder.
 */
class AdhanReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_ADHAN = "com.example.action.TRIGGER_ADHAN"
        const val ACTION_TRIGGER_PRE_ADHAN = "com.example.action.TRIGGER_PRE_ADHAN"
        const val ACTION_TRIGGER_IQAMA = "com.example.action.TRIGGER_IQAMA"
        const val ACTION_AUTO_SILENT_START = "com.example.action.AUTO_SILENT_START"
        const val ACTION_AUTO_SILENT_RESTORE = "com.example.action.AUTO_SILENT_RESTORE"
        const val ACTION_TRIGGER_FRIDAY_KAHF = "com.example.action.TRIGGER_FRIDAY_KAHF"

        private const val KAHF_CHANNEL_ID = "rafiq_friday_kahf_channel"
        private const val KAHF_NOTIFICATION_ID = 5001
    }

    override fun onReceive(context: Context, intent: Intent?) {
        runCatching {
            val action = intent?.action ?: ACTION_TRIGGER_ADHAN

            when (action) {
                ACTION_AUTO_SILENT_START -> {
                    handleAutoSilentStart(
                        context = context,
                        durationMin = intent?.getIntExtra(
                            PrayerAlarmScheduler.EXTRA_OFFSET_MINUTES,
                            PrayerScheduleStore.getAutoSilentDurationMinutes(context)
                        ) ?: 15
                    )
                    runCatching { PrayerAlarmScheduler.scheduleAllPrayers(context) }
                    return
                }

                ACTION_AUTO_SILENT_RESTORE -> {
                    handleAutoSilentRestore(context)
                    return
                }

                ACTION_TRIGGER_FRIDAY_KAHF -> {
                    postFridayKahfNotification(context)
                    runCatching { PrayerAlarmScheduler.scheduleAllPrayers(context) }
                    return
                }
            }

            val prayerKey = intent?.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_KEY) ?: "Fajr"
            val prayerName = intent?.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_NAME) ?: "الصلاة"
            val prayerTime = intent?.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_TIME).orEmpty()
            val offsetMinutes = intent?.getIntExtra(PrayerAlarmScheduler.EXTRA_OFFSET_MINUTES, 0) ?: 0

            val serviceAction = when (action) {
                ACTION_TRIGGER_PRE_ADHAN -> AdhanPlaybackService.ACTION_START_PRE_ADHAN
                ACTION_TRIGGER_IQAMA -> AdhanPlaybackService.ACTION_START_IQAMA
                else -> AdhanPlaybackService.ACTION_START_ADHAN
            }

            val serviceIntent = Intent(context, AdhanPlaybackService::class.java).apply {
                this.action = serviceAction
                putExtra(PrayerAlarmScheduler.EXTRA_PRAYER_KEY, prayerKey)
                putExtra(PrayerAlarmScheduler.EXTRA_PRAYER_NAME, prayerName)
                putExtra(PrayerAlarmScheduler.EXTRA_PRAYER_TIME, prayerTime)
                putExtra(PrayerAlarmScheduler.EXTRA_OFFSET_MINUTES, offsetMinutes)
            }

            runCatching {
                ContextCompat.startForegroundService(context, serviceIntent)
            }

            // Reschedule all daily Adhan, Pre-Adhan, Iqama, Auto-Silent & Friday alarms
            runCatching { PrayerAlarmScheduler.scheduleAllPrayers(context) }

            // Update the persistent next-prayer countdown notification if enabled
            runCatching { PrayerCountdownService.syncState(context) }

            // Refresh Home Screen Prayer Times widgets
            runCatching { PrayerTimesWidgetProvider.updateAllWidgets(context) }
        }
    }

    private fun handleAutoSilentStart(context: Context, durationMin: Int) {
        if (!PrayerScheduleStore.isAutoSilentEnabled(context)) return
        if (!PrayerScheduleStore.isDndPermissionGranted(context)) return

        runCatching {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val prevRinger = am?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
            val prevFilter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && nm != null) {
                nm.currentInterruptionFilter
            } else {
                NotificationManager.INTERRUPTION_FILTER_ALL
            }

            PrayerScheduleStore.savePreSilentState(context, prevRinger, prevFilter)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && nm != null) {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
            }
            am?.ringerMode = AudioManager.RINGER_MODE_SILENT

            PrayerAlarmScheduler.scheduleAutoSilentRestore(context, durationMin)
        }
    }

    private fun handleAutoSilentRestore(context: Context) {
        val saved = PrayerScheduleStore.consumePreSilentState(context) ?: return
        if (!PrayerScheduleStore.isDndPermissionGranted(context)) return

        runCatching {
            val (prevRinger, prevFilter) = saved
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && nm != null && prevFilter >= 0) {
                nm.setInterruptionFilter(prevFilter)
            }
            if (prevRinger >= 0) {
                am?.ringerMode = prevRinger
            }
        }
    }

    private fun postFridayKahfNotification(context: Context) {
        if (!PrayerScheduleStore.isFridayKahfReminderEnabled(context)) return
        if (!PermissionsHelper.isNotificationGranted(context)) return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                KAHF_CHANNEL_ID,
                "تذكير سورة الكهف يوم الجمعة",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "تذكير لطيف كل يوم جمعة لقراءة سورة الكهف والصلاة على النبي ﷺ"
            }
            nm.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            KAHF_NOTIFICATION_ID,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = "جمعة مباركة 🌿 لا تنسَ قراءة سورة الكهف والإكثار من الصلاة على النبي ﷺ"
        val notification = NotificationCompat.Builder(context, KAHF_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_mosque)
            .setContentTitle("جمعة مباركة 🌿")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        runCatching {
            nm.notify(KAHF_NOTIFICATION_ID, notification)
        }
    }
}
