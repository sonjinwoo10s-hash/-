package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.alarm.PermissionsHelper
import com.example.alarm.PrayerScheduleStore
import com.example.widget.PrayerTimesWidgetProvider

/**
 * Lightweight ForegroundService displaying a silent, ongoing status-bar notification
 * showing the next prayer and remaining hours/minutes, updated every 60 seconds.
 */
class PrayerCountdownService : Service() {

    companion object {
        const val ACTION_START_OR_UPDATE = "com.example.action.START_PRAYER_COUNTDOWN"
        const val ACTION_STOP = "com.example.action.STOP_PRAYER_COUNTDOWN"

        const val CHANNEL_ID = "rafiq_prayer_countdown_channel"
        const val NOTIFICATION_ID = 4201
        private const val UPDATE_INTERVAL_MS = 60_000L

        fun syncState(context: Context) {
            val appCtx = context.applicationContext
            val enabled = PrayerScheduleStore.isCountdownNotificationEnabled(appCtx)
            val hasPerm = PermissionsHelper.isNotificationGranted(appCtx)
            if (enabled && hasPerm) {
                startOrRefresh(appCtx)
            } else {
                stop(appCtx)
            }
        }

        fun startOrRefresh(context: Context) {
            val appCtx = context.applicationContext
            if (!PermissionsHelper.isNotificationGranted(appCtx)) return
            val intent = Intent(appCtx, PrayerCountdownService::class.java).apply {
                action = ACTION_START_OR_UPDATE
            }
            runCatching {
                ContextCompat.startForegroundService(appCtx, intent)
            }
        }

        fun stop(context: Context) {
            val appCtx = context.applicationContext
            runCatching {
                appCtx.stopService(Intent(appCtx, PrayerCountdownService::class.java))
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val tickRunnable = object : Runnable {
        override fun run() {
            if (!PrayerScheduleStore.isCountdownNotificationEnabled(applicationContext)) {
                stopServiceGracefully()
                return
            }
            updateNotification()
            handler.postDelayed(this, UPDATE_INTERVAL_MS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP ||
            !PrayerScheduleStore.isCountdownNotificationEnabled(this) ||
            !PermissionsHelper.isNotificationGranted(this)
        ) {
            stopServiceGracefully()
            return START_NOT_STICKY
        }

        createSilentChannel()
        val notification = buildCountdownNotification()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }

        handler.removeCallbacks(tickRunnable)
        handler.postDelayed(tickRunnable, UPDATE_INTERVAL_MS)
        return START_STICKY
    }

    private fun updateNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        nm.notify(NOTIFICATION_ID, buildCountdownNotification())
        PrayerTimesWidgetProvider.updateAllWidgets(applicationContext)
    }

    private fun buildCountdownNotification(): Notification {
        val info = PrayerScheduleStore.computeNextPrayerCountdown(this)

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            4202,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "الصلاة القادمة: ${info.arabicName} (${info.formatted12})"
        val body = "متبقي ${info.remainingHours} ساعة و ${info.remainingMinutes} دقيقة على الأذان"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_mosque)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setContentIntent(contentIntent)
            .build()
    }

    private fun createSilentChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "الوقت المتبقي للصلاة القادمة",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعار صامت ومستمر يعرض الوقت المتبقي على الأذان القادم"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun stopServiceGracefully() {
        handler.removeCallbacks(tickRunnable)
        runCatching {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        }
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacks(tickRunnable)
        super.onDestroy()
    }
}
