package com.example.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

data class AppReleaseInfo(
    val versionName: String,
    val tagName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val assetSizeBytes: Long
)

data class DownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long,
    val progressPercent: Int
)

/**
 * Lightweight In-App Direct Auto-Update Manager.
 * Checks GitHub Releases API, compares semantic version names, downloads the APK
 * directly inside app-private Downloads storage, and launches the Android Package
 * Installer via FileProvider without ever opening a browser.
 */
object AppUpdateManager {

    private val latestReleaseApiUrl: String
        get() = com.example.security.SecureConfigVault.latestReleaseApiEndpoint

    private val fallbackApkDirectUrl: String
        get() = com.example.security.SecureConfigVault.fallbackApkDownloadEndpoint

    private const val UPDATE_APK_FILE_NAME = "rafiq-al-muslim-update.apk"
    private const val UPDATE_TMP_FILE_NAME = "rafiq-al-muslim-update.apk.tmp"

    private const val PREFS_NAME = "app_update_prefs"
    private const val KEY_LAST_CHECK_MS = "last_update_check_ms"
    private const val CHECK_INTERVAL_MS = 12L * 60L * 60L * 1000L // 12 hours

    private val apiHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private val downloadHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }

    fun getUpdateDirectory(context: Context): File {
        val extDir = context.applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        return (extDir ?: context.applicationContext.filesDir).apply {
            if (!exists()) mkdirs()
        }
    }

    fun getUpdateApkFile(context: Context): File =
        File(getUpdateDirectory(context), UPDATE_APK_FILE_NAME)

    private fun getUpdateTmpFile(context: Context): File =
        File(getUpdateDirectory(context), UPDATE_TMP_FILE_NAME)

    /**
     * Removes any leftover APK or temporary download file from previous sessions to save storage.
     */
    suspend fun cleanupLeftoverUpdateApk(context: Context) = withContext(Dispatchers.IO) {
        val appCtx = context.applicationContext
        val dirs = listOfNotNull(
            appCtx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            appCtx.filesDir
        ).distinctBy { it.absolutePath }

        dirs.forEach { dir ->
            runCatching {
                File(dir, UPDATE_APK_FILE_NAME).takeIf { it.exists() }?.delete()
                File(dir, UPDATE_TMP_FILE_NAME).takeIf { it.exists() }?.delete()
            }
        }
    }

    fun shouldRunSilentCheck(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastCheck = prefs.getLong(KEY_LAST_CHECK_MS, 0L)
        return (System.currentTimeMillis() - lastCheck) >= CHECK_INTERVAL_MS
    }

    fun recordCheckTimestamp(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_CHECK_MS, System.currentTimeMillis()).apply()
    }

    /**
     * Fetches the latest release metadata and returns [AppReleaseInfo] ONLY if a newer version
     * than [currentVersionName] is available; returns `Result.success(null)` if already up-to-date.
     */
    suspend fun checkForNewerRelease(
        context: Context,
        currentVersionName: String = BuildConfig.VERSION_NAME
    ): Result<AppReleaseInfo?> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(latestReleaseApiUrl)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "RafiqAlMuslim-Android/${currentVersionName}")
                .build()

            val payload = apiHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}")
                }
                response.body?.string()?.trim()
                    ?: throw IOException("Empty release payload")
            }

            recordCheckTimestamp(context)

            val json = JSONObject(payload)
            if (json.optBoolean("draft", false)) {
                return@withContext Result.success(null)
            }

            val rawTag = json.optString("tag_name", "").trim()
            if (rawTag.isBlank()) {
                return@withContext Result.success(null)
            }

            val cleanVersion = normalizeVersionTag(rawTag)
            if (!isRemoteVersionNewer(remoteVersion = cleanVersion, currentVersion = currentVersionName)) {
                return@withContext Result.success(null)
            }

            val notes = json.optString("body", "").trim()
                .ifBlank { "يتضمن هذا التحديث تحسينات في الأداء وإصلاحات عامة." }

            var resolvedApkUrl = fallbackApkDirectUrl
            var resolvedSize = -1L

            val assets = json.optJSONArray("assets")
            if (assets != null && assets.length() > 0) {
                var firstAssetUrl: String? = null
                var firstAssetSize = -1L
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val assetName = asset.optString("name", "").trim()
                    val browserUrl = asset.optString("browser_download_url", "").trim()
                    val assetSize = asset.optLong("size", -1L)
                    if (firstAssetUrl == null && browserUrl.startsWith("https://")) {
                        firstAssetUrl = browserUrl
                        firstAssetSize = assetSize
                    }
                    if (assetName.endsWith(".apk", ignoreCase = true) && browserUrl.startsWith("https://")) {
                        resolvedApkUrl = browserUrl
                        resolvedSize = assetSize
                        break
                    }
                }
                if (resolvedApkUrl == fallbackApkDirectUrl && !firstAssetUrl.isNullOrBlank()) {
                    resolvedApkUrl = firstAssetUrl
                    resolvedSize = firstAssetSize
                }
            }

            Result.success(
                AppReleaseInfo(
                    versionName = cleanVersion,
                    tagName = rawTag,
                    releaseNotes = notes,
                    downloadUrl = resolvedApkUrl,
                    assetSizeBytes = resolvedSize
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads the APK directly to `getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)/rafiq-al-muslim-update.apk`.
     * Reports throttled progress updates via [onProgress] and cleans up temporary files on failure/cancel.
     */
    suspend fun downloadUpdateApk(
        context: Context,
        downloadUrl: String,
        expectedSizeBytes: Long = -1L,
        onProgress: (DownloadProgress) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val targetFile = getUpdateApkFile(context)
        val tmpFile = getUpdateTmpFile(context)

        try {
            val safeUrl = downloadUrl.trim().takeIf { it.startsWith("https://") }
                ?: fallbackApkDirectUrl

            if (tmpFile.exists()) tmpFile.delete()
            if (targetFile.exists()) targetFile.delete()

            val request = Request.Builder()
                .url(safeUrl)
                .header("Accept", "application/vnd.android.package-archive, application/octet-stream, */*")
                .header("User-Agent", "RafiqAlMuslim-Android/${BuildConfig.VERSION_NAME}")
                .build()

            val call = downloadHttpClient.newCall(request)
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}")
                }
                val body = response.body ?: throw IOException("ملف التحديث فارغ")
                val headerLength = body.contentLength()
                val totalBytes = when {
                    headerLength > 0L -> headerLength
                    expectedSizeBytes > 0L -> expectedSizeBytes
                    else -> -1L
                }

                var downloadedBytes = 0L
                var lastReportTimeMs = 0L
                var lastReportedPercent = -1
                val buffer = ByteArray(16 * 1024)

                onProgress(DownloadProgress(0L, totalBytes, 0))

                body.byteStream().use { input ->
                    FileOutputStream(tmpFile).use { output ->
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloadedBytes += read

                            val now = System.currentTimeMillis()
                            val percent = if (totalBytes > 0L) {
                                ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)
                            } else {
                                -1
                            }

                            if (now - lastReportTimeMs >= 120L || (percent >= 0 && percent != lastReportedPercent)) {
                                lastReportTimeMs = now
                                lastReportedPercent = percent
                                onProgress(
                                    DownloadProgress(
                                        downloadedBytes = downloadedBytes,
                                        totalBytes = totalBytes,
                                        progressPercent = percent.coerceAtLeast(0)
                                    )
                                )
                            }
                        }
                        output.flush()
                    }
                }

                if (!tmpFile.exists() || tmpFile.length() < 1024L || !hasApkZipSignature(tmpFile)) {
                    tmpFile.delete()
                    throw IOException("ملف التحديث غير مكتمل أو غير صالح")
                }

                if (!tmpFile.renameTo(targetFile)) {
                    tmpFile.copyTo(targetFile, overwrite = true)
                    tmpFile.delete()
                }

                val finalSize = targetFile.length()
                onProgress(DownloadProgress(finalSize, finalSize, 100))
                Result.success(targetFile)
            }
        } catch (e: CancellationException) {
            runCatching { tmpFile.delete() }
            throw e
        } catch (e: Exception) {
            runCatching { tmpFile.delete() }
            Result.failure(e)
        }
    }

    /**
     * Checks whether the app is allowed to request package installs on Android 8+ (API 26+).
     */
    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)
        } else {
            true
        }
    }

    /**
     * Builds the system Settings intent for allowing unknown app installs for this package on Android 8+.
     */
    fun buildUnknownSourcesSettingsIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
    }

    /**
     * Triggers the native Android Package Installer directly using FileProvider (never opens a browser).
     */
    fun launchApkInstaller(context: Context, apkFile: File): Result<Unit> {
        return runCatching {
            require(apkFile.exists() && apkFile.length() > 0L) {
                "ملف التحديث غير موجود"
            }
            val authority = "${BuildConfig.APPLICATION_ID}.fileprovider"
            val apkUri = FileProvider.getUriForFile(context, authority, apkFile)
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        }
    }

    fun normalizeVersionTag(rawTag: String): String {
        val trimmed = rawTag.trim()
        return if (trimmed.startsWith("v", ignoreCase = true)) {
            trimmed.substring(1).trim()
        } else {
            trimmed
        }
    }

    fun isRemoteVersionNewer(remoteVersion: String, currentVersion: String): Boolean {
        val remoteParts = parseNumericVersionParts(remoteVersion)
        val currentParts = parseNumericVersionParts(currentVersion)
        if (remoteParts.isEmpty() || currentParts.isEmpty()) return false

        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    fun formatMegabytes(bytes: Long): String {
        if (bytes <= 0L) return "0.0"
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return String.format(Locale.US, "%.1f", mb)
    }

    private fun parseNumericVersionParts(version: String): List<Int> {
        val cleaned = normalizeVersionTag(version)
            .substringBefore('-')
            .substringBefore('+')
            .trim()
        if (cleaned.isBlank()) return emptyList()
        return cleaned.split('.').mapNotNull { segment ->
            val digits = segment.takeWhile { it.isDigit() }
            digits.toIntOrNull()
        }
    }

    private fun hasApkZipSignature(file: File): Boolean {
        return runCatching {
            file.inputStream().use { input ->
                val header = ByteArray(2)
                input.read(header) == 2 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()
            }
        }.getOrDefault(false)
    }
}
