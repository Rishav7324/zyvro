package com.zyvro.app.engine

import android.content.Context
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDL.UpdateChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object YtDlpUpdater {
    private const val TAG = "YtDlpUpdater"
    private const val PREFS_NAME = "zyvro-ytdlp"
    private const val VERSION_KEY = "dlpVersion"
    private const val VERSION_NAME_KEY = "dlpVersionName"
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 60_000
    private const val USER_AGENT = "Zyvro/3.0 (Android; yt-dlp updater)"
    private const val LAST_CHECK_KEY = "dlpLastCheckMs"
    private const val AUTO_CHECK_INTERVAL_MS = 7L * 24L * 60L * 60L * 1000L // 7 days

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Version/update depends only on the yt-dlp core. FFmpeg and Aria2 must not
     * block the Update button.
     */
    private fun ensureYtDlpCore(context: Context) {
        YoutubeDL.getInstance().init(context.applicationContext)
    }

    suspend fun getVersion(context: Context): String = withContext(Dispatchers.IO) {        runCatching {
            ensureYtDlpCore(context)
            val storedName = prefs(context).getString(VERSION_NAME_KEY, null).orEmpty()
            val storedTag = prefs(context).getString(VERSION_KEY, null).orEmpty()
            storedName.ifBlank { storedTag }.ifBlank { "Bundled yt-dlp" }
        }.getOrElse { error ->
            Log.e(TAG, "Failed to initialize/read yt-dlp version", error)
            "Engine unavailable"
        }
    }

    /**
     * Best-effort background refresh: if the bundled extractor is older than
     * 7 days, try updating once. Never throws — callers ignore the result.
     * Stale extractors are the #1 cause of Instagram/Facebook failures.
     */
    suspend fun maybeAutoUpdate(context: Context) = withContext(Dispatchers.IO) {
        runCatching {
            val prefs = prefs(context)
            val last = prefs.getLong(LAST_CHECK_KEY, 0L)
            if (System.currentTimeMillis() - last < AUTO_CHECK_INTERVAL_MS) return@runCatching
            prefs.edit().putLong(LAST_CHECK_KEY, System.currentTimeMillis()).apply()
            updateEngine(context)
        }.onFailure { Log.w(TAG, "Background yt-dlp check skipped", it) }
    }

    suspend fun updateEngine(
        context: Context,
        channel: UpdateChannel = UpdateChannel.STABLE
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val appContext = context.applicationContext
            ensureYtDlpCore(appContext)

            val release = resolveReleaseWithoutApi() ?: fetchLatestRelease(channel.apiUrl)
            val oldTag = prefs(appContext).getString(VERSION_KEY, null)
            if (release.tag == oldTag) return@runCatching release.name

            val baseDir = File(appContext.noBackupFilesDir, YoutubeDL.baseName)
            val ytdlpDir = File(baseDir, YoutubeDL.ytdlpDirName)
            if (!ytdlpDir.exists() && !ytdlpDir.mkdirs()) {
                throw IOException("Cannot create yt-dlp directory")
            }

            val tempFile = File.createTempFile("yt-dlp-update-", ".tmp", ytdlpDir)
            val binary = File(ytdlpDir, YoutubeDL.ytdlpBin)
            try {
                downloadBinary(release.binaryUrl, tempFile)
                if (!tempFile.renameTo(binary)) {
                    tempFile.copyTo(binary, overwrite = true)
                    if (!tempFile.delete()) Log.w(TAG, "Could not delete updater temp file")
                }
                if (!binary.exists() || binary.length() < 1024) {
                    throw IOException("yt-dlp binary installation failed")
                }
                binary.setExecutable(true, false)
                prefs(appContext).edit()
                    .putString(VERSION_KEY, release.tag)
                    .putString(VERSION_NAME_KEY, release.name)
                    .apply()
                Log.d(TAG, "yt-dlp updated successfully: ${release.tag} (${release.name})")
                release.name
            } finally {
                if (tempFile.exists()) tempFile.delete()
            }
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { error ->
                val message = error.message?.trim().orEmpty()
                val detail = message.ifBlank { error.javaClass.simpleName }
                Log.e(TAG, "yt-dlp update failed: $detail", error)
                Result.failure(IOException("yt-dlp update failed: $detail", error))
            }
        )
    }

    private data class Release(
        val tag: String,
        val name: String,
        val binaryUrl: String
    )

    private fun resolveReleaseWithoutApi(): Release? = runCatching {
        val conn = (URL("https://github.com/yt-dlp/yt-dlp/releases/latest").openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false
            requestMethod = "HEAD"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("User-Agent", USER_AGENT)
        }
        try {
            val location = conn.getHeaderField("Location") ?: conn.getHeaderField("location")
            if (!location.isNullOrBlank()) {
                val tag = location.substringAfterLast("/")
                if (tag.isNotBlank()) {
                    Release(
                        tag = tag,
                        name = "yt-dlp $tag",
                        binaryUrl = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp"
                    )
                } else null
            } else null
        } finally {
            conn.disconnect()
        }
    }.getOrNull()

    private fun fetchLatestRelease(apiUrl: String): Release {
        val connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", USER_AGENT)
        }
        return try {
            val code = connection.responseCode
            if (code !in 200..299) {
                val body = runCatching {
                    connection.errorStream?.bufferedReader()?.use { it.readText() }
                }.getOrNull().orEmpty().replace(Regex("\\s+"), " ").take(240)
                throw IOException("GitHub API HTTP $code${if (body.isNotBlank()) ": $body" else ""}")
            }
            val json = connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
            val tag = json.optString("tag_name").trim()
            if (tag.isBlank()) throw IOException("GitHub release response has no tag_name")
            val name = json.optString("name").trim().ifBlank { tag }
            val assets = json.optJSONArray("assets") ?: throw IOException("GitHub release has no assets")
            var binaryUrl = ""
            for (index in 0 until assets.length()) {
                val asset = assets.optJSONObject(index) ?: continue
                if (asset.optString("name") == YoutubeDL.ytdlpBin) {
                    binaryUrl = asset.optString("browser_download_url")
                    break
                }
            }
            if (binaryUrl.isBlank()) throw IOException("GitHub release has no yt-dlp binary asset")
            Release(tag, name, binaryUrl)
        } finally {
            connection.disconnect()
        }
    }

    private fun downloadBinary(url: String, target: File) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/octet-stream")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("yt-dlp download HTTP $code")
            connection.inputStream.use { input ->
                target.outputStream().use { output -> input.copyTo(output, 64 * 1024) }
            }
            if (target.length() < 1024) throw IOException("Downloaded yt-dlp binary is unexpectedly small")
        } finally {
            connection.disconnect()
        }
    }
}
