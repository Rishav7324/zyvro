package com.zyvro.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import com.zyvro.app.ui.theme.LocalAppDark
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zyvro.app.data.local.MediaType
import com.zyvro.app.engine.DownloadFormat
import com.zyvro.app.engine.VideoInfo
import com.zyvro.app.ui.theme.NovaCyan
import com.zyvro.app.ui.theme.NovaCyanDeep
import com.zyvro.app.ui.theme.NovaPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatSelectionSheet(
    videoInfo: VideoInfo,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onStartDownload: (String, MediaType, String) -> Unit,
    onQueueDownload: (String, MediaType, String) -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    val isDark = LocalAppDark.current
    val isInstagram = remember(videoInfo) {
        videoInfo.extractor.contains("instagram", ignoreCase = true)
    }
    val isImageMedia = remember(videoInfo) {
        videoInfo.formats.any { it.extension in listOf("jpg", "jpeg", "png", "webp") } ||
        videoInfo.title.contains("photo", ignoreCase = true)
    }
    val videos = remember(videoInfo) {
        val nonAudio = videoInfo.formats.filter { !it.isAudioOnly }
        val pList = nonAudio.filter { it.resolution.endsWith("p") }
            .sortedByDescending { it.resolution.removeSuffix("p").toIntOrNull() ?: 0 }
            .distinctBy { it.resolution }
            .take(8)
        if (pList.isNotEmpty()) {
            pList
        } else {
            nonAudio.distinctBy { it.resolution }.take(8)
        }
    }
    val best = remember(isInstagram, isImageMedia) {
        if (isInstagram || isImageMedia) {
            DownloadFormat("best", if (isImageMedia) "jpg" else "mp4", "Best (Original)", "Optimal Platform Stream", false)
        } else {
            DownloadFormat("bestvideo+bestaudio/best", "mp4", "Best (Highest)", "Merged video + audio stream", false)
        }
    }
    val qualities = remember(videos, best) {
        if (videos.any { it.formatId == best.formatId }) videos else listOf(best) + videos
    }
    val audioOutputs = listOf("mp3", "m4a", "opus", "wav", "flac")
    var selectedVideo by remember(qualities) { mutableStateOf(qualities.first().formatId) }
    var selectedAudio by remember { mutableStateOf("mp3") }

    // HD availability check: YouTube without login caps at ~720p behind bot-checks.
    val maxHeight = remember(videos) {
        videos.mapNotNull { it.resolution.removeSuffix("p").toIntOrNull() }.maxOrNull() ?: 0
    }
    val needsLoginForHd = remember(videoInfo) {
        videoInfo.extractor.contains("youtube", ignoreCase = true) && maxHeight in 1..720
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) Color(0xFF0C1926) else Color(0xFFEEF7F7),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header
            item {
                Text(
                    text = "Select Format & Quality",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = videoInfo.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Login nudge: only low formats visible = YouTube bot-check, needs cookies.
            if (needsLoginForHd) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFF9F0A).copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, Color(0xFFFF9F0A).copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Key,
                                contentDescription = null,
                                tint = Color(0xFFFF9F0A),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Only SD formats visible — YouTube limits HD without login. Import cookies.txt in Settings → YouTube Login.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Segmented Tabs: Video & Audio vs Audio Only
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (isDark) Color(0xFF142436) else Color.White)
                        .border(1.dp, if (isDark) Color(0xFF2E4B66) else Color(0xFFCCE7E8), RoundedCornerShape(22.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val activeBrush = if (isDark) {
                        Brush.horizontalGradient(listOf(Color(0xFF00E5FF).copy(alpha = 0.3f), Color(0xFF0077B6).copy(alpha = 0.4f)))
                    } else {
                        Brush.horizontalGradient(listOf(NovaCyan, NovaCyanDeep))
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(18.dp))
                            .then(
                                if (tab == 0) Modifier.background(activeBrush).border(1.dp, if (isDark) NovaCyan else Color.White, RoundedCornerShape(18.dp))
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(onClick = { tab = 0 }, modifier = Modifier.fillMaxSize()) {
                            Text(
                                if (isImageMedia) "Photo / Image" else "Video & Audio",
                                fontWeight = FontWeight.Bold,
                                color = if (tab == 0) (if (isDark) NovaCyan else Color(0xFF0C2326)) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(18.dp))
                            .then(
                                if (tab == 1) Modifier.background(activeBrush).border(1.dp, if (isDark) NovaCyan else Color.White, RoundedCornerShape(18.dp))
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(onClick = { tab = 1 }, modifier = Modifier.fillMaxSize()) {
                            Text(
                                "Audio Only",
                                fontWeight = FontWeight.Bold,
                                color = if (tab == 1) (if (isDark) NovaCyan else Color(0xFF0C2326)) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (tab == 0) {
                item {
                    Text(
                        if (isImageMedia) "AVAILABLE MEDIA & PHOTO FORMATS" else "AVAILABLE VIDEO STREAMS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) NovaCyan else NovaPrimary
                    )
                }
                items(qualities, key = { it.formatId }) { format ->
                    QualityCard(format, selectedVideo == format.formatId, isDark) {
                        selectedVideo = format.formatId
                    }
                }
            } else {
                item {
                    Text(
                        "AUDIO ENCODING PRESETS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) NovaCyan else NovaPrimary
                    )
                }
                items(audioOutputs, key = { it }) { ext ->
                    AudioCard(ext, selectedAudio == ext, isDark) {
                        selectedAudio = ext
                    }
                }
            }

            // Engine Notice Card
            item {
                LiquidGlassCard(
                    shape = RoundedCornerShape(18.dp),
                    elevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Bolt, null, tint = if (isDark) NovaCyan else NovaPrimary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (tab == 0) "Aria2c Multi-Thread Video Mux" else "FFmpeg Lossless Audio Extraction",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "Parallel streaming with automatic stream container assembly.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Action Buttons
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            val type = if (tab == 0) MediaType.VIDEO else MediaType.AUDIO
                            onQueueDownload(if (tab == 0) selectedVideo else "bestaudio/best", type, selectedAudio)
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, if (isDark) NovaCyan.copy(alpha = 0.5f) else NovaPrimary.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Rounded.Queue, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add to Queue", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val type = if (tab == 0) MediaType.VIDEO else MediaType.AUDIO
                            onStartDownload(if (tab == 0) selectedVideo else "bestaudio/best", type, selectedAudio)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = NovaCyanDeep),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NovaCyanDeep,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Rounded.Download, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Download Now", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun QualityCard(format: DownloadFormat, selected: Boolean, isDark: Boolean, onClick: () -> Unit) {
    LiquidGlassCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = if (selected) 4.dp else 1.dp
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) (if (isDark) NovaCyan.copy(alpha = 0.25f) else Color(0xFFE0F5F6))
                        else (if (isDark) Color(0xFF162A3E) else Color(0xFFF0F6FC))
                    ),
                contentAlignment = Alignment.Center
            ) {
                val isImg = format.extension in listOf("jpg", "jpeg", "png", "webp")
                Icon(
                    if (isImg) Icons.Rounded.Image else Icons.Rounded.Videocam,
                    null,
                    tint = if (selected) (if (isDark) NovaCyan else NovaPrimary) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(format.resolution, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleSmall)
                    if (format.resolution.contains("2160") || format.resolution.contains("4K") || format.resolution.contains("1080")) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isDark) NovaCyan.copy(alpha = 0.2f) else Color(0xFFE0F5F6))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                if (format.resolution.contains("2160") || format.resolution.contains("4K")) "4K Ultra HD" else "Full HD",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) NovaCyan else NovaPrimary
                            )
                        }
                    }
                }
                Text(
                    format.note.ifBlank { "Available stream" },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    format.extension.uppercase() + (format.fps?.let { " • ${it}fps" } ?: ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDark) NovaCyan else NovaPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (selected) {
                Icon(Icons.Rounded.CheckCircle, null, tint = if (isDark) NovaCyan else NovaPrimary, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun AudioCard(extension: String, selected: Boolean, isDark: Boolean, onClick: () -> Unit) {
    LiquidGlassCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = if (selected) 4.dp else 1.dp
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) (if (isDark) NovaCyan.copy(alpha = 0.25f) else Color(0xFFE0F5F6))
                        else (if (isDark) Color(0xFF162A3E) else Color(0xFFF0F6FC))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Audiotrack,
                    null,
                    tint = if (selected) (if (isDark) NovaCyan else NovaPrimary) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(extension.uppercase(), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleSmall)
                val note = when (extension) {
                    "mp3" -> "Universal • 320kbps High Compatibility"
                    "flac" -> "Lossless 24-bit Studio Quality"
                    "opus" -> "Next-Gen Low Latency / High Efficiency"
                    "m4a" -> "AAC Native Apple Ecosystem"
                    else -> "High Quality Audio"
                }
                Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) {
                Icon(Icons.Rounded.CheckCircle, null, tint = if (isDark) NovaCyan else NovaPrimary, modifier = Modifier.size(22.dp))
            }
        }
    }
}
