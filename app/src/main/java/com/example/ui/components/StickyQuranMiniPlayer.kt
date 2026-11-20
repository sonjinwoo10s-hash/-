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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.audio.AudioPlayerState
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
    onDownloadMp3: () -> Unit,
    onOpenReciterLibrary: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surahNum = audioState.loadedSurahNumber ?: return

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
                                if (audioState.isBuffering) append(" • جاري التحميل...")
                                else if (audioState.usedFallbackStream && audioState.isPlaying) append(" • بث بديل")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFF6DC97),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDownloadMp3,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("mini_player_download_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "تحميل السورة MP3",
                            tint = Color(0xFFF6DC97),
                            modifier = Modifier.size(20.dp)
                        )
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
                        .clickable(onClick = onTogglePlay),
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
