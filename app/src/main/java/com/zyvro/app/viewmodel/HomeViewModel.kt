package com.zyvro.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zyvro.app.YtDlpApp
import com.zyvro.app.data.local.DownloadEntity
import com.zyvro.app.data.local.DownloadStatus
import com.zyvro.app.data.local.MediaType
import com.zyvro.app.engine.VideoInfo
import com.zyvro.app.engine.YtDlpEngine
import com.zyvro.app.service.DownloadService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

sealed interface HomeUiState {
    object Idle : HomeUiState
    object Loading : HomeUiState
    data class Success(val videoInfo: VideoInfo) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as YtDlpApp).repository

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    val recentDownloads = repository.allDownloads

    fun onUrlChanged(newUrl: String) {
        _urlInput.value = newUrl
    }

    fun parseUrl(url: String? = null) {
        val raw = (url ?: _urlInput.value).trim()
        if (raw.isBlank()) return

        val targetUrl = YtDlpEngine.normalizeUrl(raw)
        _urlInput.value = targetUrl
        _uiState.value = HomeUiState.Loading

        viewModelScope.launch {
            val cookiesFile = runCatching {
                val cookies = repository.preferences.cookiesContent.first()
                if (cookies.isNotBlank()) {
                    File(getApplication<Application>().cacheDir, "cookies_info.txt").also { it.writeText(cookies) }
                } else null
            }.getOrNull()

            try {
                val result = YtDlpEngine.fetchVideoInfo(getApplication(), targetUrl, cookiesFile)
                result.fold(
                    onSuccess = { info ->
                        _uiState.value = HomeUiState.Success(info)
                    },
                    onFailure = { error ->
                        _uiState.value = HomeUiState.Error(formatError(error))
                    }
                )
            } finally {
                cookiesFile?.delete()
            }
        }
    }

    private fun formatError(error: Throwable): String {
        val detail = generateSequence(error) { it.cause }
            .mapNotNull { it.message?.trim()?.takeIf(String::isNotBlank) }
            .firstOrNull { it.length >= 8 }

        return when {
            !YtDlpEngine.lastInitError.isNullOrBlank() && !YtDlpEngine.isInitialized ->
                "yt-dlp engine could not start. ${YtDlpEngine.lastInitError}"
            detail != null && (detail.contains("login", ignoreCase = true) || detail.contains("rate-limit", ignoreCase = true)) ->
                "Platform requested authentication or rate-limited. Try importing login cookies in Settings."
            detail != null -> detail
            error is java.net.UnknownHostException -> "No internet connection. Check your network and try again."
            else -> "Could not read this media link. Please verify the URL and try again."
        }
    }

    fun startDownload(
        videoInfo: VideoInfo,
        formatId: String,
        mediaType: MediaType,
        audioExt: String,
        autoStart: Boolean = true
    ) {
        viewModelScope.launch {
            val download = DownloadEntity(
                url = videoInfo.url,
                title = videoInfo.title,
                uploader = videoInfo.uploader,
                thumbnailUrl = videoInfo.thumbnailUrl,
                durationSeconds = videoInfo.durationSeconds,
                formatId = formatId,
                mediaType = mediaType,
                status = DownloadStatus.QUEUED
            )

            val downloadId = repository.enqueueDownload(download)
            if (autoStart) {
                DownloadService.startDownload(getApplication(), downloadId)
            }
        }
    }

    fun cancelDownload(downloadId: Long) {
        DownloadService.cancelDownload(getApplication(), downloadId)
    }

    fun deleteDownload(downloadId: Long) {
        viewModelScope.launch {
            repository.deleteDownload(downloadId)
        }
    }

    fun resetState() {
        _uiState.value = HomeUiState.Idle
    }
}
