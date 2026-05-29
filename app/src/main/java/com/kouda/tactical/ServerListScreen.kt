package com.kouda.tactical

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.kouda.tactical.ui.theme.NeonOrange
import com.kouda.tactical.ui.theme.TextDim
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kouda.tactical.data.ServerInfo
import com.kouda.tactical.data.SortMode
import com.kouda.tactical.ui.theme.BgDark
import com.kouda.tactical.ui.theme.CardBg
import com.kouda.tactical.ui.theme.NeonOrange
import com.kouda.tactical.ui.theme.TextMid


 @OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerListScreen(viewModel: KoudaViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedServer by remember { mutableStateOf<ServerInfo?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var copiedIp by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }

    val servers = remember(state.servers, state.currentFilter, state.sortMode, searchQuery) {
        val base = viewModel.filteredAndSorted()
        if (searchQuery.isBlank()) base
        else base.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.map.contains(searchQuery, ignoreCase = true) ||
            it.ip.contains(searchQuery, ignoreCase = true)
        }
    }

    LaunchedEffect(copiedIp) {
        if (copiedIp != null) { kotlinx.coroutines.delay(2000); copiedIp = null }
    }

    if (state.slotAlert != null) {
        SlotAlertDialog(serverName = state.slotAlert!!, onDismiss = viewModel::clearAlert)
    }

    if (selectedServer != null) {
        val selectedIp = selectedServer!!.ip
        val liveServer = servers.find { it.ip == selectedIp }
            ?: state.servers.find { it.ip == selectedIp }
        if (liveServer != null) {
            ServerOptionsDialog(
                server = liveServer,
                history = viewModel.getHistory(selectedIp),
                onScan = { selectedServer = null; viewModel.scanPlayers(selectedIp) },
                onWatch = { selectedServer = null; viewModel.watchSlot(selectedIp, liveServer.name) },
                onToggleAutoWatch = { viewModel.toggleAutoWatch(selectedIp) },
                onShare = {
                    val text = "🎮 ${liveServer.name}\n🗺 Mapa: ${liveServer.map}\n👥 Jugadores: ${liveServer.players}\n📡 Ping: ${liveServer.pingStr}\n🌍 Pais: ${liveServer.country}\n🔗 IP: ${liveServer.ip}\n\nConectate desde Kouda Tactical"
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(intent, "Compartir servidor"))
                },
                onDelete = { selectedServer = null; viewModel.removeServer(selectedIp) },
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
                        if (searchActive) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text("Buscar servidor, mapa, IP...", color = TextDim, fontSize = 13.sp)
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonOrange,
                                    unfocusedBorderColor = Color.Transparent,
                                    cursorColor = NeonOrange,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth().padding(end = 8.dp)
                            )
                        } else {
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
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDark),
                    navigationIcon = {
                        IconButton(onClick = {
                            if (searchActive) {
                                searchActive = false
                                searchQuery = ""
                            } else {
                                onBack()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextMid)
                        }
                    },
                    actions = {
                        // Boton busqueda
                        IconButton(onClick = {
                            searchActive = !searchActive
                            if (!searchActive) searchQuery = ""
                        }) {
                            Icon(
                                if (searchActive) Icons.Default.Close else Icons.Default.Search,
                                "Buscar",
                                tint = if (searchActive) NeonOrange else TextMid
                            )
                        }
                        // Boton ordenar (solo si no hay busqueda activa)
                        if (!searchActive) {
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

            if (!searchActive) {
                GameFilterRow(
                    currentFilter = state.currentFilter,
                    onFilterSelected = viewModel::setFilter
                )
            }

            if (state.watchingIp != null && !searchActive) {
                WatchingBanner(ip = state.watchingIp!!, onCancel = viewModel::cancelWatch)
            }

            if (copiedIp != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(NeonOrange.copy(0.15f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, null, tint = NeonOrange, modifier = Modifier.size(14.dp))
                    Text("Copiado: $copiedIp", color = NeonOrange, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }

            // Indicador de resultados cuando hay busqueda activa
            if (searchActive && searchQuery.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${servers.size} resultado${if (servers.size != 1) "s" else ""} para \"$searchQuery\"",
                        color = TextDim,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            when {
                state.isLoading && servers.isEmpty() -> LoadingState()
                servers.isEmpty() && !state.isLoading && searchQuery.isNotBlank() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Search, null, tint = TextDim, modifier = Modifier.size(48.dp))
                            Text("Sin resultados para \"$searchQuery\"", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Proba con otro nombre o IP", color = TextDim, fontSize = 13.sp)
                        }
                    }
                }
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
