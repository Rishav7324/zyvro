package com.zyvro.app.ui.screens

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zyvro.app.R
import com.zyvro.app.YtDlpApp
import com.zyvro.app.data.local.MediaType
import com.zyvro.app.player.MediaPlayerManager
import com.zyvro.app.ui.components.DownloadItemCard
import com.zyvro.app.ui.components.FormatSelectionSheet
import com.zyvro.app.ui.components.LiquidGlassCard
import com.zyvro.app.ui.components.LiquidGlassPill
import com.zyvro.app.ui.components.NovaButton
import com.zyvro.app.ui.components.VideoPreviewCard
import com.zyvro.app.ui.components.batch.BatchDownloadModal
import com.zyvro.app.ui.components.equalizer.EqualizerDialog
import com.zyvro.app.ui.components.ambientLiquidBackground
import com.zyvro.app.ui.components.liquidGlass
import com.zyvro.app.ui.theme.*
import com.zyvro.app.viewmodel.HomeUiState
import com.zyvro.app.viewmodel.HomeViewModel

// ═══════════════════════════════════════════════════════════════════════════
// ZYVRO HOME SCREEN v4.0 — Deep Space Aura
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as android.app.Application
        )
    ),
    onNavigateToQueue: () -> Unit = {},
    onNavigateToBrowser: () -> Unit = {}
) {
    val context         = LocalContext.current
    val uiState         by viewModel.uiState.collectAsState()
    val urlInput        by viewModel.urlInput.collectAsState()
    val recentDownloads by viewModel.recentDownloads.collectAsState(initial = emptyList())
    val playerManager   = MediaPlayerManager.getInstance(context)
    val isDark          = LocalAppDark.current
    val keyboard        = LocalSoftwareKeyboardController.current

    var showBottomSheet by remember { mutableStateOf(false) }
    var showBatchModal  by remember { mutableStateOf(false) }
    var showEqualizer   by remember { mutableStateOf(false) }
    val sheetState      = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Aurora orb pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "aurora")
    val orb1Scale by infiniteTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.15f,
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "orb1"
    )
    val orb2Scale by infiniteTransition.animateFloat(
        initialValue  = 1.1f,
        targetValue   = 0.9f,
        animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "orb2"
    )

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Aurora Background ──────────────────────────────────────────
        if (isDark) {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .scale(orb1Scale)
                    .offset(x = (-60).dp, y = (-40).dp)
                    .background(
                        Brush.radialGradient(listOf(NovaPrimary.copy(alpha = 0.12f), Color.Transparent)),
                        CircleShape
                    )
                    .blur(60.dp)
            )
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .scale(orb2Scale)
                    .align(Alignment.TopEnd)
                    .offset(x = 60.dp, y = 80.dp)
                    .background(
                        Brush.radialGradient(listOf(NovaViolet.copy(alpha = 0.10f), Color.Transparent)),
                        CircleShape
                    )
                    .blur(50.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .ambientLiquidBackground(isDark)
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding      = PaddingValues(top = 10.dp, bottom = 120.dp)
        ) {

            // ── Header ─────────────────────────────────────────────────
            item {
                ZyvroHeader(
                    isDark         = isDark,
                    onEqualizer    = { showEqualizer = true },
                    onBatch        = {
                        if (uiState is HomeUiState.Success) showBatchModal = true
                        else if (urlInput.isNotBlank()) viewModel.parseUrl(urlInput)
                    }
                )
            }

            // ── URL Input ──────────────────────────────────────────────
            item {
                AuraUrlInput(
                    value    = urlInput,
                    isDark   = isDark,
                    onChange = { viewModel.onUrlChanged(it) },
                    onSearch = {
                        keyboard?.hide()
                        val raw = urlInput.trim()
                        if (raw.isNotBlank()) viewModel.parseUrl(raw)
                    },
                    onPaste  = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val text = clipboard?.primaryClip
                            ?.takeIf { it.description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) }
                            ?.getItemAt(0)?.coerceToText(context)?.toString() ?: ""
                        if (text.isNotBlank()) {
                            viewModel.onUrlChanged(text)
                            keyboard?.hide()
                            viewModel.parseUrl(text)
                        }
                    }
                )
            }

            // ── Platform Chips ─────────────────────────────────────────
            item {
                PlatformChipsRow(
                    isDark   = isDark,
                    onSelect = { url ->
                        viewModel.onUrlChanged(url)
                        onNavigateToBrowser()
                    }
                )
            }

            // ── URL State: Loading / Success / Error ──────────────────
            item {
                AnimatedContent(
                    targetState  = uiState,
                    transitionSpec = {
                        (fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 6 })
                            .togetherWith(fadeOut(tween(160)))
                    },
                    label = "url-state"
                ) { state ->
                    when (state) {
                        is HomeUiState.Loading -> AuraLoadingCard(isDark)
                        is HomeUiState.Success -> VideoPreviewCard(
                            videoInfo         = state.videoInfo,
                            onConfigureDownload = { showBottomSheet = true }
                        )
                        is HomeUiState.Error   -> AuraErrorCard(
                            message  = state.message,
                            isDark   = isDark,
                            onRetry  = { if (urlInput.isNotBlank()) viewModel.parseUrl(urlInput) },
                            onCopy   = {
                                runCatching {
                                    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    cb?.setPrimaryClip(android.content.ClipData.newPlainText("url", urlInput))
                                }
                            }
                        )
                        HomeUiState.Idle -> {}
                    }
                }
            }

            // ── Recent Downloads ───────────────────────────────────────
            if (recentDownloads.isNotEmpty()) {
                item {
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(3.dp, 18.dp)
                                    .background(
                                        Brush.verticalGradient(listOf(NovaPrimary, NovaViolet)),
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text       = "Recent Activity",
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        TextButton(onClick = onNavigateToQueue) {
                            Text(
                                "View All",
                                fontWeight = FontWeight.Bold,
                                color      = NovaPrimary
                            )
                        }
                    }
                }

                items(recentDownloads.take(5), key = { it.id }) { item ->
                    DownloadItemCard(
                        download = item,
                        onCancel = { },
                        onDelete = { },
                        onPlay   = { playerManager.playMedia(item) }
                    )
                }
            }
        }
    }

    // Format Selection Sheet
    if (showBottomSheet && uiState is HomeUiState.Success) {
        val videoInfo = (uiState as HomeUiState.Success).videoInfo
        FormatSelectionSheet(
            sheetState = sheetState,
            videoInfo  = videoInfo,
            onDismiss  = { showBottomSheet = false },
            onStartDownload = { formatId, mediaType, audioExt ->
                viewModel.startDownload(videoInfo, formatId, mediaType, audioExt, autoStart = true)
                showBottomSheet = false
                onNavigateToQueue()
            },
            onQueueDownload = { formatId, mediaType, audioExt ->
                viewModel.startDownload(videoInfo, formatId, mediaType, audioExt, autoStart = false)
                showBottomSheet = false
                onNavigateToQueue()
            }
        )
    }

    // Batch Modal
    if (showBatchModal && uiState is HomeUiState.Success) {
        val info = (uiState as HomeUiState.Success).videoInfo
        BatchDownloadModal(
            playlistTitle = info.title,
            itemsList     = listOf(info),
            onDismiss     = { showBatchModal = false },
            onBatchDownload = { items, formatId, mediaType, audioExt ->
                items.forEach { item -> viewModel.startDownload(item, formatId, mediaType, audioExt) }
                showBatchModal = false
                onNavigateToQueue()
            }
        )
    }

    if (showEqualizer) {
        EqualizerDialog(onDismiss = { showEqualizer = false })
    }
}

