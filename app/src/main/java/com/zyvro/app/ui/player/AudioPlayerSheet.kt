package com.zyvro.app.ui.player

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.offset
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.zyvro.app.YtDlpApp
import com.zyvro.app.data.local.PlaylistEntity
import com.zyvro.app.player.MediaPlayerManager
import com.zyvro.app.ui.components.AudioTrimmerDialog
import com.zyvro.app.ui.components.equalizer.EqualizerDialog
import com.zyvro.app.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

// ═══════════════════════════════════════════════════════════════════════════
// ZYVRO AUDIO PLAYER SHEET v4.0 — Deep Space Aura · Spotify-Level Design
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerSheet(onDismiss: () -> Unit) {
    val context    = LocalContext.current
    val scope      = rememberCoroutineScope()
    val manager    = MediaPlayerManager.getInstance(context)
    val isDark     = LocalAppDark.current

    val media     by manager.currentMedia.collectAsState()
    val queue     by manager.queue.collectAsState()
    val playing   by manager.isPlaying.collectAsState()
    val position  by manager.currentPosition.collectAsState()
    val duration  by manager.duration.collectAsState()
    val speed     by manager.playbackSpeed.collectAsState()
    val repeat    by manager.repeatMode.collectAsState()
    val shuffle   by manager.isShuffleEnabled.collectAsState()

    var selectedTab        by remember { mutableIntStateOf(0) }
    var showEqualizer      by remember { mutableStateOf(false) }
    var showTrimmer        by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var sleepTimerMinutes  by remember { mutableIntStateOf(0) }
    var sleepTimerJob      by remember { mutableStateOf<Job?>(null) }
    var sleepTimerActive   by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val item       = media ?: return
    val isDbItem   = item.id in 1 until 1_000_000
    val repository = remember { (context.applicationContext as YtDlpApp).repository }
    var isFavDb    by remember(item.id, item.isFavorite) { mutableStateOf(item.isFavorite) }
    val deviceFavs by repository.preferences.deviceFavorites.collectAsState(initial = emptySet())
    val isFav      = if (isDbItem) isFavDb else deviceFavs.contains(item.targetPath)
    val playlists  by repository.playlists.collectAsState(initial = emptyList())
    val lyrics     = remember(item.targetPath) { findLyrics(item.targetPath) }

    // Artwork scale breathing animation
    val artScale by animateFloatAsState(
        targetValue   = if (playing) 1.0f else 0.88f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "art-scale"
    )

    // Aurora rotation for background
    val infiniteTransition = rememberInfiniteTransition(label = "aurora")
    val auroraRotation by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing)),
        label         = "aurora-rotation"
    )

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
        sheetState       = sheetState,
        containerColor   = if (isDark) SpaceBlack else LightBg,
        shape            = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle       = null
    ) {
        LazyColumn(
            modifier       = Modifier.fillMaxWidth().navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            // ── Drag Handle ─────────────────────────────────────────────
            item {
                Column(
                    modifier            = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(44.dp).height(4.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(listOf(NovaPrimary.copy(alpha = 0.6f), NovaViolet.copy(alpha = 0.4f)))
                            )
                    )
                }
            }

            // ── Header bar (Now Playing + close) ────────────────────────
            item {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.KeyboardArrowDown, "Collapse", Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "NOW PLAYING",
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color      = NovaPrimary,
                            letterSpacing = 2.sp
                        )
                        Text(
                            "Audio Player",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row {
                        IconButton(onClick = { showEqualizer = true }) {
                            Icon(Icons.Default.GraphicEq, "EQ", tint = NovaViolet, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }

            // ── Album Art ───────────────────────────────────────────────
            item {
                Box(
                    modifier            = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp),
                    contentAlignment    = Alignment.Center
                ) {
                    // Aurora blur halo
                    if (isDark) {
                        Box(
                            modifier = Modifier
                                .size(280.dp)
                                .graphicsLayer { rotationZ = auroraRotation }
                                .background(
                                    Brush.sweepGradient(
                                        listOf(
                                            NovaPrimary.copy(alpha = 0.3f),
                                            NovaViolet.copy(alpha = 0.2f),
                                            NovaRose.copy(alpha = 0.15f),
                                            NovaPrimary.copy(alpha = 0.3f)
                                        )
                                    ),
                                    CircleShape
                                )
                                .blur(40.dp)
                        )
                    }

                    // Artwork
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .scale(artScale)
                            .shadow(
                                elevation  = if (playing) 28.dp else 12.dp,
                                shape      = RoundedCornerShape(28.dp),
                                spotColor  = NovaPrimary.copy(alpha = if (playing) 0.4f else 0.15f)
                            )
                            .clip(RoundedCornerShape(28.dp))
                            .background(Brush.linearGradient(listOf(SpaceGlass, SpaceCardHigh))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            null,
                            tint     = NovaPrimary.copy(alpha = 0.5f),
                            modifier = Modifier.size(80.dp)
                        )
                        val imageModel = remember(item.thumbnailUrl, item.targetPath) {
                            when {
                                item.thumbnailUrl.isNotBlank() -> item.thumbnailUrl
                                item.targetPath.isNotBlank()   -> File(item.targetPath)
                                else -> null
                            }
                        }
                        if (imageModel != null) {
                            val request = coil.request.ImageRequest.Builder(context)
                                .data(imageModel)
                                .crossfade(true)
                                .build()
                            AsyncImage(
                                model            = request,
                                contentDescription = item.title,
                                contentScale     = ContentScale.Crop,
                                modifier         = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // ── Track Info + Favourite ──────────────────────────────────
            item {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            item.title,
                            style      = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines   = 2,
                            overflow   = TextOverflow.Ellipsis,
                            color      = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            item.uploader.ifBlank { "Unknown Artist" },
                            style  = MaterialTheme.typography.bodyMedium,
                            color  = NovaPrimary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            scope.launch {
                                if (isDbItem) {
                                    repository.toggleFavorite(item.id)
                                    isFavDb = !isFavDb
                                } else {
                                    repository.preferences.toggleDeviceFavorite(item.targetPath)
                                }
                            }
                        }
                    ) {
                        Icon(
                            if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            "Favorite",
                            tint     = if (isFav) NovaRose else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // ── Progress Slider ─────────────────────────────────────────
            item {
                var dragging by remember { mutableStateOf(false) }
                var dragValue by remember { mutableFloatStateOf(0f) }
                val displayPos = if (dragging) dragValue else position.toFloat()
                val dur        = duration.toFloat().coerceAtLeast(1f)

                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Slider(
                        value       = (displayPos / dur).coerceIn(0f, 1f),
                        onValueChange = { frac ->
                            dragging  = true
                            dragValue = frac * dur
                        },
                        onValueChangeFinished = {
                            manager.seekTo(dragValue.toLong())
                            dragging = false
                        },
                        colors = SliderDefaults.colors(
                            thumbColor            = NovaPrimary,
                            activeTrackColor      = NovaPrimary,
                            inactiveTrackColor    = if (isDark) SpaceBorder else Color(0xFFD0DCFF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatDuration(displayPos.toLong()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatDuration(duration), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ── Main Controls ───────────────────────────────────────────
            item {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    // Shuffle
                    ControlIconButton(
                        icon   = Icons.Default.Shuffle,
                        active = shuffle,
                        size   = 44.dp,
                        onClick = { manager.toggleShuffle() }
                    )

                    // Skip back 10s
                    ControlIconButton(
                        icon   = Icons.Default.Replay10,
                        active = false,
                        size   = 50.dp,
                        onClick = { manager.seekTo((position - 10000L).coerceAtLeast(0L)) }
                    )

                    // Play/Pause (large neon)
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .shadow(20.dp, CircleShape, spotColor = NovaPrimary.copy(alpha = 0.4f))
                            .clip(CircleShape)
                            .background(Brush.linearGradient(GradientCyan))
                            .clickable { manager.togglePlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            "Play/Pause",
                            tint     = Color(0xFF001824),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Forward 10s
                    ControlIconButton(
                        icon   = Icons.Default.Forward10,
                        active = false,
                        size   = 50.dp,
                        onClick = { manager.seekTo((position + 10000L).coerceAtMost(duration)) }
                    )

                    // Repeat
                    ControlIconButton(
                        icon   = if (repeat == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        active = repeat != Player.REPEAT_MODE_OFF,
                        size   = 44.dp,
                        onClick = {
                            manager.toggleRepeatMode()
                        }
                    )
                }
            }

            // ── Skip Track Row ──────────────────────────────────────────
            item {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(horizontal = 48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { manager.playPrevious() }) {
                        Icon(Icons.Rounded.SkipPrevious, "Previous", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurface)
                    }
                    // Speed chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(NovaPrimary.copy(alpha = 0.12f))
                            .border(0.5.dp, NovaPrimary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .clickable {
                                val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
                                val idx    = speeds.indexOfFirst { kotlin.math.abs(it - speed) < 0.01f }.let { if (it < 0) 2 else it }
                                val next   = speeds[(idx + 1) % speeds.size]
                                manager.setSpeed(next)
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text("${speed}×", fontWeight = FontWeight.Bold, color = NovaPrimary, fontSize = 13.sp)
                    }
                    IconButton(onClick = { manager.playNext() }) {
                        Icon(Icons.Rounded.SkipNext, "Next", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            // ── Action chips ────────────────────────────────────────────
            item {
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        ActionChip(
                            icon    = Icons.Default.Timer,
                            label   = if (sleepTimerActive) "${sleepTimerMinutes}m" else "Sleep",
                            active  = sleepTimerActive,
                            onClick = {
                                val options = listOf(0, 5, 10, 15, 30, 60)
                                val next    = options[(options.indexOf(sleepTimerMinutes) + 1) % options.size]
                                startSleepTimer(next)
                            }
                        )
                    }
                    item {
                        ActionChip(Icons.Rounded.ContentCut, "Trim", false) { showTrimmer = true }
                    }
                    if (isDbItem) {
                        item {
                            ActionChip(Icons.Default.PlaylistAdd, "Playlist", false) { showPlaylistDialog = true }
                        }
                    }
                    item {
                        ActionChip(Icons.Default.GraphicEq, "EQ", false) { showEqualizer = true }
                    }
                }
            }

            // ── Tabs: Queue | Lyrics ────────────────────────────────────
            item {
                TabRow(
                    selectedTabIndex  = selectedTab,
                    modifier          = Modifier.padding(horizontal = 20.dp).clip(RoundedCornerShape(14.dp)),
                    containerColor    = if (isDark) SpaceCardHigh else Color(0xFFF0F4FF),
                    contentColor      = NovaPrimary,
                    indicator         = { tabPositions ->
                        Box(
                            modifier = Modifier
                                .tabIndicatorOffset(tabPositions[selectedTab])
                                .height(2.dp)
                                .background(Brush.horizontalGradient(GradientCyan), RoundedCornerShape(1.dp))
                        )
                    }
                ) {
                    listOf("Queue", "Lyrics", "Info").forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick  = { selectedTab = index },
                            text     = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }

            // ── Tab Content ─────────────────────────────────────────────
            when (selectedTab) {
                0 -> {
                    // Queue
                    if (queue.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                                Text("Queue is empty", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        itemsIndexed(queue, key = { _, q -> q.id }) { idx, track ->
                            val isCurrent = track.id == item.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isCurrent) NovaPrimary.copy(alpha = 0.12f)
                                        else Color.Transparent
                                    )
                                    .clickable { manager.playMedia(track) }
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${idx + 1}",
                                    modifier = Modifier.width(28.dp),
                                    color    = if (isCurrent) NovaPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        track.title,
                                        maxLines   = 1,
                                        overflow   = TextOverflow.Ellipsis,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color      = if (isCurrent) NovaPrimary else MaterialTheme.colorScheme.onSurface,
                                        fontSize   = 14.sp
                                    )
                                    Text(
                                        track.uploader.ifBlank { "Unknown" },
                                        style   = MaterialTheme.typography.labelSmall,
                                        color   = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isCurrent && playing) {
                                    Icon(Icons.Default.GraphicEq, "Playing", tint = NovaPrimary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Lyrics
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isDark) SpaceCardHigh else LightCardAlt)
                                .padding(20.dp)
                        ) {
                            if (lyrics != null) {
                                Text(
                                    lyrics,
                                    style   = MaterialTheme.typography.bodyMedium,
                                    color   = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 24.sp
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Default.MusicNote, null, tint = NovaPrimary.copy(alpha = 0.4f), modifier = Modifier.size(40.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text("No lyrics found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Add a .lrc or .txt file next to the audio", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Info
                    item {
                        val file = File(item.targetPath)
                        Column(
                            modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            InfoRow("Title", item.title)
                            InfoRow("Artist", item.uploader.ifBlank { "—" })
                            InfoRow("Speed", "${speed}×")
                            InfoRow("Format", when {
                                item.targetPath.endsWith(".mp3") -> "MP3 Audio"
                                item.targetPath.endsWith(".m4a") -> "M4A Audio"
                                item.targetPath.endsWith(".opus") -> "Opus Audio"
                                else -> "Audio File"
                            })
                            InfoRow("Size", if (file.exists()) "%.2f MB".format(file.length() / 1048576f) else "Streaming")
                            InfoRow("Path", item.targetPath.ifBlank { "App Storage" })
                        }
                    }
                }
            }
        }
    }

    if (showEqualizer) EqualizerDialog(onDismiss = { showEqualizer = false })
    if (showTrimmer)   AudioTrimmerDialog(media = item, onDismiss = { showTrimmer = false })
    if (showPlaylistDialog && isDbItem) {
        PlaylistPickerDialog(
            playlists = playlists,
            onCreate  = { name -> scope.launch { runCatching { repository.createPlaylist(name) } } },
            onPick    = { pid  ->
                scope.launch {
                    runCatching { repository.addSongToPlaylist(pid, item.id) }
                    showPlaylistDialog = false
                }
            },
            onDismiss = { showPlaylistDialog = false }
        )
    }
}

// ── Private Subcomponents ──────────────────────────────────────────────────

@Composable
private fun ControlIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(size)) {
        Icon(
            icon,
            null,
            tint     = if (active) NovaPrimary else LocalContentColor.current.copy(alpha = 0.7f),
            modifier = Modifier.size(size * 0.56f)
        )
    }
}

@Composable
private fun ActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val isDark = LocalAppDark.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (active) NovaPrimary.copy(alpha = 0.15f)
                else if (isDark) SpaceCardHigh else Color(0xFFF0F4FF)
            )
            .border(
                0.5.dp,
                if (active) NovaPrimary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, tint = if (active) NovaPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (active) NovaPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val isDark = LocalAppDark.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) SpaceCardHigh else LightCardAlt)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f), fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End, modifier = Modifier.weight(1.5f))
    }
}

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
        title = { Text("Add to Playlist", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value         = newName,
                        onValueChange = { newName = it },
                        placeholder   = { Text("New playlist…") },
                        singleLine    = true,
                        shape         = RoundedCornerShape(12.dp),
                        modifier      = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { if (newName.isNotBlank()) { onCreate(newName.trim()); newName = "" } }) {
                        Icon(Icons.Default.PlaylistAdd, "Create", tint = NovaPrimary)
                    }
                }
                playlists.forEach { pl ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onPick(pl.id) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(pl.name, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("Done", fontWeight = FontWeight.Bold) } }
    )
}

private fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600; val m = (total % 3600) / 60; val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

private fun findLyrics(targetPath: String): String? = runCatching {
    val media = File(targetPath); if (!media.exists()) return null
    val dir  = media.parentFile ?: return null
    val base = media.nameWithoutExtension
    val lrc  = File(dir, "$base.lrc")
    if (lrc.exists()) {
        val lines = lrc.readLines()
            .map { it.replace(Regex("\\[\\d{1,3}:\\d{2}(\\.\\d{1,3})?\\]"), "").trim() }
            .filter { it.isNotBlank() }
        if (lines.isNotEmpty()) return lines.take(80).joinToString("\n")
    }
    val txt = File(dir, "$base.txt")
    if (txt.exists()) txt.readText().trim().take(3000).ifBlank { null } else null
}.getOrNull()

// Use Material3 TabRowDefaults.tabIndicatorOffset
private fun Modifier.tabIndicatorOffset(currentTabPosition: TabPosition): Modifier =
    this
        .fillMaxWidth()
        .wrapContentSize(Alignment.BottomStart)
        .offset(x = currentTabPosition.left)
        .width(currentTabPosition.width)
