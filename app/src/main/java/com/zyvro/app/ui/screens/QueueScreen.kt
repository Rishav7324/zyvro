package com.zyvro.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zyvro.app.data.local.DownloadStatus
import com.zyvro.app.ui.components.DownloadItemCard
import com.zyvro.app.ui.components.LiquidGlassCard
import com.zyvro.app.ui.components.ambientLiquidBackground
import com.zyvro.app.ui.components.liquidGlass
import com.zyvro.app.ui.theme.*
import com.zyvro.app.viewmodel.QueueViewModel

// ═══════════════════════════════════════════════════════════════════════════
// ZYVRO QUEUE SCREEN v4.0 — Deep Space Aura · Live Download Center
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun QueueScreen(
    viewModel: QueueViewModel = viewModel()
) {
    val activeQueue by viewModel.activeQueue.collectAsState()
    val isDark = LocalAppDark.current

    val downloadingCount = activeQueue.count { it.status == DownloadStatus.DOWNLOADING }
    val queuedCount = activeQueue.count { it.status == DownloadStatus.QUEUED }

    val infiniteTransition = rememberInfiniteTransition(label = "queue-pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseGlow"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .ambientLiquidBackground(isDark)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // ── Header ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Download Queue",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (downloadingCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NovaPrimary.copy(alpha = pulseGlow))
                        )
                    }
                }
                Text(
                    text = when {
                        activeQueue.isEmpty() -> "All tasks completed"
                        downloadingCount > 0 -> "$downloadingCount active · $queuedCount queued"
                        else -> "${activeQueue.size} tasks pending"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Turbo Engine Acceleration Status Card ───────────────────────────
        if (activeQueue.isNotEmpty()) {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = NovaPrimary.copy(alpha = 0.5f))
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(GradientCyan)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = Color(0xFF001824),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Turbo Engine Active",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NovaPrimary.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    "MULTI-THREAD",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NovaPrimary
                                )
                            }
                        }
                        Text(
                            text = "Accelerated multi-fragment downloader running",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // ── Queue List or Empty State ───────────────────────────────────────
        if (activeQueue.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 100.dp),
                contentAlignment = Alignment.Center
            ) {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(0.92f),
                    shape = RoundedCornerShape(28.dp),
                    elevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .shadow(16.dp, CircleShape, spotColor = NovaPrimary.copy(alpha = 0.3f))
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(SpaceCardHigh, SpaceGlass)))
                                .border(1.dp, NovaPrimary.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudDone,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = NovaPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "Queue is Idle",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Paste any link on the Home screen to queue high-speed downloads with live ETA.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(activeQueue, key = { it.id }) { item ->
                    DownloadItemCard(
                        download = item,
                        onCancel = { viewModel.cancelDownload(it) },
                        onDelete = { viewModel.deleteDownload(it) }
                    )
                }
            }
        }
    }
}
