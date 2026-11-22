package com.example.data.repository

import android.content.Context
import com.example.data.local.OfflineIslamicData
import com.example.data.local.PersistentOfflineStorage
import com.example.data.model.PrayerAlertCategory
import com.example.data.model.PrayerAlertSound
import com.example.data.model.PrayerAlertsSettings
import com.example.security.SecureConfigVault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Fetches, caches, and downloads Pre-Adhan & Iqama alert voices from `alerts.json`,
 * including `per_prayer` voice packs (5 separate MP3 files: fajr, dhuhr, asr, maghrib, isha).
 * Works offline-first using cached JSON + downloaded MP3 files in internal storage,
 * with built-in system fallback voices always available.
 */
object PrayerAlertRepository {

    val DEFAULT_ALERTS_REPO_URL: String
        get() = SecureConfigVault.alertsEndpoint

    private const val CACHE_JSON_FILENAME = "alerts_cache.json"
    private const val ALERTS_AUDIO_SUBDIR = "alerts"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    val builtInFallbackSounds: List<PrayerAlertSound> = listOf(
        PrayerAlertSound(
            id = PrayerAlertsSettings.DEFAULT_PRE_ADHAN_SOUND_ID,
            name = PrayerAlertsSettings.DEFAULT_PRE_ADHAN_SOUND_NAME,
            subtitle = "نغمة تنبيه هادئة تعمل دون إنترنت",
            url = "",
            category = PrayerAlertCategory.PRE_ADHAN,
            isBuiltInFallback = true
        ),
        PrayerAlertSound(
            id = PrayerAlertsSettings.DEFAULT_IQAMA_SOUND_ID,
            name = PrayerAlertsSettings.DEFAULT_IQAMA_SOUND_NAME,
            subtitle = "نغمة إقامة افتراضية تعمل دون إنترنت",
            url = "",
            category = PrayerAlertCategory.IQAMA,
            isBuiltInFallback = true
        )
    )

    fun getCatalogCacheFile(context: Context): File =
        File(PersistentOfflineStorage.adhanDir(context), CACHE_JSON_FILENAME)

    fun getAlertAudioDir(context: Context): File {
        return File(PersistentOfflineStorage.adhanDir(context), ALERTS_AUDIO_SUBDIR).apply {
            if (!exists()) mkdirs()
        }
    }

    fun getCachedAlertFile(context: Context, soundId: String): File {
        val safeId = sanitizeId(soundId)
        return File(getAlertAudioDir(context), "$safeId.mp3")
    }

