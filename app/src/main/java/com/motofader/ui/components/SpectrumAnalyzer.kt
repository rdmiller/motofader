package com.motofader.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motofader.audio.AudioCaptureManager
import com.motofader.ui.theme.SpectrumBarHigh
import com.motofader.ui.theme.SpectrumBarLow
import com.motofader.ui.theme.SpectrumBarMid
import com.motofader.ui.theme.SpectrumGrid
import com.motofader.ui.theme.SpectrumLabel
import com.motofader.ui.theme.SpectrumPeakHold

@Composable
fun SpectrumAnalyzer(
    bands: FloatArray,
    peaks: FloatArray,
    modifier: Modifier = Modifier,
    showLabels: Boolean = true,
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier.fillMaxSize()) {
        val leftPad = if (showLabels) 32.dp.toPx() else 8.dp.toPx()
        val rightPad = 8.dp.toPx()
        val topPad = 8.dp.toPx()
        val bottomPad = if (showLabels) 24.dp.toPx() else 8.dp.toPx()

        val plotLeft = leftPad
        val plotRight = size.width - rightPad
        val plotTop = topPad
        val plotBottom = size.height - bottomPad
        val plotWidth = plotRight - plotLeft
        val plotHeight = plotBottom - plotTop

        if (plotWidth <= 0 || plotHeight <= 0) return@Canvas

        // Grid lines
        val dbLines = listOf(0f, -10f, -20f, -30f, -40f, -50f, -60f)
        val labelStyle = TextStyle(color = SpectrumLabel, fontSize = 8.sp)

        for (db in dbLines) {
            val y = plotTop + plotHeight * (-db / 60f)
            drawLine(
                color = SpectrumGrid,
                start = Offset(plotLeft, y),
                end = Offset(plotRight, y),
                strokeWidth = 1f
            )
            if (showLabels) {
                val label = if (db == 0f) " 0" else "${db.toInt()}"
                val result = textMeasurer.measure(label, labelStyle)
                drawText(result, topLeft = Offset(plotLeft - result.size.width - 4.dp.toPx(), y - result.size.height / 2f))
            }
        }

        // Bars
        val bandCount = bands.size.coerceAtMost(AudioCaptureManager.BAND_COUNT)
        val barGap = 2.dp.toPx()
        val totalGaps = (bandCount - 1) * barGap
        val barWidth = (plotWidth - totalGaps) / bandCount

        for (i in 0 until bandCount) {
            val x = plotLeft + i * (barWidth + barGap)
            val normalizedLevel = ((bands[i] + 60f) / 60f).coerceIn(0f, 1f)
            val barHeight = normalizedLevel * plotHeight
            val barTop = plotBottom - barHeight

            // Bar with gradient
            if (barHeight > 0) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(SpectrumBarHigh, SpectrumBarMid, SpectrumBarLow),
                        startY = plotTop,
                        endY = plotBottom
                    ),
                    topLeft = Offset(x, barTop),
                    size = Size(barWidth, barHeight)
                )

                // Glow effect on top
                drawRect(
                    color = Color.White.copy(alpha = 0.12f),
                    topLeft = Offset(x, barTop),
                    size = Size(barWidth, (barHeight * 0.1f).coerceAtMost(3.dp.toPx()))
                )
            }

            // Peak hold marker
            if (i < peaks.size) {
                val peakNorm = ((peaks[i] + 60f) / 60f).coerceIn(0f, 1f)
                val peakY = plotBottom - peakNorm * plotHeight
                if (peaks[i] > -59f) {
                    drawRect(
                        color = SpectrumPeakHold,
                        topLeft = Offset(x, peakY),
                        size = Size(barWidth, 2.dp.toPx())
                    )
                }
            }

            // Frequency labels (sparse)
            if (showLabels && i < AudioCaptureManager.BAND_LABELS.size) {
                val label = AudioCaptureManager.BAND_LABELS[i]
                if (label.isNotEmpty()) {
                    val result = textMeasurer.measure(label, labelStyle)
                    drawText(
                        result,
                        topLeft = Offset(
                            x + barWidth / 2f - result.size.width / 2f,
                            plotBottom + 4.dp.toPx()
                        )
                    )
                }
            }
        }
    }
}
