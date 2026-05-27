package com.kouda.tactical

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
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
import com.kouda.tactical.data.ServerInfo
import com.kouda.tactical.data.SortMode
import com.kouda.tactical.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults

// ─── MENU SCREEN ─────────────────────────────────────────────────────────────

@Composable
fun MenuScreen(onEnter: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "menu_anim")

    // Rotating glow angle
    val glowAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "glow"
    )
    // Pulse for the button
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    // Scanline offset
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
                // Animated corner glow
                val rad = Math.toRadians(glowAngle.toDouble())
                val cx = size.width / 2 + cos(rad).toFloat() * size.width * 0.4f
                val cy = size.height / 2 + sin(rad).toFloat() * size.height * 0.3f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(NeonOrange.copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = size.width * 0.6f
                    ),
                    radius = size.width * 0.6f,
                    center = Offset(cx, cy)
                )
                // Subtle scanline
                val lineY = scanY * size.height
                drawLine(
                    color = NeonOrange.copy(alpha = 0.04f),
                    start = Offset(0f, lineY),
                    end = Offset(size.width, lineY),
                    strokeWidth = 2f
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Grid dots in background
        GridDots()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            // Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(NeonOrange.copy(alpha = 0.15f))
                    .border(1.dp, NeonOrange.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    "GAME SERVER BROWSER",
                    color = NeonOrange,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(Modifier.height(20.dp))

            // Main title
            Text(
                text = "KOUDA",
                color = Color.White,
                fontSize = 64.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 8.sp,
                lineHeight = 64.sp
            )
            Text(
                text = "TACTICAL",
                color = NeonOrange,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 12.sp
            )

            Spacer(Modifier.height(48.dp))

            // Stats row
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatBadge("CS 1.6", "☑")
                StatBadge("CS:GO", "☑")
                StatBadge("TF2", "☑")
                StatBadge("HL", "☑")
            }

            Spacer(Modifier.height(48.dp))

            // Launch button
            Button(
                onClick = onEnter,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonOrange),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(Icons.Default.Radar, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "LAUNCH HUB",
                    color = Color.Black,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    letterSpacing = 3.sp
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "Source Engine Query Protocol",
                color = TextDim,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun StatBadge(label: String, icon: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
        val dotR = 1.5f
        var x = spacing / 2
        while (x < size.width) {
            var y = spacing / 2
            while (y < size.height) {
                drawCircle(NeonOrange.copy(alpha = alpha), dotR, Offset(x, y))
                y += spacing
            }
            x += spacing
        }
    })
}

