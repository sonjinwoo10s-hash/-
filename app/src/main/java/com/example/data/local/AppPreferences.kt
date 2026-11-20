package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.FajrAlarmSettings
import com.example.data.model.MuezzinAudioOverride
import com.example.data.model.MuezzinItem
import com.example.data.model.MushafPaperTheme
import com.example.data.model.QuranBookmark
import com.example.data.model.ReciterOption
import com.example.data.model.SurahSummary
import com.example.data.model.TasbihState
import com.example.data.remote.IslamicNetworkClient
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.emptyPreferences

private val Context.rafiqDataStore: DataStore<Preferences> by preferencesDataStore(name = "rafiq_al_muslim_prefs")

class AppPreferences(private val context: Context) {

    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val SELECTED_CITY_ID = stringPreferencesKey("selected_city_id")
        val TRACKER_DATE = stringPreferencesKey("tracker_date")
        val TRACKER_DONE_IDS = stringSetPreferencesKey("tracker_done_ids")

        val BOOKMARK_SURAH_NUM = intPreferencesKey("bookmark_surah_num")
        val BOOKMARK_SURAH_NAME = stringPreferencesKey("bookmark_surah_name")
        val BOOKMARK_AYAH_NUM = intPreferencesKey("bookmark_ayah_num")
        val QURAN_FONT_SIZE = floatPreferencesKey("quran_font_size")
        val SURAH_CACHE_JSON = stringPreferencesKey("surah_cache_json")

        val TASBIH_PHRASE = stringPreferencesKey("tasbih_phrase")
        val TASBIH_TARGET = intPreferencesKey("tasbih_target")
        val TASBIH_CURRENT = intPreferencesKey("tasbih_current")
        val TASBIH_ROUNDS = intPreferencesKey("tasbih_rounds")
        val TASBIH_LIFETIME = intPreferencesKey("tasbih_lifetime")

        val AZKAR_DATE = stringPreferencesKey("azkar_date")
        val AZKAR_REMAINING_JSON = stringPreferencesKey("azkar_remaining_json")

        // Global Muezzin Library
        val DEFAULT_MUEZZIN_ID = stringPreferencesKey("default_muezzin_id")
        val FAJR_MUEZZIN_ID = stringPreferencesKey("fajr_muezzin_id")
        val USE_DEDICATED_FAJR_ADHAN = booleanPreferencesKey("use_dedicated_fajr_adhan")
        val FAVORITE_MUEZZIN_IDS = stringSetPreferencesKey("favorite_muezzin_ids")
        val CUSTOM_MUEZZINS_JSON = stringPreferencesKey("custom_muezzins_json")
        val MUEZZIN_AUDIO_OVERRIDES_JSON = stringPreferencesKey("muezzin_audio_overrides_json")

        // Quran Reciters Library & Mushaf
        val SELECTED_QURAN_RECITER_JSON = stringPreferencesKey("selected_quran_reciter_json")
        val RECITERS_CACHE_JSON = stringPreferencesKey("reciters_cache_json")
        val MUSHAF_PAPER_THEME = stringPreferencesKey("mushaf_paper_theme")

        // Smart Fajr Alarm
        val FAJR_ALARM_SETTINGS_JSON = stringPreferencesKey("fajr_alarm_settings_json")

