package com.example.data.repository

import android.content.Context
import android.util.LruCache
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
     * Maps supported popular reciters to their official Quran.com v4 `chapter_recitations` IDs.
     * Returns `null` for mp3quran.net-only reciters so they cleanly fall back to full-Surah playback
     * with Ayah highlighting disabled (preventing any inaccurate timing).
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
            "featured_ghamdi" -> return 4
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
     * Loads official Quran.com v4 audio URL + millisecond timestamps from:
     *  1) Tiny LRU memory cache
     *  2) Local disk cache (`filesDir/quran_timings/r{reciterId}_s{surahNumber}.json`)
     *  3) Remote Quran.com v4 API (`chapter_recitations/{reciterId}/{surahNumber}?segments=true`)
     */
    suspend fun fetchSurahTimings(
        context: Context?,
        reciter: ReciterOption,
        surahNumber: Int
    ): SurahTimingMap? = withContext(Dispatchers.IO) {
        val reciterId = resolveQuranComReciterId(reciter) ?: return@withContext null
        val cacheKey = "${reciterId}_$surahNumber"

        synchronized(memoryCache) {
            memoryCache.get(cacheKey)?.let { return@withContext it }
        }

        // 1. Try local disk cache first (instant & offline-ready)
        if (context != null) {
            val diskFile = getDiskCacheFile(context, reciterId, surahNumber)
            if (diskFile.exists() && diskFile.length() > 32L) {
                val cached = runCatching {
                    parseTimingJson(diskFile.readText(Charsets.UTF_8), reciterId, surahNumber)
                }.getOrNull()
                if (cached != null) {
                    synchronized(memoryCache) { memoryCache.put(cacheKey, cached) }
                    return@withContext cached
                }
            }
        }

        // 2. Fetch fresh from Quran.com v4 API
        val url = "https://api.quran.com/api/v4/chapter_recitations/$reciterId/$surahNumber?segments=true"
        try {
            val req = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build()
            val body = client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return@withContext null
                res.body?.string() ?: return@withContext null
            }

            val parsed = parseTimingJson(body, reciterId, surahNumber) ?: return@withContext null

            // Persist lightweight JSON locally for future instant reuse
            if (context != null) {
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

            synchronized(memoryCache) { memoryCache.put(cacheKey, parsed) }
            parsed
        } catch (_: Exception) {
            null
        }
    }

    private fun parseTimingJson(
        rawJson: String,
        reciterId: Int,
        surahNumber: Int
    ): SurahTimingMap? {
        val root = JSONObject(rawJson)
        val audioFile = root.optJSONObject("audio_file") ?: return null
        val audioUrl = audioFile.optString("audio_url", "").trim()
        if (audioUrl.isBlank() || (!audioUrl.startsWith("https://") && !audioUrl.startsWith("http://"))) {
            return null
        }

        val timestamps = audioFile.optJSONArray("timestamps") ?: return null
        if (timestamps.length() == 0) return null

        val entries = ArrayList<AyahTimingEntry>(timestamps.length())
        var anyWordSegments = false

        for (i in 0 until timestamps.length()) {
            val obj = timestamps.optJSONObject(i) ?: continue
            val verseKey = obj.optString("verse_key", "").trim()
            val parts = verseKey.split(":")
            val sNum = parts.getOrNull(0)?.toIntOrNull() ?: surahNumber
            val aNum = parts.getOrNull(1)?.toIntOrNull() ?: (i + 1)
            val fromMs = obj.optInt("timestamp_from", -1)
            val toMs = obj.optInt("timestamp_to", -1)
            if (fromMs < 0 || toMs <= fromMs) continue

            val segArray = obj.optJSONArray("segments")
            val wordSegments = ArrayList<WordTimingSegment>(segArray?.length() ?: 0)
            if (segArray != null) {
                for (j in 0 until segArray.length()) {
                    val seg = segArray.optJSONArray(j) ?: continue
                    if (seg.length() >= 3) {
                        val wordPos1Based = seg.optInt(0, -1)
                        val segStart = seg.optInt(1, -1)
                        val segEnd = seg.optInt(2, -1)
                        if (wordPos1Based > 0 && segStart >= 0 && segEnd > segStart) {
                            wordSegments.add(
                                WordTimingSegment(
                                    wordIndexZeroBased = wordPos1Based - 1,
                                    startMs = segStart,
                                    endMs = segEnd
                                )
                            )
                        }
                    }
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

        if (entries.isEmpty()) return null
        entries.sortBy { it.startMs }

        return SurahTimingMap(
            reciterId = reciterId,
            surahNumber = surahNumber,
            quranComAudioUrl = audioUrl,
            ayahs = entries,
            hasWordSegments = anyWordSegments
        )
    }
}
