package com.motofader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.motofader.model.AudioChannel
import com.motofader.ui.theme.Amber
import com.motofader.ui.theme.BorderDim
import com.motofader.ui.theme.DarkSurfaceVariant
import com.motofader.ui.theme.DimText
import com.motofader.ui.theme.ErrorRed
import com.motofader.ui.theme.MutedText

@Composable
fun ChannelStrip(
    channel: AudioChannel,
    onVolumeChange: (Int) -> Unit,
    onMuteToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isMuted = channel.isMuted

    Column(
        modifier = modifier
            .width(64.dp)
            .padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Channel name
        Text(
            text = channel.shortName,
            style = MaterialTheme.typography.labelMedium,
            color = if (isMuted) DimText else Amber,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Fader
        Box(
            modifier = Modifier
                .weight(1f)
                .width(60.dp)
        ) {
            Fader(
                value = channel.normalizedVolume,
                onValueChange = { normalized ->
                    val vol = (normalized * channel.maxVolume).toInt().coerceIn(0, channel.maxVolume)
                    onVolumeChange(vol)
                },
                enabled = !isMuted,
                steps = channel.maxVolume,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Volume value display
        Text(
            text = "${channel.volume}",
            style = MaterialTheme.typography.labelMedium,
            color = if (isMuted) DimText else Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Mute button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (isMuted) ErrorRed.copy(alpha = 0.8f) else DarkSurfaceVariant)
                .border(1.dp, if (isMuted) ErrorRed else BorderDim, RoundedCornerShape(4.dp))
                .clickable { onMuteToggle() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "M",
                style = MaterialTheme.typography.labelLarge,
                color = if (isMuted) Color.White else MutedText,
            )
        }
    }
}