        // Upgraded Hadith Library
        val FAVORITE_HADITH_UIDS = stringSetPreferencesKey("favorite_hadith_uids")
        val HADITH_FONT_SIZE = floatPreferencesKey("hadith_font_size")
    }

    private val safeDataFlow: Flow<Preferences> = context.rafiqDataStore.data.catch {
        emit(emptyPreferences())
    }

    private val moshi = IslamicNetworkClient.moshi
    private val surahListAdapter = moshi.adapter<List<SurahSummary>>(
        Types.newParameterizedType(List::class.java, SurahSummary::class.java)
    )
    private val azkarMapAdapter = moshi.adapter<Map<String, Int>>(
        Types.newParameterizedType(Map::class.java, String::class.java, Integer::class.java)
    )
    private val muezzinListAdapter = moshi.adapter<List<MuezzinItem>>(
        Types.newParameterizedType(List::class.java, MuezzinItem::class.java)
    )
    private val muezzinOverrideListAdapter = moshi.adapter<List<MuezzinAudioOverride>>(
        Types.newParameterizedType(List::class.java, MuezzinAudioOverride::class.java)
    )
    private val reciterAdapter = moshi.adapter(ReciterOption::class.java)
    private val reciterListAdapter = moshi.adapter<List<ReciterOption>>(
        Types.newParameterizedType(List::class.java, ReciterOption::class.java)
    )
    private val fajrAlarmAdapter = moshi.adapter(FajrAlarmSettings::class.java)

    val darkModeFlow: Flow<Boolean> = context.rafiqDataStore.data.map { it[Keys.DARK_MODE] ?: false }

    val selectedCityIdFlow: Flow<String> = context.rafiqDataStore.data.map {
        it[Keys.SELECTED_CITY_ID] ?: OfflineIslamicData.cities.first().id
    }

    val quranFontSizeFlow: Flow<Float> = context.rafiqDataStore.data.map {
        it[Keys.QURAN_FONT_SIZE] ?: 25f
    }

    val bookmarkFlow: Flow<QuranBookmark?> = context.rafiqDataStore.data.map { prefs ->
        val num = prefs[Keys.BOOKMARK_SURAH_NUM]
        val name = prefs[Keys.BOOKMARK_SURAH_NAME]
        val ayah = prefs[Keys.BOOKMARK_AYAH_NUM]
        if (num != null && name != null && ayah != null) {
            QuranBookmark(surahNumber = num, surahName = name, ayahNumber = ayah)
        } else null
    }

    val tasbihFlow: Flow<TasbihState> = context.rafiqDataStore.data.map { prefs ->
        TasbihState(
            phrase = prefs[Keys.TASBIH_PHRASE] ?: "سبحان الله",
            target = prefs[Keys.TASBIH_TARGET] ?: 33,
            current = prefs[Keys.TASBIH_CURRENT] ?: 0,
            rounds = prefs[Keys.TASBIH_ROUNDS] ?: 0,
            lifetime = prefs[Keys.TASBIH_LIFETIME] ?: 0
        )
    }

    val defaultMuezzinIdFlow: Flow<String> = context.rafiqDataStore.data.map {
        it[Keys.DEFAULT_MUEZZIN_ID] ?: MuezzinLibraryData.defaultMuezzinId
    }

    val fajrMuezzinIdFlow: Flow<String> = context.rafiqDataStore.data.map {
        it[Keys.FAJR_MUEZZIN_ID] ?: MuezzinLibraryData.defaultFajrMuezzinId
    }

    val useDedicatedFajrAdhanFlow: Flow<Boolean> = context.rafiqDataStore.data.map {
        it[Keys.USE_DEDICATED_FAJR_ADHAN] ?: true
    }

    val favoriteMuezzinIdsFlow: Flow<Set<String>> = context.rafiqDataStore.data.map {
        it[Keys.FAVORITE_MUEZZIN_IDS] ?: setOf(MuezzinLibraryData.defaultMuezzinId)
    }

    val customMuezzinsFlow: Flow<List<MuezzinItem>> = safeDataFlow.map { prefs ->
        val json = prefs[Keys.CUSTOM_MUEZZINS_JSON] ?: return@map emptyList()
        runCatching { muezzinListAdapter.fromJson(json) }.getOrNull().orEmpty()
    }

    val muezzinAudioOverridesFlow: Flow<Map<String, MuezzinAudioOverride>> = safeDataFlow.map { prefs ->
        val json = prefs[Keys.MUEZZIN_AUDIO_OVERRIDES_JSON] ?: return@map emptyMap()
        runCatching { muezzinOverrideListAdapter.fromJson(json) }
            .getOrNull()
            .orEmpty()
            .associateBy { it.muezzinId }
    }

    val selectedQuranReciterFlow: Flow<ReciterOption> = safeDataFlow.map { prefs ->
        val json = prefs[Keys.SELECTED_QURAN_RECITER_JSON]
        if (json.isNullOrBlank()) {
            OfflineIslamicData.reciters.first()
        } else {
            runCatching { reciterAdapter.fromJson(json) }.getOrNull()
                ?: OfflineIslamicData.reciters.first()
        }
    }

    val mushafPaperThemeFlow: Flow<MushafPaperTheme> = safeDataFlow.map { prefs ->
        val raw = prefs[Keys.MUSHAF_PAPER_THEME]
        MushafPaperTheme.entries.find { it.name == raw } ?: MushafPaperTheme.CREAM
    }

    val fajrAlarmSettingsFlow: Flow<FajrAlarmSettings> = safeDataFlow.map { prefs ->
        val json = prefs[Keys.FAJR_ALARM_SETTINGS_JSON]
        if (json.isNullOrBlank()) {
            FajrAlarmSettings()
        } else {
            runCatching { fajrAlarmAdapter.fromJson(json) }.getOrNull() ?: FajrAlarmSettings()
        }
    }

    val favoriteHadithUidsFlow: Flow<Set<String>> = safeDataFlow.map { prefs ->
        prefs[Keys.FAVORITE_HADITH_UIDS] ?: emptySet()
    }

    val hadithFontSizeFlow: Flow<Float> = safeDataFlow.map { prefs ->
        prefs[Keys.HADITH_FONT_SIZE] ?: 20f
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.rafiqDataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    suspend fun setSelectedCityId(cityId: String) {
        context.rafiqDataStore.edit { it[Keys.SELECTED_CITY_ID] = cityId }
    }

    suspend fun getTrackerDoneIds(todayKey: String): Set<String> {
        val prefs = context.rafiqDataStore.data.first()
        return if (prefs[Keys.TRACKER_DATE] == todayKey) {
            prefs[Keys.TRACKER_DONE_IDS] ?: emptySet()
        } else {
            emptySet()
        }
    }

    suspend fun saveTrackerDoneIds(todayKey: String, ids: Set<String>) {
        context.rafiqDataStore.edit {
            it[Keys.TRACKER_DATE] = todayKey
            it[Keys.TRACKER_DONE_IDS] = ids
        }
    }

    suspend fun saveBookmark(bookmark: QuranBookmark) {
        context.rafiqDataStore.edit {
            it[Keys.BOOKMARK_SURAH_NUM] = bookmark.surahNumber
            it[Keys.BOOKMARK_SURAH_NAME] = bookmark.surahName
            it[Keys.BOOKMARK_AYAH_NUM] = bookmark.ayahNumber
        }
    }

    suspend fun setQuranFontSize(sizeSp: Float) {
        context.rafiqDataStore.edit { it[Keys.QURAN_FONT_SIZE] = sizeSp }
    }

    suspend fun saveTasbih(state: TasbihState) {
        context.rafiqDataStore.edit {
            it[Keys.TASBIH_PHRASE] = state.phrase
            it[Keys.TASBIH_TARGET] = state.target
            it[Keys.TASBIH_CURRENT] = state.current
            it[Keys.TASBIH_ROUNDS] = state.rounds
            it[Keys.TASBIH_LIFETIME] = state.lifetime
        }
    }

    suspend fun getAzkarRemaining(todayKey: String): Map<String, Int> {
        val prefs = context.rafiqDataStore.data.first()
        if (prefs[Keys.AZKAR_DATE] != todayKey) return emptyMap()
        val json = prefs[Keys.AZKAR_REMAINING_JSON] ?: return emptyMap()
        return runCatching { azkarMapAdapter.fromJson(json) }.getOrNull().orEmpty()
    }

    suspend fun saveAzkarRemaining(todayKey: String, map: Map<String, Int>) {
        val json = runCatching { azkarMapAdapter.toJson(map) }.getOrNull() ?: return
        context.rafiqDataStore.edit {
            it[Keys.AZKAR_DATE] = todayKey
            it[Keys.AZKAR_REMAINING_JSON] = json
        }
    }

    suspend fun getCachedSurahs(): List<SurahSummary>? {
        val json = context.rafiqDataStore.data.first()[Keys.SURAH_CACHE_JSON] ?: return null
        return runCatching { surahListAdapter.fromJson(json) }.getOrNull()
    }

    suspend fun cacheSurahs(surahs: List<SurahSummary>) {
        val json = runCatching { surahListAdapter.toJson(surahs) }.getOrNull() ?: return
        context.rafiqDataStore.edit { it[Keys.SURAH_CACHE_JSON] = json }
    }

    suspend fun setDefaultMuezzinId(id: String) {
        context.rafiqDataStore.edit { it[Keys.DEFAULT_MUEZZIN_ID] = id }
    }

    suspend fun setFajrMuezzinId(id: String) {
        context.rafiqDataStore.edit { it[Keys.FAJR_MUEZZIN_ID] = id }
    }

    suspend fun setUseDedicatedFajrAdhan(enabled: Boolean) {
        context.rafiqDataStore.edit { it[Keys.USE_DEDICATED_FAJR_ADHAN] = enabled }
    }

    suspend fun saveFavoriteMuezzinIds(ids: Set<String>) {
        context.rafiqDataStore.edit { it[Keys.FAVORITE_MUEZZIN_IDS] = ids }
    }

    suspend fun saveCustomMuezzins(items: List<MuezzinItem>) {
        val json = runCatching { muezzinListAdapter.toJson(items) }.getOrNull() ?: return
        runCatching { context.rafiqDataStore.edit { it[Keys.CUSTOM_MUEZZINS_JSON] = json } }
    }

    suspend fun saveMuezzinAudioOverrides(overrides: Map<String, MuezzinAudioOverride>) {
        val json = runCatching { muezzinOverrideListAdapter.toJson(overrides.values.toList()) }.getOrNull() ?: return
        runCatching { context.rafiqDataStore.edit { it[Keys.MUEZZIN_AUDIO_OVERRIDES_JSON] = json } }
    }

    suspend fun saveSelectedQuranReciter(reciter: ReciterOption) {
        val json = runCatching { reciterAdapter.toJson(reciter) }.getOrNull() ?: return
        context.rafiqDataStore.edit { it[Keys.SELECTED_QURAN_RECITER_JSON] = json }
    }

    suspend fun getCachedReciters(): List<ReciterOption>? {
        val json = context.rafiqDataStore.data.first()[Keys.RECITERS_CACHE_JSON] ?: return null
        return runCatching { reciterListAdapter.fromJson(json) }.getOrNull()
    }

    suspend fun cacheReciters(reciters: List<ReciterOption>) {
        val json = runCatching { reciterListAdapter.toJson(reciters) }.getOrNull() ?: return
        runCatching { context.rafiqDataStore.edit { it[Keys.RECITERS_CACHE_JSON] = json } }
    }

    suspend fun setMushafPaperTheme(theme: MushafPaperTheme) {
        runCatching { context.rafiqDataStore.edit { it[Keys.MUSHAF_PAPER_THEME] = theme.name } }
    }

    suspend fun saveFajrAlarmSettings(settings: FajrAlarmSettings) {
        val json = runCatching { fajrAlarmAdapter.toJson(settings) }.getOrNull() ?: return
        runCatching { context.rafiqDataStore.edit { it[Keys.FAJR_ALARM_SETTINGS_JSON] = json } }
    }

    suspend fun saveFavoriteHadithUids(uids: Set<String>) {
        runCatching { context.rafiqDataStore.edit { it[Keys.FAVORITE_HADITH_UIDS] = uids } }
    }

    suspend fun setHadithFontSize(sizeSp: Float) {
        runCatching { context.rafiqDataStore.edit { it[Keys.HADITH_FONT_SIZE] = sizeSp } }
    }
}
