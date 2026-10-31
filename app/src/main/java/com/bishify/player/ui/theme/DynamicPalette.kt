package com.bishify.player.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ExtractedTheme(
    val primary: Color,
    val secondary: Color,
    val surface: Color,
    val background: Color,
    val onPrimary: Color = Color.White,
    val onSurface: Color = Color(0xFFF0F2F5)
)

object PaletteManager {
    private val cache = mutableMapOf<String, ExtractedTheme>()

    val defaultTheme = ExtractedTheme(
        primary = Color(0xFFE63946),
        secondary = Color(0xFF9B111E),
        surface = Color(0xFF16181E),
        background = Color(0xFF101216)
    )

    suspend fun extractFromUri(context: Context, artworkUriString: String?): ExtractedTheme {
        if (artworkUriString.isNullOrBlank()) return defaultTheme

        cache[artworkUriString]?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(artworkUriString)
                val bitmap: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                        // Sample down to 128x128 for ultra fast palette extraction
                        decoder.setTargetSampleSize(calculateSampleSize(info.size.width, info.size.height, 128, 128))
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }

                if (bitmap != null) {
                    val palette = Palette.from(bitmap).generate()
                    val vibrant = palette.vibrantSwatch
                    val dominant = palette.dominantSwatch
                    val muted = palette.mutedSwatch

                    val primaryInt = vibrant?.rgb ?: dominant?.rgb ?: 0xFFE63946.toInt()
                    val secondaryInt = muted?.rgb ?: vibrant?.titleTextColor ?: 0xFF9B111E.toInt()

                    val primary = Color(primaryInt)
                    val secondary = Color(secondaryInt)
                    val surface = Color(0xFF16181E)
                    val background = Color(0xFF101216)

                    val extracted = ExtractedTheme(
                        primary = primary,
                        secondary = secondary,
                        surface = surface,
                        background = background
                    )
                    cache[artworkUriString] = extracted
                    extracted
                } else {
                    defaultTheme
                }
            } catch (e: Exception) {
                defaultTheme
            }
        }
    }

    private fun calculateSampleSize(srcWidth: Int, srcHeight: Int, reqWidth: Int, reqHeight: Int): Int {
        var sampleSize = 1
        if (srcHeight > reqHeight || srcWidth > reqWidth) {
            val halfHeight = srcHeight / 2
            val halfWidth = srcWidth / 2
            while (halfHeight / sampleSize >= reqHeight && halfWidth / sampleSize >= reqWidth) {
                sampleSize *= 2
            }
        }
        return sampleSize
    }
}

@Composable
fun rememberAnimatedTheme(targetTheme: ExtractedTheme): ExtractedTheme {
    val primaryAnim by animateColorAsState(targetTheme.primary, tween(500), label = "primaryAnim")
    val secondaryAnim by animateColorAsState(targetTheme.secondary, tween(500), label = "secondaryAnim")
    val surfaceAnim by animateColorAsState(targetTheme.surface, tween(500), label = "surfaceAnim")
    val backgroundAnim by animateColorAsState(targetTheme.background, tween(500), label = "backgroundAnim")

    return ExtractedTheme(
        primary = primaryAnim,
        secondary = secondaryAnim,
        surface = surfaceAnim,
        background = backgroundAnim,
        onPrimary = targetTheme.onPrimary,
        onSurface = targetTheme.onSurface
    )
}

