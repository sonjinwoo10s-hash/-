package com.example.data.model

import com.squareup.moshi.JsonClass

enum class MuezzinRegion(val label: String) {
    ALL("الكل"),
    FAVORITES("المفضلة"),
    HARAMAIN("الحرمين الشريفين"),
    EGYPT("مصر"),
    GULF("الخليج"),
    LEVANT_AQSA("الشام والأقصى"),
    TURKEY_MAGHREB("تركيا والعالم الإسلامي"),
    FAJR("أذان الفجر"),
    CUSTOM("مؤذنوني")
}

@JsonClass(generateAdapter = true)
data class MuezzinItem(
    val id: String,
    val name: String,
    val subtitle: String,
    val region: String, // MuezzinRegion.name
    val primaryUrl: String,
    val fallbackUrls: List<String> = emptyList(),
    val localFilePath: String? = null,
    val isFajr: Boolean = false,
    val isCustom: Boolean = false,
    val hasCustomAudio: Boolean = false,
    val customAudioLabel: String? = null
) {
    fun regionEnum(): MuezzinRegion =
        runCatching { MuezzinRegion.valueOf(region) }.getOrDefault(MuezzinRegion.CUSTOM)

    fun isUsingCustomAudio(): Boolean =
        isCustom || hasCustomAudio || !localFilePath.isNullOrBlank()

    fun hasAudioSource(): Boolean =
        !localFilePath.isNullOrBlank() || primaryUrl.isNotBlank()
}

@JsonClass(generateAdapter = true)
data class MuezzinAudioOverride(
    val muezzinId: String,
    val customUrl: String? = null,
    val localFilePath: String? = null,
    val localFileName: String? = null
)

enum class AdhanPlaybackStatus(val arabicLabel: String) {
    IDLE(""),
    LOADING("جاري التحميل..."),
    PLAYING("يعمل الآن"),
    PAUSED("متوقف مؤقتاً"),
    FALLBACK_PLAYING("يعمل (بث بديل)"),
    ERROR("تعذر التشغيل")
}

data class AdhanPlayerState(
    val activeMuezzinId: String? = null,
    val activeMuezzinName: String? = null,
    val status: AdhanPlaybackStatus = AdhanPlaybackStatus.IDLE,
    val errorDetail: String? = null,
    val activeStreamIndex: Int = 0,
    val totalStreams: Int = 1,
    val progressFraction: Float = 0f
)
