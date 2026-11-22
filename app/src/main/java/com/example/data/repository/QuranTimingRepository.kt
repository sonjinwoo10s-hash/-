package com.example.data.repository

import android.content.Context
import android.util.LruCache
import com.example.BuildConfig
import com.example.data.local.OfflineIslamicData
import com.example.data.model.ReciterOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class WordTimingSegment(
    val wordIndexZeroBased: Int,
    val startMs: Int,
    val endMs: Int
)

data class AyahTimingEntry(
    val verseKey: String,
    val surahNumber: Int,
    val ayahNumber: Int,
    val startMs: Int,
    val endMs: Int,
    val wordSegments: List<WordTimingSegment>
) {
    /**
     * Binary-searches [wordSegments] for the exact word active at [effectiveMs].
     */
    fun resolveWordAt(effectiveMs: Int): Int? {
        if (wordSegments.isEmpty()) return null
        var low = 0
        var high = wordSegments.lastIndex
        var candidate: Int? = null
        while (low <= high) {
            val mid = (low + high) ushr 1
            val seg = wordSegments[mid]
            when {
                effectiveMs < seg.startMs -> high = mid - 1
                effectiveMs >= seg.endMs -> {
                    candidate = seg.wordIndexZeroBased
                    low = mid + 1
                }
                else -> return seg.wordIndexZeroBased
            }
        }
        return candidate
    }

    fun findWordStartMs(wordIndexZeroBased: Int): Int? {
        return wordSegments.firstOrNull { it.wordIndexZeroBased == wordIndexZeroBased }?.startMs
    }
}

data class SurahTimingMap(
    val reciterId: Int,
    val surahNumber: Int,
    val quranComAudioUrl: String,
    val ayahs: List<AyahTimingEntry>,
    val hasWordSegments: Boolean
) {
    /**
     * Exact O(log N) binary search across [ayahs] for `timestamp_from <= effectiveMs < timestamp_to`.
     * Returns `(ayahNumber, wordIndexZeroBased?)` or `null` if outside timestamps.
     */
    fun resolveAtPosition(effectiveMs: Int): Pair<Int, Int?>? {
        if (ayahs.isEmpty() || effectiveMs < 0) return null

        var low = 0
        var high = ayahs.lastIndex
        var matched: AyahTimingEntry? = null

        while (low <= high) {
            val mid = (low + high) ushr 1
            val entry = ayahs[mid]
            when {
                effectiveMs < entry.startMs -> high = mid - 1
                effectiveMs >= entry.endMs -> low = mid + 1
                else -> {
                    matched = entry
                    break
                }
            }
        }

        // Handle tiny inter-verse silence gap cleanly without flickering highlight off
        if (matched == null) {
            val idx = high.coerceIn(0, ayahs.lastIndex)
            val prev = ayahs[idx]
            if (effectiveMs >= prev.startMs && effectiveMs <= prev.endMs + 400) {
                matched = prev
            } else {
                return null
            }
        }

        val wordIdx = if (hasWordSegments) matched.resolveWordAt(effectiveMs) else null
        return matched.ayahNumber to wordIdx
    }

    /**
     * O(log N) binary search by [ayahNumber] to find exact `timestamp_from` / `timestamp_to` for tap-to-seek.
     */
    fun findAyahEntry(ayahNumber: Int): AyahTimingEntry? {
        if (ayahs.isEmpty()) return null
        var low = 0
        var high = ayahs.lastIndex
        while (low <= high) {
            val mid = (low + high) ushr 1
            val entry = ayahs[mid]
            when {
                ayahNumber < entry.ayahNumber -> high = mid - 1
                ayahNumber > entry.ayahNumber -> low = mid + 1
                else -> return entry
            }
        }
        return ayahs.firstOrNull { it.ayahNumber == ayahNumber }
    }
}

/**
 * Fetches and locally caches official millisecond-accurate chapter audio URLs and word-level
 * timing segments from Quran.com v4 API (`chapter_recitations/{reciter_id}/{surah_number}?segments=true`).
 *
 * Uses a tiny bounded in-memory LRU cache (max 6 Surahs) + local disk cache in
 * `filesDir/quran_timings/r{reciterId}_s{surahNumber}.json` to stay ultra-lightweight on 2GB RAM devices.
 */
