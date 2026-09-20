package com.zyvro.app.ui.player

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.rounded.ContentCut
import com.zyvro.app.ui.components.AudioTrimmerDialog
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.zyvro.app.YtDlpApp
import com.zyvro.app.data.local.PlaylistEntity
import com.zyvro.app.player.MediaPlayerManager
import com.zyvro.app.ui.components.equalizer.EqualizerDialog
import com.zyvro.app.ui.theme.NovaAqua
import com.zyvro.app.ui.theme.NovaAquaDeep
import com.zyvro.app.ui.theme.NovaInk
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = MediaPlayerManager.getInstance(context)

    val media by manager.currentMedia.collectAsState()
    val queue by manager.queue.collectAsState()
    val playing by manager.isPlaying.collectAsState()
    val position by manager.currentPosition.collectAsState()
    val duration by manager.duration.collectAsState()
    val speed by manager.playbackSpeed.collectAsState()
    val repeat by manager.repeatMode.collectAsState()
    val shuffle by manager.isShuffleEnabled.collectAsState()
    val pointA by manager.loopPointA.collectAsState()
    val pointB by manager.loopPointB.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showEqualizer by remember { mutableStateOf(false) }
    var showTrimmer by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var sleepTimerMinutes by remember { mutableIntStateOf(0) }
    var sleepTimerJob by remember { mutableStateOf<Job?>(null) }
    var sleepTimerActive by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val item = media ?: return
    // DB-backed items (id < 1M) support favorites/playlists; device scans are display-only.
    val isDbItem = item.id in 1 until 1_000_000
    val repository = remember { (context.applicationContext as YtDlpApp).repository }
    var isFavDb by remember(item.id, item.isFavorite) { mutableStateOf(item.isFavorite) }
    val deviceFavs by repository.preferences.deviceFavorites.collectAsState(initial = emptySet())
    val isFav = if (isDbItem) isFavDb else deviceFavs.contains(item.targetPath)
    val playlists by repository.playlists.collectAsState(initial = emptyList())
    val artScale by animateFloatAsState(if (playing) 1.0f else 0.94f, tween(300), label = "art-scale")

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        sleepTimerMinutes = minutes
        if (minutes > 0) {
            sleepTimerActive = true
            sleepTimerJob = scope.launch {
                delay(minutes * 60 * 1000L)
                manager.pause()
                sleepTimerActive = false
                sleepTimerMinutes = 0
            }
        } else {
            sleepTimerActive = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Drag Handle & Top Action Header
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(4.5.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.KeyboardArrowDown, "Collapse Sheet", Modifier.size(30.dp))
                        }
                        Text(
                            text = "NOW PLAYING",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = NovaAqua,
                            letterSpacing = 1.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                scope.launch {
                                    runCatching {
                                        if (isDbItem) {
                                            repository.toggleFavorite(item.id)
                                            isFavDb = !isFavDb
                                        } else {
                                            repository.preferences.toggleDeviceFavorite(item.targetPath)
                                        }
                                    }
                                }
                            }) {
                                    Icon(
                                        if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        "Favorite",
                                        tint = if (isFav) Color(0xFFE1306C) else NovaAqua
                                    )
                                }
                            if (isDbItem) {
                                IconButton(onClick = { showPlaylistDialog = true }) {
                                    Icon(Icons.Default.PlaylistAdd, "Add to playlist", tint = NovaAqua)
                                }
                            }
                            IconButton(onClick = { showEqualizer = true }) {
                                Icon(Icons.Default.Tune, "Equalizer", tint = NovaAqua)
                            }
                            IconButton(onClick = { showTrimmer = true }) {
                                Icon(Icons.Rounded.ContentCut, "Trim & Ringtone", tint = NovaAqua)
                            }
                        }
                    }
                }
            }

            // Tabs: Material You single-choice segmented control
            item {
                val tabLabels = listOf("Player", "Queue (${queue.size})", "Details")
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabLabels.forEachIndexed { index, label ->
                        SegmentedButton(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = tabLabels.size
                            ),
                            label = {
                                Text(label, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        )
                    }
                }
            }

            if (selectedTab == 0) {
                // Tab 0: Main Audio Player View
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        // Album art with Retro-style blurred backdrop (API 31+)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && item.thumbnailUrl.isNotBlank()) {
                                AsyncImage(
                                    model = item.thumbnailUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(260.dp)
                                        .alpha(0.45f)
                                        .graphicsLayer {
                                            renderEffect = BlurEffect(48f, 48f)
                                        }
                                        .clip(RoundedCornerShape(32.dp))
                                )
                            }
                            // Liquid Glass Album Art Frame
                            Box(
                                modifier = Modifier
                                    .size(240.dp)
                                    .scale(artScale)
                                    .shadow(20.dp, RoundedCornerShape(32.dp), ambientColor = NovaAqua)
                                .clip(RoundedCornerShape(32.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(NovaAqua.copy(alpha = 0.2f), Color.White.copy(alpha = 0.05f))
                                    )
                                )
                                .border(1.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(32.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (item.thumbnailUrl.isNotBlank()) {
                                AsyncImage(
                                    model = item.thumbnailUrl,
                                    contentDescription = item.title,
                                    modifier = Modifier.fillMaxWidth(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        modifier = Modifier.size(76.dp),
                                        tint = NovaAqua
                                    )
                                }
                            }
                        } // end album-art frame
                        } // end blurred backdrop wrapper

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.uploader.ifBlank { "Zyvro Audio" },
                            color = NovaAqua,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Timeline Scrubber
                item {
                    val safeDur = duration.coerceAtLeast(1L)
                    Slider(
                        value = position.coerceIn(0L, safeDur).toFloat(),
                        onValueChange = { manager.seekTo(it.toLong()) },
                        valueRange = 0f..safeDur.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = NovaAqua,
                            activeTrackColor = NovaAqua,
                            inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDuration(position),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = formatDuration(duration),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Playback Control Buttons Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { manager.toggleShuffle() }) {
                            Icon(
                                Icons.Default.Shuffle,
                                "Shuffle",
                                tint = if (shuffle) NovaAqua else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { manager.seekRewind(10000) }) {
                            Icon(Icons.Default.Replay10, "Rewind 10 seconds", Modifier.size(32.dp))
                        }

                        // Giant Play/Pause with Spring Physics
                        IconButton(
                            onClick = { manager.togglePlayPause() },
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(NovaAqua)
                                .shadow(12.dp, CircleShape, ambientColor = NovaAqua)
                        ) {
                            Icon(
                                imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                modifier = Modifier.size(40.dp),
                                tint = NovaInk
                            )
                        }

                        IconButton(onClick = { manager.seekForward(10000) }) {
                            Icon(Icons.Default.Forward10, "Forward 10 seconds", Modifier.size(32.dp))
                        }

                        IconButton(onClick = { manager.toggleRepeatMode() }) {
                            Icon(
                                imageVector = if (repeat == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                contentDescription = "Repeat",
                                tint = if (repeat != Player.REPEAT_MODE_OFF) NovaAqua else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Playback Speed Chips
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)) { itemSpeed ->
                            FilterChip(
                                selected = speed == itemSpeed,
                                onClick = { manager.setSpeed(itemSpeed) },
                                label = { Text("${itemSpeed}x", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NovaAqua,
                                    selectedLabelColor = NovaInk
                                )
                            )
                        }
                    }
                }

                // Sleep Timer Selector
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Timer, contentDescription = null, tint = NovaAqua, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sleep Timer", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                if (sleepTimerActive) {
                                    Text("${sleepTimerMinutes}m Active", color = NovaAqua, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                items(listOf(0, 15, 30, 45, 60)) { mins ->
                                    FilterChip(
                                        selected = sleepTimerMinutes == mins && (mins == 0 || sleepTimerActive),
                                        onClick = { startSleepTimer(mins) },
                                        label = { Text(if (mins == 0) "Off" else "${mins}m", fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NovaAqua,
                                            selectedLabelColor = NovaInk
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // A-B Loop Controls
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("A–B Loop Section", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = pointA != null,
                                    onClick = { manager.setLoopPointA() },
                                    label = { Text(if (pointA == null) "Set Loop A" else "A: ${formatDuration(pointA ?: 0)}") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = pointB != null,
                                    onClick = { manager.setLoopPointB() },
                                    label = { Text(if (pointB == null) "Set Loop B" else "B: ${formatDuration(pointB ?: 0)}") },
                                    modifier = Modifier.weight(1f)
                                )
                                if (pointA != null || pointB != null) {
                                    IconButton(onClick = { manager.clearAbLoop() }) {
                                        Icon(Icons.Default.Close, "Clear loop", tint = Color.Red.copy(alpha = 0.8f))
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (selectedTab == 1) {
                // Tab 1: Queue Management
                if (queue.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.MusicNote, null, Modifier.size(54.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(10.dp))
                            Text("Queue is empty", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    itemsIndexed(queue, key = { _, q -> q.id }) { index, qItem ->
                        Card(
                            onClick = { manager.playMedia(qItem, queue, false) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (qItem.id == item.id) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (qItem.id == item.id) Icons.Default.GraphicEq else Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = if (qItem.id == item.id) NovaAqua else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = qItem.title,
                                        fontWeight = if (qItem.id == item.id) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = qItem.uploader,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                // Retro-style manual reorder
                                Column {
                                    IconButton(
                                        onClick = { manager.moveQueueItem(index, index - 1) },
                                        enabled = index > 0,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowUp, "Move up", Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { manager.moveQueueItem(index, index + 1) },
                                        enabled = index < queue.size - 1,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowDown, "Move down", Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Tab 2: Technical Media Details
                item {
                    val file = File(item.targetPath)
                    val lyrics = remember(item.id, item.targetPath) { findLyrics(item.targetPath) }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DetailGlassRow("Title", item.title)
                        DetailGlassRow("Artist / Uploader", item.uploader.ifBlank { "Unknown" })
                        DetailGlassRow("Duration", formatDuration(duration))
                        DetailGlassRow("Playback Speed", "${speed}x")
                        DetailGlassRow("Format / Container", if (item.targetPath.endsWith(".mp3")) "Audio / MP3" else "Media File")
                        DetailGlassRow(
                            "File Size",
                            if (file.exists()) "%.2f MB".format(file.length() / 1048576f) else "Streaming / Temporary"
                        )
                        DetailGlassRow("File Path", item.targetPath.ifBlank { "App Storage" })
                        if (lyrics != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                )
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    Text(
                                        "Lyrics",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        lyrics,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 14,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showEqualizer) {
            EqualizerDialog(onDismiss = { showEqualizer = false })
        }

        if (showTrimmer) {
            AudioTrimmerDialog(media = item, onDismiss = { showTrimmer = false })
        }

        if (showPlaylistDialog && isDbItem) {
            PlaylistPickerDialog(
                playlists = playlists,
                onCreate = { name -> scope.launch { runCatching { repository.createPlaylist(name) } } },
                onPick = { pid ->
                    scope.launch {
                        runCatching { repository.addSongToPlaylist(pid, item.id) }
                        showPlaylistDialog = false
                    }
                },
                onDismiss = { showPlaylistDialog = false }
            )
        }
    }
}

@Composable
private fun DetailGlassRow(label: String, value: String) {    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                fontSize = 13.sp
            )
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1.3f),
                fontSize = 13.sp
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

/** Reads a same-name .lrc (timestamps stripped) or .txt next to the audio file. */
private fun findLyrics(targetPath: String): String? {
    return runCatching {
        val media = File(targetPath)
        if (!media.exists()) return null
        val dir = media.parentFile ?: return null
        val base = media.nameWithoutExtension
        val lrc = File(dir, "$base.lrc")
        if (lrc.exists()) {
            val lines = lrc.readLines()
                .map { it.replace(Regex("\\[\\d{1,3}:\\d{2}(\\.\\d{1,3})?\\]"), "").trim() }
                .filter { it.isNotBlank() }
            if (lines.isNotEmpty()) return lines.take(80).joinToString("\n")
        }
        val txt = File(dir, "$base.txt")
        if (txt.exists()) {
            val text = txt.readText().trim().take(3000)
            if (text.isNotBlank()) return text
        }
        null
    }.getOrNull()
}

/** Retro-style add-to-playlist dialog with inline creation. */
@Composable
private fun PlaylistPickerDialog(
    playlists: List<PlaylistEntity>,
    onCreate: (String) -> Unit,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to playlist", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        placeholder = { Text("New playlist…") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newName.isNotBlank()) {
                                onCreate(newName.trim())
                                newName = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.PlaylistAdd, "Create playlist", tint = NovaAqua)
                    }
                }
                if (playlists.isEmpty()) {
                    Text(
                        "No playlists yet — create one above.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(modifier = Modifier.height(220.dp)) {
                        items(playlists, key = { it.id }) { pl ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(pl.id) }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(pl.name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", fontWeight = FontWeight.Bold) }
        }
    )
}
