package com.example.data.local

import android.content.SharedPreferences
import androidx.datastore.preferences.core.Preferences
import org.json.JSONArray
import java.util.Locale

/**
 * Type-safe, backward-compatible readers for [SharedPreferences] and Jetpack DataStore [Preferences].
 *
 * Prevents startup `ClassCastException` crashes when upgrading over older versions where a key was
 * previously stored under a different primitive type (e.g., `Int` instead of `String`, `Float`
 * instead of `String`, `String` instead of `Int`/`Boolean`/`Set<String>`).
 *
 * On type mismatch in [SharedPreferences]:
 * 1. Inspects the raw legacy value in `all[key]`.
 * 2. If safely convertible to the new expected type, migrates ONLY that single key in-place and
 *    returns the migrated value (preserving the user's data).
 * 3. If unconvertible, clears ONLY that single corrupted key and returns [default] without
 *    touching any other keys.
 */
object SafePrefs {

    fun getString(prefs: SharedPreferences, key: String, default: String? = null): String? {
        return try {
            prefs.getString(key, default)
        } catch (_: ClassCastException) {
            migrateAndReadString(prefs, key, default)
        } catch (_: Exception) {
            default
        }
    }

    fun getInt(prefs: SharedPreferences, key: String, default: Int): Int {
        return try {
            prefs.getInt(key, default)
        } catch (_: ClassCastException) {
            migrateAndReadInt(prefs, key, default)
        } catch (_: Exception) {
            default
        }
    }

    fun getLong(prefs: SharedPreferences, key: String, default: Long): Long {
        return try {
            prefs.getLong(key, default)
        } catch (_: ClassCastException) {
            migrateAndReadLong(prefs, key, default)
        } catch (_: Exception) {
            default
        }
    }

    fun getFloat(prefs: SharedPreferences, key: String, default: Float): Float {
        return try {
            prefs.getFloat(key, default)
        } catch (_: ClassCastException) {
            migrateAndReadFloat(prefs, key, default)
        } catch (_: Exception) {
            default
        }
    }

    fun getBoolean(prefs: SharedPreferences, key: String, default: Boolean): Boolean {
        return try {
            prefs.getBoolean(key, default)
        } catch (_: ClassCastException) {
            migrateAndReadBoolean(prefs, key, default)
        } catch (_: Exception) {
            default
        }
    }

    fun getStringSet(
        prefs: SharedPreferences,
        key: String,
        default: Set<String> = emptySet()
    ): Set<String> {
        return try {
            prefs.getStringSet(key, default)?.toSet() ?: default
        } catch (_: ClassCastException) {
            migrateAndReadStringSet(prefs, key, default)
        } catch (_: Exception) {
            default
        }
    }

    private fun readRawValue(prefs: SharedPreferences, key: String): Any? {
        return runCatching { prefs.all[key] }.getOrNull()
    }

    private fun migrateAndReadString(prefs: SharedPreferences, key: String, default: String?): String? {
        val raw = readRawValue(prefs, key)
        val converted: String? = when (raw) {
            is String -> raw
            is Number -> raw.toString()
            is Boolean -> raw.toString()
            is Set<*> -> raw.mapNotNull { it?.toString()?.trim() }
                .filter { it.isNotEmpty() }
                .joinToString(",")
            else -> null
        }
        return if (converted != null) {
            runCatching { prefs.edit().putString(key, converted).apply() }
            converted
        } else {
            runCatching { prefs.edit().remove(key).apply() }
            default
        }
    }

    private fun migrateAndReadInt(prefs: SharedPreferences, key: String, default: Int): Int {
        val raw = readRawValue(prefs, key)
        val converted: Int? = when (raw) {
            is Int -> raw
            is Number -> raw.toInt()
            is String -> raw.trim().toIntOrNull() ?: raw.trim().toDoubleOrNull()?.toInt()
            is Boolean -> if (raw) 1 else 0
            else -> null
        }
        return if (converted != null) {
            runCatching { prefs.edit().putInt(key, converted).apply() }
            converted
        } else {
            runCatching { prefs.edit().remove(key).apply() }
            default
        }
    }

    private fun migrateAndReadLong(prefs: SharedPreferences, key: String, default: Long): Long {
        val raw = readRawValue(prefs, key)
        val converted: Long? = when (raw) {
            is Long -> raw
            is Number -> raw.toLong()
            is String -> raw.trim().toLongOrNull() ?: raw.trim().toDoubleOrNull()?.toLong()
            else -> null
        }
        return if (converted != null) {
            runCatching { prefs.edit().putLong(key, converted).apply() }
            converted
        } else {
            runCatching { prefs.edit().remove(key).apply() }
            default
        }
    }

    private fun migrateAndReadFloat(prefs: SharedPreferences, key: String, default: Float): Float {
        val raw = readRawValue(prefs, key)
        val converted: Float? = when (raw) {
            is Float -> raw
            is Number -> raw.toFloat()
            is String -> raw.trim().toFloatOrNull()
            else -> null
        }
        return if (converted != null && converted.isFinite()) {
            runCatching { prefs.edit().putFloat(key, converted).apply() }
            converted
        } else {
            runCatching { prefs.edit().remove(key).apply() }
            default
        }
    }

