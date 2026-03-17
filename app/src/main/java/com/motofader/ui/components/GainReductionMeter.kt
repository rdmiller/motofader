package com.motofader.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motofader.ui.theme.Cyan
import com.motofader.ui.theme.VuOff
import com.motofader.ui.theme.VuRed
import com.motofader.ui.theme.VuYellow

@Composable
fun GainReductionMeter(
    gainReduction: Float, // 0 to -30 dB (negative values = more reduction)
    modifier: Modifier = Modifier,
    maxGr: Float = -24f,
    segments: Int = 24,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = Color(0xFF555555), fontSize = 7.sp)

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val barWidth = w * 0.55f
        val barLeft = (w - barWidth) / 2f
        val topPad = 2.dp.toPx()
        val bottomPad = 2.dp.toPx()
        val barHeight = h - topPad - bottomPad
        val segGap = 2.dp.toPx()
        val segH = (barHeight - segGap * (segments - 1)) / segments
        val cornerR = CornerRadius(1.dp.toPx())

        // GR is 0 (no reduction) to maxGr (heavy reduction)
        // Meter reads from top (0 dB) downward
        val normalizedGr = (gainReduction / maxGr).coerceIn(0f, 1f)
        val litSegments = (normalizedGr * segments).toInt()

        for (i in 0 until segments) {
            val segTop = topPad + i * (segH + segGap)
            val fraction = i.toFloat() / segments

            val litColor = when {
                fraction > 0.75f -> VuRed
                fraction > 0.5f -> VuYellow
                else -> Cyan
            }

            val color = if (i < litSegments) litColor else VuOff

            drawRoundRect(
                color = color,
                topLeft = Offset(barLeft, segTop),
                size = Size(barWidth, segH),
                cornerRadius = cornerR,
            )
        }

        // Scale labels on right side
        val dbMarks = listOf(0f, -6f, -12f, -18f, -24f)
        val labelX = barLeft + barWidth + 2.dp.toPx()
        for (db in dbMarks) {
            val normalized = (db / maxGr).coerceIn(0f, 1f)
            val y = topPad + normalized * barHeight
            val label = if (db == 0f) "0" else "${db.toInt()}"
            val result = textMeasurer.measure(label, labelStyle)
            drawText(result, topLeft = Offset(labelX, y - result.size.height / 2f))
        }
    }
}
