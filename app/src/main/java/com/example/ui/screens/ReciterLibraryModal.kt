package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.OfflineIslamicData
import com.example.data.model.MoshafOption
import com.example.data.model.ReciterOption
import com.example.ui.theme.DeepGreen
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LocalIslamicColors
import com.example.ui.theme.SoftGold

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReciterLibraryModal(
    featuredReciters: List<ReciterOption>,
    apiReciters: List<ReciterOption>,
    selectedReciter: ReciterOption,
    isLoading: Boolean,
    errorMessage: String?,
    onClose: () -> Unit,
    onRetryFetch: () -> Unit,
    onSelectReciter: (ReciterOption) -> Unit
) {
    BackHandler { onClose() }

    val islamic = LocalIslamicColors.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedRiwayahFilter by remember { mutableStateOf("الكل") }

    val riwayahFilters = listOf("الكل", "حفص", "ورش", "قالون", "مجود")

    val normalizedQuery = remember(searchQuery) {
        OfflineIslamicData.normalizeArabic(searchQuery)
    }

    val displayedApiReciters = remember(apiReciters, normalizedQuery, selectedRiwayahFilter) {
        val source = if (apiReciters.isNotEmpty()) apiReciters else featuredReciters
        source.filter { reciter ->
            val matchQuery = normalizedQuery.isEmpty() ||
                OfflineIslamicData.normalizeArabic(reciter.arabicName).contains(normalizedQuery) ||
                OfflineIslamicData.normalizeArabic(reciter.riwayahName).contains(normalizedQuery)
            val matchRiwayah = selectedRiwayahFilter == "الكل" ||
                reciter.riwayahName.contains(selectedRiwayahFilter) ||
                reciter.moshafList.any { it.name.contains(selectedRiwayahFilter) }
            matchQuery && matchRiwayah
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reciter_library_modal"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            // Emerald Gradient Header
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
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.14f))
                                .testTag("close_reciter_library_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق مكتبة القراء",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "مكتبة قراء القرآن الكريم",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = if (apiReciters.isNotEmpty()) {
                                    "${apiReciters.size} قارئاً من MP3Quran"
                                } else {
                                    "أشهر القراء مع دعم جميع الروايات"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFF6DC97)
                            )
                        }
                    }

                    IconButton(
                        onClick = onRetryFetch,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.14f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "تحديث قائمة القراء",
                            tint = Color(0xFFF6DC97)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // Current Selected Reciter Banner
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = islamic.goldSoft,
                    border = BorderStroke(1.dp, islamic.gold.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = islamic.goldText,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "القارئ المختار حالياً",
                                style = MaterialTheme.typography.labelSmall,
                                color = islamic.textMuted
                            )
                            Text(
                                text = "${selectedReciter.arabicName} • ${selectedReciter.riwayahName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Featured Popular Reciters Pinned Strip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = islamic.gold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "قائمة أشهر القراء",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(featuredReciters, key = { it.id }) { reciter ->
                        val isSelected = selectedReciter.serverUrl == reciter.serverUrl ||
                            selectedReciter.id == reciter.id
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else islamic.border
                            ),
                            modifier = Modifier
                                .testTag("featured_reciter_${reciter.id}")
                                .clickable {
                                    onSelectReciter(reciter)
                                    onClose()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFFF6DC97),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                }
                                Text(
                                    text = reciter.arabicName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar across all MP3Quran reciters
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reciter_search_input"),
                    placeholder = { Text("ابحث في جميع القراء بالاسم (مثال: الدوسري، العجمي، الشريم)...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "بحث", tint = islamic.textMuted)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = islamic.border
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Riwayah Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(riwayahFilters) { filter ->
                        FilterChip(
                            selected = selectedRiwayahFilter == filter,
                            onClick = { selectedRiwayahFilter = filter },
                            label = { Text(filter, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                when {
                    isLoading && apiReciters.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("جاري تحميل قائمة جميع القراء...", color = islamic.textMuted)
                            }
                        }
                    }
                    displayedApiReciters.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("لا يوجد قارئ مطابق لبحثك", color = islamic.textMuted)
                                if (errorMessage != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = onRetryFetch) {
                                        Text("إعادة تحميل القائمة")
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 20.dp)
                        ) {
                            items(displayedApiReciters, key = { it.id }) { reciter ->
                                ReciterListCard(
                                    reciter = reciter,
                                    selectedReciter = selectedReciter,
                                    onSelect = { chosen ->
                                        onSelectReciter(chosen)
                                        onClose()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReciterListCard(
    reciter: ReciterOption,
    selectedReciter: ReciterOption,
    onSelect: (ReciterOption) -> Unit
) {
    val islamic = LocalIslamicColors.current
    val isCurrentReciter = selectedReciter.id == reciter.id ||
        selectedReciter.serverUrl == reciter.serverUrl ||
        reciter.moshafList.any { it.server == selectedReciter.serverUrl }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(reciter) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentReciter) islamic.emeraldSoft else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isCurrentReciter) 1.5.dp else 1.dp,
            color = if (isCurrentReciter) MaterialTheme.colorScheme.primary else islamic.border
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCurrentReciter) MaterialTheme.colorScheme.primary else islamic.emeraldSoft
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCurrentReciter) Icons.Default.Check else Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = if (isCurrentReciter) Color.White else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = reciter.arabicName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = reciter.riwayahName,
                            style = MaterialTheme.typography.bodySmall,
                            color = islamic.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Show Riwayah / Moshaf selector chips when reciter has multiple recordings
            if (reciter.moshafList.size > 1) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    reciter.moshafList.forEach { moshaf: MoshafOption ->
                        val moshafSelected = selectedReciter.serverUrl == moshaf.server
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (moshafSelected) SoftGold else islamic.cardAlt,
                            border = BorderStroke(
                                1.dp,
                                if (moshafSelected) islamic.gold else islamic.border
                            ),
                            modifier = Modifier.clickable {
                                onSelect(
                                    reciter.copy(
                                        id = "${reciter.id}_m${moshaf.id}",
                                        riwayahName = moshaf.name,
                                        serverUrl = moshaf.server
                                    )
                                )
                            }
                        ) {
                            Text(
                                text = "${moshaf.name} (${moshaf.surahTotal})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (moshafSelected) Color(0xFF1A1405) else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