// ── Subcomponents ──────────────────────────────────────────────────────────

@Composable
private fun ZyvroHeader(
    isDark: Boolean,
    onEqualizer: () -> Unit,
    onBatch: () -> Unit
) {
    val borderBrush = Brush.linearGradient(
        listOf(NovaPrimary.copy(alpha = 0.5f), NovaViolet.copy(alpha = 0.3f))
    )
    LiquidGlassCard(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(22.dp),
        elevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Logo with glow
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .shadow(12.dp, RoundedCornerShape(14.dp), spotColor = NovaPrimary.copy(alpha = 0.5f))
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, borderBrush, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter           = painterResource(id = R.drawable.app_logo),
                            contentDescription = "Zyvro",
                            modifier          = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text       = "Zyvro",
                                style      = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color      = MaterialTheme.colorScheme.onSurface
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Brush.horizontalGradient(listOf(NovaPrimary, NovaViolet)))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("v4", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        Text(
                            text  = "Download · Convert · Play",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HeaderIconButton(Icons.Rounded.Tune, "Equalizer", NovaPrimary, onEqualizer)
                    HeaderIconButton(Icons.Rounded.PlaylistAdd, "Batch", NovaViolet, onBatch)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Feature chips
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple(Icons.Filled.Bolt,        "Turbo",   GradientCyan),
                    Triple(Icons.Filled.Hd,           "4K/8K",   GradientViolet),
                    Triple(Icons.Filled.AutoAwesome,  "Private", GradientGreen)
                ).forEach { (icon, label, gradient) ->
                    FeatureChip(icon, label, gradient, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HeaderIconButton(icon: ImageVector, desc: String, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.12f))
            .border(0.5.dp, tint.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, desc, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun FeatureChip(
    icon: ImageVector,
    label: String,
    gradient: List<Color>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.horizontalGradient(gradient.map { it.copy(alpha = 0.15f) }))
            .border(0.5.dp, gradient.first().copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = gradient.first(), modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = gradient.first())
        }
    }
}

@Composable
private fun AuraUrlInput(
    value: String,
    isDark: Boolean,
    onChange: (String) -> Unit,
    onSearch: () -> Unit,
    onPaste: () -> Unit
) {
    val glowAlpha by animateFloatAsState(
        targetValue   = if (value.isNotBlank()) 0.5f else 0.15f,
        animationSpec = tween(300),
        label         = "url-glow"
    )
    val borderBrush = Brush.horizontalGradient(
        listOf(NovaPrimary.copy(alpha = glowAlpha), NovaViolet.copy(alpha = glowAlpha * 0.7f))
    )
    val bgColor = if (isDark) SpaceCard.copy(alpha = 0.96f) else Color.White.copy(alpha = 0.98f)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Input field
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(if (value.isNotBlank()) 12.dp else 4.dp, RoundedCornerShape(18.dp),
                    spotColor = NovaPrimary.copy(alpha = glowAlpha * 0.4f))
                .background(bgColor, RoundedCornerShape(18.dp))
                .border(1.dp, borderBrush, RoundedCornerShape(18.dp))
        ) {
            Row(
                modifier          = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Link,
                    "URL",
                    tint     = NovaPrimary.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                OutlinedTextField(
                    value         = value,
                    onValueChange = onChange,
                    modifier      = Modifier.weight(1f),
                    placeholder   = {
                        Text(
                            "Paste any video / audio URL...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    singleLine    = true,
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor   = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    textStyle      = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                if (value.isNotBlank()) {
                    IconButton(onClick = { onChange("") }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Close, "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Action buttons row
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Paste button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isDark) SpaceCardHigh else Color(0xFFF0F4FF))
                    .border(0.5.dp, NovaPrimary.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                    .clickable(onClick = onPaste),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.ContentPaste, "Paste", tint = NovaPrimary, modifier = Modifier.size(16.dp))
                    Text("Paste", fontWeight = FontWeight.SemiBold, color = NovaPrimary, fontSize = 13.sp)
                }
            }

            // Analyze / Download button
            NovaButton(
                onClick = onSearch,
                modifier = Modifier.weight(2f).height(48.dp),
                colors   = listOf(NovaPrimary, NovaPrimaryDeep),
                shape    = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Rounded.Search, "Analyze", tint = Color(0xFF001824), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (value.isBlank()) "Analyze URL" else "Fetch Info",
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFF001824),
                    fontSize   = 14.sp
                )
            }
        }
    }
}

