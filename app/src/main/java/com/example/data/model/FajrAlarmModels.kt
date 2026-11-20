package com.example.data.model

import com.squareup.moshi.JsonClass
import java.util.Locale

enum class FajrChallengeType(val arabicTitle: String, val arabicDescription: String) {
    QR_CODE("مسح رمز QR", "امسح الرمز المحفوظ بكاميرا الهاتف لإيقاف المنبه"),
    MATH("مسائل رياضية", "حل مسائل حسابية متتالية لتنشيط ذهنك"),
    MEMORY("لعبة الذاكرة", "احفظ مواقع المربعات المضيئة ثم اضغط عليها بالترتيب"),
    TRIVIA("أسئلة عشوائية", "أجب عن 3 أسئلة صحيحة متتالية في التاريخ والرياضيات")
}

enum class FajrAlarmSound(val arabicLabel: String) {
    FAJR_ADHAN("أذان الفجر (الصلاة خير من النوم)"),
    LOUD_ALARM("نغمة منبه قوية متكررة")
}

enum class MathDifficulty(val arabicLabel: String) {
    EASY("سهل"),
    MEDIUM("متوسط"),
    HARD("صعب")
}

enum class MemoryGridSize(val arabicLabel: String, val dimension: Int, val targetCount: Int) {
    GRID_3X3("شبكة 3×3", 3, 4),
    GRID_4X4("شبكة 4×4", 4, 6)
}

@JsonClass(generateAdapter = true)
data class FajrAlarmSettings(
    val enabled: Boolean = false,
    val offsetMinutesBeforeFajr: Int = 15,
    val soundType: String = FajrAlarmSound.FAJR_ADHAN.name,
    val challengeType: String = FajrChallengeType.MATH.name,
    val targetQrCode: String = "RAFIQ-FAJR-2026",
    val mathDifficulty: String = MathDifficulty.MEDIUM.name,
    val mathProblemCount: Int = 3,
    val memoryGridSize: String = MemoryGridSize.GRID_3X3.name
) {
    fun resolvedSound(): FajrAlarmSound =
        FajrAlarmSound.entries.find { it.name == soundType } ?: FajrAlarmSound.FAJR_ADHAN

    fun resolvedChallenge(): FajrChallengeType =
        FajrChallengeType.entries.find { it.name == challengeType } ?: FajrChallengeType.MATH

    fun resolvedMathDifficulty(): MathDifficulty =
        MathDifficulty.entries.find { it.name == mathDifficulty } ?: MathDifficulty.MEDIUM

    fun resolvedMemoryGrid(): MemoryGridSize =
        MemoryGridSize.entries.find { it.name == memoryGridSize } ?: MemoryGridSize.GRID_3X3
}

data class MathChallengeProblem(
    val expression: String,
    val answer: Int
)

data class TriviaChallengeQuestion(
    val id: Int,
    val category: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int
)

data class CalculatedWakeupTime(
    val time24: String,
    val formatted12Arabic: String,
    val fajrFormatted12Arabic: String
)

object FajrTimeCalculator {
    fun calculateWakeupTime(fajrTime24: String, offsetMinutesBefore: Int): CalculatedWakeupTime {
        val parts = fajrTime24.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 4
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 55
        val fajrTotalMinutes = (h * 60) + m
        val clampedOffset = offsetMinutesBefore.coerceIn(0, 180)
        val wakeupTotal = ((fajrTotalMinutes - clampedOffset) % 1440 + 1440) % 1440
        val wh = wakeupTotal / 60
        val wm = wakeupTotal % 60
        val wakeup24 = String.format(Locale.US, "%02d:%02d", wh, wm)
        return CalculatedWakeupTime(
            time24 = wakeup24,
            formatted12Arabic = format12Arabic(wh, wm),
            fajrFormatted12Arabic = format12Arabic(h, m)
        )
    }

    private fun format12Arabic(hour24: Int, minute: Int): String {
        val suffix = if (hour24 >= 12) "م" else "ص"
        val h12 = if (hour24 % 12 == 0) 12 else hour24 % 12
        val minStr = minute.toString().padStart(2, '0')
        return "$h12:$minStr $suffix"
    }
}
