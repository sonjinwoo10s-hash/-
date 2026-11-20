package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AyahAudioState
import com.example.data.model.AyahItem
import com.example.data.model.AyahRepeatMode
import com.example.data.model.MushafPaperTheme
import com.example.data.model.SurahSummary
import com.example.ui.theme.AmiriQuranFontFamily
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LocalIslamicColors
import com.example.ui.theme.MushafCreamBorder
import com.example.ui.theme.MushafCreamHighlight
import com.example.ui.theme.MushafCreamInnerBorder
import com.example.ui.theme.MushafCreamPaper
import com.example.ui.theme.MushafCreamSubtle
import com.example.ui.theme.MushafCreamText
import com.example.ui.theme.MushafWhiteBorder
import com.example.ui.theme.MushafWhiteHighlight
import com.example.ui.theme.MushafWhitePaper
import com.example.ui.theme.MushafWhiteSubtle
import com.example.ui.theme.MushafWhiteText
import com.example.ui.theme.NightReadDivider
import com.example.ui.theme.NightReadGold
import com.example.ui.theme.NightReadHighlight
import com.example.ui.theme.NightReadPaper
import com.example.ui.theme.NightReadTafsirBg
import com.example.ui.theme.NightReadTafsirText
import com.example.ui.theme.NightReadText
import com.example.ui.theme.SoftGold

data class MushafVisualPalette(
    val paperBg: Color,
    val textColor: Color,
    val outerBorder: Color,
    val innerBorder: Color,
    val medallionColor: Color,
    val activeHighlightBg: Color,
    val bannerBg: Color,
    val subtleText: Color,
    val tafsirBg: Color,
    val tafsirText: Color,
    val dividerColor: Color
)

fun resolveMushafPalette(theme: MushafPaperTheme): MushafVisualPalette = when (theme) {
    MushafPaperTheme.CREAM -> MushafVisualPalette(
        paperBg = MushafCreamPaper,
        textColor = MushafCreamText,
        outerBorder = MushafCreamBorder,
        innerBorder = MushafCreamInnerBorder,
        medallionColor = Color(0xFF8C681A),
        activeHighlightBg = MushafCreamHighlight,
        bannerBg = Color(0xFFF3EBD2),
        subtleText = MushafCreamSubtle,
        tafsirBg = Color(0xFFF3EEDB),
        tafsirText = Color(0xFF473E2B),
        dividerColor = Color(0xFFE3D5AF)
    )
    MushafPaperTheme.PURE_WHITE -> MushafVisualPalette(
        paperBg = MushafWhitePaper,
        textColor = MushafWhiteText,
        outerBorder = MushafWhiteBorder.copy(alpha = 0.7f),
        innerBorder = SoftGold,
        medallionColor = EmeraldGreen,
        activeHighlightBg = MushafWhiteHighlight,
        bannerBg = Color(0xFFF1F7F4),
        subtleText = MushafWhiteSubtle,
        tafsirBg = Color(0xFFF5F8F6),
        tafsirText = Color(0xFF3A4B45),
        dividerColor = Color(0xFFDCE7E2)
    )
    MushafPaperTheme.NIGHT -> MushafVisualPalette(
        paperBg = NightReadPaper,
        textColor = NightReadText,
        outerBorder = NightReadGold.copy(alpha = 0.55f),
        innerBorder = Color(0xFF219675).copy(alpha = 0.65f),
        medallionColor = NightReadGold,
        activeHighlightBg = NightReadHighlight,
        bannerBg = Color(0xFF1B2722),
        subtleText = NightReadTafsirText,
        tafsirBg = NightReadTafsirBg,
        tafsirText = NightReadTafsirText,
        dividerColor = NightReadDivider
    )
}

fun toArabicIndicDigits(number: Int): String {
    val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    return number.toString().map { ch ->
        if (ch in '0'..'9') arabicDigits[ch - '0'] else ch
    }.joinToString("")
}

