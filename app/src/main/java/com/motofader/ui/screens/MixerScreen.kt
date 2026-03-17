package com.motofader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.motofader.ui.components.ChannelStrip
import com.motofader.ui.components.StereoVuMeter
import com.motofader.ui.theme.Amber
import com.motofader.ui.theme.DarkSurface
import com.motofader.ui.theme.DarkSurfaceVariant
import com.motofader.viewmodel.MixerViewModel

@Composable
fun MixerScreen(viewModel: MixerViewModel) {
    val channels by viewModel.channels.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSurface)
            .padding(top = 8.dp, bottom = 8.dp)
    ) {
        // Channel strips (scrollable)
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .horizontalScroll(rememberScrollState())
                .padding(start = 4.dp),
        ) {
            channels.forEach { channel ->
                ChannelStrip(
                    channel = channel,
                    onVolumeChange = { vol -> viewModel.setVolume(channel.streamType, vol) },
                    onMuteToggle = { viewModel.toggleMute(channel.streamType) },
                    modifier = Modifier.fillMaxHeight()
                )
            }
        }

        // Divider
        VerticalDivider(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 16.dp),
            thickness = 1.dp,
            color = DarkSurfaceVariant
        )

        // Master section with VU meters (isolated recomposition scope)
        Column(
            modifier = Modifier
                .width(72.dp)
                .fillMaxHeight()
                .padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "OUT",
                style = MaterialTheme.typography.labelMedium,
                color = Amber,
            )

            Spacer(modifier = Modifier.height(4.dp))

            StereoVuMeter(
                rmsLevelFlow = viewModel.rmsLevel,
                peakLevelFlow = viewModel.peakLevel,
                isActiveFlow = viewModel.isVisualizerActive,
                showPeakReadout = true,
                modifier = Modifier.weight(1f),
            )

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
