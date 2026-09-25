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
import androidx.compose.material.icons.rounded.*
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
import com.zyvro.app.ui.theme.*

// ═══════════════════════════════════════════════════════════════════════════
// ZYVRO VIDEO PREVIEW CARD v4.0 — Deep Space Aura
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun VideoPreviewCard(
    videoInfo: VideoInfo,
    onConfigureDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark      = LocalAppDark.current
    val isInstagram = videoInfo.extractor.contains("instagram", ignoreCase = true)
    val isYouTube   = videoInfo.extractor.contains("youtube", ignoreCase = true)
    val isTikTok    = videoInfo.extractor.contains("tiktok", ignoreCase = true)
    val isTwitter   = videoInfo.extractor.contains("twitter", ignoreCase = true) || videoInfo.extractor.contains("x", ignoreCase = true)
    val isSoundCloud= videoInfo.extractor.contains("soundcloud", ignoreCase = true)
    val isPhoto     = videoInfo.title.contains("photo", ignoreCase = true) ||
            videoInfo.formats.any { it.extension in listOf("jpg", "jpeg", "png", "webp") }

    // Platform color
    val platformColor = when {
        isYouTube    -> YouTubeRed
        isInstagram  -> InstagramPink
        isTikTok     -> TikTokCyan
        isTwitter    -> TwitterBlue
        isSoundCloud -> SoundCloudOrange
        else         -> NovaPrimary
    }

    val platformName = when {
        isYouTube    -> "YOUTUBE"
        isInstagram  -> "INSTAGRAM"
        isTikTok     -> "TIKTOK"
        isTwitter    -> "TWITTER / X"
        isSoundCloud -> "SOUNDCLOUD"
        else         -> videoInfo.extractor.uppercase().take(12)
    }

    LiquidGlassCard(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(24.dp),
        elevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // ── Thumbnail ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isDark) SpaceGlass else Color(0xFFE8EEFF))
            ) {
                if (videoInfo.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                        model            = videoInfo.thumbnailUrl,
                        contentDescription = videoInfo.title,
                        contentScale     = ContentScale.Crop,
                        modifier         = Modifier.fillMaxSize()
                    )
                    // Gradient scrim
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.55f)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))
                            )
                    )
                } else {
                    val pulse  = rememberInfiniteTransition(label = "placeholder")
                    val alpha  = pulse.animateFloat(0.4f, 0.8f, infiniteRepeatable(tween(900), RepeatMode.Reverse), "placeholder-alpha")
                    Box(
                        Modifier.fillMaxSize().alpha(alpha.value),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isPhoto) Icons.Rounded.PhotoCamera else Icons.Rounded.Movie,
                            null,
                            modifier = Modifier.size(52.dp),
                            tint     = platformColor.copy(alpha = 0.6f)
                        )
                    }
                }

                // Platform badge (top-left) with neon glow
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .shadow(6.dp, RoundedCornerShape(8.dp), spotColor = platformColor.copy(alpha = 0.4f))
                        .clip(RoundedCornerShape(8.dp))
                        .background(platformColor)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(platformName, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                }

                // Duration / Photo badge (bottom-right)
                val durText = when {
                    videoInfo.durationSeconds > 0 -> {
                        val m = videoInfo.durationSeconds / 60; val s = videoInfo.durationSeconds % 60
                        "%02d:%02d".format(m, s)
                    }
                    isPhoto -> "PHOTO"
                    else    -> null
                }
                if (durText != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.80f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(durText, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }

                // Quality indicator (bottom-left)
                val formats    = videoInfo.formats
                val maxHeight  = formats.mapNotNull { it.resolution.removeSuffix("p").toIntOrNull() }.maxOrNull()
                val qualLabel  = when {
                    maxHeight != null && maxHeight >= 2160 -> "8K/4K"
                    maxHeight != null && maxHeight >= 1080 -> "Full HD"
                    maxHeight != null && maxHeight >= 720  -> "HD"
                    formats.isNotEmpty() -> "SD"
                    else -> null
                }
                if (qualLabel != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.horizontalGradient(listOf(NovaPrimary, NovaViolet)))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(qualLabel, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Title ──────────────────────────────────────────────────
            Text(
                videoInfo.title.ifBlank { "Media Stream" },
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis,
                color      = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(8.dp))

            // ── Meta row ───────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(20.dp).clip(CircleShape).background(platformColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Person, null, modifier = Modifier.size(12.dp), tint = platformColor)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        videoInfo.uploader.ifBlank { "Creator" },
                        style    = MaterialTheme.typography.bodySmall,
                        color    = platformColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (videoInfo.viewCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Rounded.Visibility, null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${compactNumber(videoInfo.viewCount)} views", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Download Button ────────────────────────────────────────
            NovaButton(
                onClick  = onConfigureDownload,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors   = listOf(NovaPrimary, NovaPrimaryDeep),
                shape    = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.Download, null, tint = Color(0xFF001824), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Select Format & Download", fontWeight = FontWeight.Bold, color = Color(0xFF001824), fontSize = 14.sp)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Rounded.KeyboardArrowRight, null, tint = Color(0xFF001824), modifier = Modifier.size(18.dp))
            }
        }
    }
}

private fun compactNumber(value: Long): String = when {
    value >= 1_000_000_000 -> "%.1fB".format(value / 1_000_000_000f)
    value >= 1_000_000     -> "%.1fM".format(value / 1_000_000f)
    value >= 1_000         -> "%.1fK".format(value / 1_000f)
    else                   -> value.toString()
}
