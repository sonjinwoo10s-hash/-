package com.example.alarm

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.example.data.local.MuezzinLibraryData
import com.example.data.local.OfflineIslamicData
import com.example.data.model.MuezzinItem
import java.util.Calendar
import java.util.Locale

data class ScheduledPrayerInfo(
    val key: String,
    val arabicName: String,
    val time24: String,
    val formatted12: String,
    val triggerAtMillis: Long
)

data class NextPrayerCountdownInfo(
    val key: String,
    val arabicName: String,
    val formatted12: String,
    val remainingHours: Int,
    val remainingMinutes: Int
)

data class AdhanAudioConfig(
    val muezzinId: String,
    val muezzinName: String,
    val localFilePath: String?,
    val remoteUrl: String?
)

data class AppPermissionsStatus(
    val notificationsGranted: Boolean = false,
    val exactAlarmsGranted: Boolean = false,
    val batteryOptimizationIgnored: Boolean = false,
    val locationGranted: Boolean = false
) {
    val allGranted: Boolean
        get() = notificationsGranted && exactAlarmsGranted && batteryOptimizationIgnored && locationGranted
}

object PermissionsHelper {

    fun checkAll(context: Context): AppPermissionsStatus {
        return AppPermissionsStatus(
            notificationsGranted = isNotificationGranted(context),
            exactAlarmsGranted = isExactAlarmGranted(context),
            batteryOptimizationIgnored = isBatteryOptimizationIgnored(context),
            locationGranted = isLocationGranted(context)
        )
    }

    fun isNotificationGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun isExactAlarmGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            am?.canScheduleExactAlarms() == true
        } else {
            true
        }
    }

    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.isIgnoringBatteryOptimizations(context.packageName) == true
        } else {
            true
        }
    }

    fun isLocationGranted(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }
}

object OnboardingPrefs {
    private const val PREFS_NAME = "rafiq_permissions_prefs"
    private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

    fun isCompleted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setCompleted(context: Context, completed: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }
}

/**
 * Synchronous SharedPreferences mirror of today's prayer timings, selected Muezzins,
 * and countdown notification toggle so BroadcastReceivers and ForegroundServices
 * can operate reliably even after a cold boot or process kill.
 */
object PrayerScheduleStore {

    private const val PREFS_NAME = "rafiq_prayer_schedule_store"

    private const val KEY_FAJR = "timing_Fajr"
    private const val KEY_DHUHR = "timing_Dhuhr"
    private const val KEY_ASR = "timing_Asr"
    private const val KEY_MAGHRIB = "timing_Maghrib"
    private const val KEY_ISHA = "timing_Isha"

    private const val KEY_DEFAULT_MUEZZIN_ID = "default_muezzin_id"
    private const val KEY_DEFAULT_MUEZZIN_NAME = "default_muezzin_name"
    private const val KEY_DEFAULT_MUEZZIN_URL = "default_muezzin_url"
    private const val KEY_DEFAULT_MUEZZIN_LOCAL = "default_muezzin_local"

    private const val KEY_FAJR_MUEZZIN_ID = "fajr_muezzin_id"
    private const val KEY_FAJR_MUEZZIN_NAME = "fajr_muezzin_name"
    private const val KEY_FAJR_MUEZZIN_URL = "fajr_muezzin_url"
    private const val KEY_FAJR_MUEZZIN_LOCAL = "fajr_muezzin_local"
    private const val KEY_USE_DEDICATED_FAJR = "use_dedicated_fajr"

    private const val KEY_COUNTDOWN_ENABLED = "countdown_notification_enabled"
    private const val KEY_ADHAN_SILENCE_ON_VOLUME = "adhan_silence_on_volume"
    private const val KEY_ADHAN_SILENCE_ON_FLIP = "adhan_silence_on_flip"

    val prayerOrder: List<Pair<String, String>> = listOf(
        "Fajr" to "الفجر",
        "Dhuhr" to "الظهر",
        "Asr" to "العصر",
        "Maghrib" to "المغرب",
        "Isha" to "العشاء"
    )

