package com.zyvro.app.engine

import android.content.Context
import android.os.Environment
import android.util.Log
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoInfo as YtdlVideoInfo
import com.zyvro.app.data.local.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

data class EngineFormat(
    val formatId: String,
    val extension: String,
    val resolution: String,
    val note: String,
    val isAudioOnly: Boolean,
    val fileSizeApprox: Long = 0L,
    val fps: Int? = null,
    val vcodec: String? = null,
    val acodec: String? = null
)

object YtDlpEngine {
    private const val TAG = "YtDlpEngine"
    private const val DEFAULT_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
    /**
     * Anonymous-first Meta fix: Instagram/Facebook serve public Reels far more
     * reliably to a mobile Safari UA + proper referer than to a desktop Chrome
     * UA (which often 302-redirects to /login). YouTube keeps the desktop UA.
     */
    private const val META_MOBILE_USER_AGENT =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1"
    private const val INSTAGRAM_REFERER = "https://www.instagram.com/"
    private const val FACEBOOK_REFERER = "https://m.facebook.com/"

    private val initMutex = Mutex()
    @Volatile var isInitialized = false
        private set
    @Volatile var isAria2Initialized = false
        private set
    @Volatile var lastInitError: String? = null
        private set

    suspend fun ensureInitialized(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext Result.success(Unit)
        initMutex.withLock {
            if (isInitialized) return@withContext Result.success(Unit)
            runCatching {
                val appContext = context.applicationContext
                YoutubeDL.getInstance().init(appContext)
                FFmpeg.getInstance().init(appContext)
                runCatching {
                    Aria2c.getInstance().init(appContext)
                    isAria2Initialized = true
                }.onFailure {
                    isAria2Initialized = false
                    Log.w(TAG, "Aria2 unavailable; using yt-dlp downloader", it)
                }
                isInitialized = true
                lastInitError = null
            }.fold(
                onSuccess = { Result.success(Unit) },
                onFailure = { error ->
                    lastInitError = error.message ?: "Engine initialization failed"
                    Log.e(TAG, "Engine initialization failed", error)
                    Result.failure(error)
                }
            )
        }
    }

    suspend fun updateEngine(context: Context): Result<String> =
        YtDlpUpdater.updateEngine(context)

    fun normalizeUrl(rawUrl: String): String {
        var url = rawUrl.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }

