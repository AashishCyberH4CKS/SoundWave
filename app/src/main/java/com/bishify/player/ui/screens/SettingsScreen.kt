package com.bishify.player.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bishify.player.R
import com.bishify.player.local.ThemePreset
import com.bishify.player.ui.components.SoundWaveTopBar
import com.bishify.player.ui.components.GlassCard
import com.bishify.player.ui.theme.LocalSoundWaveColors
import com.bishify.player.ui.viewmodel.MusicViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SettingsScreen(
    viewModel: MusicViewModel,
    onNavigateToEqualizer: () -> Unit,
    onNavigateToDrivingMode: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSoundWaveColors.current
    val context = LocalContext.current
    val preferences by viewModel.preferences.collectAsState()
    val scope = rememberCoroutineScope()
    val excludedFolders by viewModel.excludedFolderPaths.collectAsState()

    var showFolderExclusionDialog by remember { mutableStateOf(false) }

    // System Photo Picker for custom wallpaper - copies to app private storage for permanent retention
    val wallpaperPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val wallpaperFile = File(context.filesDir, "custom_wallpaper_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        wallpaperFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    // Clean up any old wallpaper file
                    preferences.wallpaperUri?.let { oldUriStr ->
                        try {
                            val oldUri = Uri.parse(oldUriStr)
                            if (oldUri.scheme == "file") {
                                oldUri.path?.let { File(it).delete() }
                            }
                        } catch (_: Exception) {}
                    }
                    viewModel.preferencesManager.setWallpaperUri(Uri.fromFile(wallpaperFile).toString())
                } catch (e: Exception) {
                    Log.e("SettingsScreen", "Failed to persist custom wallpaper: ${e.message}", e)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        SoundWaveTopBar(title = "Settings")

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Appearance & Themes
            item {
                SettingsCategoryTitle("Appearance & Themes")
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = colors.surfaceElevated
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Theme Presets",
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Theme Preset Buttons
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemePreset.values().forEach { preset ->
                                val isSelected = preferences.themePreset == preset
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) colors.primary.copy(alpha = 0.15f) else Color.Transparent
                                        )
                                        .clickable {
                                            scope.launch {
                                                viewModel.preferencesManager.setThemePreset(preset)
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = preset.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                        color = if (isSelected) colors.primary else colors.textPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, null, tint = colors.primary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            color = colors.border.copy(alpha = 0.3f),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        // AMOLED Black Toggle
                        SettingsToggleRow(
                            title = "True AMOLED Black",
                            subtitle = "Pure black backgrounds for OLED displays",
                            checked = preferences.isAmoled,
                            onCheckedChange = { scope.launch { viewModel.preferencesManager.setAmoled(it) } }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Dynamic Album Art Theme Toggle
                        SettingsToggleRow(
                            title = "Dynamic Album Art Theme",
                            subtitle = "Adapt accent colors automatically to current track's artwork",
                            checked = preferences.dynamicArtworkTheme,
                            onCheckedChange = { scope.launch { viewModel.preferencesManager.setDynamicArtworkTheme(it) } }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Vinyl Animation Toggle
                        SettingsToggleRow(
                            title = "Rotating Vinyl Disc",
                            subtitle = "Animate rotating album artwork in Now Playing",
                            checked = preferences.vinylAnimationEnabled,
                            onCheckedChange = { scope.launch { viewModel.preferencesManager.setVinylAnimationEnabled(it) } }
                        )
                    }
                }
            }

            // Custom Wallpaper & Transparency
            item {
                SettingsCategoryTitle("Custom Wallpaper & Backdrop")
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = colors.surfaceElevated
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Custom Wallpaper", color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    if (preferences.wallpaperUri != null) "Custom image active" else "Default graphite background",
                                    color = colors.textSecondary,
                                    fontSize = 12.sp
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (preferences.wallpaperUri != null) {
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch(Dispatchers.IO) {
                                                preferences.wallpaperUri?.let { oldUriStr ->
                                                    try {
                                                        val oldUri = Uri.parse(oldUriStr)
                                                        if (oldUri.scheme == "file") {
                                                            oldUri.path?.let { File(it).delete() }
                                                        }
                                                    } catch (_: Exception) {}
                                                }
                                                viewModel.preferencesManager.setWallpaperUri(null)
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Remove", fontSize = 12.sp)
                                    }
                                }

                                Button(
                                    onClick = {
                                        wallpaperPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Pick Image", fontSize = 12.sp)
                                }
                            }
                        }

                        if (preferences.wallpaperUri != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("Wallpaper Dimming: ${(preferences.wallpaperDim * 100).toInt()}%", color = colors.textSecondary, fontSize = 12.sp)
                            Slider(
                                value = preferences.wallpaperDim,
                                onValueChange = { scope.launch { viewModel.preferencesManager.setWallpaperDim(it) } },
                                valueRange = 0.1f..0.9f,
                                colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                            )
                        }
                    }
                }
            }

            // Playback & Audio
            item {
                SettingsCategoryTitle("Playback & Sound")
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = colors.surfaceElevated
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Fade-in on start
                        SettingsToggleRow(
                            title = "Fade-In on Track Start",
                            subtitle = "Smoothly ramp volume up when song starts",
                            checked = preferences.fadeInEnabled,
                            onCheckedChange = { scope.launch { viewModel.preferencesManager.setFadeInEnabled(it) } }
                        )

                        if (preferences.fadeInEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Fade Duration: ${preferences.fadeInDurationMs}ms", color = colors.textSecondary, fontSize = 12.sp)
                            Slider(
                                value = preferences.fadeInDurationMs.toFloat(),
                                onValueChange = { scope.launch { viewModel.preferencesManager.setFadeInDurationMs(it.toLong()) } },
                                valueRange = 400f..3000f,
                                steps = 12,
                                colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                            )
                        }

                        HorizontalDivider(
                            color = colors.border.copy(alpha = 0.3f),
                            modifier = Modifier.padding(vertical = 10.dp)
                        )

                        // Pause on headset disconnect
                        SettingsToggleRow(
                            title = "Pause on Disconnect",
                            subtitle = "Pause playback when headphones or Bluetooth unplug",
                            checked = preferences.pauseOnHeadsetDisconnect,
                            onCheckedChange = { scope.launch { viewModel.preferencesManager.setPauseOnHeadsetDisconnect(it) } }
                        )

                        HorizontalDivider(
                            color = colors.border.copy(alpha = 0.3f),
                            modifier = Modifier.padding(vertical = 10.dp)
                        )

                        // Equalizer shortcut
                        SettingsNavRow(
                            title = "Equalizer & Sound Effects",
                            subtitle = "5-band EQ, Bass Boost, 3D Surround",
                            icon = Icons.Default.Tune,
                            onClick = onNavigateToEqualizer
                        )
                    }
                }
            }

            // Driving Mode & Statistics
            item {
                SettingsCategoryTitle("Smart Features")
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = colors.surfaceElevated
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingsNavRow(
                            title = "Driving Mode",
                            subtitle = "High-contrast simplified controls for safe driving",
                            icon = Icons.Default.DirectionsCar,
                            onClick = onNavigateToDrivingMode
                        )

                        HorizontalDivider(
                            color = colors.border.copy(alpha = 0.3f),
                            modifier = Modifier.padding(vertical = 10.dp)
                        )

                        SettingsNavRow(
                            title = "Listening Analytics",
                            subtitle = "Local play count, total time & top artists",
                            icon = Icons.Default.BarChart,
                            onClick = onNavigateToStatistics
                        )

                        HorizontalDivider(
                            color = colors.border.copy(alpha = 0.3f),
                            modifier = Modifier.padding(vertical = 10.dp)
                        )

                        SettingsToggleRow(
                            title = "Private Listening Mode",
                            subtitle = "Do not save tracks to history while enabled",
                            checked = preferences.privateListeningMode,
                            onCheckedChange = { scope.launch { viewModel.preferencesManager.setPrivateListeningMode(it) } }
                        )
                    }
                }
            }

            // Local Library & Exclusions
            item {
                SettingsCategoryTitle("Local Storage & Folders")
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = colors.surfaceElevated
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.rescanLibrary() }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Refresh, null, tint = colors.primary)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Rescan Music Library", color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Refresh audio files from device MediaStore", color = colors.textSecondary, fontSize = 12.sp)
                            }
                        }

                        HorizontalDivider(
                            color = colors.border.copy(alpha = 0.3f),
                            modifier = Modifier.padding(vertical = 10.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showFolderExclusionDialog = true }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FolderOff, null, tint = colors.textSecondary)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Excluded Folders (${excludedFolders.size})", color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Manage folders hidden from library", color = colors.textSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // About SoundWave & Developer
            item {
                SettingsCategoryTitle("About SoundWave")
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = colors.surfaceElevated
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.soundwave_logo_branded),
                                    contentDescription = "SoundWave",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "SoundWave Music Player",
                                    color = colors.textPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "v1.0.0 • 100% Offline • No Ads • No Tracking",
                                    color = colors.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "SoundWave is crafted for pure offline music immersion. All songs, playlists, custom sound profiles, and listening history remain strictly on your device.",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )

                        HorizontalDivider(
                            color = colors.border.copy(alpha = 0.35f),
                            modifier = Modifier.padding(vertical = 14.dp)
                        )

                        // Developer Info Card
                        Text(
                            text = "DEVELOPER",
                            color = colors.textTertiary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(colors.primary, colors.secondary)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Aashish Babu",
                                    color = colors.textPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Lead Developer & Architect",
                                    color = colors.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Contact Buttons (Telegram & GitHub)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Telegram Contact Button
                            Button(
                                onClick = {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://t.me/NeoCypher666")
                                    )
                                    try {
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF229ED9).copy(alpha = 0.2f),
                                    contentColor = Color(0xFF229ED9)
                                ),
                                contentPadding = PaddingValues(vertical = 10.dp, horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "@NeoCypher666",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // GitHub Contact Button
                            Button(
                                onClick = {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://github.com/AashishCyberH4CKS")
                                    )
                                    try {
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.surface.copy(alpha = 0.9f),
                                    contentColor = colors.textPrimary
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(vertical = 10.dp, horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AashishCyberH4CKS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    viewModel.preferencesManager.resetAllPreferences()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.secondary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Reset All Settings to Defaults")
                        }
                    }
                }
            }
        }
    }

    // Excluded Folders Dialog
    if (showFolderExclusionDialog) {
        AlertDialog(
            onDismissRequest = { showFolderExclusionDialog = false },
            title = { Text("Excluded Folders", color = colors.textPrimary) },
            text = {
                Column {
                    if (excludedFolders.isEmpty()) {
                        Text("No folders currently excluded. You can exclude folders from the Songs -> Folders tab.", color = colors.textSecondary, fontSize = 13.sp)
                    } else {
                        excludedFolders.forEach { path ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(path, color = colors.textPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                TextButton(onClick = { viewModel.includeFolder(path) }) {
                                    Text("Include", color = colors.primary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFolderExclusionDialog = false }) {
                    Text("Close", color = colors.primary)
                }
            },
            containerColor = colors.surfaceElevated
        )
    }
}

@Composable
private fun SettingsCategoryTitle(text: String) {
    val colors = LocalSoundWaveColors.current
    Text(
        text = text,
        color = colors.primary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalSoundWaveColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = colors.textSecondary, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.primary,
                checkedTrackColor = colors.primary.copy(alpha = 0.4f)
            )
        )
    }
}

@Composable
private fun SettingsNavRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val colors = LocalSoundWaveColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = colors.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = colors.textSecondary, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = colors.textTertiary)
    }
}

