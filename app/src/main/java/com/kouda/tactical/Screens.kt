package com.kouda.tactical

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
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
import com.kouda.tactical.data.GameFilter
import com.kouda.tactical.data.PlayerInfo
import com.kouda.tactical.data.ServerHistory
import com.kouda.tactical.data.ServerInfo
import com.kouda.tactical.data.ServerSnapshot
import com.kouda.tactical.data.SortMode
import com.kouda.tactical.ui.theme.BgDark
import com.kouda.tactical.ui.theme.CardBg
import com.kouda.tactical.ui.theme.CardBorder
import com.kouda.tactical.ui.theme.FillFull
import com.kouda.tactical.ui.theme.NeonOrange
import com.kouda.tactical.ui.theme.PingRed
import com.kouda.tactical.ui.theme.TextDim
import com.kouda.tactical.ui.theme.TextMid
import com.kouda.tactical.ui.theme.fillColor
import com.kouda.tactical.ui.theme.pingColor
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
                    fontWeight = FontWeight.Bold, letterSpacing = 3.sp, fontFamily = FontFamily.Monospace
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerListScreen(viewModel: KoudaViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val servers = remember(state.servers, state.currentFilter, state.sortMode) {
        viewModel.filteredAndSorted()
    }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedServer by remember { mutableStateOf<ServerInfo?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var copiedIp by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(copiedIp) {
        if (copiedIp != null) {
            kotlinx.coroutines.delay(2000)
            copiedIp = null
        }
    }

    if (state.slotAlert != null) {
        AlertDialog(
            onDismissRequest = viewModel::clearAlert,
            containerColor = CardBg,
            shape = RoundedCornerShape(16.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.NotificationsActive, null, tint = NeonOrange)
                    Text("SLOT LIBRE", color = NeonOrange, fontWeight = FontWeight.ExtraBold)
                }
            },
            text = {
                Text("Hay un lugar disponible en ${state.slotAlert}.", color = Color.White)
            },
            confirmButton = {
                Button(
                    onClick = viewModel::clearAlert,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonOrange)
                ) {
                    Text("ENTENDIDO", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (selectedServer != null) {
        val selectedIp = selectedServer!!.ip
        val liveServer = servers.find { it.ip == selectedIp }
            ?: state.servers.find { it.ip == selectedIp }
        if (liveServer != null) {
            ServerOptionsDialog(
                server = liveServer,
                history = viewModel.getHistory(selectedIp),
                onScan = {
                    selectedServer = null
                    viewModel.scanPlayers(selectedIp)
                },
                onWatch = {
                    selectedServer = null
                    viewModel.watchSlot(selectedIp, liveServer.name)
                },
                onToggleAutoWatch = { viewModel.toggleAutoWatch(selectedIp) },
                onShare = {
                    val text = "🎮 ${liveServer.name}\n🗺 Mapa: ${liveServer.map}\n👥 Jugadores: ${liveServer.players}\n📡 Ping: ${liveServer.pingStr}\n🌍 Pais: ${liveServer.country}\n🔗 IP: ${liveServer.ip}\n\nConectate desde Kouda Tactical"
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(intent, "Compartir servidor"))
                },
                onDelete = {
                    selectedServer = null
                    viewModel.removeServer(selectedIp)
                },
                onDismiss = { selectedServer = null }
            )
        }
    }

    if (state.isScanning || state.scanResult != null) {
        PlayerScanDialog(
            isLoading = state.isScanning,
            players = state.scanResult ?: emptyList(),
            onDismiss = viewModel::clearScanResult
        )
    }

    if (showAddDialog) {
        AddServerDialog(
            onAdd = { ip -> viewModel.addServer(ip); showAddDialog = false },
            onDismiss = { showAddDialog = false }
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
                                "KOUDA TACTICAL", color = Color.White,
                                fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, letterSpacing = 2.sp
                            )
                            if (state.totalOnline > 0) {
                                Text(
                                    "${state.totalOnline} jugadores online", color = NeonOrange,
                                    fontSize = 11.sp, fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDark),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextMid)
                        }
                    },
                    actions = {
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, "Sort", tint = TextMid)
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                containerColor = CardBg
                            ) {
                                SortMode.entries.forEach { mode ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                mode.label,
                                                color = if (state.sortMode == mode) NeonOrange else Color.White,
                                                fontWeight = if (state.sortMode == mode) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        leadingIcon = {
                                            if (state.sortMode == mode)
                                                Icon(Icons.Default.Check, null, tint = NeonOrange, modifier = Modifier.size(16.dp))
                                        },
                                        onClick = { viewModel.setSortMode(mode); showSortMenu = false }
                                    )
                                }
                            }
                        }
                        IconButton(onClick = viewModel::refresh) {
                            Icon(Icons.Default.Refresh, "Refresh", tint = NeonOrange)
                        }
                    }
                )
                Box(
                    modifier = Modifier.fillMaxWidth().height(1.dp).background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, NeonOrange.copy(0.5f), Color.Transparent)
                        )
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = NeonOrange,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, "Add server", tint = Color.Black)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            GameFilterRow(
                currentFilter = state.currentFilter,
                onFilterSelected = viewModel::setFilter
            )
            if (state.watchingIp != null) {
                WatchingBanner(ip = state.watchingIp!!, onCancel = viewModel::cancelWatch)
            }
            if (copiedIp != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NeonOrange.copy(0.15f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, null, tint = NeonOrange, modifier = Modifier.size(14.dp))
                    Text("Copiado: $copiedIp", color = NeonOrange, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
            when {
                state.isLoading && servers.isEmpty() -> LoadingState()
                servers.isEmpty() && !state.isLoading -> EmptyState()
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(servers, key = { _, s -> s.ip }) { index, server ->
                            AnimatedServerCard(
                                server = server,
                                index = index,
                                onClick = { selectedServer = server },
                                onFavToggle = { viewModel.toggleFavorite(server.ip) },
                                onLongPress = {
                                    clipboard.setText(AnnotatedString(server.ip))
                                    copiedIp = server.ip
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(72.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
fun GameFilterRow(currentFilter: GameFilter, onFilterSelected: (GameFilter) -> Unit) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GameFilter.entries.forEach { filter ->
            val selected = currentFilter == filter
            FilterChip(
                selected = selected,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        filter.label,
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal,
                        fontSize = 12.sp
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NeonOrange, selectedLabelColor = Color.Black,
                    containerColor = CardBg, labelColor = TextMid
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true, selected = selected,
                    selectedBorderColor = Color.Transparent, borderColor = CardBorder
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

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
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        server.players,
                        color = if (server.isFull) FillFull else Color.White,
                        fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, fontFamily = FontFamily.Monospace
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
                        .background(Brush.horizontalGradient(listOf(cardFillColor.copy(0.5f), cardFillColor)))
                )
            }
        }
    }
}

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
                Text(
                    server.name, color = Color.White, fontWeight = FontWeight.ExtraBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(server.ip, color = TextDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(BgDark)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("JUGADORES", server.players, NeonOrange)
                    StatItem("PING", server.pingStr, pingColor(server.ping))
                    StatItem("MAPA", server.map, TextMid)
                }
                if (history != null && history.snapshots.size >= 2) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BgDark)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "HISTORIAL", color = TextDim, fontSize = 9.sp,
                            letterSpacing = 2.sp, fontFamily = FontFamily.Monospace
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            HistoryItem("CONSULTAS", "${history.snapshots.size}")
                            HistoryItem("PROM 24HS", "${history.recentAverage()} jug")
                            HistoryItem("HORA PICO", history.peakHour()?.let { "${it}:00hs" } ?: "---")
                        }
                        if (history.snapshots.size >= 4) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "ACTIVIDAD RECIENTE", color = TextDim, fontSize = 9.sp,
                                letterSpacing = 2.sp, fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            MiniBarChart(snapshots = history.snapshots.takeLast(12))
                        }
                    }
                }
                Button(
                    onClick = onScan,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonOrange),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PersonSearch, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ESCANEAR JUGADORES", color = Color.Black, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                }
                Button(
                    onClick = onShare,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A6B2A)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Share, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("COMPARTIR SERVIDOR", color = Color.White, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                }
                if (server.isFull) {
                    Button(
                        onClick = onWatch,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC0000)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Visibility, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("VIGILAR SLOT AHORA", color = Color.White, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (server.autoWatch) NeonOrange.copy(0.1f) else BgDark)
                        .border(
                            1.dp,
                            if (server.autoWatch) NeonOrange.copy(0.4f) else CardBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.NotificationsActive, null,
                        tint = if (server.autoWatch) NeonOrange else TextDim,
                        modifier = Modifier.size(18.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Vigilancia automatica",
                            color = if (server.autoWatch) Color.White else TextMid,
                            fontWeight = FontWeight.SemiBold, fontSize = 13.sp
                        )
                        Text(
                            if (server.autoWatch) "Activa — te notifica cuando haya slot"
                            else "Inactiva — no te notifica",
                            color = if (server.autoWatch) NeonOrange else TextDim,
                            fontSize = 10.sp, fontFamily = FontFamily.Monospace
                        )
                    }
                    Switch(
                        checked = server.autoWatch,
                        onCheckedChange = { onToggleAutoWatch() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White, checkedTrackColor = NeonOrange,
                            uncheckedThumbColor = TextDim, uncheckedTrackColor = CardBorder
                        )
                    )
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, Color(0xFF440000)),
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
            TextButton(onClick = onDismiss) { Text("Cancelar", color = TextDim) }
        }
    )
}

