package com.example.alarm

import android.content.Context
import com.example.data.local.safeGetString
import com.example.data.local.safeGetStringSet
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

data class PrayerLogStats(
    val todayCompleted: Int = 0,
    val weeklyCompleted: Int = 0,
    val weeklyTotal: Int = 35,
    val weeklyPercent: Int = 0,
    val monthlyCompleted: Int = 0,
    val monthlyTotal: Int = 150,
    val monthlyPercent: Int = 0,
    val currentStreakDays: Int = 0,
    val pendingQadaaCount: Int = 0,
    val yesterdayMissedCount: Int = 0
)

data class QadaaItem(
    val dateKey: String,
    val prayerKey: String,
    val prayerArabicName: String,
    val friendlyDateArabic: String,
    val isYesterday: Boolean
)

data class SunnahHabitDef(
    val key: String,
    val arabicLabel: String
)

enum class DayCompletionStatus {
    FUTURE,
    BEFORE_TRACKING,
    FULL,       // 5/5 (Green)
    PARTIAL,    // 1..4/5 (Amber)
    MISSED      // 0/5 past tracked day (Red)
}

data class MonthDayCell(
    val dayOfMonth: Int,
    val dateKey: String,
    val performedKeys: Set<String>,
    val madeUpKeys: Set<String>,
    val completedCount: Int,
    val status: DayCompletionStatus,
    val isToday: Boolean,
    val sunnahDoneKeys: Set<String>
)

data class MonthCalendarData(
    val year: Int,
    val monthZeroBased: Int,
    val monthLabelArabic: String,
    val firstDayOffsetFromSat: Int, // 0=Sat, 1=Sun, 2=Mon, 3=Tue, 4=Wed, 5=Thu, 6=Fri
    val days: List<MonthDayCell>,
    val completedPrayers: Int,
    val totalTrackablePrayers: Int,
    val completionPercent: Int,
    val perfectDaysCount: Int
)

data class YearlyMonthStat(
    val monthZeroBased: Int,
    val monthNameArabic: String,
    val completedPrayers: Int,
    val trackablePrayers: Int,
    val percent: Int,
    val perfectDaysCount: Int
)

data class YearlyOverviewData(
    val year: Int,
    val months: List<YearlyMonthStat>,
    val annualCompleted: Int,
    val annualTrackable: Int,
    val annualPercent: Int,
    val perfectDaysCount: Int
)

data class BackupImportResult(
    val success: Boolean,
    val importedDaysCount: Int = 0,
    val errorMessage: String? = null
)

/**
 * Local unlimited per-date Prayer & Worship Tracker store keyed by `YYYY-MM-DD`.
 *
 * - Tracks all 5 Fard prayers across unlimited months and years.
 * - Automatically detects missed past Fard prayers from `first_tracked_date` up to yesterday
 *   for the Auto-Qadaa queue ("صلوات فائتة (للقضاء)").
 * - Tracks optional Sunnah habits (never added to Qadaa).
 * - Mirrors every change to `filesDir/prayer_tracker_backup.json` so Android Auto Backup
 *   restores the entire history automatically after reinstall without any login.
 */
object PrayerLogStore {

    private const val PREFS_NAME = "rafiq_prayer_log_store"
    private const val KEY_PREFIX = "log_"
    private const val KEY_SUNNAH_PREFIX = "sunnah_"
    private const val KEY_FIRST_TRACKED_DATE = "first_tracked_date"
    private const val KEY_QADAA_MADE_UP = "qadaa_made_up_set"
    private const val BACKUP_MIRROR_FILENAME = "prayer_tracker_backup.json"
    private const val BACKUP_DEBOUNCE_MS = 1_200L

