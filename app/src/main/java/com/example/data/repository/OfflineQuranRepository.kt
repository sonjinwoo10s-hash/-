package com.example.data.repository

import android.content.Context
import com.example.data.model.SurahSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

data class MushafPageAyah(
    val surahNumber: Int,
    val surahName: String,
    val numberInSurah: Int,
    val globalNumber: Int,
    val text: String,
    val juz: Int,
    val page: Int
)

data class MushafSurahBlock(
    val surahNumber: Int,
    val surahName: String,
    val revelationType: String,
    val totalSurahAyahs: Int,
    val startsOnThisPage: Boolean,
    val ayahs: List<MushafPageAyah>
)

data class MushafPageData(
    val pageNumber: Int,
    val primarySurahNumber: Int,
    val primarySurahName: String,
    val juzNumber: Int,
    val blocks: List<MushafSurahBlock>
)

data class OfflineMushafBook(
    val pages: List<MushafPageData>,
    val surahs: List<SurahSummary>,
    val surahStartPages: Map<Int, Int>,
    val juzStartPages: Map<Int, Int>,
    val loadedFromLocalCache: Boolean
) {
    fun findPageForAyah(surahNumber: Int, ayahNumber: Int): Int {
        if (ayahNumber <= 1) {
            return surahStartPages[surahNumber] ?: 1
        }
        for (page in pages) {
            for (block in page.blocks) {
                if (block.surahNumber == surahNumber &&
                    block.ayahs.any { it.numberInSurah == ayahNumber }
                ) {
                    return page.pageNumber
                }
            }
        }
        return surahStartPages[surahNumber] ?: 1
    }
}

object OfflineQuranRepository {

    const val QURAN_UTHMANI_URL = "https://api.alquran.cloud/v1/quran/quran-uthmani"
    private const val LOCAL_FILE_NAME = "quran_offline.json"
    private const val ASSET_FILE_NAME = "quran_offline.json"

    private val downloadClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    val juzArabicNames: List<String> = listOf(
        "الجزء الأول",
        "الجزء الثاني",
        "الجزء الثالث",
        "الجزء الرابع",
        "الجزء الخامس",
        "الجزء السادس",
        "الجزء السابع",
        "الجزء الثامن",
        "الجزء التاسع",
        "الجزء العاشر",
        "الجزء الحادي عشر",
        "الجزء الثاني عشر",
        "الجزء الثالث عشر",
        "الجزء الرابع عشر",
        "الجزء الخامس عشر",
        "الجزء السادس عشر",
        "الجزء السابع عشر",
        "الجزء الثامن عشر",
        "الجزء التاسع عشر",
        "الجزء العشرون",
        "الجزء الحادي والعشرون",
        "الجزء الثاني والعشرون",
        "الجزء الثالث والعشرون",
        "الجزء الرابع والعشرون",
        "الجزء الخامس والعشرون",
        "الجزء السادس والعشرون",
        "الجزء السابع والعشرون",
        "الجزء الثامن والعشرون",
        "الجزء التاسع والعشرون",
        "الجزء الثلاثون"
    )

    fun getJuzArabicName(juzNumber: Int): String {
        return juzArabicNames.getOrElse(juzNumber - 1) { "الجزء $juzNumber" }
    }

    fun getLocalFile(context: Context): File {
        val target = File(com.example.data.local.PersistentOfflineStorage.quranDir(context), LOCAL_FILE_NAME)
        val legacy = File(context.applicationContext.filesDir, LOCAL_FILE_NAME)
        return com.example.data.local.PersistentOfflineStorage.migrateFileIfNeeded(legacy, target)
    }

    fun isCachedLocally(context: Context): Boolean {
        val local = getLocalFile(context)
        if (local.exists() && local.length() > 100_000L) return true
        return hasBundledAsset(context)
    }

