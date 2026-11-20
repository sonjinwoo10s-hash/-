package com.example.data.model

enum class HadithBookCollection(
    val id: String,
    val arabicTitle: String,
    val shortTitle: String,
    val subtitle: String,
    val apiEdition: String
) {
    BUKHARI(
        id = "bukhari",
        arabicTitle = "صحيح البخاري",
        shortTitle = "البخاري",
        subtitle = "أصح الكتب بعد كتاب الله للإمام محمد بن إسماعيل البخاري",
        apiEdition = "ara-bukhari"
    ),
    MUSLIM(
        id = "muslim",
        arabicTitle = "صحيح مسلم",
        shortTitle = "مسلم",
        subtitle = "الجامع الصحيح للإمام مسلم بن الحجاج النيسابوري",
        apiEdition = "ara-muslim"
    ),
    QUDSI(
        id = "qudsi",
        arabicTitle = "الأحاديث القدسية",
        shortTitle = "القدسية",
        subtitle = "الأربعون القدسية مما يرويه النبي ﷺ عن ربه تبارك وتعالى",
        apiEdition = "ara-qudsi"
    ),
    NAWAWI(
        id = "nawawi",
        arabicTitle = "الأربعين النووية",
        shortTitle = "الأربعين",
        subtitle = "جوامع الكلم وأصول الدين للإمام يحيى بن شرف النووي",
        apiEdition = "ara-nawawi"
    )
}

object HadithChapters {
    const val ALL = "الكل"
    const val FAVORITES = "المفضلة"
    const val IMAN = "باب الإيمان"
    const val SALAH = "باب الصلاة"
    const val ZIKR = "باب الذكر والدعاء"
    const val AKHLAQ = "باب الأخلاق والبر"
    const val SIYAM = "باب الصيام"
    const val TAWBAH = "باب التوبة والرحمة"
    const val GENERAL = "أبواب متنوعة"

    val allChapterTabs: List<String> = listOf(
        ALL,
        FAVORITES,
        IMAN,
        SALAH,
        ZIKR,
        AKHLAQ,
        SIYAM,
        TAWBAH,
        GENERAL
    )

    fun classifyChapter(arabicText: String, bookSectionName: String = ""): String {
        val combined = "$bookSectionName $arabicText"
        return when {
            combined.contains("الصلاة") || combined.contains("صلى") || combined.contains("المسجد") ||
                combined.contains("الفجر") || combined.contains("الركوع") || combined.contains("السجود") ||
                combined.contains("الوضوء") || combined.contains("الطهور") -> SALAH

            combined.contains("صام") || combined.contains("الصيام") || combined.contains("الصوم") ||
                combined.contains("رمضان") || combined.contains("السحور") || combined.contains("الصائمين") -> SIYAM

            combined.contains("التوبة") || combined.contains("تاب") || combined.contains("الرحمة") ||
                combined.contains("رحمتي") || combined.contains("مغفرة") || combined.contains("غفر") ||
                combined.contains("استغفر") || combined.contains("الذنوب") -> TAWBAH

            combined.contains("الذكر") || combined.contains("ذكرني") || combined.contains("سبحان") ||
                combined.contains("الدعاء") || combined.contains("يدعوني") || combined.contains("الحمد") ||
                combined.contains("لا إله إلا الله") -> ZIKR

            combined.contains("البر") || combined.contains("الخلق") || combined.contains("الوالدين") ||
                combined.contains("الرحم") || combined.contains("الصدق") || combined.contains("أخيه") ||
                combined.contains("جاره") || combined.contains("لا تغضب") || combined.contains("الحياء") ||
                combined.contains("الظلم") || combined.contains("صدقة") -> AKHLAQ

            combined.contains("الإيمان") || combined.contains("يؤمن") || combined.contains("الإسلام") ||
                combined.contains("النية") || combined.contains("النيات") || combined.contains("التوحيد") ||
                combined.contains("الشرك") || combined.contains("القدر") || combined.contains("العلم") -> IMAN

            else -> GENERAL
        }
    }
}

data class LibraryHadith(
    val uid: String, // e.g. "bukhari_1"
    val book: HadithBookCollection,
    val number: Int,
    val chapter: String,
    val title: String,
    val narrator: String,
    val text: String,
    val explanation: String = "",
    val source: String
)
