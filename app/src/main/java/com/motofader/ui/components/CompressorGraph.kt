package com.motofader.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motofader.audio.computeOutput
import com.motofader.ui.theme.Amber
import com.motofader.ui.theme.Cyan
import com.motofader.ui.theme.DimText
import com.motofader.ui.theme.SpectrumGrid
import kotlin.math.max
import kotlin.math.min

@Composable
fun CompressorGraph(
    threshold: Float,    // dB
    ratio: Float,        // x:1
    kneeWidth: Float,    // dB
    inputLevel: Float,   // current input dB for operating point
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = DimText, fontSize = 7.sp)

    Canvas(modifier = modifier.fillMaxSize()) {
        val pad = 24.dp.toPx()
        val plotLeft = pad
        val plotRight = size.width - 4.dp.toPx()
        val plotTop = 4.dp.toPx()
        val plotBottom = size.height - pad
        val plotW = plotRight - plotLeft
        val plotH = plotBottom - plotTop

        if (plotW <= 0 || plotH <= 0) return@Canvas

        val dbMin = -60f
        val dbMax = 0f
        val dbRange = dbMax - dbMin

        fun dbToX(db: Float) = plotLeft + ((db - dbMin) / dbRange) * plotW
        fun dbToY(db: Float) = plotBottom - ((db - dbMin) / dbRange) * plotH

        // Grid lines
        val gridDbs = listOf(0f, -10f, -20f, -30f, -40f, -50f, -60f)
        for (db in gridDbs) {
            val x = dbToX(db)
            val y = dbToY(db)
            // Horizontal
            drawLine(SpectrumGrid, Offset(plotLeft, y), Offset(plotRight, y), 1f)
            // Vertical
            drawLine(SpectrumGrid, Offset(x, plotTop), Offset(x, plotBottom), 1f)

            // Labels
            val label = if (db == 0f) "0" else "${db.toInt()}"
            val result = textMeasurer.measure(label, labelStyle)
            // Bottom axis
            drawText(result, topLeft = Offset(x - result.size.width / 2f, plotBottom + 4.dp.toPx()))
            // Left axis
            drawText(result, topLeft = Offset(plotLeft - result.size.width - 3.dp.toPx(), y - result.size.height / 2f))
        }

        // 1:1 reference line (unity diagonal)
        drawLine(
            color = Color(0xFF3A3A3A),
            start = Offset(dbToX(dbMin), dbToY(dbMin)),
            end = Offset(dbToX(dbMax), dbToY(dbMax)),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)),
        )

        // Threshold vertical marker
        val threshX = dbToX(threshold)
        drawLine(
            color = DimText,
            start = Offset(threshX, plotTop),
            end = Offset(threshX, plotBottom),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)),
        )

        // Knee region shading
        val kneeStart = threshold - kneeWidth / 2f
        val kneeEnd = threshold + kneeWidth / 2f
        if (kneeWidth > 0.5f) {
            val ksX = dbToX(max(kneeStart, dbMin))
            val keX = dbToX(min(kneeEnd, dbMax))
            drawRect(
                color = Amber.copy(alpha = 0.06f),
                topLeft = Offset(ksX, plotTop),
                size = androidx.compose.ui.geometry.Size(keX - ksX, plotH),
            )
        }

        // Compression curve
        val curvePath = Path()
        val steps = 200
        for (i in 0..steps) {
            val inputDb = dbMin + (dbRange * i / steps)
            val outputDb = computeOutput(inputDb, threshold, ratio, kneeWidth)
            val x = dbToX(inputDb)
            val y = dbToY(outputDb.coerceIn(dbMin, dbMax))
            if (i == 0) curvePath.moveTo(x, y) else curvePath.lineTo(x, y)
        }
        drawPath(
            curvePath,
            color = Amber,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )

        // Operating point
        if (inputLevel > -59f) {
            val outDb = computeOutput(inputLevel, threshold, ratio, kneeWidth)
            val opX = dbToX(inputLevel)
            val opY = dbToY(outDb.coerceIn(dbMin, dbMax))
            val refY = dbToY(inputLevel.coerceIn(dbMin, dbMax))

            // GR line (vertical from 1:1 to curve)
            if (outDb < inputLevel - 0.5f) {
                drawLine(
                    color = Cyan.copy(alpha = 0.5f),
                    start = Offset(opX, refY),
                    end = Offset(opX, opY),
                    strokeWidth = 1.5.dp.toPx(),
                )
            }

            // Operating point dot
            drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(opX, opY))
            drawCircle(color = Amber, radius = 2.5.dp.toPx(), center = Offset(opX, opY))
        }
    }
}
