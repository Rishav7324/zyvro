package com.zyvro.app.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.zyvro.app.YtDlpApp
import com.zyvro.app.data.local.DownloadEntity
import com.zyvro.app.data.local.MediaType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.zyvro.app.service.NotificationHelper
import java.io.File

/** A single selectable audio/subtitle track surfaced to the UI. */
data class PlayerTrack(
    val label: String,
    val group: TrackGroup,
    val trackIndex: Int,
    val isSelected: Boolean
)

@OptIn(UnstableApi::class)
class MediaPlayerManager private constructor(private val context: Context) {

    val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.CONTENT_TYPE_MOVIE)
                .build(),
            true // handle audio focus: duck/pause on calls & notifications
        )
        .setHandleAudioBecomingNoisy(true) // auto-pause on headphone disconnect
        .build()

    private val _currentMedia = MutableStateFlow<DownloadEntity?>(null)
    val currentMedia: StateFlow<DownloadEntity?> = _currentMedia.asStateFlow()

    private val _queue = MutableStateFlow<List<DownloadEntity>>(emptyList())
    val queue: StateFlow<List<DownloadEntity>> = _queue.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _isVideoExpanded = MutableStateFlow(false)
    val isVideoExpanded: StateFlow<Boolean> = _isVideoExpanded.asStateFlow()

    private val _isAudioSheetOpen = MutableStateFlow(false)
    val isAudioSheetOpen: StateFlow<Boolean> = _isAudioSheetOpen.asStateFlow()

    // A-B Loop
    private val _loopPointA = MutableStateFlow<Long?>(null)
    val loopPointA: StateFlow<Long?> = _loopPointA.asStateFlow()

    private val _loopPointB = MutableStateFlow<Long?>(null)
    val loopPointB: StateFlow<Long?> = _loopPointB.asStateFlow()

    // Track selection (audio / subtitles)
    private val _audioTracks = MutableStateFlow<List<PlayerTrack>>(emptyList())
    val audioTracks: StateFlow<List<PlayerTrack>> = _audioTracks.asStateFlow()

    private val _subtitleTracks = MutableStateFlow<List<PlayerTrack>>(emptyList())
    val subtitleTracks: StateFlow<List<PlayerTrack>> = _subtitleTracks.asStateFlow()

    private val _subtitlesEnabled = MutableStateFlow(true)
    val subtitlesEnabled: StateFlow<Boolean> = _subtitlesEnabled.asStateFlow()

    private val _playerError = MutableStateFlow<String?>(null)
    val playerError: StateFlow<String?> = _playerError.asStateFlow()

    // SFX availability surfaced to the equalizer UI
    private val _fxActive = MutableStateFlow(false)
    val fxActive: StateFlow<Boolean> = _fxActive.asStateFlow()

    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
                if (playing) startProgressTracking() else stopProgressTracking()
                updateNotification()
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    _duration.value = player.duration.coerceAtLeast(0L)
                    AudioFxManager.instance.initAudioEffects(player.audioSessionId)
                    _fxActive.value = AudioFxManager.instance.hasActiveSession()
                } else if (state == Player.STATE_ENDED) {
                    // Native playlist already auto-advanced; just drop the bookmark.
                    persistResumePosition()
                    updateNotification()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // Fires for native auto-advance AND manual next/previous: keep app
                // state (current item, stats, A-B loop) in sync with the player.
                // PLAYLIST_CHANGED is skipped — openQueue() sets state itself.
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                    reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK
                ) {
                    val id = mediaItem?.mediaId?.toLongOrNull()
                    val entity = _queue.value.firstOrNull { it.id == id } ?: return
                    _currentMedia.value = entity
                    scope.launch {
                        runCatching { YtDlpApp.instance.repository.recordPlay(entity.id) }
                    }
                    clearAbLoop()
                    _playerError.value = null
                    updateNotification()
                }
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                    AudioFxManager.instance.initAudioEffects(audioSessionId)
                    _fxActive.value = AudioFxManager.instance.hasActiveSession()
                }
            }

            override fun onRepeatModeChanged(repeat: Int) {
                _repeatMode.value = repeat
            }

            override fun onTracksChanged(tracks: Tracks) {
                refreshTrackFlows(tracks)
            }

            override fun onPlayerError(error: PlaybackException) {
                _playerError.value = error.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: "Playback error (${error.errorCodeName})"
            }
        })
    }

    private fun refreshTrackFlows(tracks: Tracks = player.currentTracks) {
        val audio = mutableListOf<PlayerTrack>()
        val subs = mutableListOf<PlayerTrack>()
        var subIndex = 0
        for (group in tracks.groups) {
            when (group.type) {
                C.TRACK_TYPE_AUDIO -> {
                    for (i in 0 until group.length) {
                        if (!group.isTrackSupported(i)) continue
                        val f = group.getTrackFormat(i)
                        audio.add(
                            PlayerTrack(
                                label = audioTrackLabel(f.language, f.sampleMimeType, f.channelCount),
                                group = group.mediaTrackGroup,
                                trackIndex = i,
                                isSelected = group.isTrackSelected(i)
                            )
                        )
                    }
                }
                C.TRACK_TYPE_TEXT -> {
                    for (i in 0 until group.length) {
                        if (!group.isTrackSupported(i)) continue
                        val f = group.getTrackFormat(i)
                        subIndex++
                        subs.add(
                            PlayerTrack(
                                label = f.label?.takeIf { it.isNotBlank() }
                                    ?: f.language?.takeIf { it.isNotBlank() && it != "und" }
                                    ?: "Subtitle $subIndex",
                                group = group.mediaTrackGroup,
                                trackIndex = i,
                                isSelected = group.isTrackSelected(i)
                            )
                        )
                    }
                }
            }
        }
        _audioTracks.value = audio
        _subtitleTracks.value = subs
        // Any selected/enabled text track means subtitles are on.
        if (subs.isNotEmpty()) {
            _subtitlesEnabled.value = subs.any { it.isSelected }
        }
    }

    private fun audioTrackLabel(language: String?, mime: String?, channels: Int): String {
        val parts = mutableListOf<String>()
        language?.takeIf { it.isNotBlank() && it != "und" }?.let { parts.add(it) }
        mime?.substringAfter('/')?.uppercase()?.let { parts.add(it) }
        if (channels > 0) parts.add("${channels}ch")
        return parts.ifEmpty { listOf("Audio track") }.joinToString(" · ")
    }

    /** Select an embedded or sideloaded audio track. */
    fun selectAudioTrack(track: PlayerTrack) {
        val params = player.trackSelectionParameters.buildUpon()
            .setOverrideForType(TrackSelectionOverride(track.group, track.trackIndex))
            .build()
        player.trackSelectionParameters = params
        refreshTrackFlows()
    }

    /** Select an embedded or sideloaded subtitle track (also re-enables subs). */
    fun selectSubtitleTrack(track: PlayerTrack) {
        val params = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .setOverrideForType(TrackSelectionOverride(track.group, track.trackIndex))
            .build()
        player.trackSelectionParameters = params
        _subtitlesEnabled.value = true
        refreshTrackFlows()
    }

    /** Turn subtitles off completely (embedded + sideloaded). */
    fun disableSubtitles() {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
        _subtitlesEnabled.value = false
        refreshTrackFlows()
    }

    /** Re-enable subtitle rendering (system/override selection applies). */
    fun enableSubtitles() {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .build()
        _subtitlesEnabled.value = true
        refreshTrackFlows()
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val pos = player.currentPosition.coerceAtLeast(0L)
                _currentPosition.value = pos
                _duration.value = player.duration.coerceAtLeast(0L)

                // A-B Loop Check
                val a = _loopPointA.value
                val b = _loopPointB.value
                if (a != null && b != null && b > a) {
                    if (pos >= b) {
                        player.seekTo(a)
                    }
                }

                delay(200)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
    }

    fun playMedia(entity: DownloadEntity, playlist: List<DownloadEntity> = emptyList(), openFullscreenIfVideo: Boolean = true) {
        val newQueue = when {
            playlist.isNotEmpty() -> playlist
            _queue.value.any { it.id == entity.id } -> _queue.value
            else -> listOf(entity) + _queue.value
        }
        openQueue(newQueue, entity, openFullscreenIfVideo)
    }

    /** Loads the app queue into ExoPlayer natively so advance/repeat/shuffle
     *  are handled by the engine (reliable auto-next, gapless transitions). */
    private fun openQueue(queue: List<DownloadEntity>, start: DownloadEntity, openFullscreenIfVideo: Boolean) {
        val items = queue.mapNotNull { e -> buildMirrorItem(e)?.let { e to it } }
        if (items.isEmpty()) return
        val startIndex = items.indexOfFirst { it.first.id == start.id }.takeIf { it >= 0 } ?: 0
        val startEntity = items[startIndex].first

        _queue.value = items.map { it.first }
        _currentMedia.value = startEntity
        scope.launch {
            runCatching { YtDlpApp.instance.repository.recordPlay(startEntity.id) }
        }

        clearAbLoop()
        _playerError.value = null
        _audioTracks.value = emptyList()
        _subtitleTracks.value = emptyList()
        _subtitlesEnabled.value = true
        player.playWhenReady = true
        player.setMediaItems(items.map { it.second }, startIndex, 0L)
        player.prepare()
        _playbackSpeed.value = 1.0f
        player.playbackParameters = PlaybackParameters(1.0f)

        if (startEntity.mediaType == MediaType.VIDEO) {
            if (openFullscreenIfVideo) {
                _isVideoExpanded.value = true
            }
        } else {
            _isAudioSheetOpen.value = true
        }
    }

    private fun resolveUri(entity: DownloadEntity): Uri? {
        val file = File(entity.targetPath)
        return when {
            entity.targetPath.startsWith("content://") -> Uri.parse(entity.targetPath)
            file.exists() -> Uri.fromFile(file)
            entity.url.startsWith("http://") || entity.url.startsWith("https://") -> Uri.parse(entity.url)
            else -> null
        }
    }

    private fun buildMirrorItem(entity: DownloadEntity): MediaItem? {
        val uri = resolveUri(entity) ?: return null
        return MediaItem.Builder()
            .setMediaId(entity.id.toString())
            .setUri(uri)
            .setSubtitleConfigurations(findSidecarSubtitles(File(entity.targetPath)))
            .build()
    }

    /**
     * Auto-loads same-name subtitle sidecars (video.srt / video.vtt / video.ass)
     * sitting next to the media file, matching desktop-player behavior.
     */
    private fun findSidecarSubtitles(mediaFile: File): List<MediaItem.SubtitleConfiguration> {
        if (!mediaFile.exists()) return emptyList()
        val base = mediaFile.nameWithoutExtension
        val dir = mediaFile.parentFile ?: return emptyList()
        val mimeByExt = mapOf(
            "srt" to MimeTypes.APPLICATION_SUBRIP,
            "vtt" to MimeTypes.TEXT_VTT,
            "ass" to MimeTypes.TEXT_SSA,
            "ssa" to MimeTypes.TEXT_SSA
        )
        return mimeByExt.mapNotNull { (ext, mime) ->
            val sub = File(dir, "$base.$ext")
            if (sub.exists() && sub.isFile) {
                MediaItem.SubtitleConfiguration.Builder(Uri.fromFile(sub))
                    .setMimeType(mime)
                    .setLanguage("und")
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()
            } else null
        }
    }

    /** Native advance: the engine walks the mirrored playlist (repeat-aware). */
    fun playNext() {
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        }
    }

    fun playPrevious() {
        if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else {
            seekTo(0L)
        }
    }

    fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
        player.shuffleModeEnabled = _isShuffleEnabled.value
    }

    /** Retro-style manual queue reorder (drag-equivalent via up/down controls). */
    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val q = _queue.value.toMutableList()
        if (fromIndex !in q.indices || toIndex !in q.indices || fromIndex == toIndex) return
        val moved = q.removeAt(fromIndex)
        q.add(toIndex, moved)
        _queue.value = q
        // Re-mirror around the currently playing item without interrupting it.
        val currentId = _currentMedia.value?.id?.toString()
        val items = q.mapNotNull { buildMirrorItem(it) }
        if (items.isEmpty()) return
        val idx = items.indexOfFirst { it.mediaId == currentId }.takeIf { it >= 0 } ?: 0
        val pos = player.currentPosition.coerceAtLeast(0L)
        val wasPlaying = player.playWhenReady
        player.playWhenReady = wasPlaying
        player.setMediaItems(items, idx, pos)
        player.prepare()
    }

    fun pause() {
        player.pause()
        persistResumePosition()
    }

    fun play() {
        player.play()
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
            persistResumePosition()
        } else {
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceIn(0L, player.duration.coerceAtLeast(0L)))
        _currentPosition.value = positionMs
    }

    fun seekForward(deltaMs: Long = 10000L) {
        seekTo(player.currentPosition + deltaMs)
    }

    fun seekRewind(deltaMs: Long = 10000L) {
        seekTo(player.currentPosition - deltaMs)
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        player.playbackParameters = PlaybackParameters(speed)
    }

    fun toggleRepeatMode() {
        val nextMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
        player.repeatMode = nextMode
        _repeatMode.value = nextMode
    }

    // A-B Loop Functions
    fun setLoopPointA() {
        _loopPointA.value = player.currentPosition
    }

    fun setLoopPointB() {
        if (_loopPointA.value != null && player.currentPosition > _loopPointA.value!!) {
            _loopPointB.value = player.currentPosition
        }
    }

    fun clearAbLoop() {
        _loopPointA.value = null
        _loopPointB.value = null
    }

    fun setVideoExpanded(expanded: Boolean) {
        _isVideoExpanded.value = expanded
    }

    fun setAudioSheetOpen(open: Boolean) {
        _isAudioSheetOpen.value = open
    }

    /** NextPlayer-style resume: remember position for finished/paused media. */
    private fun persistResumePosition() {
        val media = _currentMedia.value ?: return
        val pos = player.currentPosition.coerceAtLeast(0L)
        val dur = player.duration.coerceAtLeast(0L)
        scope.launch {
            runCatching {
                val prefs = YtDlpApp.instance.preferences
                // Drop the bookmark when finished (<10s left) or barely started.
                if (dur > 0 && pos > 10_000L && pos < dur - 10_000L) {
                    prefs.saveResumePosition(media.id, pos)
                } else {
                    prefs.clearResumePosition(media.id)
                }
            }
        }
    }

    suspend fun getResumePosition(mediaId: Long): Long =
        runCatching { YtDlpApp.instance.preferences.getResumePosition(mediaId) }.getOrDefault(0L)

    /** Stats-for-nerds snapshot for the current video. */
    data class PlaybackStats(
        val resolution: String,
        val videoCodec: String,
        val audioCodec: String,
        val bitrate: String,
        val frameRate: String
    )

    fun getPlaybackStats(): PlaybackStats? {
        val v = player.videoFormat ?: return null
        val a = player.audioFormat
        val res = if (v.width > 0 && v.height > 0) "${v.width}×${v.height}" else "—"
        val vcodec = v.sampleMimeType?.substringAfter('/')?.uppercase() ?: "—"
        val acodec = a?.sampleMimeType?.substringAfter('/')?.uppercase() ?: "—"
        val br = v.bitrate.takeIf { it > 0 }?.let { "%.1f Mbps".format(it / 1_000_000f) } ?: "—"
        val fps = v.frameRate.takeIf { it > 0 }?.let { "%.0f fps".format(it) } ?: "—"
        return PlaybackStats(res, vcodec, acodec, br, fps)
    }

    fun closePlayer() {
        persistResumePosition()
        player.stop()
        _currentMedia.value = null
        _playerError.value = null
        _isVideoExpanded.value = false
        _isAudioSheetOpen.value = false
        clearAbLoop()
        AudioFxManager.instance.release()
        stopProgressTracking()
        NotificationHelper.cancelMediaPlaybackNotification(context)
    }

    fun stopMedia() = closePlayer()

    private fun updateNotification() {
        val media = _currentMedia.value ?: run {
            NotificationHelper.cancelMediaPlaybackNotification(context)
            return
        }
        NotificationHelper.updateMediaPlaybackNotification(
            context = context,
            title = media.title,
            artist = media.uploader,
            isPlaying = _isPlaying.value
        )
    }

    companion object {
        @Volatile
        private var instance: MediaPlayerManager? = null

        fun getInstance(context: Context): MediaPlayerManager {
            return instance ?: synchronized(this) {
                instance ?: MediaPlayerManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
