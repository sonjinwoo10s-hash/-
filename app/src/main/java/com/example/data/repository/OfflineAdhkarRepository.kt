package com.example.data.repository

import android.content.Context
import com.example.data.local.OfflineIslamicData
import com.example.data.model.ZikrItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 100% offline Adhkar loader backed directly by `assets/adhkar.json`.
 * Loads strictly on-demand on [Dispatchers.IO] when the Adhkar screen is opened
 * (zero startup RAM overhead) and supports instant memory release on tab exit.
 */
object OfflineAdhkarRepository {

    private const val ASSET_FILE_NAME = "adhkar.json"

    @Volatile
    private var cachedCategories: LinkedHashMap<String, List<ZikrItem>>? = null

    suspend fun loadCategories(context: Context): LinkedHashMap<String, List<ZikrItem>> {
        cachedCategories?.let { return it }
        return withContext(Dispatchers.IO) {
            val parsed = runCatching {
                val jsonText = context.applicationContext.assets.open(ASSET_FILE_NAME)
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }
                parseAdhkarJson(jsonText)
            }.getOrNull()

            val result = if (!parsed.isNullOrEmpty()) parsed else OfflineIslamicData.azkarCategories
            cachedCategories = result
            result
        }
    }

    fun clearMemoryCache() {
        cachedCategories = null
    }

    private fun parseAdhkarJson(jsonText: String): LinkedHashMap<String, List<ZikrItem>> = runCatching {
        val root = JSONObject(jsonText)
        val categoriesArr = root.optJSONArray("categories") ?: return@runCatching linkedMapOf()
        val result = LinkedHashMap<String, List<ZikrItem>>(categoriesArr.length())
        for (i in 0 until categoriesArr.length()) {
            runCatching {
                val catObj = categoriesArr.optJSONObject(i) ?: return@runCatching
                val name = catObj.optString("name", "").trim()
                if (name.isEmpty()) return@runCatching
                val itemsArr = catObj.optJSONArray("items") ?: return@runCatching
                val items = ArrayList<ZikrItem>(itemsArr.length())
                for (j in 0 until itemsArr.length()) {
                    runCatching {
                        val itemObj = itemsArr.optJSONObject(j) ?: return@runCatching
                        val id = itemObj.optString("id", "").trim()
                        val text = itemObj.optString("text", "").trim()
                        val virtue = itemObj.optString("virtue", "").trim()
                        val count = itemObj.optInt("count", 1).coerceAtLeast(1)
                        if (id.isNotEmpty() && text.isNotEmpty()) {
                            items.add(ZikrItem(id = id, text = text, virtue = virtue, requiredCount = count))
                        }
                    }
                }
                if (items.isNotEmpty()) {
                    result[name] = items
                }
            }
        }
        result
    }.getOrDefault(linkedMapOf())
}
