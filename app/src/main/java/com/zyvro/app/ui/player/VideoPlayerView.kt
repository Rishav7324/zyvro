package com.zyvro.app.ui.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.util.Rational
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.zyvro.app.player.MediaPlayerManager
import com.zyvro.app.player.PlayerTrack
import com.zyvro.app.ui.components.equalizer.EqualizerDialog
import com.zyvro.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class PlayerGestureMode { NONE, BRIGHTNESS, VOLUME, SEEK }

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerView(onClose: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val manager = MediaPlayerManager.getInstance(context)

    val media by manager.currentMedia.collectAsState()
    val playing by manager.isPlaying.collectAsState()
    val position by manager.currentPosition.collectAsState()
    val duration by manager.duration.collectAsState()
    val speed by manager.playbackSpeed.collectAsState()
    val loopA by manager.loopPointA.collectAsState()
    val loopB by manager.loopPointB.collectAsState()

    var controlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var isLandscape by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var resizeModeName by remember { mutableStateOf("Fit") }
    var moreMenuExpanded by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    var showTrackPicker by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var resumeOfferMs by remember { mutableLongStateOf(0L) }

    val audioTracks by manager.audioTracks.collectAsState()
    val subtitleTracks by manager.subtitleTracks.collectAsState()
    val subtitlesOn by manager.subtitlesEnabled.collectAsState()
    val playerError by manager.playerError.collectAsState()

    // Gesture HUD states
    var gestureMode by remember { mutableStateOf(PlayerGestureMode.NONE) }
    var hudBrightness by remember { mutableFloatStateOf(0.5f) }
    var hudVolume by remember { mutableFloatStateOf(0.5f) }
    var hudSeekDeltaSeconds by remember { mutableLongStateOf(0L) }
    var hudSeekTargetMs by remember { mutableLongStateOf(0L) }
    var skipRippleText by remember { mutableStateOf<String?>(null) }
    var generalHudText by remember { mutableStateOf<String?>(null) }

    val item = media ?: return

    // NextPlayer-style resume: offer the saved position once per item.
    LaunchedEffect(item.id) {
        resumeOfferMs = 0L
        val saved = manager.getResumePosition(item.id)
        val dur = manager.duration.value
        if (dur > 0 && saved > 10_000L && saved < dur - 10_000L) {
            resumeOfferMs = saved
        } else if (dur <= 0 && saved > 10_000L) {
            // Duration not known yet; re-check after ready below.
            resumeOfferMs = -1L
        }
    }
    // If duration arrived late, validate the pending offer.
    LaunchedEffect(duration) {
        if (resumeOfferMs == -1L && duration > 0) {
            val saved = manager.getResumePosition(item.id)
            resumeOfferMs = if (saved > 10_000L && saved < duration - 10_000L) saved else 0L
        }
        // Hide the offer once the user has caught up / passed it.
        if (resumeOfferMs > 0 && position >= resumeOfferMs - 3_000L) {
            resumeOfferMs = 0L
        }
    }

    // Keep screen on during playback
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.window?.let {
                WindowCompat.getInsetsController(it, it.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Auto-hide controls after 4 seconds of active playback
    LaunchedEffect(controlsVisible, playing, isLocked) {
        if (controlsVisible && playing && !isLocked) {
            delay(4000)
            controlsVisible = false
        }
    }

    // Auto-dismiss transient skip ripple HUD
    LaunchedEffect(skipRippleText) {
        if (skipRippleText != null) {
            delay(750)
            skipRippleText = null
        }
    }

    // Auto-dismiss general HUD bubble
    LaunchedEffect(generalHudText) {
        if (generalHudText != null) {
            delay(1100)
            generalHudText = null
        }
    }

    fun toggleLandscape() {
        isLandscape = !isLandscape
        activity?.requestedOrientation = if (isLandscape) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        activity?.window?.let { window ->
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (isLandscape) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        generalHudText = if (isLandscape) "Landscape Mode" else "Portrait Mode"
    }

    fun cycleAspectRatio() {
        resizeMode = when (resizeMode) {
            AspectRatioFrameLayout.RESIZE_MODE_FIT -> {
                resizeModeName = "Crop to Fill (Zoom)"
                AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> {
                resizeModeName = "100% Stretch (Fill)"
                AspectRatioFrameLayout.RESIZE_MODE_FILL
            }
            AspectRatioFrameLayout.RESIZE_MODE_FILL -> {
                resizeModeName = "Fixed Width"
                AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
            }
            else -> {
                resizeModeName = "Fit Screen"
                AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        }
        generalHudText = resizeModeName
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Unified MX Player gesture detector using pointerInput and awaitEachGesture
                .pointerInput(isLocked) {
                    val touchSlop = viewConfiguration.touchSlop
                    var lastTapTime = 0L

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downTime = System.currentTimeMillis()
                        val downPos = down.position
                        val containerWidth = size.width
                        val containerHeight = size.height
                        val isLeftHalf = downPos.x < (containerWidth / 2f)

                        // Initialize brightness & volume state on down
                        var currentBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
                        if (currentBrightness < 0f) {
                            currentBrightness = runCatching {
                                Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
                            }.getOrDefault(0.5f)
                        }

                        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                        val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                        var currentVolProgress = curVol.toFloat() / maxVol

                        val initialSeekMs = manager.currentPosition.value
                        var seekDeltaMs = 0L

                        var mode = PlayerGestureMode.NONE
                        var totalDx = 0f
                        var totalDy = 0f

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break

                            if (change.isConsumed) break

                            if (!change.pressed) {
                                // Pointer lifted (ACTION_UP)
                                if (mode != PlayerGestureMode.NONE) {
                                    change.consume()
                                    if (mode == PlayerGestureMode.SEEK) {
                                        val totalDur = manager.duration.value.coerceAtLeast(1L)
                                        val finalTarget = (initialSeekMs + seekDeltaMs).coerceIn(0L, totalDur)
                                        manager.seekTo(finalTarget)
                                    }
                                    gestureMode = PlayerGestureMode.NONE
                                } else {
                                    // Tap / Double Tap Disambiguation
                                    val now = System.currentTimeMillis()
                                    val isTap = (now - downTime < 350L) &&
                                        (abs(totalDx) < touchSlop) &&
                                        (abs(totalDy) < touchSlop)

                                    if (isTap) {
                                        if (now - lastTapTime < 300L) {
                                            // Double tap registered
                                            lastTapTime = 0L
                                            if (!isLocked) {
                                                if (downPos.x < containerWidth * 0.35f) {
                                                    manager.seekRewind(10000)
                                                    skipRippleText = "−10 sec"
                                                } else if (downPos.x > containerWidth * 0.65f) {
                                                    manager.seekForward(10000)
                                                    skipRippleText = "+10 sec"
                                                } else {
                                                    manager.togglePlayPause()
                                                }
                                            }
                                        } else {
                                            // Single tap
                                            lastTapTime = now
                                            controlsVisible = !controlsVisible
                                        }
                                    }
                                }
                                break
                            }

                            val dx = change.positionChange().x
                            val dy = change.positionChange().y
                            totalDx += dx
                            totalDy += dy

                            if (!isLocked) {
                                if (mode == PlayerGestureMode.NONE) {
                                    if (abs(totalDx) > touchSlop && abs(totalDx) > abs(totalDy) * 1.2f) {
                                        mode = PlayerGestureMode.SEEK
                                        gestureMode = PlayerGestureMode.SEEK
                                    } else if (abs(totalDy) > touchSlop && abs(totalDy) > abs(totalDx) * 1.2f) {
                                        mode = if (isLeftHalf) PlayerGestureMode.BRIGHTNESS else PlayerGestureMode.VOLUME
                                        gestureMode = mode
                                    }
                                }

                                if (mode != PlayerGestureMode.NONE) {
                                    change.consume()
                                    when (mode) {
                                        PlayerGestureMode.BRIGHTNESS -> {
                                            // Drag up increases brightness (-dy)
                                            val delta = -dy / (containerHeight * 0.8f)
                                            currentBrightness = (currentBrightness + delta).coerceIn(0.01f, 1.0f)
                                            activity?.window?.attributes = activity?.window?.attributes?.apply {
                                                screenBrightness = currentBrightness
                                            }
                                            hudBrightness = currentBrightness
                                        }
                                        PlayerGestureMode.VOLUME -> {
                                            val delta = -dy / (containerHeight * 0.8f)
                                            currentVolProgress = (currentVolProgress + delta).coerceIn(0f, 1f)
                                            val targetVol = (currentVolProgress * maxVol).roundToInt().coerceIn(0, maxVol)
                                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                                            hudVolume = currentVolProgress
                                        }
                                        PlayerGestureMode.SEEK -> {
                                            val totalDur = manager.duration.value.coerceAtLeast(1L)
                                            val scrubWindow = 90_000L.coerceAtMost(totalDur)
                                            val stepDelta = ((dx / containerWidth) * scrubWindow).toLong()
                                            seekDeltaMs += stepDelta
                                            hudSeekDeltaSeconds = seekDeltaMs / 1000L
                                            hudSeekTargetMs = (initialSeekMs + seekDeltaMs).coerceIn(0L, totalDur)
                                        }
                                        PlayerGestureMode.NONE -> Unit
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            // LAYER 1: ExoPlayer Video Canvas (Full-bleed Edge-to-Edge)
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = manager.player
                        useController = false
                        this.resizeMode = resizeMode
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { it.resizeMode = resizeMode },
                modifier = Modifier.fillMaxSize()
            )

            // LAYER 2: Gesture HUD Displays
            // Vertical Brightness HUD (Left)
            if (gestureMode == PlayerGestureMode.BRIGHTNESS) {
                VerticalLevelHUD(
                    icon = if (hudBrightness > 0.5f) Icons.Default.BrightnessHigh else Icons.Default.BrightnessLow,
                    title = "Brightness",
                    percent = (hudBrightness * 100).roundToInt(),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 36.dp)
                )
            }

            // Vertical Volume HUD (Right)
            if (gestureMode == PlayerGestureMode.VOLUME) {
                VerticalLevelHUD(
                    icon = if (hudVolume == 0f) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                    title = "Volume",
                    percent = (hudVolume * 100).roundToInt(),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 36.dp)
                )
            }

            // Seek Scrub HUD (Center)
            if (gestureMode == PlayerGestureMode.SEEK) {
                SeekScrubHUD(
                    deltaSeconds = hudSeekDeltaSeconds,
                    targetTimeMs = hudSeekTargetMs,
                    totalDurationMs = duration.coerceAtLeast(1L),
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Double Tap Skip Ripple HUD
            if (skipRippleText != null) {
                DoubleTapSkipHUD(text = skipRippleText ?: "", modifier = Modifier.align(Alignment.Center))
            }

            // General Notification Bubble
            if (generalHudText != null) {
                GeneralHudPill(text = generalHudText ?: "", modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp))
            }

            // LAYER 3: Screen Lock Unlock Button
            if (isLocked) {
                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(tween(180)),
                    exit = fadeOut(tween(150)),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 24.dp)
                ) {
                    IconButton(
                        onClick = {
                            isLocked = false
                            controlsVisible = true
                            generalHudText = "Screen Unlocked"
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.75f))
                            .border(1.5.dp, NovaPrimary, CircleShape)
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = "Unlock Controls", tint = NovaPrimary)
                    }
                }
            } else {
                // LAYER 4: Full MX Player Controls Overlay
                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(tween(180)),
                    exit = fadeOut(tween(150)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Ambient Gradients
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .align(Alignment.TopCenter)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f))
                                    )
                                )
                        )

                        // Top Controls Bar (Safely inside safeDrawing / status bars)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .safeDrawingPadding()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onClose) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = item.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = item.uploader.ifBlank { "Zyvro Video Player" },
                                    color = Color.White.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Audio Background Switch
                            IconButton(onClick = {
                                manager.setVideoExpanded(false)
                                manager.setAudioSheetOpen(true)
                            }) {
                                Icon(Icons.Default.Headphones, contentDescription = "Switch to Audio", tint = Color.White)
                            }

                            // Equalizer Dialog
                            IconButton(onClick = { showEqualizer = true }) {
                                Icon(Icons.Default.Tune, contentDescription = "Equalizer", tint = Color.White)
                            }

                            // PiP Mode
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                IconButton(onClick = {
                                    runCatching {
                                        val rational = Rational(16, 9)
                                        activity?.enterPictureInPictureMode(
                                            PictureInPictureParams.Builder().setAspectRatio(rational).build()
                                        )
                                    }
                                }) {
                                    Icon(Icons.Default.PictureInPicture, contentDescription = "Picture-in-Picture", tint = Color.White)
                                }
                            }

                            // Overflow Menu
                            IconButton(onClick = { moreMenuExpanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = Color.White)
                            }

                            DropdownMenu(
                                expanded = moreMenuExpanded,
                                onDismissRequest = { moreMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Screen Lock") },
                                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                                    onClick = {
                                        isLocked = true
                                        controlsVisible = false
                                        moreMenuExpanded = false
                                        generalHudText = "Screen Locked"
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Aspect Ratio ($resizeModeName)") },
                                    leadingIcon = { Icon(Icons.Default.AspectRatio, null) },
                                    onClick = {
                                        cycleAspectRatio()
                                        moreMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isLandscape) "Switch to Portrait" else "Switch to Landscape") },
                                    leadingIcon = { Icon(Icons.Default.ScreenRotation, null) },
                                    onClick = {
                                        toggleLandscape()
                                        moreMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Playback Speed (${speed}x)") },
                                    leadingIcon = { Icon(Icons.Default.Speed, null) },
                                    onClick = {
                                        val nextSpeed = if (speed >= 2.0f) 0.5f else speed + 0.25f
                                        manager.setSpeed(nextSpeed)
                                        moreMenuExpanded = false
                                    }
                                )
                                if (audioTracks.size > 1) {
                                    DropdownMenuItem(
                                        text = { Text("Audio Track (${audioTracks.count { it.isSelected }} / ${audioTracks.size})") },
                                        leadingIcon = { Icon(Icons.Default.Audiotrack, null) },
                                        onClick = {
                                            showTrackPicker = true
                                            moreMenuExpanded = false
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (subtitleTracks.isEmpty()) "Subtitles (None Found)"
                                            else if (subtitlesOn) "Subtitles (On)"
                                            else "Subtitles (Off)"
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Default.ClosedCaption, null) },
                                    onClick = {
                                        if (subtitleTracks.isEmpty()) {
                                            generalHudText = "No subtitle tracks in this file"
                                        } else {
                                            showTrackPicker = true
                                        }
                                        moreMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Set Loop Point A") },
                                    onClick = {
                                        manager.setLoopPointA()
                                        moreMenuExpanded = false
                                        generalHudText = "Loop A Set"
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Set Loop Point B") },
                                    onClick = {
                                        manager.setLoopPointB()
                                        moreMenuExpanded = false
                                        generalHudText = "Loop B Set"
                                    }
                                )
                                if (loopA != null || loopB != null) {
                                    DropdownMenuItem(
                                        text = { Text("Clear A-B Loop") },
                                        onClick = {
                                            manager.clearAbLoop()
                                            moreMenuExpanded = false
                                            generalHudText = "Loop Cleared"
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Playback stats") },
                                    leadingIcon = { Icon(Icons.Default.Info, null) },
                                    onClick = {
                                        showStats = true
                                        moreMenuExpanded = false
                                    }
                                )
                            }
                        }

                        // Bottom Controls Bar (Safely inside safeDrawing / navigation bars)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .safeDrawingPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            // Timeline Scrubber Row
                            val safeDur = duration.coerceAtLeast(1L)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatDuration(position),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Slider(
                                    value = position.coerceIn(0L, safeDur).toFloat(),
                                    onValueChange = { manager.seekTo(it.toLong()) },
                                    valueRange = 0f..safeDur.toFloat(),
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = NovaPrimary,
                                        activeTrackColor = NovaPrimary,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = formatDuration(duration),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Playback Buttons Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // -10s Rewind
                                IconButton(onClick = {
                                    manager.seekRewind(10000)
                                    skipRippleText = "−10 sec"
                                }) {
                                    Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Play / Pause Button
                                IconButton(
                                    onClick = { manager.togglePlayPause() },
                                    modifier = Modifier
                                        .size(54.dp)
                                        .shadow(12.dp, CircleShape, spotColor = NovaPrimary.copy(alpha = 0.5f))
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(GradientCyan))
                                ) {
                                    Icon(
                                        imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = Color(0xFF001824),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // +10s Forward
                                IconButton(onClick = {
                                    manager.seekForward(10000)
                                    skipRippleText = "+10 sec"
                                }) {
                                    Icon(Icons.Default.Forward10, contentDescription = "Forward 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                // Aspect Ratio Button
                                IconButton(onClick = { cycleAspectRatio() }) {
                                    Icon(
                                        imageVector = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) Icons.Default.Fullscreen else Icons.Default.FullscreenExit,
                                        contentDescription = "Cycle Aspect Ratio",
                                        tint = Color.White
                                    )
                                }

                                // Orientation Toggle Button
                                IconButton(onClick = { toggleLandscape() }) {
                                    Icon(Icons.Default.ScreenRotation, contentDescription = "Orientation", tint = Color.White)
                                }

                                // Lock Button
                                IconButton(onClick = {
                                    isLocked = true
                                    controlsVisible = false
                                    generalHudText = "Screen Locked"
                                }) {
                                    Icon(Icons.Default.Lock, contentDescription = "Lock Screen", tint = Color.White)
                                }
                            }

                            // Speed Selector Chips
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            ) {
                                items(listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 2.5f)) { itemSpeed ->
                                    FilterChip(
                                        selected = speed == itemSpeed,
                                        onClick = { manager.setSpeed(itemSpeed) },
                                        label = { Text("${itemSpeed}x", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NovaPrimary,
                                            selectedLabelColor = Color(0xFF001824),
                                            containerColor = Color.White.copy(alpha = 0.15f),
                                            labelColor = Color.White
                                        ),
                                        border = null
                                    )
                                }
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

    if (showTrackPicker) {
        TrackPickerDialog(
            audioTracks = audioTracks,
            subtitleTracks = subtitleTracks,
            subtitlesOn = subtitlesOn,
            onSelectAudio = { manager.selectAudioTrack(it) },
            onSelectSubtitle = { manager.selectSubtitleTrack(it) },
            onDisableSubtitles = { manager.disableSubtitles() },
            onDismiss = { showTrackPicker = false }
        )
    }

    // Playback error pill (instead of a stuck spinner)
    if (playerError != null && !isLocked) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 96.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFB3261E).copy(alpha = 0.92f)
            ) {
                Text(
                    text = playerError ?: "",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }

    // Resume-from-bookmark pill
    if (resumeOfferMs > 0 && !isLocked) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 148.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.Black.copy(alpha = 0.82f),
                border = androidx.compose.foundation.BorderStroke(1.dp, NovaPrimary.copy(alpha = 0.5f)),
                modifier = Modifier.clickable {
                    manager.seekTo(resumeOfferMs)
                    resumeOfferMs = 0L
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = NovaPrimary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Resume from ${formatDuration(resumeOfferMs)}",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showStats) {
        val stats = manager.getPlaybackStats()
        Dialog(onDismissRequest = { showStats = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Playback stats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    StatsRow("Resolution", stats?.resolution ?: "—")
                    StatsRow("Video codec", stats?.videoCodec ?: "—")
                    StatsRow("Audio codec", stats?.audioCodec ?: "—")
                    StatsRow("Bitrate", stats?.bitrate ?: "—")
                    StatsRow("Frame rate", stats?.frameRate ?: "—")
                    StatsRow("Speed", "${speed}x")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        androidx.compose.material3.TextButton(onClick = { showStats = false }) {
                            Text("Close", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VerticalLevelHUD(
    icon: ImageVector,
    title: String,
    percent: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(64.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.82f))
            .border(1.2.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(vertical = 14.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(24.dp))

            // Vertical Level Track
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .weight(1f)
                    .padding(vertical = 8.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight((percent / 100f).coerceIn(0f, 1f))
                        .clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(NovaPrimary, NovaViolet)))
                )
            }

            Text(
                text = "$percent%",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun SeekScrubHUD(
    deltaSeconds: Long,
    targetTimeMs: Long,
    totalDurationMs: Long,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color.Black.copy(alpha = 0.86f))
            .border(1.2.dp, NovaPrimary.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
            .padding(horizontal = 26.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val isPositive = deltaSeconds >= 0
            val deltaText = if (isPositive) "+${deltaSeconds}s" else "${deltaSeconds}s"
            val deltaColor = if (isPositive) Color(0xFF00E676) else Color(0xFFFF5252)

            Text(
                text = deltaText,
                color = deltaColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${formatDuration(targetTimeMs)} / ${formatDuration(totalDurationMs)}",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DoubleTapSkipHUD(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.78f))
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun GeneralHudPill(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.8f))
            .border(1.dp, NovaPrimary.copy(alpha = 0.5f), CircleShape)
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = NovaPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

private fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

/** Audio-track + subtitle picker (embedded tracks and sideloaded sidecars). */@Composable
private fun TrackPickerDialog(
    audioTracks: List<PlayerTrack>,
    subtitleTracks: List<PlayerTrack>,
    subtitlesOn: Boolean,
    onSelectAudio: (PlayerTrack) -> Unit,
    onSelectSubtitle: (PlayerTrack) -> Unit,
    onDisableSubtitles: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .height(460.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "Audio & Subtitles",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
                LazyColumn(modifier = Modifier.weight(1f)) {
                    if (audioTracks.size > 1) {
                        item {
                            Text(
                                text = "AUDIO",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                            )
                        }
                        items(audioTracks, key = { "a${it.group.hashCode()}:${it.trackIndex}" }) { track ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectAudio(track) }
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = track.isSelected, onClick = { onSelectAudio(track) })
                                Spacer(Modifier.width(4.dp))
                                Text(track.label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    item {
                        Text(
                            text = "SUBTITLES",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                        )
                    }
                    if (subtitleTracks.isEmpty()) {
                        item {
                            Text(
                                text = "No subtitle tracks. Place a same-name .srt/.vtt file next to the video to auto-load it.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                            )
                        }
                    } else {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onDisableSubtitles() }
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = !subtitlesOn, onClick = { onDisableSubtitles() })
                                Spacer(Modifier.width(4.dp))
                                Text("Off", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        items(subtitleTracks, key = { "s${it.group.hashCode()}:${it.trackIndex}" }) { track ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectSubtitle(track) }
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = subtitlesOn && track.isSelected,
                                    onClick = { onSelectSubtitle(track) }
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(track.label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                TextButtonRow(label = "Done", onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun TextButtonRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.End
    ) {
        androidx.compose.material3.TextButton(onClick = onClick) {
            Text(label, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatsRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
