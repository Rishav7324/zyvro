package com.zyvro.app.util

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.zyvro.app.data.local.MediaType
import java.io.File
import java.io.FileInputStream

object StorageHelper {
    private const val TAG = "StorageHelper"

    fun getPublicMediaDirectory(mediaType: MediaType): File {
        val folderName = if (mediaType == MediaType.AUDIO) {
            Environment.DIRECTORY_MUSIC
        } else {
            Environment.DIRECTORY_DOWNLOADS
        }
        val publicDir = File(Environment.getExternalStoragePublicDirectory(folderName), "Zyvro")
        if (!publicDir.exists()) {
            runCatching { publicDir.mkdirs() }
        }
        return publicDir
    }

    fun exportToPublicStorage(
        context: Context,
        srcFile: File,
        mediaType: MediaType,
        title: String,
        customDirUriOrPath: String? = null
    ): File {
        if (!srcFile.exists()) return srcFile

        try {
            val isAudio = mediaType == MediaType.AUDIO
            val ext = srcFile.extension.lowercase()
            val isImage = ext in listOf("jpg", "jpeg", "png", "webp")
            val mimeType = when {
                isAudio -> "audio/*"
                isImage -> "image/*"
                else -> "video/*"
            }

            // If user configured a custom download directory or SD card path
            if (!customDirUriOrPath.isNullOrBlank()) {
                val customTarget = runCatching {
                    val dir = File(customDirUriOrPath)
                    if (dir.exists() || dir.mkdirs()) {
                        val dest = File(dir, srcFile.name)
                        srcFile.copyTo(dest, overwrite = true)
                        dest
                    } else null
                }.getOrNull()
                if (customTarget != null && customTarget.exists()) {
                    MediaScannerConnection.scanFile(
                        context.applicationContext,
                        arrayOf(customTarget.absolutePath),
                        arrayOf(mimeType),
                        null
                    )
                    return customTarget
                }
            }

            // 1. Android 10+ (API 29+) MediaStore scoped storage insertion
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                runCatching {
                    val collection: Uri = when {
                        isAudio -> MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                        isImage -> MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                        else -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    }

                    val relativePath = when {
                        isAudio -> "${Environment.DIRECTORY_MUSIC}/Zyvro"
                        isImage -> "${Environment.DIRECTORY_PICTURES}/Zyvro"
                        else -> "${Environment.DIRECTORY_MOVIES}/Zyvro"
                    }

                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, srcFile.name)
                        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }

                    val resolver = context.contentResolver
                    val itemUri = resolver.insert(collection, values)

                    if (itemUri != null) {
                        resolver.openOutputStream(itemUri)?.use { out ->
                            FileInputStream(srcFile).use { input ->
                                input.copyTo(out, 64 * 1024)
                            }
                        }
                        values.clear()
                        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(itemUri, values, null, null)
                        Log.d(TAG, "Exported to Android MediaStore: $itemUri")
                    }
                }.onFailure { Log.w(TAG, "MediaStore insertion fallback", it) }
            }

            // 2. Legacy / Direct file copy for immediate local disk path resolution
            val folderName = when {
                isAudio -> Environment.DIRECTORY_MUSIC
                isImage -> Environment.DIRECTORY_PICTURES
                else -> Environment.DIRECTORY_DOWNLOADS
            }
            val publicDir = File(Environment.getExternalStoragePublicDirectory(folderName), "Zyvro")
            if (!publicDir.exists()) publicDir.mkdirs()
            val destFile = File(publicDir, srcFile.name)

            if (srcFile.absolutePath != destFile.absolutePath) {
                srcFile.copyTo(destFile, overwrite = true)
            }

            // Trigger system MediaScanner so Gallery and Music apps index it immediately
            MediaScannerConnection.scanFile(
                context.applicationContext,
                arrayOf(destFile.absolutePath, srcFile.absolutePath),
                arrayOf(mimeType, null)
            ) { path, uri ->
                Log.d(TAG, "Indexed file in system: $path -> $uri")
            }

            return if (destFile.exists()) destFile else srcFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy to public storage, using source file", e)
            return srcFile
        }
    }
}
