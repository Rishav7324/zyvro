package com.zyvro.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zyvro.app.player.MediaPlayerManager

class MediaControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val manager = MediaPlayerManager.getInstance(context)
        when (intent.action) {
            NotificationHelper.ACTION_PLAY_PAUSE -> manager.togglePlayPause()
            NotificationHelper.ACTION_PREVIOUS -> manager.playPrevious()
            NotificationHelper.ACTION_NEXT -> manager.playNext()
            NotificationHelper.ACTION_STOP -> manager.stopMedia()
        }
    }
}
