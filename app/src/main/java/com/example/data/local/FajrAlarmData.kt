package com.example.data.local

import com.example.data.model.MathChallengeProblem
import com.example.data.model.MathDifficulty
import com.example.data.model.TriviaChallengeQuestion
import kotlin.random.Random

object FajrAlarmData {

    val quickOffsetPresets: List<Int> = listOf(10, 15, 30, 45, 60)

    fun generateMathProblem(difficulty: MathDifficulty): MathChallengeProblem {
        return when (difficulty) {
            MathDifficulty.EASY -> {
                val isAdd = Random.nextBoolean()
                if (isAdd) {
                    val a = Random.nextInt(12, 55)
                    val b = Random.nextInt(11, 45)
                    MathChallengeProblem("$a + $b = ?", a + b)
                } else {
                    val a = Random.nextInt(35, 95)
                    val b = Random.nextInt(11, a - 5)
                    MathChallengeProblem("$a - $b = ?", a - b)
                }
            }
            MathDifficulty.MEDIUM -> {
                when (Random.nextInt(3)) {
                    0 -> {
                        val a = Random.nextInt(6, 13)
                        val b = Random.nextInt(4, 10)
                        val c = Random.nextInt(5, 22)
                        MathChallengeProblem("$a × $b - $c = ?", a * b - c)
                    }
                    1 -> {
                        val a = Random.nextInt(5, 12)
                        val b = Random.nextInt(5, 11)
                        val c = Random.nextInt(8, 28)
                        MathChallengeProblem("$a × $b + $c = ?", a * b + c)
                    }
                    else -> {
                        val a = Random.nextInt(45, 130)
                        val b = Random.nextInt(25, 85)
                        MathChallengeProblem("$a + $b = ?", a + b)
                    }
                }
            }
            MathDifficulty.HARD -> {
                when (Random.nextInt(3)) {
                    0 -> {
                        val a = Random.nextInt(11, 19)
                        val b = Random.nextInt(6, 12)
                        val c = Random.nextInt(15, 45)
                        MathChallengeProblem("$a × $b - $c = ?", a * b - c)
                    }
                    1 -> {
                        val divisor = Random.nextInt(4, 10)
                        val quotient = Random.nextInt(6, 14)
                        val dividend = divisor * quotient
                        val mult = Random.nextInt(4, 9)
                        val add = Random.nextInt(7, 25)
                        MathChallengeProblem("($dividend ÷ $divisor) × $mult + $add = ?", quotient * mult + add)
                    }
                    else -> {
                        val a = Random.nextInt(12, 24)
                        val b = Random.nextInt(5, 11)
                        val c = Random.nextInt(18, 49)
                        MathChallengeProblem("$a × $b + $c = ?", a * b + c)
                    }
                }
            }
        }
    }