    private fun hasBundledAsset(context: Context): Boolean {
        return try {
            context.assets.open(ASSET_FILE_NAME).use { it.available() > 0 }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Loads the complete 604-page Uthmani Mushaf strictly offline if present in internal storage
     * or app assets. Only if neither exists on very first launch, downloads it once from
     * AlQuran Cloud and saves it to filesDir/quran_offline.json for permanent offline use.
     */
    suspend fun loadOrDownloadMushaf(
        context: Context,
        onStatus: (String) -> Unit = {}
    ): Result<OfflineMushafBook> = withContext(Dispatchers.IO) {
        try {
            val localFile = getLocalFile(context)

            // 1. Always prefer internal storage cache (100% offline, zero network)
            if (localFile.exists() && localFile.length() > 100_000L) {
                onStatus("جاري فتح المصحف الشريف أوفلاين...")
                val jsonText = localFile.readText(Charsets.UTF_8)
                val parsed = parseCompleteQuranJson(jsonText, loadedFromLocalCache = true)
                if (parsed.pages.size >= 600) {
                    return@withContext Result.success(parsed)
                }
            }

            // 2. Fallback to bundled assets/quran_offline.json if packaged with APK
            if (hasBundledAsset(context)) {
                onStatus("جاري قراءة المصحف المدمج أوفلاين...")
                val jsonText = context.assets.open(ASSET_FILE_NAME).bufferedReader(Charsets.UTF_8).use {
                    it.readText()
                }
                val parsed = parseCompleteQuranJson(jsonText, loadedFromLocalCache = true)
                if (parsed.pages.size >= 600) {
                    // Copy to filesDir for fast direct file reads next time
                    runCatching { localFile.writeText(jsonText, Charsets.UTF_8) }
                    return@withContext Result.success(parsed)
                }
            }

            // 3. First-launch one-time download -> atomic write to filesDir/quran_offline.json
            onStatus("جاري تحميل المصحف الكامل (٦٠٤ صفحة) لمرة واحدة فقط...")
            val request = Request.Builder()
                .url(QURAN_UTHMANI_URL)
                .header("Accept", "application/json")
                .build()

            val downloadedJson = downloadClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}")
                }
                response.body?.string() ?: throw IOException("Empty response body")
            }

            onStatus("جاري حفظ المصحف للعمل بدون إنترنت...")
            val parsed = parseCompleteQuranJson(downloadedJson, loadedFromLocalCache = false)
            if (parsed.pages.size < 600) {
                throw IOException("بيانات المصحف غير مكتملة")
            }

            val tmpFile = File(localFile.parentFile, "$LOCAL_FILE_NAME.tmp")
            tmpFile.writeText(downloadedJson, Charsets.UTF_8)
            if (localFile.exists()) {
                localFile.delete()
            }
            if (!tmpFile.renameTo(localFile)) {
                localFile.writeText(downloadedJson, Charsets.UTF_8)
                tmpFile.delete()
            }

            Result.success(parsed.copy(loadedFromLocalCache = true))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private val bismillahPrefixes = listOf(
        "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
        "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
        "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
        "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
    )

    private fun stripLeadingBasmalahIfNeeded(
        surahNumber: Int,
        numberInSurah: Int,
        rawText: String
    ): String {
        if (surahNumber == 1 || surahNumber == 9 || numberInSurah != 1) {
            return rawText.trim()
        }
        val trimmed = rawText.trimStart('\uFEFF', ' ', '\n', '\r')
        for (prefix in bismillahPrefixes) {
            if (trimmed.startsWith(prefix)) {
                return trimmed.removePrefix(prefix).trim()
            }
        }
        // Fallback: if first 4 space-separated words match normalized Basmalah
        val words = trimmed.split(Regex("\\s+"))
        if (words.size > 4) {
            val firstFour = words.take(4).joinToString(" ")
            val normalized = firstFour
                .replace(Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]"), "")
                .replace('ٱ', 'ا')
            if (normalized == "بسم الله الرحمن الرحيم") {
                return words.drop(4).joinToString(" ").trim()
            }
        }
        return trimmed
    }

    private fun parseCompleteQuranJson(
        jsonString: String,
        loadedFromLocalCache: Boolean
    ): OfflineMushafBook {
        val root = JSONObject(jsonString)
        val dataObj = root.optJSONObject("data") ?: root
        val surahsArray = dataObj.getJSONArray("surahs")

        // Map of pageNumber (1..604) -> ordered list of (SurahMeta, Ayah)
        data class SurahMeta(
            val number: Int,
            val name: String,
            val englishName: String,
            val revelationType: String,
            val totalAyahs: Int
        )

        val pageBuckets = HashMap<Int, MutableList<Pair<SurahMeta, MushafPageAyah>>>(604)
        val surahSummaries = ArrayList<SurahSummary>(114)
        val surahStartPages = LinkedHashMap<Int, Int>(114)
        val juzStartPages = LinkedHashMap<Int, Int>(30)

        for (i in 0 until surahsArray.length()) {
            val sObj = surahsArray.getJSONObject(i)
            val sNum = sObj.getInt("number")
            val sName = sObj.getString("name")
            val sEnglish = sObj.optString("englishName", "")
            val sRevType = sObj.optString("revelationType", "Meccan")
            val ayahsArray = sObj.getJSONArray("ayahs")
            val totalAyahs = ayahsArray.length()

            val meta = SurahMeta(
                number = sNum,
                name = sName,
                englishName = sEnglish,
                revelationType = sRevType,
                totalAyahs = totalAyahs
            )

            var firstPageOfSurah = 1

            for (j in 0 until totalAyahs) {
                val aObj = ayahsArray.getJSONObject(j)
                val globalNum = aObj.optInt("number", 0)
                val numInSurah = aObj.getInt("numberInSurah")
                val pageNum = aObj.getInt("page").coerceIn(1, 604)
                val juzNum = aObj.optInt("juz", 1).coerceIn(1, 30)
                val cleanedText = stripLeadingBasmalahIfNeeded(
                    surahNumber = sNum,
                    numberInSurah = numInSurah,
                    rawText = aObj.getString("text")
                )

                if (j == 0) {
                    firstPageOfSurah = pageNum
                    surahStartPages[sNum] = pageNum
                }
                if (!juzStartPages.containsKey(juzNum)) {
                    juzStartPages[juzNum] = pageNum
                }

                val ayah = MushafPageAyah(
                    surahNumber = sNum,
                    surahName = sName,
                    numberInSurah = numInSurah,
                    globalNumber = globalNum,
                    text = cleanedText,
                    juz = juzNum,
                    page = pageNum
                )
                pageBuckets.getOrPut(pageNum) { ArrayList() }.add(meta to ayah)
            }

            surahSummaries.add(
                SurahSummary(
                    number = sNum,
                    name = sName,
                    englishName = sEnglish,
                    numberOfAyahs = totalAyahs,
                    revelationType = sRevType,
                    startPage = firstPageOfSurah
                )
            )
        }

        val orderedPages = ArrayList<MushafPageData>(604)
        val sortedPageNumbers = pageBuckets.keys.sorted()

        for (pageNum in sortedPageNumbers) {
            val entries = pageBuckets[pageNum].orEmpty()
            if (entries.isEmpty()) continue

            // Group consecutive entries on the page by Surah while preserving exact order
            val blocks = ArrayList<MushafSurahBlock>()
            var currentMeta: SurahMeta? = null
            var currentAyahs = ArrayList<MushafPageAyah>()

            fun flushBlock() {
                val m = currentMeta ?: return
                if (currentAyahs.isEmpty()) return
                val startsHere = currentAyahs.first().numberInSurah == 1
                blocks.add(
                    MushafSurahBlock(
                        surahNumber = m.number,
                        surahName = m.name,
                        revelationType = m.revelationType,
                        totalSurahAyahs = m.totalAyahs,
                        startsOnThisPage = startsHere,
                        ayahs = currentAyahs
                    )
                )
            }

            for ((meta, ayah) in entries) {
                if (currentMeta?.number != meta.number) {
                    flushBlock()
                    currentMeta = meta
                    currentAyahs = ArrayList()
                }
                currentAyahs.add(ayah)
            }
            flushBlock()

            val firstAyah = entries.first().second
            orderedPages.add(
                MushafPageData(
                    pageNumber = pageNum,
                    primarySurahNumber = firstAyah.surahNumber,
                    primarySurahName = firstAyah.surahName,
                    juzNumber = firstAyah.juz,
                    blocks = blocks
                )
            )
        }

        return OfflineMushafBook(
            pages = orderedPages,
            surahs = surahSummaries,
            surahStartPages = surahStartPages,
            juzStartPages = juzStartPages,
            loadedFromLocalCache = loadedFromLocalCache
        )
    }
}
