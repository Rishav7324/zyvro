package com.zyvro.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.os.IBinder
import android.util.Log
import com.zyvro.app.YtDlpApp
import com.zyvro.app.data.local.DownloadStatus
import com.zyvro.app.data.repository.DownloadRepository
import com.zyvro.app.engine.YtDlpEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

class DownloadService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var currentJob: Job? = null
    private var currentDownloadId: Long? = null

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_DOWNLOAD -> {
                val downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1L)
                if (downloadId != -1L) processNextOrSpecific(downloadId)
            }
            ACTION_CANCEL_DOWNLOAD -> {
                val downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1L)
                if (downloadId != -1L) cancelDownload(downloadId)
            }
        }
        return START_NOT_STICKY
    }

    private fun processNextOrSpecific(downloadId: Long) {
        if (currentJob?.isActive == true) {
            Log.d(TAG, "Download service is already processing; task $downloadId remains queued.")
            return
        }
        currentJob = serviceScope.launch { processDownload(downloadId) }
    }

    private suspend fun processDownload(downloadId: Long) {
        val repository = YtDlpApp.instance.repository
        val download = repository.getDownloadById(downloadId)
        if (download == null) {
            finishOrStartNext(repository)
            return
        }

        currentDownloadId = download.id
        val initialNotification = NotificationHelper.buildForegroundNotification(
            this@DownloadService,
            download.title,
            0,
            "",
            ""
        ).build()
        startForeground(NotificationHelper.NOTIFICATION_ID_FOREGROUND, initialNotification)

        val cookiesFile = try {
            val cookiesContent = repository.preferences.cookiesContent.first()
            if (cookiesContent.isBlank()) null else File(cacheDir, "cookies.txt").also { it.writeText(cookiesContent) }
        } catch (error: Exception) {
            Log.w(TAG, "Could not prepare cookies file", error)
            null
        }

        try {
            val targetDir = File(repository.preferences.downloadPath.first())
            val embedThumbnail = repository.preferences.embedThumbnail.first()
            val embedSubtitles = repository.preferences.embedSubtitles.first()
            val useAria2 = repository.preferences.useAria2.first()
            val customArgs = repository.preferences.customArguments.first()
            val ytAndroidClient = repository.preferences.ytAndroidClient.first()
            val speedLimit = repository.preferences.speedLimit.first()
            val customDir = repository.preferences.customDownloadDir.first()
            val taskId = "download_${download.id}"

            val result = YtDlpEngine.executeDownload(
                context = this@DownloadService,
                taskId = taskId,
                url = download.url,
                outputDir = targetDir,
                mediaType = download.mediaType,
                formatId = download.formatId,
                embedThumbnail = embedThumbnail,
                embedSubtitles = embedSubtitles,
                useAria2 = useAria2,
                customArgs = customArgs,
                cookiesFile = cookiesFile,
                ytAndroidClient = ytAndroidClient,
                speedLimit = speedLimit
            ) { progress, speed, eta, _ ->
                val progressPercent = progress.toInt().coerceIn(0, 100)
                serviceScope.launch {
                    runCatching {
                        repository.updateProgress(
                            id = download.id,
                            progress = progress,
                            speed = speed,
                            eta = eta,
                            downloadedBytes = 0L,
                            status = DownloadStatus.DOWNLOADING
                        )
                    }.onFailure { Log.w(TAG, "Failed to persist progress", it) }
                }

                runCatching {
                    NotificationHelper.buildForegroundNotification(
                        this@DownloadService,
                        download.title,
                        progressPercent,
                        speed,
                        eta
                    ).build().also { notification ->
                        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                        nm.notify(NotificationHelper.NOTIFICATION_ID_FOREGROUND, notification)
                    }
                }.onFailure { Log.w(TAG, "Failed to update download notification", it) }
            }

            result.fold(
                onSuccess = { file ->
                    val publicFile = runCatching {
                        com.zyvro.app.util.StorageHelper.exportToPublicStorage(
                            context = this@DownloadService,
                            srcFile = file,
                            mediaType = download.mediaType,
                            title = download.title,
                            customDirUriOrPath = customDir
                        )
                    }.getOrNull()
                    val finalFile = if (publicFile != null && publicFile.exists()) publicFile else file

                    repository.markCompleted(download.id, finalFile.absolutePath)
                    // Requested-vs-actual proof: probe real video height into formatNote.
                    runCatching {
                        val actual = probeVideoHeight(finalFile)
                        if (actual != null) {
                            repository.updateFormatNote(download.id, "${actual}p")
                        }
                    }
                    NotificationHelper.showCompletedNotification(
                        this@DownloadService,
                        download.id.toInt(),
                        download.title,
                        finalFile.absolutePath
                    )
                },
                onFailure = { error ->
                    repository.markFailed(
                        download.id,
                        com.zyvro.app.engine.DownloadErrors.friendlyMessage(
                            error.message ?: "Unknown download error",
                            hasCookies = cookiesFile != null
                        )
                    )
                }
            )
        } finally {
            cookiesFile?.delete()
            currentDownloadId = null
        }

        finishOrStartNext(repository)
    }

    private suspend fun finishOrStartNext(repository: DownloadRepository) {
        val next = repository.activeAndQueuedDownloads.first().firstOrNull {
            it.status == DownloadStatus.QUEUED
        }
        if (next != null) {
            processDownload(next.id)
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun cancelDownload(downloadId: Long) {
        YtDlpEngine.cancelDownload("download_$downloadId")
        serviceScope.launch {
            runCatching {
                YtDlpApp.instance.repository.updateProgress(
                    id = downloadId,
                    progress = 0f,
                    speed = "",
                    eta = "",
                    downloadedBytes = 0L,
                    status = DownloadStatus.CANCELLED
                )
            }.onFailure { Log.w(TAG, "Failed to mark download cancelled", it) }
        }
        if (currentDownloadId == downloadId) {
            currentJob?.cancel()
            currentDownloadId = null
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /** Probes the real video height of a finished file (null for audio/unknown). */
    private fun probeVideoHeight(file: File): Int? {
        if (!file.exists()) return null
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    companion object {
        private const val TAG = "DownloadService"
        const val ACTION_START_DOWNLOAD = "com.zyvro.app.ACTION_START_DOWNLOAD"
        const val ACTION_CANCEL_DOWNLOAD = "com.zyvro.app.ACTION_CANCEL_DOWNLOAD"
        const val EXTRA_DOWNLOAD_ID = "extra_download_id"

        fun startDownload(context: Context, downloadId: Long) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START_DOWNLOAD
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun cancelDownload(context: Context, downloadId: Long) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_CANCEL_DOWNLOAD
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            }
            context.startService(intent)
        }
    }
}
