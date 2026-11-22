package com.example.data.model

import java.util.Locale

enum class PrayerAlertCategory {
    PRE_ADHAN,
    IQAMA,
    BOTH
}

data class PrayerAlertSound(
    val id: String,
    val name: String,
    val subtitle: String = "",
    val url: String = "",
    val category: PrayerAlertCategory = PrayerAlertCategory.BOTH,
    val isBuiltInFallback: Boolean = false,
    val isPerPrayer: Boolean = false,
    val perPrayerUrls: Map<String, String> = emptyMap()
) {
    fun urlForPrayer(prayerKey: String): String {
        val normalized = prayerKey.trim().lowercase(Locale.US)
        return perPrayerUrls[normalized].orEmpty()
    }
}

data class ActiveIqamaCountdown(
    val prayerKey: String,
    val prayerArabicName: String,
    val remainingText: String
)

data class PrayerAlertsSettings(
    val preAdhanEnabled: Boolean = true,
    val preAdhanMinutes: Int = DEFAULT_PRE_ADHAN_MINUTES,
    val preAdhanSoundId: String = DEFAULT_PRE_ADHAN_SOUND_ID,
    val preAdhanSoundName: String = DEFAULT_PRE_ADHAN_SOUND_NAME,
    val preAdhanSoundUrl: String = "",
    val preAdhanIsPerPrayer: Boolean = false,
    val preAdhanPerPrayerUrls: Map<String, String> = emptyMap(),
    val iqamaEnabled: Boolean = true,
    val iqamaMinutesByPrayer: Map<String, Int> = DEFAULT_IQAMA_MINUTES,
    val iqamaSoundId: String = DEFAULT_IQAMA_SOUND_ID,
    val iqamaSoundName: String = DEFAULT_IQAMA_SOUND_NAME,
    val iqamaSoundUrl: String = ""
) {
    fun iqamaMinutesFor(prayerKey: String): Int {
        return (iqamaMinutesByPrayer[prayerKey]
            ?: DEFAULT_IQAMA_MINUTES[prayerKey]
            ?: 15).coerceIn(MIN_IQAMA_MINUTES, MAX_IQAMA_MINUTES)
    }

    fun preAdhanUrlForPrayer(prayerKey: String): String {
        if (preAdhanIsPerPrayer) {
            val normalized = prayerKey.trim().lowercase(Locale.US)
            val specific = preAdhanPerPrayerUrls[normalized]
            if (!specific.isNullOrBlank()) return specific
        }
        return preAdhanSoundUrl
    }

    companion object {
        const val DEFAULT_PRE_ADHAN_MINUTES = 10
        val PRE_ADHAN_MINUTE_OPTIONS: List<Int> = listOf(5, 10, 15, 30)

        const val MIN_IQAMA_MINUTES = 5
        const val MAX_IQAMA_MINUTES = 60

        val PRAYER_URL_KEYS: List<String> = listOf("fajr", "dhuhr", "asr", "maghrib", "isha")

        val DEFAULT_IQAMA_MINUTES: Map<String, Int> = linkedMapOf(
            "Fajr" to 20,
            "Dhuhr" to 15,
            "Asr" to 15,
            "Maghrib" to 10,
            "Isha" to 15
        )

        const val PREFERRED_DEFAULT_PRE_ADHAN_SOUND_ID = "pre_voice_hamed"

        const val DEFAULT_PRE_ADHAN_SOUND_ID = "default_pre_adhan_tone"
        const val DEFAULT_PRE_ADHAN_SOUND_NAME = "تنبيه هادئ قبل الأذان (افتراضي)"

        const val DEFAULT_IQAMA_SOUND_ID = "default_iqama_tone"
        const val DEFAULT_IQAMA_SOUND_NAME = "تنبيه الإقامة الافتراضي"
    }
}
