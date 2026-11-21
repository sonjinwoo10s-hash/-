package com.example.quran

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle

enum class TajweedRule(
    val arabicTitle: String,
    val badgeLabel: String,
    val lightColor: Color,
    val darkColor: Color,
    val pronunciationGuide: String,
    val involvedLetters: String,
    val quranExample: String,
    val exampleReference: String
) {
    MADD(
        arabicTitle = "المدود (المد المتصل والمنفصل واللازم)",
        badgeLabel = "٤ – ٦ حركات",
        lightColor = Color(0xFFC62828),
        darkColor = Color(0xFFEF5350),
        pronunciationGuide = "إطالة الصوت بحرف المد (الألف أو الواو أو الياء) عند وجود علامة المد (~) أو الهمزة أو السكون بمقدار ٤ إلى ٦ حركات بسلاسة دون انقطاع.",
        involvedLetters = "ا  •  و  •  ي  •  علامة المد (ٓ) والألف الخنجرية (ٰ)",
        quranExample = "وَلَا ٱلضَّآلِّينَ  •  إِنَّآ أَعْطَيْنَٰكَ ٱلْكَوْثَرَ",
        exampleReference = "الفاتحة ٧ • الكوثر ١"
    ),
    GHUNNAH(
        arabicTitle = "الغنة والإدغام بغنة",
        badgeLabel = "حركتان من الخيشوم",
        lightColor = Color(0xFF2E7D32),
        darkColor = Color(0xFF66BB6A),
        pronunciationGuide = "صوت رخيم يخرج من الخيشوم (أعلى الأنف) بمقدار حركتين عند النون والميم المشددتين (نّ، مّ)، وعند إدغام النون الساكنة أو التنوين في حروف (ينمو)، أو إدغام الميم الساكنة في مثلها.",
        involvedLetters = "نّ  •  مّ  •  وحروف الإدغام بغنة: ي  ن  م  و",
        quranExample = "إِنَّ ٱللَّهَ  •  فَمَن يَعْمَلْ  •  عَمَّ يَتَسَآءَلُونَ",
        exampleReference = "الزلزلة ٧ • النبأ ١"
    ),
    IKHFA(
        arabicTitle = "الإخفاء (الحقيقي والشفوي)",
        badgeLabel = "حالة وسط بغنة",
        lightColor = Color(0xFFEF6C00),
        darkColor = Color(0xFFFFA726),
        pronunciationGuide = "نطق النون الساكنة أو التنوين (أو الميم الساكنة قبل الباء) بحالة وسط بين الإظهار والإدغام مع بقاء غنة واضحة بمقدار حركتين وتهيئة اللسان لمخرج الحرف التالي دون إلصاقه.",
        involvedLetters = "ت  ث  ج  د  ذ  ز  س  ش  ص  ض  ط  ظ  ف  ق  ك  (والميم قبل ب)",
        quranExample = "مِن شَرِّ  •  أَنزَلْنَٰهُ  •  تَرْمِيهِم بِحِجَارَةٍ",
        exampleReference = "الفلق ٢ • القدر ١ • الفيل ٤"
    ),
    QALQALAH(
        arabicTitle = "القلقلة",
        badgeLabel = "اهتزاز خفيف للمخرج",
        lightColor = Color(0xFF1565C0),
        darkColor = Color(0xFF42A5F5),
        pronunciationGuide = "إحداث نبرة اهتزاز واضحة ومرتدّة عند النطق بأحد حروف القلقلة عندما يكون ساكناً، فتظهر القوة في صوت الحرف دون أن يميل إلى فتحة أو كسرة أو ضمة.",
        involvedLetters = "ق  •  ط  •  ب  •  ج  •  د  (مجموعة في: قُطبُ جَدّ)",
        quranExample = "قُلْ هُوَ ٱللَّهُ أَحَدٌ  •  لَمْ يَلِدْ وَلَمْ يُولَدْ",
        exampleReference = "الإخلاص ١ – ٣"
    ),
    IQLAB(
        arabicTitle = "الإقلاب",
        badgeLabel = "قلب النون ميماً بغنة",
        lightColor = Color(0xFF6A1B9A),
        darkColor = Color(0xFFBA68C8),
        pronunciationGuide = "قلب النون الساكنة أو التنوين ميماً مخفاة مع غنة بمقدار حركتين إذا جاء بعدها حرف الباء، مع تلامس لطيف بين الشفتين دون ضغط شديد (تُعرف بعلامة الميم الصغيرة ۢ).",
        involvedLetters = "النون الساكنة أو التنوين + حرف (ب) • علامة (ۢ)",
        quranExample = "مِنۢ بَعْدِ  •  سَمِيعٌۢ بَصِيرٌ  •  لَيُنۢبَذَنَّ",
        exampleReference = "الهمزة ٤ • لقمان ٢٨"
    ),
    SILENT(
        arabicTitle = "الإدغام بلا غنة والحروف الصامتة",
        badgeLabel = "لا يُنطق في الوصل",
        lightColor = Color(0xFF757575),
        darkColor = Color(0xFF9E9E9E),
        pronunciationGuide = "حروف تُكتب في الرسم العثماني ولا تُنطق عند الوصل: مثل همزة الوصل (ٱ) وسط الكلام، واللام الشمسية، والحروف التي فوقها سكون مستدير (۟)، والنون المدغمة بلا غنة قبل (ل، ر).",
        involvedLetters = "ٱ (همزة الوصل) • اللام الشمسية • علامة (۟) • ن/تنوين قبل (ل، ر)",
        quranExample = "مِن رَّبِّهِمْ  •  أُو۟لَٰٓئِكَ  •  قَالُوا۟  •  ٱلشَّمْسِ",
        exampleReference = "البقرة ٥ • الشمس ١"
    );

    fun colorForMode(isNightMode: Boolean): Color = if (isNightMode) darkColor else lightColor
}

