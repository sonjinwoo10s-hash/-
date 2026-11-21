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
    val spa5kSlug: String
) {
    MUYASSAR(
        bookId = "ar.muyassar",
        arabicName = "التفسير الميسر",
        shortAuthor = "مجمع الملك فهد",
        alQuranEdition = "ar.muyassar",
        spa5kSlug = "ar-tafsir-muyassar"
    ),
    JALALAYN(
        bookId = "ar.jalalayn",
        arabicName = "تفسير الجلالين",
        shortAuthor = "المحلي والسيوطي",
        alQuranEdition = "ar.jalalayn",
        spa5kSlug = "ar-tafsir-al-jalalayn"
    ),
    IBN_KATHIR(
        bookId = "ar-tafsir-ibn-kathir",
        arabicName = "تفسير ابن كثير",
        shortAuthor = "الحافظ ابن كثير",
        alQuranEdition = null,
        spa5kSlug = "ar-tafsir-ibn-kathir"
    ),
    SAADI(
        bookId = "ar-tafseer-al-saddi",
        arabicName = "تفسير السعدي",
        shortAuthor = "الشيخ عبد الرحمن السعدي",
        alQuranEdition = null,
        spa5kSlug = "ar-tafseer-al-saddi"
    ),
    QURTUBI(
        bookId = "ar.qurtubi",
        arabicName = "تفسير القرطبي",
        shortAuthor = "الإمام القرطبي",
        alQuranEdition = "ar.qurtubi",
        spa5kSlug = "ar-tafseer-al-qurtubi"
    ),
    TABARI(
        bookId = "ar-tafsir-al-tabari",
        arabicName = "تفسير الطبري",
        shortAuthor = "الإمام الطبري",
        alQuranEdition = null,
        spa5kSlug = "ar-tafsir-al-tabari"
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
        return f.exists() && f.length() > 10_000L
    }

    fun getDownloadedBookIds(context: Context): Set<String> {
        return TafsirBook.entries
            .filter { isBookDownloaded(context, it) }
            .map { it.bookId }
            .toSet()
    }

    /**
     * Always checks local offline JSON first (`tafsir_{bookId}.json`);
     * if not downloaded yet, fetches the single Ayah's Tafsir online from
     * api.alquran.cloud and/or cdn.jsdelivr.net/gh/spa5k/tafsir_api@main/tafsir/.
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

        // 2. Check local offline JSON file (tafsir_{bookId}.json)
        val localFile = getLocalBookFile(context, book)
        if (localFile.exists() && localFile.length() > 10_000L) {
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
     * Downloads the full Tafsir book once into `getFilesDir()/tafsir_{bookId}.json`
     * reporting progress (0f..1f) so the UI displays a smooth progress bar.
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
                                val sObj = surahs.getJSONObject(i)
                                val sNum = sObj.getInt("number")
                                val ayahs = sObj.getJSONArray("ayahs")
                                for (j in 0 until ayahs.length()) {
                                    val aObj = ayahs.getJSONObject(j)
                                    val aNum = aObj.getInt("numberInSurah")
                                    val txt = cleanTafsirText(aObj.optString("text", ""))
                                    if (txt.isNotBlank()) {
                                        entriesMap["$sNum:$aNum"] = txt
                                    }
                                }
                            }
                            if (entriesMap.size >= 6000) {
                                fetchedFromAlQuran = true
                                onProgress(0.88f)
                            }
                        }
                    }
                }
            }

            // Batched 114-Surah download from jsDelivr spa5k tafsir_api (Ibn Kathir, Saadi, Tabari, or fallback)
            if (!fetchedFromAlQuran) {
                entriesMap.clear()
                var completedSurahs = 0
                val surahBatches = (1..114).chunked(6)
                for (batch in surahBatches) {
                    val batchResults = coroutineScope {
                        batch.map { surahNum ->
                            async(Dispatchers.IO) {
                                surahNum to fetchSurahFromSpa5k(book.spa5kSlug, surahNum)
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

            // Write normalized compact JSON atomically to filesDir/tafsir_{bookId}.json
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

            fullBookMemoryCache[book.bookId] = entriesMap
            onProgress(1.0f)
            Result.success(entriesMap.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchSingleAyahOnline(book: TafsirBook, surahNumber: Int, ayahNumber: Int): String? {
        // Primary: AlQuran Cloud if available for this book
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

        // Primary for spa5k books & fallback for AlQuran Cloud books
        val spaUrl = "https://cdn.jsdelivr.net/gh/spa5k/tafsir_api@main/tafsir/${book.spa5kSlug}/$surahNumber/$ayahNumber.json"
        val spaRaw = executeGet(spaUrl)
        if (!spaRaw.isNullOrBlank()) {
            val parsed = runCatching {
                JSONObject(spaRaw).optString("text", "")
            }.getOrNull()
            if (!parsed.isNullOrBlank()) return parsed
        }

        // Secondary fallback: full Surah JSON from spa5k (some verses are grouped in Surah JSON)
        val surahMap = fetchSurahFromSpa5k(book.spa5kSlug, surahNumber)
        return surahMap[ayahNumber]
    }

    private fun fetchSurahFromSpa5k(slug: String, surahNumber: Int): Map<Int, String> {
        val url = "https://cdn.jsdelivr.net/gh/spa5k/tafsir_api@main/tafsir/$slug/$surahNumber.json"
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

    private fun loadLocalBookMap(file: File): Map<String, String> {
        val raw = file.readText(Charsets.UTF_8)
        val obj = JSONObject(raw)
        val result = HashMap<String, String>(obj.length())
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            result[key] = obj.optString(key, "")
        }
        return result
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
