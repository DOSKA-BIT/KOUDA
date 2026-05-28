package com.kouda.tactical

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import com.kouda.tactical.ui.theme.NeonOrange
import com.kouda.tactical.ui.theme.TextDim
import com.kouda.tactical.ui.theme.TextMid
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MenuScreen(onEnter: () -> Unit) {
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .drawBehind {
                val rad = Math.toRadians(glowAngle.toDouble())
                val cx = size.width / 2 + cos(rad).toFloat() * size.width * 0.4f
                val cy = size.height / 2 + sin(rad).toFloat() * size.height * 0.3f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(NeonOrange.copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(cx, cy), radius = size.width * 0.6f
                    ),
                    radius = size.width * 0.6f, center = Offset(cx, cy)
                )
                drawLine(
                    color = NeonOrange.copy(alpha = 0.04f),
                    start = Offset(0f, scanY * size.height),
                    end = Offset(size.width, scanY * size.height),
                    strokeWidth = 2f
                )
            },
        contentAlignment = Alignment.Center
    ) {
        GridDots()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(NeonOrange.copy(alpha = 0.15f))
                    .border(1.dp, NeonOrange.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    "GAME SERVER BROWSER", color = NeonOrange, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 3.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "KOUDA", color = Color.White, fontSize = 64.sp,
                fontWeight = FontWeight.ExtraBold, letterSpacing = 8.sp, lineHeight = 64.sp
            )
            Text(
                "TACTICAL", color = NeonOrange, fontSize = 28.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 12.sp
            )
            Spacer(modifier = Modifier.height(48.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatBadge("CS 1.6", "☑")
                StatBadge("CS:GO", "☑")
                StatBadge("TF2", "☑")
                StatBadge("HL", "☑")
            }
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = onEnter,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonOrange),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(Icons.Default.Radar, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "LAUNCH HUB", color = Color.Black, fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp, letterSpacing = 3.sp
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Source Engine Query Protocol", color = TextDim, fontSize = 11.sp,
                letterSpacing = 1.sp, fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun StatBadge(label: String, icon: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(icon, color = NeonOrange, fontSize = 12.sp)
        Text(label, color = TextMid, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
    }
}

@Composable
fun GridDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.03f, targetValue = 0.07f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "dotAlpha"
    )
    Box(modifier = Modifier.fillMaxSize().drawBehind {
        val spacing = 36.dp.toPx()
        var x = spacing / 2
        while (x < size.width) {
            var y = spacing / 2
            while (y < size.height) {
                drawCircle(NeonOrange.copy(alpha = alpha), 1.5f, Offset(x, y))
                y += spacing
            }
            x += spacing
        }
    })
}
