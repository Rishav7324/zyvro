package com.zyvro.app.ui.components

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.zyvro.app.data.local.DownloadEntity
import com.zyvro.app.data.local.DownloadStatus
import com.zyvro.app.data.local.MediaType
import com.zyvro.app.data.local.fileSize
import com.zyvro.app.data.local.formattedFileSize
import com.zyvro.app.ui.theme.*
import java.io.File

// ═══════════════════════════════════════════════════════════════════════════
// ZYVRO DOWNLOAD ITEM CARD v4.0 — Deep Space Aura
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun DownloadItemCard(
    download: DownloadEntity,
    onCancel: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onPlay: ((DownloadEntity) -> Unit)? = null,
    onShare: ((DownloadEntity) -> Unit)? = null,
    onToggleFavorite: ((Long) -> Unit)? = null,
    onTrim: ((DownloadEntity) -> Unit)? = null,
    isFavoriteOverride: Boolean? = null,
    modifier: Modifier = Modifier
) {
    val isDark    = LocalAppDark.current
    val isVideo   = download.mediaType == MediaType.VIDEO
    val isAudio   = download.mediaType == MediaType.AUDIO
    val accentGrad = if (isAudio) GradientViolet else GradientCyan
    val isFav     = isFavoriteOverride ?: download.isFavorite

    LiquidGlassCard(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        elevation = 6.dp,
        tintColor = when (download.status) {
            DownloadStatus.COMPLETED -> null
            DownloadStatus.ERROR     -> AccentRed.copy(alpha = 0.3f)
            else                     -> NovaPrimary.copy(alpha = 0.2f)
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier          = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ── Thumbnail ─────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .shadow(8.dp, RoundedCornerShape(14.dp), spotColor = accentGrad.first().copy(alpha = 0.3f))
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(accentGrad.map { it.copy(alpha = 0.5f) })),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isAudio) Icons.Rounded.MusicNote else Icons.Rounded.PlayCircle,
                        null,
                        tint     = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(28.dp)
                    )
                    val context    = LocalContext.current
                    val imageModel = remember(download.thumbnailUrl, download.targetPath) {
                        when {
                            download.thumbnailUrl.isNotBlank() -> download.thumbnailUrl
                            download.targetPath.isNotBlank()   -> File(download.targetPath)
                            else -> null
                        }
                    }
                    if (imageModel != null) {
                        val req = remember(imageModel) {
                            ImageRequest.Builder(context)
                                .data(imageModel)
                                .apply { if (isVideo) videoFrameMillis(2000) }
                                .crossfade(true)
                                .build()
                        }
                        AsyncImage(req, download.title, ContentScale.Crop, Modifier.fillMaxSize())
                    }

                    // Status badge overlay
                    when (download.status) {
                        DownloadStatus.COMPLETED -> Box(
                            Modifier
                                .size(18.dp)
                                .align(Alignment.BottomEnd)
                                .offset(x = 4.dp, y = 4.dp)
                                .shadow(4.dp, CircleShape)
                                .clip(CircleShape)
                                .background(AccentGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(11.dp))
                        }
                        DownloadStatus.ERROR -> Box(
                            Modifier
                                .size(18.dp)
                                .align(Alignment.BottomEnd)
                                .offset(x = 4.dp, y = 4.dp)
                                .shadow(4.dp, CircleShape)
                                .clip(CircleShape)
                                .background(AccentRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Error, null, tint = Color.White, modifier = Modifier.size(11.dp))
                        }
                        else -> {}
                    }
                }

                Spacer(Modifier.width(12.dp))

                // ── Content ───────────────────────────────────────────
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        download.title,
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        color      = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(Modifier.height(3.dp))

                    when (download.status) {
                        DownloadStatus.DOWNLOADING -> {
                            val animProgress by animateFloatAsState(
                                (download.progress / 100f).coerceIn(0f, 1f),
                                AppleSmoothSpringSpec,
                                label = "dl-progress"
                            )

                            // Neon animated progress bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (isDark) SpaceBorder else Color(0xFFD0DCFF))
                            ) {
                                val shimmer by rememberInfiniteTransition(label = "shimmer").animateFloat(
                                    0f, 1f,
                                    infiniteRepeatable(tween(1200), RepeatMode.Reverse),
                                    label = "shimmer-float"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(animProgress)
                                        .fillMaxHeight()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(NovaPrimary, NovaViolet.copy(alpha = 0.8f + shimmer * 0.2f), NovaPrimary)
                                            )
                                        )
                                )
                            }

                            Spacer(Modifier.height(4.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${download.progress.coerceIn(0, 100)}%",
                                    style      = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color      = NovaPrimary
                                )
                                if (download.speed.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(NovaPrimary.copy(alpha = 0.12f))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(download.speed, style = MaterialTheme.typography.labelSmall, color = NovaPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (download.eta.isNotBlank()) {
                                    Text("ETA ${download.eta}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        DownloadStatus.COMPLETED -> {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Type badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(accentGrad.first().copy(alpha = 0.12f))
                                        .border(0.5.dp, accentGrad.first().copy(alpha = 0.4f), RoundedCornerShape(5.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        if (isAudio) "AUDIO" else if (isVideo) "VIDEO" else "FILE",
                                        style  = MaterialTheme.typography.labelSmall,
                                        color  = accentGrad.first(),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                // File size
                                val file = remember(download.targetPath) { File(download.targetPath) }
                                if (file.exists() && file.length() > 0) {
                                    Text(
                                        download.formattedFileSize(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        DownloadStatus.ERROR -> {
                            Text(
                                "Download failed",
                                style  = MaterialTheme.typography.labelSmall,
                                color  = AccentRed,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        else -> {
                            Text(
                                download.status.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ── Action Buttons ─────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (download.status) {
                        DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED, DownloadStatus.PAUSED -> {
                            SmallActionButton(Icons.Rounded.Stop, "Cancel", AccentRed) { onCancel(download.id) }
                        }
                        DownloadStatus.COMPLETED -> {
                            if (onPlay != null) {
                                SmallActionButton(Icons.Rounded.PlayArrow, "Play", NovaPrimary) { onPlay(download) }
                            }
                            if (onToggleFavorite != null) {
                                SmallActionButton(
                                    if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    "Favorite",
                                    if (isFav) NovaRose else MaterialTheme.colorScheme.onSurfaceVariant
                                ) { onToggleFavorite(download.id) }
                            }
                            if (onShare != null) {
                                SmallActionButton(Icons.Rounded.Share, "Share", MaterialTheme.colorScheme.onSurfaceVariant) { onShare(download) }
                            }
                            SmallActionButton(Icons.Rounded.Delete, "Delete", AccentRed.copy(alpha = 0.7f)) { onDelete(download.id) }
                        }
                        DownloadStatus.ERROR -> {
                            SmallActionButton(Icons.Rounded.Delete, "Remove", AccentRed.copy(alpha = 0.7f)) { onDelete(download.id) }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    tint: Color,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(icon, desc, tint = tint, modifier = Modifier.size(18.dp))
    }
}
