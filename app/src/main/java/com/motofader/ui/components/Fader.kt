package com.motofader.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motofader.ui.theme.FaderCapBottom
import com.motofader.ui.theme.FaderCapLine
import com.motofader.ui.theme.FaderCapTop
import com.motofader.ui.theme.FaderTickLabel
import com.motofader.ui.theme.FaderTickMark
import com.motofader.ui.theme.FaderTrack
import com.motofader.ui.theme.FaderTrackEdge

private val CAP_HEIGHT = 14.dp

@Composable
fun Fader(
    value: Float, // 0f to 1f
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    steps: Int = 0, // 0 = continuous
) {
    val textMeasurer = rememberTextMeasurer()
    var dragValue by remember(value) { mutableFloatStateOf(value) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(enabled, steps) {
                if (!enabled) return@pointerInput
                val capHalfH = (CAP_HEIGHT / 2).toPx()
                detectTapGestures { offset ->
                    val trackTop = capHalfH
                    val trackBottom = size.height - capHalfH
                    val trackRange = trackBottom - trackTop
                    val raw = 1f - ((offset.y - trackTop) / trackRange).coerceIn(0f, 1f)
                    val snapped = if (steps > 0) (raw * steps).toInt().toFloat() / steps else raw
                    dragValue = snapped
                    onValueChange(snapped)
                }
            }
            .pointerInput(enabled, steps) {
                if (!enabled) return@pointerInput
                val capHalfH = (CAP_HEIGHT / 2).toPx()
                detectDragGestures { change, _ ->
                    change.consume()
                    val trackTop = capHalfH
                    val trackBottom = size.height - capHalfH
                    val trackRange = trackBottom - trackTop
                    val raw = 1f - ((change.position.y - trackTop) / trackRange).coerceIn(0f, 1f)
                    val snapped = if (steps > 0) (raw * steps).toInt().toFloat() / steps else raw
                    dragValue = snapped
                    onValueChange(snapped)
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val centerX = w / 2f
        val trackWidth = 6.dp.toPx()
        val capWidth = 44.dp.toPx()
        val capHeight = CAP_HEIGHT.toPx()
        val capHalf = capHeight / 2f

        val trackTop = capHalf
        val trackBottom = h - capHalf
        val trackRange = trackBottom - trackTop

        // Track groove
        drawRoundRect(
            color = FaderTrack,
            topLeft = Offset(centerX - trackWidth / 2f, trackTop),
            size = Size(trackWidth, trackRange),
            cornerRadius = CornerRadius(2.dp.toPx())
        )
        // Track left edge highlight
        drawLine(
            color = FaderTrackEdge,
            start = Offset(centerX - trackWidth / 2f, trackTop),
            end = Offset(centerX - trackWidth / 2f, trackBottom),
            strokeWidth = 1f
        )
        // Track right edge highlight
        drawLine(
            color = FaderTrackEdge,
            start = Offset(centerX + trackWidth / 2f, trackTop),
            end = Offset(centerX + trackWidth / 2f, trackBottom),
            strokeWidth = 1f
        )

        // Tick marks and dB labels
        drawTickMarks(centerX, trackTop, trackRange, capWidth, textMeasurer)

        // Fader cap position
        val capY = trackBottom - dragValue * trackRange

        // Cap shadow
        drawRoundRect(
            color = Color(0x40000000),
            topLeft = Offset(centerX - capWidth / 2f + 1.dp.toPx(), capY - capHalf + 2.dp.toPx()),
            size = Size(capWidth, capHeight),
            cornerRadius = CornerRadius(3.dp.toPx())
        )

        // Cap body with gradient
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = if (enabled) listOf(FaderCapTop, FaderCapBottom) else listOf(Color(0xFF505050), Color(0xFF404040)),
                startY = capY - capHalf,
                endY = capY + capHalf
            ),
            topLeft = Offset(centerX - capWidth / 2f, capY - capHalf),
            size = Size(capWidth, capHeight),
            cornerRadius = CornerRadius(3.dp.toPx())
        )

        // Cap center line
        drawLine(
            color = if (enabled) FaderCapLine else Color(0xFF606060),
            start = Offset(centerX - capWidth * 0.35f, capY),
            end = Offset(centerX + capWidth * 0.35f, capY),
            strokeWidth = 1.5f
        )

        // Cap top highlight
        drawLine(
            color = Color(0x30FFFFFF),
            start = Offset(centerX - capWidth / 2f + 3.dp.toPx(), capY - capHalf + 1.dp.toPx()),
            end = Offset(centerX + capWidth / 2f - 3.dp.toPx(), capY - capHalf + 1.dp.toPx()),
            strokeWidth = 1f
        )
    }
}

private fun DrawScope.drawTickMarks(
    centerX: Float,
    trackTop: Float,
    trackRange: Float,
    capWidth: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val tickPositions = listOf(
        1.0f to "0",
        0.75f to "-6",
        0.5f to "-12",
        0.25f to "-24",
        0.1f to "-40",
        0.0f to "-\u221E",
    )
    val tickExtend = capWidth / 2f + 4.dp.toPx()
    val labelStyle = TextStyle(color = FaderTickLabel, fontSize = 8.sp)

    for ((pos, label) in tickPositions) {
        val y = trackTop + trackRange * (1f - pos)
        // Left tick
        drawLine(
            color = FaderTickMark,
            start = Offset(centerX - tickExtend, y),
            end = Offset(centerX - capWidth / 2f - 2.dp.toPx(), y),
            strokeWidth = 1f
        )
        // Right tick
        drawLine(
            color = FaderTickMark,
            start = Offset(centerX + capWidth / 2f + 2.dp.toPx(), y),
            end = Offset(centerX + tickExtend, y),
            strokeWidth = 1f
        )

        // Label (right side)
        val result = textMeasurer.measure(label, labelStyle)
        drawText(
            result,
            topLeft = Offset(centerX + tickExtend + 2.dp.toPx(), y - result.size.height / 2f)
        )
    }
}
