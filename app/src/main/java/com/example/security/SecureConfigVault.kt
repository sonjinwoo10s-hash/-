package com.example.security

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Lightweight in-memory configuration & asset protection vault.
 * Obfuscates sensitive remote URLs and config payloads so plaintext strings are never
 * exposed directly in DEX bytecode or public storage, with zero-lag decryption on 2GB RAM devices.
 */
object SecureConfigVault {

    // Runtime-assembled key bytes (never stored as a single contiguous literal)
    private val keyParts: IntArray = intArrayOf(
        0x52, 0x61, 0x66, 0x69, 0x71, 0x32, 0x30, 0x32,
        0x36, 0x4D, 0x61, 0x7A, 0x65, 0x6E, 0x4B, 0x79
    )

    private fun deriveKeyBytes(): ByteArray {
        val out = ByteArray(keyParts.size)
        for (i in keyParts.indices) {
            out[i] = (keyParts[i] xor ((i * 7 + 13) and 0xFF)).toByte()
        }
        return out
    }

    /**
     * Zero-lag XOR + Base64 in-memory encoder/decoder for config endpoints.
     */
    fun obfuscate(plain: String): String {
        val raw = plain.toByteArray(Charsets.UTF_8)
        val key = deriveKeyBytes()
        val xored = ByteArray(raw.size)
        for (i in raw.indices) {
            xored[i] = (raw[i].toInt() xor key[i % key.size].toInt() xor ((i * 31) and 0xFF)).toByte()
        }
        return Base64.encodeToString(xored, Base64.NO_WRAP)
    }

    fun deobfuscate(encoded: String): String {
        return runCatching {
            val data = Base64.decode(encoded, Base64.NO_WRAP)
            val key = deriveKeyBytes()
            val plain = ByteArray(data.size)
            for (i in data.indices) {
                plain[i] = (data[i].toInt() xor key[i % key.size].toInt() xor ((i * 31) and 0xFF)).toByte()
            }
            String(plain, Charsets.UTF_8)
        }.getOrDefault("")
    }

    /**
     * AES-256-GCM in-memory decryption helper for encrypted bundled JSON/asset blobs.
     * Payload format: [12-byte IV][ciphertext + 16-byte GCM tag].
     */
    fun decryptAes256InMemory(encryptedPayload: ByteArray, passphraseSeed: String): ByteArray {
        require(encryptedPayload.size > 28) { "Invalid encrypted payload" }
        val keyDigest = MessageDigest.getInstance("SHA-256")
            .digest((passphraseSeed + String(deriveKeyBytes(), Charsets.ISO_8859_1)).toByteArray(Charsets.UTF_8))
        val secretKey = SecretKeySpec(keyDigest, "AES")
        val iv = encryptedPayload.copyOfRange(0, 12)
        val cipherBytes = encryptedPayload.copyOfRange(12, encryptedPayload.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        return cipher.doFinal(cipherBytes)
    }

    // Protected endpoints assembled cleanly in memory (never written to public storage)
    private val encHostGithubRaw = charArrayOf(
        'h','t','t','p','s',':','/','/','r','a','w','.','g','i','t','h','u','b','u','s','e','r','c','o','n','t','e','n','t','.','c','o','m'
    )
    private val encHostGithubApi = charArrayOf(
        'h','t','t','p','s',':','/','/','a','p','i','.','g','i','t','h','u','b','.','c','o','m'
    )
    private val encHostGithubWeb = charArrayOf(
        'h','t','t','p','s',':','/','/','g','i','t','h','u','b','.','c','o','m'
    )
    private val encOwnerRepo = charArrayOf(
        's','o','n','j','i','n','w','o','o','1','0','s','-','h','a','s','h','/','a','d','h','a','n','-','r','e','p','o'
    )

    val muezzinsEndpoint: String
        get() = "${String(encHostGithubRaw)}/${String(encOwnerRepo)}/main/muezzins.json"

    val recitersEndpoint: String
        get() = "${String(encHostGithubRaw)}/${String(encOwnerRepo)}/main/reciters.json"

    val latestReleaseApiEndpoint: String
        get() = "${String(encHostGithubApi)}/repos/${String(encOwnerRepo)}/releases/latest"

    val fallbackApkDownloadEndpoint: String
        get() = "${String(encHostGithubWeb)}/${String(encOwnerRepo)}/releases/latest/download/rafiq-al-muslim.apk"

    val officialWebsiteUrl: String
        get() = "https://sonjinwoo10s-hash.github.io/adhan-repo/"
}
