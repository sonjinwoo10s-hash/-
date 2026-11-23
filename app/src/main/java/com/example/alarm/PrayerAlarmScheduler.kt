package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.MainActivity
import com.example.receiver.AdhanReceiver
import java.util.Calendar
import java.util.TimeZone

object PrayerAlarmScheduler {

    const val EXTRA_PRAYER_KEY = "extra_prayer_key"
    const val EXTRA_PRAYER_NAME = "extra_prayer_name"
    const val EXTRA_PRAYER_TIME = "extra_prayer_time"
    const val EXTRA_OFFSET_MINUTES = "extra_offset_minutes"

    private const val SHOW_ALARM_INFO_REQUEST_CODE = 1099

    private val adhanRequestCodeMap = mapOf(
        "Fajr" to 1001,
        "Dhuhr" to 1002,
        "Asr" to 1003,
        "Maghrib" to 1004,
        "Isha" to 1005
    )

    private val preAdhanRequestCodeMap = mapOf(
        "Fajr" to 2001,
        "Dhuhr" to 2002,
        "Asr" to 2003,
        "Maghrib" to 2004,
        "Isha" to 2005
    )

    private val iqamaRequestCodeMap = mapOf(
        "Fajr" to 3001,
        "Dhuhr" to 3002,
        "Asr" to 3003,
        "Maghrib" to 3004,
        "Isha" to 3005
    )

    private val autoSilentStartRequestCodeMap = mapOf(
        "Fajr" to 4001,
        "Dhuhr" to 4002,
        "Asr" to 4003,
        "Maghrib" to 4004,
        "Isha" to 4005
    )

    private const val AUTO_SILENT_RESTORE_REQUEST_CODE = 4100
    private const val FRIDAY_KAHF_REQUEST_CODE = 5001

