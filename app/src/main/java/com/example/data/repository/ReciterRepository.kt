package com.example.data.repository

import android.content.Context
import com.example.data.model.MoshafOption
import com.example.data.model.ReciterOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

object ReciterRepository {

    val DEFAULT_RECITERS_REPO_URL: String
        get() = com.example.security.SecureConfigVault.recitersEndpoint

    private const val CACHE_FILE_NAME = "reciters_cache.json"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    fun buildSurahMp3Url(server: String, surahNumber: Int): String {
        val safeSurah = surahNumber.coerceIn(1, 114)
        val normalizedServer = if (server.endsWith("/")) server else "$server/"
        return normalizedServer + String.format(Locale.US, "%03d.mp3", safeSurah)
    }

    fun getCacheFile(context: Context): File = File(context.filesDir, CACHE_FILE_NAME)

    fun hasLocalCache(context: Context): Boolean {
        val f = getCacheFile(context)
        return f.exists() && f.length() > 10L
    }

    suspend fun loadCachedReciters(context: Context): List<ReciterOption> = withContext(Dispatchers.IO) {
        val f = getCacheFile(context)
        if (!f.exists() || f.length() <= 10L) return@withContext emptyList()
        runCatching {
            val raw = f.readText(Charsets.UTF_8)
            parseRecitersJson(raw)
        }.getOrDefault(emptyList())
    }

    suspend fun syncRecitersFromRepo(
        context: Context,
        repoUrl: String = DEFAULT_RECITERS_REPO_URL
    ): Result<List<ReciterOption>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(repoUrl)
                .header("Accept", "application/json")
                .build()

            val body = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}")
                }
                response.body?.string() ?: throw IOException("Empty reciters payload")
            }

            val parsed = parseRecitersJson(body)
            if (parsed.isEmpty()) {
                throw IOException("لم يتم العثور على قراء في القائمة")
            }

            // Atomically persist to filesDir/reciters_cache.json for instant offline loading
            val cacheFile = getCacheFile(context)
            val tmpFile = File(context.filesDir, "$CACHE_FILE_NAME.tmp")
            tmpFile.writeText(body, Charsets.UTF_8)
            if (cacheFile.exists()) cacheFile.delete()
            if (!tmpFile.renameTo(cacheFile)) {
                cacheFile.writeText(body, Charsets.UTF_8)
                tmpFile.delete()
            }

            Result.success(parsed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseRecitersJson(raw: String): List<ReciterOption> {
        val trimmed = raw.trim()
        val array: JSONArray = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            trimmed.startsWith("{") -> {
                val obj = JSONObject(trimmed)
                obj.optJSONArray("reciters")
                    ?: obj.optJSONArray("data")
                    ?: JSONArray()
            }
            else -> JSONArray()
        }

        val list = ArrayList<ReciterOption>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val id = obj.optString("id", "").trim().ifBlank { "repo_reciter_$i" }
            val name = obj.optString("name", "").trim()
            val riwaya = obj.optString("riwaya", obj.optString("riwayah", "حفص عن عاصم")).trim()
                .ifBlank { "حفص عن عاصم" }
            val serverRaw = obj.optString("server", "").trim()
            if (name.isBlank() || serverRaw.isBlank()) continue
            val normalizedServer = if (serverRaw.endsWith("/")) serverRaw else "$serverRaw/"

            list.add(
                ReciterOption(
                    id = id,
                    arabicName = name,
                    riwayahName = riwaya,
                    serverUrl = normalizedServer,
                    moshafList = listOf(
                        MoshafOption(
                            id = i + 1,
                            name = riwaya,
                            server = normalizedServer,
                            surahTotal = 114
                        )
                    ),
                    isFeatured = false
                )
            )
        }
        return list
    }
}
