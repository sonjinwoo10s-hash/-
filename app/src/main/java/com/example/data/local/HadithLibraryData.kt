package com.example.data.local

import com.example.data.model.HadithBookCollection
import com.example.data.model.HadithChapters
import com.example.data.model.LibraryHadith

object HadithLibraryData {

    val bukhariOffline: List<LibraryHadith> = listOf(
        LibraryHadith(
            uid = "bukhari_1",
            book = HadithBookCollection.BUKHARI,
            number = 1,
            chapter = HadithChapters.IMAN,
            title = "الأعمال بالنيات",
            narrator = "عن عمر بن الخطاب رضي الله عنه",
            text = "إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ، وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى، فَمَنْ كَانَتْ هِجْرَتُهُ إِلَى دُنْيَا يُصِيبُهَا أَوْ إِلَى امْرَأَةٍ يَنْكِحُهَا، فَهِجْرَتُهُ إِلَى مَا هَاجَرَ إِلَيْهِ.",
            explanation = "النية الصالحة شرط لقبول العمل الصالح ومضاعفة أجره، وبها تتمايز العبادات عن العادات.",
            source = "صحيح البخاري • حديث رقم 1"
        ),
        LibraryHadith(
            uid = "bukhari_8",
            book = HadithBookCollection.BUKHARI,
            number = 8,
            chapter = HadithChapters.IMAN,
            title = "بُني الإسلام على خمس",
            narrator = "عن عبد الله بن عمر رضي الله عنهما",
            text = "بُنِيَ الإِسْلَامُ عَلَى خَمْسٍ: شَهَادَةِ أَنْ لَا إِلَهَ إِلَّا اللَّهُ وَأَنَّ مُحَمَّدًا رَسُولُ اللَّهِ، وَإِقَامِ الصَّلَاةِ، وَإِيتَاءِ الزَّكَاةِ، وَالحَجِّ، وَصَوْمِ رَمَضَانَ.",
            explanation = "بيان الأركان الخمسة التي يقوم عليها بنيان الإسلام العظيم.",
            source = "صحيح البخاري • حديث رقم 8"
        ),
        LibraryHadith(
            uid = "bukhari_13",
            book = HadithBookCollection.BUKHARI,
            number = 13,
            chapter = HadithChapters.IMAN,
            title = "كمال الإيمان بمحبة الخير للناس",
            narrator = "عن أنس بن مالك رضي الله عنه",
            text = "لَا يُؤْمِنُ أَحَدُكُمْ حَتَّى يُحِبَّ لِأَخِيهِ مَا يُحِبُّ لِنَفْسِهِ.",
            explanation = "من علامات الإيمان الصادق سلامة الصدر ومحبة الخير لإخوانك المسلمين.",
            source = "صحيح البخاري • حديث رقم 13"
        ),
        LibraryHadith(
            uid = "bukhari_527",
            book = HadithBookCollection.BUKHARI,
            number = 527,
            chapter = HadithChapters.SALAH,
            title = "أحب الأعمال إلى الله الصلاة على وقتها",
            narrator = "عن عبد الله بن مسعود رضي الله عنه",
            text = "سَأَلْتُ النَّبِيَّ ﷺ: أَيُّ العَمَلِ أَحَبُّ إِلَى اللَّهِ؟ قَالَ: «الصَّلَاةُ عَلَى وَقْتِهَا». قَالَ: ثُمَّ أَيٌّ؟ قَالَ: «ثُمَّ بِرُّ الوَالِدَيْنِ». قَالَ: ثُمَّ أَيٌّ؟ قَالَ: «الجِهَادُ فِي سَبِيلِ اللَّهِ».",
            explanation = "تقديم الصلاة في أول وقتها من أعظم القربات إلى الله، ويليها بر الوالدين.",
            source = "صحيح البخاري • حديث رقم 527"
        ),
        LibraryHadith(
            uid = "bukhari_574",
            book = HadithBookCollection.BUKHARI,
            number = 574,
            chapter = HadithChapters.SALAH,
            title = "فضل صلاة البردين (الفجر والعصر)",
            narrator = "عن أبي موسى الأشعري رضي الله عنه",
            text = "مَنْ صَلَّى البَرْدَيْنِ دَخَلَ الجَنَّةَ.",
            explanation = "البردان هما الفجر والعصر؛ لوقوعهما في وقتي برد النهار، والمحافظة عليهما سبب لدخول الجنة.",
            source = "صحيح البخاري • حديث رقم 574"
        ),
        LibraryHadith(
            uid = "bukhari_645",
            book = HadithBookCollection.BUKHARI,
            number = 645,
            chapter = HadithChapters.SALAH,
            title = "فضل صلاة الجماعة",
            narrator = "عن عبد الله بن عمر رضي الله عنهما",
            text = "صَلَاةُ الجَمَاعَةِ تَفْضُلُ صَلَاةَ الفَذِّ بِسَبْعٍ وَعِشْرِينَ دَرَجَةً.",
            explanation = "الترغيب في شهود الصلاة مع الجماعة ومضاعفة أجرها سبعاً وعشرين درجة.",
            source = "صحيح البخاري • حديث رقم 645"
        ),
        LibraryHadith(
            uid = "bukhari_1894",
            book = HadithBookCollection.BUKHARI,
            number = 1894,
            chapter = HadithChapters.SIYAM,
            title = "الصيام جُنّة وحصن",
            narrator = "عن أبي هريرة رضي الله عنه",
            text = "الصِّيَامُ جُنَّةٌ فَلَا يَرْفُثْ وَلَا يَجْهَلْ، وَإِنِ امْرُؤٌ قَاتَلَهُ أَوْ شَاتَمَهُ فَلْيَقُلْ: إِنِّي صَائِمٌ، مَرَّتَيْنِ.",
            explanation = "الصيام وقاية من الذنوب ومن النار، ويهذّب اللسان والسلوك عند الغضب.",
            source = "صحيح البخاري • حديث رقم 1894"
        ),
        LibraryHadith(
            uid = "bukhari_1896",
            book = HadithBookCollection.BUKHARI,
            number = 1896,
            chapter = HadithChapters.SIYAM,
            title = "باب الريان للصائمين",
            narrator = "عن سهل بن سعد رضي الله عنه",
            text = "إِنَّ فِي الجَنَّةِ بَابًا يُقَالُ لَهُ الرَّيَّانُ، يَدْخُلُ مِنْهُ الصَّائِمُونَ يَوْمَ القِيَامَةِ، لَا يَدْخُلُ مِنْهُ أَحَدٌ غَيْرُهُمْ.",
            explanation = "إكرام الله لأهل الصيام بباب خاص في الجنة جزاء صبرهم على العطش والجوع.",
            source = "صحيح البخاري • حديث رقم 1896"
        ),
        LibraryHadith(
            uid = "bukhari_5027",
            book = HadithBookCollection.BUKHARI,
            number = 5027,
            chapter = HadithChapters.ZIKR,
            title = "خيركم من تعلم القرآن وعلمه",
            narrator = "عن عثمان بن عفان رضي الله عنه",
            text = "خَيْرُكُمْ مَنْ تَعَلَّمَ القُرْآنَ وَعَلَّمَهُ.",
            explanation = "أشرف الناس منزلة عند الله من يجمع بين تعلم كلام الله وتعليمه للناس.",
            source = "صحيح البخاري • حديث رقم 5027"
        ),
        LibraryHadith(
            uid = "bukhari_6406",
            book = HadithBookCollection.BUKHARI,
            number = 6406,
            chapter = HadithChapters.ZIKR,
            title = "كلمتان حبيبتان إلى الرحمن",
            narrator = "عن أبي هريرة رضي الله عنه",
            text = "كَلِمَتَانِ خَفِيفَتَانِ عَلَى اللِّسَانِ، ثَقِيلَتَانِ فِي المِيزَانِ، حَبِيبَتَانِ إِلَى الرَّحْمَنِ: سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، سُبْحَانَ اللَّهِ العَظِيمِ.",
            explanation = "فضل المداومة على التسبيح والتحميد وعظيم ثقلهما في ميزان الحسنات.",
            source = "صحيح البخاري • حديث رقم 6406"
        ),
        LibraryHadith(
            uid = "bukhari_6306",
            book = HadithBookCollection.BUKHARI,
            number = 6306,
            chapter = HadithChapters.TAWBAH,
            title = "سيد الاستغفار",
            narrator = "عن شداد بن أوس رضي الله عنه",
            text = "سَيِّدُ الِاسْتِغْفَارِ أَنْ تَقُولَ: اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ بِذَنْبِي فَاغْفِرْ لِي فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ.",
            explanation = "من قالها من النهار موقناً بها فمات من يومه فهو من أهل الجنة، وكذلك في المساء.",
            source = "صحيح البخاري • حديث رقم 6306"
        ),
        LibraryHadith(
            uid = "bukhari_6094",
            book = HadithBookCollection.BUKHARI,
            number = 6094,
            chapter = HadithChapters.AKHLAQ,
            title = "الصدق يهدي إلى البر",
            narrator = "عن عبد الله بن مسعود رضي الله عنه",
            text = "إِنَّ الصِّدْقَ يَهْدِي إِلَى البِرِّ، وَإِنَّ البِرَّ يَهْدِي إِلَى الجَنَّةِ، وَإِنَّ الرَّجُلَ لَيَصْدُقُ حَتَّى يَكُونَ صِدِّيقًا، وَإِنَّ الكَذِبَ يَهْدِي إِلَى الفُجُورِ، وَإِنَّ الفُجُورَ يَهْدِي إِلَى النَّارِ.",
            explanation = "الصدق مفتاح كل خير وموصل إلى الجنة، والكذب بوابة الشرور.",
            source = "صحيح البخاري • حديث رقم 6094"
        ),
        LibraryHadith(
            uid = "bukhari_6116",
            book = HadithBookCollection.BUKHARI,
            number = 6116,
            chapter = HadithChapters.AKHLAQ,
            title = "الوصية النبوية: لا تغضب",
            narrator = "عن أبي هريرة رضي الله عنه",
            text = "أَنَّ رَجُلًا قَالَ لِلنَّبِيِّ ﷺ: أَوْصِنِي، قَالَ: «لَا تَغْضَبْ». فَرَدَّدَ مِرَارًا، قَالَ: «لَا تَغْضَبْ».",
            explanation = "ضبط النفس عند الغضب يحفظ الإنسان من الزلل ويجمع له خير الدنيا والآخرة.",
            source = "صحيح البخاري • حديث رقم 6116"
        ),
        LibraryHadith(
            uid = "bukhari_5971",
            book = HadithBookCollection.BUKHARI,
            number = 5971,
            chapter = HadithChapters.AKHLAQ,
            title = "أحق الناس بحسن الصحبة",
            narrator = "عن أبي هريرة رضي الله عنه",
            text = "جَاءَ رَجُلٌ إِلَى رَسُولِ اللَّهِ ﷺ فَقَالَ: يَا رَسُولَ اللَّهِ، مَنْ أَحَقُّ النَّاسِ بِحُسْنِ صَحَابَتِي؟ قَالَ: «أُمُّكَ». قَالَ: ثُمَّ مَنْ؟ قَالَ: «ثُمَّ أُمُّكَ». قَالَ: ثُمَّ مَنْ؟ قَالَ: «ثُمَّ أُمُّكَ». قَالَ: ثُمَّ مَنْ؟ قَالَ: «ثُمَّ أَبُوكَ».",
            explanation = "عِظم حق الأم وتأكيده ثلاث مرات ثم حق الأب.",
            source = "صحيح البخاري • حديث رقم 5971"
        )
    )