data class TajweedSpan(
    val start: Int,
    val end: Int,
    val rule: TajweedRule
)

object TajweedEngine {

    // Tiny bounded LRU caches (max ~5 Mushaf pages ≈ 80 Ayahs) so Tajweed parsing never re-runs
    // on tap/scroll/80ms ticks while keeping RAM footprint minimal on 2GB-3GB Android devices.
    private val tajweedSpanCache = LruCache<String, List<TajweedSpan>>(80)
    private val wordRangeCache = LruCache<String, IntArray>(25)

    fun clearCaches() {
        tajweedSpanCache.evictAll()
        wordRangeCache.evictAll()
    }

    private const val SUKOON_STD = '\u0652'
    private const val SUKOON_UTHMANI = '\u06E1' // ۡ
    private const val SHADDAH = '\u0651'        // ّ
    private const val MADDAH = '\u0653'         // ٓ
    private const val DAGGER_ALEF = '\u0670'    // ٰ
    private const val ALEF_WASLA = '\u0671'     // ٱ
    private const val SMALL_HIGH_ZERO = '\u06DF' // ۟ (silent letter)
    private const val SMALL_HIGH_MEEM = '\u06E2' // ۢ (iqlab)
    private const val SMALL_LOW_MEEM = '\u06ED'  // ۭ (iqlab)
    private const val SMALL_WAW = '\u06E5'       // ۥ
    private const val SMALL_YAA = '\u06E6'       // ۦ

    private val tanweenMarks = setOf('\u064B', '\u064C', '\u064D', '\u08F0', '\u08F1', '\u08F2')
    private val vowelMarks = setOf('\u064E', '\u064F', '\u0650') // fatha, damma, kasra
    private val qalqalahLetters = setOf('ق', 'ط', 'ب', 'ج', 'د')
    private val idghamGhunnahLetters = setOf('ي', 'ن', 'م', 'و')
    private val idghamNoGhunnahLetters = setOf('ل', 'ر')
    private val ikhfaLetters = setOf(
        'ت', 'ث', 'ج', 'د', 'ذ', 'ز', 'س', 'ش', 'ص', 'ض', 'ط', 'ظ', 'ف', 'ق', 'ك'
    )
    private val solarLetters = setOf(
        'ت', 'ث', 'د', 'ذ', 'ر', 'ز', 'س', 'ش', 'ص', 'ض', 'ط', 'ظ', 'ل', 'ن'
    )

    private fun isCombiningMark(ch: Char): Boolean {
        val code = ch.code
        return (code in 0x064B..0x065F) ||
            code == 0x0670 ||
            (code in 0x06D6..0x06ED) ||
            (code in 0x08F0..0x08F3)
    }

    private fun isArabicLetter(ch: Char): Boolean {
        val code = ch.code
        return (code in 0x0621..0x064A) || code == 0x0671 || code == 0x0670 || code == 0x06E5 || code == 0x06E6
    }

