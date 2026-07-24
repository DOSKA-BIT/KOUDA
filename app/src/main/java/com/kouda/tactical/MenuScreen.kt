package com.kouda.tactical

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kouda.tactical.ui.theme.BgDark
import com.kouda.tactical.ui.theme.CardBg
import com.kouda.tactical.ui.theme.CardBorder
import com.kouda.tactical.ui.theme.NeonOrange
import com.kouda.tactical.ui.theme.TextDim
import com.kouda.tactical.ui.theme.TextMid
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MenuScreen(
    onEnterSource: () -> Unit,
    onEnterMinecraft: () -> Unit,
    onEnterRoblox: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "menu_anim")

    val glowAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "glow"
    )
    val scanY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "scan"
    )
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.03f, targetValue = 0.07f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "dotAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .drawBehind {
                // Grid dots
                val spacing = 36.dp.toPx()
                var x = spacing / 2
                while (x < size.width) {
                    var y = spacing / 2
                    while (y < size.height) {
                        drawCircle(NeonOrange.copy(alpha = dotAlpha), 1.5f, Offset(x, y))
                        y += spacing
                    }
                    x += spacing
                }
                // Rotating glow
                val rad = Math.toRadians(glowAngle.toDouble())
                val cx = size.width / 2 + cos(rad).toFloat() * size.width * 0.4f
                val cy = size.height / 2 + sin(rad).toFloat() * size.height * 0.3f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(NeonOrange.copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(cx, cy), radius = size.width * 0.6f
                    ),
                    radius = size.width * 0.6f, center = Offset(cx, cy)
                )
                // Scanline
                drawLine(
                    color = NeonOrange.copy(alpha = 0.04f),
                    start = Offset(0f, scanY * size.height),
                    end = Offset(size.width, scanY * size.height),
                    strokeWidth = 2f
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(NeonOrange.copy(alpha = 0.12f))
                    .border(1.dp, NeonOrange.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    "UNIVERSAL GAME SERVER BROWSER",
                    color = NeonOrange, fontSize = 9.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Title
            Text(
                "KOUDA", color = Color.White, fontSize = 64.sp,
                fontWeight = FontWeight.ExtraBold, letterSpacing = 8.sp, lineHeight = 64.sp
            )
            Text(
                "TACTICAL", color = NeonOrange, fontSize = 28.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 12.sp
            )

            Spacer(modifier = Modifier.height(52.dp))

            // Category cards
            Text(
                "SELECCIONÁ UNA CATEGORÍA",
                color = TextDim, fontSize = 10.sp,
                letterSpacing = 2.sp, fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Source Engine card
            CategoryCard(
                emoji = "🎮",
                title = "SOURCE ENGINE",
                subtitle = "CS 1.6 · CS:GO · TF2 · Half-Life",
                accentColor = NeonOrange,
                games = listOf("CS 1.6", "CS:GO", "TF2", "HL"),
                onClick = onEnterSource
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Minecraft card
            CategoryCard(
                emoji = "⛏",
                title = "MINECRAFT",
                subtitle = "Java Edition · Server List Ping",
                accentColor = Color(0xFF4CAF50),
                games = listOf("Java Ed.", "SLP", "Forge", "Fabric"),
                onClick = onEnterMinecraft
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Roblox card
            CategoryCard(
                emoji = "🟥",
                title = "ROBLOX",
                subtitle = "Explorador de experiencias",
                accentColor = Color(0xFFE53935),
                games = listOf("Top jugados", "Buscar", "Favoritos"),
                onClick = onEnterRoblox
            )

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                "v2.0 · Open Source · DOSKA-BIT",
                color = TextDim, fontSize = 10.sp,
                letterSpacing = 1.sp, fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun CategoryCard(
    emoji: String,
    title: String,
    subtitle: String,
    accentColor: Color,
    games: List<String>,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, accentColor.copy(0.25f), RoundedCornerShape(14.dp))
            .background(CardBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Emoji icon
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 26.sp)
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    title, color = Color.White, fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp
                )
                Text(
                    subtitle, color = accentColor, fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                // Game tags
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    games.forEach { game ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(accentColor.copy(0.1f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                game, color = accentColor.copy(0.8f),
                                fontSize = 9.sp, fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Arrow
            Text("›", color = accentColor.copy(0.6f), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// Mantener compatibilidad con el onEnter original por si acaso
@Composable
fun MenuScreen(onEnter: () -> Unit) {
    MenuScreen(
        onEnterSource = onEnter,
        onEnterMinecraft = {},
        onEnterRoblox = {}
    )
}
