package com.bishify.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bishify.player.ui.theme.LocalSoundWaveColors

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = LocalSoundWaveColors.current
    val bg = backgroundColor ?: colors.surface.copy(alpha = 0.85f)
    val border = borderColor ?: colors.border.copy(alpha = 0.5f)

    var boxModifier = modifier
        .clip(shape)
        .background(bg)
        .border(borderWidth, border, shape)

    if (onClick != null) {
        boxModifier = boxModifier.clickable { onClick() }
    }

    Box(modifier = boxModifier, content = content)
}

