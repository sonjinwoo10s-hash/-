package com.example.ramadan

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.Immutable
import com.example.alarm.AstronomicalPrayerCalculator
import com.example.alarm.PrayerScheduleStore
import com.example.data.local.safeGetBoolean
import com.example.data.local.safeGetString
import com.example.data.repository.GlobalLocationConfig
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Permanent official live broadcast sources for Ramadan crescent sighting.
 * Uses YouTube's permanent channel live stream URLs (zero hardcoded or expired video IDs).
 */
@Immutable
enum class CrescentLiveSource(
    val id: String,
    val arabicName: String,
    val shortName: String,
    val subtitle: String,
    val channelId: String
) {
    DAR_AL_IFTA(
        id = "dar_al_ifta",
        arabicName = "دار الإفتاء المصرية",
        shortName = "دار الإفتاء",
        subtitle = "المصدر الرسمي الأساسي • إعلان رؤية هلال رمضان",
        channelId = "UCScGoaiJ7oNtJAnzaj5QVVA"
    ),
    SAUDI_QURAN_TV(
        id = "saudi_quran_tv",
        arabicName = "قناة القرآن الكريم",
        shortName = "القرآن الكريم",
        subtitle = "البث الرسمي المباشر من المسجد الحرام • السعودية",
        channelId = "UCos52azQNBgW63_9uDJoPDA"
    ),
    AL_EKHBARIYA(
        id = "al_ekhbariya",
        arabicName = "قناة الإخبارية",
        shortName = "الإخبارية",
        subtitle = "التغطية الرسمية لتحري هلال رمضان • السعودية",
        channelId = "UCV01ajGl6nt09h40iDoHDNg"
    );

    /**
     * Permanent YouTube channel live embed URL (never relies on static/old video IDs).
     */
    val embedUrl: String
        get() = "https://www.youtube.com/embed/live_stream?channel=$channelId&autoplay=1&playsinline=1&rel=0&modestbranding=1"

    /**
     * Direct fallback URL opening the channel's current live broadcast in YouTube / browser.
     */
    val watchLiveUrl: String
        get() = "https://www.youtube.com/channel/$channelId/live"

    companion object {
        fun fromId(id: String?): CrescentLiveSource {
            return entries.find { it.id == id } ?: DAR_AL_IFTA
        }
    }
}

@Immutable
data class RamadanSightingTarget(
    val targetHijriYear: Int,
    val currentHijriYear: Int,
    val currentHijriMonth: Int,
    val currentHijriDay: Int,
    val currentHijriFormatted: String,
    val isRamadanNow: Boolean,
    val isShaban29Today: Boolean,
    val sightingMaghribMillis: Long,
    val sightingMaghribFormatted12: String,
    val sightingGregorianDateLabel: String
)

@Immutable
data class RamadanCountdownTick(
    val days: Long,
    val hours: Long,
    val minutes: Long,
    val seconds: Long,
    val remainingMillis: Long,
    val hasSightingArrived: Boolean
)

object RamadanCrescentManager {

    private const val PREFS_NAME = "rafiq_ramadan_crescent_prefs"
    private const val KEY_DISMISSED_YEAR_PREFIX = "crescent_live_dismissed_year_"
    private const val KEY_PREFERRED_SOURCE_ID = "crescent_live_source_id"

    /**
     * Computes the exact upcoming (or today's) 29th of Sha'ban crescent sighting target
     * synchronized with the user's global location, Maghrib time adjustment, and Hijri day offset (-1 / 0 / +1).
     */
    fun computeSightingTarget(
        location: GlobalLocationConfig,
        hijriDayOffset: Int,
        maghribAdjustmentMinutes: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): RamadanSightingTarget {
        val safeOffset = hijriDayOffset.coerceIn(-2, 2)
        val todayInfo = PrayerScheduleStore.computeAdjustedHijriInfo(nowMillis, safeOffset)
        val isRamadanNow = todayInfo.hijriMonth == 9
        val isShaban29Today = todayInfo.hijriMonth == 8 && todayInfo.hijriDay == 29

        val tz = TimeZone.getDefault()
        val searchCal = Calendar.getInstance(tz).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If we're currently on 29 Sha'ban, target is TODAY at Maghrib.
        // Otherwise walk forward day-by-day (max 380 days) until we hit 29 Sha'ban.
        var targetCal: Calendar? = null
        var targetYear = todayInfo.hijriYear
        if (isShaban29Today) {
            targetCal = searchCal.clone() as Calendar
            targetYear = todayInfo.hijriYear
        } else {
            val startStep = if (isRamadanNow) 1 else 1
            for (dayStep in startStep..380) {
                val probe = (searchCal.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, dayStep)
                }
                val info = PrayerScheduleStore.computeAdjustedHijriInfo(probe.timeInMillis, safeOffset)
                if (info.hijriMonth == 8 && info.hijriDay == 29) {
                    targetCal = probe
                    targetYear = info.hijriYear
                    break
                }
            }
        }

