package com.zyvro.app.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.zyvro.app.data.local.DownloadEntity
import com.zyvro.app.ui.theme.LocalAppDark
import com.zyvro.app.ui.theme.NovaCyan
import com.zyvro.app.ui.theme.NovaCyanDeep
import com.zyvro.app.util.AudioTrimmerUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioTrimmerDialog(
    media: DownloadEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = LocalAppDark.current

    val inputFile = remember { File(media.targetPath) }
    var totalDurationMs by remember { mutableLongStateOf(media.duration * 1000L) }
    var startMs by remember { mutableLongStateOf(0L) }
    var endMs by remember { mutableLongStateOf(if (media.duration > 0) (media.duration * 1000L).coerceAtMost(30000L) else 30000L) }

    var isPreviewPlaying by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    val previewPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            if (inputFile.exists()) {
                setMediaItem(MediaItem.fromUri(Uri.fromFile(inputFile)))
                prepare()
            }
        }
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY && totalDurationMs <= 0L) {
                    totalDurationMs = previewPlayer.duration.coerceAtLeast(1000L)
                    if (endMs <= 0L || endMs > totalDurationMs) {
                        endMs = (totalDurationMs).coerceAtMost(30000L)
                    }
                }
            }
            override fun onIsPlayingChanged(playing: Boolean) {
                isPreviewPlaying = playing
            }
        }
        previewPlayer.addListener(listener)

        onDispose {
            previewPlayer.stop()
            previewPlayer.release()
        }
    }

    // Interval preview loop
    LaunchedEffect(isPreviewPlaying) {
        if (isPreviewPlaying) {
            while (isActive && previewPlayer.isPlaying) {
                if (previewPlayer.currentPosition >= endMs) {
                    previewPlayer.seekTo(startMs)
                }
                delay(100)
            }
        }
    }

    fun togglePreview() {
        if (previewPlayer.isPlaying) {
            previewPlayer.pause()
        } else {
            previewPlayer.seekTo(startMs)
            previewPlayer.play()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = if (isDark) Color(0xFF0C1926) else Color(0xFFEEF7F7)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF162B3D) else Color(0xFFE0F5F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.ContentCut,
                            contentDescription = null,
                            tint = if (isDark) NovaCyan else NovaCyanDeep,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Audio Cutter & Ringtone",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            media.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            Spacer(Modifier.height(16.dp))

            // Duration and Range Card
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Start Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatTime(startMs), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) Color(0xFF142C3F) else Color(0xFFE0F5F6))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "Length: ${formatTime(endMs - startMs)}",
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) NovaCyan else NovaCyanDeep,
                                fontSize = 12.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("End Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatTime(endMs), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Start Slider
                    Text("Start: ${formatTime(startMs)}", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = startMs.toFloat(),
                        onValueChange = {
                            startMs = it.toLong().coerceAtMost(endMs - 1000L)
                        },
                        valueRange = 0f..(totalDurationMs.coerceAtLeast(30000L).toFloat()),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // End Slider
                    Text("End: ${formatTime(endMs)}", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = endMs.toFloat(),
                        onValueChange = {
                            endMs = it.toLong().coerceAtLeast(startMs + 1000L)
                        },
                        valueRange = 0f..(totalDurationMs.coerceAtLeast(30000L).toFloat()),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    // Step buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedButton(
                            onClick = { startMs = (startMs - 1000L).coerceAtLeast(0L) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("-1s Start", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { startMs = (startMs + 1000L).coerceAtMost(endMs - 1000L) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("+1s Start", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { endMs = (endMs - 1000L).coerceAtLeast(startMs + 1000L) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("-1s End", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { endMs = (endMs + 1000L).coerceAtMost(totalDurationMs.coerceAtLeast(30000L)) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("+1s End", fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Preview Play Button
            Button(
                onClick = { togglePreview() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0xFF162B3D) else Color(0xFFE0F5F6),
                    contentColor = if (isDark) NovaCyan else NovaCyanDeep
                )
            ) {
                Icon(
                    if (isPreviewPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(if (isPreviewPlaying) "Pause Preview" else "Play Selected Cut", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isProcessing = true
                            previewPlayer.pause()
                            val trimmedResult = AudioTrimmerUtil.trimAudio(
                                context = context,
                                inputFile = inputFile,
                                outputFileName = media.title,
                                startMs = startMs,
                                endMs = endMs
                            )
                            trimmedResult.fold(
                                onSuccess = { file ->
                                    val ringtoneResult = AudioTrimmerUtil.saveToRingtones(context, file, media.title, setAsDefault = false)
                                    ringtoneResult.fold(
                                        onSuccess = {
                                            Toast.makeText(context, "Saved to Ringtones folder!", Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                        },
                                        onFailure = {
                                            Toast.makeText(context, "Export error: ${it.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                },
                                onFailure = {
                                    Toast.makeText(context, "Trim error: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                            )
                            isProcessing = false
                        }
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isProcessing
                ) {
                    Icon(Icons.Rounded.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save Audio", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }

                Button(
                    onClick = {
                        scope.launch {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(context)) {
                                Toast.makeText(context, "Please allow 'Modify System Settings' to apply ringtone", Toast.LENGTH_LONG).show()
                                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                                return@launch
                            }

                            isProcessing = true
                            previewPlayer.pause()
                            val trimmedResult = AudioTrimmerUtil.trimAudio(
                                context = context,
                                inputFile = inputFile,
                                outputFileName = media.title,
                                startMs = startMs,
                                endMs = endMs
                            )
                            trimmedResult.fold(
                                onSuccess = { file ->
                                    val ringtoneResult = AudioTrimmerUtil.saveToRingtones(context, file, media.title, setAsDefault = true)
                                    ringtoneResult.fold(
                                        onSuccess = {
                                            Toast.makeText(context, "Phone Ringtone updated successfully!", Toast.LENGTH_LONG).show()
                                            onDismiss()
                                        },
                                        onFailure = {
                                            Toast.makeText(context, "Failed to apply ringtone: ${it.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                },
                                onFailure = {
                                    Toast.makeText(context, "Trim error: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                            )
                            isProcessing = false
                        }
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NovaCyanDeep, contentColor = Color.White),
                    enabled = !isProcessing
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Set Ringtone", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    val millis = (ms % 1000L) / 100L
    return String.format(java.util.Locale.US, "%02d:%02d.%d", minutes, seconds, millis)
}
