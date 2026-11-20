package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.FajrAlarmData
import com.example.data.model.CalculatedWakeupTime
import com.example.data.model.FajrAlarmSettings
import com.example.data.model.FajrAlarmSound
import com.example.data.model.FajrChallengeType
import com.example.data.model.MathDifficulty
import com.example.data.model.MemoryGridSize
import com.example.ui.components.QrCameraScannerBox
import com.example.ui.theme.DeepGreen
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LocalIslamicColors
import com.example.ui.theme.SoftGold
import com.example.util.QrCodeUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FajrAlarmSettingsModal(
    settings: FajrAlarmSettings,
    wakeupTime: CalculatedWakeupTime,
    onClose: () -> Unit,
    onUpdateSettings: (FajrAlarmSettings) -> Unit,
    onTestAlarmNow: () -> Unit,
    onShowToast: (String) -> Unit
) {
    BackHandler { onClose() }

    val context = LocalContext.current
    val islamic = LocalIslamicColors.current
    var customOffsetInput by remember(settings.offsetMinutesBeforeFajr) {
        mutableStateOf(settings.offsetMinutesBeforeFajr.toString())
    }
    var qrInputText by remember(settings.targetQrCode) { mutableStateOf(settings.targetQrCode) }
    var showQrCaptureScanner by remember { mutableStateOf(false) }

    val qrBitmap = remember(settings.targetQrCode) {
        QrCodeUtils.generateQrBitmap(settings.targetQrCode, sizePx = 420)
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("fajr_alarm_settings_modal"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(DeepGreen, EmeraldGreen)))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SoftGold.copy(alpha = 0.22f))
                                .border(1.dp, SoftGold.copy(alpha = 0.55f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = Color(0xFFF6DC97),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "منبه صلاة الفجر الذكي",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "استيقظ لصلاة الفجر بتحدٍ ذكي يمنع العودة للنوم",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_fajr_alarm_modal")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = Color.White
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Master Toggle + Dynamic Calculated Wake-up Time + Test Button
                item(key = "alarm_hero") {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.5.dp, if (settings.enabled) MaterialTheme.colorScheme.primary else islamic.border)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "تفعيل منبه الفجر الذكي",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (settings.enabled) "المنبه مفعّل ومضبوط تلقائياً مع موعد الفجر" else "المنبه متوقف حالياً",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = islamic.textMuted
                                    )
                                }
                                Switch(
                                    checked = settings.enabled,
                                    onCheckedChange = { enabled ->
                                        onUpdateSettings(settings.copy(enabled = enabled))
                                    },
                                    modifier = Modifier.testTag("fajr_alarm_enable_switch")
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Calculated Wake-up Time Banner
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = islamic.emeraldSoft,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "وقت الاستيقاظ المحسوب اليوم",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = islamic.textMuted
                                        )
                                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                            Text(
                                                text = wakeupTime.formatted12Arabic,
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "أذان الفجر اليوم",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = islamic.textMuted
                                        )
                                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                            Text(
                                                text = wakeupTime.fajrFormatted12Arabic,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = islamic.goldText
                                            )
                                        }
                                        Text(
                                            text = "قبل الفجر بـ ${settings.offsetMinutesBeforeFajr} دقيقة",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = islamic.textMuted
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Prominent Test Alarm CTA
                            Button(
                                onClick = onTestAlarmNow,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("test_fajr_alarm_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SoftGold,
                                    contentColor = Color(0xFF1A1405)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تجربة المنبه الآن",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }

                // 2. Wake-up Offset Before Fajr
                item(key = "offset_picker") {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, islamic.border)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SectionHeading(title = "وقت التنبيه قبل أذان الفجر")
                            Spacer(modifier = Modifier.height(10.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FajrAlarmData.quickOffsetPresets.forEach { mins ->
                                    val selected = settings.offsetMinutesBeforeFajr == mins
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            customOffsetInput = mins.toString()
                                            onUpdateSettings(settings.copy(offsetMinutesBeforeFajr = mins))
                                        },
                                        label = { Text("$mins دقيقة", fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = Color.White
                                        ),
                                        modifier = Modifier.testTag("fajr_offset_$mins")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = customOffsetInput,
                                    onValueChange = { raw ->
                                        val digits = raw.filter { it.isDigit() }.take(3)
                                        customOffsetInput = digits
                                        digits.toIntOrNull()?.let { value ->
                                            if (value in 0..180) {
                                                onUpdateSettings(settings.copy(offsetMinutesBeforeFajr = value))
                                            }
                                        }
                                    },
                                    label = { Text("وقت مخصص بالدقائق (0 - 180)") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("fajr_custom_offset_input")
                                )
                            }
                        }
                    }
                }

                // 3. Alarm Sound Selector
                item(key = "sound_picker") {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, islamic.border)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SectionHeading(title = "صوت المنبه (يتكرر حتى إتمام التحدي)")
                            Spacer(modifier = Modifier.height(10.dp))

                            FajrAlarmSound.entries.forEach { sound ->
                                val selected = settings.resolvedSound() == sound
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .testTag("fajr_sound_${sound.name}")
                                        .clickable {
                                            onUpdateSettings(settings.copy(soundType = sound.name))
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selected) islamic.emeraldSoft else islamic.cardAlt,
                                    border = BorderStroke(
                                        1.2.dp,
                                        if (selected) MaterialTheme.colorScheme.primary else islamic.border
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Icon(
                                                imageVector = Icons.Default.VolumeUp,
                                                contentDescription = null,
                                                tint = if (selected) MaterialTheme.colorScheme.primary else islamic.textMuted,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = sound.arabicLabel,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        if (selected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "مختار",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Dismissal Challenge Selector (4 Challenges)
                item(key = "challenge_selector") {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, islamic.border)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SectionHeading(title = "تحدي إيقاف المنبه")
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "لن يُغلق المنبه إلا بعد إتمام التحدي المختار بنجاح:",
                                style = MaterialTheme.typography.bodySmall,
                                color = islamic.textMuted
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            FajrChallengeType.entries.forEach { challenge ->
                                val selected = settings.resolvedChallenge() == challenge
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .testTag("fajr_challenge_${challenge.name}")
                                        .clickable {
                                            onUpdateSettings(settings.copy(challengeType = challenge.name))
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selected) islamic.goldSoft else islamic.cardAlt,
                                    border = BorderStroke(
                                        1.5.dp,
                                        if (selected) islamic.gold else islamic.border
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (selected) islamic.gold.copy(alpha = 0.25f)
                                                    else islamic.emeraldSoft
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = challengeIcon(challenge),
                                                contentDescription = null,
                                                tint = if (selected) islamic.goldText else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = challenge.arabicTitle,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = challenge.arabicDescription,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = islamic.textMuted
                                            )
                                        }
                                        if (selected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "مختار",
                                                tint = islamic.goldText
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Per-Challenge Configuration Panel
                item(key = "challenge_config") {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, islamic.gold.copy(alpha = 0.45f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            when (settings.resolvedChallenge()) {
                                FajrChallengeType.QR_CODE -> {
                                    SectionHeading(title = "إعدادات رمز QR للطباعة أو المسح")
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "اطبع الرمز التالي وضعه في مكان بعيد عن السرير (مثل قرب مكان الوضوء)، أو امسح أي رمز QR متوفر لديك لحفظه:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = islamic.textMuted
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (qrBitmap != null) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = Color.White,
                                                border = BorderStroke(2.dp, islamic.gold),
                                                shadowElevation = 2.dp
                                            ) {
                                                Image(
                                                    bitmap = qrBitmap.asImageBitmap(),
                                                    contentDescription = "رمز QR لمنبه الفجر",
                                                    modifier = Modifier
                                                        .size(185.dp)
                                                        .padding(10.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "قيمة الرمز المحفوظ: ${settings.targetQrCode}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = qrInputText,
                                        onValueChange = {
                                            qrInputText = it
                                            if (it.isNotBlank()) {
                                                onUpdateSettings(settings.copy(targetQrCode = it.trim()))
                                            }
                                        },
                                        label = { Text("نص رمز QR المستهدف") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { showQrCaptureScanner = !showQrCaptureScanner },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(if (showQrCaptureScanner) "إخفاء الكاميرا" else "مسح رمز بالكاميرا")
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                val fresh = QrCodeUtils.generateRandomFajrQrValue()
                                                qrInputText = fresh
                                                onUpdateSettings(settings.copy(targetQrCode = fresh))
                                                onShowToast("تم توليد رمز QR جديد")
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("توليد رمز جديد")
                                        }

                                        if (qrBitmap != null) {
                                            OutlinedButton(
                                                onClick = {
                                                    val saved = QrCodeUtils.saveQrBitmapToGallery(context, qrBitmap)
                                                    onShowToast(
                                                        if (saved) "تم حفظ صورة رمز QR للطباعة في المعرض"
                                                        else "تعذّر حفظ الصورة حالياً"
                                                    )
                                                },
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("حفظ للطباعة")
                                            }
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                copyTextToClipboard(context, settings.targetQrCode)
                                                onShowToast("تم نسخ نص الرمز")
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("نسخ الرمز")
                                        }
                                    }

                                    if (showQrCaptureScanner) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        QrCameraScannerBox(
                                            modifier = Modifier.height(220.dp),
                                            onQrCodeScanned = { scanned ->
                                                qrInputText = scanned
                                                onUpdateSettings(settings.copy(targetQrCode = scanned))
                                                showQrCaptureScanner = false
                                                onShowToast("تم حفظ رمز QR بنجاح: $scanned")
                                            }
                                        )
                                    }
                                }

                                FajrChallengeType.MATH -> {
                                    SectionHeading(title = "إعدادات المسائل الرياضية")
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "مستوى الصعوبة:",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        MathDifficulty.entries.forEach { diff ->
                                            val sel = settings.resolvedMathDifficulty() == diff
                                            FilterChip(
                                                selected = sel,
                                                onClick = { onUpdateSettings(settings.copy(mathDifficulty = diff.name)) },
                                                label = { Text(diff.arabicLabel, fontWeight = FontWeight.Bold) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "عدد المسائل المطلوب حلها: ${settings.mathProblemCount} مسائل",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf(1, 2, 3, 4, 5).forEach { count ->
                                            val sel = settings.mathProblemCount == count
                                            FilterChip(
                                                selected = sel,
                                                onClick = { onUpdateSettings(settings.copy(mathProblemCount = count)) },
                                                label = { Text("$count", fontWeight = FontWeight.Bold) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = islamic.gold,
                                                    selectedLabelColor = Color(0xFF1A1405)
                                                )
                                            )
                                        }
                                    }
                                }

                                FajrChallengeType.MEMORY -> {
                                    SectionHeading(title = "إعدادات لعبة الذاكرة")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "تومض المربعات المستهدفة لمدة 3 ثوانٍ ثم يجب الضغط على نفس المواقع لإيقاف المنبه:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = islamic.textMuted
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        MemoryGridSize.entries.forEach { grid ->
                                            val sel = settings.resolvedMemoryGrid() == grid
                                            FilterChip(
                                                selected = sel,
                                                onClick = { onUpdateSettings(settings.copy(memoryGridSize = grid.name)) },
                                                label = {
                                                    Text(
                                                        "${grid.arabicLabel} (${grid.targetCount} مربعات)",
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }
                                }

                                FajrChallengeType.TRIVIA -> {
                                    SectionHeading(title = "تحدي الأسئلة العشوائية")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "تحدٍ معرفي ممتع يضم بنكاً غنياً من الأسئلة في التاريخ الإسلامي، والتاريخ المصري والعام، والرياضيات والمنطق السريع. يجب الإجابة عن 3 أسئلة صحيحة متتالية لإيقاف المنبه.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = islamic.textMuted,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom test button spacer
                item(key = "bottom_test_btn") {
                    Button(
                        onClick = onTestAlarmNow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تشغيل تجربة المنبه والتحدي الآن",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun challengeIcon(type: FajrChallengeType): ImageVector = when (type) {
    FajrChallengeType.QR_CODE -> Icons.Default.QrCode2
    FajrChallengeType.MATH -> Icons.Default.Calculate
    FajrChallengeType.MEMORY -> Icons.Default.GridView
    FajrChallengeType.TRIVIA -> Icons.Default.Quiz
}
