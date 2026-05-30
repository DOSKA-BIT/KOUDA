package com.kouda.tactical

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kouda.tactical.data.ServerInfo
import com.kouda.tactical.data.SortMode
import com.kouda.tactical.network.SearchResult
import com.kouda.tactical.ui.theme.BgDark
import com.kouda.tactical.ui.theme.CardBg
import com.kouda.tactical.ui.theme.CardBorder
import com.kouda.tactical.ui.theme.FillFull
import com.kouda.tactical.ui.theme.NeonOrange
import com.kouda.tactical.ui.theme.TextDim
import com.kouda.tactical.ui.theme.TextMid
import com.kouda.tactical.ui.theme.fillColor
import com.kouda.tactical.ui.theme.pingColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerListScreen(viewModel: KoudaViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val myServers = remember(state.myServers, state.currentFilter, state.sortMode) {
        viewModel.filteredAndSorted()
    }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedServer by remember { mutableStateOf<ServerInfo?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var copiedIp by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }

    // Buscar en Gametracker con debounce
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 3) {
            kotlinx.coroutines.delay(600)
            viewModel.searchOnline(searchQuery)
        } else if (searchQuery.isBlank()) {
            viewModel.clearSearch()
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
        val liveServer = myServers.find { it.ip == selectedIp }
            ?: state.myServers.find { it.ip == selectedIp }
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
                                    Text(
                                        "Buscar en Gametracker...",
                                        color = TextDim, fontSize = 13.sp
                                    )
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
                                viewModel.clearSearch()
                            } else onBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextMid)
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            searchActive = !searchActive
                            if (!searchActive) { searchQuery = ""; viewModel.clearSearch() }
                        }) {
                            Icon(
                                if (searchActive) Icons.Default.Close else Icons.Default.Search,
                                "Buscar",
                                tint = if (searchActive) NeonOrange else TextMid
                            )
                        }
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
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(
                    Brush.horizontalGradient(listOf(Color.Transparent, NeonOrange.copy(0.5f), Color.Transparent))
                ))
            }
        },
        floatingActionButton = {
            if (!searchActive) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = NeonOrange, shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, "Add server", tint = Color.Black)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // ─── FILTROS (solo fuera de busqueda) ───
            if (!searchActive) {
                item {
                    GameFilterRow(
                        currentFilter = state.currentFilter,
                        onFilterSelected = viewModel::setFilter
                    )
                }
            }

            // ─── BANNER DE VIGILANCIA ───
            if (state.watchingIp != null && !searchActive) {
                item {
                    WatchingBanner(ip = state.watchingIp!!, onCancel = viewModel::cancelWatch)
                }
            }

            // ─── TOAST DE COPIADO ───
            if (copiedIp != null) {
                item {
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
            }

            // ─── SECCIÓN: MIS SERVIDORES ───
            item {
                SectionHeader(
                    title = "MIS SERVIDORES",
                    count = myServers.size,
                    isLoading = state.isLoadingMy
                )
            }

            if (state.isLoadingMy && myServers.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NeonOrange, strokeWidth = 2.dp)
                    }
                }
            } else if (myServers.isEmpty() && !state.isLoadingMy) {
                item {
                    EmptyMyServers(onDiscover = { viewModel.discoverServers() })
                }
            } else {
                itemsIndexed(myServers, key = { _, s -> "my_${s.ip}" }) { index, server ->
                    AnimatedServerCard(
                        server = server, index = index,
                        onClick = { selectedServer = server },
                        onFavToggle = { viewModel.toggleFavorite(server.ip) },
                        onLongPress = { clipboard.setText(AnnotatedString(server.ip)); copiedIp = server.ip }
                    )
                }
            }

            // ─── SECCIÓN: RESULTADOS DE BÚSQUEDA ───
            if (searchActive) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                item {
                    SectionHeader(
                        title = if (state.lastSearchQuery.isBlank()) "BUSCAR EN INTERNET"
                                else "RESULTADOS EN GAMETRACKER",
                        count = state.searchResults.size,
                        isLoading = state.isSearching,
                        subtitle = if (state.lastSearchQuery.isNotBlank())
                            "\"${state.lastSearchQuery}\"" else null
                    )
                }

                if (state.isSearching) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(color = NeonOrange, strokeWidth = 2.dp)
                                Text("Buscando en Gametracker...", color = TextDim, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                } else if (searchQuery.length < 3 && searchQuery.isNotBlank()) {
                    item {
                        Text(
                            "Escribi al menos 3 caracteres para buscar",
                            color = TextDim, fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else if (state.searchResults.isEmpty() && state.lastSearchQuery.isNotBlank()) {
                    item {
                        Text(
                            "Sin resultados para \"${state.lastSearchQuery}\"",
                            color = TextDim, fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                        )
                    }
                } else {
                    itemsIndexed(state.searchResults, key = { _, s -> "search_${s.ip}" }) { _, result ->
                        SearchResultCard(
                            result = result,
                            alreadySaved = state.myServers.any { it.ip == result.ip },
                            onSave = { viewModel.saveFromSearch(result) },
                            onLongPress = { clipboard.setText(AnnotatedString(result.ip)); copiedIp = result.ip }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}

// ─── SECTION HEADER ──────────────────────────────────────────────────────────

@Composable
fun SectionHeader(
    title: String,
    count: Int,
    isLoading: Boolean = false,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.size(3.dp, 16.dp).background(NeonOrange, RoundedCornerShape(2.dp)))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = NeonOrange, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp, fontFamily = FontFamily.Monospace)
            if (subtitle != null) {
                Text(subtitle, color = TextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }
        if (isLoading) {
            CircularProgressIndicator(color = NeonOrange, strokeWidth = 1.5.dp, modifier = Modifier.size(14.dp))
        } else if (count > 0) {
            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(NeonOrange.copy(0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text("$count", color = NeonOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

// ─── EMPTY MY SERVERS ────────────────────────────────────────────────────────

@Composable
fun EmptyMyServers(onDiscover: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("No tenés servidores guardados.", color = TextDim, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onDiscover) {
                Icon(Icons.Default.Search, null, tint = NeonOrange, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.size(4.dp))
                Text("Buscar automaticamente", color = NeonOrange, fontSize = 12.sp)
            }
        }
        Text(
            "O usá la lupa para buscar en Gametracker",
            color = TextDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace
        )
    }
}

// ─── SEARCH RESULT CARD ──────────────────────────────────────────────────────

@Composable
fun SearchResultCard(
    result: SearchResult,
    alreadySaved: Boolean,
    onSave: () -> Unit,
    onLongPress: () -> Unit
) {
    val cardFillColor = fillColor(result.fillRatio)

    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (alreadySaved) NeonOrange.copy(0.3f) else CardBorder, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(NeonOrange.copy(0.15f)).padding(horizontal = 5.dp, vertical = 1.dp)) {
                            Text(result.country, color = NeonOrange, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                        }
                        Text(result.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text("${result.map}  ·  ${result.ip}", color = TextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(modifier = Modifier.size(8.dp))
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        result.players,
                        color = if (result.isFull) FillFull else Color.White,
                        fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, fontFamily = FontFamily.Monospace
                    )
                    // Boton guardar
                    if (alreadySaved) {
                        Text("Guardado", color = NeonOrange, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    } else {
                        IconButton(onClick = onSave, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.AddCircle, "Guardar", tint = NeonOrange, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            // Barra de ocupacion
            Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(CardBorder)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(result.fillRatio.coerceIn(0f, 1f))
                        .height(3.dp)
                        .background(Brush.horizontalGradient(listOf(cardFillColor.copy(0.5f), cardFillColor)))
                )
            }
        }
    }
}
