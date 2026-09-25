package com.zyvro.app.ui.motion

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically

/**
 * Zyvro Motion v4.0 — Deep Space Aura Animation System.
 * Fluid, expressive animations inspired by iOS 18 spring physics.
 */
object NovaMotion {
    const val FAST    = 150
    const val MEDIUM  = 260
    const val SLOW    = 380
    const val XSLOW   = 500

    // Spring specs
    val GentleSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness    = Spring.StiffnessMediumLow
    )
    val TactileSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness    = Spring.StiffnessMediumLow
    )
    val BouncySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioHighBouncy,
        stiffness    = Spring.StiffnessMedium
    )

    fun <T> fastTween()   = tween<T>(FAST,   easing = FastOutSlowInEasing)
    fun <T> mediumTween() = tween<T>(MEDIUM, easing = FastOutSlowInEasing)
    fun <T> slowTween()   = tween<T>(SLOW,   easing = FastOutSlowInEasing)
    fun <T> xslowTween()  = tween<T>(XSLOW,  easing = FastOutSlowInEasing)

    // ── Navigation transitions ──────────────────────────────────────────
    val NavEnter    = fadeIn(mediumTween()) + slideInHorizontally(mediumTween()) { it / 6 }
    val NavExit     = fadeOut(fastTween()) + slideOutHorizontally(fastTween()) { -it / 6 }
    val NavPopEnter = fadeIn(mediumTween()) + slideInHorizontally(mediumTween()) { -it / 6 }
    val NavPopExit  = fadeOut(fastTween()) + slideOutHorizontally(fastTween()) { it / 6 }

    // ── Cards & sheets ──────────────────────────────────────────────────
    val CardEnter = fadeIn(mediumTween()) + slideInVertically(mediumTween()) { it / 8 }
    val CardExit  = fadeOut(fastTween()) + slideOutVertically(fastTween()) { it / 8 }

    // ── Scale pop (for modals, players) ────────────────────────────────
    val PopIn  = fadeIn(mediumTween()) + scaleIn(mediumTween(), initialScale = 0.88f)
    val PopOut = fadeOut(fastTween()) + scaleOut(fastTween(), targetScale = 0.88f)

    // ── Bottom sheet slide ──────────────────────────────────────────────
    val SheetEnter = slideInVertically(slowTween()) { it }
    val SheetExit  = slideOutVertically(mediumTween()) { it }
}
