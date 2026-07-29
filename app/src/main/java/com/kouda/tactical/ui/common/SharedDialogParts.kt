package com.kouda.tactical.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kouda.tactical.ui.theme.BgDark
import com.kouda.tactical.ui.theme.CardBorder
import com.kouda.tactical.ui.theme.TextDim
import com.kouda.tactical.ui.theme.TextMid

/**
 * Estas piezas las usan tanto el diálogo de Source Engine como el de Minecraft
 * (y cualquier proveedor nuevo que se sume). La idea es que el "look" del diálogo
 * de detalle sea uno solo aunque los datos vengan de protocolos distintos.
 */

data class DialogStat(val label: String, val value: String, val color: Color)

@Composable
fun DialogStatsRow(stats: List<DialogStat>) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(BgDark).padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        stats.forEach { stat ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stat.label, color = TextDim, fontSize = 9.sp, letterSpacing = 1.sp, fontFamily = FontFamily.Monospace)
                Text(stat.value, color = stat.color, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun AutoWatchToggleRow(
    isWatched: Boolean,
    accentColor: Color,
    onToggle: () -> Unit,
    activeText: String = "Activa — te notifica cuando haya lugar",
    inactiveText: String = "Inactiva — no te notifica"
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isWatched) accentColor.copy(alpha = 0.1f) else BgDark)
            .border(1.dp, if (isWatched) accentColor.copy(alpha = 0.4f) else CardBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Default.NotificationsActive, null, tint = if (isWatched) accentColor else TextDim, modifier = Modifier.size(18.dp))
        Column(Modifier.weight(1f)) {
            Text("Vigilancia automática", color = if (isWatched) Color.White else TextMid, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(if (isWatched) activeText else inactiveText, color = if (isWatched) accentColor else TextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
        Switch(
            checked = isWatched, onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White, checkedTrackColor = accentColor,
                uncheckedThumbColor = TextDim, uncheckedTrackColor = CardBorder
            )
        )
    }
}

@Composable
fun ShareServerButton(onClick: () -> Unit, label: String = "COMPARTIR SERVIDOR") {
    Button(
        onClick = onClick, modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A6B2A)), shape = RoundedCornerShape(8.dp)
    ) {
        Icon(Icons.Default.Share, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = Color.White, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
    }
}

@Composable
fun RemoveServerButton(onClick: () -> Unit, label: String = "Eliminar servidor") {
    OutlinedButton(
        onClick = onClick, modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, Color(0xFF440000)), shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF4444))
    ) {
        Icon(Icons.Default.DeleteOutline, null, tint = Color(0xFFFF4444), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.Medium)
    }
}
