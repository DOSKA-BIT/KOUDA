package com.kouda.tactical.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NeonOrange  = Color(0xFFFF6B00)
val NeonOrangeD = Color(0xFFCC5500)
val BgDark      = Color(0xFF0A0A0A)
val BgMid       = Color(0xFF111111)
val CardBg      = Color(0xFF181818)
val CardBorder  = Color(0xFF2A2A2A)
val TextDim     = Color(0xFF606060)
val TextMid     = Color(0xFF909090)

val PingGreen  = Color(0xFF00E676)
val PingYellow = Color(0xFFFFD600)
val PingRed    = Color(0xFFFF1744)

val FillLow    = Color(0xFF00E676)
val FillMid    = Color(0xFFFFD600)
val FillHigh   = Color(0xFFFF6B00)
val FillFull   = Color(0xFFFF1744)

private val KoudaColorScheme = darkColorScheme(
    primary         = NeonOrange,
    background      = BgDark,
    surface         = CardBg,
    onBackground    = Color.White,
    onSurface       = Color.White,
    secondary       = TextDim,
    outline         = CardBorder
)

fun pingColor(ping: Int): Color = when {
    ping < 60  -> PingGreen
    ping < 120 -> PingYellow
    else       -> PingRed
}

fun fillColor(ratio: Float): Color = when {
    ratio < 0.5f  -> FillLow
    ratio < 0.75f -> FillMid
    ratio < 1.0f  -> FillHigh
    else          -> FillFull
}

@Composable
fun KoudaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = KoudaColorScheme, content = content)
}
