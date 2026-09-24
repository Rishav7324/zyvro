package com.zyvro.app.ui.motion

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically

/**
 * Zyvro Motion 2.0 — Minimal iOS-clean presets.
 * Every screen must reuse these instead of inventing its own tween numbers,
 * so navigation, sheets and lists feel like one app.
 */
object NovaMotion {
    const val FAST = 160
    const val MEDIUM = 220
    const val SLOW = 320

    val GentleSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    val TactileSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    fun <T> fastTween() = tween<T>(FAST, easing = FastOutSlowInEasing)
    fun <T> mediumTween() = tween<T>(MEDIUM, easing = FastOutSlowInEasing)
    fun <T> slowTween() = tween<T>(SLOW, easing = FastOutSlowInEasing)

    // NavHost presets
    val NavEnter = fadeIn(mediumTween()) + slideInHorizontally(mediumTween()) { it / 8 }
    val NavExit = fadeOut(fastTween()) + slideOutHorizontally(fastTween()) { -it / 8 }
    val NavPopEnter = fadeIn(mediumTween()) + slideInHorizontally(mediumTween()) { -it / 8 }
    val NavPopExit = fadeOut(fastTween()) + slideOutHorizontally(fastTween()) { it / 8 }

    // Cards / sheets
    val CardEnter = fadeIn(mediumTween()) + slideInVertically(mediumTween()) { it / 10 }
    val CardExit = fadeOut(fastTween()) + slideOutVertically(fastTween()) { it / 10 }
}
