package com.bishify.player.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bishify.player.ui.theme.LocalSoundWaveColors

sealed class Screen(val route: String, val title: String, val iconSelected: ImageVector, val iconUnselected: ImageVector) {
    data object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    data object Songs : Screen("songs", "Songs", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic)
    data object NowPlaying : Screen("now_playing", "Playing", Icons.Filled.PlayCircle, Icons.Outlined.PlayCircle)
    data object Playlists : Screen("playlists", "Playlists", Icons.Filled.QueueMusic, Icons.Outlined.QueueMusic)
    data object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun SoundWaveBottomNav(
    currentRoute: String,
    onNavigate: (Screen) -> Unit,
    isPlaying: Boolean,
    onCenterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSoundWaveColors.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface.copy(alpha = 0.98f))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(colors.border.copy(alpha = 0.6f), Color.Transparent)
                ),
                shape = RectangleShape
            )
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // Home Tab
            NavItem(
                screen = Screen.Home,
                isSelected = currentRoute == Screen.Home.route,
                onClick = { onNavigate(Screen.Home) }
            )

            // Songs Tab
            NavItem(
                screen = Screen.Songs,
                isSelected = currentRoute == Screen.Songs.route,
                onClick = { onNavigate(Screen.Songs) }
            )

            // Center Prominent Now Playing Button
            CenterNowPlayingButton(
                isPlaying = isPlaying,
                onClick = onCenterClick
            )

            // Playlists Tab
            NavItem(
                screen = Screen.Playlists,
                isSelected = currentRoute == Screen.Playlists.route,
                onClick = { onNavigate(Screen.Playlists) }
            )

            // Settings Tab
            NavItem(
                screen = Screen.Settings,
                isSelected = currentRoute == Screen.Settings.route,
                onClick = { onNavigate(Screen.Settings) }
            )
        }
    }
}

@Composable
private fun NavItem(
    screen: Screen,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalSoundWaveColors.current
    val tint = animateColorAsState(
        if (isSelected) colors.primary else colors.textSecondary.copy(alpha = 0.7f),
        label = "navTint"
    ).value

    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (isSelected) screen.iconSelected else screen.iconUnselected,
            contentDescription = screen.title,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = screen.title,
            color = tint,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun CenterNowPlayingButton(
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalSoundWaveColors.current

    Box(
        modifier = Modifier
            .offset(y = (-8).dp)
            .size(52.dp)
            .shadow(12.dp, CircleShape, spotColor = colors.primary.copy(alpha = 0.5f))
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(colors.primary, colors.secondary)
                )
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.GraphicEq else Icons.Default.MusicNote,
            contentDescription = "Now Playing Center",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}
