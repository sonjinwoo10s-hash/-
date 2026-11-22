package com.example.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Centralized persistent offline storage rooted strictly inside `context.applicationContext.filesDir`
 * (NEVER `cacheDir` or `externalCacheDir`), organized cleanly into dedicated subfolders:
 *  - `filesDir/offline_quran/`  (Mushaf JSON, timing JSONs, downloaded Surah MP3 recitations)
 *  - `filesDir/offline_tafsir/` (Downloaded offline Tafsir books)
 *  - `filesDir/offline_hadith/` (Downloaded Hadith collections, counts & favorites snapshots)
 *  - `filesDir/offline_adhan/`  (Downloaded Muezzin Adhan MP3 files, catalog & custom audio)
 *
 * Because all user downloads reside inside `filesDir`, clearing the app cache
 * ("مسح ذاكرة التخزين المؤقت") from Android Settings or cleaner apps NEVER deletes them.
 */
object PersistentOfflineStorage {

    private const val DIR_QURAN = "offline_quran"
    private const val DIR_QURAN_AUDIO = "audio"
    private const val DIR_QURAN_TIMINGS = "timings"
    private const val DIR_TAFSIR = "offline_tafsir"
    private const val DIR_HADITH = "offline_hadith"
    private const val DIR_ADHAN = "offline_adhan"
    private const val DIR_ADHAN_CUSTOM = "custom"

    private val downloadClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }

    fun quranDir(context: Context): File =
        File(context.applicationContext.filesDir, DIR_QURAN).ensureDir()

    fun quranAudioDir(context: Context): File =
        File(quranDir(context), DIR_QURAN_AUDIO).ensureDir()

    fun quranTimingsDir(context: Context): File =
        File(quranDir(context), DIR_QURAN_TIMINGS).ensureDir()

    fun tafsirDir(context: Context): File =
        File(context.applicationContext.filesDir, DIR_TAFSIR).ensureDir()

    fun hadithDir(context: Context): File =
        File(context.applicationContext.filesDir, DIR_HADITH).ensureDir()

    fun adhanDir(context: Context): File =
        File(context.applicationContext.filesDir, DIR_ADHAN).ensureDir()

    fun adhanCustomDir(context: Context): File =
        File(adhanDir(context), DIR_ADHAN_CUSTOM).ensureDir()

    /**
     * Transparently moves a legacy file from an older path into its organized `filesDir/offline_*`
     * destination on first access so existing users keep all previously downloaded offline data.
     */
    fun migrateFileIfNeeded(legacyFile: File, targetFile: File): File {
        if (!targetFile.exists() && legacyFile.exists() && legacyFile.isFile && legacyFile.length() > 0L) {
            runCatching {
                targetFile.parentFile?.mkdirs()
                if (!legacyFile.renameTo(targetFile)) {
                    legacyFile.copyTo(targetFile, overwrite = true)
                    legacyFile.delete()
                }
            }
        }
        return targetFile
    }

    fun surahAudioKey(reciterId: String, surahNumber: Int): String {
        val safeReciter = sanitizeSegment(reciterId)
        val padded = surahNumber.coerceIn(1, 114).toString().padStart(3, '0')
        return "${safeReciter}_$padded"
    }

    /**
     * Persistent offline Surah MP3 path: `filesDir/offline_quran/audio/{safeReciterId}_{001}.mp3`.
     */
    fun getOfflineSurahAudioFile(context: Context, reciterId: String, surahNumber: Int): File {
        return File(quranAudioDir(context), "${surahAudioKey(reciterId, surahNumber)}.mp3")
    }

    fun isSurahAudioDownloaded(context: Context, reciterId: String, surahNumber: Int): Boolean {
        val f = getOfflineSurahAudioFile(context, reciterId, surahNumber)
        return f.exists() && f.length() > 1_024L
    }

    /**
     * Dynamically scans `filesDir/offline_quran/audio/` for all verified non-empty downloaded MP3s
     * and returns their normalized `{safeReciterId}_{001}` keys.
     */
    fun getDownloadedSurahKeys(context: Context): Set<String> {
        val dir = quranAudioDir(context)
        val files = dir.listFiles() ?: return emptySet()
        return files
            .asSequence()
            .filter { it.isFile && it.name.endsWith(".mp3", ignoreCase = true) && it.length() > 1_024L }
            .map { it.name.removeSuffix(".mp3") }
            .toSet()
    }

    /**
     * Streams a Surah MP3 directly to `filesDir/offline_quran/audio/` using a fixed 8KB buffer
     * (never loading the MP3 into RAM) with atomic `.tmp` swap.
     */
    suspend fun downloadSurahAudioOffline(
        context: Context,
        reciterId: String,
        surahNumber: Int,
        streamUrl: String
    ): Result<File> = withContext(Dispatchers.IO) {
        val target = getOfflineSurahAudioFile(context, reciterId, surahNumber)
        val tmp = File(target.parentFile, "${target.name}.tmp")
        try {
            val request = Request.Builder()
                .url(streamUrl)
                .header("Accept", "audio/mpeg, audio/*;q=0.9, */*;q=0.8")
                .build()

            downloadClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP ${response.code}"))
                }
                val body = response.body ?: return@withContext Result.failure(IOException("استجابة فارغة"))
                var totalBytes = 0L
                BufferedInputStream(body.byteStream(), 8 * 1024).use { input ->
                    BufferedOutputStream(FileOutputStream(tmp), 8 * 1024).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            totalBytes += read
                        }
                        output.flush()
                    }
                }
                if (totalBytes <= 1_024L) {
                    tmp.delete()
                    return@withContext Result.failure(IOException("الملف الصوتي غير مكتمل"))
                }
            }

            if (target.exists()) target.delete()
            if (!tmp.renameTo(target)) {
                tmp.copyTo(target, overwrite = true)
                tmp.delete()
            }
            Result.success(target)
        } catch (e: Exception) {
            runCatching { tmp.delete() }
            Result.failure(e)
        }
    }

    private fun File.ensureDir(): File {
        if (!exists()) mkdirs()
        return this
    }

    private fun sanitizeSegment(raw: String): String {
        val cleaned = raw.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return cleaned.ifBlank { "default" }
    }
}
