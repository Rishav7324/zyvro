package com.zyvro.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import com.zyvro.app.ui.theme.LocalAppDark
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
import com.zyvro.app.ui.theme.NovaCyan
import com.zyvro.app.ui.theme.NovaCyanDeep

// Apple-inspired tactile spring specs
val AppleSpringSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMediumLow
)

val AppleSmoothSpringSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium
)

/**
 * Zyvro screen backdrop.
 * iOS-style flat grouped background so cards read as clean inset groups.
 */
@Composable
fun Modifier.ambientLiquidBackground(
    isDark: Boolean = LocalAppDark.current
): Modifier {
    return this.background(MaterialTheme.colorScheme.background)
}

/**
 * Zyvro clean-glass Modifier (2.0 minimal).
 * Flat iOS-grouped surface: near-solid fill, hairline border, restrained shadow.
 * Keeps the same API so all screens keep compiling.
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(22.dp),
    borderAlpha: Float = 0.35f,
    elevation: Dp = 2.dp,
    tintColor: Color? = null,
    isDark: Boolean = LocalAppDark.current
): Modifier {
    val backgroundColor = if (isDark) {
        tintColor?.copy(alpha = 0.22f) ?: Color(0xFF1C1C1E).copy(alpha = 0.96f)
    } else {
        tintColor?.copy(alpha = 0.10f) ?: Color.White.copy(alpha = 0.98f)
    }

    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.10f * (1f + borderAlpha))
    } else {
        Color(0xFFC6C6C8).copy(alpha = 0.55f)
    }

    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = if (isDark) 0.4f else 0.08f),
            spotColor = Color.Black.copy(alpha = if (isDark) 0.4f else 0.08f)
        )
        .background(backgroundColor, shape)
        .border(0.5.dp, borderColor, shape)
}

/**
 * Interactive clean-glass Card with a subtle press scale.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    tintColor: Color? = null,
    borderAlpha: Float = 0.35f,
    elevation: Dp = 2.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.965f else 1.0f,
        animationSpec = AppleSpringSpec,
        label = "liquid-glass-card-scale"
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
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            ),
        content = content
    )
}

/**
 * Capsule / Pill for tags, chips, and quick action triggers.
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
        targetValue = if (isPressed && onClick != null) 0.92f else 1.0f,
        animationSpec = AppleSpringSpec,
        label = "pill-scale"
    )

    val shape = CircleShape
    val isDark = LocalAppDark.current

    val bgBrush = if (isSelected) {
        Brush.horizontalGradient(
            listOf(
                selectedColor.copy(alpha = if (isDark) 0.90f else 0.95f),
                if (isDark) NovaCyanDeep else NovaCyan
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                if (isDark) Color(0xFF162B3D).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.92f),
                if (isDark) Color(0xFF0F1E2C).copy(alpha = 0.85f) else Color(0xFFE5F1FA).copy(alpha = 0.70f)
            )
        )
    }

    val borderBrush = Brush.linearGradient(
        listOf(
            if (isSelected) Color.White.copy(alpha = 0.90f) else if (isDark) Color.White.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.98f),
            if (isSelected) Color.White.copy(alpha = 0.40f) else if (isDark) NovaCyan.copy(alpha = 0.30f) else Color(0xFF00B4D8).copy(alpha = 0.45f)
        )
    )

    Row(
        modifier = modifier
            .scale(scale)
            .shadow(elevation = elevation, shape = shape, spotColor = if (isSelected) selectedColor.copy(alpha = 0.35f) else Color.Transparent)
            .background(bgBrush, shape)
            .border(1.2.dp, borderBrush, shape)
            .clip(shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
