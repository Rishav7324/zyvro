package com.zyvro.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zyvro.app.player.MediaPlayerManager
import com.zyvro.app.ui.browser.WebBrowserScreen
import com.zyvro.app.ui.components.LiquidGlassNavigationBar
import com.zyvro.app.ui.player.AudioPlayerSheet
import com.zyvro.app.ui.player.MiniPlayerBar
import com.zyvro.app.ui.player.VideoPlayerView
import com.zyvro.app.ui.motion.NovaMotion
import com.zyvro.app.ui.screens.HomeScreen
import com.zyvro.app.ui.screens.LegalScreen
import com.zyvro.app.ui.screens.LibraryScreen
import com.zyvro.app.ui.screens.PermissionScreen
import com.zyvro.app.ui.screens.QueueScreen
import com.zyvro.app.ui.screens.SettingsScreen
import com.zyvro.app.viewmodel.HomeViewModel

sealed class Screen(val route: String, val title: String) {
    object Permissions : Screen("permissions", "Permissions")
    object Home : Screen("home", "Home")
    object Browser : Screen("browser", "Browser")
    object Queue : Screen("queue", "Queue")
    object Library : Screen("library", "Library")
    object Settings : Screen("settings", "Settings")
    object Legal : Screen("legal", "Legal")
}

val navItems = listOf(Screen.Home, Screen.Browser, Screen.Queue, Screen.Library, Screen.Settings)

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    sharedUrl: String? = null,
    startDestination: String = Screen.Permissions.route,
    widgetTab: String? = null,
    onWidgetTabConsumed: () -> Unit = {}
) {
    val homeViewModel: HomeViewModel = viewModel()
    val context = androidx.compose.ui.platform.LocalContext.current
    val entry by navController.currentBackStackEntryAsState()
    val currentDestination = entry?.destination?.route
    val playerManager = MediaPlayerManager.getInstance(context)
    val currentMedia by playerManager.currentMedia.collectAsState()
    val isVideoExpanded by playerManager.isVideoExpanded.collectAsState()
    val isAudioSheetOpen by playerManager.isAudioSheetOpen.collectAsState()

    val isMainTab = currentDestination in listOf(
        Screen.Home.route,
        Screen.Browser.route,
        Screen.Queue.route,
        Screen.Library.route,
        Screen.Settings.route
    )

    androidx.compose.runtime.LaunchedEffect(sharedUrl) {
        if (!sharedUrl.isNullOrBlank()) {
            homeViewModel.onUrlChanged(sharedUrl)
            homeViewModel.parseUrl(sharedUrl)
            if (currentDestination != Screen.Home.route) {
                navController.navigate(Screen.Home.route) {
                    launchSingleTop = true
                }
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(widgetTab) {
        val tab = widgetTab?.lowercase().orEmpty()
        val route = when (tab) {
            "queue" -> Screen.Queue.route
            "library" -> Screen.Library.route
            "browser" -> Screen.Browser.route
            "settings" -> Screen.Settings.route
            "home" -> Screen.Home.route
            else -> null
        }
        if (route != null && currentDestination != route) {
            navController.navigate(route) { launchSingleTop = true }
            onWidgetTabConsumed()
        } else if (route != null) {
            onWidgetTabConsumed()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { NovaMotion.NavEnter },
            exitTransition = { NovaMotion.NavExit },
            popEnterTransition = { NovaMotion.NavPopEnter },
            popExitTransition = { NovaMotion.NavPopExit }
        ) {
            composable(Screen.Permissions.route) {
                PermissionScreen(
                    onPermissionsCompleted = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Permissions.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    homeViewModel,
                    onNavigateToQueue = { navController.navigate(Screen.Queue.route) },
                    onNavigateToBrowser = { url ->
                        if (url.isNotBlank()) {
                            homeViewModel.onUrlChanged(url)
                            homeViewModel.parseUrl(url)
                        }
                        navController.navigate(Screen.Browser.route)
                    }
                )
            }

            composable(Screen.Browser.route) {
                WebBrowserScreen(
                    onDownloadUrl = { url ->
                        homeViewModel.onUrlChanged(url)
                        homeViewModel.parseUrl(url)
                        navController.navigate(Screen.Home.route) { launchSingleTop = true }
                    }
                )
            }

            composable(Screen.Queue.route) {
                QueueScreen()
            }

            composable(Screen.Library.route) {
                LibraryScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onOpenLegal = { navController.navigate(Screen.Legal.route) }
                )
            }

            composable(Screen.Legal.route) {
                LegalScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // Bottom Navigation Bar & Mini Player
        if (isMainTab) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .wrapContentHeight(),
                verticalArrangement = Arrangement.Bottom
            ) {
                AnimatedVisibility(
                    visible = currentMedia != null && !isVideoExpanded,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    MiniPlayerBar()
                }

                LiquidGlassNavigationBar(
                    currentRoute = currentDestination,
                    onNavigate = { route ->
                        if (currentDestination != route) {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    // iOS tab bar: flush bottom edge, insets handled inside the bar.
                    modifier = Modifier
                )
            }
        }

        // Expanded Video Player
        AnimatedVisibility(
            visible = isVideoExpanded,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(200)),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(200))
        ) {
            VideoPlayerView(onClose = { playerManager.setVideoExpanded(false) })
        }

        // Audio Player Sheet
        AnimatedVisibility(
            visible = isAudioSheetOpen,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200))
        ) {
            AudioPlayerSheet(onDismiss = { playerManager.setAudioSheetOpen(false) })
        }
    }
}
