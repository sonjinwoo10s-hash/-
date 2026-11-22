package com.example.data.repository

import android.content.Context
import android.util.JsonReader
import android.util.JsonToken
import com.example.data.local.HadithLibraryData
import com.example.data.model.HadithBookCollection
import com.example.data.model.HadithChapters
import com.example.data.model.LibraryHadith
import com.example.data.model.OfflineHadithBookMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object OfflineHadithRepository {

    const val HADITH_CDN_BASE_URL = "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/"
    private const val HADITH_GITHUB_RAW_BASE_URL = "https://raw.githubusercontent.com/fawazahmed0/hadith-api/1/editions/"
    private const val META_COUNTS_FILE = "hadith_meta_counts.json"
    private const val FAVORITES_SNAPSHOT_FILE = "hadith_favorites.json"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Builds resilient candidate mirror URLs for each Hadith book so CDN hiccups or edition
     * filename variants (`.min.json` vs `.json`) fall back seamlessly without failing the download.
     */
    private fun candidateDownloadUrls(book: HadithBookCollection): List<String> {
        val editionFiles = linkedSetOf<String>().apply {
            add(book.editionFile)
            val unminified = book.editionFile.replace(".min.json", ".json")
            add(unminified)
        }
        return buildList {
            for (file in editionFiles) {
                add("$HADITH_CDN_BASE_URL$file")
                add("$HADITH_GITHUB_RAW_BASE_URL$file")
            }
        }.distinct()
    }

    /**
     * Safely cleans up any leftover cached files/metadata from books removed in newer app versions
     * (without touching any other downloaded Hadith collections or favorites).
     */
    fun cleanupLegacyRemovedBooks(context: Context) {
        val removedIds = listOf("riyadussalihin")
        for (legacyId in removedIds) {
            runCatching {
                val fileName = "hadith_${legacyId}.json"
                val persistentFile = File(com.example.data.local.PersistentOfflineStorage.hadithDir(context), fileName)
                if (persistentFile.exists()) persistentFile.delete()
                val legacyFile = File(context.applicationContext.filesDir, fileName)
                if (legacyFile.exists()) legacyFile.delete()
                removeBookCount(context, legacyId)
            }
        }
    }

    // Low-RAM memory cache: cleared immediately when leaving a book/screen
    private val memoryCache = ConcurrentHashMap<String, List<LibraryHadith>>()

    fun clearMemoryCache() {
        memoryCache.clear()
    }

    fun getLocalBookFile(context: Context, book: HadithBookCollection): File {
        val fileName = "hadith_${book.id}.json"
        val target = File(com.example.data.local.PersistentOfflineStorage.hadithDir(context), fileName)
        val legacy = File(context.applicationContext.filesDir, fileName)
        return com.example.data.local.PersistentOfflineStorage.migrateFileIfNeeded(legacy, target)
    }

    private fun getMetaCountsFile(context: Context): File {
        val target = File(com.example.data.local.PersistentOfflineStorage.hadithDir(context), META_COUNTS_FILE)
        val legacy = File(context.applicationContext.filesDir, META_COUNTS_FILE)
        return com.example.data.local.PersistentOfflineStorage.migrateFileIfNeeded(legacy, target)
    }

    private fun getFavoritesFile(context: Context): File {
        val target = File(com.example.data.local.PersistentOfflineStorage.hadithDir(context), FAVORITES_SNAPSHOT_FILE)
        val legacy = File(context.applicationContext.filesDir, FAVORITES_SNAPSHOT_FILE)
        return com.example.data.local.PersistentOfflineStorage.migrateFileIfNeeded(legacy, target)
    }

    fun isBookDownloaded(context: Context, book: HadithBookCollection): Boolean {
        val file = getLocalBookFile(context, book)
        return file.exists() && file.length() > 1_024L
    }

    fun getAllBookStatuses(context: Context): Map<String, OfflineHadithBookMeta> {
        val savedCounts = readSavedCounts(context)
        return HadithBookCollection.entries.associate { book ->
            val file = getLocalBookFile(context, book)
            val downloaded = file.exists() && file.length() > 1_024L
            val count = if (downloaded) {
                memoryCache[book.id]?.size ?: savedCounts[book.id] ?: book.expectedCount
            } else {
                book.expectedCount
            }
            book.id to OfflineHadithBookMeta(
                bookId = book.id,
                isDownloaded = downloaded,
                actualCount = count,
                fileSizeBytes = if (downloaded) file.length() else 0L
            )
        }
    }

    /**
     * Normalizes Arabic text for accurate offline search without harakat/tashkeel,
     * and converts Arabic-Indic digits (٠-٩) to ASCII (0-9) for Hadith number search.
     */
    fun normalizeArabicForSearch(input: String): String {
        if (input.isEmpty()) return ""
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                in '\u0610'..'\u061A',
                in '\u064B'..'\u065F',
                '\u0670',
                in '\u06D6'..'\u06ED',
                '\u0640' -> Unit // Strip diacritics / harakat / tatweel
                'أ', 'إ', 'آ', 'ٱ' -> sb.append('ا')
                'ؤ' -> sb.append('و')
                'ئ' -> sb.append('ي')
                'ة' -> sb.append('ه')
                'ى' -> sb.append('ي')
                '٠' -> sb.append('0')
                '١' -> sb.append('1')
                '٢' -> sb.append('2')
                '٣' -> sb.append('3')
                '٤' -> sb.append('4')
                '٥' -> sb.append('5')
                '٦' -> sb.append('6')
                '٧' -> sb.append('7')
                '٨' -> sb.append('8')
                '٩' -> sb.append('9')
                else -> sb.append(ch.lowercaseChar())
            }
        }
        return sb.toString().trim()
    }

    /**
     * Downloads a Hadith book JSON once with multi-mirror fallback into
     * `offline_hadith/hadith_{bookId}.json`, streaming directly to disk and reporting 0f..1f progress.
     */
    suspend fun downloadBookOffline(
        context: Context,
        book: HadithBookCollection,
        onProgress: (Float) -> Unit
    ): Result<List<LibraryHadith>> = withContext(Dispatchers.IO) {
        val targetFile = getLocalBookFile(context, book)
        targetFile.parentFile?.mkdirs()
        val tmpFile = File(targetFile.parentFile, "hadith_${book.id}.json.tmp")

        val urls = candidateDownloadUrls(book)
        var lastError: Throwable? = null

        for (url in urls) {
            try {
                if (tmpFile.exists()) tmpFile.delete()
                onProgress(0.02f)

                val req = Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .header("User-Agent", "RafiqAlMuslim-Android")
                    .build()

                var httpFailureMsg: String? = null
                httpClient.newCall(req).execute().use { response ->
                    if (!response.isSuccessful) {
                        httpFailureMsg = "تعذّر تحميل ${book.arabicTitle} (رمز ${response.code})"
                        return@use
                    }
                    val body = response.body
                    if (body == null) {
                        httpFailureMsg = "استجابة فارغة من الخادم"
                        return@use
                    }

                    val contentLength = body.contentLength()
                    val totalEstimate = if (contentLength > 0L) contentLength else book.estimatedBytes
                    var bytesReadTotal = 0L
                    var lastReported = 0.02f

                    BufferedInputStream(body.byteStream(), 32 * 1024).use { input ->
                        BufferedOutputStream(FileOutputStream(tmpFile), 32 * 1024).use { output ->
                            val buffer = ByteArray(32 * 1024)
                            while (true) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                output.write(buffer, 0, read)
                                bytesReadTotal += read
                                val fraction = (bytesReadTotal.toFloat() / totalEstimate.toFloat())
                                    .coerceIn(0.02f, 0.86f)
                                if (fraction - lastReported >= 0.02f) {
                                    lastReported = fraction
                                    onProgress(fraction)
                                }
                            }
                            output.flush()
                        }
                    }
                }

                if (httpFailureMsg != null) {
                    lastError = IOException(httpFailureMsg)
                    continue
                }

                if (!tmpFile.exists() || tmpFile.length() < 128L) {
                    lastError = IOException("الملف المحمّل فارغ أو غير مكتمل")
                    continue
                }

                onProgress(0.90f)
                val parsedList = runCatching { parseHadithJsonFile(tmpFile, book) }
                    .getOrElse { parseErr ->
                        lastError = IOException("تعذّر تحليل بيانات ${book.arabicTitle}: ${parseErr.localizedMessage ?: "تنسيق غير صالح"}")
                        emptyList()
                    }
                if (parsedList.isEmpty()) {
                    runCatching { tmpFile.delete() }
                    if (lastError == null) {
                        lastError = IOException("لم يُعثر على أحاديث صالحة داخل ملف ${book.arabicTitle}")
                    }
                    continue
                }

                onProgress(0.96f)
                if (targetFile.exists()) targetFile.delete()
                if (!tmpFile.renameTo(targetFile)) {
                    tmpFile.copyTo(targetFile, overwrite = true)
                    tmpFile.delete()
                }

                memoryCache[book.id] = parsedList
                saveBookCount(context, book.id, parsedList.size)
                onProgress(1.0f)
                return@withContext Result.success(parsedList)
            } catch (e: Exception) {
                runCatching { tmpFile.delete() }
                lastError = e
            }
        }

        runCatching { tmpFile.delete() }
        Result.failure(
            lastError ?: IOException("تعذّر تحميل ${book.arabicTitle}، يرجى المحاولة مجدداً")
        )
    }

    /**
     * Loads a downloaded Hadith book strictly from internal storage (`hadith_{bookId}.json`).
     */
    suspend fun loadBookFromLocalStorage(
        context: Context,
        book: HadithBookCollection
    ): Result<List<LibraryHadith>> = withContext(Dispatchers.IO) {
        memoryCache[book.id]?.let { cached ->
            if (cached.isNotEmpty()) return@withContext Result.success(cached)
        }

        val localFile = getLocalBookFile(context, book)
        if (!localFile.exists() || localFile.length() <= 1_024L) {
            return@withContext Result.failure(IOException("الكتاب غير محمّل بعد"))
        }

        return@withContext try {
            val parsed = parseHadithJsonFile(localFile, book)
            if (parsed.isEmpty()) {
                Result.failure(IOException("الملف المحلي تالف، يرجى إعادة التحميل"))
            } else {
                memoryCache[book.id] = parsed
                saveBookCount(context, book.id, parsed.size)
                Result.success(parsed)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes the offline JSON file for a book to free internal storage.
     */
    suspend fun deleteBookOffline(
        context: Context,
        book: HadithBookCollection
    ): Boolean = withContext(Dispatchers.IO) {
        memoryCache.remove(book.id)
        removeBookCount(context, book.id)
        val file = getLocalBookFile(context, book)
        if (file.exists()) file.delete() else true
    }

    /**
     * Streaming JsonReader parser for fawazahmed0/hadith-api JSON files.
     * Tolerates both root objects (`{metadata, hadiths}`) and root arrays,
     * handles null/missing fields gracefully, and preserves duplicate hadith numbers
     * with unique UID suffixes.
     */
    private fun parseHadithJsonFile(file: File, book: HadithBookCollection): List<LibraryHadith> {
        val sectionsMap = HashMap<String, String>()
        val rawEntries = ArrayList<RawHadithEntry>(book.expectedCount.coerceAtLeast(64))

        InputStreamReader(BufferedInputStream(FileInputStream(file), 32 * 1024), Charsets.UTF_8).use { isr ->
            JsonReader(isr).use { reader ->
                reader.isLenient = true
                when (reader.peek()) {
                    JsonToken.BEGIN_OBJECT -> {
                        reader.beginObject()
                        while (reader.hasNext()) {
                            when (reader.nextName()) {
                                "metadata" -> readMetadataSections(reader, sectionsMap)
                                "hadiths", "data", "items" -> readHadithsArray(reader, rawEntries)
                                else -> reader.skipValue()
                            }
                        }
                        reader.endObject()
                    }
                    JsonToken.BEGIN_ARRAY -> {
                        readHadithsArray(reader, rawEntries)
                    }
                    else -> return emptyList()
                }
            }
        }

        val offlineEnrichment = HadithLibraryData.offlineForBook(book).associateBy { it.number }
        val seenUids = HashSet<String>(rawEntries.size)
        val result = ArrayList<LibraryHadith>(rawEntries.size)

        rawEntries.forEachIndexed { idx, raw ->
            val cleanedText = raw.text.trim()
            if (cleanedText.isEmpty()) return@forEachIndexed
            val num = if (raw.hadithNumber > 0) raw.hadithNumber else (idx + 1)
            var uid = "${book.id}_$num"
            if (!seenUids.add(uid)) {
                // Preserve multi-part or duplicate-numbered narrations with unique UID suffixes
                uid = "${book.id}_${num}_$idx"
                seenUids.add(uid)
            }

            val sectionName = raw.sectionBookKey?.let { sectionsMap[it] }.orEmpty().trim()
            val enrichment = offlineEnrichment[num]
            val title = enrichment?.title?.takeIf { it.isNotBlank() } ?: sectionName
            val narrator = enrichment?.narrator.orEmpty()
            val chapter = enrichment?.chapter ?: HadithChapters.classifyChapter(cleanedText, sectionName)
            val explanation = enrichment?.explanation.orEmpty()

            val searchBlob = normalizeArabicForSearch("$num $title $narrator $cleanedText")
            result.add(
                LibraryHadith(
                    uid = uid,
                    book = book,
                    number = num,
                    chapter = chapter,
                    title = title,
                    narrator = narrator,
                    text = cleanedText,
                    explanation = explanation,
                    source = "${book.arabicTitle} • حديث رقم $num",
                    normalizedSearchText = searchBlob
                )
            )
        }
        return result
    }

    private data class RawHadithEntry(
        val hadithNumber: Int,
        val text: String,
        val sectionBookKey: String?
    )

    private fun readMetadataSections(reader: JsonReader, sectionsMap: MutableMap<String, String>) {
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            reader.skipValue()
            return
        }
        reader.beginObject()
        while (reader.hasNext()) {
            val name = reader.nextName()
            if (name == "sections" && reader.peek() == JsonToken.BEGIN_OBJECT) {
                reader.beginObject()
                while (reader.hasNext()) {
                    val key = reader.nextName()
                    if (reader.peek() == JsonToken.STRING) {
                        val value = reader.nextString().trim()
                        if (value.isNotEmpty()) sectionsMap[key] = value
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endObject()
            } else {
                reader.skipValue()
            }
        }
        reader.endObject()
    }

    private fun readHadithsArray(reader: JsonReader, out: MutableList<RawHadithEntry>) {
        if (reader.peek() != JsonToken.BEGIN_ARRAY) {
            reader.skipValue()
            return
        }
        reader.beginArray()
        while (reader.hasNext()) {
            if (reader.peek() != JsonToken.BEGIN_OBJECT) {
                reader.skipValue()
                continue
            }
            var hadithNum = 0
            var arabicNum = 0
            var text = ""
            var refBook: String? = null

            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "hadithnumber", "number", "id" -> {
                        val v = readLenientInt(reader)
                        if (hadithNum == 0 && v > 0) hadithNum = v
                    }
                    "arabicnumber" -> arabicNum = readLenientInt(reader)
                    "text", "arab", "arabic" -> {
                        val candidate = if (reader.peek() == JsonToken.STRING) reader.nextString() else {
                            reader.skipValue(); ""
                        }
                        if (text.isBlank() && candidate.isNotBlank()) text = candidate
                    }
                    "reference" -> {
                        if (reader.peek() == JsonToken.BEGIN_OBJECT) {
                            reader.beginObject()
                            while (reader.hasNext()) {
                                when (reader.nextName()) {
                                    "book" -> {
                                        refBook = when (reader.peek()) {
                                            JsonToken.NUMBER -> runCatching { reader.nextDouble().toInt().toString() }.getOrNull()
                                            JsonToken.STRING -> reader.nextString()
                                            else -> { reader.skipValue(); null }
                                        }
                                    }
                                    else -> reader.skipValue()
                                }
                            }
                            reader.endObject()
                        } else {
                            reader.skipValue()
                        }
                    }
                    else -> reader.skipValue()
                }
            }
            reader.endObject()

            val effectiveNumber = if (hadithNum > 0) hadithNum else arabicNum
            if (text.isNotBlank()) {
                out.add(RawHadithEntry(effectiveNumber, text, refBook))
            }
        }
        reader.endArray()
    }

    private fun readLenientInt(reader: JsonReader): Int {
        return when (reader.peek()) {
            JsonToken.NUMBER -> runCatching { reader.nextDouble().toInt() }.getOrDefault(0)
            JsonToken.STRING -> runCatching {
                val raw = reader.nextString().trim()
                raw.toDoubleOrNull()?.toInt()
                    ?: Regex("\\d+").find(raw)?.value?.toIntOrNull()
                    ?: 0
            }.getOrDefault(0)
            else -> {
                reader.skipValue()
                0
            }
        }
    }

    // ---- Lightweight persisted counts index ----

    private fun readSavedCounts(context: Context): Map<String, Int> {
        val f = getMetaCountsFile(context)
        if (!f.exists()) return emptyMap()
        return runCatching {
            val obj = JSONObject(f.readText(Charsets.UTF_8))
            val map = HashMap<String, Int>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.optInt(k, 0)
            }
            map
        }.getOrDefault(emptyMap())
    }

    private fun saveBookCount(context: Context, bookId: String, count: Int) {
        runCatching {
            val f = getMetaCountsFile(context)
            val obj = if (f.exists()) {
                runCatching { JSONObject(f.readText(Charsets.UTF_8)) }.getOrDefault(JSONObject())
            } else {
                JSONObject()
            }
            obj.put(bookId, count)
            f.writeText(obj.toString(), Charsets.UTF_8)
        }
    }

    private fun removeBookCount(context: Context, bookId: String) {
        runCatching {
            val f = getMetaCountsFile(context)
            if (!f.exists()) return
            val obj = JSONObject(f.readText(Charsets.UTF_8))
            obj.remove(bookId)
            f.writeText(obj.toString(), Charsets.UTF_8)
        }
    }

    // ---- Offline Favorites Snapshot Persistence (always instant on Favorites tab) ----

    suspend fun loadSavedFavorites(context: Context): List<LibraryHadith> = withContext(Dispatchers.IO) {
        val f = getFavoritesFile(context)
        if (!f.exists()) return@withContext emptyList()
        runCatching {
            val arr = JSONArray(f.readText(Charsets.UTF_8))
            val list = ArrayList<LibraryHadith>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val book = HadithBookCollection.fromId(obj.optString("bookId")) ?: continue
                val num = obj.optInt("number", 0)
                val text = obj.optString("text", "")
                if (text.isBlank()) continue
                val title = obj.optString("title", "")
                val narrator = obj.optString("narrator", "")
                list.add(
                    LibraryHadith(
                        uid = obj.optString("uid", "${book.id}_$num"),
                        book = book,
                        number = num,
                        chapter = obj.optString("chapter", HadithChapters.GENERAL),
                        title = title,
                        narrator = narrator,
                        text = text,
                        explanation = obj.optString("explanation", ""),
                        source = obj.optString("source", "${book.arabicTitle} • حديث رقم $num"),
                        normalizedSearchText = normalizeArabicForSearch("$num ${book.arabicTitle} $title $narrator $text")
                    )
                )
            }
            list
        }.getOrDefault(emptyList())
    }

    suspend fun saveFavorites(context: Context, favorites: List<LibraryHadith>) = withContext(Dispatchers.IO) {
        runCatching {
            val arr = JSONArray()
            favorites.forEach { h ->
                arr.put(
                    JSONObject().apply {
                        put("uid", h.uid)
                        put("bookId", h.book.id)
                        put("number", h.number)
                        put("chapter", h.chapter)
                        put("title", h.title)
                        put("narrator", h.narrator)
                        put("text", h.text)
                        put("explanation", h.explanation)
                        put("source", h.source)
                    }
                )
            }
            val target = getFavoritesFile(context)
            val tmp = File(target.parentFile, "$FAVORITES_SNAPSHOT_FILE.tmp")
            tmp.writeText(arr.toString(), Charsets.UTF_8)
            if (target.exists()) target.delete()
            if (!tmp.renameTo(target)) {
                target.writeText(arr.toString(), Charsets.UTF_8)
                tmp.delete()
            }
        }
    }

    fun formatFileSizeArabic(bytes: Long): String {
        if (bytes <= 0L) return ""
        val kb = bytes / 1024.0
        return if (kb < 1024.0) {
            String.format(java.util.Locale.US, "%.0f KB", kb)
        } else {
            String.format(java.util.Locale.US, "%.1f MB", kb / 1024.0)
        }
    }
}
