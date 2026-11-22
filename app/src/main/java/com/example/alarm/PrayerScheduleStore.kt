package com.example.alarm

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.example.data.local.MuezzinLibraryData
import com.example.data.local.OfflineIslamicData
import com.example.data.local.safeGetBoolean
import com.example.data.local.safeGetInt
import com.example.data.local.safeGetString
import com.example.data.model.ActiveIqamaCountdown
import com.example.data.model.MuezzinItem
import com.example.data.model.PrayerAlertsSettings
import com.example.data.repository.GlobalLocationConfig
import android.icu.util.IslamicCalendar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class AdjustedHijriInfo(
    val formattedDate: String,
    val hijriDay: Int,
    val hijriMonth: Int,
    val hijriYear: Int,
    val dayOfWeek: Int,
    val blessedDayBadge: String?
)

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

data class IslamicNightTimesInfo(
    val todayMaghribMs: Long,
    val tomorrowFajrMs: Long,
    val nightDurationMs: Long,
    val islamicMidnightMs: Long,
    val lastThirdNightMs: Long,
    val islamicMidnightFormatted: String,
    val lastThirdNightFormatted: String
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
        return prefs.safeGetBoolean(KEY_ONBOARDING_COMPLETED, false)
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
    private const val KEY_TIMINGS_DATE_STAMP = "timing_date_stamp"

    private const val KEY_DEFAULT_MUEZZIN_ID = "default_muezzin_id"
    private const val KEY_DEFAULT_MUEZZIN_NAME = "default_muezzin_name"
    private const val KEY_DEFAULT_MUEZZIN_URL = "default_muezzin_url"
    private const val KEY_DEFAULT_MUEZZIN_LOCAL = "default_muezzin_local"

    private const val KEY_FAJR_MUEZZIN_ID = "fajr_muezzin_id"
    private const val KEY_FAJR_MUEZZIN_NAME = "fajr_muezzin_name"
    private const val KEY_FAJR_MUEZZIN_URL = "fajr_muezzin_url"
    private const val KEY_FAJR_MUEZZIN_LOCAL = "fajr_muezzin_local"
    private const val KEY_USE_DEDICATED_FAJR = "use_dedicated_fajr"

    // Optional Per-Prayer Muezzin mode (OFF by default -> unified Muezzin for all 5 prayers)
    private const val KEY_PER_PRAYER_MUEZZIN_ENABLED = "per_prayer_muezzin_enabled"
    private const val KEY_PRAYER_MUEZZIN_ID_PREFIX = "muezzin_id_"
    private const val KEY_PRAYER_MUEZZIN_NAME_PREFIX = "muezzin_name_"
    private const val KEY_PRAYER_MUEZZIN_URL_PREFIX = "muezzin_url_"
    private const val KEY_PRAYER_MUEZZIN_LOCAL_PREFIX = "muezzin_local_"

    private const val KEY_COUNTDOWN_ENABLED = "countdown_notification_enabled"
    private const val KEY_ADHAN_SILENCE_ON_VOLUME = "adhan_silence_on_volume"
    private const val KEY_ADHAN_SILENCE_ON_FLIP = "adhan_silence_on_flip"
    private const val KEY_HIJRI_DATE = "cached_hijri_date"
    private const val KEY_HIJRI_DAY_STAMP = "cached_hijri_day_stamp"
    private const val KEY_CITY_LABEL = "cached_city_label"

    // Pre-Adhan Reminder & Post-Adhan Iqama Alert Preferences (both ON by default)
    private const val KEY_PRE_ADHAN_ENABLED = "pre_adhan_enabled"
    private const val KEY_PRE_ADHAN_MINUTES = "pre_adhan_minutes"
    private const val KEY_PRE_ADHAN_SOUND_ID = "pre_adhan_sound_id"
    private const val KEY_PRE_ADHAN_SOUND_NAME = "pre_adhan_sound_name"
    private const val KEY_PRE_ADHAN_SOUND_URL = "pre_adhan_sound_url"
    private const val KEY_PRE_ADHAN_IS_PER_PRAYER = "pre_adhan_is_per_prayer"
    private const val KEY_PRE_ADHAN_URL_PREFIX = "pre_adhan_url_"
    private const val KEY_PRE_ADHAN_SOUND_USER_SET = "pre_adhan_sound_user_set"

    private const val KEY_IQAMA_ENABLED = "iqama_enabled"
    private const val KEY_IQAMA_MIN_PREFIX = "iqama_minutes_"
    private const val KEY_IQAMA_SOUND_ID = "iqama_sound_id"
    private const val KEY_IQAMA_SOUND_NAME = "iqama_sound_name"
    private const val KEY_IQAMA_SOUND_URL = "iqama_sound_url"
    private const val KEY_IQAMA_SOUND_USER_SET = "iqama_sound_user_set"

    // Global Auto-Location & Calculation Method Preferences
    private const val KEY_LOC_LAT = "global_loc_lat"
    private const val KEY_LOC_LNG = "global_loc_lng"
    private const val KEY_LOC_LABEL = "global_loc_label"
    private const val KEY_LOC_COUNTRY_CODE = "global_loc_country_code"
    private const val KEY_LOC_COUNTRY_NAME = "global_loc_country_name"
    private const val KEY_LOC_CITY_NAME = "global_loc_city_name"
    private const val KEY_LOC_METHOD_ID = "global_loc_method_id"
    private const val KEY_LOC_METHOD_MANUAL = "global_loc_method_manual"

    // Hijri Date Adjustment (-2..+2 days, default 0)
    private const val KEY_HIJRI_DAY_OFFSET = "hijri_day_offset"

    // Auto-Silent Mode During Prayer (OFF by default)
    private const val KEY_AUTO_SILENT_ENABLED = "auto_silent_enabled"
    private const val KEY_AUTO_SILENT_AT_IQAMA = "auto_silent_at_iqama"
    private const val KEY_AUTO_SILENT_DURATION_MIN = "auto_silent_duration_min"
    private const val KEY_PRE_SILENT_RINGER = "pre_silent_ringer_mode"
    private const val KEY_PRE_SILENT_FILTER = "pre_silent_interruption_filter"
    private const val KEY_PRE_SILENT_ACTIVE = "pre_silent_active"

    // Friday Surah Al-Kahf Reminder (ON by default)
    private const val KEY_FRIDAY_KAHF_ENABLED = "friday_kahf_reminder_enabled"

    val AUTO_SILENT_DURATION_OPTIONS: List<Int> = listOf(10, 15, 20, 30)

    private val hijriMonthsArabic = arrayOf(
        "محرم",
        "صفر",
        "ربيع الأول",
        "ربيع الآخر",
        "جمادى الأولى",
        "جمادى الآخرة",
        "رجب",
        "شعبان",
        "رمضان",
        "شوال",
        "ذو القعدة",
        "ذو الحجة"
    )

    private val arabicWeekdays = arrayOf(
        "",
        "الأحد",
        "الإثنين",
        "الثلاثاء",
        "الأربعاء",
        "الخميس",
        "الجمعة",
        "السبت"
    )

    val prayerOrder: List<Pair<String, String>> = listOf(
        "Fajr" to "الفجر",
        "Dhuhr" to "الظهر",
        "Asr" to "العصر",
        "Maghrib" to "المغرب",
        "Isha" to "العشاء"
    )

    fun saveTimings(context: Context, timings24: Map<String, String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todayStamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        prefs.edit()
            .putString(KEY_FAJR, timings24["Fajr"] ?: OfflineIslamicData.fallbackTimings24["Fajr"])
            .putString(KEY_DHUHR, timings24["Dhuhr"] ?: OfflineIslamicData.fallbackTimings24["Dhuhr"])
            .putString(KEY_ASR, timings24["Asr"] ?: OfflineIslamicData.fallbackTimings24["Asr"])
            .putString(KEY_MAGHRIB, timings24["Maghrib"] ?: OfflineIslamicData.fallbackTimings24["Maghrib"])
            .putString(KEY_ISHA, timings24["Isha"] ?: OfflineIslamicData.fallbackTimings24["Isha"])
            .putString(KEY_TIMINGS_DATE_STAMP, todayStamp)
            .apply()
    }

    /**
     * If the stored timings belong to a previous date (e.g., midnight rolled over while the app was
     * closed), recomputes today's exact prayer times offline from the saved global coordinates and
     * calculation method so widgets and background services stay accurate 100% offline.
     */
    fun ensureTodayTimingsFresh(context: Context, nowMillis: Long = System.currentTimeMillis()) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todayStamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMillis))
        val savedStamp = prefs.safeGetString(KEY_TIMINGS_DATE_STAMP, null)
        if (savedStamp == todayStamp) return
        val loc = getGlobalLocation(context)
        val method = PrayerCalculationMethod.fromId(loc.calculationMethodId)
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val fresh = AstronomicalPrayerCalculator.calculateTimings24(
            latitude = loc.lat,
            longitude = loc.lng,
            dateCalendar = cal,
            method = method
        )
        saveTimings(context, fresh)
    }

    fun getTimings(context: Context): Map<String, String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val fallback = OfflineIslamicData.fallbackTimings24
        return mapOf(
            "Fajr" to (prefs.safeGetString(KEY_FAJR, fallback["Fajr"]) ?: "04:55"),
            "Dhuhr" to (prefs.safeGetString(KEY_DHUHR, fallback["Dhuhr"]) ?: "11:50"),
            "Asr" to (prefs.safeGetString(KEY_ASR, fallback["Asr"]) ?: "14:42"),
            "Maghrib" to (prefs.safeGetString(KEY_MAGHRIB, fallback["Maghrib"]) ?: "17:02"),
            "Isha" to (prefs.safeGetString(KEY_ISHA, fallback["Isha"]) ?: "18:22")
        )
    }

    fun getDefaultMuezzinId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.safeGetString(KEY_DEFAULT_MUEZZIN_ID, null)?.takeIf { it.isNotBlank() }
            ?: MuezzinLibraryData.defaultMuezzinId
    }

    fun getDefaultMuezzinSnapshot(context: Context): AdhanAudioConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaultBuiltIn = MuezzinLibraryData.builtInMuezzins.first()
        return AdhanAudioConfig(
            muezzinId = prefs.safeGetString(KEY_DEFAULT_MUEZZIN_ID, null)?.takeIf { it.isNotBlank() } ?: defaultBuiltIn.id,
            muezzinName = prefs.safeGetString(KEY_DEFAULT_MUEZZIN_NAME, null)?.takeIf { it.isNotBlank() } ?: defaultBuiltIn.name,
            localFilePath = prefs.safeGetString(KEY_DEFAULT_MUEZZIN_LOCAL, null),
            remoteUrl = prefs.safeGetString(KEY_DEFAULT_MUEZZIN_URL, defaultBuiltIn.primaryUrl)
        )
    }

    fun getFajrMuezzinId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.safeGetString(KEY_FAJR_MUEZZIN_ID, null)?.takeIf { it.isNotBlank() }
            ?: MuezzinLibraryData.defaultFajrMuezzinId
    }

    fun isUseDedicatedFajr(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.safeGetBoolean(KEY_USE_DEDICATED_FAJR, false)
    }

    fun setUseDedicatedFajr(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_USE_DEDICATED_FAJR, enabled).apply()
    }

    fun isPerPrayerMuezzinEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.safeGetBoolean(KEY_PER_PRAYER_MUEZZIN_ENABLED, false)
    }

    fun setPerPrayerMuezzinEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_PER_PRAYER_MUEZZIN_ENABLED, enabled).apply()
    }

    fun saveDefaultMuezzin(context: Context, muezzin: MuezzinItem) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_DEFAULT_MUEZZIN_ID, muezzin.id)
            .putString(KEY_DEFAULT_MUEZZIN_NAME, muezzin.name)
            .putString(KEY_DEFAULT_MUEZZIN_URL, muezzin.primaryUrl)
            .putString(KEY_DEFAULT_MUEZZIN_LOCAL, muezzin.localFilePath)
            .apply()
    }

    fun saveFajrMuezzin(context: Context, muezzin: MuezzinItem) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_FAJR_MUEZZIN_ID, muezzin.id)
            .putString(KEY_FAJR_MUEZZIN_NAME, muezzin.name)
            .putString(KEY_FAJR_MUEZZIN_URL, muezzin.primaryUrl)
            .putString(KEY_FAJR_MUEZZIN_LOCAL, muezzin.localFilePath)
            .apply()
    }

    fun saveMuezzinForPrayer(context: Context, prayerKey: String, muezzin: MuezzinItem) {
        val normKey = normalizePrayerKey(prayerKey)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_PRAYER_MUEZZIN_ID_PREFIX + normKey, muezzin.id)
            .putString(KEY_PRAYER_MUEZZIN_NAME_PREFIX + normKey, muezzin.name)
            .putString(KEY_PRAYER_MUEZZIN_URL_PREFIX + normKey, muezzin.primaryUrl)
            .putString(KEY_PRAYER_MUEZZIN_LOCAL_PREFIX + normKey, muezzin.localFilePath)
            .apply()
    }

    fun getPerPrayerMuezzinIds(context: Context): Map<String, String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val fallbackId = getDefaultMuezzinId(context)
        return prayerOrder.associate { (key, _) ->
            val normKey = normalizePrayerKey(key)
            val stored = prefs.safeGetString(KEY_PRAYER_MUEZZIN_ID_PREFIX + normKey, null)
                ?.takeIf { it.isNotBlank() }
                ?: if (key == "Fajr" && isUseDedicatedFajr(context)) getFajrMuezzinId(context) else fallbackId
            key to stored
        }
    }

    /**
     * Updates cached metadata (name/url/localPath) for an already-selected Muezzin ID ONLY if that
     * exact ID is currently selected (never alters the selected ID itself).
     */
    fun refreshSavedAudioMetadataIfSelected(context: Context, muezzin: MuezzinItem) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        var changed = false
        if (prefs.safeGetString(KEY_DEFAULT_MUEZZIN_ID, null) == muezzin.id) {
            editor.putString(KEY_DEFAULT_MUEZZIN_NAME, muezzin.name)
            editor.putString(KEY_DEFAULT_MUEZZIN_URL, muezzin.primaryUrl)
            editor.putString(KEY_DEFAULT_MUEZZIN_LOCAL, muezzin.localFilePath)
            changed = true
        }
        if (prefs.safeGetString(KEY_FAJR_MUEZZIN_ID, null) == muezzin.id) {
            editor.putString(KEY_FAJR_MUEZZIN_NAME, muezzin.name)
            editor.putString(KEY_FAJR_MUEZZIN_URL, muezzin.primaryUrl)
            editor.putString(KEY_FAJR_MUEZZIN_LOCAL, muezzin.localFilePath)
            changed = true
        }
        for ((key, _) in prayerOrder) {
            val normKey = normalizePrayerKey(key)
            if (prefs.safeGetString(KEY_PRAYER_MUEZZIN_ID_PREFIX + normKey, null) == muezzin.id) {
                editor.putString(KEY_PRAYER_MUEZZIN_NAME_PREFIX + normKey, muezzin.name)
                editor.putString(KEY_PRAYER_MUEZZIN_URL_PREFIX + normKey, muezzin.primaryUrl)
                editor.putString(KEY_PRAYER_MUEZZIN_LOCAL_PREFIX + normKey, muezzin.localFilePath)
                changed = true
            }
        }
        if (changed) editor.apply()
    }

    fun saveMuezzinSelection(
        context: Context,
        defaultMuezzin: MuezzinItem,
        fajrMuezzin: MuezzinItem,
        useDedicatedFajr: Boolean
    ) {
        saveDefaultMuezzin(context, defaultMuezzin)
        saveFajrMuezzin(context, fajrMuezzin)
        setUseDedicatedFajr(context, useDedicatedFajr)
    }

    private fun normalizePrayerKey(prayerKey: String): String {
        return prayerKey.trim().lowercase(Locale.US)
    }

    fun getAdhanAudioForPrayer(context: Context, prayerKey: String): AdhanAudioConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaultConfig = getDefaultMuezzinSnapshot(context)

        // 1. Optional Per-Prayer Muezzin Mode (when enabled by user)
        if (isPerPrayerMuezzinEnabled(context)) {
            val normKey = normalizePrayerKey(prayerKey)
            val pId = prefs.safeGetString(KEY_PRAYER_MUEZZIN_ID_PREFIX + normKey, null)
            val pName = prefs.safeGetString(KEY_PRAYER_MUEZZIN_NAME_PREFIX + normKey, null)
            val pLocal = prefs.safeGetString(KEY_PRAYER_MUEZZIN_LOCAL_PREFIX + normKey, null)
            val pUrl = prefs.safeGetString(KEY_PRAYER_MUEZZIN_URL_PREFIX + normKey, null)
            if (!pId.isNullOrBlank()) {
                return AdhanAudioConfig(
                    muezzinId = pId,
                    muezzinName = pName?.takeIf { it.isNotBlank() } ?: defaultConfig.muezzinName,
                    localFilePath = pLocal,
                    remoteUrl = pUrl
                )
            }
        }

        // 2. Legacy/dedicated Fajr Adhan (when enabled and not in full per-prayer mode)
        val useDedicatedFajr = prefs.safeGetBoolean(KEY_USE_DEDICATED_FAJR, false)
        if (prayerKey.equals("Fajr", ignoreCase = true) && useDedicatedFajr) {
            val fajrId = prefs.safeGetString(KEY_FAJR_MUEZZIN_ID, null)
            val fajrName = prefs.safeGetString(KEY_FAJR_MUEZZIN_NAME, null)
            val fajrLocal = prefs.safeGetString(KEY_FAJR_MUEZZIN_LOCAL, null)
            val fajrUrl = prefs.safeGetString(KEY_FAJR_MUEZZIN_URL, null)
            if (!fajrId.isNullOrBlank()) {
                return AdhanAudioConfig(
                    muezzinId = fajrId,
                    muezzinName = fajrName?.takeIf { it.isNotBlank() } ?: defaultConfig.muezzinName,
                    localFilePath = fajrLocal,
                    remoteUrl = fajrUrl
                )
            }
        }

        // 3. Unified default Muezzin for all 5 prayers
        return defaultConfig
    }

    fun isCountdownNotificationEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.safeGetBoolean(KEY_COUNTDOWN_ENABLED, false)
    }

    fun setCountdownNotificationEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_COUNTDOWN_ENABLED, enabled).apply()
    }

    // ---- Global Location & Calculation Method ----

    fun hasSavedGlobalLocation(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(KEY_LOC_LAT) && prefs.contains(KEY_LOC_LNG)
    }

    fun saveGlobalLocation(context: Context, config: GlobalLocationConfig) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_LOC_LAT, config.lat.toString())
            .putString(KEY_LOC_LNG, config.lng.toString())
            .putString(KEY_LOC_LABEL, config.displayLabel)
            .putString(KEY_LOC_COUNTRY_CODE, config.countryCode)
            .putString(KEY_LOC_COUNTRY_NAME, config.countryName)
            .putString(KEY_LOC_CITY_NAME, config.cityName)
            .putInt(KEY_LOC_METHOD_ID, config.calculationMethodId)
            .putBoolean(KEY_LOC_METHOD_MANUAL, config.isMethodManualOverride)
            .putString(KEY_CITY_LABEL, config.displayLabel)
            .apply()
    }

    fun getGlobalLocation(context: Context): GlobalLocationConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lat = prefs.safeGetString(KEY_LOC_LAT, null)?.toDoubleOrNull() ?: 30.0444
        val lng = prefs.safeGetString(KEY_LOC_LNG, null)?.toDoubleOrNull() ?: 31.2357
        val label = prefs.safeGetString(KEY_LOC_LABEL, null)
            ?: prefs.safeGetString(KEY_CITY_LABEL, null)
            ?: "مصر - القاهرة"
        val cc = prefs.safeGetString(KEY_LOC_COUNTRY_CODE, "EG").orEmpty()
        val country = prefs.safeGetString(KEY_LOC_COUNTRY_NAME, "مصر").orEmpty()
        val city = prefs.safeGetString(KEY_LOC_CITY_NAME, "القاهرة").orEmpty()
        val defaultMethod = PrayerCalculationMethod.forCountryCode(cc).alAdhanMethodId
        val methodId = prefs.safeGetInt(KEY_LOC_METHOD_ID, defaultMethod)
        val isManual = prefs.safeGetBoolean(KEY_LOC_METHOD_MANUAL, false)
        return GlobalLocationConfig(
            lat = lat,
            lng = lng,
            displayLabel = label,
            countryCode = cc,
            countryName = country,
            cityName = city,
            calculationMethodId = methodId,
            isMethodManualOverride = isManual
        )
    }

    // ---- Hijri Date Offset (-2..+2) ----

    fun getHijriDayOffset(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.safeGetInt(KEY_HIJRI_DAY_OFFSET, 0).coerceIn(-2, 2)
    }

    fun setHijriDayOffset(context: Context, offsetDays: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_HIJRI_DAY_OFFSET, offsetDays.coerceIn(-2, 2)).apply()
    }

    // ---- Auto-Silent DND During Prayer (OFF by default) ----

    fun isDndPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.isNotificationPolicyAccessGranted == true
        } else {
            true
        }
    }

    fun isAutoSilentEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.safeGetBoolean(KEY_AUTO_SILENT_ENABLED, false)
    }

    fun setAutoSilentEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_SILENT_ENABLED, enabled).apply()
    }

    fun isAutoSilentAtIqama(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.safeGetBoolean(KEY_AUTO_SILENT_AT_IQAMA, true)
    }

    fun setAutoSilentAtIqama(context: Context, atIqama: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_SILENT_AT_IQAMA, atIqama).apply()
    }

    fun getAutoSilentDurationMinutes(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.safeGetInt(KEY_AUTO_SILENT_DURATION_MIN, 15).coerceIn(5, 60)
    }

    fun setAutoSilentDurationMinutes(context: Context, minutes: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_AUTO_SILENT_DURATION_MIN, minutes.coerceIn(5, 60)).apply()
    }

    fun savePreSilentState(context: Context, ringerMode: Int, interruptionFilter: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Do not overwrite saved state if an earlier silent window is still active
        if (prefs.safeGetBoolean(KEY_PRE_SILENT_ACTIVE, false)) return
        prefs.edit()
            .putInt(KEY_PRE_SILENT_RINGER, ringerMode)
            .putInt(KEY_PRE_SILENT_FILTER, interruptionFilter)
            .putBoolean(KEY_PRE_SILENT_ACTIVE, true)
            .apply()
    }

    fun consumePreSilentState(context: Context): Pair<Int, Int>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.safeGetBoolean(KEY_PRE_SILENT_ACTIVE, false)) return null
        val ringer = prefs.safeGetInt(KEY_PRE_SILENT_RINGER, -1)
        val filter = prefs.safeGetInt(KEY_PRE_SILENT_FILTER, -1)
        prefs.edit().putBoolean(KEY_PRE_SILENT_ACTIVE, false).apply()
        return ringer to filter
    }

    // ---- Friday Surah Al-Kahf Reminder (ON by default) ----

    fun isFridayKahfReminderEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.safeGetBoolean(KEY_FRIDAY_KAHF_ENABLED, true)
    }

    fun setFridayKahfReminderEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_FRIDAY_KAHF_ENABLED, enabled).apply()
    }

    fun isAdhanSilenceOnVolumeEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.safeGetBoolean(KEY_ADHAN_SILENCE_ON_VOLUME, true)
    }

    fun setAdhanSilenceOnVolumeEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ADHAN_SILENCE_ON_VOLUME, enabled).apply()
    }

    fun isAdhanSilenceOnFlipEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.safeGetBoolean(KEY_ADHAN_SILENCE_ON_FLIP, true)
    }

    fun setAdhanSilenceOnFlipEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ADHAN_SILENCE_ON_FLIP, enabled).apply()
    }

    fun getPrayerAlertsSettings(context: Context): PrayerAlertsSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val iqamaMap = linkedMapOf<String, Int>()
        PrayerAlertsSettings.DEFAULT_IQAMA_MINUTES.forEach { (key, defaultMin) ->
            val stored = prefs.safeGetInt(KEY_IQAMA_MIN_PREFIX + key, defaultMin)
            iqamaMap[key] = stored.coerceIn(
                PrayerAlertsSettings.MIN_IQAMA_MINUTES,
                PrayerAlertsSettings.MAX_IQAMA_MINUTES
            )
        }
        val perPrayerUrls = linkedMapOf<String, String>()
        PrayerAlertsSettings.PRAYER_URL_KEYS.forEach { pKey ->
            val url = prefs.safeGetString(KEY_PRE_ADHAN_URL_PREFIX + pKey, "").orEmpty()
            if (url.isNotBlank()) perPrayerUrls[pKey] = url
        }
        return PrayerAlertsSettings(
            preAdhanEnabled = prefs.safeGetBoolean(KEY_PRE_ADHAN_ENABLED, true),
            preAdhanMinutes = prefs.safeGetInt(KEY_PRE_ADHAN_MINUTES, PrayerAlertsSettings.DEFAULT_PRE_ADHAN_MINUTES)
                .takeIf { it in PrayerAlertsSettings.PRE_ADHAN_MINUTE_OPTIONS }
                ?: PrayerAlertsSettings.DEFAULT_PRE_ADHAN_MINUTES,
            preAdhanSoundId = prefs.safeGetString(
                KEY_PRE_ADHAN_SOUND_ID,
                PrayerAlertsSettings.DEFAULT_PRE_ADHAN_SOUND_ID
            ) ?: PrayerAlertsSettings.DEFAULT_PRE_ADHAN_SOUND_ID,
            preAdhanSoundName = prefs.safeGetString(
                KEY_PRE_ADHAN_SOUND_NAME,
                PrayerAlertsSettings.DEFAULT_PRE_ADHAN_SOUND_NAME
            ) ?: PrayerAlertsSettings.DEFAULT_PRE_ADHAN_SOUND_NAME,
            preAdhanSoundUrl = prefs.safeGetString(KEY_PRE_ADHAN_SOUND_URL, "").orEmpty(),
            preAdhanIsPerPrayer = prefs.safeGetBoolean(KEY_PRE_ADHAN_IS_PER_PRAYER, false),
            preAdhanPerPrayerUrls = perPrayerUrls,
            iqamaEnabled = prefs.safeGetBoolean(KEY_IQAMA_ENABLED, true),
            iqamaMinutesByPrayer = iqamaMap,
            iqamaSoundId = prefs.safeGetString(
                KEY_IQAMA_SOUND_ID,
                PrayerAlertsSettings.DEFAULT_IQAMA_SOUND_ID
            ) ?: PrayerAlertsSettings.DEFAULT_IQAMA_SOUND_ID,
            iqamaSoundName = prefs.safeGetString(
                KEY_IQAMA_SOUND_NAME,
                PrayerAlertsSettings.DEFAULT_IQAMA_SOUND_NAME
            ) ?: PrayerAlertsSettings.DEFAULT_IQAMA_SOUND_NAME,
            iqamaSoundUrl = prefs.safeGetString(KEY_IQAMA_SOUND_URL, "").orEmpty()
        )
    }

    fun savePrayerAlertsSettings(
        context: Context,
        settings: PrayerAlertsSettings,
        markPreAdhanSoundUserCustomized: Boolean = false,
        markIqamaSoundUserCustomized: Boolean = false
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
            .putBoolean(KEY_PRE_ADHAN_ENABLED, settings.preAdhanEnabled)
            .putInt(KEY_PRE_ADHAN_MINUTES, settings.preAdhanMinutes)
            .putString(KEY_PRE_ADHAN_SOUND_ID, settings.preAdhanSoundId)
            .putString(KEY_PRE_ADHAN_SOUND_NAME, settings.preAdhanSoundName)
            .putString(KEY_PRE_ADHAN_SOUND_URL, settings.preAdhanSoundUrl)
            .putBoolean(KEY_PRE_ADHAN_IS_PER_PRAYER, settings.preAdhanIsPerPrayer)
            .putBoolean(KEY_IQAMA_ENABLED, settings.iqamaEnabled)
            .putString(KEY_IQAMA_SOUND_ID, settings.iqamaSoundId)
            .putString(KEY_IQAMA_SOUND_NAME, settings.iqamaSoundName)
            .putString(KEY_IQAMA_SOUND_URL, settings.iqamaSoundUrl)

        PrayerAlertsSettings.PRAYER_URL_KEYS.forEach { pKey ->
            val url = settings.preAdhanPerPrayerUrls[pKey].orEmpty()
            if (url.isNotBlank()) {
                editor.putString(KEY_PRE_ADHAN_URL_PREFIX + pKey, url)
            } else {
                editor.remove(KEY_PRE_ADHAN_URL_PREFIX + pKey)
            }
        }
        PrayerAlertsSettings.DEFAULT_IQAMA_MINUTES.keys.forEach { key ->
            editor.putInt(KEY_IQAMA_MIN_PREFIX + key, settings.iqamaMinutesFor(key))
        }
        if (markPreAdhanSoundUserCustomized) {
            editor.putBoolean(KEY_PRE_ADHAN_SOUND_USER_SET, true)
        }
        if (markIqamaSoundUserCustomized) {
            editor.putBoolean(KEY_IQAMA_SOUND_USER_SET, true)
        }
        editor.apply()
    }

    fun isPreAdhanSoundUserCustomized(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.safeGetBoolean(KEY_PRE_ADHAN_SOUND_USER_SET, false)
    }

    fun isIqamaSoundUserCustomized(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.safeGetBoolean(KEY_IQAMA_SOUND_USER_SET, false)
    }

    /**
     * Returns an [ActiveIqamaCountdown] if Iqama alerts are enabled and [nowMillis] is strictly
     * between any prayer's Adhan time and its configured Iqama time (`adhanMs <= now < iqamaMs`).
     */
    fun computeActiveIqamaCountdown(
        settings: PrayerAlertsSettings,
        timings24: Map<String, String>,
        nowMillis: Long = System.currentTimeMillis()
    ): ActiveIqamaCountdown? {
        if (!settings.iqamaEnabled) return null
        for ((key, arabicName) in prayerOrder) {
            val time24 = timings24[key] ?: continue
            val adhanTodayMs = parseTodayMillis(time24, nowMillis) ?: continue
            val iqamaDurationMs = settings.iqamaMinutesFor(key) * 60_000L
            val iqamaEndMs = adhanTodayMs + iqamaDurationMs
            if (nowMillis in adhanTodayMs until iqamaEndMs) {
                val remainingSec = ((iqamaEndMs - nowMillis) / 1000L).coerceAtLeast(0L)
                val mins = remainingSec / 60L
                val secs = remainingSec % 60L
                return ActiveIqamaCountdown(
                    prayerKey = key,
                    prayerArabicName = arabicName,
                    remainingText = String.format(Locale.US, "%02d:%02d", mins, secs)
                )
            }
            // Handle late-night Isha crossing midnight into early morning
            if (key == "Isha") {
                val yesterdayIshaMs = adhanTodayMs - 24L * 3_600_000L
                val yesterdayIqamaEndMs = yesterdayIshaMs + iqamaDurationMs
                if (nowMillis in yesterdayIshaMs until yesterdayIqamaEndMs) {
                    val remainingSec = ((yesterdayIqamaEndMs - nowMillis) / 1000L).coerceAtLeast(0L)
                    val mins = remainingSec / 60L
                    val secs = remainingSec % 60L
                    return ActiveIqamaCountdown(
                        prayerKey = key,
                        prayerArabicName = arabicName,
                        remainingText = String.format(Locale.US, "%02d:%02d", mins, secs)
                    )
                }
            }
        }
        return null
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

    fun saveDisplayMetadata(
        context: Context,
        hijriDate: String,
        cityLabel: String,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dayStamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMillis))
        val editor = prefs.edit().putString(KEY_CITY_LABEL, cityLabel)
        if (hijriDate.isNotBlank() && !hijriDate.contains("وضع عدم الاتصال") && !hijriDate.contains("جاري")) {
            editor.putString(KEY_HIJRI_DATE, hijriDate)
            editor.putString(KEY_HIJRI_DAY_STAMP, dayStamp)
        }
        editor.apply()
    }

    fun getCityLabel(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.safeGetString(KEY_CITY_LABEL, OfflineIslamicData.cities.first().arabicLabel)
            ?: OfflineIslamicData.cities.first().arabicLabel
    }

    fun getHijriDateOrOffline(
        context: Context,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        val offset = getHijriDayOffset(context)
        if (offset != 0) {
            return computeAdjustedHijriInfo(nowMillis, offset).formattedDate
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todayStamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMillis))
        val savedStamp = prefs.safeGetString(KEY_HIJRI_DAY_STAMP, null)
        val savedHijri = prefs.safeGetString(KEY_HIJRI_DATE, null)
        if (savedStamp == todayStamp && !savedHijri.isNullOrBlank()) {
            return savedHijri
        }
        return computeAdjustedHijriInfo(nowMillis, 0).formattedDate
    }

    fun computeAdjustedHijriInfo(
        dateMillis: Long = System.currentTimeMillis(),
        offsetDays: Int = 0
    ): AdjustedHijriInfo {
        val gregCal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val actualDayOfWeek = gregCal.get(Calendar.DAY_OF_WEEK).coerceIn(1, 7)

        return runCatching {
            val shiftedMillis = dateMillis + offsetDays.coerceIn(-2, 2) * 86_400_000L
            val hijri = IslamicCalendar().apply {
                timeInMillis = shiftedMillis
            }
            val hDay = hijri.get(IslamicCalendar.DAY_OF_MONTH)
            val hMonthIdx = hijri.get(IslamicCalendar.MONTH).coerceIn(0, 11)
            val hYear = hijri.get(IslamicCalendar.YEAR)

            val formatted = "${arabicWeekdays[actualDayOfWeek]}، $hDay ${hijriMonthsArabic[hMonthIdx]} $hYear هـ"

            val badges = mutableListOf<String>()
            if (hDay in 13..15) {
                badges.add("الأيام البيض")
            }
            if (actualDayOfWeek == Calendar.MONDAY) {
                badges.add("سُنة الإثنين")
            } else if (actualDayOfWeek == Calendar.THURSDAY) {
                badges.add("سُنة الخميس")
            }

            AdjustedHijriInfo(
                formattedDate = formatted,
                hijriDay = hDay,
                hijriMonth = hMonthIdx + 1,
                hijriYear = hYear,
                dayOfWeek = actualDayOfWeek,
                blessedDayBadge = badges.takeIf { it.isNotEmpty() }?.joinToString(" • ")
            )
        }.getOrElse {
            AdjustedHijriInfo(
                formattedDate = "التقويم الهجري",
                hijriDay = 1,
                hijriMonth = 1,
                hijriYear = 1447,
                dayOfWeek = actualDayOfWeek,
                blessedDayBadge = when (actualDayOfWeek) {
                    Calendar.MONDAY -> "سُنة الإثنين"
                    Calendar.THURSDAY -> "سُنة الخميس"
                    else -> null
                }
            )
        }
    }

    fun computeOfflineHijriDate(
        nowMillis: Long = System.currentTimeMillis(),
        offsetDays: Int = 0
    ): String {
        return computeAdjustedHijriInfo(nowMillis, offsetDays).formattedDate
    }

    /**
     * Parses a 24h "HH:mm" string into tomorrow's timestamp in milliseconds (seconds/millis = 0).
     */
    fun parseTomorrowMillis(time24: String, nowMillis: Long = System.currentTimeMillis()): Long? {
        val parts = time24.trim().split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: return null
        return Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /**
     * Offline calculation of Islamic Midnight (منتصف الليل الشرعي) and Last Third of the Night
     * (الثلث الأخير من الليل) from today's Maghrib and tomorrow's Fajr in milliseconds:
     *   nightDurationMs = tomorrowFajrMs - todayMaghribMs
     *   islamicMidnightMs = todayMaghribMs + (nightDurationMs / 2)
     *   lastThirdNightMs = tomorrowFajrMs - (nightDurationMs / 3)
     */
    fun computeIslamicNightTimes(
        todayMaghrib24: String,
        tomorrowFajr24: String,
        nowMillis: Long = System.currentTimeMillis()
    ): IslamicNightTimesInfo {
        val todayMaghribMs = parseTodayMillis(todayMaghrib24, nowMillis)
            ?: parseTodayMillis("17:02", nowMillis)
            ?: nowMillis
        var tomorrowFajrMs = parseTomorrowMillis(tomorrowFajr24, nowMillis)
            ?: parseTomorrowMillis("04:55", nowMillis)
            ?: (todayMaghribMs + 12L * 3_600_000L)

        if (tomorrowFajrMs <= todayMaghribMs) {
            tomorrowFajrMs += 24L * 3_600_000L
        }

        val nightDurationMs = (tomorrowFajrMs - todayMaghribMs).coerceAtLeast(0L)
        val islamicMidnightMs = todayMaghribMs + (nightDurationMs / 2L)
        val lastThirdNightMs = tomorrowFajrMs - (nightDurationMs / 3L)

        return IslamicNightTimesInfo(
            todayMaghribMs = todayMaghribMs,
            tomorrowFajrMs = tomorrowFajrMs,
            nightDurationMs = nightDurationMs,
            islamicMidnightMs = islamicMidnightMs,
            lastThirdNightMs = lastThirdNightMs,
            islamicMidnightFormatted = formatMillis12HourArabic(islamicMidnightMs),
            lastThirdNightFormatted = formatMillis12HourArabic(lastThirdNightMs)
        )
    }

    fun computeIslamicNightTimes(
        context: Context,
        nowMillis: Long = System.currentTimeMillis()
    ): IslamicNightTimesInfo {
        val timings = getTimings(context)
        val maghrib = timings["Maghrib"] ?: OfflineIslamicData.fallbackTimings24["Maghrib"] ?: "17:02"
        val fajr = timings["Fajr"] ?: OfflineIslamicData.fallbackTimings24["Fajr"] ?: "04:55"
        return computeIslamicNightTimes(
            todayMaghrib24 = maghrib,
            tomorrowFajr24 = fajr,
            nowMillis = nowMillis
        )
    }

    fun formatMillis12HourArabic(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val h24 = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val suffix = if (h24 >= 12) "م" else "ص"
        val h12 = if (h24 % 12 == 0) 12 else h24 % 12
        return String.format(Locale.US, "%02d:%02d %s", h12, minute, suffix)
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
