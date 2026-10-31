package com.bishify.player.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.bishify.player.local.ThemePreset

data class SoundWaveColors(
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color
)

val LocalSoundWaveColors = staticCompositionLocalOf {
    SoundWaveColors(
        primary = CrimsonRed,
        secondary = DeepBurgundy,
        background = GraphiteDark,
        surface = GraphiteSurface,
        surfaceElevated = GraphiteSurfaceElevated,
        border = GraphiteBorder,
        textPrimary = TextPrimary,
        textSecondary = TextSecondary,
        textTertiary = TextTertiary
    )
}

@Composable
fun SoundWaveTheme(
    preset: ThemePreset = ThemePreset.GRAPHITE_BURGUNDY,
    isAmoled: Boolean = false,
    customAccent: Color = CrimsonRed,
    dynamicTheme: ExtractedTheme? = null,
    content: @Composable () -> Unit
) {
    val baseColors = when (preset) {
        ThemePreset.GRAPHITE_BURGUNDY -> {
            val bg = if (isAmoled) AmoledBlack else GraphiteDark
            val surf = if (isAmoled) AmoledSurface else GraphiteSurface
            SoundWaveColors(
                primary = CrimsonRed,
                secondary = DeepBurgundy,
                background = bg,
                surface = surf,
                surfaceElevated = GraphiteSurfaceElevated,
                border = GraphiteBorder,
                textPrimary = TextPrimary,
                textSecondary = TextSecondary,
                textTertiary = TextTertiary
            )
        }
        ThemePreset.AMOLED_CRIMSON -> {
            SoundWaveColors(
                primary = Color(0xFFFF2A55),
                secondary = DeepBurgundy,
                background = AmoledBlack,
                surface = AmoledSurface,
                surfaceElevated = Color(0xFF141416),
                border = Color(0xFF222226),
                textPrimary = TextPrimary,
                textSecondary = TextSecondary,
                textTertiary = TextTertiary
            )
        }
        ThemePreset.MIDNIGHT_BLUE -> {
            val bg = if (isAmoled) AmoledBlack else MidnightBg
            val surf = if (isAmoled) AmoledSurface else MidnightSurface
            SoundWaveColors(
                primary = MidnightPrimary,
                secondary = Color(0xFF1D4ED8),
                background = bg,
                surface = surf,
                surfaceElevated = Color(0xFF1C273C),
                border = Color(0xFF243450),
                textPrimary = TextPrimary,
                textSecondary = TextSecondary,
                textTertiary = TextTertiary
            )
        }
        ThemePreset.FOREST_GREEN -> {
            val bg = if (isAmoled) AmoledBlack else ForestBg
            val surf = if (isAmoled) AmoledSurface else ForestSurface
            SoundWaveColors(
                primary = ForestPrimary,
                secondary = Color(0xFF0F766E),
                background = bg,
                surface = surf,
                surfaceElevated = Color(0xFF1D352E),
                border = Color(0xFF26463D),
                textPrimary = TextPrimary,
                textSecondary = TextSecondary,
                textTertiary = TextTertiary
            )
        }
        ThemePreset.PURPLE_HAZE -> {
            val bg = if (isAmoled) AmoledBlack else PurpleBg
            val surf = if (isAmoled) AmoledSurface else PurpleSurface
            SoundWaveColors(
                primary = PurplePrimary,
                secondary = Color(0xFF6B21A8),
                background = bg,
                surface = surf,
                surfaceElevated = Color(0xFF2A163B),
                border = Color(0xFF381D4E),
                textPrimary = TextPrimary,
                textSecondary = TextSecondary,
                textTertiary = TextTertiary
            )
        }
        ThemePreset.CUSTOM -> {
            val bg = if (isAmoled) AmoledBlack else GraphiteDark
            val surf = if (isAmoled) AmoledSurface else GraphiteSurface
            SoundWaveColors(
                primary = customAccent,
                secondary = customAccent.copy(alpha = 0.7f),
                background = bg,
                surface = surf,
                surfaceElevated = GraphiteSurfaceElevated,
                border = GraphiteBorder,
                textPrimary = TextPrimary,
                textSecondary = TextSecondary,
                textTertiary = TextTertiary
            )
        }
    }

    val activePrimary = dynamicTheme?.primary ?: baseColors.primary
    val activeSecondary = dynamicTheme?.secondary ?: baseColors.secondary

    val animPrimary = animateColorAsState(activePrimary, tween(400), label = "animPrimary").value
    val animSecondary = animateColorAsState(activeSecondary, tween(400), label = "animSecondary").value
    val animBg = animateColorAsState(baseColors.background, tween(400), label = "animBg").value
    val animSurf = animateColorAsState(baseColors.surface, tween(400), label = "animSurf").value

    val finalColors = baseColors.copy(
        primary = animPrimary,
        secondary = animSecondary,
        background = animBg,
        surface = animSurf
    )

    val materialColors = darkColorScheme(
        primary = finalColors.primary,
        secondary = finalColors.secondary,
        background = finalColors.background,
        surface = finalColors.surface,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = finalColors.textPrimary,
        onSurface = finalColors.textPrimary
    )

    CompositionLocalProvider(LocalSoundWaveColors provides finalColors) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = SoundWaveTypography,
            content = content
        )
    }
}

