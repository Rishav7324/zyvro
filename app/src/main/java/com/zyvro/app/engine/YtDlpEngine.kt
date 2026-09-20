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

        // Clean tracking & analytics parameters from Instagram/Facebook/Threads/TikTok/Pinterest
        if (url.contains("instagram.com") || url.contains("threads.net")) {
            val queryIdx = url.indexOf('?')
            if (queryIdx != -1) {
                url = url.substring(0, queryIdx)
            }
            if (!url.endsWith("/")) {
                url = "$url/"
            }
        } else if (url.contains("facebook.com") || url.contains("fb.watch")) {
            // Keep ?v= on facebook watch URLs, otherwise strip tracking
            if (!url.contains("/watch") && !url.contains("?v=")) {
                val queryIdx = url.indexOf('?')
                if (queryIdx != -1) url = url.substring(0, queryIdx)
            } else {
                url = url.replace(Regex("""[&?]fbclid=[^&]+"""), "")
                    .replace(Regex("""[&?]mibextid=[^&]+"""), "")
                    .replace(Regex("""[&?]sfnsn=[^&]+"""), "")
            }
        } else if (url.contains("pinterest.com") || url.contains("pin.it")) {
            val queryIdx = url.indexOf('?')
            if (queryIdx != -1) url = url.substring(0, queryIdx)
        }
        return url
    }

    suspend fun fetchVideoInfo(context: Context, url: String, cookiesFile: File? = null): Result<VideoInfo> = withContext(Dispatchers.IO) {
        runCatching {
            ensureInitialized(context).getOrThrow()
            val normalized = normalizeUrl(url)
            val isInstagram = normalized.contains("instagram.com")
            val isMeta = isInstagram ||
                    normalized.contains("facebook.com") ||
                    normalized.contains("fb.watch") ||
                    normalized.contains("threads.net")

            val request = YoutubeDLRequest(normalized).apply {
                addOption("--no-playlist")
                addOption("--user-agent", DEFAULT_USER_AGENT)
                if (cookiesFile?.exists() == true) {
                    addOption("--cookies", cookiesFile.absolutePath)
                }
            }

            val info = YoutubeDL.getInstance().getInfo(request)
            val videoId = info.id.orEmpty().ifBlank { System.currentTimeMillis().toString() }
            VideoInfo(
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
        }.fold({ Result.success(it) }) { error ->
            Log.e(TAG, "Metadata extraction failed for $url", error)
            Result.failure(error)
        }
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
            val normalized = normalizeUrl(url)
            val isInstagram = normalized.contains("instagram.com")
            val isMeta = isInstagram ||
                    normalized.contains("facebook.com") ||
                    normalized.contains("fb.watch") ||
                    normalized.contains("threads.net") ||
                    normalized.contains("pinterest.com") ||
                    normalized.contains("pin.it")

            val request = YoutubeDLRequest(normalized).apply {
                addOption("-o", "${validDir.absolutePath}/%(title).180B [%(id)s].%(ext)s")
                addOption("--no-mtime")
                addOption("--restrict-filenames")
                addOption("--newline")
                addOption("--no-playlist")
                addOption("--user-agent", DEFAULT_USER_AGENT)
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
                request.addOption("-f", "ba/b")
                request.addOption("-x")
                request.addOption("--audio-format", audioExtension)
                request.addOption("--audio-quality", "0")
                request.addOption("--add-metadata")
                if (embedThumbnail) request.addOption("--embed-thumbnail")
            } else {
                // Smart merged selector with resilience for Instagram, FB, Threads reels/photos and YouTube
                val selected = when {
                    formatId.isBlank() || formatId == "best" ->
                        if (isMeta) "b/best" else "bv*[height<=1080]+ba/b[height<=1080]/best"
                    formatId == "bestvideo+bestaudio/best" ->
                        if (isMeta) "b/best" else "bv*[height<=1080][ext=mp4]+ba[ext=m4a]/bv*[height<=1080]+ba/b[height<=1080]/best"
                    formatId.contains("+") || formatId.contains("/") || formatId.contains("[") -> formatId
                    isMeta -> "$formatId/best"
                    else -> "$formatId+ba/b/$formatId/best"
                }
                request.addOption("-f", selected)

                val isImage = formatId.contains("photo", ignoreCase = true) || formatId.contains("image", ignoreCase = true)
                if (!isImage) {
                    request.addOption("--merge-output-format", "mp4")
                }

                if (ytAndroidClient) {
                    request.addOption("--extractor-args", "youtube:player_client=android")
                }
                if (embedThumbnail && !isInstagram) request.addOption("--embed-thumbnail")
                if (embedSubtitles) request.addOption("--embed-subs")
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
