package com.example.data.model

enum class HadithBookCollection(
    val id: String,
    val arabicTitle: String,
    val shortTitle: String,
    val author: String,
    val subtitle: String,
    val editionFile: String,
    val expectedCount: Int,
    val estimatedBytes: Long,
    val autoDownloadOnFirstOpen: Boolean = false
) {
    NAWAWI(
        id = "nawawi",
        arabicTitle = "الأربعون النووية",
        shortTitle = "الأربعون النووية",
        author = "الإمام يحيى بن شرف النووي",
        subtitle = "جوامع الكلم وأصول الدين الإسلامي",
        editionFile = "ara-nawawi.min.json",
        expectedCount = 42,
        estimatedBytes = 95_000L,
        autoDownloadOnFirstOpen = true
    ),
    BUKHARI(
        id = "bukhari",
        arabicTitle = "صحيح البخاري",
        shortTitle = "البخاري",
        author = "الإمام محمد بن إسماعيل البخاري",
        subtitle = "أصح الكتب بعد كتاب الله عز وجل",
        editionFile = "ara-bukhari.min.json",
        expectedCount = 7563,
        estimatedBytes = 8_400_000L
    ),
    MUSLIM(
        id = "muslim",
        arabicTitle = "صحيح مسلم",
        shortTitle = "مسلم",
        author = "الإمام مسلم بن الحجاج النيسابوري",
        subtitle = "الجامع الصحيح المسند من حديث رسول الله ﷺ",
        editionFile = "ara-muslim.min.json",
        expectedCount = 7563,
        estimatedBytes = 7_600_000L
    ),
    RIYAD(
        id = "riyadussalihin",
        arabicTitle = "رياض الصالحين",
        shortTitle = "رياض الصالحين",
        author = "الإمام يحيى بن شرف النووي",
        subtitle = "مختارات من أصح الأحاديث في الآداب والرقائق",
        editionFile = "ara-riyadussalihin.min.json",
        expectedCount = 1896,
        estimatedBytes = 2_100_000L
    ),
    ABUDAWUD(
        id = "abudawud",
        arabicTitle = "سنن أبي داود",
        shortTitle = "أبو داود",
        author = "الإمام سليمان بن الأشعث السجستاني",
        subtitle = "من أمهات كتب السنن والأحكام الفقهية",
        editionFile = "ara-abudawud.min.json",
        expectedCount = 5274,
        estimatedBytes = 5_400_000L
    ),
    TIRMIDHI(
        id = "tirmidhi",
        arabicTitle = "جامع الترمذي",
        shortTitle = "الترمذي",
        author = "الإمام محمد بن عيسى الترمذي",
        subtitle = "الجامع الكبير في السنن والعلل والفقه",
        editionFile = "ara-tirmidhi.min.json",
        expectedCount = 3956,
        estimatedBytes = 4_800_000L
    ),
    NASAI(
        id = "nasai",
        arabicTitle = "سنن النسائي",
        shortTitle = "النسائي",
        author = "الإمام أحمد بن شعيب النسائي",
        subtitle = "المجتبى من السنن الصغرى",
        editionFile = "ara-nasai.min.json",
        expectedCount = 5758,
        estimatedBytes = 5_600_000L
    ),
    IBNMAJAH(
        id = "ibnmajah",
        arabicTitle = "سنن ابن ماجه",
        shortTitle = "ابن ماجه",
        author = "الإمام محمد بن يزيد بن ماجه القزويني",
        subtitle = "سادس الكتب الستة في السنة النبوية",
        editionFile = "ara-ibnmajah.min.json",
        expectedCount = 4341,
        estimatedBytes = 4_300_000L
    );

    companion object {
        fun fromId(id: String): HadithBookCollection? = entries.find { it.id == id }
    }
}

data class OfflineHadithBookMeta(
    val bookId: String,
    val isDownloaded: Boolean,
    val actualCount: Int,
    val fileSizeBytes: Long
)

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
    val source: String,
    val normalizedSearchText: String = ""
)
