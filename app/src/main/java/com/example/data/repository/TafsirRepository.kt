package com.example.data.repository

import android.content.Context
import com.example.data.local.OfflineIslamicData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

enum class TafsirBook(
    val bookId: String,
    val arabicName: String,
    val shortAuthor: String,
    val alQuranEdition: String?,
    val spa5kSlug: String,
    val quranComTafsirId: Int?
) {
    MUYASSAR(
        bookId = "ar.muyassar",
        arabicName = "التفسير الميسر",
        shortAuthor = "مجمع الملك فهد",
        alQuranEdition = "ar.muyassar",
        spa5kSlug = "ar-tafsir-muyassar",
        quranComTafsirId = 16
    ),
    JALALAYN(
        bookId = "ar.jalalayn",
        arabicName = "تفسير الجلالين",
        shortAuthor = "المحلي والسيوطي",
        alQuranEdition = "ar.jalalayn",
        spa5kSlug = "ar-tafsir-al-jalalayn",
        quranComTafsirId = null
    ),
    IBN_KATHIR(
        bookId = "ar-tafsir-ibn-kathir",
        arabicName = "تفسير ابن كثير",
        shortAuthor = "الحافظ ابن كثير",
        alQuranEdition = null,
        spa5kSlug = "ar-tafsir-ibn-kathir",
        quranComTafsirId = 14
    ),
    SAADI(
        bookId = "ar-tafseer-al-saddi",
        arabicName = "تفسير السعدي",
        shortAuthor = "الشيخ عبد الرحمن السعدي",
        alQuranEdition = null,
        spa5kSlug = "ar-tafseer-al-saddi",
        quranComTafsirId = 91
    ),
    QURTUBI(
        bookId = "ar.qurtubi",
        arabicName = "تفسير القرطبي",
        shortAuthor = "الإمام القرطبي",
        alQuranEdition = "ar.qurtubi",
        spa5kSlug = "ar-tafseer-al-qurtubi",
        quranComTafsirId = 90
    ),
    TABARI(
        bookId = "ar-tafsir-al-tabari",
        arabicName = "تفسير الطبري",
        shortAuthor = "الإمام الطبري",
        alQuranEdition = null,
        spa5kSlug = "ar-tafsir-al-tabari",
        quranComTafsirId = 15
    )
}

object TafsirRepository {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    // Low-RAM bounded cache: hold at most ONE full offline book map in RAM at a time,
    // and clear it immediately when the user closes the Mushaf so GC frees RAM.
    private val fullBookMemoryCache = ConcurrentHashMap<String, Map<String, String>>()

    // Bounded session cache of single-ayah online fetches (max 40 entries)
    private val singleAyahCache = ConcurrentHashMap<String, String>()

    fun clearMemoryCache() {
        fullBookMemoryCache.clear()
        singleAyahCache.clear()
    }

    fun getLocalBookFile(context: Context, book: TafsirBook): File {
        val fileName = "tafsir_${book.bookId}.json"
        val target = File(com.example.data.local.PersistentOfflineStorage.tafsirDir(context), fileName)
        val legacy = File(context.applicationContext.filesDir, fileName)
        return com.example.data.local.PersistentOfflineStorage.migrateFileIfNeeded(legacy, target)
    }

    fun isBookDownloaded(context: Context, book: TafsirBook): Boolean {
        val f = getLocalBookFile(context, book)
        return f.exists() && f.isFile && f.length() > 512L
    }

    fun getDownloadedBookIds(context: Context): Set<String> {
        return TafsirBook.entries
            .filter { isBookDownloaded(context, it) }
            .map { it.bookId }
            .toSet()
    }

