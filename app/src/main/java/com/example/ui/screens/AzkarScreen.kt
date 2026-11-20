package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import com.example.util.VibrationHelper
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.OfflineIslamicData
import com.example.data.model.TasbihState
import com.example.data.model.ZikrItem
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.DeepGreen
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LocalIslamicColors
import com.example.ui.theme.SoftGold
import com.example.ui.viewmodel.AzkarUiState

@Composable
fun AzkarScreen(
    state: AzkarUiState,
    tasbihState: TasbihState,
    onSelectSubTab: (Int) -> Unit,
    onSelectCategory: (String) -> Unit,
    onDecrementZikr: (String) -> Unit,
    onResetCategory: () -> Unit,
    onIncrementTasbih: () -> Unit,
    onSetTasbihPhrase: (String) -> Unit,
    onSetTasbihTarget: (Int) -> Unit,
    onResetTasbihCurrent: () -> Unit,
    onResetTasbihLifetime: () -> Unit
) {
    val islamic = LocalIslamicColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("azkar_screen")
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Segmented Sub-Tab Switcher
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, islamic.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SubTabButton(
                    label = "الأذكار والأدعية",
                    selected = state.subTabIndex == 0,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("subtab_azkar"),
                    onClick = { onSelectSubTab(0) }
                )
                SubTabButton(
                    label = "السبحة الإلكترونية",
                    selected = state.subTabIndex == 1,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("subtab_tasbih"),
                    onClick = { onSelectSubTab(1) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (state.subTabIndex == 0) {
            AzkarListSubView(
                state = state,
                onSelectCategory = onSelectCategory,
                onDecrementZikr = onDecrementZikr,
                onResetCategory = onResetCategory
            )
        } else {
            ElectronicTasbihSubView(
                tasbihState = tasbihState,
                onIncrement = onIncrementTasbih,
                onSetPhrase = onSetTasbihPhrase,
                onSetTarget = onSetTasbihTarget,
                onResetCurrent = onResetTasbihCurrent,
                onResetLifetime = onResetTasbihLifetime
            )
        }
    }
}

@Composable
private fun SubTabButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "subtab_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else LocalIslamicColors.current.textMuted,
        label = "subtab_text"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
private fun AzkarListSubView(
    state: AzkarUiState,
    onSelectCategory: (String) -> Unit,
    onDecrementZikr: (String) -> Unit,
    onResetCategory: () -> Unit
) {
    val islamic = LocalIslamicColors.current
    val haptic = LocalHapticFeedback.current
    val categories = remember { OfflineIslamicData.azkarCategories.keys.toList() }
    val currentItems = OfflineIslamicData.azkarCategories[state.selectedCategory].orEmpty()
    val completedCount = currentItems.count { (state.remainingCounts[it.id] ?: it.requiredCount) == 0 }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(categories) { cat ->
            FilterChip(
                selected = cat == state.selectedCategory,
                onClick = { onSelectCategory(cat) },
                label = { Text(cat, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White
                )
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = islamic.emeraldSoft
        ) {
            Text(
                text = "$completedCount / ${currentItems.size} مكتمل",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }

        MiniActionPill(
            icon = Icons.Default.Refresh,
            label = "إعادة تصفير القسم",
            onClick = onResetCategory
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 18.dp)
    ) {
        items(currentItems, key = { it.id }) { zikr ->
            val remaining = state.remainingCounts[zikr.id] ?: zikr.requiredCount
            ZikrInteractiveCard(
                zikr = zikr,
                remaining = remaining,
                onTap = {
                    if (remaining > 0) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDecrementZikr(zikr.id)
                    }
                }
            )
        }
    }
}

@Composable
private fun ZikrInteractiveCard(
    zikr: ZikrItem,
    remaining: Int,
    onTap: () -> Unit
) {
    val islamic = LocalIslamicColors.current
    val isCompleted = remaining == 0
    val progress = ((zikr.requiredCount - remaining).toFloat() / zikr.requiredCount.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "zikr_ring")
    val cardBg by animateColorAsState(
        targetValue = if (isCompleted) islamic.emeraldSoft else MaterialTheme.colorScheme.surface,
        label = "zikr_card_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isCompleted) MaterialTheme.colorScheme.primary else islamic.border,
        label = "zikr_card_border"
    )
    val ringColor = if (isCompleted) islamic.gold else MaterialTheme.colorScheme.primary
    val trackColor = islamic.border

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("zikr_card_${zikr.id}")
            .clickable(onClick = onTap),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = zikr.text,
                fontFamily = AmiriFontFamily,
                fontSize = 21.sp,
                lineHeight = 38.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = islamic.border)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = zikr.virtue,
                    style = MaterialTheme.typography.bodySmall,
                    color = islamic.textMuted,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Smooth Circular Progress Ring
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round)
                        drawCircle(color = trackColor, style = stroke)
                        drawArc(
                            color = ringColor,
                            startAngle = -90f,
                            sweepAngle = animatedProgress * 360f,
                            useCenter = false,
                            style = stroke
                        )
                    }
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "مكتمل",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = remaining.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ElectronicTasbihSubView(
    tasbihState: TasbihState,
    onIncrement: () -> Unit,
    onSetPhrase: (String) -> Unit,
    onSetTarget: (Int) -> Unit,
    onResetCurrent: () -> Unit,
    onResetLifetime: () -> Unit
) {
    val islamic = LocalIslamicColors.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var phraseMenuOpen by remember { mutableStateOf(false) }
    var targetMenuOpen by remember { mutableStateOf(false) }
    var customPhraseInput by remember { mutableStateOf("") }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tasbih_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TasbihStatCard("العدد الحالي", tasbihState.current.toString(), Modifier.weight(1f))
            TasbihStatCard("الدورات", tasbihState.rounds.toString(), Modifier.weight(1f))
            TasbihStatCard("الإجمالي الكلي", tasbihState.lifetime.toString(), Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
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
                // Phrase & Target dropdown selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = islamic.cardAlt,
                            border = BorderStroke(1.dp, islamic.border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { phraseMenuOpen = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "اختر الذكر",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                        DropdownMenu(
                            expanded = phraseMenuOpen,
                            onDismissRequest = { phraseMenuOpen = false }
                        ) {
                            OfflineIslamicData.tasbihPhrases.forEach { phrase ->
                                DropdownMenuItem(
                                    text = { Text(phrase) },
                                    onClick = {
                                        phraseMenuOpen = false
                                        onSetPhrase(phrase)
                                    }
                                )
                            }
                        }
                    }

                    Box {
                        val targetLabel = if (tasbihState.target == 0) "مفتوح ∞" else "الهدف: ${tasbihState.target}"
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = islamic.cardAlt,
                            border = BorderStroke(1.dp, islamic.border),
                            modifier = Modifier.clickable { targetMenuOpen = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = targetLabel, style = MaterialTheme.typography.bodySmall)
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                        DropdownMenu(
                            expanded = targetMenuOpen,
                            onDismissRequest = { targetMenuOpen = false }
                        ) {
                            listOf(33 to "33", 99 to "99", 100 to "100", 0 to "مفتوح ∞").forEach { (valInt, lbl) ->
                                DropdownMenuItem(
                                    text = { Text(lbl) },
                                    onClick = {
                                        targetMenuOpen = false
                                        onSetTarget(valInt)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Custom Zikr phrase input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customPhraseInput,
                        onValueChange = { customPhraseInput = it },
                        placeholder = { Text("أو اكتب ذكراً مخصصاً...", fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_zikr_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = islamic.border
                        )
                    )
                    Button(
                        onClick = {
                            if (customPhraseInput.isNotBlank()) {
                                onSetPhrase(customPhraseInput)
                                customPhraseInput = ""
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = islamic.emeraldSoft,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp)
                    ) {
                        Text("تعيين", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = tasbihState.phrase,
                    fontFamily = AmiriFontFamily,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Large Tactile Tap Button
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .scale(buttonScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF178566), EmeraldGreen, DeepGreen)
                            )
                        )
                        .border(6.dp, SoftGold.copy(alpha = 0.55f), CircleShape)
                        .testTag("tasbih_tap_button")
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            val completesRound = tasbihState.target > 0 && (tasbihState.current + 1) >= tasbihState.target
                            if (VibrationHelper.isVibrationSupported(context)) {
                                if (completesRound) {
                                    VibrationHelper.vibrateRoundComplete(context)
                                } else {
                                    VibrationHelper.vibrateShortTick(context)
                                }
                            }
                            onIncrement()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = tasbihState.current.toString(),
                            fontSize = 52.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = if (tasbihState.target > 0) {
                                "اضغط للتسبيح (${tasbihState.current} / ${tasbihState.target})"
                            } else {
                                "تسبيح مفتوح ∞"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFF4D88B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onResetCurrent,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("tasbih_reset_current")
                    ) {
                        Text("تصفير العداد")
                    }
                    Button(
                        onClick = onResetLifetime,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = islamic.emeraldSoft,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("tasbih_reset_lifetime")
                    ) {
                        Text("تصفير الإجمالي", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TasbihStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    val islamic = LocalIslamicColors.current
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, islamic.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
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
