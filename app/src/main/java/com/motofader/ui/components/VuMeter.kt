package com.motofader.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.motofader.ui.theme.VuGreen
import com.motofader.ui.theme.VuOff
import com.motofader.ui.theme.VuRed
import com.motofader.ui.theme.VuYellow

@Composable
fun VuMeter(
    level: Float, // -60 to 0 dB
    peakLevel: Float = level,
    modifier: Modifier = Modifier,
    segments: Int = 30,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val segGap = 2.dp.toPx()
        val segHeight = (h - segGap * (segments - 1)) / segments
        val cornerR = CornerRadius(1.5.dp.toPx())

        val normalizedLevel = ((level + 60f) / 60f).coerceIn(0f, 1f)
        val normalizedPeak = ((peakLevel + 60f) / 60f).coerceIn(0f, 1f)
        val litSegments = (normalizedLevel * segments).toInt()
        val peakSegment = (normalizedPeak * segments).toInt().coerceIn(0, segments - 1)

        for (i in 0 until segments) {
            val segBottom = h - i * (segHeight + segGap)
            val segTop = segBottom - segHeight
            val fraction = i.toFloat() / segments

            val litColor = when {
                fraction > 0.85f -> VuRed
                fraction > 0.65f -> VuYellow
                else -> VuGreen
            }

            val color = when {
                i < litSegments -> litColor
                i == peakSegment && peakLevel > -59f -> litColor.copy(alpha = 0.9f)
                else -> VuOff
            }

            drawRoundRect(
                color = color,
                topLeft = Offset(0f, segTop),
                size = Size(w, segHeight),
                cornerRadius = cornerR
            )

            // Highlight on lit segments
            if (i < litSegments) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.15f),
                    topLeft = Offset(0f, segTop),
                    size = Size(w, segHeight * 0.4f),
                    cornerRadius = cornerR
                )
            }
        }
    }
}
