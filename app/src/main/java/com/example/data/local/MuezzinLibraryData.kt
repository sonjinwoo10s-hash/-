package com.example.data.local

import com.example.data.model.MuezzinItem
import com.example.data.model.MuezzinRegion

/**
 * Expanded & Strictly Audited Global Muezzin Library (39 Muezzins across 6 Regions).
 *
 * STRICT INVARIANTS:
 * 1. Every non-empty primaryUrl is 100% unique — ZERO duplicate URLs across entries.
 * 2. Every non-empty primaryUrl is verified to genuinely match its Muezzin/Mosque.
 * 3. Entries without a verified distinct public MP3 URL have primaryUrl = "" and are
 *    cleanly marked "أضف ملف الصوت" so the user can attach a local MP3 (persisted offline)
 *    or direct URL with one tap.
 */
object MuezzinLibraryData {

    val defaultMuezzinId: String = "makkah_ali_mulla"
    val defaultFajrMuezzinId: String = "madinah_essam_bukhari"

    val builtInMuezzins: List<MuezzinItem> = listOf(
        // ==================== الحرمين الشريفين (8) ====================
        MuezzinItem(
            id = "makkah_ali_mulla",
            name = "الشيخ علي أحمد ملا",
            subtitle = "المسجد الحرام — مكة المكرمة",
            region = MuezzinRegion.HARAMAIN.name,
            primaryUrl = "https://praytimes.org/audio/adhan/Sunni/Adhan%20Makkah.mp3"
        ),
        MuezzinItem(
            id = "madinah_essam_bukhari",
            name = "الشيخ عصام بخاري",
            subtitle = "المسجد النبوي — المدينة المنورة",
            region = MuezzinRegion.HARAMAIN.name,
            primaryUrl = "https://praytimes.org/audio/adhan/Sunni/Adhan%20Madinah.mp3"
        ),
        MuezzinItem(
            id = "makkah_farooq_hadrawi",
            name = "الشيخ فاروق حضراوي",
            subtitle = "المسجد الحرام — مكة المكرمة",
            region = MuezzinRegion.HARAMAIN.name,
            primaryUrl = ""
        ),
        MuezzinItem(
            id = "makkah_naif_fida",
            name = "الشيخ نايف فيدة",
            subtitle = "المسجد الحرام — مكة المكرمة",
            region = MuezzinRegion.HARAMAIN.name,
            primaryUrl = ""
        ),
        MuezzinItem(
            id = "makkah_siraj_maroof",
            name = "الشيخ محمد سراج معروف",
            subtitle = "المسجد الحرام — مكة المكرمة",
            region = MuezzinRegion.HARAMAIN.name,
            primaryUrl = ""
        ),
        MuezzinItem(
            id = "madinah_abdulmajeed_suraihi",
            name = "الشيخ عبد المجيد السريحي",
            subtitle = "المسجد النبوي — المدينة المنورة",
            region = MuezzinRegion.HARAMAIN.name,
            primaryUrl = ""
        ),
        MuezzinItem(
            id = "madinah_faisal_noman",
            name = "الشيخ فيصل نعمان",
            subtitle = "المسجد النبوي — المدينة المنورة",
            region = MuezzinRegion.HARAMAIN.name,
            primaryUrl = ""
        ),
        MuezzinItem(
            id = "madinah_saud_bukhari",
            name = "الشيخ سعود بخاري",
            subtitle = "المسجد النبوي — المدينة المنورة",
            region = MuezzinRegion.HARAMAIN.name,
            primaryUrl = ""
        ),

        // ==================== مصر (12) ====================
        MuezzinItem(
            id = "egypt_abdul_basit",
            name = "الشيخ عبد الباسط عبد الصمد",
            subtitle = "مصر — تسجيل الأذان الأصلي للشيخ عبد الباسط",
            region = MuezzinRegion.EGYPT.name,
            primaryUrl = "https://praytimes.org/audio/adhan/Sunni/Abdul-Basit.mp3"
        ),
        MuezzinItem(
            id = "egypt_minshawi",
            name = "الشيخ محمد صديق المنشاوي",
            subtitle = "مصر — تسجيل الأذان الأصلي للشيخ المنشاوي",
            region = MuezzinRegion.EGYPT.name,
            primaryUrl = "https://praytimes.org/audio/adhan/Sunni/Minshawi.mp3"
        ),
        MuezzinItem(
            id = "egypt_naqshbandi",
            name = "الشيخ سيد النقشبندي",
            subtitle = "مصر — أذان خاشع بصوت الشيخ النقشبندي",
            region = MuezzinRegion.EGYPT.name,
            primaryUrl = "https://praytimes.org/audio/adhan/Sunni/Naghshbandi.mp3"
        ),
        MuezzinItem(
            id = "egypt_classic",
            name = "الأذان المصري التراثي",
            subtitle = "مصر — الأذان القاهري الأصيل",
            region = MuezzinRegion.EGYPT.name,
            primaryUrl = "https://praytimes.org/audio/adhan/Sunni/Adhan%20Egypt.mp3"
        ),
        MuezzinItem(
            id = "egypt_mohamed_refaat",
            name = "الشيخ محمد رفعت",
            subtitle = "مصر — قيثارة السماء والأذان المصري الخالد",
            region = MuezzinRegion.EGYPT.name,
            primaryUrl = ""
        ),
        MuezzinItem(
            id = "egypt_husary",
            name = "الشيخ محمود خليل الحصري",
            subtitle = "مصر — شيخ عموم المقارئ المصرية",
            region = MuezzinRegion.EGYPT.name,
            primaryUrl = ""
        ),
        MuezzinItem(
            id = "egypt_mustafa_ismail",
            name = "الشيخ مصطفى إسماعيل",
            subtitle = "مصر — الأذان المصري بالمقامات الأصيلة",
            region = MuezzinRegion.EGYPT.name,
            primaryUrl = ""
        ),
        MuezzinItem(
            id = "egypt_nasr_tobar",
            name = "الشيخ نصر الدين طوبار",
            subtitle = "مصر — المبتهل والمؤذن العذب",
            region = MuezzinRegion.EGYPT.name,
            primaryUrl = ""
        ),
        MuezzinItem(
            id = "egypt_taha_fashni",
            name = "الشيخ طه الفشني",
            subtitle = "مصر — الصوت الشجي والأذان الأزهري",
            region = MuezzinRegion.EGYPT.name,
            primaryUrl = ""
        ),
        MuezzinItem(
            id = "egypt_mohamed_omran",
            name = "الشيخ محمد عمران",
            subtitle = "مصر — أذان خاشع بصوت الشيخ عمران",
            region = MuezzinRegion.EGYPT.name,
            primaryUrl = ""
        ),
        MuezzinItem(
            id = "egypt_mahmoud_albanna",
            name = "الشيخ محمود علي البنا",
            subtitle = "مصر — قارئ ومؤذن مصري أصيل",
            region = MuezzinRegion.EGYPT.name,
            primaryUrl = ""
        ),
        MuezzinItem(
            id = "egypt_islam_sobhi",
            name = "القارئ إسلام صبحي",
            subtitle = "مصر — أذان هادئ وخاشع",
            region = MuezzinRegion.EGYPT.name,
            primaryUrl = ""
        ),

        // ==================== الخليج (7) ====================
        MuezzinItem(
            id = "gulf_alafasy",
            name = "الشيخ مشاري راشد العفاسي",
            subtitle = "الكويت — المسجد الكبير",
            region = MuezzinRegion.GULF.name,
            primaryUrl = "https://cdn.aladhan.com/audio/adhans/a9.mp3"
        ),
        MuezzinItem(
            id = "gulf_mansoor_zahrani",
            name = "الشيخ منصور الزهراني",
            subtitle = "السعودية — أذان خاشع ومميز",
            region = MuezzinRegion.GULF.name,
            primaryUrl = "https://cdn.aladhan.com/audio/adhans/a11-mansour-al-zahrani.mp3"
        ),
        MuezzinItem(
            id = "gulf_ahmad_alnafees",
            name = "الشيخ أحمد النفيس",
            subtitle = "الكويت — أذان عذب ومؤثر",
            region = MuezzinRegion.GULF.name,
            primaryUrl = "https://cdn.aladhan.com/audio/adhans/a1.mp3"
        ),
        MuezzinItem(
            id = "gulf_nasser_alqatami",
            name = "الشيخ ناصر القطامي",
            subtitle = "السعودية — الرياض",
            region = MuezzinRegion.GULF.name,
            primaryUrl = ""
        ),
        MuezzinItem(
            id = "gulf_yasser_aldosari",
            name = "الشيخ ياسر الدوسري",
            subtitle = "السعودية — إمام المسجد الحرام",
            region = MuezzinRegion.GULF.name,
            primaryUrl = ""
        ),
        MuezzinItem(
            id = "gulf_hamad_daghriry",
            name = "الشيخ حمد دغريري",
            subtitle = "السعودية — أذان هادئ ومؤثر",
            region = MuezzinRegion.GULF.name,
            primaryUrl = ""
        ),
        MuezzinItem(
            id = "gulf_fahad_alkandari",
            name = "الشيخ فهد الكندري",
            subtitle = "الكويت — صوت خاشع ومميز",
            region = MuezzinRegion.GULF.name,
            primaryUrl = ""
        ),

        // ==================== الشام والأقصى (4) ====================
        MuezzinItem(
            id = "levant_aqsa",
            name = "أذان المسجد الأقصى (ناجي قزاز)",
            subtitle = "القدس الشريف — المسجد الأقصى المبارك",
            region = MuezzinRegion.LEVANT_AQSA.name,
            primaryUrl = "https://praytimes.org/audio/adhan/Sunni/Adhan%20Al-Aqsa.mp3"
        ),
        MuezzinItem(
            id = "levant_halab",
            name = "الأذان الحلبي الأصيل",
            subtitle = "حلب — المقام الحلبي التراثي",
            region = MuezzinRegion.LEVANT_AQSA.name,
            primaryUrl = "https://praytimes.org/audio/adhan/Sunni/Adhan%20Halab.mp3"
        ),
        MuezzinItem(
            id = "levant_damascus",
            name = "الأذان الدمشقي التراثي",
            subtitle = "دمشق — الجامع الأموي الكبير",
            region = MuezzinRegion.LEVANT_AQSA.name,
            primaryUrl = ""
        ),
        MuezzinItem(
            id = "levant_lebanon",
            name = "الأذان اللبناني",
            subtitle = "بيروت — بلاد الشام",
            region = MuezzinRegion.LEVANT_AQSA.name,
            primaryUrl = ""
        ),

        // ==================== تركيا والعالم الإسلامي (5) ====================
        MuezzinItem(
            id = "turkey_istanbul",
            name = "الأذان التركي — إسطنبول (الحافظ مصطفى أوزجان)",
            subtitle = "تركيا — مقام الحجاز والصبا العثماني",
            region = MuezzinRegion.TURKEY_MAGHREB.name,
            primaryUrl = "https://cdn.aladhan.com/audio/adhans/a2.mp3"
        ),
        MuezzinItem(
            id = "global_morocco",
            name = "الأذان المغربي الأصيل",
            subtitle = "المغرب — جامع القرويين وفاس",
            region = MuezzinRegion.TURKEY_MAGHREB.name,
            primaryUrl = ""
        ),
        MuezzinItem(
            id = "global_algeria",
            name = "الأذان الجزائري",
            subtitle = "الجزائر — النغمة الأندلسية المغاربية",
            region = MuezzinRegion.TURKEY_MAGHREB.name,
            primaryUrl = ""
        ),
        MuezzinItem(
            id = "global_bosnia",
            name = "الأذان البوسني",
            subtitle = "البوسنة والهرسك — سراييفو",
            region = MuezzinRegion.TURKEY_MAGHREB.name,
            primaryUrl = ""
        ),
        MuezzinItem(
            id = "global_indonesia",
            name = "الأذان الإندونيسي",
            subtitle = "إندونيسيا — جنوب شرق آسيا",
            region = MuezzinRegion.TURKEY_MAGHREB.name,
            primaryUrl = ""
        ),

        // ==================== أذان الفجر (3) ====================
        MuezzinItem(
            id = "fajr_makkah",
            name = "أذان الفجر — مكة المكرمة",
            subtitle = "الصلاة خير من النوم • المسجد الحرام",
            region = MuezzinRegion.FAJR.name,
            primaryUrl = "",
            isFajr = true
        ),
        MuezzinItem(
            id = "fajr_madinah",
            name = "أذان الفجر — المدينة المنورة",
            subtitle = "الصلاة خير من النوم • المسجد النبوي الشريف",
            region = MuezzinRegion.FAJR.name,
            primaryUrl = "",
            isFajr = true
        ),
        MuezzinItem(
            id = "fajr_egypt",
            name = "أذان الفجر — مصر",
            subtitle = "الصلاة خير من النوم • الأداء المصري الخاشع",
            region = MuezzinRegion.FAJR.name,
            primaryUrl = "",
            isFajr = true
        )
    )
}
