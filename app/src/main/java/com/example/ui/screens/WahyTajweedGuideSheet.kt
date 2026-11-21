package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quran.TajweedEngine
import com.example.quran.TajweedEngine.appendUthmaniWithTajweed
import com.example.quran.TajweedRule
import com.example.ui.theme.AmiriQuranFontFamily
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LocalIslamicColors
import com.example.ui.theme.MushafCreamPaper
import com.example.ui.theme.NightReadPaper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WahyTajweedGuideSheet(
    tajweedColorsEnabled: Boolean,
    isNightMode: Boolean,
    onToggleTajweedColors: () -> Unit,
    onDismiss: () -> Unit
) {
    val islamic = LocalIslamicColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val rules = remember { TajweedRule.entries }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .testTag("tajweed_guide_bottom_sheet")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = islamic.emeraldSoft,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "دليل التجويد والنطق الصحيح",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "تعرّف على ألوان أحكام التلاوة وكيفية تطبيقها أثناء القراءة",
                            style = MaterialTheme.typography.labelSmall,
                            color = islamic.textMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick inline switch for Tajweed Colors in the Mushaf
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (tajweedColorsEnabled) islamic.emeraldSoft else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(
                    1.dp,
                    if (tajweedColorsEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else islamic.border
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = if (tajweedColorsEnabled) MaterialTheme.colorScheme.primary else islamic.textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "تلوين أحكام التجويد في المصحف",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (tajweedColorsEnabled) "مفعّل الآن على جميع صفحات المصحف" else "متوقف — النص باللون الموحّد",
                                style = MaterialTheme.typography.labelSmall,
                                color = islamic.textMuted
                            )
                        }
                    }

                    Switch(
                        checked = tajweedColorsEnabled,
                        onCheckedChange = { onToggleTajweedColors() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldGreen
                        ),
                        modifier = Modifier.testTag("guide_tajweed_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 22.dp)
            ) {
                items(rules, key = { it.name }) { rule ->
                    TajweedRuleGuideCard(
                        rule = rule,
                        isNightMode = isNightMode
                    )
                }
            }
        }
    }
}

@Composable
private fun TajweedRuleGuideCard(
    rule: TajweedRule,
    isNightMode: Boolean
) {
    val islamic = LocalIslamicColors.current
    val ruleColor = rule.colorForMode(isNightMode)
    val examplePaperBg = if (isNightMode) NightReadPaper else MushafCreamPaper
    val exampleBaseText = if (isNightMode) Color(0xFFEDE8DC) else Color(0xFF1F1B14)

    val colorizedExample = remember(rule, isNightMode) {
        buildAnnotatedString {
            TajweedEngine.run {
                appendUthmaniWithTajweed(
                    text = rule.quranExample,
                    tajweedEnabled = true,
                    isNightMode = isNightMode,
                    baseTextColor = exampleBaseText
                )
            }
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
        ),
        border = BorderStroke(1.dp, ruleColor.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tajweed_rule_card_${rule.name.lowercase()}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Rule Header: Color Dot + Arabic Title + Timing/Behavior Badge
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
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(ruleColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = rule.arabicTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = ruleColor
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ruleColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = rule.badgeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ruleColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Pronunciation Explanation
            Text(
                text = rule.pronunciationGuide,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Involved Letters
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.7.dp, islamic.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = ruleColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "الحروف: ${rule.involvedLetters}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = islamic.textMuted
                    )
                }
            }

            // Color-Coded Quranic Example Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = examplePaperBg,
                border = BorderStroke(1.dp, ruleColor.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = colorizedExample,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = AmiriQuranFontFamily,
                            fontSize = 21.sp,
                            lineHeight = 36.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    HorizontalDivider(
                        color = ruleColor.copy(alpha = 0.18f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Text(
                        text = "مثال قرآني: ${rule.exampleReference}",
                        style = MaterialTheme.typography.labelSmall,
                        color = islamic.textMuted
                    )
                }
            }
        }
    }
}
