package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.local.OfflineIslamicData
import com.example.data.model.CalculatedWakeupTime
import com.example.data.model.FajrAlarmSettings
import com.example.data.model.GuideSection
import com.example.data.model.MuezzinItem
import com.example.sensor.QiblaCompassSensor
import com.example.ui.theme.LocalIslamicColors
import com.example.ui.theme.SoftGold
import com.example.ui.viewmodel.ToolsUiState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlin.math.roundToInt

@Composable
fun ToolsScreen(
    state: ToolsUiState,
    activeMuezzin: MuezzinItem,
    fajrAlarmSettings: FajrAlarmSettings,
    fajrWakeupTime: CalculatedWakeupTime,
    darkMode: Boolean,
    onOpenMuezzinLibrary: () -> Unit,
    onOpenFajrAlarm: () -> Unit,
    onGpsLocation: (Location) -> Unit,
    onHeadingChanged: (Float) -> Unit,
    onToggleCompassSensor: (Boolean) -> Unit,
    onToggleGuide: (String) -> Unit,
    onToggleDarkMode: () -> Unit,
    onResetTracker: () -> Unit,
    onShowToast: (String) -> Unit
) {
    val context = LocalContext.current
    val islamic = LocalIslamicColors.current

    // Live hardware compass sensor lifecycle
    val compassSensor = remember(context) {
        QiblaCompassSensor(context) { azimuth ->
            onHeadingChanged(azimuth)
        }
    }

    DisposableEffect(state.isSensorEnabled) {
        if (state.isSensorEnabled) {
            val started = compassSensor.start()
            if (!started) {
                onToggleCompassSensor(false)
                onShowToast("مستشعر البوصلة غير متوفر على هذا الجهاز، يمكنك استخدام شريط المعايرة")
            }
        } else {
            compassSensor.stop()
        }
        onDispose { compassSensor.stop() }
    }

    @SuppressLint("MissingPermission")
    fun requestCurrentGps() {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()
        onShowToast("جاري تحديد موقعك بدقة...")
        client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    onGpsLocation(location)
                } else {
                    client.lastLocation.addOnSuccessListener { last ->
                        if (last != null) {
                            onGpsLocation(last)
                        } else {
                            onShowToast("تعذّر الحصول على الإحداثيات، تأكد من تفعيل خدمة الموقع")
                        }
                    }
                }
            }
            .addOnFailureListener {
                onShowToast("تعذّر الوصول للموقع حالياً")
            }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            requestCurrentGps()
        } else {
            onShowToast("يرجى السماح بصلاحية الموقع لحساب القبلة من موقعك الحالي")
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tools_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 0A. Smart Fajr Alarm Card
        item(key = "fajr_alarm_entry") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tools_fajr_alarm_card")
                    .clickable(onClick = onOpenFajrAlarm),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    width = if (fajrAlarmSettings.enabled) 1.5.dp else 1.dp,
                    color = if (fajrAlarmSettings.enabled) MaterialTheme.colorScheme.primary else islamic.gold.copy(alpha = 0.55f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(islamic.goldSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = "منبه صلاة الفجر الذكي",
                                tint = islamic.goldText
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "منبه صلاة الفجر الذكي",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (fajrAlarmSettings.enabled) {
                                    "مفعّل • ${fajrWakeupTime.formatted12Arabic} (${fajrAlarmSettings.resolvedChallenge().arabicTitle})"
                                } else {
                                    "تحديات ذكية للاستيقاظ: QR، رياضيات، ذاكرة، أسئلة"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = islamic.textMuted
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "فتح منبه الفجر الذكي",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 0B. Global Muezzin Library Card
        item(key = "muezzin_library_entry") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tools_muezzin_library_card")
                    .clickable(onClick = onOpenMuezzinLibrary),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, islamic.gold.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(islamic.emeraldSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "مكتبة المؤذنين العالمية",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "مكتبة المؤذنين العالمية",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "المؤذن الحالي: ${activeMuezzin.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = islamic.textMuted
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "فتح مكتبة المؤذنين",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 1. Qibla Compass Card
        item(key = "qibla_card") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("qibla_compass_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, islamic.border)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeading(title = "اتجاه القبلة")
                        Button(
                            onClick = {
                                val fineGranted = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                val coarseGranted = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                if (fineGranted || coarseGranted) {
                                    requestCurrentGps()
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = islamic.emeraldSoft,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("detect_location_button")
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تحديد موقعي", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "الموقع الحالي: ${state.locationLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = islamic.textMuted
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    QiblaCompassDial(
                        qiblaAngleDeg = state.qiblaAngleDeg,
                        headingDeg = state.headingDeg
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QiblaStatBox(
                            value = "${state.qiblaAngleDeg.roundToInt()}°",
                            label = "زاوية القبلة من الشمال",
                            modifier = Modifier.weight(1f)
                        )
                        QiblaStatBox(
                            value = "${state.distanceKm} كم",
                            label = "المسافة إلى الكعبة",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "معايرة اتجاه الهاتف",
                            style = MaterialTheme.typography.labelSmall,
                            color = islamic.textMuted
                        )
                        Text(
                            text = "اتجاه الجهاز: ${state.headingDeg.roundToInt()}°",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Slider(
                        value = state.headingDeg,
                        onValueChange = {
                            if (state.isSensorEnabled) onToggleCompassSensor(false)
                            onHeadingChanged(it)
                        },
                        valueRange = 0f..360f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("compass_heading_slider")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    MiniActionPill(
                        icon = Icons.Default.Explore,
                        label = if (state.isSensorEnabled) "إيقاف مستشعر البوصلة" else "تفعيل مستشعر البوصلة الحقيقي",
                        highlighted = state.isSensorEnabled,
                        onClick = { onToggleCompassSensor(!state.isSensorEnabled) }
                    )
                }
            }
        }

        // 2. Step-by-Step Guides Heading
        item(key = "guides_header") {
            SectionHeading(title = "أدلة العبادات الميسّرة")
        }

        items(OfflineIslamicData.worshipGuides, key = { it.id }) { guide ->
            GuideAccordionCard(
                guide = guide,
                expanded = state.expandedGuideIds.contains(guide.id),
                onToggle = { onToggleGuide(guide.id) }
            )
        }

        // 3. Settings Card
        item(key = "settings_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, islamic.border)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeading(title = "إعدادات التطبيق")
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "المظهر الداكن (الوضع الليلي)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Switch(
                            checked = darkMode,
                            onCheckedChange = { onToggleDarkMode() },
                            modifier = Modifier.testTag("settings_dark_mode_switch")
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = islamic.border
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تصفير ورد اليوم",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedButton(
                            onClick = onResetTracker,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("reset_today_tracker_button")
                        ) {
                            Text("إعادة ضبط")
                        }
                    }
                }
            }
        }

        // 4. About & Contact Section (عن التطبيق والتواصل)
        item(key = "about_contact_card") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("about_contact_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, islamic.gold.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        SectionHeading(title = "عن التطبيق والتواصل")
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Respectful Attribution Badge
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = islamic.goldSoft,
                        border = BorderStroke(1.dp, islamic.gold.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = islamic.goldText,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "صُنع هذا التطبيق بواسطة فاعل خير — نسألكم الدعاء",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = islamic.goldText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Styled Green WhatsApp Contact Button
                    Button(
                        onClick = {
                            val opened = runCatching {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://wa.me/6285124189973")
                                ).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                                true
                            }.getOrDefault(false)
                            if (!opened) {
                                onShowToast("تعذّر فتح واتساب، يرجى التأكد من تثبيت المتصفح أو التطبيق")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("whatsapp_contact_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF25D366),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "واتساب",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تواصل معنا عبر واتساب",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QiblaCompassDial(
    qiblaAngleDeg: Float,
    headingDeg: Float
) {
    val islamic = LocalIslamicColors.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val borderColor = islamic.border
    val dialBg = islamic.cardAlt
    val mutedColor = islamic.textMuted
    val animatedDialRotation by animateFloatAsState(targetValue = -headingDeg, label = "compass_rot")

    Box(
        modifier = Modifier.size(230.dp),
        contentAlignment = Alignment.Center
    ) {
        // Rotating compass dial
        Box(
            modifier = Modifier
                .fillMaxSize()
                .rotate(animatedDialRotation),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                // Outer ring
                drawCircle(
                    color = borderColor,
                    radius = radius - 6.dp.toPx(),
                    center = center,
                    style = Stroke(width = 5.dp.toPx())
                )
                // Inner dashed golden circle
                drawCircle(
                    color = dialBg,
                    radius = radius - 16.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = SoftGold,
                    radius = radius - 16.dp.toPx(),
                    center = center,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )
                )

                // Qibla Arrow rotated to qiblaAngleDeg from North
                rotate(degrees = qiblaAngleDeg, pivot = center) {
                    val arrowTipY = center.y - radius + 28.dp.toPx()
                    drawLine(
                        color = primaryColor,
                        start = center,
                        end = Offset(center.x, arrowTipY + 14.dp.toPx()),
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    val tipPath = Path().apply {
                        moveTo(center.x, arrowTipY)
                        lineTo(center.x - 9.dp.toPx(), arrowTipY + 18.dp.toPx())
                        lineTo(center.x + 9.dp.toPx(), arrowTipY + 18.dp.toPx())
                        close()
                    }
                    drawPath(path = tipPath, color = primaryColor)

                    // Stylized Kaaba marker near tip
                    val kaabaSize = 18.dp.toPx()
                    val kaabaTop = arrowTipY + 24.dp.toPx()
                    drawRoundRect(
                        color = Color(0xFF181818),
                        topLeft = Offset(center.x - kaabaSize / 2f, kaabaTop),
                        size = Size(kaabaSize, kaabaSize),
                        cornerRadius = CornerRadius(3.dp.toPx())
                    )
                    drawRoundRect(
                        color = SoftGold,
                        topLeft = Offset(center.x - kaabaSize / 2f, kaabaTop),
                        size = Size(kaabaSize, kaabaSize),
                        cornerRadius = CornerRadius(3.dp.toPx()),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawLine(
                        color = SoftGold,
                        start = Offset(center.x - kaabaSize / 2f, kaabaTop + 5.dp.toPx()),
                        end = Offset(center.x + kaabaSize / 2f, kaabaTop + 5.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                // Center pivot
                drawCircle(color = SoftGold, radius = 8.dp.toPx(), center = center)
                drawCircle(color = Color.White, radius = 3.5.dp.toPx(), center = center)
            }

            // Cardinal letters positioned around dial
            Text(
                text = "ش",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFD9534F),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 18.dp)
            )
            Text(
                text = "ج",
                style = MaterialTheme.typography.labelMedium,
                color = mutedColor,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 18.dp)
            )
            // Note: Inside RTL layout, CenterStart is physical right (East = ق) and CenterEnd is physical left (West = غ)
            Text(
                text = "ق",
                style = MaterialTheme.typography.labelMedium,
                color = mutedColor,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 20.dp)
            )
            Text(
                text = "غ",
                style = MaterialTheme.typography.labelMedium,
                color = mutedColor,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 20.dp)
            )
        }
    }
}

@Composable
private fun QiblaStatBox(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val islamic = LocalIslamicColors.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = islamic.cardAlt,
        border = BorderStroke(1.dp, islamic.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = islamic.textMuted
            )
        }
    }
}

@Composable
private fun GuideAccordionCard(
    guide: GuideSection,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val islamic = LocalIslamicColors.current
    val arrowRot by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "guide_arrow")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("guide_card_${guide.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, islamic.border)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = guide.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "طي" else "توسيع",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.rotate(arrowRot)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    HorizontalDivider(color = islamic.border)
                    Spacer(modifier = Modifier.height(10.dp))

                    if (guide.subtitle != null) {
                        Text(
                            text = guide.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = islamic.textMuted
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    guide.steps.forEach { step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(islamic.emeraldSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = step.badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = step.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = step.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = islamic.textMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
