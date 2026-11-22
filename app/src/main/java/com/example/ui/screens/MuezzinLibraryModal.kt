package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.OfflineIslamicData
import com.example.data.model.AdhanPlaybackStatus
import com.example.data.model.AdhanPlayerState
import com.example.data.model.MuezzinItem
import com.example.data.model.MuezzinRegion
import com.example.ui.theme.DeepGreen
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LocalIslamicColors
import com.example.ui.theme.SoftGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun MuezzinLibraryModal(
    allMuezzins: List<MuezzinItem>,
    defaultMuezzinId: String,
    fajrMuezzinId: String,
    useDedicatedFajrAdhan: Boolean,
    perPrayerMuezzinEnabled: Boolean,
    perPrayerMuezzinIds: Map<String, String>,
    favoriteIds: Set<String>,
    adhanState: AdhanPlayerState,
    isSyncingRepo: Boolean,
    repoStatusMessage: String?,
    cachedAdhanIds: Set<String>,
    downloadingMuezzinIds: Set<String>,
    onClose: () -> Unit,
    onSyncRepo: () -> Unit,
    onDownloadOffline: (MuezzinItem) -> Unit,
    onTogglePreview: (MuezzinItem) -> Unit,
    onSwitchFallbackStream: () -> Unit,
    onRetryAdhan: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onSetDefaultMuezzin: (MuezzinItem) -> Unit,
    onSetFajrMuezzin: (MuezzinItem) -> Unit,
    onToggleDedicatedFajr: (Boolean) -> Unit,
    onTogglePerPrayerMuezzin: (Boolean) -> Unit,
    onSetMuezzinForPrayer: (prayerKey: String, muezzin: MuezzinItem) -> Unit,
    onAddCustomFromUrl: (name: String, url: String, isFajr: Boolean) -> Unit,
    onAddCustomFromFile: (name: String, uri: Uri, isFajr: Boolean) -> Unit,
    onOverrideAudioWithUrl: (muezzin: MuezzinItem, newUrl: String) -> Unit,
    onOverrideAudioWithFile: (muezzin: MuezzinItem, uri: Uri, displayName: String) -> Unit,
    onResetAudioToDefault: (muezzin: MuezzinItem) -> Unit,
    onDeleteCustomMuezzin: (MuezzinItem) -> Unit
) {
    BackHandler { onClose() }

    val islamic = LocalIslamicColors.current
    var searchQuery by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    var selectedRegion by remember { mutableStateOf(MuezzinRegion.ALL) }
    var selectedTargetPrayerKey by remember { mutableStateOf("Fajr") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAudioMuezzin by remember { mutableStateOf<MuezzinItem?>(null) }

    // 300ms debounce so search stays smooth on low-end devices
    LaunchedEffect(searchQuery) {
        delay(300L)
        debouncedQuery = searchQuery
    }

    val normalizedQuery = remember(debouncedQuery) {
        OfflineIslamicData.normalizeArabic(debouncedQuery)
    }

    val filteredMuezzins = remember(allMuezzins, selectedRegion, normalizedQuery, favoriteIds) {
        allMuezzins.filter { item ->
            val matchRegion = when (selectedRegion) {
                MuezzinRegion.ALL -> true
                MuezzinRegion.FAVORITES -> favoriteIds.contains(item.id)
                MuezzinRegion.FAJR -> item.isFajr || item.regionEnum() == MuezzinRegion.FAJR
                MuezzinRegion.CUSTOM -> item.isCustom
                else -> item.regionEnum() == selectedRegion
            }
            val matchSearch = normalizedQuery.isEmpty() ||
                OfflineIslamicData.normalizeArabic(item.name).contains(normalizedQuery) ||
                OfflineIslamicData.normalizeArabic(item.subtitle).contains(normalizedQuery) ||
                OfflineIslamicData.normalizeArabic(item.regionEnum().label).contains(normalizedQuery)
            matchRegion && matchSearch
        }
    }

    // Paginate Muezzin list: render 25 items initially and auto-load +25 on scroll
    var visibleCount by remember(selectedRegion, normalizedQuery, allMuezzins.size) {
        mutableIntStateOf(25)
    }
    val pagedMuezzins = remember(filteredMuezzins, visibleCount) {
        filteredMuezzins.take(visibleCount)
    }
    val listState = rememberLazyListState()
    LaunchedEffect(listState, filteredMuezzins.size, visibleCount) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .collect { lastVisible ->
                val totalVisibleItems = listState.layoutInfo.totalItemsCount
                if (visibleCount < filteredMuezzins.size && totalVisibleItems > 0 && lastVisible >= totalVisibleItems - 5) {
                    visibleCount = (visibleCount + 25).coerceAtMost(filteredMuezzins.size)
                }
            }
    }

    val activeDefaultMuezzin = allMuezzins.find { it.id == defaultMuezzinId }
    val activeFajrMuezzin = allMuezzins.find { it.id == fajrMuezzinId }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("muezzin_library_modal"),
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
                                .testTag("close_muezzin_library_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق المكتبة",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "مكتبة المؤذنين العالمية",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "${allMuezzins.size} مؤذناً • ${allMuezzins.count { it.hasAudioSource() }} بصوت جاهز للتشغيل",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFF6DC97)
                            )
                        }
                    }

                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SoftGold,
                            contentColor = Color(0xFF1A1405)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("open_add_custom_muezzin_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "أضف مؤذنك",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Active playback global status bar if currently loading/playing/error
            AnimatedVisibility(visible = adhanState.status != AdhanPlaybackStatus.IDLE) {
                AdhanStatusBanner(
                    adhanState = adhanState,
                    onSwitchFallback = onSwitchFallbackStream,
                    onRetry = onRetryAdhan,
                    onStop = {
                        allMuezzins.find { it.id == adhanState.activeMuezzinId }?.let(onTogglePreview)
                    }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Clean Refresh Button
                Button(
                    onClick = onSyncRepo,
                    enabled = !isSyncingRepo,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                        disabledContentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sync_muezzin_repo_button")
                ) {
                    if (isSyncingRepo) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFF6DC97)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "تحديث",
                            tint = Color(0xFFF6DC97),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isSyncingRepo) {
                                "جاري التحديث..."
                            } else {
                                "تحديث"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!repoStatusMessage.isNullOrBlank()) {
                            Text(
                                text = repoStatusMessage,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFF6DC97),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Current Default & Fajr Summary Strip
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(1.dp, islamic.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (activeDefaultMuezzin != null) {
                                    val defaultHasAudio = activeDefaultMuezzin.hasAudioSource()
                                    val defaultPlaying = adhanState.activeMuezzinId == activeDefaultMuezzin.id &&
                                        (adhanState.status == AdhanPlaybackStatus.PLAYING ||
                                            adhanState.status == AdhanPlaybackStatus.FALLBACK_PLAYING)
                                    val defaultLoading = adhanState.activeMuezzinId == activeDefaultMuezzin.id &&
                                        adhanState.status == AdhanPlaybackStatus.LOADING
                                    IconButton(
                                        onClick = {
                                            if (defaultHasAudio) {
                                                onTogglePreview(activeDefaultMuezzin)
                                            } else {
                                                editingAudioMuezzin = activeDefaultMuezzin
                                            }
                                        },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    !defaultHasAudio -> islamic.goldSoft
                                                    defaultPlaying -> islamic.gold
                                                    else -> MaterialTheme.colorScheme.primary
                                                }
                                            )
                                            .testTag("default_muezzin_quick_preview")
                                    ) {
                                        when {
                                            !defaultHasAudio -> Icon(
                                                imageVector = Icons.Default.UploadFile,
                                                contentDescription = "أضف ملف الصوت للأذان الافتراضي",
                                                tint = islamic.goldText,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            defaultLoading -> CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color = Color.White
                                            )
                                            defaultPlaying -> Icon(
                                                imageVector = Icons.Default.Stop,
                                                contentDescription = "إيقاف الأذان الافتراضي",
                                                tint = Color(0xFF1A1405),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            else -> Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "معاينة الأذان الافتراضي",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "الأذان الافتراضي لبطاقة مواقيت الصلاة",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = islamic.textMuted
                                    )
                                    Text(
                                        text = activeDefaultMuezzin?.name ?: "غير محدد",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = islamic.goldSoft
                            ) {
                                Text(
                                    text = activeDefaultMuezzin?.regionEnum()?.label.orEmpty(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = islamic.goldText,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(islamic.cardAlt)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NightsStay,
                                    contentDescription = null,
                                    tint = islamic.goldText,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "تخصيص مؤذن مختلف لكل صلاة",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (perPrayerMuezzinEnabled) {
                                            "اختر الصلاة بالأسفل ثم عيّن مؤذنها"
                                        } else {
                                            "مؤذن موحّد لجميع الصلوات الخمس"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = islamic.textMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Switch(
                                checked = perPrayerMuezzinEnabled,
                                onCheckedChange = onTogglePerPrayerMuezzin,
                                modifier = Modifier.testTag("per_prayer_muezzin_switch")
                            )
                        }

                        AnimatedVisibility(visible = perPrayerMuezzinEnabled) {
                            val prayerTabs = listOf(
                                "Fajr" to "الفجر",
                                "Dhuhr" to "الظهر",
                                "Asr" to "العصر",
                                "Maghrib" to "المغرب",
                                "Isha" to "العشاء"
                            )
                            val assignedIdForTarget = perPrayerMuezzinIds[selectedTargetPrayerKey] ?: defaultMuezzinId
                            val assignedNameForTarget = allMuezzins.find { it.id == assignedIdForTarget }?.name
                                ?: activeDefaultMuezzin?.name
                                ?: "غير محدد"
                            val targetArabic = prayerTabs.find { it.first == selectedTargetPrayerKey }?.second ?: "الفجر"

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    prayerTabs.forEach { (pKey, pLabel) ->
                                        val isTabSelected = selectedTargetPrayerKey == pKey
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isTabSelected) MaterialTheme.colorScheme.primary else islamic.cardAlt,
                                            border = BorderStroke(
                                                1.dp,
                                                if (isTabSelected) MaterialTheme.colorScheme.primary else islamic.border
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { selectedTargetPrayerKey = pKey }
                                                .testTag("per_prayer_pill_$pKey")
                                        ) {
                                            Text(
                                                text = pLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isTabSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "مؤذن صلاة $targetArabic الحالي: $assignedNameForTarget",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (!perPrayerMuezzinEnabled) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(islamic.cardAlt)
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "أذان الفجر المخصص (الصلاة خير من النوم)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (useDedicatedFajrAdhan && activeFajrMuezzin != null) {
                                        Text(
                                            text = activeFajrMuezzin.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = islamic.textMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Switch(
                                    checked = useDedicatedFajrAdhan,
                                    onCheckedChange = onToggleDedicatedFajr,
                                    modifier = Modifier.testTag("dedicated_fajr_switch")
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("muezzin_search_input"),
                    placeholder = { Text("ابحث باسم المؤذن أو البلد (مثال: رفعت، ملا، الأقصى)...") },
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

                // Region Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(MuezzinRegion.entries) { region ->
                        val count = when (region) {
                            MuezzinRegion.ALL -> allMuezzins.size
                            MuezzinRegion.FAVORITES -> favoriteIds.size
                            MuezzinRegion.FAJR -> allMuezzins.count { it.isFajr || it.regionEnum() == MuezzinRegion.FAJR }
                            MuezzinRegion.CUSTOM -> allMuezzins.count { it.isCustom }
                            else -> allMuezzins.count { it.regionEnum() == region }
                        }
                        FilterChip(
                            selected = selectedRegion == region,
                            onClick = { selectedRegion = region },
                            label = {
                                Text(
                                    text = "${region.label} ($count)",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("muezzin_region_${region.name.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (filteredMuezzins.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (selectedRegion == MuezzinRegion.CUSTOM) {
                                    "لم تضف مؤذنين مخصصين بعد"
                                } else {
                                    "لا يوجد مؤذن مطابق لبحثك"
                                },
                                color = islamic.textMuted
                            )
                            if (selectedRegion == MuezzinRegion.CUSTOM) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(onClick = { showAddDialog = true }) {
                                    Text("أضف مؤذنك المفضل")
                                }
                            }
                        }
                    }
                } else {
                    val groupedByRegion = remember(pagedMuezzins, selectedRegion) {
                        if (selectedRegion == MuezzinRegion.ALL) {
                            val regionOrder = listOf(
                                MuezzinRegion.CUSTOM,
                                MuezzinRegion.HARAMAIN,
                                MuezzinRegion.EGYPT,
                                MuezzinRegion.GULF,
                                MuezzinRegion.LEVANT_AQSA,
                                MuezzinRegion.TURKEY_MAGHREB,
                                MuezzinRegion.FAJR
                            )
                            regionOrder.mapNotNull { reg ->
                                val list = pagedMuezzins.filter { it.regionEnum() == reg }
                                if (list.isNotEmpty()) reg to list else null
                            }
                        } else {
                            emptyList()
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        if (selectedRegion == MuezzinRegion.ALL && groupedByRegion.isNotEmpty()) {
                            groupedByRegion.forEach { (region, regionMuezzins) ->
                                item(key = "section_${region.name}") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp, bottom = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .width(4.dp)
                                                    .height(16.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(islamic.gold)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = region.label,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = islamic.emeraldSoft
                                        ) {
                                            Text(
                                                text = "${regionMuezzins.size} مؤذن",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                val targetPrayerArabic = when (selectedTargetPrayerKey) {
                                    "Fajr" -> "الفجر"
                                    "Dhuhr" -> "الظهر"
                                    "Asr" -> "العصر"
                                    "Maghrib" -> "المغرب"
                                    "Isha" -> "العشاء"
                                    else -> "الفجر"
                                }
                                items(regionMuezzins, key = { it.id }) { muezzin ->
                                    val isAssignedToTarget = perPrayerMuezzinIds[selectedTargetPrayerKey] == muezzin.id
                                    MuezzinCard(
                                        muezzin = muezzin,
                                        isDefault = if (perPrayerMuezzinEnabled) isAssignedToTarget else muezzin.id == defaultMuezzinId,
                                        isFajrDefault = muezzin.id == fajrMuezzinId,
                                        perPrayerMode = perPrayerMuezzinEnabled,
                                        targetPrayerArabic = targetPrayerArabic,
                                        isFavorite = favoriteIds.contains(muezzin.id),
                                        isCachedOffline = cachedAdhanIds.contains(muezzin.id),
                                        isDownloadingOffline = downloadingMuezzinIds.contains(muezzin.id),
                                        adhanState = adhanState,
                                        onTogglePreview = { onTogglePreview(muezzin) },
                                        onToggleFavorite = { onToggleFavorite(muezzin.id) },
                                        onSetDefault = {
                                            if (perPrayerMuezzinEnabled) {
                                                onSetMuezzinForPrayer(selectedTargetPrayerKey, muezzin)
                                            } else {
                                                onSetDefaultMuezzin(muezzin)
                                            }
                                        },
                                        onSetFajr = { onSetFajrMuezzin(muezzin) },
                                        onDownloadOffline = { onDownloadOffline(muezzin) },
                                        onEditAudio = { editingAudioMuezzin = muezzin },
                                        onDelete = if (muezzin.isCustom) ({ onDeleteCustomMuezzin(muezzin) }) else null
                                    )
                                }
                            }
                        } else {
                            val targetPrayerArabic = when (selectedTargetPrayerKey) {
                                "Fajr" -> "الفجر"
                                "Dhuhr" -> "الظهر"
                                "Asr" -> "العصر"
                                "Maghrib" -> "المغرب"
                                "Isha" -> "العشاء"
                                else -> "الفجر"
                            }
                            items(pagedMuezzins, key = { it.id }) { muezzin ->
                                val isAssignedToTarget = perPrayerMuezzinIds[selectedTargetPrayerKey] == muezzin.id
                                MuezzinCard(
                                    muezzin = muezzin,
                                    isDefault = if (perPrayerMuezzinEnabled) isAssignedToTarget else muezzin.id == defaultMuezzinId,
                                    isFajrDefault = muezzin.id == fajrMuezzinId,
                                    perPrayerMode = perPrayerMuezzinEnabled,
                                    targetPrayerArabic = targetPrayerArabic,
                                    isFavorite = favoriteIds.contains(muezzin.id),
                                    isCachedOffline = cachedAdhanIds.contains(muezzin.id),
                                    isDownloadingOffline = downloadingMuezzinIds.contains(muezzin.id),
                                    adhanState = adhanState,
                                    onTogglePreview = { onTogglePreview(muezzin) },
                                    onToggleFavorite = { onToggleFavorite(muezzin.id) },
                                    onSetDefault = {
                                        if (perPrayerMuezzinEnabled) {
                                            onSetMuezzinForPrayer(selectedTargetPrayerKey, muezzin)
                                        } else {
                                            onSetDefaultMuezzin(muezzin)
                                        }
                                    },
                                    onSetFajr = { onSetFajrMuezzin(muezzin) },
                                    onDownloadOffline = { onDownloadOffline(muezzin) },
                                    onEditAudio = { editingAudioMuezzin = muezzin },
                                    onDelete = if (muezzin.isCustom) ({ onDeleteCustomMuezzin(muezzin) }) else null
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddCustomMuezzinDialog(
            onDismiss = { showAddDialog = false },
            onAddFromUrl = { name, url, isFajr ->
                onAddCustomFromUrl(name, url, isFajr)
                showAddDialog = false
            },
            onAddFromFile = { name, uri, isFajr ->
                onAddCustomFromFile(name, uri, isFajr)
                showAddDialog = false
            }
        )
    }

    val targetToEdit = editingAudioMuezzin
    if (targetToEdit != null) {
        EditMuezzinAudioDialog(
            muezzin = targetToEdit,
            onDismiss = { editingAudioMuezzin = null },
            onSaveUrl = { url ->
                onOverrideAudioWithUrl(targetToEdit, url)
                editingAudioMuezzin = null
            },
            onSaveFile = { uri, displayName ->
                onOverrideAudioWithFile(targetToEdit, uri, displayName)
                editingAudioMuezzin = null
            },
            onResetDefault = if (!targetToEdit.isCustom && targetToEdit.isUsingCustomAudio()) {
                {
                    onResetAudioToDefault(targetToEdit)
                    editingAudioMuezzin = null
                }
            } else null
        )
    }
}

@Composable
private fun AdhanStatusBanner(
    adhanState: AdhanPlayerState,
    onSwitchFallback: () -> Unit,
    onRetry: () -> Unit,
    onStop: () -> Unit
) {
    val bgColor = when (adhanState.status) {
        AdhanPlaybackStatus.ERROR -> Color(0xFF7F1D1D)
        AdhanPlaybackStatus.LOADING -> DeepGreen
        else -> EmeraldGreen
    }
    val streamTag = if (adhanState.activeStreamIndex == 0) {
        "البث الرئيسي"
    } else {
        "بث بديل (${adhanState.activeStreamIndex + 1}/${adhanState.totalStreams})"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                when (adhanState.status) {
                    AdhanPlaybackStatus.LOADING -> CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFFF6DC97)
                    )
                    AdhanPlaybackStatus.ERROR -> Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = Color(0xFFFECACA),
                        modifier = Modifier.size(18.dp)
                    )
                    else -> Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = Color(0xFFF6DC97),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${adhanState.activeMuezzinName.orEmpty()} — ${adhanState.status.arabicLabel}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (adhanState.status == AdhanPlaybackStatus.ERROR) {
                            adhanState.errorDetail ?: "تحقق من الاتصال بالإنترنت"
                        } else {
                            streamTag
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFF6DC97),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (adhanState.status == AdhanPlaybackStatus.ERROR) {
                    TextButton(
                        onClick = onRetry,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("adhan_retry_button")
                    ) {
                        Text("إعادة", color = Color(0xFFF6DC97), fontWeight = FontWeight.Bold)
                    }
                } else {
                    if (adhanState.totalStreams > 1) {
                        TextButton(
                            onClick = onSwitchFallback,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("adhan_switch_fallback_button")
                        ) {
                            Text("بث بديل", color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    TextButton(
                        onClick = onStop,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("adhan_banner_stop_button")
                    ) {
                        Text("إيقاف", color = Color(0xFFF6DC97), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (adhanState.status == AdhanPlaybackStatus.PLAYING ||
            adhanState.status == AdhanPlaybackStatus.FALLBACK_PLAYING
        ) {
            LinearProgressIndicator(
                progress = { adhanState.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = SoftGold,
                trackColor = Color.White.copy(alpha = 0.16f)
            )
        }
    }
}

@Composable
private fun MuezzinCard(
    muezzin: MuezzinItem,
    isDefault: Boolean,
    isFajrDefault: Boolean,
    perPrayerMode: Boolean,
    targetPrayerArabic: String,
    isFavorite: Boolean,
    isCachedOffline: Boolean,
    isDownloadingOffline: Boolean,
    adhanState: AdhanPlayerState,
    onTogglePreview: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSetDefault: () -> Unit,
    onSetFajr: () -> Unit,
    onDownloadOffline: () -> Unit,
    onEditAudio: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val islamic = LocalIslamicColors.current
    val hasAudio = muezzin.hasAudioSource() || isCachedOffline
    val isThisActive = adhanState.activeMuezzinId == muezzin.id
    val isLoading = isThisActive && adhanState.status == AdhanPlaybackStatus.LOADING
    val isPlaying = isThisActive && (
        adhanState.status == AdhanPlaybackStatus.PLAYING ||
            adhanState.status == AdhanPlaybackStatus.FALLBACK_PLAYING
        )
    val isError = isThisActive && adhanState.status == AdhanPlaybackStatus.ERROR

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("muezzin_card_${muezzin.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDefault) islamic.emeraldSoft else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            width = if (isDefault || isPlaying) 1.5.dp else 1.dp,
            color = when {
                isPlaying -> islamic.gold
                isDefault -> MaterialTheme.colorScheme.primary
                else -> islamic.border
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play / Pause Preview Button (or Upload Audio button when no verified URL)
                IconButton(
                    onClick = if (hasAudio) onTogglePreview else onEditAudio,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                !hasAudio -> islamic.goldSoft
                                isPlaying -> islamic.gold
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                        .testTag("muezzin_play_${muezzin.id}")
                ) {
                    when {
                        !hasAudio -> Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = "أضف ملف الصوت",
                            tint = islamic.goldText
                        )
                        isLoading -> CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                            color = Color.White
                        )
                        isPlaying -> Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "إيقاف المعاينة",
                            tint = Color(0xFF1A1405)
                        )
                        else -> Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "معاينة صوت المؤذن",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = muezzin.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (muezzin.isFajr) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = islamic.goldSoft
                            ) {
                                Text(
                                    text = "الفجر",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = islamic.goldText,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = muezzin.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = islamic.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    // Audio Source Badge: "صوت مخصص" vs "صوت افتراضي" vs "أضف ملف الصوت"
                    val isCustomAudio = muezzin.isUsingCustomAudio()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        when {
                            isCustomAudio -> {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = islamic.goldSoft,
                                    border = BorderStroke(1.dp, islamic.gold.copy(alpha = 0.7f)),
                                    modifier = Modifier.testTag("muezzin_audio_badge_${muezzin.id}")
                                ) {
                                    Text(
                                        text = "صوت مخصص",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = islamic.goldText,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                if (!muezzin.customAudioLabel.isNullOrBlank()) {
                                    Text(
                                        text = muezzin.customAudioLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = islamic.textMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                }
                            }
                            hasAudio -> {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (isCachedOffline) Color(0xFFDCFCE7) else islamic.cardAlt,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isCachedOffline) Color(0xFF15803D).copy(alpha = 0.5f) else islamic.border
                                    ),
                                    modifier = Modifier.testTag("muezzin_audio_badge_${muezzin.id}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        if (isCachedOffline) {
                                            Icon(
                                                imageVector = Icons.Default.CloudDone,
                                                contentDescription = null,
                                                tint = Color(0xFF15803D),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = if (isCachedOffline) "محفوظ بدون إنترنت ✓" else "صوت افتراضي",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCachedOffline) Color(0xFF15803D) else islamic.textMuted
                                        )
                                    }
                                }
                            }
                            else -> {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = islamic.goldSoft,
                                    border = BorderStroke(1.dp, islamic.gold.copy(alpha = 0.8f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable(onClick = onEditAudio)
                                        .testTag("muezzin_audio_badge_${muezzin.id}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.UploadFile,
                                            contentDescription = null,
                                            tint = islamic.goldText,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "أضف ملف الصوت",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = islamic.goldText
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isThisActive && adhanState.status != AdhanPlaybackStatus.IDLE) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isError && !adhanState.errorDetail.isNullOrBlank()) {
                                adhanState.errorDetail
                            } else {
                                adhanState.status.arabicLabel
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isError -> Color(0xFFDC2626)
                                isPlaying -> MaterialTheme.colorScheme.primary
                                else -> islamic.goldText
                            }
                        )
                    }
                }

                // Favorite Heart Toggle
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("muezzin_fav_${muezzin.id}")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFavorite) "إزالة من المفضلة" else "إضافة للمفضلة",
                        tint = if (isFavorite) Color(0xFFD94646) else islamic.textMuted
                    )
                }

                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "حذف المؤذن المخصص",
                            tint = Color(0xFFD94646)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Row 1: Set as Default + Set as Fajr
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSetDefault,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDefault) MaterialTheme.colorScheme.primary else islamic.emeraldSoft,
                        contentColor = if (isDefault) Color.White else MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("muezzin_set_default_${muezzin.id}")
                ) {
                    if (isDefault) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (perPrayerMode) "مؤذن $targetPrayerArabic" else "الأذان الافتراضي",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = if (perPrayerMode) "تعيين لصلاة $targetPrayerArabic" else "تعيين كأذان افتراضي",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (!perPrayerMode) {
                    OutlinedButton(
                        onClick = onSetFajr,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isFajrDefault) islamic.gold else islamic.border
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isFajrDefault) islamic.goldSoft else Color.Transparent,
                            contentColor = if (isFajrDefault) islamic.goldText else islamic.textMuted
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("muezzin_set_fajr_${muezzin.id}")
                    ) {
                        Text(
                            text = if (isFajrDefault) "أذان الفجر ✓" else "تعيين للفجر",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action Row 2: Offline MP3 Download (when remote URL exists) + Edit/Upload Audio
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (muezzin.primaryUrl.isNotBlank() && muezzin.localFilePath.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = onDownloadOffline,
                        enabled = !isDownloadingOffline,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isCachedOffline) Color(0xFF15803D).copy(alpha = 0.55f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isCachedOffline) Color(0xFFDCFCE7) else islamic.emeraldSoft,
                            contentColor = if (isCachedOffline) Color(0xFF15803D) else MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("muezzin_download_offline_${muezzin.id}")
                    ) {
                        when {
                            isDownloadingOffline -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "جاري التحميل...",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            isCachedOffline -> {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = "محفوظ بدون إنترنت",
                                    tint = Color(0xFF15803D),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "محفوظ محلياً ✓",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D)
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "تحميل للتشغيل بدون إنترنت",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "تحميل بدون إنترنت",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onEditAudio,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (hasAudio) islamic.border else islamic.gold.copy(alpha = 0.85f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (hasAudio) islamic.cardAlt else islamic.goldSoft,
                        contentColor = if (hasAudio) MaterialTheme.colorScheme.primary else islamic.goldText
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("muezzin_edit_audio_${muezzin.id}")
                ) {
                    Icon(
                        imageVector = if (hasAudio) Icons.Default.Edit else Icons.Default.UploadFile,
                        contentDescription = if (hasAudio) "تعديل الرابط أو رفع صوت" else "أضف ملف الصوت",
                        tint = if (hasAudio) MaterialTheme.colorScheme.primary else islamic.goldText,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (hasAudio) "تعديل / رفع صوت" else "أضف ملف الصوت (MP3 / رابط)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (hasAudio) MaterialTheme.colorScheme.primary else islamic.goldText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun EditMuezzinAudioDialog(
    muezzin: MuezzinItem,
    onDismiss: () -> Unit,
    onSaveUrl: (url: String) -> Unit,
    onSaveFile: (uri: Uri, displayName: String) -> Unit,
    onResetDefault: (() -> Unit)?
) {
    val context = LocalContext.current
    val islamic = LocalIslamicColors.current
    val hasAudio = muezzin.hasAudioSource()
    var modeTab by remember {
        mutableIntStateOf(if (!hasAudio || !muezzin.localFilePath.isNullOrBlank()) 0 else 1)
    }
    var directUrl by remember(muezzin.id, muezzin.primaryUrl) { mutableStateOf(muezzin.primaryUrl) }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var pickedFileName by remember { mutableStateOf(muezzin.customAudioLabel.orEmpty()) }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pickedUri = uri
            pickedFileName = queryDisplayName(context, uri) ?: "ملف صوتي مخصص.mp3"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (hasAudio) "تعديل صوت المؤذن" else "إضافة ملف الصوت",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = muezzin.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Current badge status
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        muezzin.isUsingCustomAudio() || !hasAudio -> islamic.goldSoft
                        else -> islamic.emeraldSoft
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when {
                            muezzin.isUsingCustomAudio() ->
                                "الحالة الحالية: صوت مخصص (${muezzin.customAudioLabel ?: "ملف/رابط مخصص"})"
                            hasAudio ->
                                "الحالة الحالية: صوت افتراضي موثّق"
                            else ->
                                "لا يوجد ملف صوتي — ارفع ملف MP3 من جهازك أو أضف رابطاً مباشراً"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            muezzin.isUsingCustomAudio() || !hasAudio -> islamic.goldText
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }

                // Mode selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = modeTab == 0,
                        onClick = { modeTab = 0 },
                        leadingIcon = { Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("رفع ملف MP3", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = modeTab == 1,
                        onClick = { modeTab = 1 },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("رابط MP3 مباشر", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (modeTab == 0) {
                    OutlinedButton(
                        onClick = { audioPickerLauncher.launch(arrayOf("audio/mpeg", "audio/*")) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_pick_mp3_file_button")
                    ) {
                        Icon(Icons.Default.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (pickedUri != null) pickedFileName else "اختر ملف MP3 من جهازك",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "يُحفظ الملف محلياً بشكل دائم داخل التطبيق ليعمل حتى دون اتصال بالإنترنت.",
                        style = MaterialTheme.typography.labelSmall,
                        color = islamic.textMuted
                    )
                } else {
                    OutlinedTextField(
                        value = directUrl,
                        onValueChange = { directUrl = it },
                        label = { Text("رابط ملف الأذان (MP3)") },
                        placeholder = { Text("https://example.com/adhan.mp3") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_muezzin_url_input")
                    )
                }

                if (onResetDefault != null) {
                    OutlinedButton(
                        onClick = onResetDefault,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, islamic.gold),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = islamic.goldText),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_muezzin_audio_default_button")
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("استعادة الصوت الافتراضي", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (modeTab == 0) {
                        val uri = pickedUri ?: return@Button
                        onSaveFile(uri, pickedFileName)
                    } else {
                        val clean = directUrl.trim()
                        if (clean.startsWith("http://") || clean.startsWith("https://")) {
                            onSaveUrl(clean)
                        }
                    }
                },
                enabled = (modeTab == 0 && pickedUri != null) ||
                    (modeTab == 1 && (directUrl.trim().startsWith("http://") || directUrl.trim().startsWith("https://"))),
                modifier = Modifier.testTag("save_edited_muezzin_audio_button")
            ) {
                Text("حفظ الصوت")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun AddCustomMuezzinDialog(
    onDismiss: () -> Unit,
    onAddFromUrl: (name: String, url: String, isFajr: Boolean) -> Unit,
    onAddFromFile: (name: String, uri: Uri, isFajr: Boolean) -> Unit
) {
    val context = LocalContext.current
    val islamic = LocalIslamicColors.current
    var modeTab by remember { mutableIntStateOf(0) } // 0 = Local MP3 File, 1 = Direct MP3 URL
    var customName by remember { mutableStateOf("") }
    var directUrl by remember { mutableStateOf("") }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var pickedFileName by remember { mutableStateOf("") }
    var isFajrAdhan by remember { mutableStateOf(false) }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pickedUri = uri
            pickedFileName = queryDisplayName(context, uri) ?: "ملف صوتي مختار"
            if (customName.isBlank()) {
                customName = pickedFileName.substringBeforeLast(".")
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "أضف مؤذنك المفضل",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Mode selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = modeTab == 0,
                        onClick = { modeTab = 0 },
                        leadingIcon = { Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("ملف من الجهاز", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = modeTab == 1,
                        onClick = { modeTab = 1 },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("رابط MP3", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text("اسم المؤذن") },
                    placeholder = { Text("مثال: الشيخ مصطفى إسماعيل") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_muezzin_name_input")
                )

                if (modeTab == 0) {
                    OutlinedButton(
                        onClick = { audioPickerLauncher.launch(arrayOf("audio/mpeg", "audio/*")) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pick_mp3_file_button")
                    ) {
                        Icon(Icons.Default.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (pickedFileName.isNotEmpty()) pickedFileName else "اختر ملف MP3 من جهازك",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "سيتم حفظ الملف محلياً داخل التطبيق ليعمل دائماً حتى دون إنترنت.",
                        style = MaterialTheme.typography.labelSmall,
                        color = islamic.textMuted
                    )
                } else {
                    OutlinedTextField(
                        value = directUrl,
                        onValueChange = { directUrl = it },
                        label = { Text("رابط ملف الأذان (MP3)") },
                        placeholder = { Text("https://example.com/adhan.mp3") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_muezzin_url_input")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تصنيف كـ أذان الفجر (الصلاة خير من النوم)",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = isFajrAdhan,
                        onCheckedChange = { isFajrAdhan = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedName = customName.trim()
                    if (trimmedName.isEmpty()) return@Button
                    if (modeTab == 0) {
                        val uri = pickedUri ?: return@Button
                        onAddFromFile(trimmedName, uri, isFajrAdhan)
                    } else {
                        val url = directUrl.trim()
                        if (url.startsWith("http://") || url.startsWith("https://")) {
                            onAddFromUrl(trimmedName, url, isFajrAdhan)
                        }
                    }
                },
                enabled = customName.isNotBlank() && (
                    (modeTab == 0 && pickedUri != null) ||
                        (modeTab == 1 && (directUrl.trim().startsWith("http://") || directUrl.trim().startsWith("https://")))
                    ),
                modifier = Modifier.testTag("save_custom_muezzin_button")
            ) {
                Text("حفظ وإضافة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

private fun queryDisplayName(context: android.content.Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) cursor.getString(idx) else null
            } else null
        }
    }.getOrNull()
}
