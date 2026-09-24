package com.zyvro.app.ui.screens

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import com.zyvro.app.ui.theme.LocalAppDark
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
import com.zyvro.app.ui.components.VideoPreviewCard
import com.zyvro.app.ui.components.batch.BatchDownloadModal
import com.zyvro.app.ui.components.equalizer.EqualizerDialog
import com.zyvro.app.ui.components.ambientLiquidBackground
import com.zyvro.app.ui.components.liquidGlass
import com.zyvro.app.ui.theme.AccentOrange
import com.zyvro.app.ui.theme.FacebookBlue
import com.zyvro.app.ui.theme.InstagramPink
import com.zyvro.app.ui.theme.NovaAqua
import com.zyvro.app.ui.theme.NovaAquaDeep
import com.zyvro.app.ui.theme.NovaAquaSoft
import com.zyvro.app.ui.theme.NovaInk
import com.zyvro.app.ui.theme.PinterestRed
import com.zyvro.app.ui.theme.RedditOrange
import com.zyvro.app.ui.theme.SoundCloudOrange
import com.zyvro.app.ui.theme.ThreadsDark
import com.zyvro.app.ui.theme.TikTokCyan
import com.zyvro.app.ui.theme.TwitchPurple
import com.zyvro.app.ui.theme.TwitterBlue
import com.zyvro.app.ui.theme.YouTubeRed
import com.zyvro.app.viewmodel.HomeUiState
import com.zyvro.app.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onNavigateToQueue: () -> Unit = {},
    onNavigateToBrowser: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val isDark = LocalAppDark.current
    val playerManager = remember { MediaPlayerManager.getInstance(context) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val uiState by viewModel.uiState.collectAsState()
    val urlInput by viewModel.urlInput.collectAsState()
    val recentDownloads by viewModel.recentDownloads.collectAsState(initial = emptyList())

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }
    var showBatchModal by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    var clipboardDetectedUrl by remember { mutableStateOf<String?>(null) }

    // Safe Clipboard Auto-Detect (protected from SecurityException on Android 10-15)
    LaunchedEffect(Unit) {
        runCatching {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true) {
                val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString()?.trim() ?: ""
                val isMediaLink = text.startsWith("http://") || text.startsWith("https://") ||
                        text.contains("youtu") || text.contains("instagram") || text.contains("facebook") ||
                        text.contains("fb.watch") || text.contains("threads") || text.contains("pinterest") ||
                        text.contains("pin.it") || text.contains("tiktok") || text.contains("twitter") || text.contains("x.com") || text.contains("reddit")
                if (isMediaLink && text != urlInput) {
                    clipboardDetectedUrl = text
                }
            }
        }
    }

    val platforms = listOf(
        Triple("YouTube", YouTubeRed, "https://m.youtube.com"),
        Triple("Instagram", InstagramPink, "https://www.instagram.com"),
        Triple("Facebook", FacebookBlue, "https://m.facebook.com"),
        Triple("Threads", if (isDark) Color.White else ThreadsDark, "https://www.threads.net"),
        Triple("Pinterest", PinterestRed, "https://www.pinterest.com"),
        Triple("TikTok", TikTokCyan, "https://www.tiktok.com"),
        Triple("X / Twitter", TwitterBlue, "https://x.com"),
        Triple("SoundCloud", SoundCloudOrange, "https://m.soundcloud.com"),
        Triple("Twitch", TwitchPurple, "https://m.twitch.tv"),
        Triple("Reddit", RedditOrange, "https://www.reddit.com")
    )
    // Minimal hub: 5 curated first, rest behind "More".
    var showAllPlatforms by remember { mutableStateOf(false) }
    val visiblePlatforms = if (showAllPlatforms) platforms else platforms.take(5)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .ambientLiquidBackground()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 110.dp)
    ) {
        // Zyvro header card with app icon
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .shadow(10.dp, RoundedCornerShape(15.dp), spotColor = NovaAqua)
                                    .clip(RoundedCornerShape(15.dp))
                                    .border(
                                        1.dp,
                                        Brush.linearGradient(listOf(NovaAqua.copy(alpha = 0.6f), Color.Transparent)),
                                        RoundedCornerShape(15.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.app_logo),
                                    contentDescription = "Zyvro Logo",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Zyvro",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(NovaAqua, NovaAquaDeep)
                                                )
                                            )
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "PRO",
                                            color = NovaInk,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                                Text(
                                    text = "Fast · Private · Modern",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = { showEqualizer = true },
                                modifier = Modifier
                                    .size(38.dp)
                                    .liquidGlass(shape = CircleShape, elevation = 4.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Tune,
                                    contentDescription = "Audio Studio FX",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (uiState is HomeUiState.Success) {
                                        showBatchModal = true
                                    } else if (urlInput.isNotBlank()) {
                                        viewModel.parseUrl(urlInput)
                                    }
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .liquidGlass(shape = CircleShape, elevation = 4.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.PlaylistPlay,
                                    contentDescription = "Batch Downloader",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Feature highlight chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val chipBg = if (isDark) Color(0xFF142C3F) else Color(0xFFE0F5F6)
                        val chipTextColor = if (isDark) NovaAqua else NovaAquaDeep

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(chipBg)
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Turbo Engine", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = chipTextColor)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(chipBg)
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("HD Quality", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = chipTextColor)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(chipBg)
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("100% Private", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = chipTextColor)
                        }
                    }
                }
            }
        }

        // URL input card
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        "Download Any Video or Audio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { viewModel.onUrlChanged(it) },
                        placeholder = { Text("Paste YouTube, Instagram, TikTok link...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        trailingIcon = {
                            if (urlInput.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        keyboardController?.hide()
                                        viewModel.parseUrl(urlInput)
                                    }
                                ) {
                                    Icon(
                                        Icons.Rounded.Download,
                                        contentDescription = "Extract & Download",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isDark) Color(0xFF1B364D) else Color(0xFFE0F5F6))
                                        .clickable {
                                            runCatching {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()?.trim() ?: ""
                                                if (clip.isNotBlank()) {
                                                    viewModel.onUrlChanged(clip)
                                                }
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        "PASTE",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDark) NovaAqua else NovaAquaDeep
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                keyboardController?.hide()
                                if (urlInput.isNotBlank()) {
                                    viewModel.parseUrl(urlInput)
                                }
                            }
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Fetch media primary CTA (iOS blue action button)
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            if (urlInput.isNotBlank()) {
                                viewModel.parseUrl(urlInput)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NovaAquaDeep,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Fetch Media Analysis",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Clipboard Detect Floating Glass Chip
                    if (!clipboardDetectedUrl.isNullOrBlank() && urlInput.isBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LiquidGlassPill(
                            onClick = {
                                clipboardDetectedUrl?.let {
                                    viewModel.onUrlChanged(it)
                                    viewModel.parseUrl(it)
                                    clipboardDetectedUrl = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Rounded.ContentPaste,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Auto-Paste: ${clipboardDetectedUrl?.take(36)}...",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Supported Platforms — minimal hub (5 curated + More)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                visiblePlatforms.forEach { (name, color, siteUrl) ->
                    LiquidGlassCard(
                        shape = RoundedCornerShape(18.dp),
                        elevation = 2.dp,
                        onClick = { onNavigateToBrowser(siteUrl) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .shadow(4.dp, CircleShape, ambientColor = color)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
                // More / Less toggle keeps Home clean on small screens.
                LiquidGlassPill(onClick = { showAllPlatforms = !showAllPlatforms }) {
                    Text(
                        if (showAllPlatforms) "Less" else "+${platforms.size - visiblePlatforms.size} More",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Extraction Status / Result
        item {
            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    LiquidGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Analyzing stream with yt-dlp...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                is HomeUiState.Success -> {
                    VideoPreviewCard(
                        videoInfo = state.videoInfo,
                        onConfigureDownload = { showBottomSheet = true }
                    )
                }
                is HomeUiState.Error -> {
                    LiquidGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        tintColor = MaterialTheme.colorScheme.error
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            // IG/FB hint: login-wall vs bad link me farq dikhao.
                            val hint = when {
                                state.message.contains("login", ignoreCase = true) ||
                                        state.message.contains("cookies", ignoreCase = true) ->
                                    "Ye link login maang raha hai. Settings me cookies.txt import karo, phir retry dabao."
                                state.message.contains("rate", ignoreCase = true) ->
                                    "Platform ne request limit lagayi hai. 1-2 min ruk kar retry karo."
                                else ->
                                    "Link public hona chahiye. Private/deleted post download nahi hoga."
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = hint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        if (urlInput.isNotBlank()) viewModel.parseUrl(urlInput)
                                    },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Retry", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = {
                                        runCatching {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            clipboard?.setPrimaryClip(
                                                android.content.ClipData.newPlainText("zyvro-url", urlInput)
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Copy link", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
                HomeUiState.Idle -> { }
            }
        }

        // Recent Downloads Section Header
        if (recentDownloads.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onNavigateToQueue) {
                        Text("View Queue", fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(recentDownloads.take(5), key = { it.id }) { item ->
                DownloadItemCard(
                    download = item,
                    onCancel = { },
                    onDelete = { },
                    onPlay = { playerManager.playMedia(item) }
                )
            }
        }
    }

    // Format Selection Modal Bottom Sheet
    if (showBottomSheet && uiState is HomeUiState.Success) {
        val videoInfo = (uiState as HomeUiState.Success).videoInfo
        FormatSelectionSheet(
            sheetState = sheetState,
            videoInfo = videoInfo,
            onDismiss = { showBottomSheet = false },
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

    // Batch Download Modal
    if (showBatchModal && uiState is HomeUiState.Success) {
        val info = (uiState as HomeUiState.Success).videoInfo
        BatchDownloadModal(
            playlistTitle = info.title,
            itemsList = listOf(info),
            onDismiss = { showBatchModal = false },
            onBatchDownload = { selectedItems, formatId, mediaType, audioExt ->
                selectedItems.forEach { item ->
                    viewModel.startDownload(
                        videoInfo = item,
                        formatId = formatId,
                        mediaType = mediaType,
                        audioExt = audioExt
                    )
                }
                showBatchModal = false
                onNavigateToQueue()
            }
        )
    }

    // Audio Studio Equalizer Dialog
    if (showEqualizer) {
        EqualizerDialog(onDismiss = { showEqualizer = false })
    }
}
