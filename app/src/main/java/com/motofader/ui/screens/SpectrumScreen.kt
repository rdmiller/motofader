package com.motofader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.motofader.audio.SpectrumSpeed
import com.motofader.ui.components.SpectrumAnalyzer
import com.motofader.ui.components.StereoVuMeter
import com.motofader.ui.theme.Amber
import com.motofader.ui.theme.BorderDim
import com.motofader.ui.theme.Cyan
import com.motofader.ui.theme.DarkSurface
import com.motofader.ui.theme.DarkSurfaceVariant
import com.motofader.ui.theme.MutedText
import com.motofader.ui.theme.SubtleText
import com.motofader.ui.theme.VuGreen
import com.motofader.ui.theme.VuRed
import com.motofader.ui.theme.VuYellow
import com.motofader.viewmodel.MixerViewModel

@Composable
fun SpectrumScreen(viewModel: MixerViewModel) {
    val spectrumBands by viewModel.spectrumBands.collectAsState()
    val spectrumPeaks by viewModel.spectrumPeaks.collectAsState()
    val isActive by viewModel.isVisualizerActive.collectAsState()
    val speed by viewModel.spectrumSpeed.collectAsState()

    val peakLevel by viewModel.peakLevel.collectAsState()
    val rmsLevel by viewModel.rmsLevel.collectAsState()
    val peakHold by viewModel.peakHoldLevel.collectAsState()
    val leq by viewModel.leq.collectAsState()
    val crest by viewModel.crestFactor.collectAsState()
    val momentary by viewModel.momentaryLevel.collectAsState()
    val shortTerm by viewModel.shortTermLevel.collectAsState()
    val elapsed by viewModel.measurementElapsed.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSurface)
            .padding(8.dp)
    ) {
        // Header: VU meters + stats panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // L/R VU meters (isolated recomposition)
            StereoVuMeter(
                rmsLevelFlow = viewModel.rmsLevel,
                peakLevelFlow = viewModel.peakLevel,
                isActiveFlow = viewModel.isVisualizerActive,
                meterWidth = 20.dp,
                modifier = Modifier.width(48.dp),
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Stats panel
            Column(
                modifier = Modifier.weight(1f),
            ) {
                // Title row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "1/3 OCTAVE RTA",
                        style = MaterialTheme.typography.titleMedium,
                        color = Amber,
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Two-column stats layout
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Left column: instantaneous
                    Column(modifier = Modifier.weight(1f)) {
                        StatRow("PEAK", peakLevel, isActive)
                        StatRow("RMS", rmsLevel, isActive, Color.White)
                        StatRow("CREST", crest, isActive, Cyan, suffix = " dB", showSign = true)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Right column: windowed / accumulated
                    Column(modifier = Modifier.weight(1f)) {
                        StatRow("M 400ms", momentary, isActive, Color.White)
                        StatRow("S 3s", shortTerm, isActive, Color.White)
                        StatRow("Leq", leq, isActive, Cyan)
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Peak hold + elapsed + reset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("HOLD", style = MaterialTheme.typography.labelSmall, color = MutedText)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isActive && peakHold > -59f) String.format("%.1f", peakHold) else "--",
                        style = MaterialTheme.typography.labelMedium,
                        color = when {
                            peakHold > -3f -> VuRed
                            peakHold > -12f -> VuYellow
                            else -> VuGreen
                        },
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = formatElapsed(elapsed),
                        style = MaterialTheme.typography.labelSmall,
                        color = SubtleText,
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Reset button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, BorderDim, RoundedCornerShape(4.dp))
                            .clickable { viewModel.resetStats() }
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("RST", style = MaterialTheme.typography.labelMedium, color = com.motofader.ui.theme.LightText)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Speed selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "SPEED",
                style = MaterialTheme.typography.labelSmall,
                color = MutedText,
                modifier = Modifier.padding(end = 8.dp),
            )
            SpectrumSpeed.entries.forEach { s ->
                val selected = s == speed
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (selected) Amber.copy(alpha = 0.2f) else DarkSurfaceVariant)
                        .border(
                            1.dp,
                            if (selected) Amber else BorderDim,
                            RoundedCornerShape(4.dp)
                        )
                        .clickable { viewModel.setSpectrumSpeed(s) }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = s.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) Amber else MutedText,
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Spectrum analyzer
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            SpectrumAnalyzer(
                bands = spectrumBands,
                peaks = spectrumPeaks,
                showLabels = true,
            )
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: Float,
    isActive: Boolean,
    valueColor: Color = when {
        value > -3f -> VuRed
        value > -12f -> VuYellow
        else -> VuGreen
    },
    suffix: String = " dB",
    showSign: Boolean = false,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MutedText,
            modifier = Modifier.width(52.dp),
        )
        Text(
            text = if (isActive && value > -59f) {
                val fmt = String.format("%+6.1f", value)
                if (showSign) "$fmt$suffix" else String.format("%6.1f", value) + suffix
            } else {
                "  --  $suffix"
            },
            style = MaterialTheme.typography.labelMedium,
            color = if (isActive && value > -59f) valueColor else com.motofader.ui.theme.DimText,
            modifier = Modifier.width(64.dp),
        )
    }
}

private fun formatElapsed(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%d:%02d", m, s)
}