@Composable
fun MushafOrnamentalFrame(
    palette: MushafVisualPalette,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.paperBg)
    ) {
        // Traditional Islamic double border frame with geometric corner ornaments
        Canvas(modifier = Modifier.matchParentSize()) {
            val outerInset = 4.dp.toPx()
            val innerInset = 10.dp.toPx()
            val cornerSize = 18.dp.toPx()

            // Outer gold/emerald border
            drawRoundRect(
                color = palette.outerBorder,
                topLeft = Offset(outerInset, outerInset),
                size = Size(size.width - outerInset * 2, size.height - outerInset * 2),
                cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )
            // Inner delicate border
            drawRoundRect(
                color = palette.innerBorder.copy(alpha = 0.55f),
                topLeft = Offset(innerInset, innerInset),
                size = Size(size.width - innerInset * 2, size.height - innerInset * 2),
                cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )

            // 4 corner medallions
            val corners = listOf(
                Offset(innerInset + 4.dp.toPx(), innerInset + 4.dp.toPx()),
                Offset(size.width - innerInset - 4.dp.toPx(), innerInset + 4.dp.toPx()),
                Offset(innerInset + 4.dp.toPx(), size.height - innerInset - 4.dp.toPx()),
                Offset(size.width - innerInset - 4.dp.toPx(), size.height - innerInset - 4.dp.toPx())
            )
            corners.forEach { pt ->
                drawCircle(
                    color = palette.outerBorder.copy(alpha = 0.85f),
                    radius = 4.dp.toPx(),
                    center = pt
                )
                drawCircle(
                    color = palette.innerBorder.copy(alpha = 0.45f),
                    radius = (cornerSize / 3f),
                    center = pt,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            content()
        }
    }
}

@Composable
fun MushafSurahTitleBanner(
    surah: SurahSummary,
    palette: MushafVisualPalette
) {
    val revLabel = if (surah.revelationType == "Meccan") "مكية" else "مدنية"
    val ayahCountArabic = toArabicIndicDigits(surah.numberOfAyahs)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Ornate Surah Header Cartouche
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = palette.bannerBg,
            border = BorderStroke(1.5.dp, palette.outerBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = palette.innerBorder.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = revLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = palette.medallionColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = surah.name,
                    fontFamily = AmiriQuranFontFamily,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = palette.innerBorder.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = "$ayahCountArabic آية",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = palette.medallionColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Centered Ornate Basmalah for every Surah except At-Tawbah (9) and Al-Fatiha (where Ayah 1 is Basmalah)
        if (surah.number != 9 && surah.number != 1) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                fontFamily = AmiriQuranFontFamily,
                fontSize = 24.sp,
                color = palette.medallionColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
        } else {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun MushafContinuousTextBlock(
    surahNumber: Int,
    ayahs: List<AyahItem>,
    fontSizeSp: Float,
    palette: MushafVisualPalette,
    bookmarkedAyahNumber: Int?,
    selectedAyahNumber: Int?,
    playingAyahNumber: Int?,
    onAyahTapped: (AyahItem) -> Unit,
    onTargetAyahVerticalOffset: (Int) -> Unit
) {
    val ayahRanges = remember(ayahs, bookmarkedAyahNumber, selectedAyahNumber, playingAyahNumber, palette) {
        val ranges = mutableMapOf<Int, IntRange>()
        val annotated = buildAnnotatedString {
            ayahs.forEach { ayah ->
                val start = length
                val isHighlighted = ayah.numberInSurah == selectedAyahNumber ||
                    ayah.numberInSurah == playingAyahNumber
                val isBookmarked = ayah.numberInSurah == bookmarkedAyahNumber
                val bgTint = when {
                    ayah.numberInSurah == playingAyahNumber -> palette.activeHighlightBg
                    ayah.numberInSurah == selectedAyahNumber -> palette.activeHighlightBg.copy(alpha = 0.85f)
                    else -> Color.Transparent
                }

                pushStringAnnotation(tag = "AYAH", annotation = ayah.numberInSurah.toString())
                withStyle(
                    SpanStyle(
                        color = palette.textColor,
                        background = bgTint
                    )
                ) {
                    append(ayah.text)
                    append(" ")
                }
                // Traditional ornate Arabic-Indic numeral medallion inline at end of verse
                val medallionText = buildString {
                    append("﴿")
                    append(toArabicIndicDigits(ayah.numberInSurah))
                    append("﴾")
                    if (isBookmarked) append(" ⚑")
                    append(" ")
                }
                withStyle(
                    SpanStyle(
                        color = if (isBookmarked) EmeraldGreen else palette.medallionColor,
                        fontWeight = FontWeight.Bold,
                        background = bgTint
                    )
                ) {
                    append(medallionText)
                }
                pop()
                val end = length
                ranges[ayah.numberInSurah] = start until end
            }
        }
        annotated to ranges
    }

    val (annotatedText, rangeMap) = ayahRanges
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    // Smoothly notify parent scroll container whenever the active/playing Ayah changes
    LaunchedEffect(playingAyahNumber, layoutResult) {
        val target = playingAyahNumber ?: return@LaunchedEffect
        val res = layoutResult ?: return@LaunchedEffect
        val range = rangeMap[target] ?: return@LaunchedEffect
        runCatching {
            val box = res.getBoundingBox(range.first)
            onTargetAyahVerticalOffset(box.top.toInt())
        }
    }

    Text(
        text = annotatedText,
        fontFamily = AmiriQuranFontFamily,
        fontSize = fontSizeSp.sp,
        lineHeight = (fontSizeSp * 2.3f).sp,
        textAlign = TextAlign.Justify,
        onTextLayout = { layoutResult = it },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mushaf_continuous_text_$surahNumber")
            .pointerInput(ayahs, annotatedText) {
                detectTapGestures { tapOffset ->
                    val res = layoutResult ?: return@detectTapGestures
                    val charIndex = res.getOffsetForPosition(tapOffset)
                    val annotation = annotatedText
                        .getStringAnnotations(tag = "AYAH", start = charIndex, end = charIndex)
                        .firstOrNull()
                    val tappedNum = annotation?.item?.toIntOrNull()
                    val matched = ayahs.find { it.numberInSurah == tappedNum }
                    if (matched != null) {
                        onAyahTapped(matched)
                    }
                }
            }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AyahActionBottomSheet(
    surah: SurahSummary,
    ayah: AyahItem,
    isBookmarked: Boolean,
    ayahAudioState: AyahAudioState,
    onDismiss: () -> Unit,
    onTogglePlayThisAyah: () -> Unit,
    onSelectRepeatMode: (AyahRepeatMode) -> Unit,
    onToggleContinueReading: (Boolean) -> Unit,
    onPlayFullSurah: () -> Unit,
    onSaveBookmark: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    val islamic = LocalIslamicColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isThisAyahLoaded = ayahAudioState.surahNumber == surah.number &&
        ayahAudioState.ayahNumber == ayah.numberInSurah
    val isThisAyahPlaying = isThisAyahLoaded && ayahAudioState.isPlaying
    val isThisAyahBuffering = isThisAyahLoaded && ayahAudioState.isBuffering

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 18.dp)
                .padding(bottom = 22.dp)
                .testTag("ayah_action_bottom_sheet")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${surah.name} • الآية ${toArabicIndicDigits(ayah.numberInSurah)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "الجزء ${toArabicIndicDigits(ayah.juz)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = islamic.textMuted
                    )
                }

                if (isBookmarked) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = islamic.goldSoft,
                        border = BorderStroke(1.dp, islamic.gold)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = islamic.goldText,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "علامة الوقف",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = islamic.goldText
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Ayah Text Preview
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = islamic.cardAlt,
                border = BorderStroke(1.dp, islamic.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${ayah.text} ﴿${toArabicIndicDigits(ayah.numberInSurah)}﴾",
                    fontFamily = AmiriQuranFontFamily,
                    fontSize = 22.sp,
                    lineHeight = 44.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.padding(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Verse-by-Verse Audio Panel (EveryAyah CDN)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = islamic.emeraldSoft,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onTogglePlayThisAyah,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("play_single_ayah_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            )
                        ) {
                            when {
                                isThisAyahBuffering -> CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                isThisAyahPlaying -> Icon(Icons.Default.Pause, contentDescription = null)
                                else -> Icon(Icons.Default.PlayArrow, contentDescription = null)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when {
                                    isThisAyahBuffering -> "جاري التحميل..."
                                    isThisAyahPlaying -> {
                                        if (ayahAudioState.repeatMode != AyahRepeatMode.ONCE) {
                                            "إيقاف (تكرار ${ayahAudioState.currentRepeatIndex})"
                                        } else {
                                            "إيقاف مؤقت"
                                        }
                                    }
                                    else -> "استماع لهذه الآية"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = onPlayFullSurah,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("السورة كاملة", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (isThisAyahLoaded && ayahAudioState.fallbackNote != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = ayahAudioState.fallbackNote,
                            style = MaterialTheme.typography.labelSmall,
                            color = islamic.goldText,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (isThisAyahLoaded && ayahAudioState.errorMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = ayahAudioState.errorMessage,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Repeat Mode Selector (Memorization)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تكرار الآية للحفظ:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AyahRepeatMode.entries.forEach { mode ->
                            val selected = ayahAudioState.repeatMode == mode
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectRepeatMode(mode) },
                                label = {
                                    Text(
                                        mode.arabicLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("ayah_repeat_${mode.name}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(6.dp))

                    // Continue Reading From Here Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "الاستمرار من هذه الآية",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "الانتقال تلقائياً للآية التالية بعد انتهاء التلاوة",
                                style = MaterialTheme.typography.labelSmall,
                                color = islamic.textMuted
                            )
                        }
                        Switch(
                            checked = ayahAudioState.continueReading,
                            onCheckedChange = onToggleContinueReading,
                            modifier = Modifier.testTag("ayah_continue_reading_switch")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tafsir Section
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = islamic.goldSoft,
                border = BorderStroke(1.dp, islamic.gold.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "التفسير الميسر للآية",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = islamic.goldText
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = ayah.tafsir.ifBlank { "التفسير غير متوفر لهذه الآية في الوضع غير المتصل." },
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bookmark + Copy + Share Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSaveBookmark,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("sheet_bookmark_ayah_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SoftGold,
                        contentColor = Color(0xFF1A1405)
                    )
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isBookmarked) "محفوظة كوقف" else "حفظ علامة الوقف",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                OutlinedButton(
                    onClick = onCopy,
                    modifier = Modifier
                        .weight(0.85f)
                        .height(48.dp)
                        .testTag("sheet_copy_ayah_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("نسخ", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier
                        .weight(0.85f)
                        .height(48.dp)
                        .testTag("sheet_share_ayah_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "مشاركة", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مشاركة", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
