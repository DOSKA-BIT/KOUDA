package com.kouda.tactical

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kouda.tactical.data.PlayerInfo
import com.kouda.tactical.data.ServerHistory
import com.kouda.tactical.data.ServerInfo
import com.kouda.tactical.ui.theme.BgDark
import com.kouda.tactical.ui.theme.CardBg
import com.kouda.tactical.ui.theme.CardBorder
import com.kouda.tactical.ui.theme.NeonOrange
import com.kouda.tactical.ui.theme.PingRed
import com.kouda.tactical.ui.theme.TextDim
import com.kouda.tactical.ui.theme.TextMid
import com.kouda.tactical.ui.theme.pingColor

@Composable
fun ServerOptionsDialog(
    server: ServerInfo,
    history: ServerHistory?,
    onScan: () -> Unit,
    onWatch: () -> Unit,
    onToggleAutoWatch: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        shape = RoundedCornerShape(16.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(server.name, color = Color.White, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(server.ip, color = TextDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(BgDark).padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("JUGADORES", server.players, NeonOrange)
                    StatItem("PING", server.pingStr, pingColor(server.ping))
                    StatItem("MAPA", server.map, TextMid)
                }
                if (history != null && history.snapshots.size >= 2) {
                    Column(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(BgDark).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("HISTORIAL", color = TextDim, fontSize = 9.sp, letterSpacing = 2.sp, fontFamily = FontFamily.Monospace)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            HistoryItem("CONSULTAS", "${history.snapshots.size}")
                            HistoryItem("PROM 24HS", "${history.recentAverage()} jug")
                            HistoryItem("HORA PICO", history.peakHour()?.let { "${it}:00hs" } ?: "---")
                        }
                        if (history.snapshots.size >= 4) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("ACTIVIDAD RECIENTE", color = TextDim, fontSize = 9.sp, letterSpacing = 2.sp, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(4.dp))
                            MiniBarChart(snapshots = history.snapshots.takeLast(12))
                        }
                    }
                }
                Button(onClick = onScan, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonOrange), shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.PersonSearch, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ESCANEAR JUGADORES", color = Color.Black, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                }
                Button(onClick = onShare, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A6B2A)), shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.Share, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("COMPARTIR SERVIDOR", color = Color.White, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                }
                if (server.isFull) {
                    Button(onClick = onWatch, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC0000)), shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Default.Visibility, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("VIGILAR SLOT AHORA", color = Color.White, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (server.autoWatch) NeonOrange.copy(0.1f) else BgDark)
                        .border(1.dp, if (server.autoWatch) NeonOrange.copy(0.4f) else CardBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.NotificationsActive, null,
                        tint = if (server.autoWatch) NeonOrange else TextDim, modifier = Modifier.size(18.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Vigilancia automatica",
                            color = if (server.autoWatch) Color.White else TextMid,
                            fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(
                            if (server.autoWatch) "Activa — te notifica cuando haya slot" else "Inactiva — no te notifica",
                            color = if (server.autoWatch) NeonOrange else TextDim,
                            fontSize = 10.sp, fontFamily = FontFamily.Monospace
                        )
                    }
                    Switch(
                        checked = server.autoWatch, onCheckedChange = { onToggleAutoWatch() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White, checkedTrackColor = NeonOrange,
                            uncheckedThumbColor = TextDim, uncheckedTrackColor = CardBorder
                        )
                    )
                }
                OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, Color(0xFF440000)), shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF4444))) {
                    Icon(Icons.Default.DeleteOutline, null, tint = Color(0xFFFF4444), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Eliminar servidor", fontWeight = FontWeight.Medium)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = TextDim) } }
    )
}

@Composable
fun PlayerScanDialog(isLoading: Boolean, players: List<PlayerInfo>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        containerColor = CardBg,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.PersonSearch, null, tint = NeonOrange, modifier = Modifier.size(20.dp))
                Text("OPERATIVOS", color = NeonOrange, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                if (!isLoading && players.isNotEmpty()) {
                    Spacer(modifier = Modifier.weight(1f))
                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(NeonOrange.copy(0.2f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("${players.size}", color = NeonOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        },
        text = {
            when {
                isLoading -> Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(color = NeonOrange, strokeWidth = 2.dp)
                        Text("Interceptando señal...", color = TextDim, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }
                players.isEmpty() -> Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                    Text("No se pudo obtener la lista.\nEl servidor bloquea consultas directas\ny no aparece en Gametracker.",
                        color = TextDim, textAlign = TextAlign.Center, fontSize = 13.sp)
                }
                else -> Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState())
                ) {
                    players.forEachIndexed { i, player ->
                        val isTop = i == 0
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(7.dp))
                                .background(if (isTop) NeonOrange.copy(0.1f) else BgDark)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("${i + 1}", color = if (isTop) NeonOrange else TextDim, fontSize = 11.sp,
                                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
                                modifier = Modifier.width(18.dp))
                            if (isTop) Icon(Icons.Default.EmojiEvents, null, tint = NeonOrange, modifier = Modifier.size(14.dp))
                            else Icon(Icons.Default.Person, null, tint = TextDim, modifier = Modifier.size(14.dp))
                            Text(player.name, color = if (isTop) Color.White else Color(0xFFCCCCCC),
                                fontWeight = if (isTop) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                            Text("${player.score}", color = if (isTop) NeonOrange else TextMid,
                                fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isLoading) TextButton(onClick = onDismiss) {
                Text("CERRAR", color = NeonOrange, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun AddServerDialog(onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    var ip by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AddCircleOutline, null, tint = NeonOrange, modifier = Modifier.size(20.dp))
                Text("ANADIR SERVIDOR", color = Color.White, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ingresa la IP y puerto del servidor:", color = TextMid, fontSize = 13.sp)
                OutlinedTextField(
                    value = ip, onValueChange = { ip = it; error = false },
                    placeholder = { Text("45.235.98.50:27015", color = TextDim, fontFamily = FontFamily.Monospace, fontSize = 13.sp) },
                    isError = error, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonOrange, unfocusedBorderColor = CardBorder,
                        cursorColor = NeonOrange, focusedTextColor = Color.White, unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                if (error) Text("Formato invalido. Usa IP:PUERTO", color = PingRed, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val t = ip.trim()
                    if (Regex("""^\d{1,3}(\.\d{1,3}){3}:\d{1,5}$""").matches(t)) onAdd(t) else error = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonOrange),
                shape = RoundedCornerShape(8.dp)
            ) { Text("VINCULAR", color = Color.Black, fontWeight = FontWeight.ExtraBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = TextDim) } }
    )
}

@Composable
fun SlotAlertDialog(serverName: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Check, null, tint = NeonOrange)
                Text("SLOT LIBRE", color = NeonOrange, fontWeight = FontWeight.ExtraBold)
            }
        },
        text = { Text("Hay un lugar disponible en $serverName.", color = Color.White) },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = NeonOrange)) {
                Text("ENTENDIDO", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    )
}
