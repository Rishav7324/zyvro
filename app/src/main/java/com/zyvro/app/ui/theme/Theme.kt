package com.zyvro.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ═══════════════════════════════════════════════════════════════════════════
// ZYVRO THEME v4.0 — Deep Space Aura
// ═══════════════════════════════════════════════════════════════════════════

/** Accent color options shown in Settings → Appearance */
object ZyvroAccents {
    const val TEAL   = "TEAL"
    const val BLUE   = "BLUE"
    const val PURPLE = "PURPLE"
    const val GREEN  = "GREEN"
    const val ORANGE = "ORANGE"
    const val PINK   = "PINK"

    val all = listOf(TEAL, BLUE, PURPLE, GREEN, ORANGE, PINK)

    fun primary(accent: String, dark: Boolean): Color = when (accent.uppercase()) {
        BLUE   -> if (dark) Color(0xFF40B4FF) else Color(0xFF007AFF)
        PURPLE -> if (dark) Color(0xFFA78BFA) else Color(0xFF7C3AED)
        GREEN  -> if (dark) Color(0xFF00E676) else Color(0xFF16A34A)
        ORANGE -> if (dark) Color(0xFFFF9F0A) else Color(0xFFEA580C)
        PINK   -> if (dark) Color(0xFFFF2D78) else Color(0xFFDB2777)
        else   -> if (dark) NovaPrimary else NovaPrimaryDeep // TEAL/CYAN
    }
}

private fun zyvroLightScheme(accent: Color) = lightColorScheme(
    primary             = accent,
    onPrimary           = Color.White,
    primaryContainer    = Color(0xFFCCEEFF),
    onPrimaryContainer  = Color(0xFF001A28),
    secondary           = NovaViolet,
    onSecondary         = Color.White,
    secondaryContainer  = Color(0xFFEDE9FF),
    onSecondaryContainer= Color(0xFF1A0050),
    tertiary            = NovaRose,
    onTertiary          = Color.White,
    background          = LightBg,
    onBackground        = LightText,
    surface             = LightCard,
    onSurface           = LightText,
    surfaceVariant      = LightCardAlt,
    onSurfaceVariant    = LightTextSub,
    outline             = LightBorder,
    error               = AccentRed,
    onError             = Color.White
)

private fun zyvroDarkScheme(accent: Color, amoled: Boolean) = darkColorScheme(
    primary             = accent,
    onPrimary           = Color(0xFF001A28),
    primaryContainer    = Color(0xFF003850),
    onPrimaryContainer  = Color(0xFFB3EEFF),
    secondary           = NovaViolet,
    onSecondary         = Color.White,
    secondaryContainer  = Color(0xFF2D1B69),
    onSecondaryContainer= Color(0xFFDDD6FE),
    tertiary            = NovaRose,
    onTertiary          = Color.White,
    background          = if (amoled) Color.Black else SpaceBlack,
    onBackground        = DarkText,
    surface             = if (amoled) Color(0xFF060912) else SpaceCard,
    onSurface           = DarkText,
    surfaceVariant      = if (amoled) Color(0xFF0D1020) else SpaceCardHigh,
    onSurfaceVariant    = DarkTextSub,
    outline             = SpaceBorder,
    error               = AccentRed,
    onError             = Color.White
)

/** True app-applied darkness (follows theme setting, NOT just system) */
val LocalAppDark = compositionLocalOf { false }

@Composable
fun YtDlpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    amoled: Boolean = false,
    accent: String = ZyvroAccents.TEAL,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val accentColor = ZyvroAccents.primary(accent, darkTheme)
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> zyvroDarkScheme(accentColor, amoled)
        else      -> zyvroLightScheme(accentColor)
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        shapes      = NovaShapes,
        content = {
            androidx.compose.runtime.CompositionLocalProvider(
                LocalAppDark provides darkTheme,
                content = content
            )
        }
    )
}

/** Resolve stored theme mode to (dark, amoled) */
fun resolveThemeMode(mode: String, systemDark: Boolean): Pair<Boolean, Boolean> =
    when (mode.uppercase()) {
        "LIGHT"  -> false to false
        "DARK"   -> true to false
        "BLACK"  -> true to true
        else     -> systemDark to false
    }