    private val backupExecutor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "rafiq-backup-io").apply { isDaemon = true }
    }

    @Volatile
    private var pendingBackupFuture: ScheduledFuture<*>? = null
    private val backupLock = Any()

    val prayerOrder: List<Pair<String, String>> = listOf(
        "Fajr" to "الفجر",
        "Dhuhr" to "الظهر",
        "Asr" to "العصر",
        "Maghrib" to "المغرب",
        "Isha" to "العشاء"
    )

    val sunnahHabits: List<SunnahHabitDef> = listOf(
        SunnahHabitDef("azkar_morning", "أذكار الصباح"),
        SunnahHabitDef("azkar_evening", "أذكار المساء"),
        SunnahHabitDef("quran_wird", "ورد القرآن"),
        SunnahHabitDef("witr_prayer", "صلاة الوتر")
    )

    private val validPrayerKeys = setOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
    private val validSunnahKeys = sunnahHabits.map { it.key }.toSet()

    val arabicMonthNames = arrayOf(
        "يناير",
        "فبراير",
        "مارس",
        "أبريل",
        "مايو",
        "يونيو",
        "يوليو",
        "أغسطس",
        "سبتمبر",
        "أكتوبر",
        "نوفمبر",
        "ديسمبر"
    )

    private fun backupMirrorFile(context: Context): File =
        File(context.applicationContext.filesDir, BACKUP_MIRROR_FILENAME)

    /**
     * Called on app startup. If SharedPreferences has no prayer logs yet (e.g., fresh install
     * restored via Android Auto Backup), automatically restores from `prayer_tracker_backup.json`.
     */
    fun ensureRestoredFromBackupFile(context: Context) {
        runCatching {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val allKeys = runCatching { prefs.all.keys }.getOrDefault(emptySet())
            val hasAnyLogs = allKeys.any { it.startsWith(KEY_PREFIX) || it.startsWith(KEY_SUNNAH_PREFIX) }
            val mirror = backupMirrorFile(context)
            if (!hasAnyLogs && mirror.exists() && mirror.length() > 10L) {
                runCatching {
                    importBackupJson(context, mirror.readText(Charsets.UTF_8))
                }
            } else if (hasAnyLogs && !mirror.exists()) {
                mirrorBackupFile(context)
            }
            // Ensure first_tracked_date is seeded if existing logs exist
            if (prefs.safeGetString(KEY_FIRST_TRACKED_DATE, null).isNullOrBlank()) {
                val earliest = allKeys
                    .filter { it.startsWith(KEY_PREFIX) }
                    .map { it.removePrefix(KEY_PREFIX) }
                    .filter { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }
                    .minOrNull()
                if (earliest != null) {
                    prefs.edit().putString(KEY_FIRST_TRACKED_DATE, earliest).apply()
                }
            }
        }
    }

    fun getFirstTrackedDate(context: Context): String? {
        return runCatching {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val stored = prefs.safeGetString(KEY_FIRST_TRACKED_DATE, null)?.takeIf { it.isNotBlank() }
            if (stored != null) return@runCatching stored
            val earliest = runCatching { prefs.all.keys }.getOrDefault(emptySet())
                .filter { it.startsWith(KEY_PREFIX) }
                .map { it.removePrefix(KEY_PREFIX) }
                .filter { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }
                .minOrNull()
            if (earliest != null) {
                prefs.edit().putString(KEY_FIRST_TRACKED_DATE, earliest).apply()
            }
            earliest
        }.getOrNull()
    }

    private fun updateFirstTrackedDateIfEarlier(prefs: android.content.SharedPreferences, dateKey: String) {
        if (!dateKey.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return
        val current = prefs.safeGetString(KEY_FIRST_TRACKED_DATE, null)
        if (current.isNullOrBlank() || dateKey < current) {
            prefs.edit().putString(KEY_FIRST_TRACKED_DATE, dateKey).apply()
        }
    }

    fun getPerformedPrayers(context: Context, dateKey: String): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.safeGetString(KEY_PREFIX + dateKey, null).orEmpty()
        if (raw.isBlank()) return emptySet()
        return raw.split(",").map { it.trim() }.filter { it in validPrayerKeys }.toSet()
    }

    fun setPerformedPrayers(context: Context, dateKey: String, keys: Set<String>) {
        val clean = keys.filter { it in validPrayerKeys }.toSet()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit().putString(KEY_PREFIX + dateKey, clean.joinToString(","))
        // If user explicitly unchecked a prayer, remove it from qadaa_made_up so it can appear if past
        val madeUp = getQadaaMadeUpSet(context).toMutableSet()
        var madeUpChanged = false
        for (pKey in validPrayerKeys) {
            if (pKey !in clean && madeUp.remove("$dateKey|$pKey")) {
                madeUpChanged = true
            }
        }
        if (madeUpChanged) {
            editor.putStringSet(KEY_QADAA_MADE_UP, madeUp)
        }
        editor.apply()
        if (clean.isNotEmpty()) {
            updateFirstTrackedDateIfEarlier(prefs, dateKey)
        }
        scheduleBackupMirror(context)
    }

    fun togglePrayer(context: Context, dateKey: String, prayerKey: String): Set<String> {
        if (prayerKey !in validPrayerKeys) return getPerformedPrayers(context, dateKey)
        val current = getPerformedPrayers(context, dateKey).toMutableSet()
        if (!current.add(prayerKey)) {
            current.remove(prayerKey)
        }
        setPerformedPrayers(context, dateKey, current)
        return current
    }

    // ---- Auto-Qadaa Queue ("صلوات فائتة (للقضاء)") ----

    private fun getQadaaMadeUpSet(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.safeGetStringSet(KEY_QADAA_MADE_UP, emptySet())
    }

    fun getMadeUpPrayersForDate(context: Context, dateKey: String): Set<String> {
        val prefix = "$dateKey|"
        return getQadaaMadeUpSet(context)
            .asSequence()
            .filter { it.startsWith(prefix) }
            .map { it.removePrefix(prefix) }
            .filter { it in validPrayerKeys }
            .toSet()
    }

    fun markQadaaCompleted(context: Context, dateKey: String, prayerKey: String) {
        if (prayerKey !in validPrayerKeys) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val madeUp = getQadaaMadeUpSet(context).toMutableSet()
        madeUp.add("$dateKey|$prayerKey")

        val performed = getPerformedPrayers(context, dateKey).toMutableSet()
        performed.add(prayerKey)

        prefs.edit()
            .putStringSet(KEY_QADAA_MADE_UP, madeUp)
            .putString(KEY_PREFIX + dateKey, performed.joinToString(","))
            .apply()
        scheduleBackupMirror(context)
    }

    /**
     * Scans all completed past days from `first_tracked_date` up to yesterday.
     * Any of the 5 Fard prayers not marked performed (and not already marked made up)
     * is automatically surfaced in the Qadaa queue, ordered most-recent first.
     */
    fun getPendingQadaaItems(
        context: Context,
        nowMillis: Long = System.currentTimeMillis()
    ): List<QadaaItem> {
        val firstDateStr = getFirstTrackedDate(context) ?: return emptyList()
        val todayCal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val yesterdayCal = (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayKey = AstronomicalPrayerCalculator.dateKey(yesterdayCal)
        val dayBeforeKey = (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -2) }
            .let { AstronomicalPrayerCalculator.dateKey(it) }

        if (firstDateStr > yesterdayKey) return emptyList()

        val madeUpSet = getQadaaMadeUpSet(context)
        val startCal = AstronomicalPrayerCalculator.parseDateKey(firstDateStr)
        val cursor = (yesterdayCal.clone() as Calendar)
        val items = mutableListOf<QadaaItem>()

        // Scan backwards from yesterday to first_tracked_date (safety cap 5 years = 1825 days)
        var scannedDays = 0
        while (!cursor.before(startCal) && scannedDays < 1825) {
            val dKey = AstronomicalPrayerCalculator.dateKey(cursor)
            val performed = getPerformedPrayers(context, dKey)
            val friendlyDate = formatFriendlyPastDateArabic(dKey, yesterdayKey, dayBeforeKey, cursor)
            val isYest = dKey == yesterdayKey

            for ((pKey, pArabic) in prayerOrder) {
                if (pKey !in performed && "$dKey|$pKey" !in madeUpSet) {
                    items.add(
                        QadaaItem(
                            dateKey = dKey,
                            prayerKey = pKey,
                            prayerArabicName = pArabic,
                            friendlyDateArabic = friendlyDate,
                            isYesterday = isYest
                        )
                    )
                }
            }
            cursor.add(Calendar.DAY_OF_YEAR, -1)
            scannedDays++
        }
        return items
    }

    private fun formatFriendlyPastDateArabic(
        dateKey: String,
        yesterdayKey: String,
        dayBeforeKey: String,
        cal: Calendar
    ): String {
        return when (dateKey) {
            yesterdayKey -> "أمس"
            dayBeforeKey -> "أول أمس"
            else -> {
                val day = cal.get(Calendar.DAY_OF_MONTH)
                val month = arabicMonthNames[cal.get(Calendar.MONTH).coerceIn(0, 11)]
                val year = cal.get(Calendar.YEAR)
                "$day $month $year"
            }
        }
    }

    // ---- Optional Sunnah Habits (Never added to Qadaa) ----

    fun getSunnahHabits(context: Context, dateKey: String): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.safeGetString(KEY_SUNNAH_PREFIX + dateKey, null).orEmpty()
        if (raw.isBlank()) return emptySet()
        return raw.split(",").map { it.trim() }.filter { it in validSunnahKeys }.toSet()
    }

    fun toggleSunnahHabit(context: Context, dateKey: String, habitKey: String): Set<String> {
        if (habitKey !in validSunnahKeys) return getSunnahHabits(context, dateKey)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = getSunnahHabits(context, dateKey).toMutableSet()
        if (!current.add(habitKey)) {
            current.remove(habitKey)
        }
        prefs.edit().putString(KEY_SUNNAH_PREFIX + dateKey, current.joinToString(",")).apply()
        scheduleBackupMirror(context)
        return current
    }

    // ---- Unlimited History: Monthly Calendar & Yearly Overview ----

    fun getAvailableYears(context: Context, nowMillis: Long = System.currentTimeMillis()): List<Int> {
        val currentYear = Calendar.getInstance().apply { timeInMillis = nowMillis }.get(Calendar.YEAR)
        val earliestYear = getFirstTrackedDate(context)
            ?.substringBefore("-")
            ?.toIntOrNull()
            ?.coerceAtMost(currentYear)
            ?: currentYear
        val startYear = minOf(earliestYear, currentYear - 1)
        return (currentYear downTo startYear).toList()
    }

    fun getMonthCalendar(
        context: Context,
        year: Int,
        monthZeroBased: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): MonthCalendarData {
        val todayCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val todayKey = AstronomicalPrayerCalculator.dateKey(todayCal)
        val firstTracked = getFirstTrackedDate(context)

        val monthCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased.coerceIn(0, 11))
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val maxDay = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        // Saturday = 0, Sunday = 1, ..., Friday = 6
        val firstDayOffsetFromSat = when (monthCal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }

        val cells = ArrayList<MonthDayCell>(maxDay)
        var completedPrayers = 0
        var trackablePrayers = 0
        var perfectDays = 0

        for (day in 1..maxDay) {
            monthCal.set(Calendar.DAY_OF_MONTH, day)
            val dKey = AstronomicalPrayerCalculator.dateKey(monthCal)
            val performed = getPerformedPrayers(context, dKey)
            val madeUp = getMadeUpPrayersForDate(context, dKey)
            val sunnah = getSunnahHabits(context, dKey)
            val count = (performed + madeUp).size.coerceAtMost(5)
            val isToday = dKey == todayKey
            val isFuture = dKey > todayKey

            val status = when {
                isFuture -> DayCompletionStatus.FUTURE
                count >= 5 -> DayCompletionStatus.FULL
                count in 1..4 -> DayCompletionStatus.PARTIAL
                firstTracked != null && dKey >= firstTracked && !isToday -> DayCompletionStatus.MISSED
                isToday -> DayCompletionStatus.PARTIAL // Today with 0 so far is neutral/in-progress
                else -> DayCompletionStatus.BEFORE_TRACKING
            }

            if (!isFuture) {
                if (count > 0 || isToday || (firstTracked != null && dKey >= firstTracked)) {
                    completedPrayers += count
                    trackablePrayers += 5
                    if (count >= 5) perfectDays++
                }
            }

            cells.add(
                MonthDayCell(
                    dayOfMonth = day,
                    dateKey = dKey,
                    performedKeys = performed,
                    madeUpKeys = madeUp,
                    completedCount = count,
                    status = status,
                    isToday = isToday,
                    sunnahDoneKeys = sunnah
                )
            )
        }

        val percent = if (trackablePrayers > 0) {
            ((completedPrayers * 100f) / trackablePrayers).toInt().coerceIn(0, 100)
        } else {
            0
        }

        return MonthCalendarData(
            year = year,
            monthZeroBased = monthZeroBased.coerceIn(0, 11),
            monthLabelArabic = "${arabicMonthNames[monthZeroBased.coerceIn(0, 11)]} $year",
            firstDayOffsetFromSat = firstDayOffsetFromSat,
            days = cells,
            completedPrayers = completedPrayers,
            totalTrackablePrayers = trackablePrayers,
            completionPercent = percent,
            perfectDaysCount = perfectDays
        )
    }

    fun getYearlyOverview(
        context: Context,
        year: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): YearlyOverviewData {
        var annualDone = 0
        var annualTrackable = 0
        var totalPerfectDays = 0
        val months = (0..11).map { m ->
            val monthData = getMonthCalendar(context, year, m, nowMillis)
            annualDone += monthData.completedPrayers
            annualTrackable += monthData.totalTrackablePrayers
            totalPerfectDays += monthData.perfectDaysCount
            YearlyMonthStat(
                monthZeroBased = m,
                monthNameArabic = arabicMonthNames[m],
                completedPrayers = monthData.completedPrayers,
                trackablePrayers = monthData.totalTrackablePrayers,
                percent = monthData.completionPercent,
                perfectDaysCount = monthData.perfectDaysCount
            )
        }
        val annualPercent = if (annualTrackable > 0) {
            ((annualDone * 100f) / annualTrackable).toInt().coerceIn(0, 100)
        } else {
            0
        }
        return YearlyOverviewData(
            year = year,
            months = months,
            annualCompleted = annualDone,
            annualTrackable = annualTrackable,
            annualPercent = annualPercent,
            perfectDaysCount = totalPerfectDays
        )
    }

    // ---- Summary Stats ----

    fun computeStats(context: Context, nowMillis: Long = System.currentTimeMillis()): PrayerLogStats {
        val todayCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val todayKey = AstronomicalPrayerCalculator.dateKey(todayCal)
        val todayDone = getPerformedPrayers(context, todayKey).size

        var weeklyDone = 0
        for (i in 0 until 7) {
            val cal = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, -i)
            }
            weeklyDone += getPerformedPrayers(context, AstronomicalPrayerCalculator.dateKey(cal)).size
        }
        val weeklyTotal = 35
        val weeklyPct = ((weeklyDone * 100f) / weeklyTotal).toInt().coerceIn(0, 100)

        var monthlyDone = 0
        for (i in 0 until 30) {
            val cal = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, -i)
            }
            monthlyDone += getPerformedPrayers(context, AstronomicalPrayerCalculator.dateKey(cal)).size
        }
        val monthlyTotal = 150
        val monthlyPct = ((monthlyDone * 100f) / monthlyTotal).toInt().coerceIn(0, 100)

        // Streak: consecutive days with all 5 prayers logged
        var streak = 0
        val startOffset = if (todayDone >= 5) 0 else 1
        for (offset in startOffset until 1825) {
            val cal = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, -offset)
            }
            val count = getPerformedPrayers(context, AstronomicalPrayerCalculator.dateKey(cal)).size
            if (count >= 5) {
                streak++
            } else {
                break
            }
        }

        val pendingQadaa = getPendingQadaaItems(context, nowMillis)
        val yesterdayMissed = pendingQadaa.count { it.isYesterday }

        return PrayerLogStats(
            todayCompleted = todayDone,
            weeklyCompleted = weeklyDone,
            weeklyTotal = weeklyTotal,
            weeklyPercent = weeklyPct,
            monthlyCompleted = monthlyDone,
            monthlyTotal = monthlyTotal,
            monthlyPercent = monthlyPct,
            currentStreakDays = streak,
            pendingQadaaCount = pendingQadaa.size,
            yesterdayMissedCount = yesterdayMissed
        )
    }

    // ---- Zero-Login Android Auto Backup Mirror & Manual JSON Export / Import ----

    /**
     * Debounces and runs backup JSON mirroring on a dedicated background I/O thread so
     * rapid prayer/habit toggles coalesce into a single non-blocking disk write.
     */
    fun scheduleBackupMirror(context: Context, delayMs: Long = BACKUP_DEBOUNCE_MS) {
        val appCtx = context.applicationContext
        synchronized(backupLock) {
            pendingBackupFuture?.cancel(false)
            pendingBackupFuture = runCatching {
                backupExecutor.schedule(
                    { mirrorBackupFile(appCtx) },
                    delayMs.coerceAtLeast(0L),
                    TimeUnit.MILLISECONDS
                )
            }.getOrNull()
        }
    }

    fun mirrorBackupFile(context: Context) {
        runCatching {
            val json = exportBackupJson(context)
            val target = backupMirrorFile(context)
            val tmp = File(target.parentFile, "$BACKUP_MIRROR_FILENAME.tmp")
            tmp.writeText(json, Charsets.UTF_8)
            if (target.exists()) target.delete()
            if (!tmp.renameTo(target)) {
                target.writeText(json, Charsets.UTF_8)
                tmp.delete()
            }
        }
    }

    fun exportBackupJson(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val root = JSONObject()
        root.put("app", "rafiq_al_muslim")
        root.put("schema_version", 1)
        root.put(
            "exported_at",
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Calendar.getInstance().time)
        )
        root.put("first_tracked_date", getFirstTrackedDate(context).orEmpty())

        val logsObj = JSONObject()
        val sunnahObj = JSONObject()
        prefs.all.forEach { (key, value) ->
            val strVal = value as? String ?: return@forEach
            when {
                key.startsWith(KEY_PREFIX) -> {
                    val dateKey = key.removePrefix(KEY_PREFIX)
                    val arr = JSONArray()
                    strVal.split(",").map { it.trim() }.filter { it in validPrayerKeys }.forEach { arr.put(it) }
                    if (arr.length() > 0) logsObj.put(dateKey, arr)
                }
                key.startsWith(KEY_SUNNAH_PREFIX) -> {
                    val dateKey = key.removePrefix(KEY_SUNNAH_PREFIX)
                    val arr = JSONArray()
                    strVal.split(",").map { it.trim() }.filter { it in validSunnahKeys }.forEach { arr.put(it) }
                    if (arr.length() > 0) sunnahObj.put(dateKey, arr)
                }
            }
        }
        root.put("prayer_logs", logsObj)
        root.put("sunnah_logs", sunnahObj)

        val qadaaArr = JSONArray()
        getQadaaMadeUpSet(context).sorted().forEach { qadaaArr.put(it) }
        root.put("qadaa_made_up", qadaaArr)

        return root.toString(2)
    }

    fun importBackupJson(context: Context, rawJson: String): BackupImportResult {
        return runCatching {
            val trimmed = rawJson.trim()
            if (trimmed.isEmpty()) {
                return BackupImportResult(false, errorMessage = "ملف النسخة الاحتياطية فارغ")
            }
            val root = JSONObject(trimmed)
            val logsObj = root.optJSONObject("prayer_logs")
                ?: return BackupImportResult(false, errorMessage = "تنسيق النسخة الاحتياطية غير صالح")
            val sunnahObj = root.optJSONObject("sunnah_logs")
            val qadaaArr = root.optJSONArray("qadaa_made_up")
            val importedFirstDate = root.optString("first_tracked_date", "").trim()

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            var importedDays = 0
            var earliestDate = prefs.safeGetString(KEY_FIRST_TRACKED_DATE, null)?.takeIf { it.isNotBlank() }

            if (importedFirstDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                if (earliestDate == null || importedFirstDate < earliestDate) {
                    earliestDate = importedFirstDate
                }
            }

            val logKeys = logsObj.keys()
            while (logKeys.hasNext()) {
                val dateKey = logKeys.next()
                if (!dateKey.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) continue
                val arr = logsObj.optJSONArray(dateKey) ?: continue
                val existing = getPerformedPrayers(context, dateKey).toMutableSet()
                for (i in 0 until arr.length()) {
                    val p = arr.optString(i).trim()
                    if (p in validPrayerKeys) existing.add(p)
                }
                if (existing.isNotEmpty()) {
                    editor.putString(KEY_PREFIX + dateKey, existing.joinToString(","))
                    importedDays++
                    if (earliestDate == null || dateKey < earliestDate) {
                        earliestDate = dateKey
                    }
                }
            }

            if (sunnahObj != null) {
                val sKeys = sunnahObj.keys()
                while (sKeys.hasNext()) {
                    val dateKey = sKeys.next()
                    if (!dateKey.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) continue
                    val arr = sunnahObj.optJSONArray(dateKey) ?: continue
                    val existing = getSunnahHabits(context, dateKey).toMutableSet()
                    for (i in 0 until arr.length()) {
                        val h = arr.optString(i).trim()
                        if (h in validSunnahKeys) existing.add(h)
                    }
                    if (existing.isNotEmpty()) {
                        editor.putString(KEY_SUNNAH_PREFIX + dateKey, existing.joinToString(","))
                    }
                }
            }

            if (qadaaArr != null) {
                val mergedQadaa = getQadaaMadeUpSet(context).toMutableSet()
                for (i in 0 until qadaaArr.length()) {
                    val entry = qadaaArr.optString(i).trim()
                    if (entry.contains("|")) mergedQadaa.add(entry)
                }
                editor.putStringSet(KEY_QADAA_MADE_UP, mergedQadaa)
            }

            if (!earliestDate.isNullOrBlank()) {
                editor.putString(KEY_FIRST_TRACKED_DATE, earliestDate)
            }

            editor.apply()
            mirrorBackupFile(context)
            BackupImportResult(success = true, importedDaysCount = importedDays)
        }.getOrElse { err ->
            BackupImportResult(
                success = false,
                errorMessage = "تعذّر قراءة ملف النسخة الاحتياطية: ${err.localizedMessage ?: "تنسيق غير مدعوم"}"
            )
        }
    }
}
