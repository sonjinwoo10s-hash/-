package com.example.ui.components

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Rational
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.repository.GlobalLocationConfig
import com.example.ramadan.CrescentLiveSource
import com.example.ramadan.RamadanCountdownTick
import com.example.ramadan.RamadanCrescentManager
import com.example.ramadan.RamadanSightingTarget
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.DeepGreen
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.SoftGold
import kotlinx.coroutines.delay
import java.util.Locale

private enum class RamadanCardMode {
    COUNTDOWN,
    LIVE_PROMPT,
    LIVE_PLAYER,
    DISMISSED_STATUS,
    RAMADAN_ACTIVE
}

/**
 * Ramadan Countdown & Live Crescent Sighting Card.
 *
 * Strictly isolated from parent 1-second prayer countdown recompositions so that once
 * the embedded live video player opens, the WebView NEVER re-renders, reloads, or flickers.
 */
@Composable
fun RamadanCrescentCard(
    globalLocation: GlobalLocationConfig,
    hijriDayOffset: Int,
    maghribAdjustmentMinutes: Int,
    onShowToast: (String) -> Unit
) {
    val context = LocalContext.current

    // Compute target sighting date/time only when location, Hijri offset, or Maghrib adjustment changes
    val sightingTarget = remember(
        globalLocation.lat,
        globalLocation.lng,
        globalLocation.calculationMethodId,
        hijriDayOffset,
        maghribAdjustmentMinutes
    ) {
        RamadanCrescentManager.computeSightingTarget(
            location = globalLocation,
            hijriDayOffset = hijriDayOffset,
            maghribAdjustmentMinutes = maghribAdjustmentMinutes
        )
    }

    var isPreviewTestActive by rememberSaveable { mutableStateOf(false) }
    var isPlayerOpen by rememberSaveable { mutableStateOf(false) }
    var selectedSourceId by rememberSaveable {
        mutableStateOf(RamadanCrescentManager.getPreferredSource(context).id)
    }
    var reloadNonce by remember { mutableIntStateOf(0) }
    var offlineWarningVisible by remember { mutableStateOf(false) }
    var countdownFinishedLive by remember(sightingTarget.sightingMaghribMillis) {
        mutableStateOf(
            RamadanCrescentManager.computeCountdownTick(sightingTarget).hasSightingArrived
        )
    }
    var isDismissedForYear by remember(sightingTarget.targetHijriYear) {
        mutableStateOf(
            RamadanCrescentManager.isDismissedForYear(context, sightingTarget.targetHijriYear)
        )
    }
    var previewDismissed by remember { mutableStateOf(false) }

    val activeSource = remember(selectedSourceId) {
        CrescentLiveSource.fromId(selectedSourceId)
    }

    val effectiveSightingArrived = isPreviewTestActive || countdownFinishedLive
    val effectiveDismissed = if (isPreviewTestActive) previewDismissed else isDismissedForYear

    val mode = remember(
        isPlayerOpen,
        effectiveSightingArrived,
        effectiveDismissed,
        sightingTarget.isRamadanNow,
        isPreviewTestActive
    ) {
        when {
            isPlayerOpen -> RamadanCardMode.LIVE_PLAYER
            effectiveSightingArrived && !effectiveDismissed -> RamadanCardMode.LIVE_PROMPT
            effectiveSightingArrived && effectiveDismissed -> RamadanCardMode.DISMISSED_STATUS
            sightingTarget.isRamadanNow -> RamadanCardMode.RAMADAN_ACTIVE
            else -> RamadanCardMode.COUNTDOWN
        }
    }

    fun requestOpenLiveStream() {
        if (!RamadanCrescentManager.isDeviceOnline(context)) {
            offlineWarningVisible = true
            onShowToast("البث المباشر يتطلب اتصالاً بالإنترنت")
            return
        }
        offlineWarningVisible = false
        isPlayerOpen = true
    }

    fun dismissLivePrompt() {
        offlineWarningVisible = false
        isPlayerOpen = false
        if (isPreviewTestActive) {
            previewDismissed = true
        } else {
            isDismissedForYear = true
            RamadanCrescentManager.setDismissedForYear(context, sightingTarget.targetHijriYear, true)
        }
    }

    fun selectSource(source: CrescentLiveSource) {
        if (source.id == selectedSourceId) return
        if (!RamadanCrescentManager.isDeviceOnline(context)) {
            offlineWarningVisible = true
            onShowToast("البث المباشر يتطلب اتصالاً بالإنترنت")
            return
        }
        offlineWarningVisible = false
        selectedSourceId = source.id
        RamadanCrescentManager.savePreferredSource(context, source)
    }

    fun openExternalYoutube(source: CrescentLiveSource) {
        if (!RamadanCrescentManager.isDeviceOnline(context)) {
            offlineWarningVisible = true
            onShowToast("البث المباشر يتطلب اتصالاً بالإنترنت")
            return
        }
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(source.watchLiveUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }.onFailure {
            onShowToast("تعذّر فتح رابط يوتيوب")
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ramadan_crescent_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0A4A38),
                            DeepGreen,
                            Color(0xFF063327)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Card Header with Crescent badge + Subtle Developer/Preview Test Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SoftGold.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.6f)),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (mode == RamadanCardMode.LIVE_PLAYER || mode == RamadanCardMode.LIVE_PROMPT) {
                                        Icons.Default.LiveTv
                                    } else {
                                        Icons.Default.NightsStay
                                    },
                                    contentDescription = "هلال رمضان",
                                    tint = Color(0xFFF6DC97),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (sightingTarget.isRamadanNow && !isPreviewTestActive) {
                                    "شهر رمضان المبارك ${sightingTarget.currentHijriYear} هـ"
                                } else {
                                    "العد التنازلي واستطلاع هلال رمضان"
                                },
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = AmiriFontFamily),
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = when {
                                    isPreviewTestActive -> "وضع معاينة البث المباشر (تجريبي)"
                                    sightingTarget.isRamadanNow -> "اليوم ${sightingTarget.currentHijriDay} من رمضان • تقبل الله طاعاتكم"
                                    sightingTarget.isShaban29Today -> "اليوم ٢٩ شعبان • موعد الاستطلاع بعد المغرب (${sightingTarget.sightingMaghribFormatted12})"
                                    else -> "ليلة استطلاع الهلال: ٢٩ شعبان ${sightingTarget.targetHijriYear} هـ بعد المغرب"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFF6DC97).copy(alpha = 0.92f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Subtle Developer / Preview Test Toggle so user can test Countdown -> Live Player anytime
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isPreviewTestActive) SoftGold.copy(alpha = 0.26f) else Color.White.copy(alpha = 0.10f),
                        border = BorderStroke(
                            1.dp,
                            if (isPreviewTestActive) SoftGold.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.22f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .testTag("ramadan_preview_test_toggle")
                            .clickable {
                                val next = !isPreviewTestActive
                                isPreviewTestActive = next
                                previewDismissed = false
                                offlineWarningVisible = false
                                if (!next) {
                                    isPlayerOpen = false
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = "معاينة البث المباشر",
                                tint = Color(0xFFF6DC97),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (isPreviewTestActive) "إيقاف المعاينة" else "معاينة البث",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF6DC97)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Offline alert banner if user attempts to watch live while offline
                AnimatedVisibility(visible = offlineWarningVisible) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF7F1D1D).copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5).copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .testTag("ramadan_live_offline_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WifiOff,
                                    contentDescription = "لا يوجد اتصال بالإنترنت",
                                    tint = Color(0xFFFECACA),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "البث المباشر يتطلب اتصالاً بالإنترنت",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            IconButton(
                                onClick = { offlineWarningVisible = false },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إغلاق التنبيه",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Main Card Body — In-place transition between Countdown -> Live Prompt -> Embedded Video Player
                AnimatedContent(
                    targetState = mode,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                    },
                    label = "ramadan_crescent_mode"
                ) { currentMode ->
                    when (currentMode) {
                        RamadanCardMode.COUNTDOWN -> {
                            RamadanCountdownSection(
                                target = sightingTarget,
                                onCountdownFinished = { countdownFinishedLive = true },
                                onManualOpenLive = { requestOpenLiveStream() }
                            )
                        }

                        RamadanCardMode.LIVE_PROMPT -> {
                            RamadanLivePromptSection(
                                sightingTarget = sightingTarget,
                                isPreviewTest = isPreviewTestActive,
                                onWatchLive = { requestOpenLiveStream() },
                                onDismiss = { dismissLivePrompt() }
                            )
                        }

                        RamadanCardMode.LIVE_PLAYER -> {
                            // Strictly isolated video player: zero 1-second timer dependencies
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                IsolatedCrescentLivePlayer(
                                    embedUrl = activeSource.embedUrl,
                                    sourceName = activeSource.arabicName,
                                    sourceSubtitle = activeSource.subtitle,
                                    reloadNonce = reloadNonce,
                                    onReload = {
                                        if (!RamadanCrescentManager.isDeviceOnline(context)) {
                                            offlineWarningVisible = true
                                            onShowToast("البث المباشر يتطلب اتصالاً بالإنترنت")
                                        } else {
                                            offlineWarningVisible = false
                                            reloadNonce += 1
                                        }
                                    },
                                    onOpenExternalYoutube = { openExternalYoutube(activeSource) },
                                    onClosePlayer = { isPlayerOpen = false },
                                    onShowToast = onShowToast
                                )

                                // Official Live Source Switcher (Dar Al-Ifta default + Saudi Quran TV / Al-Ekhbariya backups)
                                CrescentSourceSwitcherBar(
                                    selectedSource = activeSource,
                                    onSelectSource = { selectSource(it) }
                                )
                            }
                        }

                        RamadanCardMode.DISMISSED_STATUS -> {
                            RamadanDismissedSightingStatusSection(
                                sightingTarget = sightingTarget,
                                isPreviewTest = isPreviewTestActive,
                                onWatchLiveManually = { requestOpenLiveStream() },
                                onResetPreviewPrompt = if (isPreviewTestActive) {
                                    { previewDismissed = false }
                                } else null
                            )
                        }

                        RamadanCardMode.RAMADAN_ACTIVE -> {
                            RamadanActiveStatusSection(
                                sightingTarget = sightingTarget
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Self-contained 1-second Ramadan countdown section.
 * Only active during COUNTDOWN mode so its 1-second ticker NEVER touches or recomposes the live video player.
 */
@Composable
private fun RamadanCountdownSection(
    target: RamadanSightingTarget,
    onCountdownFinished: () -> Unit,
    onManualOpenLive: () -> Unit
) {
    var tick by remember(target.sightingMaghribMillis) {
        mutableStateOf(RamadanCrescentManager.computeCountdownTick(target))
    }

    LaunchedEffect(target.sightingMaghribMillis) {
        while (true) {
            val next = RamadanCrescentManager.computeCountdownTick(target)
            tick = next
            if (next.hasSightingArrived) {
                onCountdownFinished()
                break
            }
            delay(1_000L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ramadan_countdown_section"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 4-tile Days / Hours / Minutes / Seconds countdown grid
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CountdownUnitTile(
                    value = tick.days.toString(),
                    label = "يوم",
                    modifier = Modifier.weight(1f)
                )
                CountdownUnitTile(
                    value = String.format(Locale.US, "%02d", tick.hours),
                    label = "ساعة",
                    modifier = Modifier.weight(1f)
                )
                CountdownUnitTile(
                    value = String.format(Locale.US, "%02d", tick.minutes),
                    label = "دقيقة",
                    modifier = Modifier.weight(1f)
                )
                CountdownUnitTile(
                    value = String.format(Locale.US, "%02d", tick.seconds),
                    label = "ثانية",
                    highlight = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Sighting date metadata bar
        if (target.sightingGregorianDateLabel.isNotBlank()) {
            Text(
                text = "الموعد المتوقع لاستطلاع الهلال: ${target.sightingGregorianDateLabel} • الساعة ${target.sightingMaghribFormatted12}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.82f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // On 29th of Sha'ban: allow opening the live broadcast manually anytime from "متابعة رؤية هلال رمضان"
        if (target.isShaban29Today) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SoftGold.copy(alpha = 0.20f),
                border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.65f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .testTag("shaban29_manual_live_button")
                    .clickable(onClick = onManualOpenLive)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LiveTv,
                            contentDescription = "متابعة رؤية هلال رمضان",
                            tint = Color(0xFFF6DC97),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "متابعة رؤية هلال رمضان",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "افتح البث المباشر الرسمي في أي وقت طوال يوم ٢٩ شعبان",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFF6DC97)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SoftGold
                    ) {
                        Text(
                            text = "بث مباشر",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1A1405),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CountdownUnitTile(
    value: String,
    label: String,
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.Black.copy(alpha = 0.26f),
        border = BorderStroke(
            1.dp,
            if (highlight) SoftGold.copy(alpha = 0.72f) else SoftGold.copy(alpha = 0.32f)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 9.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = if (highlight) Color(0xFFF6DC97) else Color.White,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.80f),
                maxLines = 1
            )
        }
    }
}

/**
 * In-place interactive prompt shown automatically the moment the sighting countdown finishes.
 */
@Composable
private fun RamadanLivePromptSection(
    sightingTarget: RamadanSightingTarget,
    isPreviewTest: Boolean,
    onWatchLive: () -> Unit,
    onDismiss: () -> Unit
) {
    val pulse = rememberInfiniteTransition(label = "live_dot_pulse")
    val dotAlpha by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(750),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_dot_alpha"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.26f),
        border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.65f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ramadan_live_prompt_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444).copy(alpha = dotAlpha))
                )
                Text(
                    text = if (isPreviewTest) "بث مباشر (وضع المعاينة)" else "بث مباشر الآن • ٢٩ شعبان ${sightingTarget.targetHijriYear} هـ",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFF6DC97)
                )
            }

            Text(
                text = "حان وقت استطلاع هلال رمضان • هل ترغب في مشاهدة البث المباشر؟",
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = AmiriFontFamily),
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                lineHeight = 26.sp
            )

            Text(
                text = "بث رسمي متواصل من دار الإفتاء المصرية مع إمكانية التبديل لقناة القرآن الكريم أو الإخبارية.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.84f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onWatchLive,
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SoftGold,
                        contentColor = Color(0xFF1A1405)
                    ),
                    modifier = Modifier
                        .weight(1.35f)
                        .height(48.dp)
                        .testTag("ramadan_watch_live_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "مشاهدة البث المباشر",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(13.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier
                        .weight(0.85f)
                        .height(48.dp)
                        .testTag("ramadan_dismiss_live_button")
                ) {
                    Text(
                        text = "لاحقاً / رفض",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Strictly isolated, memoized embedded 16:9 YouTube live video player.
 *
 * Only loads/reloads when `embedUrl` or `reloadNonce` actually changes — immune to
 * parent recompositions or 1-second timer updates so playback never flickers or cuts out.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun IsolatedCrescentLivePlayer(
    embedUrl: String,
    sourceName: String,
    sourceSubtitle: String,
    reloadNonce: Int,
    onReload: () -> Unit,
    onOpenExternalYoutube: () -> Unit,
    onClosePlayer: () -> Unit,
    onShowToast: (String) -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val loadKey = "$embedUrl#$reloadNonce"

    var isStreamLoading by remember(loadKey) { mutableStateOf(true) }
    var hasLoadError by remember(loadKey) { mutableStateOf(false) }

    // Keep track of the exact loaded key on the WebView so Compose updates NEVER reload an already-playing stream
    val lastLoadedKeyHolder = remember { mutableStateOf<String?>(null) }
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    val fullscreenDialogRef = remember { mutableStateOf<Dialog?>(null) }
    val fullscreenCallbackRef = remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    fun dismissFullscreenVideo() {
        runCatching { fullscreenCallbackRef.value?.onCustomViewHidden() }
        fullscreenCallbackRef.value = null
        runCatching { fullscreenDialogRef.value?.dismiss() }
        fullscreenDialogRef.value = null
    }

    DisposableEffect(Unit) {
        onDispose {
            dismissFullscreenVideo()
            webViewRef.value?.let { wv ->
                runCatching {
                    wv.stopLoading()
                    wv.loadUrl("about:blank")
                    wv.onPause()
                    wv.destroy()
                }
            }
            webViewRef.value = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ramadan_live_video_player_container"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Player header bar (Live indicator + Source title + PiP / Reload / Close controls)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFDC2626)
                ) {
                    Text(
                        text = "مباشر",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sourceName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = sourceSubtitle,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color(0xFFF6DC97).copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
                    IconButton(
                        onClick = {
                            runCatching {
                                val params = PictureInPictureParams.Builder()
                                    .setAspectRatio(Rational(16, 9))
                                    .build()
                                activity.enterPictureInPictureMode(params)
                            }.onFailure {
                                onShowToast("ميزة صورة داخل صورة غير مدعومة على هذا الجهاز")
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("ramadan_live_pip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureInPictureAlt,
                            contentDescription = "صورة داخل صورة",
                            tint = Color(0xFFF6DC97),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onClosePlayer,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("ramadan_live_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق البث المباشر",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Responsive 16:9 Embedded Video Player Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF061611))
                .testTag("ramadan_live_video_frame")
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(android.graphics.Color.BLACK)
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            builtInZoomControls = false
                            displayZoomControls = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            javaScriptCanOpenWindowsAutomatically = false
                            setSupportMultipleWindows(false)
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                isStreamLoading = false
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    isStreamLoading = false
                                    hasLoadError = true
                                }
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onShowCustomView(
                                view: View?,
                                callback: CustomViewCallback?
                            ) {
                                if (view == null || activity == null) {
                                    callback?.onCustomViewHidden()
                                    return
                                }
                                dismissFullscreenVideo()
                                fullscreenCallbackRef.value = callback
                                val dialog = Dialog(
                                    activity,
                                    android.R.style.Theme_Black_NoTitleBar_Fullscreen
                                ).apply {
                                    setCancelable(true)
                                    val container = FrameLayout(activity).apply {
                                        setBackgroundColor(android.graphics.Color.BLACK)
                                        addView(
                                            view,
                                            FrameLayout.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                        )
                                    }
                                    setContentView(container)
                                    window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                                    setOnDismissListener {
                                        val cb = fullscreenCallbackRef.value
                                        fullscreenCallbackRef.value = null
                                        fullscreenDialogRef.value = null
                                        runCatching { cb?.onCustomViewHidden() }
                                    }
                                }
                                fullscreenDialogRef.value = dialog
                                runCatching { dialog.show() }
                            }

                            override fun onHideCustomView() {
                                dismissFullscreenVideo()
                            }
                        }

                        webViewRef.value = this
                        lastLoadedKeyHolder.value = loadKey
                        loadDataWithBaseURL(
                            "https://www.youtube.com",
                            buildResponsiveYoutubeIframeHtml(embedUrl),
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                },
                update = { webView ->
                    // CRITICAL: Never reload or touch WebView unless embedUrl or reloadNonce truly changed
                    if (lastLoadedKeyHolder.value != loadKey) {
                        lastLoadedKeyHolder.value = loadKey
                        hasLoadError = false
                        isStreamLoading = true
                        webView.loadDataWithBaseURL(
                            "https://www.youtube.com",
                            buildResponsiveYoutubeIframeHtml(embedUrl),
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                }
            )

            // Smooth loading skeleton overlay while iframe initializes
            if (isStreamLoading) {
                StreamLoadingSkeletonOverlay()
            }

            // Friendly in-player fallback overlay if network temporarily drops
            if (hasLoadError) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xEE08241C))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = Color(0xFFF6DC97),
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "تعذّر تحميل البث داخل المشغّل",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "يمكنك إعادة التحميل أو فتح البث مباشرة في تطبيق يوتيوب",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Player Action Strip: Reload Stream + Direct Fallback "فتح في يوتيوب"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("ramadan_live_reload_button")
                    .clickable(onClick = onReload)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "إعادة تحميل البث",
                        tint = Color(0xFFF6DC97),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "إعادة تحميل البث",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SoftGold.copy(alpha = 0.22f),
                border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.65f)),
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("ramadan_live_open_youtube_button")
                    .clickable(onClick = onOpenExternalYoutube)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "فتح في يوتيوب",
                        tint = Color(0xFFF6DC97),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "فتح في يوتيوب",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFF6DC97)
                    )
                }
            }
        }
    }
}

