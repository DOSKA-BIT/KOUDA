package com.kouda.tactical.ui.minecraft

import android.content.Intent
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kouda.tactical.network.minecraft.MinecraftServerInfo
import com.kouda.tactical.ui.common.AutoWatchToggleRow
import com.kouda.tactical.ui.common.DialogStat
import com.kouda.tactical.ui.common.DialogStatsRow
import com.kouda.tactical.ui.common.RemoveServerButton
import com.kouda.tactical.ui.common.ShareServerButton
import com.kouda.tactical.ui.theme.*

private val McGreen    = Color(0xFF4CAF50)
private val McGreenBg  = Color(0xFF1B5E20)
private val ModPurple  = Color(0xFFCE93D8)
private val ModPurpleBg = Color(0xFF4A148C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinecraftScreen(viewModel: MinecraftViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val clipboard = LocalClipboardManager.current
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedServer by remember { mutableStateOf<MinecraftServerInfo?>(null) }
    var copiedText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(copiedText) {
        if (copiedText != null) { kotlinx.coroutines.delay(2000); copiedText = null }
    }

    // Dialogo de detalles — se actualiza reactivamente desde el state
    if (selectedServer != null) {
        val ip = selectedServer!!.ip; val port = selectedServer!!.port
        val live = state.servers.find { it.ip == ip && it.port == port } ?: selectedServer!!
        MinecraftDetailDialog(
            server    = live,
            isWatched = viewModel.isWatched(ip, port),
            scanResult   = state.scanResult,
            isScanning   = state.isScanning,
            onScan       = { viewModel.scanPlayers(ip, port) },
            onClearScan  = { viewModel.clearScan() },
            onToggleWatch = { viewModel.toggleAutoWatch(ip, port, live.name) },
            onRemove     = { viewModel.removeServer(ip, port); selectedServer = null },
            onDismiss    = { selectedServer = null; viewModel.clearScan() }
        )
    }

    if (showAddDialog) {
        MinecraftAddDialog(
            error = state.addError,
            onAdd = { viewModel.addServer(it) },
            onDismiss = { showAddDialog = false; viewModel.clearError() }
        )
    }

    // Cerrar el add dialog automáticamente cuando se agrega sin error
    LaunchedEffect(state.servers.size) { if (state.addError == null) showAddDialog = false }

    Scaffold(
        containerColor = BgDark,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text("MINECRAFT", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, letterSpacing = 2.sp)
                            Text("Java Edition · Server Browser", color = McGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDark),
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = TextMid) }
                    },
                    actions = {
                        IconButton(onClick = viewModel::refresh) { Icon(Icons.Default.Refresh, "Actualizar", tint = McGreen) }
                    }
                )
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(
                    Brush.horizontalGradient(listOf(Color.Transparent, McGreen.copy(.5f), Color.Transparent))
                ))
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = McGreen, shape = CircleShape) {
                Icon(Icons.Default.Add, "Agregar", tint = Color.Black)
            }
        }
    ) { padding ->
        if (copiedText != null) {
            // Toast de copiado — se muestra en el tope del contenido
        }

        when {
            state.isLoading && state.servers.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        CircularProgressIndicator(color = McGreen, strokeWidth = 2.dp, strokeCap = StrokeCap.Round)
                        Text("Consultando servidores...", color = TextDim, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    }
                }
            }
            state.servers.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(32.dp)) {
                        Text("⛏", fontSize = 48.sp)
                        Text("Sin servidores guardados", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Tocá + para agregar un servidor\nde Minecraft Java Edition", color = TextDim, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (copiedText != null) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().background(McGreen.copy(.15f)).padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, null, tint = McGreen, modifier = Modifier.size(14.dp))
                                Text("Copiado: $copiedText", color = McGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                    itemsIndexed(state.servers, key = { _, s -> "${s.ip}:${s.port}" }) { index, server ->
                        AnimatedMcCard(server = server, index = index,
                            onClick = { selectedServer = server },
                            onLongPress = { clipboard.setText(AnnotatedString(server.address)); copiedText = server.address }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

// ─── Card animada ─────────────────────────────────────────────────────────────

@Composable
fun AnimatedMcCard(server: MinecraftServerInfo, index: Int, onClick: () -> Unit, onLongPress: () -> Unit) {
    val visible = remember { MutableTransitionState(false).also { it.targetState = false } }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(index * 50L); visible.targetState = true }
    AnimatedVisibility(visibleState = visible, enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 3 }) {
        MinecraftCard(server, onClick, onLongPress)
    }
}

@Composable
fun MinecraftCard(server: MinecraftServerInfo, onClick: () -> Unit, onLongPress: () -> Unit) {
    val fillC = fillColor(server.fillRatio)
    val pingC = if (server.ping >= 0) pingColor(server.ping) else TextDim

    Box(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, if (server.isOnline) McGreen.copy(.2f) else CardBorder, RoundedCornerShape(12.dp))
            .background(CardBg)
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }, onLongPress = { onLongPress() }) }
    ) {
        Column {
            Row(modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                // Favicon o ícono de bloque
                if (server.faviconBase64 != null && server.isOnline) {
                    AsyncImage(model = server.faviconBase64, contentDescription = null,
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)))
                } else {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).background(if (server.isOnline) McGreenBg else CardBorder), Alignment.Center) {
                        Text(when { server.isLoading -> "..." ; server.isOnline -> "⛏" ; else -> "✕" },
                            fontSize = if (server.isLoading) 11.sp else 20.sp,
                            color = if (server.isOnline) McGreen else TextDim)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(server.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(server.address, color = TextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        if (server.isOnline && server.version != "—") Tag(server.version, McGreen)
                        if (server.modType != null) Tag(server.modType, ModPurple)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    when {
                        server.isLoading -> CircularProgressIndicator(color = McGreen, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        !server.isOnline -> Text("offline", color = TextDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        else -> {
                            Text(server.players, color = if (server.isFull) FillFull else Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                            Text(server.pingStr, color = pingC, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            if (server.isOnline && server.maxPlayers > 0) {
                Box(Modifier.fillMaxWidth().height(3.dp).background(CardBorder)) {
                    Box(Modifier.fillMaxWidth(server.fillRatio.coerceIn(0f, 1f)).fillMaxHeight()
                        .background(Brush.horizontalGradient(listOf(fillC.copy(.5f), fillC))))
                }
            }
        }
    }
}

@Composable
private fun Tag(text: String, color: Color) {
    Box(Modifier.clip(RoundedCornerShape(3.dp)).background(color.copy(.15f)).padding(horizontal = 5.dp, vertical = 1.dp)) {
        Text(text, color = color, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

// ─── Diálogo de detalles (misma arquitectura que Source Engine) ───────────────

@Composable
fun MinecraftDetailDialog(
    server: MinecraftServerInfo,
    isWatched: Boolean,
    scanResult: List<String>?,
    isScanning: Boolean,
    onScan: () -> Unit,
    onClearScan: () -> Unit,
    onToggleWatch: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Diálogo de jugadores
    if (isScanning || scanResult != null) {
        MinecraftPlayerDialog(isScanning = isScanning, players = scanResult ?: emptyList(), onDismiss = onClearScan)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (server.faviconBase64 != null) {
                    AsyncImage(model = server.faviconBase64, contentDescription = null, modifier = Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)))
                } else {
                    Text("⛏", fontSize = 28.sp)
                }
                Column {
                    Text(server.name, color = Color.White, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(server.address, color = TextDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {

                // Stats — mismo componente que usa el diálogo de Source Engine
                DialogStatsRow(listOf(
                    DialogStat("JUGADORES", if (server.isOnline) server.players else "offline", if (server.isOnline) McGreen else TextDim),
                    DialogStat("PING", server.pingStr, if (server.ping >= 0) pingColor(server.ping) else TextDim),
                    DialogStat("VERSIÓN", server.version, TextMid)
                ))

                // MOTD si es distinto al nombre
                if (server.isOnline && server.motdRaw.isNotBlank() && server.motdRaw != server.name) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(BgDark).padding(10.dp),
                        verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("MOTD", color = TextDim, fontSize = 9.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp, modifier = Modifier.width(36.dp).padding(top = 2.dp))
                        Text(server.name, color = Color.White, fontSize = 12.sp, lineHeight = 17.sp)
                    }
                }

                // Badge de mods
                if (server.modType != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .background(ModPurpleBg.copy(.3f))
                            .border(1.dp, ModPurple.copy(.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Extension, null, tint = ModPurple, modifier = Modifier.size(16.dp))
                        Text(
                            if (server.mods.isEmpty()) server.modType
                            else "${server.modType} · ${server.mods.size} mods",
                            color = ModPurple, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Escanear jugadores
                Button(
                    onClick = onScan,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = McGreen),
                    shape = RoundedCornerShape(8.dp),
                    enabled = server.isOnline
                ) {
                    Icon(Icons.Default.PersonSearch, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (!server.isOnline) "SERVIDOR OFFLINE"
                        else if (server.playerSample.isEmpty() && server.curPlayers == 0) "ESCANEAR JUGADORES"
                        else "VER JUGADORES (${server.curPlayers})",
                        color = Color.Black, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp
                    )
                }

                // Compartir — mismo componente que Source Engine
                ShareServerButton(onClick = {
                    val text = "⛏ *${server.name}*\n🗺 Mapa: (Minecraft)\n👥 Jugadores: ${server.players}\n📡 Ping: ${server.pingStr}\n🔗 IP: ${server.address}\n\nConectate desde Kouda Tactical"
                    context.startActivity(Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) },
                        "Compartir servidor"
                    ))
                })

                // Vigilancia automática — mismo componente que Source Engine
                if (server.isOnline) {
                    AutoWatchToggleRow(
                        isWatched = isWatched,
                        accentColor = McGreen,
                        onToggle = onToggleWatch,
                        activeText = "Activa — notifica cuando haya lugar",
                        inactiveText = "Inactiva — no notifica"
                    )
                }

                // Eliminar — mismo componente que Source Engine
                RemoveServerButton(onClick = onRemove)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar", color = TextDim) } }
    )
}

// ─── Diálogo de jugadores ─────────────────────────────────────────────────────

@Composable
fun MinecraftPlayerDialog(isScanning: Boolean, players: List<String>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { if (!isScanning) onDismiss() },
        containerColor = CardBg,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.PersonSearch, null, tint = McGreen, modifier = Modifier.size(20.dp))
                Text("JUGADORES", color = McGreen, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                if (!isScanning && players.isNotEmpty()) {
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(McGreen.copy(.2f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("${players.size}", color = McGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        },
        text = {
            when {
                isScanning -> Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(color = McGreen, strokeWidth = 2.dp)
                        Text("Consultando...", color = TextDim, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }
                players.isEmpty() -> Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "El servidor no expone la lista de jugadores.\nEsto es configuración del servidor,\nno un error de la app.",
                        color = TextDim, textAlign = TextAlign.Center, fontSize = 13.sp
                    )
                }
                else -> Column(modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Muestra parcial — el servidor puede tener más jugadores.", color = TextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(4.dp))
                    players.forEachIndexed { i, name ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(7.dp))
                                .background(if (i == 0) McGreen.copy(.1f) else BgDark)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("${i + 1}", color = if (i == 0) McGreen else TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.width(18.dp))
                            if (i == 0) Icon(Icons.Default.EmojiEvents, null, tint = McGreen, modifier = Modifier.size(14.dp))
                            else Icon(Icons.Default.Person, null, tint = TextDim, modifier = Modifier.size(14.dp))
                            Text(name, color = if (i == 0) Color.White else Color(0xFFCCCCCC), fontWeight = if (i == 0) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isScanning) TextButton(onClick = onDismiss) { Text("CERRAR", color = McGreen, fontWeight = FontWeight.Bold) }
        }
    )
}

// ─── Diálogo de agregar servidor ──────────────────────────────────────────────

@Composable
fun MinecraftAddDialog(error: String?, onAdd: (String) -> Unit, onDismiss: () -> Unit) {
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
                Text("IP del servidor (puerto 25565 por defecto):", color = TextMid, fontSize = 13.sp)
                OutlinedTextField(
                    value = input, onValueChange = { input = it },
                    placeholder = { Text("play.servidor.com  o  ip:puerto", color = TextDim, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                    singleLine = true, isError = error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = McGreen, unfocusedBorderColor = CardBorder,
                        cursorColor = McGreen, focusedTextColor = Color.White, unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                if (error != null) Text(error, color = PingRed, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        },
        confirmButton = {
            Button(onClick = { if (input.isNotBlank()) onAdd(input.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = McGreen), shape = RoundedCornerShape(8.dp)) {
                Text("CONECTAR", color = Color.Black, fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = TextDim) } }
    )
}