        // Expand bare short-domains users often paste without scheme/path noise.
        // instagr.am/XXXX and fb.watch/XXXX are resolved by yt-dlp itself once
        // tracking params are stripped, so keep the short host intact.
        if (url.contains("instagr.am") || url.contains("instagram.com") || url.contains("threads.net")) {
            // Strip ALL tracking queries (igsh, igshid, img_index, utm_*) but keep
            // the path (/reel/ID/, /p/ID/, /share/reel/ID) untouched.
            val queryIdx = url.indexOf('?')
            if (queryIdx != -1) {
                url = url.substring(0, queryIdx)
            }
            if (!url.endsWith("/")) {
                url = "$url/"
            }
        } else if (url.contains("facebook.com") || url.contains("fb.watch")) {
            // fb.watch/ID shortlinks must keep their full path; only strip
            // known tracking params. /watch?v= links keep ?v=.
            if (url.contains("fb.watch/")) {
                val queryIdx = url.indexOf('?')
                if (queryIdx != -1) url = url.substring(0, queryIdx)
            } else if (!url.contains("/watch") && !url.contains("?v=")) {
                val queryIdx = url.indexOf('?')
                if (queryIdx != -1) url = url.substring(0, queryIdx)
            } else {
                url = stripTrackingParams(url)
            }
            // Normalize mobile/desktop hosts: m.facebook.com is the most
            // permissive for anonymous extraction.
            url = url.replace("://www.facebook.com", "://m.facebook.com")
                .replace("://web.facebook.com", "://m.facebook.com")
        } else if (url.contains("pinterest.com") || url.contains("pin.it")) {
            val queryIdx = url.indexOf('?')
            if (queryIdx != -1) url = url.substring(0, queryIdx)
        }
        return url
    }

    private fun stripTrackingParams(url: String): String {
        var out = url
        listOf("fbclid", "mibextid", "sfnsn", "igsh", "igshid", "utm_source", "utm_medium", "utm_campaign", "xmt", "s", "a").forEach { key ->
            out = out.replace(Regex("""[&?]$key=[^&]*"""), "")
        }
        // Clean up leftover "?&", trailing "?" or "&".
        out = out.replace("?&", "?").trimEnd('?', '&')
        return out
    }

    private fun refererFor(url: String): String? = when {
        url.contains("instagram.com") || url.contains("instagr.am") || url.contains("threads.net") -> INSTAGRAM_REFERER
        url.contains("facebook.com") || url.contains("fb.watch") -> FACEBOOK_REFERER
        else -> null
    }

    suspend fun fetchVideoInfo(context: Context, url: String, cookiesFile: File? = null): Result<VideoInfo> = withContext(Dispatchers.IO) {
        runCatching {
            ensureInitialized(context).getOrThrow()
            val normalized = normalizeUrl(url)
            val isInstagram = normalized.contains("instagram.com") || normalized.contains("instagr.am")
            val isMeta = isInstagram ||
                    normalized.contains("facebook.com") ||
                    normalized.contains("fb.watch") ||
                    normalized.contains("threads.net")
            val hasCookies = cookiesFile?.exists() == true

            // Anonymous-first: (1) standard request, (2) mobile-UA + referer +
            // resilient retries for Meta, (3) cookies only as last resort so
            // public Reels work without forcing a login.
            val attempts = buildList {
                add(false to DEFAULT_USER_AGENT)
                if (isMeta) add(false to META_MOBILE_USER_AGENT)
                if (hasCookies) add(true to META_MOBILE_USER_AGENT)
            }

            var lastError: Throwable? = null
            for ((index, attempt) in attempts.withIndex()) {
                val (useCookies, userAgent) = attempt
                try {
                    return@runCatching fetchVideoInfoOnce(normalized, isInstagram, userAgent, if (useCookies) cookiesFile else null, isMeta)
                } catch (error: Throwable) {
                    lastError = error
                    val msg = error.message.orEmpty()
                    Log.w(TAG, "Metadata attempt ${index + 1}/${attempts.size} failed for $normalized (cookies=$useCookies): $msg")
                    // Login-wall without cookies available: no point retrying further.
                    if (!hasCookies && isLoginWall(msg)) break
                    // Non-Meta sites: single attempt is enough.
                    if (!isMeta) break
                }
            }
            throw mapToFriendlyError(lastError, hasCookies)
        }.fold({ Result.success(it) }) { error ->
            Log.e(TAG, "Metadata extraction failed for $url", error)
            Result.failure(error)
        }
    }

    private fun fetchVideoInfoOnce(
        normalized: String,
        isInstagram: Boolean,
        userAgent: String,
        cookiesFile: File?,
        isMeta: Boolean
    ): VideoInfo {
        val request = YoutubeDLRequest(normalized).apply {
            addOption("--no-playlist")
            addOption("--no-check-certificate")
            addOption("--socket-timeout", "30")
            addOption("--retries", "3")
            addOption("--extractor-retries", "3")
            addOption("--user-agent", userAgent)
            refererFor(normalized)?.let { addOption("--referer", it) }
            if (isMeta) {
                // Bypass geo/age variations that break anonymous Meta extraction.
                addOption("--geo-bypass")
            }
            if (cookiesFile?.exists() == true) {
                addOption("--cookies", cookiesFile.absolutePath)
            }
        }

        val info = YoutubeDL.getInstance().getInfo(request)
        val videoId = info.id.orEmpty().ifBlank { System.currentTimeMillis().toString() }
        return VideoInfo(
            url = normalized,
            id = videoId,
            title = info.title.orEmpty().ifBlank { if (isInstagram) "Instagram Media" else "Untitled media" },
            uploader = info.uploader.orEmpty().ifBlank { info.extractor.orEmpty().ifBlank { if (isInstagram) "Instagram" else "Unknown creator" } },
            channelUrl = "",
            thumbnailUrl = info.thumbnail.orEmpty(),
            durationSeconds = (info.duration as? Number)?.toLong() ?: 0L,
            viewCount = (info.viewCount as? Number)?.toLong() ?: 0L,
            description = info.description.orEmpty(),
            extractor = info.extractor.orEmpty().ifBlank { if (isInstagram) "Instagram" else "" },
            formats = mapFormats(info)
        )
    }

    private fun isLoginWall(msg: String): Boolean = DownloadErrors.isLoginWall(msg)

    private fun mapToFriendlyError(cause: Throwable?, hasCookies: Boolean): Throwable {
        val raw = cause?.message.orEmpty()
        return Exception(DownloadErrors.friendlyMessage(raw, hasCookies), cause)
    }

    private fun mapFormats(info: YtdlVideoInfo): List<DownloadFormat> {
        val mapped = info.formats.orEmpty().mapNotNull { format ->
            val id = format.formatId.orEmpty()
            if (id.isBlank()) return@mapNotNull null
            val height = (format.height as? Number)?.toInt() ?: 0
            val audioOnly = height <= 0
            DownloadFormat(
                formatId = id,
                extension = format.ext.orEmpty().ifBlank { "mp4" },
                resolution = if (audioOnly) "Audio" else "${height}p",
                note = listOfNotNull(format.vcodec, format.acodec).filter { it.isNotBlank() }.joinToString(" • ").ifBlank { "Available stream" },
                isAudioOnly = audioOnly,
                fileSizeApprox = 0L,
                fps = (format.fps as? Number)?.toInt(),
                vcodec = format.vcodec,
                acodec = format.acodec
            )
        }.distinctBy { it.formatId }.sortedWith(compareByDescending<DownloadFormat> { !it.isAudioOnly }.thenByDescending { it.resolution.removeSuffix("p").toIntOrNull() ?: 0 })

        if (mapped.isNotEmpty()) return mapped

        // Fallback for single format media, Instagram photo posts, or carousels
        val ext = info.ext.orEmpty().ifBlank { "mp4" }
        val isImage = ext.equals("jpg", true) || ext.equals("jpeg", true) || ext.equals("png", true) || ext.equals("webp", true)
        return listOf(
            DownloadFormat(
                formatId = "best",
                extension = ext,
                resolution = if (isImage) "Full HD Photo" else "Best Quality",
                note = if (isImage) "Original High-Res Photo" else "Default Stream",
                isAudioOnly = false
            )
        )
    }

    suspend fun executeDownload(
        context: Context,
        taskId: String,
        url: String,
        outputDir: File,
        mediaType: MediaType,
        formatId: String,
        audioExtension: String = "mp3",
        embedThumbnail: Boolean = true,
        embedSubtitles: Boolean = false,
        useAria2: Boolean = false,
        customArgs: String = "",
        cookiesFile: File? = null,
        ytAndroidClient: Boolean = false,
        speedLimit: String = "",
        onProgress: (Float, String, String, String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            ensureInitialized(context).getOrThrow()
            val validDir = outputDir.takeIf { it.exists() || it.mkdirs() } ?: context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            val normalized    = normalizeUrl(url)
            val isInstagram   = normalized.contains("instagram.com") || normalized.contains("instagr.am")
            val isMeta        = isInstagram ||
                    normalized.contains("facebook.com") ||
                    normalized.contains("fb.watch") ||
                    normalized.contains("threads.net") ||
                    normalized.contains("pinterest.com") ||
                    normalized.contains("pin.it")
            // Platforms with combined-only streams (no DASH merge) — add fallback selectors
            val isCombinedOnly = isMeta ||
                    normalized.contains("tiktok.com")       ||
                    normalized.contains("twitter.com")      || normalized.contains("x.com")  ||
                    normalized.contains("reddit.com")       || normalized.contains("redd.it") ||
                    normalized.contains("soundcloud.com")   ||
                    normalized.contains("bilibili.com")     ||
                    normalized.contains("vimeo.com")        ||
                    normalized.contains("dailymotion.com")
            val isYouTube     = normalized.contains("youtube.com") || normalized.contains("youtu.be")
            val isTikTok      = normalized.contains("tiktok.com")
            val isTwitch      = normalized.contains("twitch.tv")

            val request = YoutubeDLRequest(normalized).apply {
                addOption("-o", "${validDir.absolutePath}/%(title).180B [%(id)s].%(ext)s")
                addOption("--no-mtime")
                addOption("--restrict-filenames")
                addOption("--newline")
                addOption("--no-playlist")
                addOption("--no-check-certificate")
                addOption("--socket-timeout", "30")
                addOption("--retries", "5")
                addOption("--fragment-retries", "8")
                // Platform-aware User-Agent routing:
                // Meta needs mobile Safari; TikTok needs mobile; YouTube gets desktop Chrome
                val ua = when {
                    isMeta   -> META_MOBILE_USER_AGENT
                    isTikTok -> "com.zhiliaoapp.musically/2022600030 (Linux; U; Android 12; en_US; Pixel 5; Build/SP2A.220405.004; Cronet/58.0.2991.0)"
                    else     -> DEFAULT_USER_AGENT
                }
                addOption("--user-agent", ua)
                refererFor(normalized)?.let { addOption("--referer", it) }
                // Geo-bypass for restricted regions
                if (isMeta || isTikTok || isCombinedOnly) addOption("--geo-bypass")
            }


            if (speedLimit.isNotBlank() && speedLimit != "0" && !speedLimit.equals("Unlimited", ignoreCase = true)) {
                if (useAria2 && isAria2Initialized && !isMeta) {
                    request.addOption("--downloader-args", "aria2c:--max-download-limit=$speedLimit")
                } else {
                    request.addOption("--limit-rate", speedLimit)
                }
            }

            if (useAria2 && isAria2Initialized && !isMeta) {
                request.addOption("--downloader", "libaria2c.so")
            } else {
                request.addOption("--concurrent-fragments", "4")
            }

            if (mediaType == MediaType.AUDIO) {
                // Audio-only download: prefer best audio, fallback to combined, then extract
                request.addOption("-f", "bestaudio/ba/b/best")
                request.addOption("-x")
                request.addOption("--audio-format", audioExtension)
                request.addOption("--audio-quality", "0")
                request.addOption("--add-metadata")
                if (embedThumbnail && !isInstagram) request.addOption("--embed-thumbnail")
            } else {
                // ── Format selector: platform-specific ─────────────────────────
                val selected = when {
                    // Already a complex format string — pass through
                    formatId.contains("+") || formatId.contains("/") || formatId.contains("[") -> formatId

                    // Meta platforms: single-stream only, no DASH mux needed
                    isMeta ->
                        if (formatId.isBlank() || formatId == "best") "b/best"
                        else "$formatId/b/best"

                    // TikTok: combined mp4 streams, no DASH
                    isTikTok ->
                        if (formatId.isBlank() || formatId == "best") "b[ext=mp4]/b/best"
                        else "$formatId/b[ext=mp4]/b/best"

                    // Twitter/X: combined streams, avoid DASH
                    normalized.contains("twitter.com") || normalized.contains("x.com") ->
                        if (formatId.isBlank() || formatId == "best") "b/best"
                        else "$formatId/b/best"

                    // Reddit: combined mp4, DASH can fail
                    normalized.contains("reddit.com") || normalized.contains("redd.it") ->
                        if (formatId.isBlank() || formatId == "best")
                            "bv*+ba/bv*[height<=1080]+ba/b[height<=1080]/best"
                        else "$formatId+ba/b/best"

                    // YouTube: full DASH merge capability
                    isYouTube ->
                        if (formatId.isBlank() || formatId == "best")
                            "bv*[height<=2160][ext=mp4]+ba[ext=m4a]/bv*[height<=1080][ext=mp4]+ba[ext=m4a]/bv*[height<=1080]+ba/b[height<=1080]/best"
                        else if (formatId == "bestvideo+bestaudio/best")
                            "bv*[height<=2160][ext=mp4]+ba[ext=m4a]/bv*[height<=1080][ext=mp4]+ba[ext=m4a]/bv*+ba/b/best"
                        else "$formatId+ba[ext=m4a]/$formatId+ba/$formatId/best"

                    // Bilibili: needs different codec
                    normalized.contains("bilibili.com") ->
                        if (formatId.isBlank() || formatId == "best") "bv*+ba/b/best"
                        else "$formatId+ba/b/best"

                    // Twitch: uses chunked HLS — best combined
                    isTwitch ->
                        if (formatId.isBlank() || formatId == "best") "b/best/1080p60/720p60"
                        else "$formatId/b/best"

                    // Generic/other: combined with DASH fallback
                    else ->
                        if (formatId.isBlank() || formatId == "best")
                            "bv*+ba/b[height<=1080]/best"
                        else "$formatId+ba/$formatId/best"
                }
                request.addOption("-f", selected)

                val isImage = formatId.contains("photo", ignoreCase = true) || formatId.contains("image", ignoreCase = true)
                if (!isImage) {
                    request.addOption("--merge-output-format", "mp4")
                }

                // YouTube: enable Android client for age-gated or throttled content
                if (ytAndroidClient && isYouTube) {
                    request.addOption("--extractor-args", "youtube:player_client=android,web")
                }
                // TikTok: no watermark extractor
                if (isTikTok) {
                    request.addOption("--extractor-args", "tiktok:app_name=trill")
                }
                // Bilibili: Chinese CDN workaround
                if (normalized.contains("bilibili.com")) {
                    request.addOption("--extractor-args", "bilibili:prefer_multi_flv=false")
                }

                if (embedThumbnail && !isInstagram && !normalized.contains("instagr.am")) {
                    request.addOption("--embed-thumbnail")
                }
                if (embedSubtitles && !isCombinedOnly) request.addOption("--embed-subs")
            }


            if (cookiesFile?.exists() == true) {
                request.addOption("--cookies", cookiesFile.absolutePath)
            }
            if (customArgs.isNotBlank()) {
                customArgs.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.forEach(request::addOption)
            }

            var speed = ""
            var eta = ""
            var resolved: File? = null

            YoutubeDL.getInstance().execute(request, taskId) { progress, etaSeconds, line ->
                Regex("""(?:at|speed)\s+([0-9.]+(?:KiB|MiB|GiB|KB|MB|GB)/s)""").find(line)?.groupValues?.getOrNull(1)?.let { speed = it }
                if (etaSeconds > 0) eta = formatEta(etaSeconds)
                val candidate = File(line.trim())
                if (candidate.isAbsolute && candidate.parentFile?.absolutePath == validDir.absolutePath && candidate.isFile) {
                    resolved = candidate
                }
                onProgress(progress.coerceIn(0f, 100f), speed, eta, line)
            }

            resolved?.takeIf(File::exists)
                ?: validDir.listFiles()?.filter {
                    it.isFile && !it.name.endsWith(".part", true) && !it.name.endsWith(".ytdl", true) && !it.name.endsWith(".temp", true)
                }?.maxByOrNull(File::lastModified)
                ?: throw YoutubeDLException("Download completed but output file was not found")
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { error ->
                Log.e(TAG, "Download failed for $url", error)
                Result.failure(error)
            }
        )
    }

    private fun formatEta(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) String.format("%02d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
    }

    fun cancelDownload(taskId: String) = runCatching {
        YoutubeDL.getInstance().destroyProcessById(taskId)
    }.onFailure { Log.w(TAG, "Failed to cancel task $taskId", it) }
}