    /**
     * Groups each Arabic base character with its trailing combining diacritics into a cluster.
     */
    private data class CharCluster(
        val baseChar: Char,
        val start: Int,
        val end: Int,
        val marks: String
    ) {
        val hasShaddah: Boolean get() = marks.indexOf(SHADDAH) >= 0
        val hasSukoon: Boolean get() = marks.indexOf(SUKOON_STD) >= 0 || marks.indexOf(SUKOON_UTHMANI) >= 0
        val hasMaddah: Boolean get() = marks.indexOf(MADDAH) >= 0 || baseChar == 'آ'
        val hasDaggerAlef: Boolean get() = marks.indexOf(DAGGER_ALEF) >= 0 || baseChar == DAGGER_ALEF
        val hasTanween: Boolean get() = marks.any { it in tanweenMarks }
        val hasShortVowel: Boolean get() = marks.any { it in vowelMarks }
        val hasIqlabMeem: Boolean get() = marks.indexOf(SMALL_HIGH_MEEM) >= 0 || marks.indexOf(SMALL_LOW_MEEM) >= 0
        val hasSilentZero: Boolean get() = marks.indexOf(SMALL_HIGH_ZERO) >= 0
        val isBareLetter: Boolean get() = !hasShortVowel && !hasTanween && !hasShaddah && !hasMaddah
    }

    private fun tokenizeClusters(text: String): List<CharCluster> {
        val result = ArrayList<CharCluster>(text.length / 2)
        var i = 0
        val n = text.length
        while (i < n) {
            val base = text[i]
            val start = i
            i++
            val marksSb = StringBuilder()
            while (i < n && isCombiningMark(text[i])) {
                marksSb.append(text[i])
                i++
            }
            result.add(CharCluster(base, start, i, marksSb.toString()))
        }
        return result
    }

    private fun nextArabicLetterIndex(clusters: List<CharCluster>, fromIndex: Int): Int {
        var idx = fromIndex + 1
        while (idx < clusters.size) {
            val c = clusters[idx]
            if (isArabicLetter(c.baseChar) && c.baseChar != ALEF_WASLA) {
                return idx
            }
            // Skip spaces and connecting hamzat al-wasl
            if (c.baseChar == ' ' || c.baseChar == ALEF_WASLA) {
                idx++
                continue
            }
            idx++
        }
        return -1
    }

    private fun hasSpaceBetween(clusters: List<CharCluster>, i1: Int, i2: Int): Boolean {
        for (k in (i1 + 1) until i2) {
            if (clusters[k].baseChar == ' ') return true
        }
        return false
    }

