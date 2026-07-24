package com.kouda.tactical.ui.minecraft

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kouda.tactical.network.minecraft.MinecraftServerInfo
import com.kouda.tactical.ui.theme.BgDark
import com.kouda.tactical.ui.theme.CardBg
import com.kouda.tactical.ui.theme.CardBorder
import com.kouda.tactical.ui.theme.NeonOrange
import com.kouda.tactical.ui.theme.PingRed
import com.kouda.tactical.ui.theme.TextDim
import com.kouda.tactical.ui.theme.TextMid
import com.kouda.tactical.ui.theme.fillColor
import com.kouda.tactical.ui.theme.pingColor

// Color acento para Minecraft
private val McGreen = Color(0xFF4CAF50)
private val McGreenDim = Color(0xFF1B5E20)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinecraftScreen(
    viewModel: MinecraftViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedServer by remember { mutableStateOf<MinecraftServerInfo?>(null) }

    if (selectedServer != null) {
        MinecraftServerDialog(
            server = selectedServer!!,
            onRemove = {
                viewModel.removeServer(selectedServer!!.ip, selectedServer!!.port)
                selectedServer = null
            },
            onDismiss = { selectedServer = null }
        )
    }

    if (showAddDialog) {
        MinecraftAddDialog(
            error = state.addError,
            onAdd = { viewModel.addServer(it) },
            onDismiss = { showAddDialog = false; viewModel.clearError() }
        )
    }

    Scaffold(
        containerColor = BgDark,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "MINECRAFT", color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp, letterSpacing = 2.sp
                            )
                            Text(
                                "Java Edition · Server Browser",
                                color = McGreen, fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDark),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextMid)
                        }
                    },
                    actions = {
                        IconButton(onClick = viewModel::refresh) {
                            Icon(Icons.Default.Refresh, "Refresh", tint = McGreen)
                        }
                    }
                )
                Box(
                    modifier = Modifier.fillMaxWidth().height(1.dp).background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, McGreen.copy(0.5f), Color.Transparent)
                        )
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = McGreen, shape = CircleShape
            ) {
                Icon(Icons.Default.Add, "Agregar servidor", tint = Color.Black)
            }
        }
    ) { padding ->
        when {
            state.isLoading && state.servers.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = McGreen, strokeWidth = 2.dp,
                            strokeCap = StrokeCap.Round
                        )
                        Text(
                            "Consultando servidores...", color = TextDim,
                            fontFamily = FontFamily.Monospace, fontSize = 13.sp
                        )
                    }
                }
            }
            state.servers.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text("⛏", fontSize = 48.sp)
                        Text(
                            "Sin servidores guardados",
                            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp
                        )
                        Text(
                            "Tocá + para agregar un servidor\nde Minecraft Java Edition",
                            color = TextDim, fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(state.servers, key = { _, s -> "${s.ip}:${s.port}" }) { index, server ->
                        AnimatedMcCard(
                            server = server,
                            index = index,
                            onClick = { selectedServer = server }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
fun AnimatedMcCard(server: MinecraftServerInfo, index: Int, onClick: () -> Unit) {
    val visibleState = remember { MutableTransitionState(false).also { it.targetState = false } }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 50L)
        visibleState.targetState = true
    }
    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 3 }
    ) {
        MinecraftServerCard(server = server, onClick = onClick)
    }
}

@Composable
fun MinecraftServerCard(server: MinecraftServerInfo, onClick: () -> Unit) {
    val pingColor = if (server.ping >= 0) pingColor(server.ping) else TextDim
    val fillRatio = server.fillRatio
    val fillC = if (server.isOnline) fillColor(fillRatio) else TextDim

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (server.isOnline) McGreen.copy(0.2f) else CardBorder,
                RoundedCornerShape(12.dp)
            )
            .background(CardBg)
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) }
    ) {
        Column {
            Row(
                modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Favicon
                if (server.faviconBase64 != null && server.isOnline) {
                    AsyncImage(
                        model = server.faviconBase64,
                        contentDescription = "Server icon",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (server.isOnline) McGreenDim else CardBorder),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (server.ping == -2) "..." else "⛏",
                            fontSize = if (server.ping == -2) 12.sp else 20.sp,
                            color = if (server.isOnline) McGreen else TextDim
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        server.name, color = Color.White, fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            server.address, color = TextDim, fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
                        )
                        if (server.isOnline && server.version.isNotBlank() && server.version != "—") {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(McGreen.copy(0.15f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    server.version, color = McGreen, fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                        if (server.modType != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFF7B1FA2).copy(0.2f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    server.modType, color = Color(0xFFCE93D8),
                                    fontSize = 9.sp, fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    when {
                        server.ping == -2 -> {
                            CircularProgressIndicator(
                                color = McGreen, strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        !server.isOnline -> {
                            Text("offline", color = TextDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                        else -> {
                            Text(
                                server.players, color = Color.White,
                                fontWeight = FontWeight.ExtraBold, fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                server.pingStr, color = pingColor, fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Barra de ocupación
            if (server.isOnline && server.maxPlayers > 0) {
                Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(CardBorder)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fillRatio.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(listOf(fillC.copy(0.5f), fillC))
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun MinecraftServerDialog(
    server: MinecraftServerInfo,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("⛏", fontSize = 20.sp)
                Column {
                    Text(server.name, color = Color.White, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(server.address, color = TextDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (server.isOnline) {
                    // Stats grid
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .background(BgDark).padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        McStatItem("JUGADORES", server.players, McGreen)
                        McStatItem("PING", server.pingStr, pingColor(server.ping))
                        McStatItem("VERSIÓN", server.version, TextMid)
                    }

                    if (server.modType != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF7B1FA2).copy(0.1f))
                                .border(1.dp, Color(0xFF7B1FA2).copy(0.3f), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Extension, null, tint = Color(0xFFCE93D8), modifier = Modifier.size(16.dp))
                            Text(
                                "${server.modType} · ${server.mods.size} mods",
                                color = Color(0xFFCE93D8), fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Text("El servidor no está respondiendo.", color = TextDim, fontSize = 13.sp)
                }

                // Botón eliminar
                androidx.compose.material3.OutlinedButton(
                    onClick = onRemove,
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF440000)),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF4444))
                ) {
                    Icon(Icons.Default.DeleteOutline, null, tint = Color(0xFFFF4444), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Eliminar servidor", fontWeight = FontWeight.Medium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar", color = TextDim) }
        }
    )
}

@Composable
fun MinecraftAddDialog(
    error: String?,
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("⛏", fontSize = 20.sp)
                Text("AGREGAR SERVIDOR", color = Color.White, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("IP del servidor (el puerto 25565 es el predeterminado):", color = TextMid, fontSize = 13.sp)
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = {
                        Text("play.servidor.com  o  ip:puerto", color = TextDim, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    },
                    singleLine = true,
                    isError = error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = McGreen,
                        unfocusedBorderColor = CardBorder,
                        cursorColor = McGreen,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                if (error != null) {
                    Text(error, color = PingRed, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (input.isNotBlank()) onAdd(input.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = McGreen),
                shape = RoundedCornerShape(8.dp)
            ) { Text("CONECTAR", color = Color.Black, fontWeight = FontWeight.ExtraBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = TextDim) }
        }
    )
}

@Composable
private fun McStatItem(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = TextDim, fontSize = 9.sp, letterSpacing = 1.sp, fontFamily = FontFamily.Monospace)
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
    }
}
