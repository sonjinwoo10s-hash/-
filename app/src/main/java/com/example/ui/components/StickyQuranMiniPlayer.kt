package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp
import com.example.audio.AudioPlayerState
import com.example.audio.QuranPlaybackSpeeds
import com.example.ui.theme.DeepGreen
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.SoftGold
import java.util.Locale

@Composable
fun StickyQuranMiniPlayer(
    audioState: AudioPlayerState,
    onTogglePlay: () -> Unit,
    onPreviousSurah: () -> Unit,
    onNextSurah: () -> Unit,
    onSeek: (Float) -> Unit,
    onSelectPlaybackSpeed: (Float) -> Unit,
    onDownloadMp3: () -> Unit,
    onOpenReciterLibrary: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isCurrentSurahDownloaded: Boolean = false,
    isCurrentSurahDownloading: Boolean = false
) {
    val surahNum = audioState.loadedSurahNumber ?: return
    val view = LocalView.current
    var showSpeedChips by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("sticky_quran_mini_player"),
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(DeepGreen, EmeraldGreen)))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Top Row: Surah & Reciter Info + Download + Switch Reciter + Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenReciterLibrary)
                ) {
                    Text(
                        text = audioState.loadedSurahName.ifBlank { "سورة رقم $surahNum" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = Color(0xFFF6DC97),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = buildString {
                                append(audioState.selectedReciter.arabicName)
                                if (audioState.isPlayingOffline || isCurrentSurahDownloaded) append(" • أوفلاين ✓")
                                else if (audioState.isBuffering) append(" • جاري التحميل...")
                                else if (audioState.usedFallbackStream && audioState.isPlaying) append(" • بث بديل")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFF6DC97),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Playback Speed Toggle Pill (e.g., 1.0x)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (showSpeedChips || audioState.playbackSpeed != 1.0f) {
                            SoftGold
                        } else {
                            Color.White.copy(alpha = 0.16f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (showSpeedChips || audioState.playbackSpeed != 1.0f) {
                                SoftGold
                            } else {
                                Color.White.copy(alpha = 0.3f)
                            }
                        ),
                        modifier = Modifier
                            .testTag("mini_player_speed_button")
                            .clickable { showSpeedChips = !showSpeedChips }
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = QuranPlaybackSpeeds.formatLabel(audioState.playbackSpeed),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                fontWeight = FontWeight.ExtraBold,
                                color = if (showSpeedChips || audioState.playbackSpeed != 1.0f) {
                                    Color(0xFF1A1405)
                                } else {
                                    Color.White
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDownloadMp3,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("mini_player_download_button")
                    ) {
                        when {
                            isCurrentSurahDownloading -> CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFFF6DC97)
                            )
                            isCurrentSurahDownloaded || audioState.isPlayingOffline -> Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "تم تحميل السورة أوفلاين",
                                tint = Color(0xFF4ADE80),
                                modifier = Modifier.size(20.dp)
                            )
                            else -> Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "تحميل السورة MP3",
                                tint = Color(0xFFF6DC97),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("mini_player_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق المشغل",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Expandable Quick Playback Speed Chip Selector (0.5x .. 2.0x)
            AnimatedVisibility(visible = showSpeedChips) {
                Column(modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "سرعة التلاوة:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF6DC97)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(QuranPlaybackSpeeds.options) { speed ->
                                val selected = audioState.playbackSpeed == speed
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) SoftGold else Color.White.copy(alpha = 0.14f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) SoftGold else Color.White.copy(alpha = 0.25f)
                                    ),
                                    modifier = Modifier
                                        .testTag("mini_speed_chip_${QuranPlaybackSpeeds.formatLabel(speed)}")
                                        .clickable {
                                            onSelectPlaybackSpeed(speed)
                                            showSpeedChips = false
                                        }
                                ) {
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                        Text(
                                            text = QuranPlaybackSpeeds.formatLabel(speed),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (selected) Color(0xFF1A1405) else Color.White,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Playback Controls + Seek Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Previous Surah (in RTL: SkipNext/Previous semantic labels)
                IconButton(
                    onClick = onPreviousSurah,
                    enabled = surahNum > 1,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("mini_player_prev_surah")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "السورة السابقة",
                        tint = if (surahNum > 1) Color.White else Color.White.copy(alpha = 0.35f)
                    )
                }

                // Play / Pause with clean buffering spinner
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(SoftGold)
                        .testTag("mini_player_play_pause")
                        .clickable {
                            performKeyTapHaptic(view)
                            onTogglePlay()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (audioState.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp,
                            color = Color(0xFF1A1405)
                        )
                    } else {
                        Icon(
                            imageVector = if (audioState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (audioState.isPlaying) "إيقاف مؤقت" else "تشغيل",
                            tint = Color(0xFF1A1405),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Next Surah
                IconButton(
                    onClick = onNextSurah,
                    enabled = surahNum < 114,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("mini_player_next_surah")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "السورة التالية",
                        tint = if (surahNum < 114) Color.White else Color.White.copy(alpha = 0.35f)
                    )
                }

                val fraction = if (audioState.durationMs > 0) {
                    (audioState.currentPositionMs.toFloat() / audioState.durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Slider(
                        value = fraction,
                        onValueChange = onSeek,
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFF6DC97),
                            activeTrackColor = Color(0xFFF6DC97),
                            inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                        )
                    )
                }

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        text = "${fmtMs(audioState.currentPositionMs)} / ${fmtMs(audioState.durationMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                }
            }

            if (audioState.errorMessage != null) {
                Text(
                    text = audioState.errorMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFFD599),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

private fun fmtMs(ms: Int): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val m = totalSec / 60
    val s = totalSec % 60
    return String.format(Locale.US, "%02d:%02d", m, s)
}