    /**
     * Strict offline-first: always checks in-memory cache and local offline JSON (`tafsir_{bookId}.json`)
     * before making any online API call.
     */
    suspend fun getAyahTafsir(
        context: Context,
        book: TafsirBook,
        surahNumber: Int,
        ayahNumber: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        val verseKey = "$surahNumber:$ayahNumber"

        // 1. Check in-memory full book cache
        fullBookMemoryCache[book.bookId]?.get(verseKey)?.let { cached ->
            if (cached.isNotBlank()) return@withContext Result.success(cached)
        }

        // 2. Check local offline JSON file (tafsir_{bookId}.json) — STRICT OFFLINE FIRST
        val localFile = getLocalBookFile(context, book)
        if (localFile.exists() && localFile.isFile && localFile.length() > 512L) {
            val loadedMap = runCatching { loadLocalBookMap(localFile) }.getOrNull()
            if (!loadedMap.isNullOrEmpty()) {
                // Keep only the single active book in RAM to protect 2GB-3GB devices
                fullBookMemoryCache.clear()
                fullBookMemoryCache[book.bookId] = loadedMap
                loadedMap[verseKey]?.takeIf { it.isNotBlank() }?.let { text ->
                    return@withContext Result.success(text)
                }
            }
        }

        // 3. Check session single-ayah memory cache
        val sessionKey = "${book.bookId}|$verseKey"
        singleAyahCache[sessionKey]?.let { cached ->
            return@withContext Result.success(cached)
        }

        // 4. Fetch single Ayah online
        val fetchedText = fetchSingleAyahOnline(book, surahNumber, ayahNumber)
        if (!fetchedText.isNullOrBlank()) {
            val cleaned = cleanTafsirText(fetchedText)
            if (singleAyahCache.size > 40) singleAyahCache.clear()
            singleAyahCache[sessionKey] = cleaned
            return@withContext Result.success(cleaned)
        }

        // 5. Built-in offline fallback for short Surahs in Muyassar
        if (book == TafsirBook.MUYASSAR) {
            val builtIn = OfflineIslamicData.offlineSurahAyahs[surahNumber]
                ?.find { it.numberInSurah == ayahNumber }
                ?.tafsir
            if (!builtIn.isNullOrBlank()) {
                return@withContext Result.success(builtIn)
            }
        }

        Result.failure(IOException("تعذّر تحميل التفسير حالياً. تأكد من الاتصال بالإنترنت أو حمّل التفسير للعمل بدون نت."))
    }

