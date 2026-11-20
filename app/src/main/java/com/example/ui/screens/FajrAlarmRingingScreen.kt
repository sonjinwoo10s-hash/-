package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.FajrAlarmData
import com.example.data.model.FajrAlarmSettings
import com.example.data.model.FajrChallengeType
import com.example.data.model.MathDifficulty
import com.example.data.model.MemoryGridSize
import com.example.ui.components.QrCameraScannerBox
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.SoftGold
import com.example.util.VibrationHelper
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FajrAlarmRingingScreen(
    settings: FajrAlarmSettings,
    onChallengeSolved: () -> Unit
) {
    // Strictly inescapable until challenge is solved
    BackHandler(enabled = true) { /* Intentionally blocked until challenge completes */ }

    var liveClockText by remember { mutableStateOf(formatCurrentTime()) }
    LaunchedEffect(Unit) {
        while (true) {
            liveClockText = formatCurrentTime()
            delay(1000L)
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "alarm_pulse")
    val iconScale by pulseTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF051C15),
                        Color(0xFF0A382A),
                        Color(0xFF041410)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("fajr_alarm_ringing_overlay")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top ringing badge (NO close / cancel button)
            Surface(
                shape = RoundedCornerShape(50),
                color = SoftGold.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = Color(0xFFF6DC97),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = settings.resolvedSound().arabicLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFF6DC97),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Pulsing Alarm Icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .scale(iconScale)
                    .clip(CircleShape)
                    .background(SoftGold.copy(alpha = 0.22f))
                    .border(2.dp, SoftGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = "منبه الفجر يرن",
                    tint = Color(0xFFF6DC97),
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Live current time
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    text = liveClockText,
                    style = MaterialTheme.typography.displayLarge,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 1.5.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "حان وقت الاستيقاظ لصلاة الفجر",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFF6DC97),
                textAlign = TextAlign.Center
            )
            Text(
                text = "الصلاة خير من النوم — أتمّ التحدي لإيقاف المنبه",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.82f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Interactive Dismissal Challenge Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E2920)),
                border = BorderStroke(1.5.dp, SoftGold.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (settings.resolvedChallenge()) {
                        FajrChallengeType.QR_CODE -> QrDismissChallenge(
                            targetQrCode = settings.targetQrCode,
                            onSolved = onChallengeSolved
                        )
                        FajrChallengeType.MATH -> MathDismissChallenge(
                            difficulty = settings.resolvedMathDifficulty(),
                            totalProblems = settings.mathProblemCount.coerceIn(1, 5),
                            onSolved = onChallengeSolved
                        )
                        FajrChallengeType.MEMORY -> MemoryDismissChallenge(
                            gridSize = settings.resolvedMemoryGrid(),
                            onSolved = onChallengeSolved
                        )
                        FajrChallengeType.TRIVIA -> TriviaDismissChallenge(
                            requiredStreak = 3,
                            onSolved = onChallengeSolved
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QrDismissChallenge(
    targetQrCode: String,
    onSolved: () -> Unit
) {
    val context = LocalContext.current
    var statusMessage by remember { mutableStateOf("وجّه الكاميرا نحو رمز QR المحفوظ لإيقاف المنبه") }
    var isError by remember { mutableStateOf(false) }
    var manualInput by remember { mutableStateOf("") }

    fun verifyCode(candidate: String) {
        val clean = candidate.trim()
        if (clean.equals(targetQrCode.trim(), ignoreCase = true)) {
            onSolved()
        } else {
            isError = true
            statusMessage = "الرمز غير مطابق! المطلوب هو الرمز المحفوظ في إعدادات المنبه."
            VibrationHelper.vibrateShortTick(context)
        }
    }

    Text(
        text = "تحدي مسح رمز QR",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = Color(0xFFF6DC97)
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = statusMessage,
        style = MaterialTheme.typography.bodySmall,
        color = if (isError) Color(0xFFFCA5A5) else Color.White.copy(alpha = 0.85f),
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(12.dp))

    QrCameraScannerBox(
        modifier = Modifier.height(220.dp),
        onQrCodeScanned = { scanned -> verifyCode(scanned) }
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Graceful fallback manual verification when camera unavailable/denied
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.Black.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "بديل الطوارئ (في حال تعذّر الكاميرا): أدخل الرمز المحفوظ",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.78f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = manualInput,
                onValueChange = { manualInput = it },
                placeholder = { Text("اكتب رمز QR المحفوظ هنا", color = Color.White.copy(alpha = 0.5f)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = SoftGold,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
                    cursorColor = SoftGold
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { verifyCode(manualInput) }),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("qr_fallback_input")
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { verifyCode(manualInput) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("qr_fallback_submit"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SoftGold,
                    contentColor = Color(0xFF1A1405)
                )
            ) {
                Text("تحقق من الرمز", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun MathDismissChallenge(
    difficulty: MathDifficulty,
    totalProblems: Int,
    onSolved: () -> Unit
) {
    val context = LocalContext.current
    var solvedCount by remember { mutableIntStateOf(0) }
    var problem by remember(difficulty) { mutableStateOf(FajrAlarmData.generateMathProblem(difficulty)) }
    var answerInput by remember { mutableStateOf("") }
    var feedbackError by remember { mutableStateOf<String?>(null) }

    fun submitAnswer() {
        val parsed = answerInput.trim().toIntOrNull()
        if (parsed == problem.answer) {
            val nextSolved = solvedCount + 1
            feedbackError = null
            answerInput = ""
            if (nextSolved >= totalProblems) {
                onSolved()
            } else {
                solvedCount = nextSolved
                problem = FajrAlarmData.generateMathProblem(difficulty)
            }
        } else {
            feedbackError = "إجابة غير صحيحة، حاول مرة أخرى!"
            VibrationHelper.vibrateShortTick(context)
        }
    }

    Text(
        text = "تحدي المسائل الرياضية (${difficulty.arabicLabel})",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = Color(0xFFF6DC97)
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = "المسألة ${solvedCount + 1} من $totalProblems",
        style = MaterialTheme.typography.labelLarge,
        color = Color.White.copy(alpha = 0.9f)
    )
    Spacer(modifier = Modifier.height(8.dp))
    LinearProgressIndicator(
        progress = { solvedCount.toFloat() / totalProblems.toFloat() },
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(50)),
        color = SoftGold,
        trackColor = Color.White.copy(alpha = 0.16f)
    )

    Spacer(modifier = Modifier.height(16.dp))

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Text(
                text = problem.expression,
                style = MaterialTheme.typography.displayLarge,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp, horizontal = 12.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    OutlinedTextField(
        value = answerInput,
        onValueChange = { raw ->
            val filtered = raw.filterIndexed { idx, ch -> ch.isDigit() || (idx == 0 && ch == '-') }
            answerInput = filtered.take(6)
            feedbackError = null
        },
        label = { Text("أدخل الناتج الصحيح", color = Color.White.copy(alpha = 0.75f)) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { submitAnswer() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = SoftGold,
            unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
            cursorColor = SoftGold
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("math_challenge_input")
    )

    if (feedbackError != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = feedbackError.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFFCA5A5),
            fontWeight = FontWeight.Bold
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Button(
        onClick = { submitAnswer() },
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("math_challenge_submit"),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SoftGold,
            contentColor = Color(0xFF1A1405)
        )
    ) {
        Icon(Icons.Default.AlarmOn, contentDescription = null)
        Spacer(modifier = Modifier.width(6.dp))
        Text("تأكيد الإجابة", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun MemoryDismissChallenge(
    gridSize: MemoryGridSize,
    onSolved: () -> Unit
) {
    val context = LocalContext.current
    val totalTiles = gridSize.dimension * gridSize.dimension

    var roundKey by remember { mutableIntStateOf(0) }
    var targetTiles by remember(roundKey, gridSize) {
        mutableStateOf((0 until totalTiles).shuffled().take(gridSize.targetCount).toSet())
    }
    var isMemorizePhase by remember(roundKey) { mutableStateOf(true) }
    var countdownFraction by remember(roundKey) { mutableStateOf(1f) }
    var pickedTiles by remember(roundKey) { mutableStateOf<Set<Int>>(emptySet()) }
    var errorTile by remember(roundKey) { mutableStateOf<Int?>(null) }

    LaunchedEffect(roundKey) {
        isMemorizePhase = true
        pickedTiles = emptySet()
        errorTile = null
        val totalMs = 3000L
        val step = 50L
        var elapsed = 0L
        while (elapsed < totalMs) {
            countdownFraction = 1f - (elapsed.toFloat() / totalMs.toFloat())
            delay(step)
            elapsed += step
        }
        countdownFraction = 0f
        isMemorizePhase = false
    }

    Text(
        text = "تحدي الذاكرة (${gridSize.arabicLabel})",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = Color(0xFFF6DC97)
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = when {
            errorTile != null -> "خطأ! سيتم عرض نمط جديد، ركّز جيداً..."
            isMemorizePhase -> "احفظ مواقع المربعات الذهبية المضيئة الآن..."
            else -> "اضغط على المربعات الصحيحة (${pickedTiles.size} / ${targetTiles.size})"
        },
        style = MaterialTheme.typography.bodySmall,
        color = if (errorTile != null) Color(0xFFFCA5A5) else Color.White.copy(alpha = 0.9f),
        textAlign = TextAlign.Center
    )

    if (isMemorizePhase) {
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { countdownFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50)),
            color = SoftGold,
            trackColor = Color.White.copy(alpha = 0.16f)
        )
    }

    Spacer(modifier = Modifier.height(14.dp))

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        for (row in 0 until gridSize.dimension) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                for (col in 0 until gridSize.dimension) {
                    val idx = row * gridSize.dimension + col
                    val litForMemorize = isMemorizePhase && targetTiles.contains(idx)
                    val pickedCorrect = pickedTiles.contains(idx)
                    val isWrong = errorTile == idx

                    val tileColor by animateColorAsState(
                        targetValue = when {
                            isWrong -> Color(0xFFDC2626)
                            litForMemorize -> SoftGold
                            pickedCorrect -> EmeraldGreen
                            else -> Color.White.copy(alpha = 0.12f)
                        },
                        label = "memory_tile_$idx"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(tileColor)
                            .border(
                                width = 1.5.dp,
                                color = if (litForMemorize || pickedCorrect) Color(0xFFF6DC97) else Color.White.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .testTag("memory_tile_$idx")
                            .clickable(enabled = !isMemorizePhase && errorTile == null && !pickedCorrect) {
                                if (targetTiles.contains(idx)) {
                                    val nextPicked = pickedTiles + idx
                                    pickedTiles = nextPicked
                                    VibrationHelper.vibrateShortTick(context)
                                    if (nextPicked.size == targetTiles.size) {
                                        onSolved()
                                    }
                                } else {
                                    errorTile = idx
                                    VibrationHelper.vibrateRoundComplete(context)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (pickedCorrect) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "صحيح",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(errorTile) {
        if (errorTile != null) {
            delay(850L)
            roundKey++
        }
    }
}

@Composable
private fun TriviaDismissChallenge(
    requiredStreak: Int,
    onSolved: () -> Unit
) {
    val context = LocalContext.current
    val shuffledPool = remember { FajrAlarmData.triviaQuestions.shuffled() }
    var cursor by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var wrongMessage by remember { mutableStateOf<String?>(null) }

    val currentQuestion = shuffledPool[cursor % shuffledPool.size]

    Text(
        text = "أسئلة عشوائية (تاريخ ورياضيات)",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = Color(0xFFF6DC97)
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = "إجابات صحيحة متتالية: $streak / $requiredStreak",
        style = MaterialTheme.typography.labelLarge,
        color = Color.White
    )
    Spacer(modifier = Modifier.height(8.dp))
    LinearProgressIndicator(
        progress = { streak.toFloat() / requiredStreak.toFloat() },
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(50)),
        color = SoftGold,
        trackColor = Color.White.copy(alpha = 0.16f)
    )

    Spacer(modifier = Modifier.height(14.dp))

    Surface(
        shape = RoundedCornerShape(50),
        color = EmeraldGreen.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.4f))
    ) {
        Text(
            text = currentQuestion.category,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFF6DC97),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    Text(
        text = currentQuestion.question,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        textAlign = TextAlign.Center,
        lineHeight = 26.sp
    )

    if (wrongMessage != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = wrongMessage.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFFCA5A5),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }

    Spacer(modifier = Modifier.height(14.dp))

    currentQuestion.options.forEachIndexed { idx, option ->
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(14.dp))
                .testTag("trivia_option_$idx")
                .clickable {
                    if (idx == currentQuestion.correctIndex) {
                        wrongMessage = null
                        val nextStreak = streak + 1
                        if (nextStreak >= requiredStreak) {
                            onSolved()
                        } else {
                            streak = nextStreak
                            cursor++
                        }
                    } else {
                        streak = 0
                        cursor++
                        wrongMessage = "إجابة خاطئة! تم إعادة العدّاد إلى 0 — ركّز في السؤال التالي"
                        VibrationHelper.vibrateRoundComplete(context)
                    }
                },
            shape = RoundedCornerShape(14.dp),
            color = Color.White.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f))
        ) {
            Text(
                text = option,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            )
        }
    }
}

private fun formatCurrentTime(): String {
    return runCatching {
        SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
    }.getOrDefault("04:30:00")
}
