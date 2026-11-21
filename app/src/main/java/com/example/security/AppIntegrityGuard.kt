package com.example.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.example.BuildConfig
import java.security.MessageDigest

/**
 * Lightweight runtime APK anti-tamper & signature verification guard.
 * Enabled strictly in RELEASE builds inside [com.example.RafiqApplication.onCreate].
 *
 * HOW TO ENABLE RELEASE SIGNATURE LOCK:
 * 1. Run `keytool -list -v -keystore my-upload-key.jks -alias upload`
 * 2. Copy the SHA-256 hex digest (uppercase, without colons or with colons — both supported)
 * 3. Paste it into [EXPECTED_SIGNATURE_SHA256] below before publishing your release APK.
 */
object AppIntegrityGuard {

    private const val EXPECTED_PACKAGE_NAME = "com.aistudio.rafiqalmuslim.kzmpxr"

    /**
     * Developer's official release keystore SHA-256 fingerprint.
     * Replace this placeholder with your real 64-char SHA-256 hex digest before signing release APKs.
     */
    const val EXPECTED_SIGNATURE_SHA256 = "REPLACE_WITH_YOUR_RELEASE_KEYSTORE_SHA256_HEX"

    const val TAMPER_BLOCK_MESSAGE_AR =
        "تم اكتشاف تعديل غير مصرح به في نسخة التطبيق. يرجى تحميل النسخة الرسمية من الموقع."

    @Volatile
    var isTampered: Boolean = false
        private set

    fun verifyReleaseIntegrity(context: Context): Boolean {
        // Never block debug / development builds
        if (BuildConfig.DEBUG) {
            isTampered = false
            return true
        }

        // 1. Verify official package name (prevents clone/repackage under a different package ID)
        if (context.packageName != EXPECTED_PACKAGE_NAME) {
            isTampered = true
            return false
        }

        // 2. Verify signing certificate SHA-256 hash when developer has configured EXPECTED_SIGNATURE_SHA256
        val expectedNormalized = EXPECTED_SIGNATURE_SHA256
            .replace(":", "")
            .replace(" ", "")
            .uppercase()

        if (expectedNormalized.length == 64 && expectedNormalized.all { it in '0'..'9' || it in 'A'..'F' }) {
            val actualDigests = readSigningCertSha256List(context)
            if (actualDigests.isEmpty() || !actualDigests.contains(expectedNormalized)) {
                isTampered = true
                return false
            }
        }

        isTampered = false
        return true
    }

    @Suppress("DEPRECATION")
    private fun readSigningCertSha256List(context: Context): List<String> {
        return runCatching {
            val pm = context.packageManager
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val pkgInfo = pm.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                val signingInfo = pkgInfo.signingInfo
                if (signingInfo != null) {
                    if (signingInfo.hasMultipleSigners()) {
                        signingInfo.apkContentsSigners
                    } else {
                        signingInfo.signingCertificateHistory
                    }
                } else {
                    emptyArray()
                }
            } else {
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                    .signatures
                    .orEmpty()
            }

            val md = MessageDigest.getInstance("SHA-256")
            signatures.map { sig ->
                val hash = md.digest(sig.toByteArray())
                buildString(hash.size * 2) {
                    for (b in hash) {
                        append(String.format("%02X", b))
                    }
                }
            }
        }.getOrDefault(emptyList())
    }
}
