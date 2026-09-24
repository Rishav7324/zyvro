package com.zyvro.app.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.app.PictureInPictureParams
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.zyvro.app.YtDlpApp
import com.zyvro.app.player.MediaPlayerManager
import com.zyvro.app.ui.navigation.AppNavigation
import com.zyvro.app.ui.theme.YtDlpTheme
import com.zyvro.app.ui.theme.resolveThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Main launcher activity. Feature-specific permissions are requested only
 * when required, keeping first launch reliable across Android versions.
 */
class MainActivity : ComponentActivity() {
    private var sharedUrlState: String? = null
    private var widgetTabState: String? = null
    private var startRouteState by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Single system splash: hold it only until onboarding state is known,
        // then start directly on permissions/home (no second Compose splash).
        var ready = false
        splash.setKeepOnScreenCondition { !ready }
        lifecycleScope.launch {
            val done = runCatching {
                (application as YtDlpApp).preferences.onboardingCompleted.first()
            }.getOrDefault(false)
            startRouteState = if (done) "home" else "permissions"
            ready = true
        }
        enableEdgeToEdge()
        handleIncomingIntent(intent)
        setContent {
            val prefs = (application as YtDlpApp).preferences
            val themeMode by prefs.darkThemeMode.collectAsState(initial = "SYSTEM")
            val accent by prefs.accentColor.collectAsState(initial = "TEAL")
            val systemDark = isSystemInDarkTheme()
            val (dark, amoled) = resolveThemeMode(themeMode, systemDark)
            YtDlpTheme(darkTheme = dark, amoled = amoled, accent = accent) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    startRouteState?.let { start ->
                        AppNavigation(
                            sharedUrl = sharedUrlState,
                            startDestination = start,
                            widgetTab = widgetTabState,
                            onWidgetTabConsumed = { widgetTabState = null }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    /** NextPlayer-style continuity: home press while fullscreen video plays -> PiP. */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        runCatching {
            if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) return
            val manager = MediaPlayerManager.getInstance(this)
            if (manager.isVideoExpanded.value && manager.isPlaying.value) {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            }
        }
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        // Widget deep-links: zyvro_tab = home/queue/library/browser/settings
        intent.getStringExtra(EXTRA_WIDGET_TAB)?.takeIf { it.isNotBlank() }?.let {
            widgetTabState = it
        }
        if (intent.action != Intent.ACTION_SEND || intent.type != "text/plain") return
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()
        if (!text.isNullOrBlank()) sharedUrlState = extractUrl(text) ?: text
    }

    companion object {
        const val EXTRA_WIDGET_TAB = "zyvro_tab"
    }

    private fun extractUrl(text: String): String? =
        Regex("""https?://[^\s]+""").find(text)?.value
}