    fun detectTajweedSpans(text: String): List<TajweedSpan> {
        if (text.isBlank()) return emptyList()
        tajweedSpanCache.get(text)?.let { return it }
        val clusters = tokenizeClusters(text)
        val spans = ArrayList<TajweedSpan>()
        val assigned = BooleanArray(clusters.size)

        fun mark(idx: Int, rule: TajweedRule) {
            if (idx !in clusters.indices || assigned[idx]) return
            assigned[idx] = true
            spans.add(TajweedSpan(clusters[idx].start, clusters[idx].end, rule))
        }

        for (i in clusters.indices) {
            val c = clusters[i]
            if (!isArabicLetter(c.baseChar)) continue

            // 1. Silent letters (Small High Rounded Zero ۟, mid-text Alef Wasla ٱ, Lam Shamsiyyah)
            if (c.hasSilentZero) {
                mark(i, TajweedRule.SILENT)
                continue
            }
            if (c.baseChar == ALEF_WASLA && i > 0) {
                mark(i, TajweedRule.SILENT)
                continue
            }
            if (c.baseChar == 'ل' && c.isBareLetter && !c.hasSukoon && i > 0) {
                val prev = clusters[i - 1].baseChar
                val nextIdx = nextArabicLetterIndex(clusters, i)
                if ((prev == ALEF_WASLA || prev == 'ا' || prev == 'ل') &&
                    nextIdx != -1 &&
                    clusters[nextIdx].baseChar in solarLetters &&
                    clusters[nextIdx].hasShaddah
                ) {
                    mark(i, TajweedRule.SILENT)
                    continue
                }
            }

            // 2. Madd (Maddah ٓ, Dagger Alef ٰ, Small Waw/Yaa ۥ/ۦ)
            if (c.hasMaddah || c.hasDaggerAlef || c.baseChar == SMALL_WAW || c.baseChar == SMALL_YAA) {
                mark(i, TajweedRule.MADD)
                continue
            }

            // 3. Ghunnah on Noon Mushaddadah (نّ) & Meem Mushaddadah (مّ)
            if ((c.baseChar == 'ن' || c.baseChar == 'م') && c.hasShaddah) {
                mark(i, TajweedRule.GHUNNAH)
                continue
            }

            // 4. Iqlab (marked with small meem ۢ/ۭ OR noon sakinah/tanween + ب)
            val isNoonSakinah = c.baseChar == 'ن' && (c.hasSukoon || c.isBareLetter)
            val isTanweenCarrier = c.hasTanween
            if (c.hasIqlabMeem) {
                mark(i, TajweedRule.IQLAB)
                continue
            }
            if (isNoonSakinah || isTanweenCarrier) {
                val nextIdx = nextArabicLetterIndex(clusters, i)
                if (nextIdx != -1) {
                    val nextLetter = clusters[nextIdx].baseChar
                    val acrossWords = hasSpaceBetween(clusters, i, nextIdx)
                    when {
                        nextLetter == 'ب' -> {
                            mark(i, TajweedRule.IQLAB)
                            continue
                        }
                        nextLetter in idghamGhunnahLetters && (acrossWords || isTanweenCarrier) -> {
                            mark(i, TajweedRule.GHUNNAH)
                            mark(nextIdx, TajweedRule.GHUNNAH)
                            continue
                        }
                        nextLetter in idghamNoGhunnahLetters && (acrossWords || isTanweenCarrier) -> {
                            mark(i, TajweedRule.SILENT)
                            continue
                        }
                        nextLetter in ikhfaLetters -> {
                            mark(i, TajweedRule.IKHFA)
                            continue
                        }
                    }
                }
            }

            // 5. Meem Sakinah rules (م sakinah/bare + م => Ghunnah/Idgham Shafawi; + ب => Ikhfa Shafawi)
            val isMeemSakinah = c.baseChar == 'م' && (c.hasSukoon || c.isBareLetter)
            if (isMeemSakinah) {
                val nextIdx = nextArabicLetterIndex(clusters, i)
                if (nextIdx != -1) {
                    val nextLetter = clusters[nextIdx].baseChar
                    if (nextLetter == 'م') {
                        mark(i, TajweedRule.GHUNNAH)
                        mark(nextIdx, TajweedRule.GHUNNAH)
                        continue
                    } else if (nextLetter == 'ب') {
                        mark(i, TajweedRule.IKHFA)
                        continue
                    }
                }
            }

            // 6. Qalqalah (ق ط ب ج د with sukoon)
            if (c.baseChar in qalqalahLetters && c.hasSukoon) {
                mark(i, TajweedRule.QALQALAH)
                continue
            }
        }

        spans.sortBy { it.start }
        val result = spans.toList()
        tajweedSpanCache.put(text, result)
        return result
    }