    fun getPerPrayerPackDir(context: Context, soundId: String): File {
        val safeId = sanitizeId(soundId)
        return File(getAlertAudioDir(context), safeId).apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Exact local cache path for a prayer inside a `per_prayer` voice pack:
     * `offline_adhan/alerts/{soundId}/{prayerKeyLower}.mp3` (e.g. `dhuhr.mp3`).
     */
    fun getCachedPerPrayerAlertFile(context: Context, soundId: String, prayerKey: String): File {
        val safePrayer = normalizePrayerKey(prayerKey)
        return File(getPerPrayerPackDir(context, soundId), "$safePrayer.mp3")
    }

    fun isAlertCached(context: Context, soundId: String): Boolean {
        if (soundId.isBlank()) return false
        val file = getCachedAlertFile(context, soundId)
        return file.exists() && file.length() > 0L
    }

    fun isPerPrayerPackFullyCached(context: Context, sound: PrayerAlertSound): Boolean {
        if (!sound.isPerPrayer) return isAlertCached(context, sound.id)
        val keys = sound.perPrayerUrls.keys.ifEmpty { PrayerAlertsSettings.PRAYER_URL_KEYS.toSet() }
        if (keys.isEmpty()) return false
        return keys.all { key ->
            val f = getCachedPerPrayerAlertFile(context, sound.id, key)
            f.exists() && f.length() > 0L
        }
    }

    fun isSoundCached(context: Context, sound: PrayerAlertSound): Boolean {
        if (sound.isBuiltInFallback) return true
        return if (sound.isPerPrayer) {
            isPerPrayerPackFullyCached(context, sound)
        } else {
            isAlertCached(context, sound.id)
        }
    }

    fun getAllCachedAlertIds(
        context: Context,
        catalog: List<PrayerAlertSound> = emptyList()
    ): Set<String> {
        val result = mutableSetOf<String>()
        val dir = getAlertAudioDir(context)
        if (dir.exists() && dir.isDirectory) {
            dir.listFiles()?.forEach { entry ->
                if (entry.isFile && entry.extension.equals("mp3", ignoreCase = true) && entry.length() > 0L) {
                    result.add(entry.nameWithoutExtension)
                } else if (entry.isDirectory) {
                    val hasAllFive = PrayerAlertsSettings.PRAYER_URL_KEYS.all { pKey ->
                        val f = File(entry, "$pKey.mp3")
                        f.exists() && f.length() > 0L
                    }
                    if (hasAllFive) {
                        result.add(entry.name)
                    }
                }
            }
        }
        catalog.forEach { sound ->
            if (!sound.isBuiltInFallback && isSoundCached(context, sound)) {
                result.add(sanitizeId(sound.id))
            }
        }
        return result
    }

    suspend fun loadCachedCatalog(context: Context): List<PrayerAlertSound> = withContext(Dispatchers.IO) {
        runCatching {
            val file = getCatalogCacheFile(context)
            if (!file.exists() || file.length() == 0L) return@runCatching emptyList()
            val raw = file.readText(Charsets.UTF_8)
            parseAlertsJson(raw)
        }.getOrDefault(emptyList())
    }

    suspend fun syncCatalogFromRemote(
        context: Context,
        repoUrl: String = DEFAULT_ALERTS_REPO_URL
    ): Result<List<PrayerAlertSound>> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(repoUrl)
                .header("Accept", "application/json")
                .header("Cache-Control", "no-cache")
                .build()

            val bodyString = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("HTTP ${response.code}")
                }
                response.body?.string()?.trim()
                    ?: throw IllegalStateException("Empty alerts.json body")
            }

            val parsed = parseAlertsJson(bodyString)
            if (parsed.isEmpty()) {
                throw IllegalStateException("قائمة الأصوات فارغة")
            }

            val target = getCatalogCacheFile(context)
            val tmp = File(target.parentFile, "$CACHE_JSON_FILENAME.tmp")
            tmp.writeText(bodyString, Charsets.UTF_8)
            if (target.exists()) target.delete()
            if (!tmp.renameTo(target)) {
                target.writeText(bodyString, Charsets.UTF_8)
                tmp.delete()
            }

            parsed
        }
    }

    /**
     * Downloads either a single alert MP3 or all 5 MP3 files for a `per_prayer` voice pack.
     */
    suspend fun downloadAlertSound(
        context: Context,
        sound: PrayerAlertSound
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (sound.isBuiltInFallback) return@runCatching
            if (sound.isPerPrayer && sound.perPrayerUrls.isNotEmpty()) {
                var downloadedAny = false
                var lastError: Throwable? = null
                sound.perPrayerUrls.forEach { (prayerKey, url) ->
                    val target = getCachedPerPrayerAlertFile(context, sound.id, prayerKey)
                    if (target.exists() && target.length() > 0L) {
                        downloadedAny = true
                    } else if (url.startsWith("http://") || url.startsWith("https://")) {
                        runCatching {
                            downloadUrlToFile(url, target)
                            downloadedAny = true
                        }.onFailure { err ->
                            lastError = err
                        }
                    }
                }
                if (!downloadedAny && lastError != null) {
                    throw lastError!!
                }
                if (lastError != null && !isPerPrayerPackFullyCached(context, sound)) {
                    throw lastError!!
                }
            } else {
                val target = getCachedAlertFile(context, sound.id)
                downloadUrlToFile(sound.url, target)
            }
        }
    }

    suspend fun downloadAlertAudio(
        context: Context,
        soundId: String,
        url: String
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val target = getCachedAlertFile(context, soundId)
            downloadUrlToFile(url, target)
            target
        }
    }

    private fun downloadUrlToFile(url: String, target: File) {
        val cleanUrl = url.trim()
        require(cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
            "رابط الصوت غير صالح"
        }

        target.parentFile?.let { if (!it.exists()) it.mkdirs() }
        val tmp = File(target.parentFile, "${target.name}.tmp")

        val request = Request.Builder()
            .url(cleanUrl)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code}")
            }
            val body = response.body ?: throw IllegalStateException("ملف صوتي فارغ")
            body.byteStream().use { input ->
                tmp.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        if (!tmp.exists() || tmp.length() == 0L) {
            tmp.delete()
            throw IllegalStateException("فشل تنزيل ملف الصوت")
        }

        if (target.exists()) target.delete()
        if (!tmp.renameTo(target)) {
            tmp.copyTo(target, overwrite = true)
            tmp.delete()
        }
    }

    /**
     * Filters available sounds for a given target category (PRE_ADHAN or IQAMA),
     * always including matching remote sounds plus the built-in offline default.
     */
    fun soundsForCategory(
        remoteCatalog: List<PrayerAlertSound>,
        target: PrayerAlertCategory
    ): List<PrayerAlertSound> {
        val matchingRemote = remoteCatalog.filter {
            it.category == target || it.category == PrayerAlertCategory.BOTH
        }
        val effectiveRemote = if (matchingRemote.isNotEmpty()) matchingRemote else remoteCatalog
        val builtIn = builtInFallbackSounds.filter { it.category == target }
        return (effectiveRemote + builtIn).distinctBy { it.id }
    }

    /**
     * Resilient parser supporting both JSON Array and categorized JSON Object schemas for `alerts.json`,
     * including `"type": "per_prayer"` packs with `"preview_url"` and `"urls": { fajr, dhuhr, asr, maghrib, isha }`.
     */
    fun parseAlertsJson(rawJson: String): List<PrayerAlertSound> {
        return runCatching {
            val trimmed = rawJson.trim()
            if (trimmed.isEmpty()) return@runCatching emptyList()

            val result = mutableListOf<PrayerAlertSound>()
            val seenIds = mutableSetOf<String>()

            if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                appendJsonArrayItems(array, defaultCategory = null, result = result, seenIds = seenIds)
                return@runCatching result
            }

            if (trimmed.startsWith("{")) {
                val root = JSONObject(trimmed)

                val preKeys = listOf("pre_adhan", "preAdhan", "before_adhan", "beforeAdhan", "before", "pre", "reminder", "reminders")
                preKeys.forEach { key ->
                    root.optJSONArray(key)?.let { arr ->
                        appendJsonArrayItems(arr, defaultCategory = PrayerAlertCategory.PRE_ADHAN, result = result, seenIds = seenIds)
                    }
                }

                val iqamaKeys = listOf("iqama", "iqamah", "post_adhan", "postAdhan", "after_adhan", "afterAdhan", "after", "post")
                iqamaKeys.forEach { key ->
                    root.optJSONArray(key)?.let { arr ->
                        appendJsonArrayItems(arr, defaultCategory = PrayerAlertCategory.IQAMA, result = result, seenIds = seenIds)
                    }
                }

                val generalKeys = listOf("alerts", "sounds", "voices", "items", "data")
                generalKeys.forEach { key ->
                    root.optJSONArray(key)?.let { arr ->
                        appendJsonArrayItems(arr, defaultCategory = null, result = result, seenIds = seenIds)
                    }
                }
            }

            result
        }.getOrDefault(emptyList())
    }

    private fun appendJsonArrayItems(
        array: JSONArray,
        defaultCategory: PrayerAlertCategory?,
        result: MutableList<PrayerAlertSound>,
        seenIds: MutableSet<String>
    ) {
        for (i in 0 until array.length()) {
            runCatching {
                val obj = array.optJSONObject(i) ?: return@runCatching
                val name = obj.optString("name", "").trim()
                    .ifBlank { obj.optString("title", "").trim() }
                    .ifBlank { obj.optString("label", "").trim() }
                if (name.isBlank()) return@runCatching

                val rawType = obj.optString("type", "").trim()
                    .ifBlank { obj.optString("category", "").trim() }
                    .ifBlank { obj.optString("kind", "").trim() }

                val urlsObj = obj.optJSONObject("urls")
                val perPrayerMap = linkedMapOf<String, String>()
                if (urlsObj != null) {
                    val iter = urlsObj.keys()
                    while (iter.hasNext()) {
                        val rawKey = iter.next()
                        val normalizedKey = normalizePrayerKey(rawKey)
                        val value = urlsObj.optString(rawKey, "").trim()
                        if (normalizedKey in PrayerAlertsSettings.PRAYER_URL_KEYS && value.isNotBlank()) {
                            perPrayerMap[normalizedKey] = value
                        }
                    }
                }

                val isPerPrayer = rawType.equals("per_prayer", ignoreCase = true) || perPrayerMap.isNotEmpty()

                val previewOrSingleUrl = obj.optString("preview_url", "").trim()
                    .ifBlank { obj.optString("previewUrl", "").trim() }
                    .ifBlank { obj.optString("url", "").trim() }
                    .ifBlank { obj.optString("audio_url", "").trim() }
                    .ifBlank { obj.optString("audioUrl", "").trim() }
                    .ifBlank { obj.optString("src", "").trim() }
                    .ifBlank { perPrayerMap["dhuhr"] ?: perPrayerMap.values.firstOrNull().orEmpty() }

                if (previewOrSingleUrl.isBlank() && perPrayerMap.isEmpty()) return@runCatching

                val rawId = obj.optString("id", "").trim()
                    .ifBlank { "alert_${sanitizeId(name)}_${previewOrSingleUrl.hashCode().toUInt()}" }
                val safeId = sanitizeId(rawId)
                if (!seenIds.add(safeId)) return@runCatching

                val category = defaultCategory ?: if (isPerPrayer) {
                    PrayerAlertCategory.PRE_ADHAN
                } else {
                    parseCategory(rawType)
                }

                val subtitle = obj.optString("subtitle", "").trim()
                    .ifBlank { obj.optString("description", "").trim() }
                    .ifBlank {
                        when {
                            isPerPrayer -> "تنبيه صوتي مخصص باسم كل صلاة (5 ملفات)"
                            category == PrayerAlertCategory.PRE_ADHAN -> "صوت تنبيه قبل الأذان"
                            category == PrayerAlertCategory.IQAMA -> "صوت تنبيه الإقامة"
                            else -> "صالح للتنبيه قبل الأذان وللإقامة"
                        }
                    }

                result.add(
                    PrayerAlertSound(
                        id = safeId,
                        name = name,
                        subtitle = subtitle,
                        url = previewOrSingleUrl,
                        category = category,
                        isBuiltInFallback = false,
                        isPerPrayer = isPerPrayer,
                        perPrayerUrls = perPrayerMap
                    )
                )
            }
        }
    }

    private fun parseCategory(rawType: String): PrayerAlertCategory {
        if (rawType.isBlank()) return PrayerAlertCategory.BOTH
        val lower = rawType.lowercase(Locale.US)
        val normAr = OfflineIslamicData.normalizeArabic(rawType)
        return when {
            lower.contains("both") || lower.contains("all") || lower.contains("general") -> PrayerAlertCategory.BOTH
            lower.contains("iqam") || lower.contains("post") || lower.contains("after") ||
                normAr.contains("اقام") || normAr.contains("بعد") -> PrayerAlertCategory.IQAMA
            lower.contains("pre") || lower.contains("before") || lower.contains("remind") ||
                normAr.contains("قبل") || normAr.contains("تذكير") -> PrayerAlertCategory.PRE_ADHAN
            else -> PrayerAlertCategory.BOTH
        }
    }

    fun normalizePrayerKey(rawPrayerKey: String): String {
        val lower = rawPrayerKey.trim().lowercase(Locale.US)
        return when {
            lower.contains("fajr") -> "fajr"
            lower.contains("dhuhr") || lower.contains("zuhr") -> "dhuhr"
            lower.contains("asr") -> "asr"
            lower.contains("maghrib") -> "maghrib"
            lower.contains("isha") -> "isha"
            else -> lower
        }
    }

    fun sanitizeId(rawId: String): String {
        val cleaned = rawId.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        return cleaned.ifBlank { "alert_default" }
    }
}
