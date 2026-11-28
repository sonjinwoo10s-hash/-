package com.example.data.model

import com.squareup.moshi.JsonClass

data class CityOption(
    val id: String,
    val city: String,
    val country: String,
    val method: Int,
    val lat: Double,
    val lng: Double,
    val arabicLabel: String
)

data class PrayerEntry(
    val key: String,
    val arabicName: String,
    val time24: String,
    val formatted12: String,
    val isNext: Boolean = false,
    val isPerformed: Boolean = false,
    val isLockedFuture: Boolean = false
)

data class PrayerTimesUiState(
    val isLoading: Boolean = false,
    val isOfflineFallback: Boolean = false,
    val selectedDateKey: String = "",
    val isSelectedDateToday: Boolean = true,
    val isSelectedDateFuture: Boolean = false,
    val isSelectedDatePast: Boolean = false,
    val hijriDate: String = "جاري تحميل التاريخ الهجري...",
    val gregorianDate: String = "",
    val blessedDayBadge: String? = null,
    val performedCount: Int = 0,
    val statusNote: String = "توقيت الهيئة المصرية العامة للمساحة",
    val prayers: List<PrayerEntry> = emptyList(),
    val lockedTodayPrayerKeys: Set<String> = emptySet(),
    val nextPrayerName: String = "---",
    val countdownText: String = "--:--:--",
    val islamicMidnightFormatted: String = "--:--",
    val lastThirdNightFormatted: String = "--:--",
    val activeIqamaCountdown: ActiveIqamaCountdown? = null
)

data class WorshipTask(
    val id: String,
    val label: String,
    val completed: Boolean = false,
    val isLockedFuture: Boolean = false
)

data class DailyQuote(
    val text: String,
    val reference: String
)

@JsonClass(generateAdapter = true)
data class SurahSummary(
    val number: Int = 1,
    val name: String = "",
    val englishName: String = "",
    val numberOfAyahs: Int = 0,
    val revelationType: String = "Meccan",
    val startPage: Int = 1
)

data class AyahItem(
    val numberInSurah: Int,
    val text: String,
    val tafsir: String,
    val globalNumber: Int = 0,
    val juz: Int = 1
)

enum class QuranViewMode(val arabicLabel: String) {
    MUSHAF("وضع المصحف"),
    LIST("وضع الآيات والتفسير")
}

enum class MushafPaperTheme(val arabicLabel: String) {
    CREAM("ورق المصحف"),
    PURE_WHITE("أبيض نقي"),
    NIGHT("ليلي مريح")
}

enum class AyahRepeatMode(val arabicLabel: String, val repeatCount: Int) {
    ONCE("مرة واحدة", 1),
    THREE("3 مرات", 3),
    FIVE("5 مرات", 5),
    CONTINUOUS("تكرار مستمر", -1)
}

data class QuranBookmark(
    val surahNumber: Int,
    val surahName: String,
    val ayahNumber: Int,
    val pageNumber: Int = 1
)

@JsonClass(generateAdapter = true)
data class MoshafOption(
    val id: Int = 0,
    val name: String = "",
    val server: String = "",
    val surahTotal: Int = 114,
    val surahList: String = ""
) {
    fun supportsSurah(surahNumber: Int): Boolean {
        if (surahList.isBlank()) return true
        return surahList.split(",").any { it.trim().toIntOrNull() == surahNumber }
    }

    fun buildSurahUrl(surahNumber: Int): String {
        val safeSurah = surahNumber.coerceIn(1, 114)
        val normalizedBase = if (server.endsWith("/")) server else "$server/"
        return normalizedBase + String.format(java.util.Locale.US, "%03d.mp3", safeSurah)
    }
}

@JsonClass(generateAdapter = true)
data class ReciterOption(
    val id: String = "",
    val arabicName: String = "",
    val riwayahName: String = "حفص عن عاصم - مرتل",
    val serverUrl: String = "",
    val fallbackUrlTemplate: String? = null,
    val moshafList: List<MoshafOption> = emptyList(),
    val isFeatured: Boolean = false
) {
    fun buildPrimaryAudioUrl(surahNumber: Int): String {
        val safeSurah = surahNumber.coerceIn(1, 114)
        val normalizedBase = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
        return normalizedBase + String.format(java.util.Locale.US, "%03d.mp3", safeSurah)
    }

    fun buildCandidateAudioUrls(surahNumber: Int): List<String> {
        val safeSurah = surahNumber.coerceIn(1, 114)
        val padded = String.format(java.util.Locale.US, "%03d", safeSurah)
        return buildList {
            if (serverUrl.isNotBlank()) {
                add(buildPrimaryAudioUrl(safeSurah))
            }
            if (!fallbackUrlTemplate.isNullOrBlank()) {
                add(
                    fallbackUrlTemplate
                        .replace("{surah}", safeSurah.toString())
                        .replace("{padded}", padded)
                )
            }
            // Verified CDN fallback
            add("https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/$safeSurah.mp3")
        }.distinct()
    }
}

data class ZikrItem(
    val id: String,
    val text: String,
    val virtue: String,
    val requiredCount: Int
)

data class TasbihState(
    val phrase: String = "سبحان الله",
    val target: Int = 33,
    val current: Int = 0,
    val rounds: Int = 0,
    val lifetime: Int = 0
)

data class HadithItem(
    val id: Int,
    val numLabel: String,
    val title: String,
    val category: String,
    val narrator: String,
    val text: String,
    val explanation: String,
    val source: String
)

data class GuideStep(
    val badge: String,
    val title: String,
    val description: String
)

data class GuideSection(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val steps: List<GuideStep>
)
