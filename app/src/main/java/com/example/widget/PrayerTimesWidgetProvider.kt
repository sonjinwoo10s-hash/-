package com.example.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Looper
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.alarm.PrayerScheduleStore
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Modern RTL Arabic Home Screen Widget for "رفيق المسلم".
 *
 * Displays:
 * - Header: App name ("رفيق المسلم") + current Hijri date + manual refresh button
 * - Main Highlight: Next prayer name, exact time, and remaining time (e.g., "متبقي 1 س 24 د")
 * - Bottom Strip: 5 daily prayers (الفجر، الظهر، العصر، المغرب، العشاء) with the next prayer highlighted
 * - Night Footer: Islamic Midnight (منتصف الليل الشرعي) & Last Third of the Night (الثلث الأخير)
 *
 * Updates automatically when prayers arrive (via AdhanReceiver & next-prayer widget alarm),
 * every 15 minutes via lightweight AlarmManager tick, and on time/date/timezone/boot changes.
 */
class PrayerTimesWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_WIDGET_REFRESH = "com.example.action.WIDGET_REFRESH_PRAYER_TIMES"
        const val ACTION_WIDGET_TICK = "com.example.action.WIDGET_PERIODIC_TICK"

        private const val REQUEST_CODE_OPEN_APP = 7301
        private const val REQUEST_CODE_REFRESH = 7302
        private const val REQUEST_CODE_PERIODIC_TICK = 7303
        private const val REQUEST_CODE_NEXT_PRAYER_TICK = 7304

        private const val PERIODIC_REFRESH_INTERVAL_MS = 15L * 60_000L

        private val COLOR_GOLD = Color.parseColor("#FFF6DC97")
        private val COLOR_WHITE = Color.parseColor("#FFFFFFFF")
        private val COLOR_WHITE_SOFT = Color.parseColor("#E6FFFFFF")

        private data class PrayerCellViewIds(
            val key: String,
            val containerId: Int,
            val labelId: Int,
            val timeId: Int
        )

        private val prayerCellViews = listOf(
            PrayerCellViewIds("Fajr", R.id.widget_cell_fajr, R.id.widget_label_fajr, R.id.widget_time_fajr),
            PrayerCellViewIds("Dhuhr", R.id.widget_cell_dhuhr, R.id.widget_label_dhuhr, R.id.widget_time_dhuhr),
            PrayerCellViewIds("Asr", R.id.widget_cell_asr, R.id.widget_label_asr, R.id.widget_time_asr),
            PrayerCellViewIds("Maghrib", R.id.widget_cell_maghrib, R.id.widget_label_maghrib, R.id.widget_time_maghrib),
            PrayerCellViewIds("Isha", R.id.widget_cell_isha, R.id.widget_label_isha, R.id.widget_time_isha)
        )

        private val widgetExecutor = Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "rafiq-widget-io").apply { isDaemon = true }
        }

        @Volatile
        private var pendingWidgetFuture: ScheduledFuture<*>? = null
        private val widgetLock = Any()

        /**
         * Debounced background widget refresh: coalesces rapid settings/prayer changes
         * into a single off-main-thread update (zero cost when no widget is placed).
         */
        fun scheduleDebouncedWidgetUpdate(context: Context, delayMs: Long = 900L) {
            val appCtx = context.applicationContext
            synchronized(widgetLock) {
                pendingWidgetFuture?.cancel(false)
                pendingWidgetFuture = runCatching {
                    widgetExecutor.schedule(
                        { performWidgetUpdateNow(appCtx) },
                        delayMs.coerceAtLeast(0L),
                        TimeUnit.MILLISECONDS
                    )
                }.getOrNull()
            }
        }

        /**
         * Safely refreshes all placed Prayer Times widgets (zero overhead if none are placed).
         * Offloads automatically when invoked from the Main thread.
         */
        fun updateAllWidgets(context: Context) {
            val appCtx = context.applicationContext
            if (Looper.myLooper() == Looper.getMainLooper()) {
                scheduleDebouncedWidgetUpdate(appCtx, delayMs = 200L)
            } else {
                performWidgetUpdateNow(appCtx)
            }
        }

        private fun performWidgetUpdateNow(appCtx: Context) {
            runCatching {
                val manager = AppWidgetManager.getInstance(appCtx)
                val component = ComponentName(appCtx, PrayerTimesWidgetProvider::class.java)
                val ids = manager.getAppWidgetIds(component)
                if (ids.isEmpty()) {
                    cancelScheduledUpdates(appCtx)
                    return
                }

                val views = buildWidgetRemoteViews(appCtx)
                ids.forEach { widgetId ->
                    runCatching { manager.updateAppWidget(widgetId, views) }
                }
                scheduleNextUpdates(appCtx)
            }
        }

        private fun buildWidgetRemoteViews(context: Context): RemoteViews {
            val nowMs = System.currentTimeMillis()
            PrayerScheduleStore.ensureTodayTimingsFresh(context, nowMs)
            val timings = PrayerScheduleStore.getTimings(context)
            val nextInfo = PrayerScheduleStore.computeNextPrayerCountdown(context, nowMs)
            val nightTimes = PrayerScheduleStore.computeIslamicNightTimes(context, nowMs)
            val hijriDate = PrayerScheduleStore.getHijriDateOrOffline(context, nowMs)
            val cityLabel = PrayerScheduleStore.getCityLabel(context)

            val remainingText = formatRemainingCompactArabic(
                hours = nextInfo.remainingHours,
                minutes = nextInfo.remainingMinutes
            )

            return RemoteViews(context.packageName, R.layout.widget_prayer_times).apply {
                setTextViewText(R.id.widget_hijri_date, hijriDate)
                setTextViewText(R.id.widget_city_name, cityLabel)
                setTextViewText(R.id.widget_next_prayer_name, "صلاة ${nextInfo.arabicName}")
                setTextViewText(R.id.widget_next_prayer_time, nextInfo.formatted12)
                setTextViewText(R.id.widget_remaining_time, remainingText)
                setTextViewText(
                    R.id.widget_midnight_text,
                    "منتصف الليل: ${nightTimes.islamicMidnightFormatted}  •  الثلث الأخير: ${nightTimes.lastThirdNightFormatted}"
                )

                // Populate 5 daily prayers strip and highlight the upcoming prayer
                for (cell in prayerCellViews) {
                    val time24 = timings[cell.key] ?: "--:--"
                    val formatted = PrayerScheduleStore.format12HourArabic(time24)
                    val isNext = cell.key == nextInfo.key

                    setTextViewText(cell.timeId, formatted)
                    setInt(
                        cell.containerId,
                        "setBackgroundResource",
                        if (isNext) R.drawable.widget_prayer_cell_next_bg else R.drawable.widget_prayer_cell_bg
                    )
                    setTextColor(cell.labelId, if (isNext) COLOR_GOLD else COLOR_WHITE_SOFT)
                    setTextColor(cell.timeId, if (isNext) COLOR_GOLD else COLOR_WHITE)
                }

                // Tapping anywhere on the widget opens MainActivity
                val openAppIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val openAppPendingIntent = PendingIntent.getActivity(
                    context,
                    REQUEST_CODE_OPEN_APP,
                    openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)

                // Small refresh icon forces an immediate widget update
                val refreshIntent = Intent(context, PrayerTimesWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_REFRESH
                }
                val refreshPendingIntent = PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE_REFRESH,
                    refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.widget_refresh_btn, refreshPendingIntent)
            }
        }

        private fun formatRemainingCompactArabic(hours: Int, minutes: Int): String {
            return when {
                hours > 0 && minutes > 0 -> "متبقي $hours س $minutes د"
                hours > 0 -> "متبقي $hours س"
                minutes > 0 -> "متبقي $minutes د"
                else -> "حان وقت الأذان"
            }
        }

        /**
         * Schedules both:
         * 1. A battery-friendly 15-minute periodic refresh so "remaining time" stays fresh.
         * 2. A targeted update right when the very next prayer time arrives.
         */
        fun scheduleNextUpdates(context: Context) {
            val appCtx = context.applicationContext
            val alarmManager = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val nowMs = System.currentTimeMillis()

            val tickIntent = Intent(appCtx, PrayerTimesWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_TICK
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

            // 1. Lightweight periodic refresh (next 15-minute boundary)
            val periodicPi = PendingIntent.getBroadcast(appCtx, REQUEST_CODE_PERIODIC_TICK, tickIntent, flags)
            val nextPeriodicMs = nowMs + PERIODIC_REFRESH_INTERVAL_MS
            runCatching {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC, nextPeriodicMs, periodicPi)
            }

            // 2. Targeted refresh right at the next upcoming prayer timestamp (+2s)
            val upcoming = PrayerScheduleStore.computeUpcomingPrayerTriggers(appCtx, nowMs)
            val nextTriggerMs = upcoming.minOfOrNull { it.triggerAtMillis }
            if (nextTriggerMs != null && nextTriggerMs > nowMs) {
                val nextPrayerPi = PendingIntent.getBroadcast(
                    appCtx,
                    REQUEST_CODE_NEXT_PRAYER_TICK,
                    tickIntent,
                    flags
                )
                val targetMs = nextTriggerMs + 2_000L
                val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    alarmManager.canScheduleExactAlarms()
                } else {
                    true
                }
                runCatching {
                    if (canExact) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC, targetMs, nextPrayerPi)
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC, targetMs, nextPrayerPi)
                    }
                }.onFailure {
                    runCatching {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC, targetMs, nextPrayerPi)
                    }
                }
            }
        }

        fun cancelScheduledUpdates(context: Context) {
            val appCtx = context.applicationContext
            val alarmManager = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val tickIntent = Intent(appCtx, PrayerTimesWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_TICK
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            runCatching {
                alarmManager.cancel(PendingIntent.getBroadcast(appCtx, REQUEST_CODE_PERIODIC_TICK, tickIntent, flags))
                alarmManager.cancel(PendingIntent.getBroadcast(appCtx, REQUEST_CODE_NEXT_PRAYER_TICK, tickIntent, flags))
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) return
        runCatching {
            val views = buildWidgetRemoteViews(context.applicationContext)
            appWidgetIds.forEach { widgetId ->
                runCatching { appWidgetManager.updateAppWidget(widgetId, views) }
            }
            scheduleNextUpdates(context.applicationContext)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        runCatching { scheduleNextUpdates(context.applicationContext) }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        runCatching { cancelScheduledUpdates(context.applicationContext) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        runCatching {
            super.onReceive(context, intent)
            when (intent.action) {
                ACTION_WIDGET_REFRESH,
                ACTION_WIDGET_TICK,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_DATE_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_LOCKED_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED -> {
                    updateAllWidgets(context)
                }
            }
        }
    }
}