    /**
     * Standard Android SpannableStringBuilder Tajweed colorizer over Uthmani Arabic text,
     * optionally applying synchronized Ayah & Word BackgroundColorSpan highlights without
     * breaking Tajweed foreground colors.
     */
    fun buildTajweedSpannable(
        text: String,
        isNightMode: Boolean = false,
        highlightFullAyah: Boolean = false,
        activeWordIndex: Int? = null
    ): SpannableStringBuilder {
        val builder = SpannableStringBuilder(text)
        if (highlightFullAyah && text.isNotEmpty()) {
            val ayahTint = if (isNightMode) 0x444CAF50 else 0x332E7D32
            builder.setSpan(
                BackgroundColorSpan(ayahTint),
                0,
                text.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            if (activeWordIndex != null) {
                val wordRange = findWordCharRange(text, activeWordIndex)
                if (wordRange != null) {
                    val wordTint = if (isNightMode) 0x774CAF50 else 0x662E7D32
                    builder.setSpan(
                        BackgroundColorSpan(wordTint),
                        wordRange.first,
                        wordRange.last + 1,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }
        val spans = detectTajweedSpans(text)
        for (span in spans) {
            if (span.start in 0 until span.end && span.end <= text.length) {
                builder.setSpan(
                    ForegroundColorSpan(span.rule.colorForMode(isNightMode).toArgb()),
                    span.start,
                    span.end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        return builder
    }

    /**
     * Returns the character index range (inclusive) of the [wordIndexZeroBased]-th whitespace-delimited word.
     * Uses a bounded LRU cache so 80ms audio word-sync ticks perform zero text scanning.
     */
    fun findWordCharRange(text: String, wordIndexZeroBased: Int): IntRange? {
        if (wordIndexZeroBased < 0 || text.isBlank()) return null
        val offsets = wordRangeCache.get(text) ?: computeWordOffsets(text).also {
            wordRangeCache.put(text, it)
        }
        val pairIdx = wordIndexZeroBased * 2
        if (pairIdx + 1 >= offsets.size) return null
        return offsets[pairIdx]..offsets[pairIdx + 1]
    }

    private fun computeWordOffsets(text: String): IntArray {
        val list = ArrayList<Int>(32)
        var i = 0
        val n = text.length
        while (i < n) {
            while (i < n && text[i].isWhitespace()) i++
            if (i >= n) break
            val start = i
            while (i < n && !text[i].isWhitespace()) i++
            list.add(start)
            list.add(i - 1)
        }
        return IntArray(list.size) { list[it] }
    }

    fun countWords(text: String): Int {
        var count = 0
        var inWord = false
        for (ch in text) {
            if (ch.isWhitespace()) {
                inWord = false
            } else if (!inWord) {
                inWord = true
                count++
            }
        }
        return count
    }

    /**
     * Resolves the 0-based word index within [text] for a tapped character offset [charOffset].
     * Uses [wordRangeCache] so tap-to-seek performs zero extra allocations.
     */
    fun findWordIndexAtCharOffset(text: String, charOffset: Int): Int? {
        if (text.isBlank() || charOffset < 0 || charOffset >= text.length) return null
        val offsets = wordRangeCache.get(text) ?: computeWordOffsets(text).also {
            wordRangeCache.put(text, it)
        }
        val wordCount = offsets.size / 2
        for (idx in 0 until wordCount) {
            val start = offsets[idx * 2]
            val end = offsets[idx * 2 + 1]
            if (charOffset in start..end) return idx
            if (charOffset < start) return idx
        }
        return if (wordCount > 0) wordCount - 1 else null
    }

    /**
     * Appends Uthmani text directly into a Jetpack Compose AnnotatedString.Builder
     * combining Tajweed foreground colors + synchronized Ayah & Word-by-Word background tint
     * with zero extra overhead when toggling Tajweed colors ON or OFF.
     */
    fun AnnotatedString.Builder.appendUthmaniWithTajweed(
        text: String,
        tajweedEnabled: Boolean,
        isNightMode: Boolean,
        baseTextColor: Color,
        backgroundColor: Color = Color.Transparent,
        activeWordIndex: Int? = null,
        activeWordBackground: Color = Color.Transparent
    ) {
        val activeWordRange = if (activeWordIndex != null && activeWordBackground != Color.Transparent) {
            findWordCharRange(text, activeWordIndex)
        } else {
            null
        }
        val tajweedSpans = if (tajweedEnabled) detectTajweedSpans(text) else emptyList()

        if (tajweedSpans.isEmpty() && activeWordRange == null) {
            withStyle(SpanStyle(color = baseTextColor, background = backgroundColor)) {
                append(text)
            }
            return
        }

        // Collect all split boundaries so foreground Tajweed colors and active word background compose cleanly
        val boundaries = sortedSetOf(0, text.length)
        for (span in tajweedSpans) {
            boundaries.add(span.start.coerceIn(0, text.length))
            boundaries.add(span.end.coerceIn(0, text.length))
        }
        if (activeWordRange != null) {
            boundaries.add(activeWordRange.first.coerceIn(0, text.length))
            boundaries.add((activeWordRange.last + 1).coerceIn(0, text.length))
        }

        val sortedPoints = boundaries.toList()
        var spanCursor = 0
        for (idx in 0 until sortedPoints.size - 1) {
            val segStart = sortedPoints[idx]
            val segEnd = sortedPoints[idx + 1]
            if (segEnd <= segStart) continue

            while (spanCursor < tajweedSpans.size && tajweedSpans[spanCursor].end <= segStart) {
                spanCursor++
            }
            val matchingRule = tajweedSpans.getOrNull(spanCursor)?.takeIf {
                segStart >= it.start && segEnd <= it.end
            }?.rule

            val fgColor = matchingRule?.colorForMode(isNightMode) ?: baseTextColor
            val isInActiveWord = activeWordRange != null &&
                segStart >= activeWordRange.first &&
                segEnd <= activeWordRange.last + 1
            val bgColor = if (isInActiveWord) activeWordBackground else backgroundColor

            withStyle(SpanStyle(color = fgColor, background = bgColor)) {
                append(text.substring(segStart, segEnd))
            }
        }
    }
}