    fun saveTimings(context: Context, timings24: Map<String, String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_FAJR, timings24["Fajr"] ?: OfflineIslamicData.fallbackTimings24["Fajr"])
            .putString(KEY_DHUHR, timings24["Dhuhr"] ?: OfflineIslamicData.fallbackTimings24["Dhuhr"])
            .putString(KEY_ASR, timings24["Asr"] ?: OfflineIslamicData.fallbackTimings24["Asr"])
            .putString(KEY_MAGHRIB, timings24["Maghrib"] ?: OfflineIslamicData.fallbackTimings24["Maghrib"])
            .putString(KEY_ISHA, timings24["Isha"] ?: OfflineIslamicData.fallbackTimings24["Isha"])
            .apply()
    }

    fun getTimings(context: Context): Map<String, String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val fallback = OfflineIslamicData.fallbackTimings24
        return mapOf(
            "Fajr" to (prefs.getString(KEY_FAJR, fallback["Fajr"]) ?: "04:55"),
            "Dhuhr" to (prefs.getString(KEY_DHUHR, fallback["Dhuhr"]) ?: "11:50"),
            "Asr" to (prefs.getString(KEY_ASR, fallback["Asr"]) ?: "14:42"),
            "Maghrib" to (prefs.getString(KEY_MAGHRIB, fallback["Maghrib"]) ?: "17:02"),
            "Isha" to (prefs.getString(KEY_ISHA, fallback["Isha"]) ?: "18:22")
        )
    }

    fun saveMuezzinSelection(
        context: Context,
        defaultMuezzin: MuezzinItem,
        fajrMuezzin: MuezzinItem,
        useDedicatedFajr: Boolean
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_DEFAULT_MUEZZIN_ID, defaultMuezzin.id)
            .putString(KEY_DEFAULT_MUEZZIN_NAME, defaultMuezzin.name)
            .putString(KEY_DEFAULT_MUEZZIN_URL, defaultMuezzin.primaryUrl)
            .putString(KEY_DEFAULT_MUEZZIN_LOCAL, defaultMuezzin.localFilePath)
            .putString(KEY_FAJR_MUEZZIN_ID, fajrMuezzin.id)
            .putString(KEY_FAJR_MUEZZIN_NAME, fajrMuezzin.name)
            .putString(KEY_FAJR_MUEZZIN_URL, fajrMuezzin.primaryUrl)
            .putString(KEY_FAJR_MUEZZIN_LOCAL, fajrMuezzin.localFilePath)
            .putBoolean(KEY_USE_DEDICATED_FAJR, useDedicatedFajr)
            .apply()
    }

    fun getAdhanAudioForPrayer(context: Context, prayerKey: String): AdhanAudioConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val useDedicatedFajr = prefs.getBoolean(KEY_USE_DEDICATED_FAJR, true)
        val defaultBuiltIn = MuezzinLibraryData.builtInMuezzins.first()

        val defaultConfig = AdhanAudioConfig(
            muezzinId = prefs.getString(KEY_DEFAULT_MUEZZIN_ID, defaultBuiltIn.id) ?: defaultBuiltIn.id,
            muezzinName = prefs.getString(KEY_DEFAULT_MUEZZIN_NAME, defaultBuiltIn.name) ?: defaultBuiltIn.name,
            localFilePath = prefs.getString(KEY_DEFAULT_MUEZZIN_LOCAL, null),
            remoteUrl = prefs.getString(KEY_DEFAULT_MUEZZIN_URL, defaultBuiltIn.primaryUrl)
        )

        if (prayerKey.equals("Fajr", ignoreCase = true) && useDedicatedFajr) {
            val fajrId = prefs.getString(KEY_FAJR_MUEZZIN_ID, null)
            val fajrName = prefs.getString(KEY_FAJR_MUEZZIN_NAME, null)
            val fajrLocal = prefs.getString(KEY_FAJR_MUEZZIN_LOCAL, null)
            val fajrUrl = prefs.getString(KEY_FAJR_MUEZZIN_URL, null)
            if (!fajrLocal.isNullOrBlank() || !fajrUrl.isNullOrBlank() || !fajrId.isNullOrBlank()) {
                return AdhanAudioConfig(
                    muezzinId = fajrId ?: defaultConfig.muezzinId,
                    muezzinName = fajrName ?: defaultConfig.muezzinName,
                    localFilePath = fajrLocal,
                    remoteUrl = fajrUrl
                )
            }
        }
        return defaultConfig
    }

    fun isCountdownNotificationEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_COUNTDOWN_ENABLED, true)
    }

    fun setCountdownNotificationEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_COUNTDOWN_ENABLED, enabled).apply()
    }

    fun isAdhanSilenceOnVolumeEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ADHAN_SILENCE_ON_VOLUME, true)
    }

    fun setAdhanSilenceOnVolumeEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ADHAN_SILENCE_ON_VOLUME, enabled).apply()
    }

    fun isAdhanSilenceOnFlipEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ADHAN_SILENCE_ON_FLIP, true)
    }

    fun setAdhanSilenceOnFlipEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ADHAN_SILENCE_ON_FLIP, enabled).apply()
    }

    /**
     * Parses a 24h "HH:mm" string into today's timestamp in milliseconds (seconds/millis = 0).
     */
    fun parseTodayMillis(time24: String, nowMillis: Long = System.currentTimeMillis()): Long? {
        val parts = time24.trim().split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: return null
        return Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /**
     * Builds the next upcoming trigger timestamp for each of the 5 daily prayers.
     * If today's time has already passed (<= now), rolls forward by 1 day to tomorrow.
     */
    fun computeUpcomingPrayerTriggers(
        context: Context,
        nowMillis: Long = System.currentTimeMillis()
    ): List<ScheduledPrayerInfo> {
        val timings = getTimings(context)
        return prayerOrder.mapNotNull { (key, arabicName) ->
            val time24 = timings[key] ?: return@mapNotNull null
            val todayMillis = parseTodayMillis(time24, nowMillis) ?: return@mapNotNull null
            val triggerMillis = if (todayMillis <= nowMillis + 1500L) {
                Calendar.getInstance().apply {
                    timeInMillis = todayMillis
                    add(Calendar.DAY_OF_YEAR, 1)
                }.timeInMillis
            } else {
                todayMillis
            }
            ScheduledPrayerInfo(
                key = key,
                arabicName = arabicName,
                time24 = time24,
                formatted12 = format12HourArabic(time24),
                triggerAtMillis = triggerMillis
            )
        }
    }

    /**
     * Calculates the very next prayer and remaining hours/minutes for the persistent status bar notification.
     */
    fun computeNextPrayerCountdown(
        context: Context,
        nowMillis: Long = System.currentTimeMillis()
    ): NextPrayerCountdownInfo {
        val upcoming = computeUpcomingPrayerTriggers(context, nowMillis)
        val next = upcoming.minByOrNull { it.triggerAtMillis }
            ?: ScheduledPrayerInfo("Fajr", "الفجر", "04:55", "4:55 ص", nowMillis + 3600_000L)

        val diffMillis = (next.triggerAtMillis - nowMillis).coerceAtLeast(0L)
        val totalMinutes = ((diffMillis + 59_999L) / 60_000L).toInt().coerceAtLeast(0)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return NextPrayerCountdownInfo(
            key = next.key,
            arabicName = next.arabicName,
            formatted12 = next.formatted12,
            remainingHours = hours,
            remainingMinutes = minutes
        )
    }

    fun format12HourArabic(time24: String): String {
        val parts = time24.trim().split(":")
        val h24 = parts.getOrNull(0)?.toIntOrNull() ?: return "--:--"
        val min = parts.getOrNull(1)?.padStart(2, '0') ?: "00"
        val suffix = if (h24 >= 12) "م" else "ص"
        val h12 = if (h24 % 12 == 0) 12 else h24 % 12
        return String.format(Locale.US, "%d:%s %s", h12, min, suffix)
    }
}
