package com.zyvro.app.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zyvro.app.ui.components.ambientLiquidBackground
import com.zyvro.app.ui.theme.*
import com.zyvro.app.viewmodel.SettingsViewModel
import com.zyvro.app.viewmodel.UpdateState
import kotlinx.coroutines.launch

/**
 * Zyvro Settings — iOS inset-grouped style.
 * Large title, section captions, white/elevated grouped cards with hairline
 * dividers, chevron rows and smooth 14dp radii. All engine wiring unchanged.
 */
@Composable
fun SettingsScreen(
    onOpenLegal: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val engineVersion by viewModel.engineVersion.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val customArguments by viewModel.customArguments.collectAsState()
    val embedSubtitles by viewModel.embedSubtitles.collectAsState()
    val useAria2 by viewModel.useAria2.collectAsState()
    val aria2Connections by viewModel.aria2Connections.collectAsState()
    val sponsorBlockEnabled by viewModel.sponsorBlockEnabled.collectAsState()
    val themeMode by viewModel.darkThemeMode.collectAsState()
    val accent by viewModel.accentColor.collectAsState()
    val cookiesContent by viewModel.cookiesContent.collectAsState()
    val ytAndroidClient by viewModel.ytAndroidClient.collectAsState()
    val speedLimit by viewModel.speedLimit.collectAsState()
    val autoResumeWifi by viewModel.autoResumeWifi.collectAsState()
    val customDownloadDir by viewModel.customDownloadDir.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()

    // cookies.txt picker (Netscape format export from a desktop browser).
    val pickCookies = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
            }.getOrNull()
            if (text.isNullOrBlank()) {
                Toast.makeText(context, "Could not read file", Toast.LENGTH_SHORT).show()
            } else if (!text.contains("Netscape HTTP Cookie File") && !text.contains("youtube.com") && !text.contains("instagram.com") && !text.contains("facebook.com")) {
                Toast.makeText(context, "Not a valid cookies.txt (needs youtube.com, instagram.com or facebook.com)", Toast.LENGTH_LONG).show()
            } else {
                viewModel.setCookiesContent(text)
                Toast.makeText(context, "Login cookies imported successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Custom folder / SD card picker
    val pickFolder = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
            val path = uri.path ?: uri.toString()
            viewModel.setCustomDownloadDir(path)
            Toast.makeText(context, "Download directory updated", Toast.LENGTH_SHORT).show()
        }
    }

    var customArgsInput by remember { mutableStateOf("") }
    var initialized by remember { mutableStateOf(false) }
    if (!initialized) {
        customArgsInput = customArguments
        initialized = true
    }

    LaunchedEffect(updateState) {
        when (val state = updateState) {
            is UpdateState.Success -> Toast.makeText(context, "yt-dlp updated to: ${state.version}", Toast.LENGTH_LONG).show()
            is UpdateState.Error -> Toast.makeText(context, "yt-dlp update failed: ${state.message}", Toast.LENGTH_LONG).show()
            else -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .ambientLiquidBackground(LocalAppDark.current)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        // iOS large title
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Engine, downloads and about",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        // ENGINE
        IOSSection(header = "YT-DLP ENGINE", footer = "The downloader core updates itself from GitHub releases.") {
            IOSValueRow(
                icon = Icons.Rounded.CloudDownload,
                title = "Engine version",
                value = engineVersion
            )
            IOSDivider()
            // Full-width iOS blue update button row
            Button(
                onClick = { viewModel.updateYtDlp() },
                enabled = updateState !is UpdateState.Checking,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NovaAquaDeep,
                    contentColor = Color.White
                )
            ) {
                if (updateState is UpdateState.Checking) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.width(10.dp))
                    Text("Checking GitHub releases…", fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Check for engine update", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // PLATFORM LOGIN (YouTube, Instagram & Facebook)
        IOSSection(
            header = "PLATFORM LOGIN (YouTube, Instagram & Facebook)",
            footer = "Export cookies.txt from a desktop browser (devtools extension). Unlocks HD on YouTube and gated Instagram/Facebook reels. Never uploaded — stays on this device."
        ) {
            IOSValueRow(
                icon = Icons.Rounded.Cookie,
                title = "Login cookies",
                value = if (cookiesContent.isBlank()) "Not set" else "${cookiesContent.lines().size} lines"
            )
            IOSDivider(startIndent = 58.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { pickCookies.launch("text/plain") },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (cookiesContent.isBlank()) "Import cookies.txt" else "Replace cookies.txt",
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (cookiesContent.isNotBlank()) {
                    OutlinedButton(
                        onClick = { viewModel.setCookiesContent("") },
                        modifier = Modifier.height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Remove")
                    }
                }
            }
            IOSDivider(startIndent = 16.dp)
            IOSSwitchRow(
                icon = Icons.Rounded.Science,
                title = "Android player client",
                subtitle = "Experimental fallback for HD without login. May break age-gated videos.",
                checked = ytAndroidClient,
                onCheckedChange = { viewModel.setYtAndroidClient(it) }
            )
        }

        // APPEARANCE (Retro-style themes + accents)
        IOSSection(header = "APPEARANCE", footer = "Black saves battery on AMOLED screens.") {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val modes = listOf("SYSTEM", "LIGHT", "DARK", "BLACK")
                modes.forEach { mode ->
                    FilterChip(
                        selected = themeMode.uppercase() == mode,
                        onClick = { viewModel.setDarkThemeMode(mode) },
                        label = {
                            Text(
                                mode.lowercase().replaceFirstChar { it.uppercase() },
                                fontWeight = if (themeMode.uppercase() == mode) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            IOSDivider(startIndent = 16.dp)
            Text(
                text = "Accent color",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ZyvroAccents.all.forEach { name ->
                    val selected = accent.uppercase() == name
                    Box(
                        modifier = Modifier
                            .size(if (selected) 40.dp else 34.dp)
                            .clip(CircleShape)
                            .background(ZyvroAccents.primary(name, systemDark))
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                            .clickable { viewModel.setAccentColor(name) }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        // STORAGE & DOWNLOAD PATH
        IOSSection(
            header = "STORAGE & DOWNLOAD PATH",
            footer = "By default, downloads export directly to Android MediaStore Gallery (Movies & Music). You can select an SD Card or custom directory."
        ) {
            IOSValueRow(
                icon = Icons.Rounded.Folder,
                title = "Download directory",
                value = if (customDownloadDir.isBlank()) "Default (MediaStore Gallery)" else "Custom Folder"
            )
            if (customDownloadDir.isNotBlank()) {
                Text(
                    text = customDownloadDir,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            IOSDivider(startIndent = 16.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { pickFolder.launch(null) },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.FolderOpen, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (customDownloadDir.isBlank()) "Choose Custom Folder" else "Change Folder", fontWeight = FontWeight.SemiBold)
                }
                if (customDownloadDir.isNotBlank()) {
                    OutlinedButton(
                        onClick = { viewModel.setCustomDownloadDir("") },
                        modifier = Modifier.height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Reset")
                    }
                }
            }
        }

        // DOWNLOADS & NETWORK
        IOSSection(header = "DOWNLOADS & BANDWIDTH", footer = "Speed limiter applies to both Aria2 multi-chunk and yt-dlp engines.") {
            IOSSwitchRow(
                icon = Icons.Rounded.Bolt,
                title = "Aria2 acceleration",
                subtitle = if (useAria2) "On · $aria2Connections connections" else "Off",
                checked = useAria2,
                onCheckedChange = { viewModel.setUseAria2(it) }
            )
            if (useAria2) {
                IOSDivider(startIndent = 58.dp)
                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(
                        text = "Parallel connections · $aria2Connections",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = aria2Connections.toFloat(),
                        onValueChange = { viewModel.setAria2Connections(it.toInt()) },
                        valueRange = 1f..16f,
                        steps = 14,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            IOSDivider(startIndent = 16.dp)
            Text(
                text = "Download speed limiter",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 6.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val limits = listOf("Unlimited", "2M", "5M", "10M")
                limits.forEach { limit ->
                    val isSelected = speedLimit.equals(limit, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setSpeedLimit(limit) },
                        label = {
                            Text(
                                if (limit == "Unlimited") "No Limit" else "$limit/s",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.5.sp
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            IOSDivider(startIndent = 58.dp)
            IOSSwitchRow(
                icon = Icons.Rounded.Wifi,
                title = "Auto-resume on Wi-Fi",
                subtitle = "Resume interrupted downloads when connected to Wi-Fi",
                checked = autoResumeWifi,
                onCheckedChange = { viewModel.setAutoResumeWifi(it) }
            )
        }

        // PROCESSING
        IOSSection(header = "POST-PROCESSING") {
            IOSSwitchRow(
                icon = Icons.Rounded.Block,
                title = "SponsorBlock",
                subtitle = "Skip sponsored segments automatically",
                checked = sponsorBlockEnabled,
                onCheckedChange = { viewModel.setSponsorBlockEnabled(it) }
            )
            IOSDivider(startIndent = 58.dp)
            IOSSwitchRow(
                icon = Icons.Rounded.Subtitles,
                title = "Auto-embed subtitles",
                subtitle = "Embed subtitle tracks when available",
                checked = embedSubtitles,
                onCheckedChange = { viewModel.setEmbedSubtitles(it) }
            )
        }

        // ADVANCED
        IOSSection(header = "ADVANCED", footer = "Extra flags are appended to every yt-dlp call.") {
            OutlinedTextField(
                value = customArgsInput,
                onValueChange = {
                    customArgsInput = it
                    viewModel.setCustomArguments(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(12.dp),
                label = { Text("Custom yt-dlp flags") },
                placeholder = { Text("--embed-chapters --write-thumbnail") }
            )
        }

        // ABOUT
        IOSSection(header = "ABOUT") {
            IOSChevronRow(
                icon = Icons.Rounded.Description,
                title = "Legal & open source",
                subtitle = "Licenses, DMCA, terms",
                onClick = onOpenLegal
            )
            IOSDivider(startIndent = 58.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { uriHandler.openUri("https://github.com/Rishav7324/ytdlp-pro-android") }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(NovaAqua, NovaAquaDeep))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("RR", fontWeight = FontWeight.Black, color = NovaInk, fontSize = 15.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Rishav Raj", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Text("Zyvro for Android · @Rishav7324", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "Zyvro · v4.0.0 (Deep Space Aura)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(120.dp))
    }
}

/** iOS inset-grouped card with optional UPPER-CASE caption header + footer note. */
@Composable
private fun IOSSection(
    header: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = LocalAppDark.current
    if (header != null) {
        Text(
            text = header,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
            fontWeight = FontWeight.Bold,
            color = if (isDark) NovaPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, bottom = 6.dp)
        )
    }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isDark) SpaceCard else Color.White,
        border = androidx.compose.foundation.BorderStroke(
            0.6.dp,
            if (isDark) SpaceBorder else Color(0xFFD4E0F0)
        ),
        tonalElevation = 0.dp,
        shadowElevation = if (isDark) 0.dp else 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
    if (footer != null) {
        Text(
            text = footer,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 18.dp)
        )
    } else {
        Spacer(Modifier.height(18.dp))
    }
}

/** iOS hairline divider, indented past the row icon like UITableView. */
@Composable
private fun IOSDivider(startIndent: Dp = 16.dp) {
    val isDark = LocalAppDark.current
    HorizontalDivider(
        thickness = 0.5.dp,
        color = if (isDark) SpaceBorder else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        modifier = Modifier.padding(start = startIndent)
    )
}

@Composable
private fun IOSRowIcon(icon: ImageVector) {
    val isDark = LocalAppDark.current
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (isDark) SpaceCardHigh else NovaPrimary.copy(alpha = 0.12f))
            .border(0.5.dp, NovaPrimary.copy(alpha = 0.35f), RoundedCornerShape(9.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = NovaPrimary, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun IOSValueRow(icon: ImageVector, title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IOSRowIcon(icon)
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun IOSSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IOSRowIcon(icon)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun IOSChevronRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IOSRowIcon(icon)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    }
}
