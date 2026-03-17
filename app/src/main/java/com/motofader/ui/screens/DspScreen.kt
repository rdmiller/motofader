package com.motofader.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.motofader.audio.computeOutput
import com.motofader.model.EQ_BAND_RANGES
import com.motofader.model.MBC_BAND_RANGES
import com.motofader.ui.components.CompressorGraph
import com.motofader.ui.components.GainReductionMeter
import com.motofader.ui.theme.Amber
import com.motofader.ui.theme.BorderDim
import com.motofader.ui.theme.Cyan
import com.motofader.ui.theme.DarkBackground
import com.motofader.ui.theme.DarkSurface
import com.motofader.ui.theme.DarkSurfaceVariant
import com.motofader.ui.theme.DimText
import com.motofader.ui.theme.ErrorRed
import com.motofader.ui.theme.MutedText
import com.motofader.ui.theme.SubtleText
import com.motofader.ui.theme.VuGreen
import com.motofader.ui.theme.VuRed
import com.motofader.viewmodel.MixerViewModel

@Composable
fun DspScreen(viewModel: MixerViewModel) {
    val available by viewModel.dspAvailable.collectAsState()
    val bypassed by viewModel.dspBypassed.collectAsState()
    val eqEnabled by viewModel.eqEnabled.collectAsState()
    val eqBands by viewModel.eqBands.collectAsState()
    val fullBand by viewModel.fullBandSettings.collectAsState()
    val mbcEnabled by viewModel.mbcEnabled.collectAsState()
    val mbcBands by viewModel.mbcBands.collectAsState()
    val limiter by viewModel.limiterSettings.collectAsState()
    val extras by viewModel.extrasSettings.collectAsState()
    val peakLevel by viewModel.peakLevel.collectAsState()

    val contentAlpha = if (bypassed) 0.4f else 1f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSurface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        if (!available) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("DynamicsProcessing unavailable", color = ErrorRed)
            }
            return@Column
        }

        // Master bypass
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (bypassed) DarkSurfaceVariant else Amber.copy(alpha = 0.15f))
                .border(1.dp, if (bypassed) BorderDim else Amber, RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = if (bypassed) "DSP BYPASSED" else "DSP ACTIVE",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (bypassed) MutedText else Amber,
                )
                Text(
                    text = "EQ \u2192 Compressor \u2192 Limiter",
                    style = MaterialTheme.typography.labelSmall,
                    color = SubtleText,
                )
            }
            Switch(
                checked = !bypassed,
                onCheckedChange = { viewModel.setDspBypassed(!it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Amber,
                    checkedTrackColor = Amber.copy(alpha = 0.3f),
                    uncheckedThumbColor = MutedText,
                    uncheckedTrackColor = DarkSurfaceVariant,
                ),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // All sections dimmed when bypassed
        Column(modifier = Modifier.alpha(contentAlpha)) {

            // === EQ SECTION ===
            var eqExpanded by rememberSaveable { mutableStateOf(true) }
            SectionHeader("EQUALIZER", eqExpanded, { eqExpanded = it }, eqEnabled, { viewModel.setEqEnabled(it) })

            AnimatedVisibility(visible = eqExpanded) {
                Column(modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
                    eqBands.forEachIndexed { i, band ->
                        val range = if (i < EQ_BAND_RANGES.size) EQ_BAND_RANGES[i] else ""
                        ParamSlider(
                            label = band.label,
                            subtitle = range,
                            value = band.gain,
                            range = -12f..12f,
                            format = { "%+.1f dB".format(it) },
                            onValueChange = { viewModel.updateEqBand(i, it) },
                            enabled = eqEnabled && !bypassed,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // === FULL-BAND COMPRESSOR SECTION ===
            var fbExpanded by rememberSaveable { mutableStateOf(true) }
            SectionHeader("COMPRESSOR", fbExpanded, { fbExpanded = it }, fullBand.enabled, {
                viewModel.updateFullBand(fullBand.copy(enabled = it))
            })

            AnimatedVisibility(visible = fbExpanded) {
                Column(modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
                    // Graph + GR meter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    ) {
                        // Compressor transfer curve
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkBackground)
                                .border(1.dp, DarkSurfaceVariant, RoundedCornerShape(6.dp))
                        ) {
                            CompressorGraph(
                                threshold = fullBand.threshold,
                                ratio = fullBand.ratio,
                                kneeWidth = fullBand.kneeWidth,
                                inputLevel = peakLevel,
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // GR meter with ballistics
                        val rawGr = if (fullBand.enabled && peakLevel > -59f) {
                            val out = computeOutput(peakLevel, fullBand.threshold, fullBand.ratio, fullBand.kneeWidth)
                            (out - peakLevel).coerceIn(-24f, 0f)
                        } else 0f
                        var smoothedGr by remember { mutableFloatStateOf(0f) }
                        smoothedGr = if (rawGr < smoothedGr) {
                            rawGr  // instant attack (GR is negative, so "less" = more reduction)
                        } else {
                            (smoothedGr + GR_RELEASE_RATE).coerceAtMost(rawGr) // smooth release back toward 0
                        }
                        val gr = smoothedGr

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("GR", style = MaterialTheme.typography.labelSmall, color = Cyan)
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .aspectRatio(0.22f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(DarkBackground)
                                    .border(1.dp, DarkSurfaceVariant, RoundedCornerShape(4.dp))
                            ) {
                                GainReductionMeter(gainReduction = gr)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (gr < -0.5f) String.format("%.1f", gr) else "0.0",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (gr < -6f) VuRed else Cyan,
                            )
                            Text("dB", style = MaterialTheme.typography.labelSmall, color = DimText)
                        }
                    }

                    // Sliders
                    ParamSlider("THRESH", fullBand.threshold, -60f..0f,
                        { "%.1f dB".format(it) },
                        { viewModel.updateFullBand(fullBand.copy(threshold = it)) },
                        fullBand.enabled && !bypassed)

                    ParamSlider("RATIO", fullBand.ratio, 1f..20f,
                        { "%.1f:1".format(it) },
                        { viewModel.updateFullBand(fullBand.copy(ratio = it)) },
                        fullBand.enabled && !bypassed)

                    ParamSlider("ATTACK", fullBand.attackTime, 0.1f..100f,
                        { if (it < 10) "%.1f ms".format(it) else "%.0f ms".format(it) },
                        { viewModel.updateFullBand(fullBand.copy(attackTime = it)) },
                        fullBand.enabled && !bypassed)

                    ParamSlider("RELEASE", fullBand.releaseTime, 10f..1000f,
                        { "%.0f ms".format(it) },
                        { viewModel.updateFullBand(fullBand.copy(releaseTime = it)) },
                        fullBand.enabled && !bypassed)

                    ParamSlider("KNEE", fullBand.kneeWidth, 0f..30f,
                        { "%.1f dB".format(it) },
                        { viewModel.updateFullBand(fullBand.copy(kneeWidth = it)) },
                        fullBand.enabled && !bypassed)

                    ParamSlider("MAKEUP", fullBand.makeupGain, -12f..12f,
                        { "%+.1f dB".format(it) },
                        { viewModel.updateFullBand(fullBand.copy(makeupGain = it)) },
                        fullBand.enabled && !bypassed)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // === MULTIBAND COMPRESSOR SECTION ===
            var mbcExpanded by rememberSaveable { mutableStateOf(false) }
            SectionHeader("MULTIBAND", mbcExpanded, { mbcExpanded = it }, mbcEnabled, { viewModel.setMbcEnabled(it) })

            if (fullBand.enabled && mbcEnabled) {
                Text(
                    text = "  Full-band compressor overrides multiband settings",
                    style = MaterialTheme.typography.labelSmall,
                    color = SubtleText,
                    modifier = Modifier.padding(start = 12.dp, top = 2.dp),
                )
            }

            AnimatedVisibility(visible = mbcExpanded) {
                Column(modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
                    mbcBands.forEachIndexed { i, band ->
                        var bandExpanded by rememberSaveable { mutableStateOf(false) }
                        val bandRange = if (i < MBC_BAND_RANGES.size) MBC_BAND_RANGES[i] else ""
                        val dimmed = fullBand.enabled

                        // Band header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { bandExpanded = !bandExpanded }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = if (bandExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null,
                                tint = MutedText,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = band.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (band.enabled && !dimmed) Cyan else DimText,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = bandRange,
                                style = MaterialTheme.typography.labelSmall,
                                color = DimText,
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "%.0f:1  %.0fdB".format(band.ratio, band.threshold),
                                style = MaterialTheme.typography.labelSmall,
                                color = DimText,
                            )
                        }

                        AnimatedVisibility(visible = bandExpanded) {
                            Column(
                                modifier = Modifier
                                    .padding(start = 16.dp)
                                    .alpha(if (dimmed) 0.4f else 1f)
                            ) {
                                ParamSlider("THRESH", band.threshold, -60f..0f,
                                    { "%.1f dB".format(it) },
                                    { viewModel.updateMbcBand(i, band.copy(threshold = it)) },
                                    mbcEnabled && !bypassed && !dimmed)

                                ParamSlider("RATIO", band.ratio, 1f..20f,
                                    { "%.1f:1".format(it) },
                                    { viewModel.updateMbcBand(i, band.copy(ratio = it)) },
                                    mbcEnabled && !bypassed && !dimmed)

                                ParamSlider("ATTACK", band.attackTime, 0.1f..100f,
                                    { if (it < 10) "%.1f ms".format(it) else "%.0f ms".format(it) },
                                    { viewModel.updateMbcBand(i, band.copy(attackTime = it)) },
                                    mbcEnabled && !bypassed && !dimmed)

                                ParamSlider("RELEASE", band.releaseTime, 10f..1000f,
                                    { "%.0f ms".format(it) },
                                    { viewModel.updateMbcBand(i, band.copy(releaseTime = it)) },
                                    mbcEnabled && !bypassed && !dimmed)

                                ParamSlider("KNEE", band.kneeWidth, 0f..30f,
                                    { "%.1f dB".format(it) },
                                    { viewModel.updateMbcBand(i, band.copy(kneeWidth = it)) },
                                    mbcEnabled && !bypassed && !dimmed)

                                ParamSlider("PRE", band.preGain, -12f..12f,
                                    { "%+.1f dB".format(it) },
                                    { viewModel.updateMbcBand(i, band.copy(preGain = it)) },
                                    mbcEnabled && !bypassed && !dimmed)

                                ParamSlider("POST", band.postGain, -12f..12f,
                                    { "%+.1f dB".format(it) },
                                    { viewModel.updateMbcBand(i, band.copy(postGain = it)) },
                                    mbcEnabled && !bypassed && !dimmed)
                            }
                        }

                        if (i < mbcBands.lastIndex) {
                            HorizontalDivider(color = DarkSurfaceVariant, thickness = 1.dp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // === LIMITER SECTION ===
            var limExpanded by rememberSaveable { mutableStateOf(true) }
            SectionHeader("LIMITER", limExpanded, { limExpanded = it }, limiter.enabled, {
                viewModel.updateLimiter(limiter.copy(enabled = it))
            })

            AnimatedVisibility(visible = limExpanded) {
                Column(modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
                    ParamSlider("THRESH", limiter.threshold, -60f..0f,
                        { "%.1f dB".format(it) },
                        { viewModel.updateLimiter(limiter.copy(threshold = it)) },
                        limiter.enabled && !bypassed)

                    ParamSlider("RATIO", limiter.ratio, 1f..50f,
                        { "%.1f:1".format(it) },
                        { viewModel.updateLimiter(limiter.copy(ratio = it)) },
                        limiter.enabled && !bypassed)

                    ParamSlider("ATTACK", limiter.attackTime, 0.1f..50f,
                        { if (it < 10) "%.1f ms".format(it) else "%.0f ms".format(it) },
                        { viewModel.updateLimiter(limiter.copy(attackTime = it)) },
                        limiter.enabled && !bypassed)

                    ParamSlider("RELEASE", limiter.releaseTime, 10f..500f,
                        { "%.0f ms".format(it) },
                        { viewModel.updateLimiter(limiter.copy(releaseTime = it)) },
                        limiter.enabled && !bypassed)

                    ParamSlider("GAIN", limiter.postGain, -12f..12f,
                        { "%+.1f dB".format(it) },
                        { viewModel.updateLimiter(limiter.copy(postGain = it)) },
                        limiter.enabled && !bypassed)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // === EXTRAS SECTION ===
            var extExpanded by rememberSaveable { mutableStateOf(false) }
            SectionHeader("EXTRAS", extExpanded, { extExpanded = it })

            AnimatedVisibility(visible = extExpanded) {
                Column(modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
                    // Bass Boost
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("BASS BOOST", style = MaterialTheme.typography.labelMedium,
                            color = if (extras.bassBoostEnabled) Cyan else MutedText,
                            modifier = Modifier.width(90.dp))
                        Switch(
                            checked = extras.bassBoostEnabled,
                            onCheckedChange = { viewModel.updateExtras(extras.copy(bassBoostEnabled = it)) },
                            colors = switchColors(),
                        )
                    }
                    ParamSlider("STRENGTH", extras.bassBoostStrength.toFloat(), 0f..1000f,
                        { "${(it / 10).toInt()}%" },
                        { viewModel.updateExtras(extras.copy(bassBoostStrength = it.toInt())) },
                        extras.bassBoostEnabled && !bypassed)

                    Spacer(modifier = Modifier.height(4.dp))

                    // Virtualizer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("VIRTUALIZER", style = MaterialTheme.typography.labelMedium,
                            color = if (extras.virtualizerEnabled) Cyan else MutedText,
                            modifier = Modifier.width(90.dp))
                        Switch(
                            checked = extras.virtualizerEnabled,
                            onCheckedChange = { viewModel.updateExtras(extras.copy(virtualizerEnabled = it)) },
                            colors = switchColors(),
                        )
                    }
                    ParamSlider("STRENGTH", extras.virtualizerStrength.toFloat(), 0f..1000f,
                        { "${(it / 10).toInt()}%" },
                        { viewModel.updateExtras(extras.copy(virtualizerStrength = it.toInt())) },
                        extras.virtualizerEnabled && !bypassed)

                    Spacer(modifier = Modifier.height(4.dp))

                    // Loudness Enhancer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("LOUDNESS", style = MaterialTheme.typography.labelMedium,
                            color = if (extras.loudnessEnabled) Cyan else MutedText,
                            modifier = Modifier.width(90.dp))
                        Switch(
                            checked = extras.loudnessEnabled,
                            onCheckedChange = { viewModel.updateExtras(extras.copy(loudnessEnabled = it)) },
                            colors = switchColors(),
                        )
                    }
                    ParamSlider("GAIN", extras.loudnessGain.toFloat(), 0f..3000f,
                        { "%.1f dB".format(it / 100f) },
                        { viewModel.updateExtras(extras.copy(loudnessGain = it.toInt())) },
                        extras.loudnessEnabled && !bypassed)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// --- Reusable composables ---

@Composable
private fun SectionHeader(
    title: String,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    enabled: Boolean? = null,
    onEnabledChange: ((Boolean) -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(DarkBackground)
            .clickable { onExpandChange(!expanded) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = Amber,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = Amber,
            )
        }
        if (enabled != null && onEnabledChange != null) {
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                colors = switchColors(),
            )
        }
    }
}

@Composable
private fun ParamSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    format: (Float) -> String,
    onValueChange: (Float) -> Unit,
    enabled: Boolean = true,
    subtitle: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.width(if (subtitle != null) 64.dp else 52.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (enabled) MutedText else DimText,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = SubtleText,
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = if (enabled) Amber else DimText,
                activeTrackColor = if (enabled) Amber.copy(alpha = 0.6f) else BorderDim,
                inactiveTrackColor = DarkSurfaceVariant,
                disabledThumbColor = BorderDim,
                disabledActiveTrackColor = Color(0xFF333333),
                disabledInactiveTrackColor = DarkSurfaceVariant,
            ),
        )
        Text(
            text = format(value),
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) Color.White else DimText,
            modifier = Modifier.width(64.dp),
        )
    }
}

private const val GR_RELEASE_RATE = 0.8f // dB per frame, smooth return toward 0

@Composable
private fun switchColors() = SwitchDefaults.colors(
    checkedThumbColor = VuGreen,
    checkedTrackColor = VuGreen.copy(alpha = 0.3f),
    uncheckedThumbColor = MutedText,
    uncheckedTrackColor = DarkSurfaceVariant,
)
