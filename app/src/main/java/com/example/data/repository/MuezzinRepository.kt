package com.example.data.repository

import android.content.Context
import com.example.data.local.OfflineIslamicData
import com.example.data.model.MuezzinItem
import com.example.data.model.MuezzinRegion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Manages syncing the Muezzin Library from the remote GitHub JSON repository,
 * caching the JSON catalog in internal storage (`muezzins_cache.json`),
 * and downloading/serving individual Adhan MP3 files in `filesDir/adhan/{id}.mp3`
 * for 100% offline playback.
 */
object MuezzinRepository {

    val DEFAULT_MUEZZIN_REPO_URL: String
        get() = com.example.security.SecureConfigVault.muezzinsEndpoint

    private const val CACHE_JSON_FILENAME = "muezzins_cache.json"
    private const val ADHAN_CACHE_DIR = "adhan"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    fun getCatalogCacheFile(context: Context): File {
        val target = File(com.example.data.local.PersistentOfflineStorage.adhanDir(context), CACHE_JSON_FILENAME)
        val legacy = File(context.applicationContext.filesDir, CACHE_JSON_FILENAME)
        return com.example.data.local.PersistentOfflineStorage.migrateFileIfNeeded(legacy, target)
    }

    fun hasCatalogCache(context: Context): Boolean {
        val file = getCatalogCacheFile(context)
        return file.exists() && file.length() > 2L
    }

    fun getAdhanCacheDir(context: Context): File =
        com.example.data.local.PersistentOfflineStorage.adhanDir(context)

    /**
     * Returns the exact offline cached MP3 file path: `getFilesDir()/offline_adhan/{id}.mp3`.
     */
    fun getCachedAdhanFile(context: Context, muezzinId: String): File {
        val safeId = sanitizeId(muezzinId)
        val target = File(getAdhanCacheDir(context), "$safeId.mp3")
        val legacy = File(File(context.applicationContext.filesDir, ADHAN_CACHE_DIR), "$safeId.mp3")
        return com.example.data.local.PersistentOfflineStorage.migrateFileIfNeeded(legacy, target)
    }

    fun isAdhanCached(context: Context, muezzinId: String): Boolean {
        if (muezzinId.isBlank()) return false
        val file = getCachedAdhanFile(context, muezzinId)
        return file.exists() && file.length() > 0L
    }

    fun getAllCachedAdhanIds(context: Context): Set<String> {
        val legacyDir = File(context.applicationContext.filesDir, ADHAN_CACHE_DIR)
        val newDir = getAdhanCacheDir(context)
        if (legacyDir.exists() && legacyDir.isDirectory) {
            legacyDir.listFiles()
                ?.filter { it.isFile && it.extension.equals("mp3", ignoreCase = true) && it.length() > 0L }
                ?.forEach { legacy ->
                    com.example.data.local.PersistentOfflineStorage.migrateFileIfNeeded(
                        legacy,
                        File(newDir, legacy.name)
                    )
                }
        }
        if (!newDir.exists() || !newDir.isDirectory) return emptySet()
        return newDir.listFiles()
            ?.asSequence()
            ?.filter { it.isFile && it.extension.equals("mp3", ignoreCase = true) && it.length() > 0L }
            ?.map { it.nameWithoutExtension }
            ?.toSet()
            .orEmpty()
    }

    fun deleteCachedAdhan(context: Context, muezzinId: String) {
        if (muezzinId.isBlank()) return
        runCatching {
            val file = getCachedAdhanFile(context, muezzinId)
            if (file.exists()) file.delete()
        }
    }

    /**
     * Loads the cached GitHub JSON catalog from internal storage (`muezzins_cache.json`).
     * Never throws; returns emptyList() if absent or unreadable.
     */
    suspend fun loadCachedCatalog(context: Context): List<MuezzinItem> = withContext(Dispatchers.IO) {
        runCatching {
            val file = getCatalogCacheFile(context)
            if (!file.exists() || file.length() == 0L) return@runCatching emptyList()
            val rawJson = file.readText(Charsets.UTF_8)
            parseMuezzinJsonArray(rawJson)
        }.getOrDefault(emptyList())
    }

    /**
     * Fetches the JSON array from [repoUrl], validates & parses it, atomically saves
     * it to `filesDir/muezzins_cache.json`, and returns the parsed [MuezzinItem] list.
     */
    suspend fun syncCatalogFromRemote(
        context: Context,
        repoUrl: String = DEFAULT_MUEZZIN_REPO_URL
    ): Result<List<MuezzinItem>> = withContext(Dispatchers.IO) {
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
                    ?: throw IllegalStateException("Empty response body")
            }

            val parsed = parseMuezzinJsonArray(bodyString)
            if (parsed.isEmpty()) {
                throw IllegalStateException("القائمة فارغة")
            }

