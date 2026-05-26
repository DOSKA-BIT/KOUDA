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

// ─── MENU SCREEN ─────────────────────────────────────────────────────────────

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
                        center = Offset(cx, cy),
                        radius = size.width * 0.6f
                    ),
                    radius = size.width * 0.6f,
                    center = Offset(cx, cy)
                )
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
                    "GAME SERVER BROWSER",
                    color = NeonOrange,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(Modifier.height(20.dp))

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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(icon, color = NeonOrange, fontSize = 12.sp)
        Text(
            label,
            color = TextMid,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp
        )
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
    Box(modifier = Modifier
        .fillMaxSize()
        .drawBehind {
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
        }
    )
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
                Button(
                    onClick = viewModel::clearAlert,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonOrange)
                ) {
                    Text("ENTENDIDO", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    selectedServer?.let { server ->
        ServerOptionsDialog(
            server = server,
            onScan = { selectedServer = null; viewModel.scanPlayers(server.ip) },
            onWatch = { selectedServer = null; viewModel.watchSlot(server.ip) },
            onDelete = { selectedServer = null; viewModel.removeServer(server.ip) },
            onDismiss = { selectedServer = null }
        )
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
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextMid
                            )
                        }
                    },
                    actions = {
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "Sort",
                                    tint = TextMid
                                )
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
                                                Icon(
                                                    Icons.Default.Check,
                                                    null,
                                                    tint = NeonOrange,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                        },
                                        onClick = {
                                            viewModel.setSortMode(mode)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        IconButton(onClick = viewModel::refresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NeonOrange)
                        }
                    }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
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
                Icon(Icons.Default.Add, contentDescription = "Add server", tint = Color.Black)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            GameFilterRow(
                currentFilter = state.currentFilter,
                onFilterSelected = viewModel::setFilter
            )

            AnimatedVisibility(visible = state.watchingIp != null) {
                WatchingBanner(
                    ip = state.watchingIp ?: "",
                    onCancel = viewModel::cancelWatch
                )
            }

            AnimatedVisibility(visible = copiedIp != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NeonOrange.copy(0.15f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        null,
                        tint = NeonOrange,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "Copiado: ${copiedIp}",
                        color = NeonOrange,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
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
        enter = fadeIn(tween(300)) + slid
