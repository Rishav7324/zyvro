package com.zyvro.app.data.preferences

import android.content.Context
import android.os.Environment
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ytdlp_settings")

class AppPreferences(private val context: Context) {

    companion object {
        val KEY_DOWNLOAD_PATH = stringPreferencesKey("download_path")
        val KEY_DEFAULT_VIDEO_QUALITY = stringPreferencesKey("default_video_quality")
        val KEY_DEFAULT_AUDIO_FORMAT = stringPreferencesKey("default_audio_format")
        val KEY_EMBED_THUMBNAIL = booleanPreferencesKey("embed_thumbnail")
        val KEY_EMBED_SUBTITLES = booleanPreferencesKey("embed_subtitles")
        val KEY_USE_ARIA2 = booleanPreferencesKey("use_aria2")
        val KEY_CONCURRENT_DOWNLOADS = intPreferencesKey("concurrent_downloads")
        val KEY_DARK_THEME_MODE = stringPreferencesKey("dark_theme_mode")
        val KEY_CUSTOM_ARGUMENTS = stringPreferencesKey("custom_arguments")
        val KEY_COOKIES_CONTENT = stringPreferencesKey("cookies_content")
        val KEY_SPONSOR_BLOCK_ENABLED = booleanPreferencesKey("sponsor_block_enabled")
        val KEY_ARIA2_CONNECTIONS = intPreferencesKey("aria2_connections")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_ACCENT_COLOR = stringPreferencesKey("accent_color")
        val KEY_DEVICE_FAVORITES = stringSetPreferencesKey("favorite_device_paths")
        val KEY_YT_ANDROID_CLIENT = booleanPreferencesKey("yt_android_client")
        val KEY_SPEED_LIMIT = stringPreferencesKey("speed_limit")
        val KEY_AUTO_RESUME_WIFI = booleanPreferencesKey("auto_resume_wifi")
        val KEY_CUSTOM_DOWNLOAD_DIR = stringPreferencesKey("custom_download_dir")
    }

    private val defaultDownloadDir: String
        get() {
            val extDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            return extDir?.absolutePath ?: context.filesDir.absolutePath
        }

    val downloadPath: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DOWNLOAD_PATH] ?: defaultDownloadDir
    }

    val defaultVideoQuality: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_VIDEO_QUALITY] ?: "best"
    }

    val defaultAudioFormat: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_AUDIO_FORMAT] ?: "mp3"
    }

    val embedThumbnail: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_EMBED_THUMBNAIL] ?: true
    }

    val embedSubtitles: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_EMBED_SUBTITLES] ?: false
    }

    val useAria2: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_USE_ARIA2] ?: false
    }

    val concurrentDownloads: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_CONCURRENT_DOWNLOADS] ?: 2
    }

    val darkThemeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DARK_THEME_MODE] ?: "SYSTEM"
    }

    val customArguments: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_CUSTOM_ARGUMENTS] ?: ""
    }

    val cookiesContent: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_COOKIES_CONTENT] ?: ""
    }

    val sponsorBlockEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SPONSOR_BLOCK_ENABLED] ?: true
    }

    val aria2Connections: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_ARIA2_CONNECTIONS] ?: 8
    }

    suspend fun setDownloadPath(path: String) {
        context.dataStore.edit { it[KEY_DOWNLOAD_PATH] = path }
    }

    suspend fun setDefaultVideoQuality(quality: String) {
        context.dataStore.edit { it[KEY_DEFAULT_VIDEO_QUALITY] = quality }
    }

    suspend fun setDefaultAudioFormat(format: String) {
        context.dataStore.edit { it[KEY_DEFAULT_AUDIO_FORMAT] = format }
    }

    suspend fun setEmbedThumbnail(enabled: Boolean) {
        context.dataStore.edit { it[KEY_EMBED_THUMBNAIL] = enabled }
    }

    suspend fun setEmbedSubtitles(enabled: Boolean) {
        context.dataStore.edit { it[KEY_EMBED_SUBTITLES] = enabled }
    }

    suspend fun setUseAria2(enabled: Boolean) {
        context.dataStore.edit { it[KEY_USE_ARIA2] = enabled }
    }

    suspend fun setConcurrentDownloads(count: Int) {
        context.dataStore.edit { it[KEY_CONCURRENT_DOWNLOADS] = count }
    }

    suspend fun setDarkThemeMode(mode: String) {
        context.dataStore.edit { it[KEY_DARK_THEME_MODE] = mode }
    }

    suspend fun setCustomArguments(args: String) {
        context.dataStore.edit { it[KEY_CUSTOM_ARGUMENTS] = args }
    }

    suspend fun setCookiesContent(cookies: String) {
        context.dataStore.edit { it[KEY_COOKIES_CONTENT] = cookies }
    }

    suspend fun setSponsorBlockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SPONSOR_BLOCK_ENABLED] = enabled }
    }

    suspend fun setAria2Connections(count: Int) {
        context.dataStore.edit { it[KEY_ARIA2_CONNECTIONS] = count }
    }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean = true) {
        context.dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    }

    /** Accent name: TEAL, BLUE, PURPLE, GREEN, ORANGE, PINK. */
    val accentColor: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_ACCENT_COLOR] ?: "TEAL"
    }

    suspend fun setAccentColor(accent: String) {
        context.dataStore.edit { it[KEY_ACCENT_COLOR] = accent }
    }

    /** Experimental YouTube android-client fallback (may unlock formats, may break age-gate). */
    val ytAndroidClient: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_YT_ANDROID_CLIENT] ?: false
    }

    suspend fun setYtAndroidClient(enabled: Boolean) {
        context.dataStore.edit { it[KEY_YT_ANDROID_CLIENT] = enabled }
    }

    /** Device (MediaStore) favorites by file path — Room only tracks downloads. */
    val deviceFavorites: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEVICE_FAVORITES] ?: emptySet()
    }

    suspend fun toggleDeviceFavorite(path: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_DEVICE_FAVORITES] ?: emptySet()
            prefs[KEY_DEVICE_FAVORITES] =
                if (current.contains(path)) current - path else current + path
        }
    }

    /** NextPlayer-style resume position per media id (0 = unknown). */
    suspend fun getResumePosition(mediaId: Long): Long {
        return context.dataStore.data.map { it[longPreferencesKey("resume_pos_$mediaId")] ?: 0L }.first()
    }

    suspend fun saveResumePosition(mediaId: Long, positionMs: Long) {
        context.dataStore.edit { it[longPreferencesKey("resume_pos_$mediaId")] = positionMs }
    }

    suspend fun clearResumePosition(mediaId: Long) {
        context.dataStore.edit { it.remove(longPreferencesKey("resume_pos_$mediaId")) }
    }

    /** Bandwidth limit for Aria2 and yt-dlp: 0 = unlimited, 1M, 5M, 10M, 20M. */
    val speedLimit: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SPEED_LIMIT] ?: "Unlimited"
    }

    suspend fun setSpeedLimit(limit: String) {
        context.dataStore.edit { it[KEY_SPEED_LIMIT] = limit }
    }

    /** Auto-resume incomplete downloads only on unmetered Wi-Fi connections. */
    val autoResumeWifi: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_RESUME_WIFI] ?: false
    }

    suspend fun setAutoResumeWifi(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_RESUME_WIFI] = enabled }
    }

    /** Custom download location (URI or absolute path) */
    val customDownloadDir: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_CUSTOM_DOWNLOAD_DIR] ?: ""
    }

    suspend fun setCustomDownloadDir(dir: String) {
        context.dataStore.edit { it[KEY_CUSTOM_DOWNLOAD_DIR] = dir }
    }
}
