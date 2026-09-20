package com.zyvro.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.zyvro.app.R
import com.zyvro.app.data.preferences.AppPreferences
import com.zyvro.app.ui.components.LiquidGlassCard
import com.zyvro.app.ui.theme.NovaAqua
import com.zyvro.app.ui.theme.NovaAquaDeep
import com.zyvro.app.ui.theme.NovaAquaSoft
import com.zyvro.app.ui.theme.NovaInk
import kotlinx.coroutines.launch

private data class AppPermissionItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val permissions: List<String>
)

@Composable
fun PermissionScreen(
    onPermissionsCompleted: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val preferences = remember { AppPreferences(context) }

    val permissionItems = remember {
        val list = mutableListOf<AppPermissionItem>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(
                AppPermissionItem(
                    id = "media_video",
                    title = "Video & Media Access",
                    description = "Required to scan, organize, and play downloaded HD/4K videos in MX Player engine.",
                    icon = Icons.Default.VideoLibrary,
                    permissions = listOf(Manifest.permission.READ_MEDIA_VIDEO)
                )
            )
            list.add(
                AppPermissionItem(
                    id = "media_audio",
                    title = "Audio & Music Library",
                    description = "Enables smooth audio playback, background music streaming, and DSP sound enhancement.",
                    icon = Icons.Default.VolumeUp,
                    permissions = listOf(Manifest.permission.READ_MEDIA_AUDIO)
                )
            )
            list.add(
                AppPermissionItem(
                    id = "notifications",
                    title = "Download Notifications",
                    description = "Displays live background yt-dlp download progress and active media control notifications.",
                    icon = Icons.Default.NotificationsActive,
                    permissions = listOf(Manifest.permission.POST_NOTIFICATIONS)
                )
            )
        } else {
            list.add(
                AppPermissionItem(
                    id = "storage",
                    title = "Storage Access",
                    description = "Required to save and play downloaded media files in your device storage.",
                    icon = Icons.Default.VideoLibrary,
                    permissions = listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                )
            )
        }
        list
    }

    var refreshTrigger by remember { mutableStateOf(0) }

    val checkGranted: (List<String>) -> Boolean = { perms ->
        perms.all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshTrigger++
    }

    fun requestPermissions(perms: List<String>) {
        val missing = perms.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            launcher.launch(missing.toTypedArray())
        }
    }

    fun requestAll() {
        val allMissing = permissionItems.flatMap { it.permissions }.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }.distinct()
        if (allMissing.isNotEmpty()) {
            launcher.launch(allMissing.toTypedArray())
        } else {
            coroutineScope.launch {
                preferences.setOnboardingCompleted(true)
                onPermissionsCompleted()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF070F1E),
                        Color(0xFF0B1728),
                        Color(0xFF050B14)
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                // Logo & Header Card
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .shadow(16.dp, RoundedCornerShape(22.dp), spotColor = NovaAqua)
                        .clip(RoundedCornerShape(22.dp))
                        .border(
                            1.5.dp,
                            Brush.linearGradient(listOf(NovaAqua, NovaAquaDeep.copy(alpha = 0.5f))),
                            RoundedCornerShape(22.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "Zyvro Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Welcome to Zyvro",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Please enable permissions to provide hardware accelerated MX Player media playback and background downloads.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))
            }

            items(permissionItems, key = { it.id }) { item ->
                val isGranted = checkGranted(item.permissions)

                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    elevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(NovaAqua.copy(alpha = 0.16f))
                                .border(1.dp, NovaAqua.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = if (isGranted) NovaAqua else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        if (isGranted) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF00E676).copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.6f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Granted",
                                    tint = Color(0xFF00E676),
                                    modifier = Modifier.padding(6.dp).size(18.dp)
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = { requestPermissions(item.permissions) },
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NovaAqua),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Allow",
                                    color = NovaAqua,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))

                // Read the refresh trigger so granting permissions recomposes this
                // block (progress ticks, "Allow" -> check, button -> Continue).
                val refresh = refreshTrigger
                val allGranted = remember(refresh) {
                    permissionItems.all { checkGranted(it.permissions) }
                }

                // Auto-advance the moment everything is granted.
                androidx.compose.runtime.LaunchedEffect(allGranted) {
                    if (allGranted) {
                        preferences.setOnboardingCompleted(true)
                        onPermissionsCompleted()
                    }
                }

                Button(
                    onClick = {
                        if (allGranted) {
                            coroutineScope.launch {
                                preferences.setOnboardingCompleted(true)
                                onPermissionsCompleted()
                            }
                        } else {
                            requestAll()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .shadow(12.dp, RoundedCornerShape(18.dp), ambientColor = NovaAqua),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NovaAqua,
                        contentColor = NovaInk
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (allGranted) "Continue to Zyvro" else "Grant Permissions",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (!allGranted) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                preferences.setOnboardingCompleted(true)
                                onPermissionsCompleted()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text(
                            text = "Skip for Now",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