        val resolvedDayCal = targetCal ?: (searchCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 30)
        }

        val method = location.resolvedMethod()
        val rawTimings = AstronomicalPrayerCalculator.calculateTimings24(
            latitude = location.lat,
            longitude = location.lng,
            dateCalendar = resolvedDayCal,
            method = method
        )
        val rawMaghrib = rawTimings["Maghrib"] ?: "18:00"
        val adjustedMaghrib = PrayerScheduleStore.shiftTime24(rawMaghrib, maghribAdjustmentMinutes)
        val parts = adjustedMaghrib.split(":")
        val mHour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 18
        val mMin = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0

        val sightingCal = (resolvedDayCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, mHour)
            set(Calendar.MINUTE, mMin)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val formatted12 = formatArabicTime12(mHour, mMin)
        val gregLabel = runCatching {
            SimpleDateFormat("d MMMM yyyy", Locale("ar")).apply {
                timeZone = tz
            }.format(sightingCal.time)
        }.getOrDefault("")

        return RamadanSightingTarget(
            targetHijriYear = targetYear,
            currentHijriYear = todayInfo.hijriYear,
            currentHijriMonth = todayInfo.hijriMonth,
            currentHijriDay = todayInfo.hijriDay,
            currentHijriFormatted = todayInfo.formattedDate,
            isRamadanNow = isRamadanNow,
            isShaban29Today = isShaban29Today,
            sightingMaghribMillis = sightingCal.timeInMillis,
            sightingMaghribFormatted12 = formatted12,
            sightingGregorianDateLabel = gregLabel
        )
    }

    fun computeCountdownTick(
        target: RamadanSightingTarget,
        nowMillis: Long = System.currentTimeMillis()
    ): RamadanCountdownTick {
        val diff = (target.sightingMaghribMillis - nowMillis).coerceAtLeast(0L)
        val totalSec = diff / 1000L
        val days = totalSec / 86_400L
        val hours = (totalSec % 86_400L) / 3_600L
        val minutes = (totalSec % 3_600L) / 60L
        val seconds = totalSec % 60L
        val arrived = !target.isRamadanNow && (diff <= 0L)
        return RamadanCountdownTick(
            days = days,
            hours = hours,
            minutes = minutes,
            seconds = seconds,
            remainingMillis = diff,
            hasSightingArrived = arrived
        )
    }

    fun isDismissedForYear(context: Context, hijriYear: Int): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.safeGetBoolean("$KEY_DISMISSED_YEAR_PREFIX$hijriYear", false)
    }

    fun setDismissedForYear(context: Context, hijriYear: Int, dismissed: Boolean) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("$KEY_DISMISSED_YEAR_PREFIX$hijriYear", dismissed).apply()
    }

    fun getPreferredSource(context: Context): CrescentLiveSource {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedId = prefs.safeGetString(KEY_PREFERRED_SOURCE_ID, CrescentLiveSource.DAR_AL_IFTA.id)
        return CrescentLiveSource.fromId(savedId)
    }

    fun savePreferredSource(context: Context, source: CrescentLiveSource) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PREFERRED_SOURCE_ID, source.id).apply()
    }

    fun isDeviceOnline(context: Context): Boolean {
        return runCatching {
            val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN))
        }.getOrDefault(false)
    }

    private fun formatArabicTime12(hour24: Int, minute: Int): String {
        val period = if (hour24 >= 12) "م" else "ص"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return String.format(Locale.US, "%d:%02d %s", hour12, minute, period)
    }
}