@Composable
fun HistoryItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, color = TextDim, fontSize = 8.sp, letterSpacing = 1.sp, fontFamily = FontFamily.Monospace)
        Text(value, color = NeonOrange, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun MiniBarChart(snapshots: List<ServerSnapshot>) {
    val maxP = snapshots.maxOfOrNull { it.maxPlayers }?.takeIf { it > 0 } ?: return
    Row(
        modifier = Modifier.fillMaxWidth().height(32.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        snapshots.forEach { snap ->
            val ratio = (snap.players.toFloat() / maxP).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(fraction = ratio.coerceAtLeast(0.05f))
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    .background(fillColor(ratio).copy(alpha = 0.8f))
            )
        }
    }
}

@Composable
fun StatItem(label: String, value: String, valueColor: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, color = TextDim, fontSize = 9.sp, letterSpacing = 1.sp, fontFamily = FontFamily.Monospace)
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun PlayerScanDialog(isLoading: Boolean, players: List<PlayerInfo>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        containerColor = CardBg,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.PersonSearch, null, tint = NeonOrange, modifier = Modifier.size(20.dp))
                Text("OPERATIVOS", color = NeonOrange, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                if (!isLoading && players.isNotEmpty()) {
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NeonOrange.copy(0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "${players.size}", color = NeonOrange, fontSize = 11.sp,
                            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        },
        text = {
            when {
                isLoading -> Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = NeonOrange, strokeWidth = 2.dp)
                        Text(
                            "Interceptando señal...", color = TextDim,
                            fontSize = 12.sp, fontFamily = FontFamily.Monospace
                        )
                    }
                }
                players.isEmpty() -> Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No se pudo obtener la lista.\nEl servidor bloquea consultas directas\ny no aparece en Gametracker.",
                        color = TextDim, textAlign = TextAlign.Center, fontSize = 13.sp
                    )
                }
                else -> Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState())
                ) {
                    players.forEachIndexed { i, player ->
                        val isTop = i == 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(7.dp))
                                .background(if (isTop) NeonOrange.copy(0.1f) else BgDark)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "${i + 1}",
                                color = if (isTop) NeonOrange else TextDim,
                                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace, modifier = Modifier.width(18.dp)
                            )
                            if (isTop) Icon(Icons.Default.EmojiEvents, null, tint = NeonOrange, modifier = Modifier.size(14.dp))
                            else Icon(Icons.Default.Person, null, tint = TextDim, modifier = Modifier.size(14.dp))
                            Text(
                                player.name,
                                color = if (isTop) Color.White else Color(0xFFCCCCCC),
                                fontWeight = if (isTop) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f), maxLines = 1,
                                overflow = TextOverflow.Ellipsis, fontSize = 13.sp
                            )
                            Text(
                                "${player.score}",
                                color = if (isTop) NeonOrange else TextMid,
                                fontWeight = FontWeight.ExtraBold, fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.AddCircleOutline, null, tint = NeonOrange, modifier = Modifier.size(20.dp))
                Text("AÑADIR SERVIDOR", color = Color.White, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ingresa la IP y puerto del servidor:", color = TextMid, fontSize = 13.sp)
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it; error = false },
                    placeholder = {
                        Text(
                            "45.235.98.50:27015", color = TextDim,
                            fontFamily = FontFamily.Monospace, fontSize = 13.sp
                        )
                    },
                    isError = error, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonOrange, unfocusedBorderColor = CardBorder,
                        cursorColor = NeonOrange, focusedTextColor = Color.White, unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                if (error) Text(
                    "Formato invalido. Usa IP:PUERTO", color = PingRed,
                    fontSize = 12.sp, fontFamily = FontFamily.Monospace
                )
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
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = TextDim) }
        }
    )
}

@Composable
fun WatchingBanner(ip: String, onCancel: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "watch")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "wp"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A0E00))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.Visibility, null,
            tint = NeonOrange.copy(alpha = pulse), modifier = Modifier.size(14.dp)
        )
        Text(
            "Vigilando: $ip", color = NeonOrange, fontSize = 12.sp,
            fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f)
        )
        TextButton(
            onClick = onCancel,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        ) {
            Text("Cancelar", color = TextDim, fontSize = 11.sp)
        }
    }
}

@Composable
fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(color = NeonOrange, strokeWidth = 2.dp, strokeCap = StrokeCap.Round)
            Text("Escaneando servidores...", color = TextDim, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        }
    }
}

@Composable
fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.WifiOff, null, tint = TextDim, modifier = Modifier.size(48.dp))
            Text("Sin servidores en radar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Usa el boton + para agregar uno", color = TextDim, fontSize = 13.sp)
        }
    }
}