// ─── SERVER LIST SCREEN ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerListScreen(viewModel: KoudaViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val servers = remember(state.servers, state.currentFilter, state.sortMode) {
        viewModel.filteredAndSorted()
    }
    val clipboard = LocalClipboardManager.current

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedServer by remember { mutableStateOf<ServerInfo?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var copiedIp by remember { mutableStateOf<String?>(null) }

    // Auto-clear copy toast
    LaunchedEffect(copiedIp) {
        if (copiedIp != null) {
            kotlinx.coroutines.delay(2000)
            copiedIp = null
        }
    }

    // Slot alert
    if (state.slotAlert != null) {
        AlertDialog(
            onDismissRequest = viewModel::clearAlert,
            containerColor = CardBg,
            shape = RoundedCornerShape(16.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = NeonOrange)
                    Text("¡SLOT LIBRE!", color = NeonOrange, fontWeight = FontWeight.ExtraBold)
                }
            },
            text = {
                Text(
                    "Hay un lugar disponible en ${state.slotAlert}.\n¡Conectate ahora!",
                    color = Color.White
                )
            },
            confirmButton = {
                Button(onClick = viewModel::clearAlert, colors = ButtonDefaults.buttonColors(containerColor = NeonOrange)) {
                    Text("ENTENDIDO", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Server options
    selectedServer?.let { server ->
    ServerOptionsDialog(
    server = server,
    onScan = {
        selectedServer = null
        viewModel.scanPlayers(server.ip)
    },
    onWatch = {
        selectedServer = null
        viewModel.watchSlot(server.ip, server.name)
    },
    onToggleAutoWatch = {
        viewModel.toggleAutoWatch(server.ip)
    },
    onDelete = {
        selectedServer = null
        viewModel.removeServer(server.ip)
    },
    onDismiss = { selectedServer = null }
)
    }

    // Player scan result
    if (state.isScanning || state.scanResult != null) {
        PlayerScanDialog(
            isLoading = state.isScanning,
            players = state.scanResult ?: emptyList(),
            onDismiss = viewModel::clearScanResult
        )
    }

    // Add server
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
                                "KOUDA TACTICAL",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                letterSpacing = 2.sp
                            )
                            if (state.totalOnline > 0) {
                                Text(
                                    "${state.totalOnline} jugadores online",
                                    color = NeonOrange,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDark),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextMid)
                        }
                    },
                    actions = {
                        // Sort dropdown
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", tint = TextMid)
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
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NeonOrange)
                        }
                    }
                )
                // Divider with orange glow
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(
                    Brush.horizontalGradient(listOf(Color.Transparent, NeonOrange.copy(0.5f), Color.Transparent))
                ))
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = NeonOrange,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add server", tint = Color.Black)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // Game filter chips
            GameFilterRow(currentFilter = state.currentFilter, onFilterSelected = viewModel::setFilter)

            // Watching banner
            AnimatedVisibility(visible = state.watchingIp != null) {
                WatchingBanner(ip = state.watchingIp ?: "", onCancel = viewModel::cancelWatch)
            }

            // Copy toast
            AnimatedVisibility(visible = copiedIp != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(NeonOrange.copy(0.15f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, null, tint = NeonOrange, modifier = Modifier.size(14.dp))
                    Text("Copiado: ${copiedIp}", color = NeonOrange, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
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
                        item { Spacer(Modifier.height(72.dp)) }
                    }
                }
            }
        }
    }
}

// ─── GAME FILTER ROW ─────────────────────────────────────────────────────────

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
                    selectedContainerColor = NeonOrange,
                    selectedLabelColor = Color.Black,
                    containerColor = CardBg,
                    labelColor = TextMid
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    selectedBorderColor = Color.Transparent,
                    borderColor = CardBorder
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

// ─── ANIMATED SERVER CARD ────────────────────────────────────────────────────

@Composable
fun AnimatedServerCard(
    server: ServerInfo,
    index: Int,
    onClick: () -> Unit,
    onFavToggle: () -> Unit,
    onLongPress: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 40L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 3 }
    ) {
        ServerCard(server = server, onClick = onClick, onFavToggle = onFavToggle, onLongPress = onLongPress)
    }
}

@Composable
fun ServerCard(
    server: ServerInfo,
    onClick: () -> Unit,
    onFavToggle: () -> Unit,
    onLongPress: () -> Unit
) {
    val fillColor = fillColor(server.fillRatio)
    val pingColor = pingColor(server.ping)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() }, onLongPress = { onLongPress() })
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = if (server.isFav) BorderStroke(1.dp, NeonOrange.copy(0.35f)) else BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left block
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    // Name + country
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Country pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(NeonOrange.copy(0.15f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(server.country, color = NeonOrange, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                        }
                        Text(
                            server.name,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    // Map + IP
                    Text(
                        "${server.map}  ·  ${server.ip}",
                        color = TextDim,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Right block
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    // Players
                    Text(
                        server.players,
                        color = if (server.isFull) FillFull else Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    // Ping with color
                    Text(
                        server.pingStr,
                        color = pingColor,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    // Fav star
                    IconButton(onClick = onFavToggle, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = if (server.isFav) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (server.isFav) Color(0xFFFFCC00) else TextDim,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Player fill bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(CardBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = server.fillRatio.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(listOf(fillColor.copy(0.5f), fillColor))
                        )
                )
            }
        }
    }
}

// ─── DIALOGS ─────────────────────────────────────────────────────────────────

@Composable
fun ServerOptionsDialog(
    server: ServerInfo,
    onScan: () -> Unit,
    onWatch: () -> Unit,
    onToggleAutoWatch: () -> Unit,
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
                    text = server.name,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = server.ip,
                    color = TextDim,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                // Stats
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

                // Escanear jugadores
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

                // Vigilar slot manual (solo si está lleno)
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

                // Toggle vigilancia automática
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (server.autoWatch) NeonOrange.copy(0.1f) else BgDark
                        )
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
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = if (server.autoWatch) NeonOrange else TextDim,
                        modifier = Modifier.size(18.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Vigilancia automatica",
                            color = if (server.autoWatch) Color.White else TextMid,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (server.autoWatch)
                                "Activa — te notifica cuando haya slot"
                            else
                                "Inactiva — no te notifica",
                            color = if (server.autoWatch) NeonOrange else TextDim,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Switch(
                        checked = server.autoWatch,
                        onCheckedChange = { onToggleAutoWatch() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NeonOrange,
                            uncheckedThumbColor = TextDim,
                            uncheckedTrackColor = CardBorder
                        )
                    )
                }

                // Eliminar
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
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextDim)
            }
        }
    )
}
 
