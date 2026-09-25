package com.zyvro.app.updater

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import com.zyvro.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val isUpdateAvailable: Boolean,
    val currentVersion: String,
    val latestVersion: String,
    val releaseTag: String,
    val downloadUrl: String,
    val releaseNotes: String = ""
)

object AppUpdater {
    private const val TAG = "AppUpdater"
    private const val GITHUB_REPO = "Rishav7324/zyvro"
    private const val CONNECT_TIMEOUT_MS = 12_000
    private const val READ_TIMEOUT_MS = 25_000
    private const val USER_AGENT = "Zyvro-AppUpdater"

    suspend fun checkForUpdate(): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val currentVersion = BuildConfig.VERSION_NAME.removePrefix("v").trim()
            
            // 1. Try redirect on /releases/latest to avoid GitHub API 403 rate-limits
            var latestTag = resolveLatestTagFromRedirect()

            var releaseNotes = ""
            var downloadUrl = ""

            // 2. If tag found from redirect, construct direct artifact URL
            if (!latestTag.isNullOrBlank()) {
                val cleanTag = latestTag.trim()
                val abi = getPreferredAbi()
                downloadUrl = "https://github.com/$GITHUB_REPO/releases/download/$cleanTag/Zyvro-$cleanTag-$abi.apk"
            } else {
                // 3. Fallback: try GitHub API
                val apiRelease = fetchFromGitHubApi()
                if (apiRelease != null) {
                    latestTag = apiRelease.tag
                    releaseNotes = apiRelease.notes
                    downloadUrl = apiRelease.downloadUrl
                }
            }

            if (latestTag.isNullOrBlank()) {
                throw Exception("Could not verify latest app release from GitHub")
            }

            val latestVersion = latestTag.removePrefix("v").trim()
            val updateAvailable = isNewerVersion(currentVersion, latestVersion)

            if (downloadUrl.isBlank()) {
                downloadUrl = "https://github.com/$GITHUB_REPO/releases/tag/$latestTag"
            }

            AppUpdateInfo(
                isUpdateAvailable = updateAvailable,
                currentVersion = currentVersion,
                latestVersion = latestVersion,
                releaseTag = latestTag,
                downloadUrl = downloadUrl,
                releaseNotes = releaseNotes
            )
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = {
                Log.e(TAG, "App update check failed", it)
                Result.failure(it)
            }
        )
    }

    private fun resolveLatestTagFromRedirect(): String? = runCatching {
        val url = "https://github.com/$GITHUB_REPO/releases/latest"
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false
            requestMethod = "HEAD"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("User-Agent", USER_AGENT)
        }
        try {
            val location = conn.getHeaderField("Location") ?: conn.getHeaderField("location")
            if (!location.isNullOrBlank()) {
                // Location: https://github.com/Rishav7324/zyvro/releases/tag/v4.0.0
                location.substringAfterLast("/")
            } else null
        } finally {
            conn.disconnect()
        }
    }.getOrNull()

    private data class ApiRelease(val tag: String, val notes: String, val downloadUrl: String)

    private fun fetchFromGitHubApi(): ApiRelease? = runCatching {
        val apiUrl = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
        val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", USER_AGENT)
        }
        try {
            if (conn.responseCode !in 200..299) return null
            val json = conn.inputStream.bufferedReader().use { JSONObject(it.readText()) }
            val tag = json.optString("tag_name")
            val notes = json.optString("body")
            val assets = json.optJSONArray("assets")
            val preferredAbi = getPreferredAbi()

            var bestUrl = ""
            var universalUrl = ""
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val name = asset.optString("name", "")
                    val url = asset.optString("browser_download_url", "")
                    if (name.contains(preferredAbi, ignoreCase = true) && name.endsWith(".apk")) {
                        bestUrl = url
                        break
                    }
                    if (name.contains("universal", ignoreCase = true) && name.endsWith(".apk")) {
                        universalUrl = url
                    }
                }
            }
            ApiRelease(
                tag = tag,
                notes = notes,
                downloadUrl = bestUrl.ifBlank { universalUrl }.ifBlank { "https://github.com/$GITHUB_REPO/releases/latest" }
            )
        } finally {
            conn.disconnect()
        }
    }.getOrNull()

    fun getPreferredAbi(): String {
        val supported = Build.SUPPORTED_ABIS
        return when {
            supported.any { it.contains("arm64-v8a", ignoreCase = true) } -> "arm64-v8a"
            supported.any { it.contains("armeabi-v7a", ignoreCase = true) } -> "armeabi-v7a"
            supported.any { it.contains("x86_64", ignoreCase = true) } -> "x86_64"
            supported.any { it.contains("x86", ignoreCase = true) } -> "x86"
            else -> "universal"
        }
    }

    fun isNewerVersion(current: String, latest: String): Boolean {
        if (current == latest) return false
        val currParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(currParts.size, latestParts.size)
        for (i in 0 until maxLen) {
            val c = currParts.getOrElse(i) { 0 }
            val l = latestParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    fun openBrowserDownload(context: Context, downloadUrl: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure {
            Log.e(TAG, "Failed to open download link in browser", it)
        }
    }

    fun startSystemDownload(context: Context, downloadUrl: String, version: String): Long? {
        return runCatching {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return null
            val fileName = "Zyvro-v$version-${getPreferredAbi()}.apk"
            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle("Downloading Zyvro v$version")
                setDescription("Updating Zyvro to latest version")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setMimeType("application/vnd.android.package-archive")
            }
            dm.enqueue(request)
        }.getOrNull()
    }
}
