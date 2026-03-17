package com.motofader.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkSurfaceVariant = Color(0xFF2A2A2A)
val Amber = Color(0xFFFFB300)
val Cyan = Color(0xFF00BCD4)
val ErrorRed = Color(0xFFFF5252)

val VuGreen = Color(0xFF00E676)
val VuYellow = Color(0xFFFFEA00)
val VuRed = Color(0xFFFF1744)
val VuOff = Color(0xFF2A2A2A)

val FaderTrack = Color(0xFF1A1A1A)
val FaderTrackEdge = Color(0xFF3A3A3A)
val FaderCapTop = Color(0xFF909090)
val FaderCapBottom = Color(0xFF606060)
val FaderCapLine = Color(0xFFB0B0B0)
val FaderTickMark = Color(0xFF555555)
val FaderTickLabel = Color(0xFF888888)

val SpectrumBarLow = Color(0xFF00E676)
val SpectrumBarMid = Color(0xFFFFEA00)
val SpectrumBarHigh = Color(0xFFFF1744)
val SpectrumGrid = Color(0xFF2A2A2A)
val SpectrumLabel = Color(0xFF666666)
val SpectrumPeakHold = Color(0xFFFFFFFF)

// Semantic text/border colors used across screens
val MutedText = Color(0xFF888888)
val DimText = Color(0xFF555555)
val SubtleText = Color(0xFF666666)
val BorderDim = Color(0xFF444444)
val LightText = Color(0xFFAAAAAA)

private val DarkColorScheme = darkColorScheme(
    primary = Amber,
    secondary = Cyan,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    error = ErrorRed,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFAAAAAA),
    onError = Color.White,
)

private val MotoFaderTypography = Typography(
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 9.sp,
        letterSpacing = 0.5.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
    ),
)

@Composable
fun MotoFaderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = MotoFaderTypography,
        content = content
    )
}