@Composable
fun StatItem(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.PersonSearch, null, tint = NeonOrange, modifier = Modifier.size(20.dp))
                Text("OPERATIVOS", color = NeonOrange, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                if (!isLoading && players.isNotEmpty()) {
                    Spacer(Modifier.weight(1f))
                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(NeonOrange.copy(0.2f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("${players.size}", color = NeonOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        },
        text = {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(color = NeonOrange, strokeWidth = 2.dp)
                        Text("Interceptando señal...", color = TextDim, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            } else if (players.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                    Text("Servidor vacío o firewall activo.", color = TextDim, textAlign = TextAlign.Center, fontSize = 13.sp)
                }
            } else {
                Column(
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
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.width(18.dp)
                            )
                            if (isTop) Icon(Icons.Default.EmojiEvents, null, tint = NeonOrange, modifier = Modifier.size(14.dp))
                            else Icon(Icons.Default.Person, null, tint = TextDim, modifier = Modifier.size(14.dp))
                            Text(
                                player.name, color = if (isTop) Color.White else Color(0xFFCCCCCC),
                                fontWeight = if (isTop) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp
                            )
                            Text(
                                "${player.score}",
                                color = if (isTop) NeonOrange else TextMid,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isLoading) {
                TextButton(onClick = onDismiss) { Text("CERRAR", color = NeonOrange, fontWeight = FontWeight.Bold) }
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
                Text("AÑADIR SERVIDOR", color = Color.White, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ingresá la IP y puerto del servidor:", color = TextMid, fontSize = 13.sp)
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it; error = false },
                    placeholder = { Text("45.235.98.50:27015", color = TextDim, fontFamily = FontFamily.Monospace, fontSize = 13.sp) },
                    isError = error,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonOrange,
                        unfocusedBorderColor = CardBorder,
                        focusedLabelColor = NeonOrange,
                        cursorColor = NeonOrange,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                if (error) {
                    Text("⚠ Formato inválido. Usá IP:PUERTO", color = PingRed, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
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
            ) {
                Text("VINCULAR", color = Color.Black, fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = TextDim) }
        }
    )
}

// ─── HELPERS ─────────────────────────────────────────────────────────────────

@Composable
fun WatchingBanner(ip: String, onCancel: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF1A0E00))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val pulse by rememberInfiniteTransition(label = "w").animateFloat(
            initialValue = 0.4f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "wp"
        )
        Icon(Icons.Default.Visibility, null, tint = NeonOrange.copy(alpha = pulse), modifier = Modifier.size(14.dp))
        Text("Vigilando: $ip", color = NeonOrange, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
        TextButton(onClick = onCancel, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
            Text("Cancelar", color = TextDim, fontSize = 11.sp)
        }
    }
}

@Composable
fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(color = NeonOrange, strokeWidth = 2.dp, strokeCap = StrokeCap.Round)
            Text("Escaneando servidores...", color = TextDim, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        }
    }
}

@Composable
fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.WifiOff, null, tint = TextDim, modifier = Modifier.size(48.dp))
            Text("Sin servidores en radar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Usá el botón + para agregar uno", color = TextDim, fontSize = 13.sp)
        }
    }
}
