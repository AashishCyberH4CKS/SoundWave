package com.bishify.player.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bishify.player.ui.theme.GlassBorderSubtle
import com.bishify.player.ui.theme.GlassSurfaceElevated
import com.bishify.player.ui.theme.LocalSoundWaveColors

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    borderWidth: Dp = 1.dp,
    glassGradient: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = LocalSoundWaveColors.current
    val bg = backgroundColor ?: colors.surface.copy(alpha = 0.72f)
    val border = borderColor ?: GlassBorderSubtle

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.98f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "glassPressScale"
    )

    var boxModifier = modifier
        .scale(scale)
        .clip(shape)
        .background(bg)

    if (glassGradient) {
        boxModifier = boxModifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.05f),
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.15f)
                )
            )
        )
    }

    boxModifier = boxModifier.border(borderWidth, border, shape)

    if (onClick != null) {
        boxModifier = boxModifier.clickable(
            interactionSource = interactionSource,
            indication = ripple(color = Color.White.copy(alpha = 0.2f)),
            onClick = onClick
        )
    }

    Box(modifier = boxModifier, content = content)
}

@Composable
fun GlassCapsule(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(percent = 50),
    backgroundColor: Color = Color.White.copy(alpha = 0.08f),
    borderColor: Color = GlassBorderSubtle,
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    GlassCard(
        modifier = modifier,
        shape = shape,
        backgroundColor = backgroundColor,
        borderColor = borderColor,
        borderWidth = borderWidth,
        glassGradient = false,
        onClick = onClick,
        content = content
    )
}


