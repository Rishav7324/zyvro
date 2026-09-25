package com.zyvro.app.data.repository

import com.zyvro.app.data.local.DownloadDao
import com.zyvro.app.data.local.DownloadEntity
import com.zyvro.app.data.local.DownloadStatus
import com.zyvro.app.data.local.MediaType
import com.zyvro.app.data.local.PlaylistEntity
import com.zyvro.app.data.local.PlaylistSong
import com.zyvro.app.data.preferences.AppPreferences
import kotlinx.coroutines.flow.Flow

class DownloadRepository(
    private val downloadDao: DownloadDao,
    val preferences: AppPreferences
) {
    val allDownloads: Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()
    val activeAndQueuedDownloads: Flow<List<DownloadEntity>> = downloadDao.getActiveAndQueuedDownloads()
    val completedDownloads: Flow<List<DownloadEntity>> = downloadDao.getCompletedDownloads()

    fun getCompletedDownloadsByType(mediaType: MediaType): Flow<List<DownloadEntity>> {
        return downloadDao.getCompletedDownloadsByType(mediaType)
    }

    suspend fun getDownloadById(id: Long): DownloadEntity? {
        return downloadDao.getDownloadById(id)
    }

    suspend fun enqueueDownload(download: DownloadEntity): Long {
        return downloadDao.insert(download)
    }

    suspend fun updateProgress(
        id: Long,
        progress: Float,
        speed: String,
        eta: String,
        downloadedBytes: Long,
        status: DownloadStatus = DownloadStatus.DOWNLOADING
    ) {
        downloadDao.updateProgress(id, progress, speed, eta, downloadedBytes, status)
    }

    suspend fun markCompleted(id: Long, targetPath: String) {
        downloadDao.markCompleted(id = id, targetPath = targetPath)
    }

    suspend fun markFailed(id: Long, error: String) {
        downloadDao.markFailed(id = id, error = error)
    }

    suspend fun retryDownload(id: Long) {
        downloadDao.resetForRetry(id)
    }

    suspend fun deleteDownload(id: Long) {
        downloadDao.deleteById(id)
    }

    suspend fun updateFormatNote(id: Long, note: String) {
        downloadDao.updateFormatNote(id, note)
    }

    suspend fun clearCompleted() {
        downloadDao.clearCompleted()
    }

    // ---- Play stats, favorites & playlists ----

    suspend fun recordPlay(id: Long) {
        downloadDao.recordPlay(id)
    }

    suspend fun toggleFavorite(id: Long) {
        downloadDao.toggleFavorite(id)
    }

    val favorites: Flow<List<DownloadEntity>> = downloadDao.getFavorites()

    fun getTopPlayed(limit: Int = 25): Flow<List<DownloadEntity>> =
        downloadDao.getTopPlayed(limit)

    fun getRecentlyPlayed(limit: Int = 25): Flow<List<DownloadEntity>> =
        downloadDao.getRecentlyPlayed(limit)

    suspend fun createPlaylist(name: String): Long =
        downloadDao.insertPlaylist(PlaylistEntity(name = name.trim()))

    val playlists: Flow<List<PlaylistEntity>> = downloadDao.getPlaylists()

    suspend fun deletePlaylist(id: Long) {
        downloadDao.clearPlaylistSongs(id)
        downloadDao.deletePlaylist(id)
    }

    suspend fun addSongToPlaylist(playlistId: Long, downloadId: Long) {
        if (downloadDao.isSongInPlaylist(playlistId, downloadId) == 0) {
            val pos = downloadDao.nextPlaylistPosition(playlistId)
            downloadDao.addSongToPlaylist(PlaylistSong(playlistId, downloadId, pos))
        }
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, downloadId: Long) {
        downloadDao.removeSongFromPlaylist(playlistId, downloadId)
    }

    fun getPlaylistSongs(playlistId: Long): Flow<List<DownloadEntity>> =
        downloadDao.getPlaylistSongs(playlistId)
}
