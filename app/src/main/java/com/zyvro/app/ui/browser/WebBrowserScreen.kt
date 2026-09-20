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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.zyvro.app.ui.components.LiquidGlassCard
import com.zyvro.app.ui.components.liquidGlass

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebBrowserScreen(
    onDownloadUrl: (String) -> Unit
) {
    var webView: WebView? by remember { mutableStateOf(null) }
    var currentUrl by remember { mutableStateOf("https://www.youtube.com") }
    var urlInput by remember { mutableStateOf("https://www.youtube.com") }
    var pageTitle by remember { mutableStateOf("yt-dlp Browser") }
    var progress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    val detectedMediaUrls = remember { mutableStateListOf<String>() }

    val quickBookmarks = listOf(
        "YouTube" to "https://m.youtube.com",
        "Instagram" to "https://www.instagram.com",
        "Facebook" to "https://m.facebook.com",
        "Threads" to "https://www.threads.net",
        "Pinterest" to "https://www.pinterest.com",
        "TikTok" to "https://www.tiktok.com",
        "X / Twitter" to "https://x.com",
        "Reddit" to "https://www.reddit.com",
        "SoundCloud" to "https://m.soundcloud.com",
        "Twitch" to "https://m.twitch.tv",
        "Bilibili" to "https://m.bilibili.com"
    )

    BackHandler(enabled = canGoBack) {
        webView?.goBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Browser Address & Navigation Bar
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
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }

                IconButton(
                    onClick = { webView?.goForward() },
                    enabled = canGoForward,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Forward")
                }

                IconButton(
                    onClick = { webView?.reload() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }

                Spacer(modifier = Modifier.width(4.dp))

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    placeholder = { Text("Search or type URL...") },
                    leadingIcon = {
                        Icon(Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bookmarks Horizontal Scroll
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickBookmarks.forEach { (name, url) ->
                    FilterChip(
                        selected = currentUrl.contains(url.replace("https://", "").replace("m.", "").replace("www.", "").split("/").first()),
                        onClick = {
                            urlInput = url
                            currentUrl = url
                            webView?.loadUrl(url)
                        },
                        label = { Text(name, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // Web Loading Progress
        if (isLoading && progress < 1f) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(2.5.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
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
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            mediaPlaybackRequiresUserGesture = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                                if (url != null) {
                                    currentUrl = url
                                    urlInput = url
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                canGoBack = view?.canGoBack() ?: false
                                canGoForward = view?.canGoForward() ?: false
                                if (url != null) {
                                    currentUrl = url
                                    urlInput = url
                                }
                            }

                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): android.webkit.WebResourceResponse? {
                                val reqUrl = request?.url?.toString() ?: ""
                                if (isMediaUrl(reqUrl)) {
                                    if (!detectedMediaUrls.contains(reqUrl)) {
                                        detectedMediaUrls.add(reqUrl)
                                    }
                                }
                                return super.shouldInterceptRequest(view, request)
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                progress = newProgress / 100f
                            }

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                if (title != null) {
                                    pageTitle = title
                                }
                            }
                        }

                        loadUrl(currentUrl)
                        webView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Floating 1-Tap Download Button
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(start = 16.dp, end = 16.dp, bottom = 104.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Main Page Downloader FAB
                Button(
                    onClick = {
                        onDownloadUrl(currentUrl)
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download Page Media", fontWeight = FontWeight.Bold)
                }

                // If direct media stream detected
                if (detectedMediaUrls.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "🎯 ${detectedMediaUrls.size} Streams Sniffed",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Button(
                                onClick = {
                                    detectedMediaUrls.lastOrNull()?.let { onDownloadUrl(it) }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Capture", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun isMediaUrl(url: String): Boolean {
    val lower = url.lowercase()
    return lower.contains(".mp4") || lower.contains(".m3u8") || lower.contains(".mpd") ||
           lower.contains(".webm") || lower.contains(".mp3") || lower.contains(".m4a") ||
           lower.contains("googlevideo.com/videoplayback") || lower.contains("tiktokcdn.com") ||
           lower.contains("cdninstagram.com") || lower.contains("twimg.com") ||
           lower.contains("fbcdn.net") || lower.contains("pinimg.com")
}