    /**
     * Schedules all 5 daily Adhan alarms using [AlarmManager.setAlarmClock] (guaranteed to bypass
     * Night Doze Mode and trigger at the exact second), plus Pre-Adhan reminders, Post-Adhan Iqama
     * alerts, optional Auto-Silent DND prayer windows, and the Friday 9:00 AM Surah Al-Kahf reminder.
     *
     * Always ensures today's timings are fresh for the current date, TimeZone, and DST offset first.
     */
    fun scheduleAllPrayers(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val nowMillis = System.currentTimeMillis()

        // Ensure timings are never stale across midnight, DST changes, or timezone shifts
        PrayerScheduleStore.ensureTodayTimingsFresh(appContext, nowMillis)
        val timings = PrayerScheduleStore.getTimings(appContext)
        val upcoming = PrayerScheduleStore.computeUpcomingPrayerTriggers(appContext, nowMillis)
        val alertSettings = PrayerScheduleStore.getPrayerAlertsSettings(appContext)

        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        // 1. Primary 5 daily Adhan alarms — use AlarmManager.setAlarmClock for exact-to-the-second
        // Doze-bypass wakeups (rolling chain reschedules again each time AdhanReceiver fires)
        for (prayer in upcoming) {
            val reqCode = adhanRequestCodeMap[prayer.key] ?: continue
            val intent = Intent(appContext, AdhanReceiver::class.java).apply {
                action = AdhanReceiver.ACTION_TRIGGER_ADHAN
                putExtra(EXTRA_PRAYER_KEY, prayer.key)
                putExtra(EXTRA_PRAYER_NAME, prayer.arabicName)
                putExtra(EXTRA_PRAYER_TIME, prayer.formatted12)
            }
            scheduleAlarmClockBroadcast(appContext, alarmManager, canExact, reqCode, intent, prayer.triggerAtMillis)
        }

        var tomorrowTimingsCache: Map<String, String>? = null
        fun tomorrowTimings(): Map<String, String> {
            return tomorrowTimingsCache
                ?: PrayerScheduleStore.computeTomorrowAdjustedTimings(appContext, nowMillis)
                    .also { tomorrowTimingsCache = it }
        }

        // 2. Pre-Adhan reminder alarms (default ON, 10 min before Adhan)
        if (alertSettings.preAdhanEnabled) {
            val offsetMin = alertSettings.preAdhanMinutes
            val offsetMs = offsetMin * 60_000L
            for ((key, arabicName) in PrayerScheduleStore.prayerOrder) {
                val reqCode = preAdhanRequestCodeMap[key] ?: continue
                val time24 = timings[key] ?: continue
                val todayPrayerMs = PrayerScheduleStore.parseTodayMillis(time24, nowMillis) ?: continue
                val todayPreMs = todayPrayerMs - offsetMs
                val (displayTime24, triggerAtMs) = if (todayPreMs <= nowMillis + 1500L) {
                    val tmrTime24 = tomorrowTimings()[key] ?: time24
                    val tmrPrayerMs = PrayerScheduleStore.parseTomorrowMillis(tmrTime24, nowMillis)
                        ?: Calendar.getInstance(TimeZone.getDefault()).apply {
                            timeInMillis = todayPrayerMs
                            add(Calendar.DAY_OF_YEAR, 1)
                        }.timeInMillis
                    tmrTime24 to (tmrPrayerMs - offsetMs)
                } else {
                    time24 to todayPreMs
                }
                val intent = Intent(appContext, AdhanReceiver::class.java).apply {
                    action = AdhanReceiver.ACTION_TRIGGER_PRE_ADHAN
                    putExtra(EXTRA_PRAYER_KEY, key)
                    putExtra(EXTRA_PRAYER_NAME, arabicName)
                    putExtra(EXTRA_PRAYER_TIME, PrayerScheduleStore.format12HourArabic(displayTime24))
                    putExtra(EXTRA_OFFSET_MINUTES, offsetMin)
                }
                scheduleExactBroadcast(appContext, alarmManager, canExact, reqCode, intent, triggerAtMs)
            }
        } else {
            cancelBroadcastGroup(appContext, alarmManager, preAdhanRequestCodeMap.values, AdhanReceiver.ACTION_TRIGGER_PRE_ADHAN)
        }

        // 3. Post-Adhan Iqama alarms (default ON; per-prayer minutes: Fajr 20, Dhuhr 15, Asr 15, Maghrib 10, Isha 15)
        if (alertSettings.iqamaEnabled) {
            for ((key, arabicName) in PrayerScheduleStore.prayerOrder) {
                val reqCode = iqamaRequestCodeMap[key] ?: continue
                val time24 = timings[key] ?: continue
                val iqamaMin = alertSettings.iqamaMinutesFor(key)
                val iqamaOffsetMs = iqamaMin * 60_000L
                val todayPrayerMs = PrayerScheduleStore.parseTodayMillis(time24, nowMillis) ?: continue
                val todayIqamaMs = todayPrayerMs + iqamaOffsetMs
                val (displayTime24, triggerAtMs) = if (todayIqamaMs <= nowMillis + 1500L) {
                    val tmrTime24 = tomorrowTimings()[key] ?: time24
                    val tmrPrayerMs = PrayerScheduleStore.parseTomorrowMillis(tmrTime24, nowMillis)
                        ?: Calendar.getInstance(TimeZone.getDefault()).apply {
                            timeInMillis = todayPrayerMs
                            add(Calendar.DAY_OF_YEAR, 1)
                        }.timeInMillis
                    tmrTime24 to (tmrPrayerMs + iqamaOffsetMs)
                } else {
                    time24 to todayIqamaMs
                }
                val intent = Intent(appContext, AdhanReceiver::class.java).apply {
                    action = AdhanReceiver.ACTION_TRIGGER_IQAMA
                    putExtra(EXTRA_PRAYER_KEY, key)
                    putExtra(EXTRA_PRAYER_NAME, arabicName)
                    putExtra(EXTRA_PRAYER_TIME, PrayerScheduleStore.format12HourArabic(displayTime24))
                    putExtra(EXTRA_OFFSET_MINUTES, iqamaMin)
                }
                scheduleExactBroadcast(appContext, alarmManager, canExact, reqCode, intent, triggerAtMs)
            }
        } else {
            cancelBroadcastGroup(appContext, alarmManager, iqamaRequestCodeMap.values, AdhanReceiver.ACTION_TRIGGER_IQAMA)
        }

        // 4. Optional Auto-Silent DND Mode During Prayer (OFF by default)
        if (PrayerScheduleStore.isAutoSilentEnabled(appContext) &&
            PrayerScheduleStore.isDndPermissionGranted(appContext)
        ) {
            val atIqama = PrayerScheduleStore.isAutoSilentAtIqama(appContext)
            val durationMin = PrayerScheduleStore.getAutoSilentDurationMinutes(appContext)
            for ((key, arabicName) in PrayerScheduleStore.prayerOrder) {
                val reqCode = autoSilentStartRequestCodeMap[key] ?: continue
                val time24 = timings[key] ?: continue
                val todayPrayerMs = PrayerScheduleStore.parseTodayMillis(time24, nowMillis) ?: continue
                val extraOffsetMs = if (atIqama) alertSettings.iqamaMinutesFor(key) * 60_000L else 0L
                val todayTriggerMs = todayPrayerMs + extraOffsetMs
                val triggerAtMs = if (todayTriggerMs <= nowMillis + 1500L) {
                    val tmrTime24 = tomorrowTimings()[key] ?: time24
                    val tmrPrayerMs = PrayerScheduleStore.parseTomorrowMillis(tmrTime24, nowMillis)
                        ?: Calendar.getInstance(TimeZone.getDefault()).apply {
                            timeInMillis = todayPrayerMs
                            add(Calendar.DAY_OF_YEAR, 1)
                        }.timeInMillis
                    tmrPrayerMs + extraOffsetMs
                } else {
                    todayTriggerMs
                }
                val intent = Intent(appContext, AdhanReceiver::class.java).apply {
                    action = AdhanReceiver.ACTION_AUTO_SILENT_START
                    putExtra(EXTRA_PRAYER_KEY, key)
                    putExtra(EXTRA_PRAYER_NAME, arabicName)
                    putExtra(EXTRA_OFFSET_MINUTES, durationMin)
                }
                scheduleExactBroadcast(appContext, alarmManager, canExact, reqCode, intent, triggerAtMs)
            }
        } else {
            cancelBroadcastGroup(
                appContext,
                alarmManager,
                autoSilentStartRequestCodeMap.values,
                AdhanReceiver.ACTION_AUTO_SILENT_START
            )
        }

        // 5. Friday Surah Al-Kahf Reminder (ON by default, every Friday at 9:00 AM)
        scheduleFridayKahfReminder(appContext, alarmManager, canExact, nowMillis)
    }

