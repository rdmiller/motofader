package com.motofader.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.motofader.ui.theme.MutedText
import com.motofader.ui.theme.VuGreen
import com.motofader.ui.theme.VuRed
import com.motofader.ui.theme.VuYellow
import kotlinx.coroutines.flow.StateFlow

/**
 * L/R VU meter pair that collects its own state from flows,
 * isolating recomposition from the parent screen.
 * Note: Android Visualizer session 0 is mono — both meters show the same data.
 */
@Composable
fun StereoVuMeter(
    rmsLevelFlow: StateFlow<Float>,
    peakLevelFlow: StateFlow<Float>,
    isActiveFlow: StateFlow<Boolean>,
    modifier: Modifier = Modifier,
    meterWidth: Dp = 16.dp,
    showPeakReadout: Boolean = false,
) {
    val rmsLevel by rmsLevelFlow.collectAsState()
    val peakLevel by peakLevelFlow.collectAsState()
    val isActive by isActiveFlow.collectAsState()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            // Left VU
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .width(meterWidth)
                ) {
                    VuMeter(level = rmsLevel, peakLevel = peakLevel)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text("L", style = MaterialTheme.typography.labelSmall, color = MutedText)
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Right VU
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .width(meterWidth)
                ) {
                    VuMeter(level = rmsLevel, peakLevel = peakLevel)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text("R", style = MaterialTheme.typography.labelSmall, color = MutedText)
            }
        }

        if (showPeakReadout) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isActive) "${String.format("%.1f", peakLevel)} dB" else "-- dB",
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    peakLevel > -3f -> VuRed
                    peakLevel > -12f -> VuYellow
                    else -> VuGreen
                },
            )
        }
    }
}
