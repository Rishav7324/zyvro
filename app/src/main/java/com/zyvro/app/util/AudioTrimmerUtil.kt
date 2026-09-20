package com.zyvro.app.util

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer

object AudioTrimmerUtil {
    private const val TAG = "AudioTrimmerUtil"

    suspend fun trimAudio(
        context: Context,
        inputFile: File,
        outputFileName: String,
        startMs: Long,
        endMs: Long
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val startUs = startMs * 1000L
            val endUs = endMs * 1000L

            val extractor = MediaExtractor()
            extractor.setDataSource(inputFile.absolutePath)

            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex == -1 || audioFormat == null) {
                extractor.release()
                error("No audio track found in media file")
            }

            extractor.selectTrack(audioTrackIndex)
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            val outputDir = File(context.cacheDir, "trimmed_audio").apply { mkdirs() }
            val ext = if (inputFile.extension.equals("mp3", true)) "m4a" else "m4a"
            val sanitizedName = outputFileName.replace(Regex("""[^a-zA-Z0-9._-]"""), "_")
            val outputFile = File(outputDir, "${sanitizedName}_cut.$ext")
            if (outputFile.exists()) outputFile.delete()

            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerTrackIndex = muxer.addTrack(audioFormat)
            muxer.start()

            val maxBufferSize = runCatching { audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) }.getOrDefault(1024 * 256)
            val buffer = ByteBuffer.allocate(maxBufferSize.coerceAtLeast(1024 * 128))
            val bufferInfo = MediaCodec.BufferInfo()

            try {
                while (true) {
                    bufferInfo.size = extractor.readSampleData(buffer, 0)
                    if (bufferInfo.size < 0) break

                    bufferInfo.presentationTimeUs = extractor.sampleTime
                    if (bufferInfo.presentationTimeUs > endUs) break

                    if (bufferInfo.presentationTimeUs >= startUs) {
                        bufferInfo.flags = extractor.sampleFlags
                        muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                    }
                    extractor.advance()
                }
            } finally {
                runCatching { muxer.stop() }
                runCatching { muxer.release() }
                runCatching { extractor.release() }
            }

            outputFile
        }
    }

    suspend fun saveToRingtones(
        context: Context,
        file: File,
        title: String,
        setAsDefault: Boolean = false
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "${title}_Ringtone.${file.extension}")
                put(MediaStore.MediaColumns.TITLE, title)
                put(MediaStore.MediaColumns.MIME_TYPE, if (file.extension == "mp3") "audio/mp3" else "audio/mp4")
                put(MediaStore.Audio.Media.IS_RINGTONE, true)
                put(MediaStore.Audio.Media.IS_NOTIFICATION, true)
                put(MediaStore.Audio.Media.IS_ALARM, true)
                put(MediaStore.Audio.Media.IS_MUSIC, false)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_RINGTONES}/Zyvro")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("Failed to insert into MediaStore Ringtones")

            resolver.openOutputStream(uri)?.use { out ->
                FileInputStream(file).use { input ->
                    input.copyTo(out)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }

            if (setAsDefault) {
                if (Settings.System.canWrite(context)) {
                    RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, uri)
                }
            }

            uri
        }
    }
}