    private fun migrateAndReadBoolean(prefs: SharedPreferences, key: String, default: Boolean): Boolean {
        val raw = readRawValue(prefs, key)
        val converted: Boolean? = when (raw) {
            is Boolean -> raw
            is Number -> raw.toInt() != 0
            is String -> when (raw.trim().lowercase(Locale.US)) {
                "true", "1", "yes", "on" -> true
                "false", "0", "no", "off" -> false
                else -> null
            }
            else -> null
        }
        return if (converted != null) {
            runCatching { prefs.edit().putBoolean(key, converted).apply() }
            converted
        } else {
            runCatching { prefs.edit().remove(key).apply() }
            default
        }
    }

    private fun migrateAndReadStringSet(
        prefs: SharedPreferences,
        key: String,
        default: Set<String>
    ): Set<String> {
        val raw = readRawValue(prefs, key)
        val converted: Set<String>? = when (raw) {
            is Set<*> -> raw.mapNotNull { it?.toString()?.trim() }.filter { it.isNotEmpty() }.toSet()
            is String -> parseLegacyStringToSet(raw)
            else -> null
        }
        return if (converted != null) {
            runCatching { prefs.edit().putStringSet(key, converted).apply() }
            converted
        } else {
            runCatching { prefs.edit().remove(key).apply() }
            default
        }
    }

    private fun parseLegacyStringToSet(raw: String): Set<String> {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return emptySet()
        if (trimmed.startsWith("[")) {
            val parsed = runCatching {
                val arr = JSONArray(trimmed)
                buildSet {
                    for (i in 0 until arr.length()) {
                        val item = arr.optString(i, "").trim()
                        if (item.isNotEmpty()) add(item)
                    }
                }
            }.getOrNull()
            if (parsed != null) return parsed
        }
        return trimmed.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }
}

// ---- SharedPreferences convenience extensions ----

fun SharedPreferences.safeGetString(key: String, default: String? = null): String? =
    SafePrefs.getString(this, key, default)

fun SharedPreferences.safeGetInt(key: String, default: Int): Int =
    SafePrefs.getInt(this, key, default)

fun SharedPreferences.safeGetLong(key: String, default: Long): Long =
    SafePrefs.getLong(this, key, default)

fun SharedPreferences.safeGetFloat(key: String, default: Float): Float =
    SafePrefs.getFloat(this, key, default)

fun SharedPreferences.safeGetBoolean(key: String, default: Boolean): Boolean =
    SafePrefs.getBoolean(this, key, default)

fun SharedPreferences.safeGetStringSet(
    key: String,
    default: Set<String> = emptySet()
): Set<String> = SafePrefs.getStringSet(this, key, default)

// ---- Jetpack DataStore Preferences safe extensions ----

private fun Preferences.findRawByName(keyName: String): Any? {
    return runCatching {
        asMap().entries.firstOrNull { it.key.name == keyName }?.value
    }.getOrNull()
}

fun Preferences.safeGetString(key: Preferences.Key<String>, default: String? = null): String? {
    return try {
        this[key] ?: default
    } catch (_: ClassCastException) {
        when (val raw = findRawByName(key.name)) {
            is String -> raw
            is Number -> raw.toString()
            is Boolean -> raw.toString()
            else -> default
        }
    } catch (_: Exception) {
        default
    }
}

fun Preferences.safeGetInt(key: Preferences.Key<Int>, default: Int): Int {
    return try {
        this[key] ?: default
    } catch (_: ClassCastException) {
        when (val raw = findRawByName(key.name)) {
            is Int -> raw
            is Number -> raw.toInt()
            is String -> raw.trim().toIntOrNull() ?: raw.trim().toDoubleOrNull()?.toInt() ?: default
            is Boolean -> if (raw) 1 else 0
            else -> default
        }
    } catch (_: Exception) {
        default
    }
}

fun Preferences.safeGetIntOrNull(key: Preferences.Key<Int>): Int? {
    return try {
        this[key]
    } catch (_: ClassCastException) {
        when (val raw = findRawByName(key.name)) {
            is Int -> raw
            is Number -> raw.toInt()
            is String -> raw.trim().toIntOrNull() ?: raw.trim().toDoubleOrNull()?.toInt()
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}

fun Preferences.safeGetFloat(key: Preferences.Key<Float>, default: Float): Float {
    return try {
        this[key] ?: default
    } catch (_: ClassCastException) {
        when (val raw = findRawByName(key.name)) {
            is Float -> raw
            is Number -> raw.toFloat()
            is String -> raw.trim().toFloatOrNull() ?: default
            else -> default
        }
    } catch (_: Exception) {
        default
    }
}

fun Preferences.safeGetBoolean(key: Preferences.Key<Boolean>, default: Boolean): Boolean {
    return try {
        this[key] ?: default
    } catch (_: ClassCastException) {
        when (val raw = findRawByName(key.name)) {
            is Boolean -> raw
            is Number -> raw.toInt() != 0
            is String -> when (raw.trim().lowercase(Locale.US)) {
                "true", "1", "yes", "on" -> true
                "false", "0", "no", "off" -> false
                else -> default
            }
            else -> default
        }
    } catch (_: Exception) {
        default
    }
}

fun Preferences.safeGetStringSet(
    key: Preferences.Key<Set<String>>,
    default: Set<String> = emptySet()
): Set<String> {
    return try {
        this[key] ?: default
    } catch (_: ClassCastException) {
        when (val raw = findRawByName(key.name)) {
            is Set<*> -> raw.mapNotNull { it?.toString()?.trim() }.filter { it.isNotEmpty() }.toSet()
            is String -> raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            else -> default
        }
    } catch (_: Exception) {
        default
    }
}