@Composable
private fun PlatformChipsRow(
    isDark: Boolean,
    onSelect: (String) -> Unit
) {
    val platforms = listOf(
        PlatformChip("YouTube",    YouTubeRed,      "https://youtube.com"),
        PlatformChip("Instagram",  InstagramPink,   "https://instagram.com"),
        PlatformChip("TikTok",     TikTokCyan,      "https://tiktok.com"),
        PlatformChip("Twitter",    TwitterBlue,     "https://twitter.com"),
        PlatformChip("Facebook",   FacebookBlue,    "https://facebook.com"),
        PlatformChip("SoundCloud", SoundCloudOrange,"https://soundcloud.com"),
        PlatformChip("Twitch",     TwitchPurple,    "https://twitch.tv"),
        PlatformChip("Reddit",     RedditOrange,    "https://reddit.com"),
        PlatformChip("Pinterest",  PinterestRed,    "https://pinterest.com"),
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Supported Platforms",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier              = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            platforms.forEach { p ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(p.color.copy(alpha = if (isDark) 0.15f else 0.08f))
                        .border(0.5.dp, p.color.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .clickable { onSelect(p.url) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(p.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = p.color)
                }
            }
        }
    }
}

private data class PlatformChip(val name: String, val color: Color, val url: String)

@Composable
private fun AuraLoadingCard(isDark: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val shimmer by infiniteTransition.animateFloat(
        initialValue  = 0.3f,
        targetValue   = 0.9f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label         = "shimmer"
    )
    LiquidGlassCard(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(22.dp),
        tintColor = NovaPrimary
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier  = Modifier.size(24.dp),
                strokeWidth = 2.5.dp,
                color       = NovaPrimary.copy(alpha = shimmer)
            )
            Spacer(Modifier.width(14.dp))
            Text(
                "Analyzing stream...",
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AuraErrorCard(
    message: String,
    isDark: Boolean,
    onRetry: () -> Unit,
    onCopy: () -> Unit
) {
    val hint = when {
        message.contains("login", ignoreCase = true) ||
        message.contains("cookies", ignoreCase = true) ->
            "This link requires login. Import cookies.txt in Settings, then retry."
        message.contains("rate", ignoreCase = true) ->
            "Platform rate-limited. Wait 1–2 min then retry."
        message.contains("unavailable", ignoreCase = true) ->
            "Media unavailable. It may be private or region-blocked."
        else ->
            "Link must be public. Private/deleted content cannot be downloaded."
    }

    LiquidGlassCard(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(22.dp),
        tintColor = AccentRed
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Error, "Error", tint = AccentRed, modifier = Modifier.size(18.dp))
                Text(
                    "Download Failed",
                    fontWeight = FontWeight.Bold,
                    color = AccentRed,
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = NovaPrimary, contentColor = Color(0xFF001824))
                ) {
                    Text("Retry", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onCopy,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(12.dp),
                    border   = ButtonDefaults.outlinedButtonBorder.copy(width = 0.5.dp)
                ) {
                    Text("Copy Link", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
