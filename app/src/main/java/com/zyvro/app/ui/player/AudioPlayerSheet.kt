package com.zyvro.app.ui.player

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
// RETRO MUSIC PLAYER — Now Playing Card Architecture
// Identical styling to RetroMusicPlayer (Card Theme with Ambient Glow,
// Tap-to-lyrics, Iconic 5-Button Retro Controls, and Slide-Up Queue Sheet)
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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

    var showQueueSheet     by remember { mutableStateOf(false) }
    var showLyricsOverlay  by remember { mutableStateOf(false) }
    var showRemainingTime  by remember { mutableStateOf(false) }
    var showMenu           by remember { mutableStateOf(false) }
    var showEqualizer      by remember { mutableStateOf(false) }
    var showTrimmer        by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var showInfoDialog     by remember { mutableStateOf(false) }
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

    // Breathing artwork scale animation
    val artScale by animateFloatAsState(
        targetValue   = if (playing) 1.0f else 0.92f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "art-scale"
    )

    // Ambient glow rotation for vinyl/retro glow
    val infiniteTransition = rememberInfiniteTransition(label = "ambient-aurora")
    val auroraRotation by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing)),
        label         = "ambient-rotation"
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

    // Format badge calculation (e.g. "MP3 • 320 KBPS" or "AAC • 256 KBPS")
    val formatBadge = remember(item.targetPath) {
        val ext = item.targetPath.substringAfterLast(".", "").uppercase()
        val file = File(item.targetPath)
        val sizeMb = if (file.exists()) "%.1f MB".format(file.length() / 1048576f) else "STREAM"
        if (ext.isNotBlank()) "$ext • $sizeMb" else "AUDIO • HQ"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = if (isDark) SpaceBlack else LightBg,
        shape            = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle       = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            // ── Drag Handle ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(if (isDark) SpaceBorder else Color(0xFFD6DFEC))
                )
            }

            // ── Top Header Bar (Retro Collapse + Badge + Menu) ──────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Collapse circular button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isDark) SpaceCardHigh else Color(0xFFEFF4FB))
                ) {
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        modifier = Modifier.size(26.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Center Title & Format Pill
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "NOW PLAYING",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = NovaPrimary,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NovaPrimary.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            formatBadge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NovaPrimary,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Right Overflow Menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isDark) SpaceCardHigh else Color(0xFFEFF4FB))
                    ) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = "Options",
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier
                            .background(if (isDark) SpaceCardHigh else Color.White)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Equalizer") },
                            leadingIcon = { Icon(Icons.Rounded.GraphicEq, null, tint = NovaViolet) },
                            onClick = { showMenu = false; showEqualizer = true }
                        )
                        DropdownMenuItem(
                            text = { Text(if (sleepTimerActive) "Sleep Timer: ${sleepTimerMinutes}m" else "Sleep Timer") },
                            leadingIcon = { Icon(Icons.Rounded.Timer, null, tint = NovaPrimary) },
                            onClick = {
                                showMenu = false
                                val options = listOf(0, 15, 30, 45, 60)
                                val next = options[(options.indexOf(sleepTimerMinutes) + 1) % options.size]
                                startSleepTimer(next)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Set Ringtone / Trim") },
                            leadingIcon = { Icon(Icons.Rounded.ContentCut, null, tint = NovaRose) },
                            onClick = { showMenu = false; showTrimmer = true }
                        )
                        if (isDbItem) {
                            DropdownMenuItem(
                                text = { Text("Add to Playlist") },
                                leadingIcon = { Icon(Icons.Rounded.PlaylistAdd, null, tint = NovaPrimary) },
                                onClick = { showMenu = false; showPlaylistDialog = true }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Track Details") },
                            leadingIcon = { Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = { showMenu = false; showInfoDialog = true }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Hero Album Art Card (Retro Floating Card + Tap-to-Lyrics) ─
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                // Ambient diffuse glow behind the card
                if (isDark) {
                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .graphicsLayer { rotationZ = auroraRotation }
                            .background(
                                Brush.sweepGradient(
                                    listOf(
                                        NovaPrimary.copy(alpha = 0.28f),
                                        NovaViolet.copy(alpha = 0.22f),
                                        NovaRose.copy(alpha = 0.16f),
                                        NovaPrimary.copy(alpha = 0.28f)
                                    )
                                ),
                                CircleShape
                            )
                            .blur(48.dp)
                    )
                }

                // Squircle Card
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .scale(artScale)
                        .shadow(
                            elevation = if (playing) 28.dp else 12.dp,
                            shape = RoundedCornerShape(28.dp),
                            spotColor = NovaPrimary.copy(alpha = if (playing) 0.35f else 0.12f)
                        )
                        .clip(RoundedCornerShape(28.dp))
                        .background(if (isDark) SpaceCardHigh else Color(0xFFE4ECF7))
                        .border(
                            1.dp,
                            if (isDark) SpaceBorder else Color.White,
                            RoundedCornerShape(28.dp)
                        )
                        .clickable { showLyricsOverlay = !showLyricsOverlay },
                    contentAlignment = Alignment.Center
                ) {
                    // Artwork Image
                    val imageModel = remember(item.thumbnailUrl, item.targetPath) {
                        when {
                            item.thumbnailUrl.isNotBlank() -> item.thumbnailUrl
                            item.targetPath.isNotBlank()   -> File(item.targetPath)
                            else -> null
                        }
                    }
                    if (imageModel != null) {
                        AsyncImage(
                            model = coil.request.ImageRequest.Builder(context)
                                .data(imageModel)
                                .crossfade(true)
                                .build(),
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Rounded.MusicNote,
                            null,
                            tint = NovaPrimary.copy(alpha = 0.5f),
                            modifier = Modifier.size(88.dp)
                        )
                    }

                    // Lyrics Overlay (Tap toggles)
                    if (showLyricsOverlay) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.82f))
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (!lyrics.isNullOrBlank()) {
                                    Text(
                                        lyrics,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        lineHeight = 24.sp,
                                        textAlign = TextAlign.Center
                                    )
                                } else {
                                    Icon(
                                        Icons.Rounded.FormatQuote,
                                        null,
                                        tint = NovaPrimary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "No lyrics available",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Tap again to return to album cover",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Tap-to-lyrics pill hint at bottom right
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (showLyricsOverlay) Icons.Rounded.Image else Icons.Rounded.Lyrics,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (showLyricsOverlay) "Cover" else "Lyrics",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Track Title, Artist & Favorite ──────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.basicMarquee()
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.uploader.ifBlank { "Unknown Artist" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = NovaPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Bouncy Favorite Heart
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
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (isFav) NovaRose.copy(alpha = 0.14f) else Color.Transparent)
                ) {
                    Icon(
                        if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFav) NovaRose else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Retro Scrubber / Progress Bar ───────────────────────────
            var dragging by remember { mutableStateOf(false) }
            var dragValue by remember { mutableFloatStateOf(0f) }
            val displayPos = if (dragging) dragValue else position.toFloat()
            val dur        = duration.toFloat().coerceAtLeast(1f)

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Slider(
                    value = (displayPos / dur).coerceIn(0f, 1f),
                    onValueChange = { frac ->
                        dragging  = true
                        dragValue = frac * dur
                    },
                    onValueChangeFinished = {
                        manager.seekTo(dragValue.toLong())
                        dragging = false
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = NovaPrimary,
                        activeTrackColor = NovaPrimary,
                        inactiveTrackColor = if (isDark) SpaceBorder else Color(0xFFD6E2F3)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        formatDuration(displayPos.toLong()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    // Tap on duration to toggle remaining time (-02:45 vs 03:15)
                    Text(
                        if (showRemainingTime) "-${formatDuration((dur - displayPos).toLong().coerceAtLeast(0L))}"
                        else formatDuration(duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { showRemainingTime = !showRemainingTime }
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // ── Iconic Retro 5-Button Playback Controls ──────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Shuffle
                IconButton(
                    onClick = { manager.toggleShuffle() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (shuffle) NovaPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                        if (shuffle) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(NovaPrimary)
                            )
                        }
                    }
                }

                // 2. Previous
                IconButton(
                    onClick = { manager.playPrevious() },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // 3. Center Retro Floating Action Play/Pause Button (FAB)
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = CircleShape,
                            spotColor = NovaPrimary.copy(alpha = 0.5f)
                        )
                        .clip(CircleShape)
                        .background(Brush.linearGradient(GradientCyan))
                        .clickable { manager.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (playing) "Pause" else "Play",
                        tint = Color(0xFF001824),
                        modifier = Modifier.size(36.dp)
                    )
                }

                // 4. Next
                IconButton(
                    onClick = { manager.playNext() },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // 5. Repeat (3 states: Off -> All -> One)
                IconButton(
                    onClick = { manager.toggleRepeatMode() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (repeat == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                            contentDescription = "Repeat",
                            tint = if (repeat != Player.REPEAT_MODE_OFF) NovaPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                        if (repeat != Player.REPEAT_MODE_OFF) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(NovaPrimary)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Retro Bottom Dock (Speed, Equalizer, Sleep, Queue) ──────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isDark) SpaceCardHigh else Color(0xFFF1F5FB))
                    .border(0.5.dp, if (isDark) SpaceBorder else Color(0xFFE2EAF4), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Playback speed cycle chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            val speeds = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)
                            val idx = speeds.indexOfFirst { kotlin.math.abs(it - speed) < 0.01f }.let { if (it < 0) 1 else it }
                            val next = speeds[(idx + 1) % speeds.size]
                            manager.setSpeed(next)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        "${speed}×",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NovaPrimary
                    )
                }

                // Equalizer button
                IconButton(
                    onClick = { showEqualizer = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Rounded.GraphicEq,
                        contentDescription = "Equalizer",
                        tint = NovaViolet,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Sleep timer button
                IconButton(
                    onClick = {
                        val options = listOf(0, 15, 30, 45, 60)
                        val next = options[(options.indexOf(sleepTimerMinutes) + 1) % options.size]
                        startSleepTimer(next)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Rounded.Timer,
                        contentDescription = "Sleep Timer",
                        tint = if (sleepTimerActive) NovaPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Ringtone Cutter shortcut
                IconButton(
                    onClick = { showTrimmer = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Rounded.ContentCut,
                        contentDescription = "Trim",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Queue button with count pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (showQueueSheet) NovaPrimary.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { showQueueSheet = !showQueueSheet }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.QueueMusic,
                        contentDescription = "Queue",
                        tint = if (showQueueSheet) NovaPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    if (queue.isNotEmpty()) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${queue.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (showQueueSheet) NovaPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // ── Slide-up Queue Bottom Sheet (Retro Style) ──────────────────────
    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            containerColor = if (isDark) SpaceCard else LightBg,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "UP NEXT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = NovaPrimary,
                            letterSpacing = 2.sp
                        )
                        Text(
                            "${queue.size} songs in queue",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showQueueSheet = false }) {
                        Icon(Icons.Rounded.Close, "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (queue.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Queue is empty", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
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
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${idx + 1}",
                                    modifier = Modifier.width(28.dp),
                                    color = if (isCurrent) NovaPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        track.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCurrent) NovaPrimary else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        track.uploader.ifBlank { "Unknown" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isCurrent && playing) {
                                    Icon(
                                        Icons.Rounded.GraphicEq,
                                        contentDescription = "Playing",
                                        tint = NovaPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
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

    if (showInfoDialog) {
        val file = File(item.targetPath)
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Track Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow("Title", item.title)
                    DetailRow("Artist", item.uploader.ifBlank { "Unknown" })
                    DetailRow("Format", item.targetPath.substringAfterLast(".", "MP3").uppercase())
                    DetailRow("Size", if (file.exists()) "%.2f MB".format(file.length() / 1048576f) else "Streaming")
                    DetailRow("Speed", "${speed}×")
                    DetailRow("File Path", item.targetPath.ifBlank { "Local storage" })
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
