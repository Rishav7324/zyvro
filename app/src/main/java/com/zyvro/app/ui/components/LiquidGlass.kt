package com.zyvro.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zyvro.app.ui.theme.LocalAppDark
import com.zyvro.app.ui.theme.NovaPrimary
import com.zyvro.app.ui.theme.NovaPrimaryDeep
import com.zyvro.app.ui.theme.NovaViolet
import com.zyvro.app.ui.theme.SpaceBorder
import com.zyvro.app.ui.theme.SpaceCard
import com.zyvro.app.ui.theme.SpaceCardHigh
import com.zyvro.app.ui.theme.SpaceGlass

// ═══════════════════════════════════════════════════════════════════════════
// ZYVRO LIQUID GLASS v4.0 — Deep Space Aura Edition
// ═══════════════════════════════════════════════════════════════════════════

val AppleSpringSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness    = Spring.StiffnessMediumLow
)

val AppleSmoothSpringSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness    = Spring.StiffnessMedium
)

/**
 * Deep Space background with subtle animated aurora gradient.
 */
@Composable
fun Modifier.ambientLiquidBackground(
    isDark: Boolean = LocalAppDark.current
): Modifier {
    return this.background(MaterialTheme.colorScheme.background)
}

/**
 * Deep Space Glass Modifier — frosted aura surface with neon border shimmer.
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(20.dp),
    borderAlpha: Float = 0.30f,
    elevation: Dp = 4.dp,
    tintColor: Color? = null,
    isDark: Boolean = LocalAppDark.current
): Modifier {
    val backgroundColor = if (isDark) {
        tintColor?.copy(alpha = 0.18f) ?: SpaceCard.copy(alpha = 0.95f)
    } else {
        tintColor?.copy(alpha = 0.08f) ?: Color.White.copy(alpha = 0.97f)
    }

    val borderColor = if (isDark) {
        tintColor?.copy(alpha = borderAlpha * 1.4f)
            ?: NovaPrimary.copy(alpha = borderAlpha * 0.5f)
    } else {
        Color(0xFFB0C8FF).copy(alpha = 0.6f)
    }

    val glowColor = tintColor?.copy(alpha = 0.12f) ?: NovaPrimary.copy(alpha = 0.08f)

    return this
        .shadow(
            elevation   = elevation,
            shape       = shape,
            ambientColor = if (isDark) glowColor else Color.Black.copy(alpha = 0.05f),
            spotColor   = if (isDark) glowColor else Color.Black.copy(alpha = 0.06f)
        )
        .background(backgroundColor, shape)
        .border(0.6.dp, borderColor, shape)
}

/**
 * Interactive Deep Space glass card with tactile press animation.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    tintColor: Color? = null,
    borderAlpha: Float = 0.30f,
    elevation: Dp = 4.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue    = if (isPressed && onClick != null) 0.96f else 1.0f,
        animationSpec  = AppleSpringSpec,
        label          = "card-scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .liquidGlass(shape = shape, borderAlpha = borderAlpha, elevation = elevation, tintColor = tintColor)
            .clip(shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication        = null,
                        onClick           = onClick
                    )
                } else Modifier
            ),
        content = content
    )
}

/**
 * Neon Pill / Chip — aurora gradient when selected.
 */
@Composable
fun LiquidGlassPill(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    selectedColor: Color = MaterialTheme.colorScheme.primary,
    elevation: Dp = 4.dp,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (isPressed && onClick != null) 0.90f else 1.0f,
        animationSpec = AppleSpringSpec,
        label         = "pill-scale"
    )

    val shape = CircleShape
    val isDark = LocalAppDark.current

    val bgBrush = if (isSelected) {
        Brush.horizontalGradient(
            listOf(selectedColor, NovaViolet)
        )
    } else {
        Brush.verticalGradient(
            if (isDark) {
                listOf(SpaceCardHigh, SpaceGlass)
            } else {
                listOf(Color.White, Color(0xFFF0F4FF))
            }
        )
    }

    val borderBrush = if (isSelected) {
        Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.2f)))
    } else {
        Brush.linearGradient(
            if (isDark) {
                listOf(NovaPrimary.copy(alpha = 0.4f), NovaViolet.copy(alpha = 0.2f))
            } else {
                listOf(Color(0xFFB0C8FF), Color(0xFFD4DCFF))
            }
        )
    }

    Row(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation  = elevation,
                shape      = shape,
                spotColor  = if (isSelected) selectedColor.copy(alpha = 0.4f) else Color.Transparent
            )
            .background(bgBrush, shape)
            .border(1.dp, borderBrush, shape)
            .clip(shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication        = null,
                        onClick           = onClick
                    )
                } else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically,
        content               = content
    )
}

/**
 * Neon Gradient button with glow effect.
 */
@Composable
fun NovaButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(NovaPrimary, NovaPrimaryDeep),
    shape: Shape = RoundedCornerShape(16.dp),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label         = "nova-btn-scale"
    )

    Row(
        modifier = modifier
            .scale(scale)
            .shadow(12.dp, shape, spotColor = colors.first().copy(alpha = 0.5f))
            .background(Brush.horizontalGradient(colors), shape)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            )
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically,
        content               = content
    )
}
