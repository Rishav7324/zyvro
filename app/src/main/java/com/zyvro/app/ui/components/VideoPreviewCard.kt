package com.zyvro.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zyvro.app.engine.VideoInfo
import com.zyvro.app.ui.theme.InstagramPink
import com.zyvro.app.ui.theme.LocalAppDark
import com.zyvro.app.ui.theme.NovaCyan
import com.zyvro.app.ui.theme.NovaCyanDeep
import com.zyvro.app.ui.theme.YouTubeRed

@Composable
fun VideoPreviewCard(
    videoInfo: VideoInfo,
    onConfigureDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppDark.current
    val isInstagram = videoInfo.extractor.contains("instagram", ignoreCase = true)
    val isYouTube = videoInfo.extractor.contains("youtube", ignoreCase = true)
    val isPhoto = videoInfo.title.contains("photo", ignoreCase = true) ||
            videoInfo.formats.any { it.extension in listOf("jpg", "jpeg", "png", "webp") }

    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isDark) Color(0xFF102131) else Color(0xFFE2F3F4))
            ) {
                if (videoInfo.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                        model = videoInfo.thumbnailUrl,
                        contentDescription = videoInfo.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(180.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                )
                            )
                    )
                } else {
                    val pulse = rememberInfiniteTransition(label = "preview-placeholder")
                    val alpha = pulse.animateFloat(
                        initialValue = 0.45f,
                        targetValue = 0.8f,
                        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
                        label = "placeholder-alpha"
                    )
                    Box(
                        modifier = Modifier.fillMaxWidth().height(180.dp).alpha(alpha.value),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPhoto) Icons.Rounded.PhotoCamera else Icons.Rounded.Movie,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = if (isDark) NovaCyan else NovaCyanDeep
                        )
                    }
                }

                // Platform Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                isInstagram -> InstagramPink.copy(alpha = 0.9f)
                                isYouTube -> YouTubeRed.copy(alpha = 0.9f)
                                else -> Color.Black.copy(alpha = 0.75f)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isInstagram) "INSTAGRAM" else if (isYouTube) "YOUTUBE" else videoInfo.extractor.uppercase(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Duration or Photo Badge
                if (videoInfo.durationSeconds > 0) {
                    val minutes = videoInfo.durationSeconds / 60
                    val seconds = videoInfo.durationSeconds % 60
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.85f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (isPhoto) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.85f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "PHOTO",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = videoInfo.title.ifBlank { "Media Stream" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isDark) NovaCyan else NovaCyanDeep
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        videoInfo.uploader.ifBlank { "Creator" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (videoInfo.viewCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${compactNumber(videoInfo.viewCount)} views",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onConfigureDownload,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .shadow(6.dp, RoundedCornerShape(16.dp), spotColor = NovaCyanDeep),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NovaCyanDeep,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Format & Quality", fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
            }
        }
    }
}

private fun compactNumber(value: Long): String = when {
    value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000f)
    value >= 1_000 -> String.format("%.1fK", value / 1_000f)
    else -> value.toString()
}
