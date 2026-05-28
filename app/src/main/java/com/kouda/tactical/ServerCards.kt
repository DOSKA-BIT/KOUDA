package com.kouda.tactical

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kouda.tactical.data.ServerInfo
import com.kouda.tactical.ui.theme.CardBg
import com.kouda.tactical.ui.theme.CardBorder
import com.kouda.tactical.ui.theme.FillFull
import com.kouda.tactical.ui.theme.NeonOrange
import com.kouda.tactical.ui.theme.TextDim
import com.kouda.tactical.ui.theme.fillColor
import com.kouda.tactical.ui.theme.pingColor

@Composable
fun AnimatedServerCard(
    server: ServerInfo,
    index: Int,
    onClick: () -> Unit,
    onFavToggle: () -> Unit,
    onLongPress: () -> Unit
) {
    val visibleState = remember {
        MutableTransitionState(false).also { it.targetState = false }
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 40L)
        visibleState.targetState = true
    }
    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 3 }
    ) {
        ServerCard(
            server = server,
            onClick = onClick,
            onFavToggle = onFavToggle,
            onLongPress = onLongPress
        )
    }
}

@Composable
fun ServerCard(
    server: ServerInfo,
    onClick: () -> Unit,
    onFavToggle: () -> Unit,
    onLongPress: () -> Unit
) {
    val cardFillColor = fillColor(server.fillRatio)
    val cardPingColor = pingColor(server.ping)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() }, onLongPress = { onLongPress() })
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = if (server.isFav) BorderStroke(1.dp, NeonOrange.copy(0.35f))
                 else BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(NeonOrange.copy(0.15f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                server.country, color = NeonOrange, fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            server.name, color = Color.White, fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        "${server.map}  ·  ${server.ip}", color = TextDim, fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        server.players,
                        color = if (server.isFull) FillFull else Color.White,
                        fontWeight = FontWeight.ExtraBold, fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        server.pingStr, color = cardPingColor, fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onFavToggle, modifier = Modifier.size(28.dp)) {
                        Icon(
                            if (server.isFav) Icons.Default.Star else Icons.Default.StarBorder,
                            null,
                            tint = if (server.isFav) Color(0xFFFFCC00) else TextDim,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(CardBorder)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(server.fillRatio.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(listOf(cardFillColor.copy(0.5f), cardFillColor))
                        )
                )
            }
        }
    }
}
