package com.zyvro.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN ('QUEUED', 'DOWNLOADING', 'PAUSED') ORDER BY createdAt ASC")
    fun getActiveAndQueuedDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' ORDER BY completedAt DESC, createdAt DESC")
    fun getCompletedDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' AND mediaType = :mediaType ORDER BY completedAt DESC")
    fun getCompletedDownloadsByType(mediaType: MediaType): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: Long): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE status = 'QUEUED' ORDER BY createdAt ASC LIMIT 1")
    suspend fun getNextQueuedDownload(): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: DownloadEntity): Long

    @Update
    suspend fun update(download: DownloadEntity)

    @Query("UPDATE downloads SET progress = :progress, speed = :speed, eta = :eta, downloadedBytes = :downloadedBytes, status = :status WHERE id = :id")
    suspend fun updateProgress(
        id: Long,
        progress: Float,
        speed: String,
        eta: String,
        downloadedBytes: Long,
        status: DownloadStatus
    )

    @Query("UPDATE downloads SET status = :status, completedAt = :completedAt, targetPath = :targetPath WHERE id = :id")
    suspend fun markCompleted(id: Long, status: DownloadStatus = DownloadStatus.COMPLETED, completedAt: Long = System.currentTimeMillis(), targetPath: String)

    @Query("UPDATE downloads SET status = :status, errorMessage = :error WHERE id = :id")
    suspend fun markFailed(id: Long, status: DownloadStatus = DownloadStatus.FAILED, error: String)

    @Delete
    suspend fun delete(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE downloads SET status = 'QUEUED', progress = 0, speed = '', eta = '', downloadedBytes = 0, errorMessage = NULL WHERE id = :id")
    suspend fun resetForRetry(id: Long)

    @Query("UPDATE downloads SET formatNote = :note WHERE id = :id")
    suspend fun updateFormatNote(id: Long, note: String)

    @Query("DELETE FROM downloads WHERE status = 'COMPLETED'")
    suspend fun clearCompleted()

    // ---- Play stats & favorites (Retro-style smart lists) ----

    @Query("UPDATE downloads SET playCount = playCount + 1, lastPlayedAt = :playedAt WHERE id = :id")
    suspend fun recordPlay(id: Long, playedAt: Long = System.currentTimeMillis())

    @Query("UPDATE downloads SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long)

    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' AND isFavorite = 1 ORDER BY lastPlayedAt DESC, completedAt DESC")
    fun getFavorites(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' AND playCount > 0 ORDER BY playCount DESC LIMIT :limit")
    fun getTopPlayed(limit: Int = 25): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' AND lastPlayedAt IS NOT NULL ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun getRecentlyPlayed(limit: Int = 25): Flow<List<DownloadEntity>>

    // ---- User playlists ----

    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getPlaylists(): Flow<List<PlaylistEntity>>

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun clearPlaylistSongs(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongToPlaylist(song: PlaylistSong)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND downloadId = :downloadId")
    suspend fun removeSongFromPlaylist(playlistId: Long, downloadId: Long)

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId AND downloadId = :downloadId")
    suspend fun isSongInPlaylist(playlistId: Long, downloadId: Long): Int

    @Query("SELECT d.* FROM downloads d INNER JOIN playlist_songs ps ON d.id = ps.downloadId WHERE ps.playlistId = :playlistId ORDER BY ps.position ASC, ps.addedAt ASC")
    fun getPlaylistSongs(playlistId: Long): Flow<List<DownloadEntity>>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun nextPlaylistPosition(playlistId: Long): Int
}
