package com.zyvro.app.ui.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.zyvro.app.ui.components.NovaButton
import com.zyvro.app.ui.theme.*

// ═══════════════════════════════════════════════════════════════════════════
// ZYVRO WEB BROWSER SCREEN v4.0 — Deep Space Aura · Stream Sniffer
// ═══════════════════════════════════════════════════════════════════════════

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebBrowserScreen(
    onDownloadUrl: (String) -> Unit
) {
    val isDark = LocalAppDark.current
    var webView: WebView? by remember { mutableStateOf(null) }
    var currentUrl by remember { mutableStateOf("https://www.youtube.com") }
    var urlInput by remember { mutableStateOf("https://www.youtube.com") }
    var pageTitle by remember { mutableStateOf("Zyvro Browser") }
    var progress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    val detectedMediaUrls = remember { mutableStateListOf<String>() }

    val quickBookmarks = listOf(
        Bookmark("YouTube",    "https://m.youtube.com",       YouTubeRed),
        Bookmark("Instagram",  "https://www.instagram.com",   InstagramPink),
        Bookmark("TikTok",     "https://www.tiktok.com",      TikTokCyan),
        Bookmark("X / Twitter","https://x.com",               TwitterBlue),
        Bookmark("Facebook",   "https://m.facebook.com",      FacebookBlue),
        Bookmark("Threads",    "https://www.threads.net",     ThreadsDark),
        Bookmark("SoundCloud", "https://m.soundcloud.com",    SoundCloudOrange),
        Bookmark("Twitch",     "https://m.twitch.tv",         TwitchPurple),
        Bookmark("Reddit",     "https://www.reddit.com",      RedditOrange),
        Bookmark("Pinterest",  "https://www.pinterest.com",   PinterestRed),
        Bookmark("Bilibili",   "https://m.bilibili.com",      NovaPrimary)
    )

    BackHandler(enabled = canGoBack) {
        webView?.goBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(if (isDark) SpaceBlack else LightBg)
    ) {
        // ── Browser Address & Navigation Bar ─────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { webView?.goBack() },
                    enabled = canGoBack,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = if (canGoBack) (if (isDark) Color.White else Color.Black) else Color.Gray.copy(alpha = 0.4f)
                    )
                }

                IconButton(
                    onClick = { webView?.goForward() },
                    enabled = canGoForward,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Rounded.ArrowForward,
                        contentDescription = "Forward",
                        tint = if (canGoForward) (if (isDark) Color.White else Color.Black) else Color.Gray.copy(alpha = 0.4f)
                    )
                }

                IconButton(
                    onClick = { webView?.reload() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = "Refresh",
                        tint = if (isDark) Color.White else Color.Black
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDark) SpaceCard else Color.White)
                        .border(
                            0.7.dp,
                            if (isDark) SpaceBorder else Color(0xFFD4E0F0),
                            RoundedCornerShape(14.dp)
                        )
                ) {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        modifier = Modifier.fillMaxSize(),
                        singleLine = true,
                        placeholder = {
                            Text(
                                "Search or URL…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Public,
                                contentDescription = null,
                                tint = NovaPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            val input = urlInput.trim()
                            val target = if (input.startsWith("http://") || input.startsWith("https://")) {
                                input
                            } else if (input.contains(".") && !input.contains(" ")) {
                                "https://$input"
                            } else {
                                "https://www.google.com/search?q=${input.replace(" ", "+")}"
                            }
                            urlInput = target
                            currentUrl = target
                            webView?.loadUrl(target)
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bookmarks Horizontal Scroll
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickBookmarks.forEach { item ->
                    val isSelected = currentUrl.contains(
                        item.url.replace("https://", "").replace("m.", "").replace("www.", "").split("/").first()
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) item.color.copy(alpha = 0.2f)
                                else if (isDark) SpaceCardHigh else Color(0xFFF0F4FF)
                            )
                            .border(
                                0.6.dp,
                                if (isSelected) item.color else (if (isDark) SpaceBorder else Color(0xFFD4E0F0)),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                urlInput = item.url
                                currentUrl = item.url
                                webView?.loadUrl(item.url)
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = item.name,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) item.color else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Web Loading Progress
        if (isLoading && progress < 1f) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(2.5.dp),
                color = NovaPrimary,
                trackColor = if (isDark) SpaceCard else Color(0xFFE2E9F8)
            )
        }

        // Web View Container
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            loadsImagesAutomatically = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                                url?.let {
                                    currentUrl = it
                                    urlInput = it
                                }
                                canGoBack = canGoBack()
                                canGoForward = canGoForward()
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                canGoBack = canGoBack()
                                canGoForward = canGoForward()
                            }

                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ) = run {
                                request?.url?.toString()?.let { reqUrl ->
                                    if (isMediaUrl(reqUrl) && !detectedMediaUrls.contains(reqUrl)) {
                                        post {
                                            if (detectedMediaUrls.size > 20) detectedMediaUrls.removeAt(0)
                                            detectedMediaUrls.add(reqUrl)
                                        }
                                    }
                                }
                                super.shouldInterceptRequest(view, request)
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                progress = newProgress / 100f
                            }

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                if (title != null) pageTitle = title
                            }
                        }

                        loadUrl(currentUrl)
                        webView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // ── Floating Sniffer & Download Controls ─────────────────────────
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(start = 16.dp, end = 16.dp, bottom = 104.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Main Page Downloader Button
                NovaButton(
                    onClick = { onDownloadUrl(currentUrl) },
                    modifier = Modifier.height(48.dp),
                    colors = listOf(NovaPrimary, NovaPrimaryDeep),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        Icons.Rounded.Download,
                        contentDescription = null,
                        tint = Color(0xFF001824),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Download Page Media",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF001824),
                        fontSize = 13.5.sp
                    )
                }

                // If direct media stream detected
                if (detectedMediaUrls.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .shadow(10.dp, RoundedCornerShape(14.dp), spotColor = NovaViolet.copy(alpha = 0.4f))
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) SpaceCardHigh else Color(0xFFEEF2FF))
                            .border(1.dp, NovaViolet.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.Stream,
                                    null,
                                    tint = NovaViolet,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "${detectedMediaUrls.size} Stream(s) Sniffed",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color.Black
                                )
                            }
                            Button(
                                onClick = {
                                    detectedMediaUrls.lastOrNull()?.let { onDownloadUrl(it) }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NovaViolet,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Capture", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class Bookmark(val name: String, val url: String, val color: Color)

private fun isMediaUrl(url: String): Boolean {
    val lower = url.lowercase()
    return lower.contains(".mp4") || lower.contains(".m3u8") || lower.contains(".mpd") ||
            lower.contains(".webm") || lower.contains(".mp3") || lower.contains(".m4a") ||
            lower.contains("googlevideo.com/videoplayback") || lower.contains("tiktokcdn.com") ||
            lower.contains("cdninstagram.com") || lower.contains("twimg.com") ||
            lower.contains("fbcdn.net") || lower.contains("pinimg.com")
}