    val triviaQuestions: List<TriviaChallengeQuestion> = listOf(
        // Islamic History
        TriviaChallengeQuestion(
            1, "تاريخ إسلامي",
            "في أي سنة هجرية وقعت غزوة بدر الكبرى؟",
            listOf("السنة الأولى للهجرة", "السنة الثانية للهجرة", "السنة الثالثة للهجرة", "السنة الخامسة للهجرة"),
            1
        ),
        TriviaChallengeQuestion(
            2, "تاريخ إسلامي",
            "من هو الصحابي الذي لُقّب بـ «سيف الله المسلول»؟",
            listOf("عمر بن الخطاب", "خالد بن الوليد", "علي بن أبي طالب", "سعد بن أبي وقاص"),
            1
        ),
        TriviaChallengeQuestion(
            3, "تاريخ إسلامي",
            "في أي عام هجري تم فتح مكة المكرمة؟",
            listOf("العام السادس", "العام السابع", "العام الثامن", "العام العاشر"),
            2
        ),
        TriviaChallengeQuestion(
            4, "تاريخ إسلامي",
            "من هو أول خليفة في الإسلام جمع القرآن الكريم في مصحف واحد؟",
            listOf("أبو بكر الصديق رضي الله عنه", "عمر بن الخطاب رضي الله عنه", "عثمان بن عفان رضي الله عنه", "علي بن أبي طالب رضي الله عنه"),
            0
        ),
        TriviaChallengeQuestion(
            5, "تاريخ إسلامي",
            "من هو القائد المسلم الذي حرر بيت المقدس في معركة حطين؟",
            listOf("طارق بن زياد", "سيف الدين قطز", "صلاح الدين الأيوبي", "الظاهر بيبرس"),
            2
        ),
        TriviaChallengeQuestion(
            6, "تاريخ إسلامي",
            "من هو الصحابي الجليل الذي فُتحت مصر على يديه في عهد عمر بن الخطاب؟",
            listOf("عمرو بن العاص", "أبو عبيدة بن الجراح", "معاذ بن جبل", "الزبير بن العوام"),
            0
        ),
        TriviaChallengeQuestion(
            7, "تاريخ إسلامي",
            "ما هي أول سورة نزلت كاملة في القرآن الكريم؟",
            listOf("سورة العلق", "سورة الفاتحة", "سورة الإخلاص", "سورة الكوثر"),
            1
        ),
        TriviaChallengeQuestion(
            8, "تاريخ إسلامي",
            "كم عدد ركعات صلاة الفجر الفرض؟",
            listOf("ركعتان", "ثلاث ركعات", "أربع ركعات", "ركعة واحدة"),
            0
        ),
        TriviaChallengeQuestion(
            9, "تاريخ إسلامي",
            "من هو أول مؤذن في الإسلام؟",
            listOf("عبد الله بن أم مكتوم", "بلال بن رباح رضي الله عنه", "سلمان الفارسي", "زيد بن حارثة"),
            1
        ),
        TriviaChallengeQuestion(
            10, "تاريخ إسلامي",
            "في أي معركة انتصر المسلمون بقيادة قطز على التتار؟",
            listOf("معركة القادسية", "معركة اليرموك", "معركة عين جالوت", "معركة ملاذكرد"),
            2
        ),

        // Egyptian & General History
        TriviaChallengeQuestion(
            11, "تاريخ مصري وعام",
            "في أي عام بُني الجامع الأزهر الشريف في القاهرة؟",
            listOf("970 ميلادياً", "850 ميلادياً", "1171 ميلادياً", "642 ميلادياً"),
            0
        ),
        TriviaChallengeQuestion(
            12, "تاريخ مصري وعام",
            "من هو القائد الذي بنى مدينة القاهرة والجامع الأزهر؟",
            listOf("أحمد بن طولون", "جوهر الصقلي", "محمد علي باشا", "عمرو بن العاص"),
            1
        ),
        TriviaChallengeQuestion(
            13, "تاريخ مصري وعام",
            "في أي يوم وتاريخ اندلعت حرب السادس من أكتوبر (العاشر من رمضان)؟",
            listOf("6 أكتوبر 1973", "23 يوليو 1952", "5 يونيو 1967", "15 مايو 1971"),
            0
        ),
        TriviaChallengeQuestion(
            14, "تاريخ مصري وعام",
            "ما هو أول مسجد بُني في مصر وإفريقيا كلها؟",
            listOf("مسجد أحمد بن طولون", "الجامع الأزهر", "مسجد عمرو بن العاص", "مسجد السلطان حسن"),
            2
        ),
        TriviaChallengeQuestion(
            15, "تاريخ مصري وعام",
            "في أي مدينة مصرية يقع معبد الكرنك الشهير؟",
            listOf("أسوان", "الأقصر", "الإسكندرية", "المنيا"),
            1
        ),
        TriviaChallengeQuestion(
            16, "تاريخ مصري وعام",
            "متى تم افتتاح قناة السويس للملاحة العالمية لأول مرة؟",
            listOf("1856", "1869", "1882", "1901"),
            1
        ),
        TriviaChallengeQuestion(
            17, "تاريخ مصري وعام",
            "من هو العالم العربي المسلم مؤسس علم الجبر؟",
            listOf("ابن سينا", "الخوارزمي", "ابن الهيثم", "جابر بن حيان"),
            1
        ),
        TriviaChallengeQuestion(
            18, "تاريخ مصري وعام",
            "من هو العالم المسلم الذي وضع أسس علم البصريات وكتاب «المناظر»؟",
            listOf("الحسن بن الهيثم", "الفارابي", "البيروني", "الإدريسي"),
            0
        ),

        // Quick Math & Logic
        TriviaChallengeQuestion(
            19, "رياضيات ومنطق",
            "ما ناتج العملية: (9 × 8) − 12 ؟",
            listOf("58", "60", "64", "72"),
            1
        ),
        TriviaChallengeQuestion(
            20, "رياضيات ومنطق",
            "إذا كانت الساعة الآن 4:35 صباحاً، فما الوقت بعد 40 دقيقة؟",
            listOf("5:05 صباحاً", "5:15 صباحاً", "5:25 صباحاً", "4:55 صباحاً"),
            1
        ),
        TriviaChallengeQuestion(
            21, "رياضيات ومنطق",
            "ما هو العدد التالي في المتتالية: 3 ، 6 ، 12 ، 24 ، ... ؟",
            listOf("36", "42", "48", "54"),
            2
        ),
        TriviaChallengeQuestion(
            22, "رياضيات ومنطق",
            "كم دقيقة توجد في ساعتين وربع؟",
            listOf("125 دقيقة", "130 دقيقة", "135 دقيقة", "145 دقيقة"),
            2
        ),
        TriviaChallengeQuestion(
            23, "رياضيات ومنطق",
            "ما ناتج: (64 ÷ 8) + 17 ؟",
            listOf("23", "25", "27", "29"),
            1
        ),
        TriviaChallengeQuestion(
            24, "رياضيات ومنطق",
            "مربع طول ضلعه 7 سم، كم تبلغ مساحته؟",
            listOf("28 سم²", "42 سم²", "49 سم²", "56 سم²"),
            2
        ),
        TriviaChallengeQuestion(
            25, "رياضيات ومنطق",
            "ما ناتج: 15 × 6 − 25 ؟",
            listOf("55", "65", "75", "85"),
            1
        ),
        TriviaChallengeQuestion(
            26, "رياضيات ومنطق",
            "إذا كان ثلث عدد ما يساوي 14، فما هو هذا العدد؟",
            listOf("28", "36", "42", "48"),
            2
        )
    )
}
