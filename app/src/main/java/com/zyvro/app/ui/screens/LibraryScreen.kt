package com.zyvro.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zyvro.app.data.local.DownloadEntity
import com.zyvro.app.data.local.MediaType
import com.zyvro.app.data.local.fileSize
import com.zyvro.app.data.scanner.LocalMediaScanner
import com.zyvro.app.player.MediaPlayerManager
import com.zyvro.app.ui.components.AudioTrimmerDialog
import com.zyvro.app.ui.components.DownloadItemCard
import com.zyvro.app.ui.components.LiquidGlassCard
import com.zyvro.app.ui.components.LiquidGlassPill
import com.zyvro.app.ui.components.ambientLiquidBackground
import com.zyvro.app.ui.components.liquidGlass
import com.zyvro.app.ui.theme.*
import com.zyvro.app.viewmodel.LibraryFilter
import com.zyvro.app.viewmodel.LibraryViewModel
import kotlinx.coroutines.launch
import java.io.File

// ═══════════════════════════════════════════════════════════════════════════
// ZYVRO LIBRARY SCREEN v4.0 — Deep Space Aura · Media Hub
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playerManager = MediaPlayerManager.getInstance(context)
    val completedList by viewModel.completedDownloads.collectAsState()
    val currentFilter by viewModel.filter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val deviceFavPaths by viewModel.deviceFavorites.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Downloads, 1: Device Storage
    val localDeviceMedia = remember { mutableStateListOf<DownloadEntity>() }
    var isScanning by remember { mutableStateOf(false) }
    var trimmingItem by remember { mutableStateOf<DownloadEntity?>(null) }

    fun refreshLocalMedia() {
        scope.launch {
            isScanning = true
            val audio = LocalMediaScanner.scanLocalAudio(context)
            val videos = LocalMediaScanner.scanLocalVideos(context)
            localDeviceMedia.clear()
            localDeviceMedia.addAll(audio + videos)
            isScanning = false
        }
    }

    LaunchedEffect(activeTab) {
        if (activeTab == 1 && localDeviceMedia.isEmpty()) {
            refreshLocalMedia()
        }
    }

    val displayList = if (activeTab == 0) completedList else localDeviceMedia.filter { item ->
        val matchesFilter = when (currentFilter) {
            LibraryFilter.ALL -> true
            LibraryFilter.VIDEOS -> item.mediaType == MediaType.VIDEO
            LibraryFilter.AUDIO -> item.mediaType == MediaType.AUDIO
            LibraryFilter.FAVORITES, LibraryFilter.TOP -> true
        }
        val matchesQuery = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.uploader.contains(searchQuery, ignoreCase = true)
        matchesFilter && matchesQuery
    }

    val isDark = LocalAppDark.current
    val activePillBrush = Brush.horizontalGradient(listOf(NovaPrimary, NovaViolet))
    val activeBorderBrush = Brush.linearGradient(
        listOf(
            NovaPrimary.copy(alpha = 0.8f),
            NovaViolet.copy(alpha = 0.6f)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .ambientLiquidBackground(isDark)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // ── Header ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Media Library",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${displayList.size} files available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (activeTab == 1) {
                    IconButton(
                        onClick = { refreshLocalMedia() },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NovaPrimary.copy(alpha = 0.12f))
                            .border(0.5.dp, NovaPrimary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = "Scan Device Media",
                            tint = NovaPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else if (completedList.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.clearAllCompleted() },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AccentRed.copy(alpha = 0.12f))
                            .border(0.5.dp, AccentRed.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            Icons.Rounded.DeleteSweep,
                            contentDescription = "Clear Completed",
                            tint = AccentRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Device Storage Glass Card ──────────────────────────────────────
        val freeBytes = remember {
            runCatching { android.os.Environment.getDataDirectory().freeSpace }.getOrDefault(50L * 1024L * 1024L * 1024L)
        }
        val freeGb = (freeBytes / (1024L * 1024L * 1024L)).toInt()
        val totalBytesSaved = completedList.sumOf { it.fileSize }
        val savedMb = (totalBytesSaved / (1024L * 1024L)).toInt()
        val formattedSaved = if (savedMb >= 1024) String.format(java.util.Locale.US, "%.1f GB", savedMb / 1024.0) else "$savedMb MB"

        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Storage,
                            contentDescription = null,
                            tint = NovaPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Device Storage",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "$freeGb GB Free",
                        style = MaterialTheme.typography.labelSmall,
                        color = NovaPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Multi-Segment Storage Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(if (isDark) SpaceBorder else Color(0xFFE2E9F8))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.20f)
                            .background(Brush.horizontalGradient(GradientCyan))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.30f)
                            .background(Brush.horizontalGradient(GradientViolet))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.50f)
                            .background(Color.Transparent)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(NovaPrimary))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Zyvro ($formattedSaved)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(NovaViolet))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Other Apps",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (isDark) SpaceBorder else Color(0xFFC0D0E8)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Free Space",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Segmented Control (Downloads vs Device Storage) ─────────────────
        LiquidGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Downloads Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .then(
                            if (activeTab == 0) {
                                Modifier
                                    .background(activePillBrush, RoundedCornerShape(20.dp))
                                    .border(1.dp, activeBorderBrush, RoundedCornerShape(20.dp))
                            } else Modifier
                        )
                        .clickable { activeTab = 0 },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Downloads (${completedList.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (activeTab == 0) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (activeTab == 0) Color(0xFF001824) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Device Storage Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .then(
                            if (activeTab == 1) {
                                Modifier
                                    .background(activePillBrush, RoundedCornerShape(20.dp))
                                    .border(1.dp, activeBorderBrush, RoundedCornerShape(20.dp))
                            } else Modifier
                        )
                        .clickable { activeTab = 1 },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Device Media",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (activeTab == 1) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (activeTab == 1) Color(0xFF001824) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Search Bar with Deep Space Glow ────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(18.dp), spotColor = NovaPrimary.copy(alpha = 0.2f))
                .clip(RoundedCornerShape(18.dp))
                .background(if (isDark) SpaceCard else Color.White)
                .border(
                    0.8.dp,
                    if (searchQuery.isNotBlank()) NovaPrimary.copy(alpha = 0.6f)
                    else (if (isDark) SpaceBorder else Color(0xFFD4E0F0)),
                    RoundedCornerShape(18.dp)
                )
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                placeholder = {
                    Text(
                        "Search titles, artists or formats…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = null,
                        tint = NovaPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Rounded.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── Category Filter Pills ──────────────────────────────────────────
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf(
                Pair(LibraryFilter.ALL, "All"),
                Pair(LibraryFilter.VIDEOS, "Videos"),
                Pair(LibraryFilter.AUDIO, "Audio"),
                Pair(LibraryFilter.FAVORITES, "Favorites"),
                Pair(LibraryFilter.TOP, "Top Played")
            )

            filters.forEach { (filterType, label) ->
                val isSelected = currentFilter == filterType
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) NovaPrimary.copy(alpha = 0.16f)
                            else if (isDark) SpaceCardHigh else Color(0xFFF0F4FF)
                        )
                        .border(
                            0.7.dp,
                            if (isSelected) NovaPrimary else (if (isDark) SpaceBorder else Color(0xFFD4E0F0)),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { viewModel.setFilter(filterType) }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) NovaPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Media List / Empty State ───────────────────────────────────────
        if (isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 2.5.dp,
                    color = NovaPrimary
                )
            }
        } else if (displayList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 100.dp),
                contentAlignment = Alignment.Center
            ) {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(0.92f),
                    shape = RoundedCornerShape(26.dp),
                    elevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .shadow(12.dp, CircleShape, spotColor = NovaPrimary.copy(alpha = 0.3f))
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(SpaceCardHigh, SpaceGlass)))
                                .border(1.dp, NovaPrimary.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (activeTab == 0) Icons.Rounded.FolderOpen else Icons.Rounded.PhoneAndroid,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = NovaPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (activeTab == 0) "No Media Downloaded" else "No Device Files Found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (activeTab == 0) "Downloaded files will be organized here with audio trimming and playlist support."
                            else "Ensure storage permission is granted to scan device audio and video files.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(displayList, key = { it.id }) { item ->
                    val isDeviceTab = activeTab == 1
                    DownloadItemCard(
                        download = item,
                        onCancel = { },
                        onDelete = { id -> viewModel.deleteDownload(id) },
                        onPlay = { playerManager.playMedia(item) },
                        onShare = { shareMedia(context, item) },
                        onTrim = { trimmingItem = it },
                        onToggleFavorite = { _ ->
                            if (isDeviceTab) viewModel.toggleDeviceFavorite(item.targetPath)
                            else viewModel.toggleFavorite(item.id)
                        },
                        isFavoriteOverride = if (isDeviceTab) {
                            deviceFavPaths.contains(item.targetPath)
                        } else null
                    )
                }
            }
        }
    }

    trimmingItem?.let { target ->
        AudioTrimmerDialog(
            media = target,
            onDismiss = { trimmingItem = null }
        )
    }
}

private fun shareMedia(context: Context, item: DownloadEntity) {
    try {
        val file = File(item.targetPath)
        if (!file.exists()) {
            Toast.makeText(context, "File does not exist on disk", Toast.LENGTH_SHORT).show()
            return
        }
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val mime = if (item.mediaType == MediaType.VIDEO) "video/*" else "audio/*"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share ${item.title}"))
    } catch (e: Exception) {
        Toast.makeText(context, "Share failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}
