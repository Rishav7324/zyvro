package com.zyvro.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import com.zyvro.app.ui.components.AppleSpringSpec
import com.zyvro.app.ui.components.AudioTrimmerDialog
import com.zyvro.app.ui.components.DownloadItemCard
import com.zyvro.app.ui.components.LiquidGlassCard
import com.zyvro.app.ui.components.LiquidGlassPill
import com.zyvro.app.ui.components.liquidGlass
import com.zyvro.app.viewmodel.LibraryFilter
import com.zyvro.app.viewmodel.LibraryViewModel
import kotlinx.coroutines.launch
import com.zyvro.app.ui.theme.LocalAppDark
import androidx.compose.ui.graphics.Brush
import com.zyvro.app.ui.components.ambientLiquidBackground
import com.zyvro.app.ui.theme.NovaCyan
import com.zyvro.app.ui.theme.NovaCyanDeep
import com.zyvro.app.ui.theme.NovaAzure
import java.io.File

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
            // Device scans carry no play stats: show everything under smart filters.
            LibraryFilter.FAVORITES, LibraryFilter.TOP -> true
        }
        val matchesQuery = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.uploader.contains(searchQuery, ignoreCase = true)
        matchesFilter && matchesQuery
    }

    val isDark = LocalAppDark.current
    val activePillBrush = if (isDark) {
        Brush.horizontalGradient(
            listOf(
                Color(0xFF00E5FF).copy(alpha = 0.35f),
                Color(0xFF0077B6).copy(alpha = 0.50f)
            )
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                NovaCyan,
                NovaCyanDeep
            )
        )
    }
    val activeBorderBrush = Brush.linearGradient(
        listOf(
            if (isDark) Color(0xFF00E5FF).copy(alpha = 0.6f) else Color.White,
            if (isDark) Color.White.copy(alpha = 0.2f) else Color(0xFF00B4D8).copy(alpha = 0.5f)
        )
    )
    val activeTextColor = if (isDark) Color(0xFF00E5FF) else Color(0xFF0A1E2C)
    val inactiveTextColor = if (isDark) Color(0xFF90A8BD) else Color(0xFF4A687D)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .ambientLiquidBackground()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Media Library",
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${displayList.size} files available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (activeTab == 1) {
                    IconButton(
                        onClick = { refreshLocalMedia() },
                        modifier = Modifier
                            .size(38.dp)
                            .liquidGlass(shape = CircleShape, elevation = 2.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = "Scan Device Media",
                            tint = if (isDark) NovaCyan else NovaCyanDeep,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else if (completedList.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.clearAllCompleted() },
                        modifier = Modifier
                            .size(38.dp)
                            .liquidGlass(shape = CircleShape, elevation = 2.dp)
                    ) {
                        Icon(
                            Icons.Rounded.DeleteSweep,
                            contentDescription = "Clear Completed",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Penpot Device Storage Breakdown Glass Card
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
            elevation = 3.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Storage,
                            contentDescription = null,
                            tint = if (isDark) NovaCyan else NovaCyanDeep,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Device Storage",
                            style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.5.sp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "$freeGb GB Free",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = if (isDark) NovaCyan else NovaCyanDeep,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Multi-Segment Storage Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF162B3D) else Color(0xFFCCE7E8))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.18f)
                            .background(if (isDark) NovaCyan else NovaCyanDeep)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.32f)
                            .background(if (isDark) Color(0xFF33556E) else Color(0xFF506F72))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.50f)
                            .background(Color.Transparent)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(if (isDark) NovaCyan else NovaCyanDeep))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Zyvro ($formattedSaved)",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                            color = inactiveTextColor
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(if (isDark) Color(0xFF33556E) else Color(0xFF506F72)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Used Apps",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                            color = inactiveTextColor
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(if (isDark) Color(0xFF162B3D) else Color(0xFFCCE7E8)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Free ($freeGb GB)",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                            color = inactiveTextColor
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // iOS Liquid Glass Segmented Control
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
                                    .border(1.2.dp, activeBorderBrush, RoundedCornerShape(20.dp))
                            } else Modifier
                        )
                        .clickable { activeTab = 0 },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Downloads (${completedList.size})",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp),
                        fontWeight = if (activeTab == 0) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (activeTab == 0) activeTextColor else inactiveTextColor
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
                                    .border(1.2.dp, activeBorderBrush, RoundedCornerShape(20.dp))
                            } else Modifier
                        )
                        .clickable { activeTab = 1 },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Device Storage",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp),
                        fontWeight = if (activeTab == 1) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (activeTab == 1) activeTextColor else inactiveTextColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar with Liquid Glass
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(shape = RoundedCornerShape(18.dp), elevation = 3.dp),
            shape = RoundedCornerShape(18.dp),
            placeholder = { Text("Search songs, videos or creators...", color = inactiveTextColor) },
            leadingIcon = {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = null,
                    tint = if (isDark) NovaCyan else NovaCyanDeep
                )
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isDark) NovaCyan else NovaCyanDeep,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Category Filter Pills (Retro-style: All / Videos / Audio / Favorites / Top)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LiquidGlassPill(
                isSelected = currentFilter == LibraryFilter.ALL,
                onClick = { viewModel.setFilter(LibraryFilter.ALL) }
            ) {
                Text(
                    text = "All",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            LiquidGlassPill(
                isSelected = currentFilter == LibraryFilter.VIDEOS,
                onClick = { viewModel.setFilter(LibraryFilter.VIDEOS) }
            ) {
                Icon(
                    Icons.Rounded.Videocam,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Videos",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            LiquidGlassPill(
                isSelected = currentFilter == LibraryFilter.AUDIO,
                onClick = { viewModel.setFilter(LibraryFilter.AUDIO) }
            ) {
                Icon(
                    Icons.Rounded.Audiotrack,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Audio",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            LiquidGlassPill(
                isSelected = currentFilter == LibraryFilter.FAVORITES,
                onClick = { viewModel.setFilter(LibraryFilter.FAVORITES) }
            ) {
                Icon(
                    Icons.Rounded.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Favorites",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            LiquidGlassPill(
                isSelected = currentFilter == LibraryFilter.TOP,
                onClick = { viewModel.setFilter(LibraryFilter.TOP) }
            ) {
                Icon(
                    Icons.Rounded.TrendingUp,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Top Played",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.primary
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
                    modifier = Modifier.fillMaxWidth(0.9f),
                    shape = RoundedCornerShape(26.dp),
                    elevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .liquidGlass(shape = CircleShape, elevation = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (activeTab == 0) Icons.Rounded.FolderOpen else Icons.Rounded.PhoneAndroid,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (activeTab == 0) "No Downloads Yet" else "No Device Media Found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (activeTab == 0) "Downloaded videos and audio will appear here." else "Ensure storage permission is granted to scan local files.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 110.dp)
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
