package com.zyvro.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.zyvro.app.R
import com.zyvro.app.ui.MainActivity

object NotificationHelper {
    const val CHANNEL_DOWNLOADS = "ytdlp_downloads_channel"
    const val CHANNEL_COMPLETED = "ytdlp_completed_channel"
    const val CHANNEL_PLAYBACK = "zyvro_playback_channel"

    const val NOTIFICATION_ID_FOREGROUND = 1001
    const val NOTIFICATION_ID_PLAYBACK = 2001

    const val ACTION_PLAY_PAUSE = "com.zyvro.app.ACTION_PLAY_PAUSE"
    const val ACTION_PREVIOUS = "com.zyvro.app.ACTION_PREVIOUS"
    const val ACTION_NEXT = "com.zyvro.app.ACTION_NEXT"
    const val ACTION_STOP = "com.zyvro.app.ACTION_STOP"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val downloadChannel = NotificationChannel(
                CHANNEL_DOWNLOADS,
                "Active Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live download progress and speed"
                enableVibration(false)
                setSound(null, null)
            }

            val completedChannel = NotificationChannel(
                CHANNEL_COMPLETED,
                "Download Completed",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when downloads finish"
            }

            val playbackChannel = NotificationChannel(
                CHANNEL_PLAYBACK,
                "Media Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active media playback controls on lockscreen and shade"
                enableVibration(false)
                setSound(null, null)
            }

            notificationManager.createNotificationChannel(downloadChannel)
            notificationManager.createNotificationChannel(completedChannel)
            notificationManager.createNotificationChannel(playbackChannel)
        }
    }

    fun buildForegroundNotification(
        context: Context,
        title: String,
        progress: Int,
        speed: String,
        eta: String
    ): NotificationCompat.Builder {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val subText = buildString {
            if (speed.isNotBlank()) append(speed)
            if (speed.isNotBlank() && eta.isNotBlank()) append(" • ")
            if (eta.isNotBlank()) append("ETA: $eta")
        }

        return NotificationCompat.Builder(context, CHANNEL_DOWNLOADS)
            .setContentTitle(title)
            .setContentText(if (subText.isNotBlank()) subText else "Downloading...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
    }

    fun showCompletedNotification(context: Context, notificationId: Int, title: String, filePath: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val openIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_COMPLETED)
            .setContentTitle("Download Complete")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun updateMediaPlaybackNotification(
        context: Context,
        title: String,
        artist: String,
        isPlaying: Boolean
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPending = PendingIntent.getActivity(
            context,
            201,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = Intent(context, MediaControlReceiver::class.java).apply { action = ACTION_PREVIOUS }
        val prevPending = PendingIntent.getBroadcast(context, 202, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val playPauseIntent = Intent(context, MediaControlReceiver::class.java).apply { action = ACTION_PLAY_PAUSE }
        val playPausePending = PendingIntent.getBroadcast(context, 203, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val nextIntent = Intent(context, MediaControlReceiver::class.java).apply { action = ACTION_NEXT }
        val nextPending = PendingIntent.getBroadcast(context, 204, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(context, MediaControlReceiver::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getBroadcast(context, 205, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, CHANNEL_PLAYBACK)
            .setContentTitle(title)
            .setContentText(artist.ifBlank { "Zyvro Player" })
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(contentPending)
            .setDeleteIntent(stopPending)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPending)
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pause" else "Play",
                playPausePending
            )
            .addAction(android.R.drawable.ic_media_next, "Next", nextPending)
            .build()

        notificationManager.notify(NOTIFICATION_ID_PLAYBACK, notification)
    }

    fun cancelMediaPlaybackNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID_PLAYBACK)
    }
}