    fun scheduleAutoSilentRestore(context: Context, durationMinutes: Int) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
        val restoreAtMs = System.currentTimeMillis() + durationMinutes.coerceIn(5, 60) * 60_000L
        val intent = Intent(appContext, AdhanReceiver::class.java).apply {
            action = AdhanReceiver.ACTION_AUTO_SILENT_RESTORE
        }
        scheduleExactBroadcast(appContext, alarmManager, canExact, AUTO_SILENT_RESTORE_REQUEST_CODE, intent, restoreAtMs)
    }

    private fun scheduleFridayKahfReminder(
        appContext: Context,
        alarmManager: AlarmManager,
        canExact: Boolean,
        nowMillis: Long
    ) {
        if (!PrayerScheduleStore.isFridayKahfReminderEnabled(appContext)) {
            cancelBroadcastGroup(
                appContext,
                alarmManager,
                listOf(FRIDAY_KAHF_REQUEST_CODE),
                AdhanReceiver.ACTION_TRIGGER_FRIDAY_KAHF
            )
            return
        }

        val target = Calendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            var daysUntilFriday = (Calendar.FRIDAY - get(Calendar.DAY_OF_WEEK) + 7) % 7
            if (daysUntilFriday == 0 && timeInMillis <= nowMillis + 15_000L) {
                daysUntilFriday = 7
            }
            add(Calendar.DAY_OF_YEAR, daysUntilFriday)
        }

        val intent = Intent(appContext, AdhanReceiver::class.java).apply {
            action = AdhanReceiver.ACTION_TRIGGER_FRIDAY_KAHF
        }
        scheduleExactBroadcast(appContext, alarmManager, canExact, FRIDAY_KAHF_REQUEST_CODE, intent, target.timeInMillis)
    }

    fun cancelAllPrayers(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        cancelBroadcastGroup(appContext, alarmManager, adhanRequestCodeMap.values, AdhanReceiver.ACTION_TRIGGER_ADHAN)
        cancelBroadcastGroup(appContext, alarmManager, preAdhanRequestCodeMap.values, AdhanReceiver.ACTION_TRIGGER_PRE_ADHAN)
        cancelBroadcastGroup(appContext, alarmManager, iqamaRequestCodeMap.values, AdhanReceiver.ACTION_TRIGGER_IQAMA)
        cancelBroadcastGroup(appContext, alarmManager, autoSilentStartRequestCodeMap.values, AdhanReceiver.ACTION_AUTO_SILENT_START)
        cancelBroadcastGroup(appContext, alarmManager, listOf(AUTO_SILENT_RESTORE_REQUEST_CODE), AdhanReceiver.ACTION_AUTO_SILENT_RESTORE)
        cancelBroadcastGroup(appContext, alarmManager, listOf(FRIDAY_KAHF_REQUEST_CODE), AdhanReceiver.ACTION_TRIGGER_FRIDAY_KAHF)
    }

    /**
     * Uses [AlarmManager.setAlarmClock], the ONLY Android alarm API guaranteed to bypass
     * Night Doze Mode and fire at the exact second even when the screen is off or the app
     * has been swiped from Recent Apps.
     */
    private fun scheduleAlarmClockBroadcast(
        appContext: Context,
        alarmManager: AlarmManager,
        canExact: Boolean,
        requestCode: Int,
        intent: Intent,
        triggerAtMillis: Long
    ) {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(appContext, requestCode, intent, flags)
        val showAppIntent = Intent(appContext, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val showPendingIntent = PendingIntent.getActivity(
            appContext,
            SHOW_ALARM_INFO_REQUEST_CODE,
            showAppIntent,
            flags
        )

        try {
            if (canExact) {
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            runCatching {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }
    }

    private fun scheduleExactBroadcast(
        appContext: Context,
        alarmManager: AlarmManager,
        canExact: Boolean,
        requestCode: Int,
        intent: Intent,
        triggerAtMillis: Long
    ) {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(appContext, requestCode, intent, flags)
        try {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            runCatching {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }
    }

    private fun cancelBroadcastGroup(
        appContext: Context,
        alarmManager: AlarmManager,
        requestCodes: Collection<Int>,
        action: String
    ) {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        requestCodes.forEach { reqCode ->
            val intent = Intent(appContext, AdhanReceiver::class.java).apply {
                this.action = action
            }
            val pi = PendingIntent.getBroadcast(appContext, reqCode, intent, flags)
            alarmManager.cancel(pi)
        }
    }
}
