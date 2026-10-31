package com.bishify.player.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bishify.player.ui.components.SoundWaveBottomNav
import com.bishify.player.ui.components.MiniPlayer
import com.bishify.player.ui.components.Screen
import com.bishify.player.ui.screens.*
import com.bishify.player.ui.theme.SoundWaveTheme
import com.bishify.player.ui.theme.rememberAnimatedTheme
import com.bishify.player.ui.viewmodel.MusicViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val preferences by viewModel.preferences.collectAsState()
            val extractedThemeRaw by viewModel.extractedTheme.collectAsState()
            val animatedDynamicTheme = extractedThemeRaw?.let { rememberAnimatedTheme(it) }

            var showSplash by remember { mutableStateOf(true) }

            SoundWaveTheme(
                preset = preferences.themePreset,
                isAmoled = preferences.isAmoled,
                customAccent = Color(preferences.customAccentColor),
                dynamicTheme = animatedDynamicTheme
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    MainAppContent(viewModel = viewModel)

                    androidx.compose.animation.AnimatedVisibility(
                        visible = showSplash,
                        enter = androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(500))
                    ) {
                        SplashScreen(onSplashFinished = { showSplash = false })
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: MusicViewModel) {
    val context = LocalContext.current
    val preferences by viewModel.preferences.collectAsState()
    val navController = rememberNavController()
    val playbackManager = viewModel.playbackManager

    val isPlaying by playbackManager.isPlaying.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    val songs by viewModel.songs.collectAsState()

    // Audio Permissions Check
    var hasAudioPermission by remember {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        mutableStateOf(ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            viewModel.rescanLibrary()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasAudioPermission) {
            val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            permissionLauncher.launch(perm)
        }
    }

    val isFullScreenRoute = currentRoute == Screen.NowPlaying.route ||
            currentRoute == "driving_mode" ||
            currentRoute.startsWith("ringtone_cutter") ||
            currentRoute.startsWith("metadata_editor") ||
            currentRoute == "statistics" ||
            currentRoute == "equalizer"

    Box(modifier = Modifier.fillMaxSize()) {
        // Custom Wallpaper Backdrop if configured
        if (!preferences.wallpaperUri.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(preferences.wallpaperUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = preferences.wallpaperDim))
            )
        }

        Scaffold(
            containerColor = if (preferences.wallpaperUri != null) Color.Transparent else MaterialTheme.colorScheme.background,
            bottomBar = {
                if (!isFullScreenRoute) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        MiniPlayer(
                            playbackManager = playbackManager,
                            onClick = { navController.navigate(Screen.NowPlaying.route) }
                        )

                        SoundWaveBottomNav(
                            currentRoute = currentRoute,
                            onNavigate = { screen ->
                                if (screen.route == Screen.Home.route) {
                                    val popped = navController.popBackStack(Screen.Home.route, inclusive = false)
                                    if (!popped) {
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                } else if (screen.route != currentRoute) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            isPlaying = isPlaying,
                            onCenterClick = {
                                navController.navigate(Screen.NowPlaying.route)
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isFullScreenRoute) PaddingValues(0.dp) else innerPadding)
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToSongs = { navController.navigate(Screen.Songs.route) },
                        onNavigateToPlaylists = { navController.navigate(Screen.Playlists.route) },
                        onNavigateToNowPlaying = { navController.navigate(Screen.NowPlaying.route) }
                    )
                }

                composable(Screen.Songs.route) {
                    SongsScreen(
                        viewModel = viewModel,
                        onNavigateToRingtone = { song ->
                            navController.navigate("ringtone_cutter/${song.id}")
                        },
                        onNavigateToEditMetadata = { song ->
                            navController.navigate("metadata_editor/${song.id}")
                        }
                    )
                }

                composable(Screen.Playlists.route) {
                    PlaylistsScreen(viewModel = viewModel)
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateToEqualizer = { navController.navigate("equalizer") },
                        onNavigateToDrivingMode = { navController.navigate("driving_mode") },
                        onNavigateToStatistics = { navController.navigate("statistics") }
                    )
                }

                composable(Screen.NowPlaying.route) {
                    NowPlayingScreen(
                        viewModel = viewModel,
                        onBackClick = { navController.popBackStack() },
                        onNavigateToEqualizer = { navController.navigate("equalizer") },
                        onNavigateToDrivingMode = { navController.navigate("driving_mode") }
                    )
                }

                composable("equalizer") {
                    EqualizerScreen(
                        viewModel = viewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable("driving_mode") {
                    DrivingModeScreen(
                        viewModel = viewModel,
                        onExit = { navController.popBackStack() }
                    )
                }

                composable("statistics") {
                    StatisticsScreen(
                        viewModel = viewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(
                    route = "ringtone_cutter/{songId}",
                    arguments = listOf(navArgument("songId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val songId = backStackEntry.arguments?.getLong("songId") ?: -1L
                    val song = songs.firstOrNull { it.id == songId }
                    if (song != null) {
                        RingtoneCutterScreen(
                            song = song,
                            viewModel = viewModel,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                }

                composable(
                    route = "metadata_editor/{songId}",
                    arguments = listOf(navArgument("songId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val songId = backStackEntry.arguments?.getLong("songId") ?: -1L
                    val song = songs.firstOrNull { it.id == songId }
                    if (song != null) {
                        MetadataEditorScreen(
                            song = song,
                            viewModel = viewModel,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

