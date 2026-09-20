package com.zyvro.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zyvro.app.YtDlpApp
import com.zyvro.app.engine.YtDlpUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UpdateState {
    object Idle : UpdateState
    object Checking : UpdateState
    data class Success(val version: String) : UpdateState
    data class Error(val message: String) : UpdateState
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = (application as YtDlpApp).preferences

    val downloadPath = preferences.downloadPath.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val embedThumbnail = preferences.embedThumbnail.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val embedSubtitles = preferences.embedSubtitles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val useAria2 = preferences.useAria2.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val darkThemeMode = preferences.darkThemeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")
    val customArguments = preferences.customArguments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val cookiesContent = preferences.cookiesContent.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val sponsorBlockEnabled = preferences.sponsorBlockEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val aria2Connections = preferences.aria2Connections.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 8)
    val accentColor = preferences.accentColor.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "TEAL")
    val ytAndroidClient = preferences.ytAndroidClient.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val speedLimit = preferences.speedLimit.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Unlimited")
    val autoResumeWifi = preferences.autoResumeWifi.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val customDownloadDir = preferences.customDownloadDir.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val _engineVersion = MutableStateFlow("Loading...")
    val engineVersion: StateFlow<String> = _engineVersion.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    init {
        loadEngineVersion()
    }

    fun loadEngineVersion() {
        viewModelScope.launch {
            _engineVersion.value = YtDlpUpdater.getVersion(getApplication())
        }
    }

    fun updateYtDlp() {
        _updateState.value = UpdateState.Checking
        viewModelScope.launch {
            val result = YtDlpUpdater.updateEngine(getApplication())
            result.fold(
                onSuccess = { newVer ->
                    _engineVersion.value = newVer
                    _updateState.value = UpdateState.Success(newVer)
                },
                onFailure = { err ->
                    _updateState.value = UpdateState.Error(err.message ?: "Failed to update engine")
                }
            )
        }
    }

    fun setDownloadPath(path: String) = viewModelScope.launch { preferences.setDownloadPath(path) }
    fun setEmbedThumbnail(enabled: Boolean) = viewModelScope.launch { preferences.setEmbedThumbnail(enabled) }
    fun setEmbedSubtitles(enabled: Boolean) = viewModelScope.launch { preferences.setEmbedSubtitles(enabled) }
    fun setUseAria2(enabled: Boolean) = viewModelScope.launch { preferences.setUseAria2(enabled) }
    fun setDarkThemeMode(mode: String) = viewModelScope.launch { preferences.setDarkThemeMode(mode) }
    fun setCustomArguments(args: String) = viewModelScope.launch { preferences.setCustomArguments(args) }
    fun setCookiesContent(cookies: String) = viewModelScope.launch { preferences.setCookiesContent(cookies) }
    fun setSponsorBlockEnabled(enabled: Boolean) = viewModelScope.launch { preferences.setSponsorBlockEnabled(enabled) }
    fun setAria2Connections(count: Int) = viewModelScope.launch { preferences.setAria2Connections(count) }
    fun setAccentColor(accent: String) = viewModelScope.launch { preferences.setAccentColor(accent) }
    fun setYtAndroidClient(enabled: Boolean) = viewModelScope.launch { preferences.setYtAndroidClient(enabled) }
    fun setSpeedLimit(limit: String) = viewModelScope.launch { preferences.setSpeedLimit(limit) }
    fun setAutoResumeWifi(enabled: Boolean) = viewModelScope.launch { preferences.setAutoResumeWifi(enabled) }
    fun setCustomDownloadDir(dir: String) = viewModelScope.launch { preferences.setCustomDownloadDir(dir) }
}
