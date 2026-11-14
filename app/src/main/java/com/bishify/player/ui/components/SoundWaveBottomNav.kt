package com.bishify.player.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bishify.player.ui.theme.GlassBorderHighlight
import com.bishify.player.ui.theme.GlassBorderSubtle
import com.bishify.player.ui.theme.GlassSurfaceDark

sealed class Screen(
    val route: String,
    val title: String,
    val iconSelected: ImageVector,
    val iconUnselected: ImageVector
) {
    data object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    data object Explore : Screen("explore", "Explore", Icons.Filled.Explore, Icons.Outlined.Explore)
    data object Library : Screen("library", "Library", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic)
    data object Search : Screen("search", "Search", Icons.Filled.Search, Icons.Outlined.Search)
    data object Songs : Screen("songs", "Songs", Icons.Filled.MusicNote, Icons.Outlined.MusicNote)
    data object NowPlaying : Screen("now_playing", "Playing", Icons.Filled.PlayCircle, Icons.Outlined.PlayCircle)
    data object Playlists : Screen("playlists", "Playlists", Icons.Filled.QueueMusic, Icons.Outlined.QueueMusic)
    data object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

/**
 * Modern Liquid Glass Floating Bottom Navigation:
 * - Rounded translucent glass capsule containing Home, Explore, and Library tabs with smooth moving indicator.
 * - Separate circular glass button on the right for Search.
 * - Floats above bottom system insets with elegant dark glassmorphic styling.
 */
@Composable
fun SoundWaveBottomNav(
    currentRoute: String,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    onCenterClick: (() -> Unit)? = null
) {
    val tabs = remember {
        listOf(
            Screen.Home,
            Screen.Explore,
            Screen.Library
        )
    }

    // Determine current active index in the main capsule
    val activeIndex = when (currentRoute) {
        Screen.Home.route -> 0
        Screen.Explore.route -> 1
        Screen.Library.route, Screen.Songs.route, Screen.Playlists.route -> 2
        else -> -1
    }

    val isSearchSelected = currentRoute == Screen.Search.route

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Main Navigation Glass Capsule (Home, Explore, Library)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(64.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(32.dp),
                    spotColor = Color.Black.copy(alpha = 0.7f),
                    ambientColor = Color.Black.copy(alpha = 0.5f)
                )
                .clip(RoundedCornerShape(32.dp))
                .background(GlassSurfaceDark.copy(alpha = 0.75f))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.25f)
                        )
                    )
                )
                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(32.dp))
                .padding(4.dp)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val tabWidth = maxWidth / tabs.size

                // Smoothly animated pill indicator behind the selected tab
                if (activeIndex in tabs.indices) {
                    val indicatorOffset by animateDpAsState(
                        targetValue = tabWidth * activeIndex,
                        animationSpec = spring(
                            dampingRatio = 0.8f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "tabIndicatorOffset"
                    )

                    Box(
                        modifier = Modifier
                            .offset(x = indicatorOffset)
                            .width(tabWidth)
                            .fillMaxHeight()
                            .padding(2.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.09f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(26.dp))
                    )
                }

                // Interactive Tab items
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, screen ->
                        val isSelected = activeIndex == index
                        val contentColor by animateColorAsState(
                            targetValue = if (isSelected) Color.White else Color(0xFFA0A0A5),
                            label = "tabColor"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(26.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, color = Color.White.copy(alpha = 0.2f))
                                ) {
                                    onNavigate(screen)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (isSelected) screen.iconSelected else screen.iconUnselected,
                                    contentDescription = screen.title,
                                    tint = contentColor,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = screen.title,
                                    color = contentColor,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    letterSpacing = 0.2.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Separate Circular Glass Button for Search
        Box(
            modifier = Modifier
                .size(64.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = CircleShape,
                    spotColor = Color.Black.copy(alpha = 0.7f),
                    ambientColor = Color.Black.copy(alpha = 0.5f)
                )
                .clip(CircleShape)
                .background(
                    if (isSearchSelected) Color.White.copy(alpha = 0.18f)
                    else GlassSurfaceDark.copy(alpha = 0.75f)
                )
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.25f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = if (isSearchSelected) GlassBorderHighlight else GlassBorderSubtle,
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = Color.White.copy(alpha = 0.25f))
                ) {
                    onNavigate(Screen.Search)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSearchSelected) Icons.Filled.Search else Icons.Outlined.Search,
                contentDescription = "Search",
                tint = if (isSearchSelected) Color.White else Color(0xFFA0A0A5),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
