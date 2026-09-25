package com.zyvro.app.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.videoFrameMillis
import com.zyvro.app.data.local.MediaType
import com.zyvro.app.player.MediaPlayerManager
import com.zyvro.app.ui.components.liquidGlass
import com.zyvro.app.ui.theme.*

// ═══════════════════════════════════════════════════════════════════════════
// ZYVRO MINI PLAYER v4.0 — Deep Space Aura
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun MiniPlayerBar() {
    val context       = LocalContext.current
    val playerManager = MediaPlayerManager.getInstance(context)
    val currentMedia  by playerManager.currentMedia.collectAsState()
    val isPlaying     by playerManager.isPlaying.collectAsState()
    val position      by playerManager.currentPosition.collectAsState()
    val duration      by playerManager.duration.collectAsState()
    val isDark        = LocalAppDark.current

    // Swipe to dismiss
    var offsetX by remember { mutableFloatStateOf(0f) }

    AnimatedVisibility(
        visible = currentMedia != null,
        enter   = slideInVertically(tween(320)) { it } + fadeIn(tween(320)),
        exit    = slideOutVertically(tween(220)) { it } + fadeOut(tween(220))
    ) {
        val item = currentMedia ?: return@AnimatedVisibility
        val progress = if (duration > 0) position.toFloat() / duration.toFloat() else 0f
        val isVideo  = item.mediaType == MediaType.VIDEO

        val accentGradient = if (isVideo) GradientCyan else GradientViolet
        val bgColor        = if (isDark) SpaceCard.copy(alpha = 0.97f) else Color.White.copy(alpha = 0.97f)

        // Pulse animation for play icon
        val infiniteTransition = rememberInfiniteTransition(label = "mini-pulse")
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue  = 0.3f,
            targetValue   = 0.7f,
            animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
            label         = "glow-alpha"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX > 150f) playerManager.closePlayer()
                            offsetX = 0f
                        },
                        onHorizontalDrag = { _, dx -> offsetX += dx }
                    )
                }
        ) {
            // Glow backdrop
            if (isDark && isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    accentGradient.first().copy(alpha = glowAlpha * 0.2f),
                                    Color.Transparent
                                )
                            ),
                            RoundedCornerShape(24.dp)
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(24.dp),
                        spotColor = accentGradient.first().copy(alpha = 0.3f))
                    .background(bgColor, RoundedCornerShape(24.dp))
                    .border(
                        0.7.dp,
                        Brush.horizontalGradient(listOf(
                            accentGradient.first().copy(alpha = 0.5f),
                            accentGradient.last().copy(alpha = 0.3f)
                        )),
                        RoundedCornerShape(24.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                        onClick           = {
                            if (isVideo) playerManager.setVideoExpanded(true)
                            else playerManager.setAudioSheetOpen(true)
                        }
                    )
            ) {
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Artwork
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .shadow(8.dp, RoundedCornerShape(14.dp), spotColor = accentGradient.first().copy(alpha = 0.4f))
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(accentGradient)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isVideo) Icons.Rounded.PlayCircle else Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint       = Color.White.copy(alpha = 0.8f),
                            modifier   = Modifier.size(24.dp)
                        )
                        val imageModel = remember(item.thumbnailUrl, item.targetPath) {
                            when {
                                item.thumbnailUrl.isNotBlank() -> item.thumbnailUrl
                                item.targetPath.isNotBlank()   -> java.io.File(item.targetPath)
                                else -> null
                            }
                        }
                        if (imageModel != null) {
                            val request = coil.request.ImageRequest.Builder(context)
                                .data(imageModel)
                                .apply { if (isVideo) videoFrameMillis(1500) }
                                .crossfade(true)
                                .build()
                            AsyncImage(
                                model            = request,
                                contentDescription = item.title,
                                contentScale     = ContentScale.Crop,
                                modifier         = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text     = item.title,
                            style    = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color    = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text  = item.uploader.ifBlank { if (isVideo) "Video" else "Audio" },
                            style = MaterialTheme.typography.labelSmall,
                            color = accentGradient.first(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Controls
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(onClick = { playerManager.playPrevious() }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Rounded.SkipPrevious, "Prev", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }

                        // Play/Pause with glow
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .shadow(if (isPlaying) 10.dp else 4.dp, CircleShape,
                                    spotColor = accentGradient.first().copy(if (isPlaying) glowAlpha * 0.5f else 0f))
                                .clip(CircleShape)
                                .background(Brush.linearGradient(accentGradient))
                                .clickable { playerManager.togglePlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint       = Color(0xFF001824),
                                modifier   = Modifier.size(22.dp)
                            )
                        }

                        IconButton(onClick = { playerManager.playNext() }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Rounded.SkipNext, "Next", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }

                        IconButton(onClick = { playerManager.closePlayer() }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.Close, "Close", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Progress bar — neon gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(if (isDark) SpaceBorder else Color(0xFFE0E8FF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(Brush.horizontalGradient(accentGradient))
                    )
                }
            }
        }
    }
}
