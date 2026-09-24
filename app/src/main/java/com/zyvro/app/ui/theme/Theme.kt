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

/** Retro-style accent choices. TEAL matches the Zyvro icon. */
object ZyvroAccents {
    const val TEAL = "TEAL"
    const val BLUE = "BLUE"
    const val PURPLE = "PURPLE"
    const val GREEN = "GREEN"
    const val ORANGE = "ORANGE"
    const val PINK = "PINK"

    val all = listOf(TEAL, BLUE, PURPLE, GREEN, ORANGE, PINK)

    /** Primary swatch per accent: light + dark variants. */
    fun primary(accent: String, dark: Boolean): Color = when (accent.uppercase()) {
        BLUE -> if (dark) Color(0xFF0A84FF) else Color(0xFF007AFF)
        PURPLE -> if (dark) Color(0xFFA78BFA) else Color(0xFF7C3AED)
        GREEN -> if (dark) Color(0xFF30D158) else Color(0xFF16A34A)
        ORANGE -> if (dark) Color(0xFFFF9F0A) else Color(0xFFEA580C)
        PINK -> if (dark) Color(0xFFF472B6) else Color(0xFFDB2777)
        else -> if (dark) NovaPrimaryDark else NovaAquaDeep // TEAL
    }
}

private fun zyvroLightScheme(accent: Color) = lightColorScheme(
    primary = accent, onPrimary = Color.White,
    primaryContainer = NovaAquaSoft, onPrimaryContainer = NovaInk,
    secondary = NovaAquaSoft, onSecondary = NovaInk, tertiary = NovaAqua,
    background = IOSGroupedLight, onBackground = IOSTitleLight,
    surface = IOSCardLight, onSurface = IOSTitleLight,
    surfaceVariant = IOSGroupedLight, onSurfaceVariant = IOSCaptionLight,
    outline = IOSSeparatorLight
)

private fun zyvroDarkScheme(accent: Color, amoled: Boolean) = darkColorScheme(
    primary = accent, onPrimary = Color.Black,
    primaryContainer = Color(0xFF234344), onPrimaryContainer = IOSTitleDark,
    secondary = NovaSecondary, onSecondary = Color.Black, tertiary = NovaAqua,
    background = if (amoled) Color.Black else IOSGroupedDark,
    onBackground = IOSTitleDark,
    surface = if (amoled) Color(0xFF0A0A0A) else IOSCardDark,
    onSurface = IOSTitleDark,
    surfaceVariant = if (amoled) Color(0xFF161616) else IOSCardDarkElevated,
    onSurfaceVariant = IOSCaptionDark,
    outline = IOSSeparatorDark
)

/** True app-applied darkness (follows theme setting, NOT just system).
 * Glass/cards must read this instead of isSystemInDarkTheme(), otherwise an
 * explicit Dark/Black choice on a light system (or vice versa) mismatches. */
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
        else -> zyvroLightScheme(accentColor)
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            // Edge-to-edge: transparent system bars, content draws behind them.
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = NovaShapes,
        content = {
            androidx.compose.runtime.CompositionLocalProvider(
                LocalAppDark provides darkTheme,
                content = content
            )
        }
    )
}

/** Resolve stored theme mode (SYSTEM/LIGHT/DARK/BLACK) to (dark, amoled). */
fun resolveThemeMode(mode: String, systemDark: Boolean): Pair<Boolean, Boolean> =
    when (mode.uppercase()) {
        "LIGHT" -> false to false
        "DARK" -> true to false
        "BLACK" -> true to true
        else -> systemDark to false
    }
