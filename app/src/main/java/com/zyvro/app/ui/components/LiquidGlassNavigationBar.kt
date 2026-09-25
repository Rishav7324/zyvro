package com.zyvro.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zyvro.app.ui.theme.*

// ═══════════════════════════════════════════════════════════════════════════
// ZYVRO NAV BAR v4.0 — Deep Space Aura Floating Navigation
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun LiquidGlassNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppDark.current

    val items = listOf(
        GlassNavItem("home",     "Home",    Icons.Rounded.Home),
        GlassNavItem("browser",  "Browse",  Icons.Rounded.Language),
        GlassNavItem("queue",    "Queue",   Icons.Rounded.CloudDownload),
        GlassNavItem("library",  "Library", Icons.Rounded.LibraryMusic),
        GlassNavItem("settings", "More",    Icons.Rounded.Tune)
    )

    val shape = RoundedCornerShape(28.dp)

    // Animated neon glow gradient border
    val borderBrush = if (isDark) {
        Brush.horizontalGradient(
            listOf(
                NovaPrimary.copy(alpha = 0.5f),
                NovaViolet.copy(alpha = 0.4f),
                NovaRose.copy(alpha = 0.3f)
            )
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                Color(0xFFB0C8FF).copy(alpha = 0.8f),
                Color(0xFFD4B8FF).copy(alpha = 0.6f)
            )
        )
    }

    val bgColor = if (isDark) {
        SpaceCard.copy(alpha = 0.92f)
    } else {
        Color.White.copy(alpha = 0.95f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Glow backdrop
        if (isDark) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                NovaPrimary.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        ),
                        shape
                    )
                    .blur(20.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(16.dp, shape, ambientColor = if (isDark) NovaPrimary.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.05f))
                .background(bgColor, shape)
                .border(1.dp, borderBrush, shape)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                AuraTabButton(
                    item     = item,
                    selected = currentRoute == item.route,
                    onClick  = { onNavigate(item.route) }
                )
            }
        }
    }
}

@Composable
private fun RowScope.AuraTabButton(
    item: GlassNavItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val isDark = LocalAppDark.current

    val scale by animateFloatAsState(
        targetValue   = if (selected) 1.0f else 0.96f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "tab-scale-${item.route}"
    )

    val iconTint by animateColorAsState(
        targetValue = if (selected) {
            if (isDark) NovaPrimary else MaterialTheme.colorScheme.primary
        } else {
            if (isDark) DarkTextSub else LightTextSub
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label         = "tab-tint-${item.route}"
    )

    val labelColor by animateColorAsState(
        targetValue = if (selected) {
            if (isDark) NovaPrimary else MaterialTheme.colorScheme.primary
        } else {
            if (isDark) DarkTextSub.copy(alpha = 0.6f) else LightTextSub.copy(alpha = 0.7f)
        },
        label = "tab-label-${item.route}"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .weight(1f)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Glow halo for selected
            if (selected && isDark) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(NovaPrimary.copy(alpha = 0.25f), Color.Transparent)
                            ),
                            CircleShape
                        )
                )
            }

            // Pill indicator for selected
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(width = 28.dp, height = 3.dp)
                        .align(Alignment.BottomCenter)
                        .offset(y = 12.dp)
                        .background(
                            Brush.horizontalGradient(listOf(NovaPrimary, NovaViolet)),
                            RoundedCornerShape(2.dp)
                        )
                )
            }

            Icon(
                imageVector     = item.icon,
                contentDescription = item.label,
                tint            = iconTint,
                modifier        = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text  = item.label,
            color = labelColor,
            fontSize = 9.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

private data class GlassNavItem(val route: String, val label: String, val icon: ImageVector)