            // Atomically write to internal storage (muezzins_cache.json)
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
     * Downloads a Muezzin's MP3 from [url] directly into `getFilesDir()/adhan/{id}.mp3`.
     */
    suspend fun downloadAdhanMp3(
        context: Context,
        muezzinId: String,
        url: String
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanUrl = url.trim()
            require(cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
                "رابط غير صالح"
            }

            val request = Request.Builder()
                .url(cleanUrl)
                .build()

            val target = getCachedAdhanFile(context, muezzinId)
            val tmp = File(target.parentFile, "${target.name}.tmp")

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("HTTP ${response.code}")
                }
                val body = response.body ?: throw IllegalStateException("ملف فارغ")
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
            target
        }
    }

    /**
     * Parses a JSON array of objects with fields:
     * `id` (String), `name` (String), `region` (String), `isFajr` (Boolean), `url` (String)
     * plus optional `subtitle` (String).
     */
    fun parseMuezzinJsonArray(rawJson: String): List<MuezzinItem> {
        val trimmed = rawJson.trim()
        if (trimmed.isEmpty()) return emptyList()
        val jsonArray = JSONArray(trimmed)
        val result = mutableListOf<MuezzinItem>()
        val seenIds = mutableSetOf<String>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i) ?: continue
            val id = obj.optString("id", "").trim()
            val name = obj.optString("name", "").trim()
            val rawRegion = obj.optString("region", "").trim()
            val isFajr = obj.optBoolean("isFajr", false)
            val url = obj.optString("url", "").trim()
            val optionalSubtitle = obj.optString("subtitle", "").trim()

            if (id.isEmpty() || name.isEmpty()) continue
            if (!seenIds.add(id)) continue

            val mappedRegion = mapRegionToEnum(rawRegion, isFajr)
            val subtitle = when {
                optionalSubtitle.isNotEmpty() -> optionalSubtitle
                rawRegion.isNotEmpty() && rawRegion != mappedRegion.name -> {
                    if (isFajr) "أذان الفجر • $rawRegion" else rawRegion
                }
                isFajr -> "أذان الفجر — الصلاة خير من النوم"
                else -> mappedRegion.label
            }

            result.add(
                MuezzinItem(
                    id = id,
                    name = name,
                    subtitle = subtitle,
                    region = mappedRegion.name,
                    primaryUrl = url,
                    fallbackUrls = emptyList(),
                    isFajr = isFajr || mappedRegion == MuezzinRegion.FAJR,
                    isCustom = false
                )
            )
        }
        return result
    }

    private fun mapRegionToEnum(rawRegion: String, isFajr: Boolean): MuezzinRegion {
        if (rawRegion.isBlank()) {
            return if (isFajr) MuezzinRegion.FAJR else MuezzinRegion.HARAMAIN
        }
        // Direct enum name match (case-insensitive)
        MuezzinRegion.entries.find { it.name.equals(rawRegion, ignoreCase = true) }?.let {
            if (it != MuezzinRegion.ALL && it != MuezzinRegion.FAVORITES) return it
        }

        val lower = rawRegion.lowercase()
        val normAr = OfflineIslamicData.normalizeArabic(rawRegion)

        return when {
            lower.contains("fajr") || normAr.contains("فجر") -> MuezzinRegion.FAJR
            lower.contains("haram") || lower.contains("makkah") || lower.contains("mecca") ||
                lower.contains("madin") || lower.contains("medin") ||
                normAr.contains("حرمين") || normAr.contains("مكه") || normAr.contains("مدينه") ->
                MuezzinRegion.HARAMAIN
            lower.contains("egypt") || lower.contains("misr") || normAr.contains("مصر") ->
                MuezzinRegion.EGYPT
            lower.contains("gulf") || lower.contains("khaleej") || lower.contains("kuwait") ||
                lower.contains("saudi") || lower.contains("qatar") || lower.contains("uae") ||
                normAr.contains("خليج") || normAr.contains("كويت") || normAr.contains("سعود") ->
                MuezzinRegion.GULF
            lower.contains("levant") || lower.contains("aqsa") || lower.contains("sham") ||
                lower.contains("syria") || lower.contains("palestin") || lower.contains("leban") ||
                lower.contains("jerusalem") || lower.contains("quds") ||
                normAr.contains("شام") || normAr.contains("اقصي") || normAr.contains("قدس") ||
                normAr.contains("سوري") || normAr.contains("لبنان") || normAr.contains("دمشق") ||
                normAr.contains("حلب") ->
                MuezzinRegion.LEVANT_AQSA
            lower.contains("turk") || lower.contains("maghreb") || lower.contains("morocc") ||
                lower.contains("alger") || lower.contains("tunis") || lower.contains("bosn") ||
                lower.contains("indones") || lower.contains("global") || lower.contains("world") ||
                normAr.contains("تركي") || normAr.contains("مغرب") || normAr.contains("جزائر") ||
                normAr.contains("بوسن") || normAr.contains("اندونيس") || normAr.contains("عالم") ->
                MuezzinRegion.TURKEY_MAGHREB
            isFajr -> MuezzinRegion.FAJR
            else -> MuezzinRegion.TURKEY_MAGHREB
        }
    }

    private fun sanitizeId(rawId: String): String {
        val cleaned = rawId.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        return cleaned.ifBlank { "adhan_default" }
    }
}