object QuranTimingRepository {

    private const val CACHE_DIR_NAME = "quran_timings"
    private const val MAX_MEMORY_ENTRIES = 6

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(18, TimeUnit.SECONDS)
            .build()
    }

    private val memoryCache = object : LruCache<String, SurahTimingMap>(MAX_MEMORY_ENTRIES) {}

    fun clearMemoryCache() {
        synchronized(memoryCache) {
            memoryCache.evictAll()
        }
    }

    /**
     * Maps supported popular reciters to their official Quran.com v4 / QDC chapter recitation IDs.
     * Returns `null` for mp3quran.net-only reciters so single-Ayah playback cleanly uses direct
     * per-Ayah audio URLs with Ayah highlighting instead of misaligned timestamps.
     */
    fun resolveQuranComReciterId(reciter: ReciterOption): Int? {
        when (reciter.id) {
            "featured_alafasy" -> return 7
            "featured_basit_murattal" -> return 2
            "featured_basit_mujawwad" -> return 1
            "featured_minshawi" -> return 9
            "featured_husary" -> return 6
            "featured_sudais" -> return 3
            "featured_shuraim" -> return 10
            "featured_shatri" -> return 4
            "featured_rifai" -> return 5
            "featured_maher" -> return 161
        }
        val norm = OfflineIslamicData.normalizeArabic(reciter.arabicName)
        val riwayahNorm = OfflineIslamicData.normalizeArabic(reciter.riwayahName)
        return when {
            norm.contains("العفاسي") -> 7
            norm.contains("عبد الباسط") || norm.contains("عبدالباسط") -> {
                if (riwayahNorm.contains("مجود")) 1 else 2
            }
            norm.contains("الحصري") -> {
                if (riwayahNorm.contains("معلم")) 12 else 6
            }
            norm.contains("المنشاوي") -> {
                if (riwayahNorm.contains("مجود")) 8 else 9
            }
            norm.contains("السديس") -> 3
            norm.contains("الشريم") -> 10
            norm.contains("الشاطري") -> 4
            norm.contains("الرفاعي") -> 5
            norm.contains("الطبلاوي") -> 11
            norm.contains("ماهر المعيقلي") -> 161
            else -> null
        }
    }

    private fun getDiskCacheFile(context: Context, reciterId: Int, surahNumber: Int): File {
        val fileName = "r${reciterId}_s${surahNumber}.json"
        val target = File(com.example.data.local.PersistentOfflineStorage.quranTimingsDir(context), fileName)
        val legacy = File(File(context.applicationContext.filesDir, CACHE_DIR_NAME), fileName)
        return com.example.data.local.PersistentOfflineStorage.migrateFileIfNeeded(legacy, target)
    }

    /**
     * Strictly local-only lookup (memory LRU + local disk cache) with ZERO network requests.
     * Used for instant offline-first playback when a Surah MP3 is already downloaded on device.
     */
    suspend fun getCachedSurahTimingsLocalOnly(
        context: Context?,
        reciter: ReciterOption,
        surahNumber: Int
    ): SurahTimingMap? = withContext(Dispatchers.IO) {
        val reciterId = resolveQuranComReciterId(reciter) ?: return@withContext null
        val cacheKey = "${reciterId}_$surahNumber"

        synchronized(memoryCache) {
            memoryCache.get(cacheKey)?.let { return@withContext it }
        }

        if (context != null) {
            val diskFile = getDiskCacheFile(context, reciterId, surahNumber)
            if (diskFile.exists() && diskFile.length() > 32L) {
                val cached = runCatching {
                    parseTimingJson(diskFile.readText(Charsets.UTF_8), reciterId, surahNumber)
                }.getOrNull()
                if (cached != null && cached.ayahs.isNotEmpty()) {
                    synchronized(memoryCache) { memoryCache.put(cacheKey, cached) }
                    return@withContext cached
                }
            }
        }
        null
    }

    /**
     * Loads official Quran.com audio URL + millisecond timestamps & word segments from:
     *  1) Tiny LRU memory cache
     *  2) Local disk cache (`filesDir/offline_quran/timings/r{reciterId}_s{surahNumber}.json`)
     *  3) Primary Quran.com v4 endpoint: `api.quran.com/api/v4/chapter_recitations/{id}/{surah}?segments=true`
     *  4) Fallback Quran.com QDC endpoint: `api.qurancdn.com/api/qdc/audio/reciters/{id}/audio_files?chapter={surah}&segments=true`
     *     (used automatically if v4 omits `timestamps`/`segments` for that reciter).
     */
    suspend fun fetchSurahTimings(
        context: Context?,
        reciter: ReciterOption,
        surahNumber: Int
    ): SurahTimingMap? = withContext(Dispatchers.IO) {
        getCachedSurahTimingsLocalOnly(context, reciter, surahNumber)?.let { return@withContext it }
        val reciterId = resolveQuranComReciterId(reciter) ?: return@withContext null
        val cacheKey = "${reciterId}_$surahNumber"

        // 2. Fetch fresh from Quran.com v4 API, falling back to Quran.com QDC audio_files if timestamps are omitted
        val candidateEndpoints = listOf(
            "https://api.quran.com/api/v4/chapter_recitations/$reciterId/$surahNumber?segments=true",
            "https://api.qurancdn.com/api/qdc/audio/reciters/$reciterId/audio_files?chapter=$surahNumber&segments=true"
        )

        var bestWithoutWordSegments: Pair<SurahTimingMap, String>? = null

        for (endpoint in candidateEndpoints) {
            val body = fetchRawJson(endpoint) ?: continue
            val parsed = runCatching {
                parseTimingJson(body, reciterId, surahNumber)
            }.getOrNull() ?: continue

            if (parsed.hasWordSegments) {
                persistTimingCache(context, reciterId, surahNumber, body)
                synchronized(memoryCache) { memoryCache.put(cacheKey, parsed) }
                return@withContext parsed
            } else if (bestWithoutWordSegments == null) {
                bestWithoutWordSegments = parsed to body
            }
        }

        bestWithoutWordSegments?.let { (parsed, body) ->
            persistTimingCache(context, reciterId, surahNumber, body)
            synchronized(memoryCache) { memoryCache.put(cacheKey, parsed) }
            return@withContext parsed
        }

        null
    }

    private fun fetchRawJson(url: String): String? {
        return try {
            val req = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "RafiqAlMuslim/${BuildConfig.VERSION_NAME} (Android)")
                .build()
            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return null
                res.body?.string()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun persistTimingCache(context: Context?, reciterId: Int, surahNumber: Int, body: String) {
        if (context == null) return
        runCatching {
            val target = getDiskCacheFile(context, reciterId, surahNumber)
            val tmp = File(target.parentFile, "${target.name}.tmp")
            tmp.writeText(body, Charsets.UTF_8)
            if (target.exists()) target.delete()
            if (!tmp.renameTo(target)) {
                target.writeText(body, Charsets.UTF_8)
                tmp.delete()
            }
        }
    }

    private fun normalizeAudioUrl(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null
        return when {
            trimmed.startsWith("https://") || trimmed.startsWith("http://") -> trimmed
            trimmed.startsWith("//") -> "https:$trimmed"
            trimmed.startsWith("/") -> "https://download.quranicaudio.com$trimmed"
            else -> "https://verses.quran.com/$trimmed"
        }
    }

    private fun parseTimingJson(
        rawJson: String,
        reciterId: Int,
        surahNumber: Int
    ): SurahTimingMap? = runCatching {
        val root = JSONObject(rawJson)
        val audioFile = root.optJSONObject("audio_file")
            ?: root.optJSONArray("audio_files")?.let { arr ->
                var matched: JSONObject? = null
                for (k in 0 until arr.length()) {
                    val obj = arr.optJSONObject(k) ?: continue
                    if (obj.optInt("chapter_id", -1) == surahNumber) {
                        matched = obj
                        break
                    }
                }
                matched ?: arr.optJSONObject(0)
            }
            ?: return@runCatching null

        val audioUrl = normalizeAudioUrl(audioFile.optString("audio_url", "")) ?: return@runCatching null

        val timestamps = audioFile.optJSONArray("timestamps")
            ?: audioFile.optJSONArray("verse_timings")
            ?: return@runCatching null
        if (timestamps.length() == 0) return@runCatching null

        val entries = ArrayList<AyahTimingEntry>(timestamps.length())
        var anyWordSegments = false

        for (i in 0 until timestamps.length()) {
            val obj = timestamps.optJSONObject(i) ?: continue
            val verseKey = obj.optString("verse_key", "").trim()
            val parts = verseKey.split(":")
            val sNum = parts.getOrNull(0)?.toIntOrNull() ?: surahNumber
            val aNum = parts.getOrNull(1)?.toIntOrNull() ?: (i + 1)
            val fromMs = obj.optDouble("timestamp_from", -1.0).toInt()
            val toMs = obj.optDouble("timestamp_to", -1.0).toInt()
            if (fromMs < 0 || toMs <= fromMs) continue

            val segArray = obj.optJSONArray("segments")
            val rawWordTriplets = ArrayList<Triple<Int, Int, Int>>(segArray?.length() ?: 0)
            if (segArray != null) {
                for (j in 0 until segArray.length()) {
                    val seg = segArray.optJSONArray(j) ?: continue
                    val len = seg.length()
                    if (len == 3) {
                        val wordPos = seg.optInt(0, -1)
                        val segStart = seg.optDouble(1, -1.0).toInt()
                        val segEnd = seg.optDouble(2, -1.0).toInt()
                        if (wordPos >= 0 && segStart >= 0 && segEnd > segStart) {
                            rawWordTriplets.add(Triple(wordPos, segStart, segEnd))
                        }
                    } else if (len >= 4) {
                        // Some QDC payloads include 4-element arrays: [wordPos, wordPosEnd, startMs, endMs]
                        val firstVal = seg.optInt(0, -1)
                        val secondVal = seg.optInt(1, -1)
                        val thirdVal = seg.optDouble(2, -1.0).toInt()
                        val fourthVal = seg.optDouble(3, -1.0).toInt()
                        if (thirdVal >= 0 && fourthVal > thirdVal) {
                            val wordPos = if (firstVal > 0) firstVal else secondVal
                            if (wordPos >= 0) {
                                rawWordTriplets.add(Triple(wordPos, thirdVal, fourthVal))
                            }
                        } else if (firstVal >= 0 && secondVal >= 0 && thirdVal > secondVal) {
                            rawWordTriplets.add(Triple(firstVal, secondVal, thirdVal))
                        }
                    }
                }
            }

            // Quran.com wordIndex is 1-based; convert cleanly to 0-based (auto-handling 0-based if ever returned)
            val minWordPos = rawWordTriplets.minOfOrNull { it.first } ?: 1
            val indexOffset = if (minWordPos == 0) 0 else 1

            val wordSegments = ArrayList<WordTimingSegment>(rawWordTriplets.size)
            for ((rawWordPos, segStart, segEnd) in rawWordTriplets) {
                val zeroBased = rawWordPos - indexOffset
                if (zeroBased >= 0) {
                    wordSegments.add(
                        WordTimingSegment(
                            wordIndexZeroBased = zeroBased,
                            startMs = segStart,
                            endMs = segEnd
                        )
                    )
                }
            }

            if (wordSegments.isNotEmpty()) {
                wordSegments.sortBy { it.startMs }
                anyWordSegments = true
            }

            entries.add(
                AyahTimingEntry(
                    verseKey = verseKey.ifBlank { "$sNum:$aNum" },
                    surahNumber = sNum,
                    ayahNumber = aNum,
                    startMs = fromMs,
                    endMs = toMs,
                    wordSegments = wordSegments
                )
            )
        }

        if (entries.isEmpty()) return@runCatching null
        entries.sortBy { it.startMs }

        SurahTimingMap(
            reciterId = reciterId,
            surahNumber = surahNumber,
            quranComAudioUrl = audioUrl,
            ayahs = entries,
            hasWordSegments = anyWordSegments
        )
    }.getOrNull()
}