    val muslimOffline: List<LibraryHadith> = listOf(
        LibraryHadith(
            uid = "muslim_223",
            book = HadithBookCollection.MUSLIM,
            number = 223,
            chapter = HadithChapters.SALAH,
            title = "الطهور شطر الإيمان",
            narrator = "عن أبي مالك الأشعري رضي الله عنه",
            text = "الطُّهُورُ شَطْرُ الإِيمَانِ، وَالحَمْدُ لِلَّهِ تَمْلَأُ المِيزَانَ، وَسُبْحَانَ اللَّهِ وَالحَمْدُ لِلَّهِ تَمْلَآَنِ مَا بَيْنَ السَّمَاوَاتِ وَالأَرْضِ، وَالصَّلَاةُ نُورٌ، وَالصَّدَقَةُ بُرْهَانٌ، وَالصَّبْرُ ضِيَاءٌ، وَالقُرْآنُ حُجَّةٌ لَكَ أَوْ عَلَيْكَ.",
            explanation = "حديث جامع لفضائل الطهارة والذكر والصلاة والصدقة والصبر وتلاوة القرآن.",
            source = "صحيح مسلم • حديث رقم 223"
        ),
        LibraryHadith(
            uid = "muslim_2999",
            book = HadithBookCollection.MUSLIM,
            number = 2999,
            chapter = HadithChapters.IMAN,
            title = "عجباً لأمر المؤمن",
            narrator = "عن صهيب الرومي رضي الله عنه",
            text = "عَجَبًا لِأَمْرِ المُؤْمِنِ، إِنَّ أَمْرَهُ كُلَّهُ خَيْرٌ، وَلَيْسَ ذَاكَ لِأَحَدٍ إِلَّا لِلْمُؤْمِنِ، إِنْ أَصَابَتْهُ سَرَّاءُ شَكَرَ فَكَانَ خَيْرًا لَهُ، وَإِنْ أَصَابَتْهُ ضَرَّاءُ صَبَرَ فَكَانَ خَيْرًا لَهُ.",
            explanation = "المؤمن يرتقي دائماً بين مقامي الشكر عند النعماء والصبر عند البلاء.",
            source = "صحيح مسلم • حديث رقم 2999"
        ),
        LibraryHadith(
            uid = "muslim_2699",
            book = HadithBookCollection.MUSLIM,
            number = 2699,
            chapter = HadithChapters.AKHLAQ,
            title = "تفريج الكرب وطلب العلم",
            narrator = "عن أبي هريرة رضي الله عنه",
            text = "مَنْ نَفَّسَ عَنْ مُؤْمِنٍ كُرْبَةً مِنْ كُرَبِ الدُّنْيَا نَفَّسَ اللَّهُ عَنْهُ كُرْبَةً مِنْ كُرَبِ يَوْمِ القِيَامَةِ، وَمَنْ يَسَّرَ عَلَى مُعْسِرٍ يَسَّرَ اللَّهُ عَلَيْهِ فِي الدُّنْيَا وَالآخِرَةِ، وَاللَّهُ فِي عَوْنِ العَبْدِ مَا كَانَ العَبْدُ فِي عَوْنِ أَخِيهِ، وَمَنْ سَلَكَ طَرِيقًا يَلْتَمِسُ فِيهِ عِلْمًا سَهَّلَ اللَّهُ لَهُ بِهِ طَرِيقًا إِلَى الجَنَّةِ.",
            explanation = "الجزاء من جنس العمل؛ فإعانة الناس وتيسير أمورهم وتعلّم العلم من أعظم أسباب التوفيق.",
            source = "صحيح مسلم • حديث رقم 2699"
        ),
        LibraryHadith(
            uid = "muslim_657",
            book = HadithBookCollection.MUSLIM,
            number = 657,
            chapter = HadithChapters.SALAH,
            title = "من صلى الصبح فهو في ذمة الله",
            narrator = "عن جندب بن عبد الله رضي الله عنه",
            text = "مَنْ صَلَّى الصُّبْحَ فَهُوَ فِي ذِمَّةِ اللَّهِ، فَلَا يَطْلُبَنَّكُمُ اللَّهُ مِنْ ذِمَّتِهِ بِشَيْءٍ.",
            explanation = "المحافظة على صلاة الفجر تجعل العبد في حفظ الله وأمانه ورعايته طوال يومه.",
            source = "صحيح مسلم • حديث رقم 657"
        ),
        LibraryHadith(
            uid = "muslim_725",
            book = HadithBookCollection.MUSLIM,
            number = 725,
            chapter = HadithChapters.SALAH,
            title = "ركعتا الفجر خير من الدنيا",
            narrator = "عن عائشة رضي الله عنها",
            text = "رَكْعَتَا الفَجْرِ خَيْرٌ مِنَ الدُّنْيَا وَمَا فِيهَا.",
            explanation = "عظيم فضل سنة الفجر القبلية وما أعدّه الله لمن يحافظ عليها.",
            source = "صحيح مسلم • حديث رقم 725"
        ),
        LibraryHadith(
            uid = "muslim_2747",
            book = HadithBookCollection.MUSLIM,
            number = 2747,
            chapter = HadithChapters.TAWBAH,
            title = "فرح الله بتوبة عبده",
            narrator = "عن أنس بن مالك رضي الله عنه",
            text = "لَلَّهُ أَشَدُّ فَرَحًا بِتَوْبَةِ عَبْدِهِ حِينَ يَتُوبُ إِلَيْهِ مِنْ أَحَدِكُمْ كَانَ عَلَى رَاحِلَتِهِ بِأَرْضِ فَلَاةٍ، فَانْفَلَتَتْ مِنْهُ وَعَلَيْهَا طَعَامُهُ وَشَرَابُهُ فَأَيِسَ مِنْهَا، ثُمَّ وَجَدَهَا قَائِمَةً عِنْدَهُ.",
            explanation = "بيان سعة رحمة الله وكرمه وإقباله على التائبين مهما عظمت ذنوبهم.",
            source = "صحيح مسلم • حديث رقم 2747"
        ),
        LibraryHadith(
            uid = "muslim_2759",
            book = HadithBookCollection.MUSLIM,
            number = 2759,
            chapter = HadithChapters.TAWBAH,
            title = "بسط يد الله بالتوبة ليلاً ونهاراً",
            narrator = "عن أبي موسى الأشعري رضي الله عنه",
            text = "إِنَّ اللَّهَ عَزَّ وَجَلَّ يَبْسُطُ يَدَهُ بِاللَّيْلِ لِيَتُوبَ مُسِيءُ النَّهَارِ، وَيَبْسُطُ يَدَهُ بِالنَّهَارِ لِيَتُوبَ مُسِيءُ اللَّيْلِ، حَتَّى تَطْلُعَ الشَّمْسُ مِنْ مَغْرِبِهَا.",
            explanation = "باب التوبة مفتوح على الدوام في الليل والنهار لمن أقبل بقلب صادق.",
            source = "صحيح مسلم • حديث رقم 2759"
        ),
        LibraryHadith(
            uid = "muslim_2692",
            book = HadithBookCollection.MUSLIM,
            number = 2692,
            chapter = HadithChapters.ZIKR,
            title = "أحب الكلام إلى الله",
            narrator = "عن سمرة بن جندب رضي الله عنه",
            text = "أَحَبُّ الكَلَامِ إِلَى اللَّهِ أَرْبَعٌ: سُبْحَانَ اللَّهِ، وَالحَمْدُ لِلَّهِ، وَلَا إِلَهَ إِلَّا اللَّهُ، وَاللَّهُ أَكْبَرُ، لَا يَضُرُّكَ بِأَيِّهِنَّ بَدَأْتَ.",
            explanation = "الباقيات الصالحات من أحب الأذكار إلى الله وأكثرها أجراً.",
            source = "صحيح مسلم • حديث رقم 2692"
        ),
        LibraryHadith(
            uid = "muslim_2702",
            book = HadithBookCollection.MUSLIM,
            number = 2702,
            chapter = HadithChapters.ZIKR,
            title = "استغفار النبي ﷺ كل يوم",
            narrator = "عن الأغر المزني رضي الله عنه",
            text = "يَا أَيُّهَا النَّاسُ تُوبُوا إِلَى اللَّهِ، فَإِنِّي أَتُوبُ فِي اليَوْمِ إِلَيْهِ مِائَةَ مَرَّةٍ.",
            explanation = "الاقتداء بالنبي ﷺ في الإكثار من الاستغفار والتوبة يومياً.",
            source = "صحيح مسلم • حديث رقم 2702"
        ),
        LibraryHadith(
            uid = "muslim_1164",
            book = HadithBookCollection.MUSLIM,
            number = 1164,
            chapter = HadithChapters.SIYAM,
            title = "صيام ست من شوال",
            narrator = "عن أبي أيوب الأنصاري رضي الله عنه",
            text = "مَنْ صَامَ رَمَضَانَ ثُمَّ أَتْبَعَهُ سِتًّا مِنْ شَوَّالٍ، كَانَ كَصِيَامِ الدَّهْرِ.",
            explanation = "الحسنة بعشر أمثالها، فرمضان بعشرة أشهر وست شوال بشهرين، فذلك صيام العام كله.",
            source = "صحيح مسلم • حديث رقم 1164"
        ),
        LibraryHadith(
            uid = "muslim_1162",
            book = HadithBookCollection.MUSLIM,
            number = 1162,
            chapter = HadithChapters.SIYAM,
            title = "فضل صيام يوم عرفة وعاشوراء",
            narrator = "عن أبي قتادة رضي الله عنه",
            text = "صِيَامُ يَوْمِ عَرَفَةَ أَحْتَسِبُ عَلَى اللَّهِ أَنْ يُكَفِّرَ السَّنَةَ الَّتِي قَبْلَهُ وَالسَّنَةَ الَّتِي بَعْدَهُ، وَصِيَامُ يَوْمِ عَاشُورَاءَ أَحْتَسِبُ عَلَى اللَّهِ أَنْ يُكَفِّرَ السَّنَةَ الَّتِي قَبْلَهُ.",
            explanation = "من أعظم مواسم الصيام التطوعي وتكفير الذنوب.",
            source = "صحيح مسلم • حديث رقم 1162"
        ),
        LibraryHadith(
            uid = "muslim_2588",
            book = HadithBookCollection.MUSLIM,
            number = 2588,
            chapter = HadithChapters.AKHLAQ,
            title = "ما نقصت صدقة من مال",
            narrator = "عن أبي هريرة رضي الله عنه",
            text = "مَا نَقَصَتْ صَدَقَةٌ مِنْ مَالٍ، وَمَا زَادَ اللَّهُ عَبْدًا بِعَفْوٍ إِلَّا عِزًّا، وَمَا تَوَاضَعَ أَحَدٌ لِلَّهِ إِلَّا رَفَعَهُ اللَّهُ.",
            explanation = "الصدقة تبارك المال، والعفو يزيد صاحبه عزاً، والتواضع يرفع قدره.",
            source = "صحيح مسلم • حديث رقم 2588"
        ),
        LibraryHadith(
            uid = "muslim_2564",
            book = HadithBookCollection.MUSLIM,
            number = 2564,
            chapter = HadithChapters.AKHLAQ,
            title = "الله ينظر إلى القلوب والأعمال",
            narrator = "عن أبي هريرة رضي الله عنه",
            text = "إِنَّ اللَّهَ لَا يَنْظُرُ إِلَى صُوَرِكُمْ وَأَمْوَالِكُمْ، وَلَكِنْ يَنْظُرُ إِلَى قُلُوبِكُمْ وَأَعْمَالِكُمْ.",
            explanation = "ميزان التفاضل عند الله هو صلاح القلوب وإخلاص الأعمال الصالحة.",
            source = "صحيح مسلم • حديث رقم 2564"
        )
    )

    fun nawawiOffline(): List<LibraryHadith> {
        return OfflineIslamicData.hadiths.map { h ->
            val mappedChapter = when (h.category) {
                "الإيمان" -> HadithChapters.IMAN
                "العبادات" -> if (h.text.contains("صام") || h.text.contains("رمضان")) HadithChapters.SIYAM else HadithChapters.SALAH
                "الأخلاق" -> HadithChapters.AKHLAQ
                "الآداب" -> HadithChapters.AKHLAQ
                else -> HadithChapters.classifyChapter(h.text, h.title)
            }
            LibraryHadith(
                uid = "nawawi_${h.id}",
                book = HadithBookCollection.NAWAWI,
                number = h.id,
                chapter = mappedChapter,
                title = h.title,
                narrator = h.narrator,
                text = h.text,
                explanation = h.explanation,
                source = "الأربعون النووية • حديث رقم ${h.id}"
            )
        }
    }

    fun offlineForBook(book: HadithBookCollection): List<LibraryHadith> = when (book) {
        HadithBookCollection.NAWAWI -> nawawiOffline()
        HadithBookCollection.BUKHARI -> bukhariOffline
        HadithBookCollection.MUSLIM -> muslimOffline
        else -> emptyList()
    }
}