    /**
     * Downloads the full Tafsir book once into `filesDir/offline_tafsir/tafsir_{bookId}.json`
     * with multi-mirror fallback (AlQuran Cloud -> jsDelivr spa5k -> GitHub Raw spa5k -> Quran.com v4)
     * so all 6 books download reliably and pass disk verification.
     */
    suspend fun downloadFullBook(
        context: Context,
        book: TafsirBook,
        onProgress: (Float) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            onProgress(0.03f)
            val entriesMap = LinkedHashMap<String, String>(6236)

            // Fast single-request download for editions hosted on AlQuran Cloud
            var fetchedFromAlQuran = false
            if (book.alQuranEdition != null) {
                runCatching {
                    onProgress(0.12f)
                    val url = "https://api.alquran.cloud/v1/quran/${book.alQuranEdition}"
                    val raw = executeGet(url)
                    if (!raw.isNullOrBlank()) {
                        onProgress(0.65f)
                        val root = JSONObject(raw)
                        val data = root.optJSONObject("data")
                        val surahs = data?.optJSONArray("surahs")
                        if (surahs != null && surahs.length() >= 114) {
                            for (i in 0 until surahs.length()) {
                                val sObj = surahs.optJSONObject(i) ?: continue
                                val sNum = sObj.optInt("number", i + 1)
                                val ayahs = sObj.optJSONArray("ayahs") ?: continue
                                for (j in 0 until ayahs.length()) {
                                    val aObj = ayahs.optJSONObject(j) ?: continue
                                    val aNum = aObj.optInt("numberInSurah", j + 1)
                                    val txt = cleanTafsirText(aObj.optString("text", ""))
                                    if (txt.isNotBlank()) {
                                        entriesMap["$sNum:$aNum"] = txt
                                    }
                                }
                            }
                            if (entriesMap.size >= 5000) {
                                fetchedFromAlQuran = true
                                onProgress(0.88f)
                            }
                        }
                    }
                }
            }

            // Batched 114-Surah download across resilient mirrors (Ibn Kathir, Saadi, Qurtubi, Tabari, or fallback)
            if (!fetchedFromAlQuran) {
                entriesMap.clear()
                var completedSurahs = 0
                val surahBatches = (1..114).chunked(4)
                for (batch in surahBatches) {
                    val batchResults = coroutineScope {
                        batch.map { surahNum ->
                            async(Dispatchers.IO) {
                                surahNum to fetchSurahWithMirrors(book, surahNum)
                            }
                        }.awaitAll()
                    }
                    for ((sNum, ayahMap) in batchResults) {
                        for ((aNum, txt) in ayahMap) {
                            entriesMap["$sNum:$aNum"] = txt
                        }
                        completedSurahs++
                    }
                    onProgress((completedSurahs.toFloat() / 114f).coerceIn(0.05f, 0.92f))
                }
            }

            if (entriesMap.size < 100) {
                return@withContext Result.failure(IOException("تعذّر تحميل كامل الكتاب، يرجى المحاولة لاحقاً"))
            }

            // Write normalized compact JSON atomically to filesDir/offline_tafsir/tafsir_{bookId}.json
            onProgress(0.95f)
            val jsonObj = JSONObject()
            for ((k, v) in entriesMap) {
                jsonObj.put(k, v)
            }
            val targetFile = getLocalBookFile(context, book)
            val tmpFile = File(targetFile.parentFile, "tafsir_${book.bookId}.json.tmp")
            tmpFile.writeText(jsonObj.toString(), Charsets.UTF_8)
            if (targetFile.exists()) targetFile.delete()
            if (!tmpFile.renameTo(targetFile)) {
                targetFile.writeText(jsonObj.toString(), Charsets.UTF_8)
                tmpFile.delete()
            }

            if (!targetFile.exists() || targetFile.length() <= 512L) {
                return@withContext Result.failure(IOException("تعذّر حفظ ملف التفسير على مساحة الجهاز"))
            }

            // Keep memory bounded while ensuring immediate offline availability
            fullBookMemoryCache.clear()
            fullBookMemoryCache[book.bookId] = entriesMap
            onProgress(1.0f)
            Result.success(entriesMap.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchSingleAyahOnline(book: TafsirBook, surahNumber: Int, ayahNumber: Int): String? {
        // 1. Primary: AlQuran Cloud if available for this book
        if (book.alQuranEdition != null) {
            val url = "https://api.alquran.cloud/v1/ayah/$surahNumber:$ayahNumber/${book.alQuranEdition}"
            val raw = executeGet(url)
            if (!raw.isNullOrBlank()) {
                val parsed = runCatching {
                    JSONObject(raw).optJSONObject("data")?.optString("text", "")
                }.getOrNull()
                if (!parsed.isNullOrBlank()) return parsed
            }
        }

        // 2. jsDelivr & GitHub Raw spa5k single-ayah endpoints
        val singleAyahUrls = listOf(
            "https://cdn.jsdelivr.net/gh/spa5k/tafsir_api@main/tafsir/${book.spa5kSlug}/$surahNumber/$ayahNumber.json",
            "https://raw.githubusercontent.com/spa5k/tafsir_api/main/tafsir/${book.spa5kSlug}/$surahNumber/$ayahNumber.json"
        )
        for (url in singleAyahUrls) {
            val raw = executeGet(url)
            if (!raw.isNullOrBlank()) {
                val parsed = runCatching {
                    JSONObject(raw).optString("text", "")
                }.getOrNull()
                if (!parsed.isNullOrBlank()) return parsed
            }
        }

        // 3. Quran.com v4 single-ayah endpoint if available
        if (book.quranComTafsirId != null) {
            val qUrl = "https://api.quran.com/api/v4/tafsirs/${book.quranComTafsirId}/by_ayah/$surahNumber:$ayahNumber"
            val qRaw = executeGet(qUrl)
            if (!qRaw.isNullOrBlank()) {
                val parsed = runCatching {
                    JSONObject(qRaw).optJSONObject("tafsir")?.optString("text", "")
                }.getOrNull()
                if (!parsed.isNullOrBlank()) return parsed
            }
        }

        // 4. Surah-level fallback (handles grouped verses)
        val surahMap = fetchSurahWithMirrors(book, surahNumber)
        return surahMap[ayahNumber]
    }

    private fun fetchSurahWithMirrors(book: TafsirBook, surahNumber: Int): Map<Int, String> {
        // Mirror 1: jsDelivr spa5k
        val fromJsDelivr = fetchSurahFromSpa5k(
            "https://cdn.jsdelivr.net/gh/spa5k/tafsir_api@main/tafsir/${book.spa5kSlug}/$surahNumber.json"
        )
        if (fromJsDelivr.isNotEmpty()) return fromJsDelivr

        // Mirror 2: GitHub Raw spa5k
        val fromGitHubRaw = fetchSurahFromSpa5k(
            "https://raw.githubusercontent.com/spa5k/tafsir_api/main/tafsir/${book.spa5kSlug}/$surahNumber.json"
        )
        if (fromGitHubRaw.isNotEmpty()) return fromGitHubRaw

        // Mirror 3: Official Quran.com v4 chapter tafsir API
        val qId = book.quranComTafsirId
        if (qId != null) {
            val fromQuranCom = fetchSurahFromQuranComV4(qId, surahNumber)
            if (fromQuranCom.isNotEmpty()) return fromQuranCom
        }

        return emptyMap()
    }

    private fun fetchSurahFromSpa5k(url: String): Map<Int, String> {
        val raw = executeGet(url) ?: return emptyMap()
        return runCatching {
            val obj = JSONObject(raw)
            val ayahs = obj.optJSONArray("ayahs") ?: return@runCatching emptyMap()
            val map = HashMap<Int, String>(ayahs.length())
            for (i in 0 until ayahs.length()) {
                val item = ayahs.optJSONObject(i) ?: continue
                val aNum = item.optInt("ayah", 0)
                val txt = cleanTafsirText(item.optString("text", ""))
                if (aNum > 0 && txt.isNotBlank()) {
                    map[aNum] = txt
                }
            }
            map
        }.getOrDefault(emptyMap())
    }

    private fun fetchSurahFromQuranComV4(tafsirId: Int, surahNumber: Int): Map<Int, String> {
        val url = "https://api.quran.com/api/v4/tafsirs/$tafsirId/by_chapter/$surahNumber?per_page=300"
        val raw = executeGet(url) ?: return emptyMap()
        return runCatching {
            val obj = JSONObject(raw)
            val arr = obj.optJSONArray("tafsirs") ?: return@runCatching emptyMap()
            val map = HashMap<Int, String>(arr.length())
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val verseKey = item.optString("verse_key", "")
                val aNum = verseKey.substringAfter(":", "").toIntOrNull()
                    ?: item.optInt("verse_number", 0)
                val txt = cleanTafsirText(item.optString("text", ""))
                if (aNum > 0 && txt.isNotBlank()) {
                    map[aNum] = txt
                }
            }
            map
        }.getOrDefault(emptyMap())
    }

    private fun loadLocalBookMap(file: File): Map<String, String> {
        return runCatching {
            val raw = file.readText(Charsets.UTF_8)
            val obj = JSONObject(raw)
            val result = HashMap<String, String>(obj.length())
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                result[key] = obj.optString(key, "")
            }
            result
        }.getOrDefault(emptyMap())
    }

    private fun executeGet(url: String): String? {
        return runCatching {
            val req = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build()
            httpClient.newCall(req).execute().use { res ->
                if (!res.isSuccessful) null else res.body?.string()
            }
        }.getOrNull()
    }

    private fun cleanTafsirText(raw: String): String {
        return raw
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</p\\s*>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), "")
            .replace("&nbsp;", " ")
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }
}