@Composable
private fun StreamLoadingSkeletonOverlay() {
    val shimmer = rememberInfiniteTransition(label = "stream_skeleton")
    val alpha by shimmer.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "stream_skeleton_alpha"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A241C).copy(alpha = alpha)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircularProgressIndicator(
                color = SoftGold,
                strokeWidth = 2.6.dp,
                modifier = Modifier.size(30.dp)
            )
            Text(
                text = "جاري تجهيز البث المباشر...",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF6DC97)
            )
        }
    }
}

@Composable
private fun CrescentSourceSwitcherBar(
    selectedSource: CrescentLiveSource,
    onSelectSource: (CrescentLiveSource) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "مصادر البث الرسمية:",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.82f),
            fontWeight = FontWeight.SemiBold
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(CrescentLiveSource.entries, key = { it.id }) { source ->
                val isSelected = source == selectedSource
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) SoftGold else Color.White.copy(alpha = 0.12f),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) SoftGold else Color.White.copy(alpha = 0.24f)
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .testTag("crescent_source_${source.id}")
                        .clickable { onSelectSource(source) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF1A1405),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = source.arabicName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) Color(0xFF1A1405) else Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RamadanDismissedSightingStatusSection(
    sightingTarget: RamadanSightingTarget,
    isPreviewTest: Boolean,
    onWatchLiveManually: () -> Unit,
    onResetPreviewPrompt: (() -> Unit)?
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.Black.copy(alpha = 0.22f),
        border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ramadan_dismissed_status_section")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "متابعة رؤية هلال رمضان ${sightingTarget.targetHijriYear} هـ",
                style = MaterialTheme.typography.titleSmall.copy(fontFamily = AmiriFontFamily),
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFF6DC97)
            )
            Text(
                text = "اللهم أهله علينا باليُمن والإيمان والسلامة والإسلام. يمكنك فتح البث المباشر الرسمي في أي وقت من هنا.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.88f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onWatchLiveManually,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldGreen,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("ramadan_manual_reopen_live_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = null,
                        tint = Color(0xFFF6DC97),
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "مشاهدة البث المباشر",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                if (isPreviewTest && onResetPreviewPrompt != null) {
                    OutlinedButton(
                        onClick = onResetPreviewPrompt,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF6DC97)),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("ramadan_reset_preview_prompt_button")
                    ) {
                        Text(
                            text = "إعادة التنبيه",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RamadanActiveStatusSection(
    sightingTarget: RamadanSightingTarget
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.Black.copy(alpha = 0.22f),
        border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ramadan_active_status_section")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "رمضان مبارك • اليوم ${sightingTarget.currentHijriDay} من رمضان",
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = AmiriFontFamily),
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFF6DC97)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "اللهم تقبل صيامنا وقيامنا وبلغنا ليلة القدر",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.88f)
                )
            }
            Surface(
                shape = CircleShape,
                color = SoftGold.copy(alpha = 0.22f),
                border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.65f)),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${sightingTarget.currentHijriDay}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFF6DC97)
                    )
                }
            }
        }
    }
}

private fun buildResponsiveYoutubeIframeHtml(embedUrl: String): String {
    val safeUrl = embedUrl.replace("\"", "&quot;")
    return """
        <!DOCTYPE html>
        <html lang="ar" dir="rtl">
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
          <style>
            html, body {
              margin: 0;
              padding: 0;
              width: 100%;
              height: 100%;
              background-color: #000000;
              overflow: hidden;
            }
            iframe {
              position: fixed;
              top: 0;
              left: 0;
              width: 100%;
              height: 100%;
              border: 0;
            }
          </style>
        </head>
        <body>
          <iframe
            src="$safeUrl"
            title="البث المباشر لاستطلاع هلال رمضان"
            frameborder="0"
            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share; fullscreen"
            allowfullscreen
            playsinline
          ></iframe>
        </body>
        </html>
    """.trimIndent()
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
