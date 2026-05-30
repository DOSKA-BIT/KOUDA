package com.kouda.tactical

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
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
import com.kouda.tactical.network.BrowseGame
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

    // Modo: "my" = mis servidores, "browse" = explorar, "search" = buscar
    var mode by remember { mutableStateOf("my") }
    var searchQuery by remember { mutableStateOf("") }

    // Busqueda con debounce
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 3) {
            kotlinx.coroutines.delay(600)
            viewModel.searchByName(searchQuery)
        } else if (searchQuery.isBlank()) {
            viewModel.clearSearch()
        }
    }

    // Cargar servidores del juego seleccionado al entrar a browse
    LaunchedEffect(mode, state.browseGame) {
        if (mode == "browse") viewModel.browseGame(state.browseGame)
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
                        type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text)
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
                        if (mode == "search") {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Buscar por nombre de servidor...", color = TextDim, fontSize = 13.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonOrange, unfocusedBorderColor = Color.Transparent,
                                    cursorColor = NeonOrange, focusedTextColor = Color.White, unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth().padding(end = 8.dp)
                            )
                        } else {
                            Column {
                                Text("KOUDA TACTICAL", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, letterSpacing = 2.sp)
                                if (state.totalOnline > 0) {
                                    Text("${state.totalOnline} jugadores online", color = NeonOrange, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDark),
                    navigationIcon = {
                        IconButton(onClick = {
                            when (mode) {
                                "search" -> { mode = "my"; searchQuery = ""; viewModel.clearSearch() }
                                "browse" -> mode = "my"
                                else -> onBack()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextMid)
                        }
                    },
                    actions = {
                        // Buscar
                        IconButton(onClick = {
                            mode = if (mode == "search") { searchQuery = ""; viewModel.clearSearch(); "my" } else "search"
                        }) {
                            Icon(
                                if (mode == "search") Icons.Default.Close else Icons.Default.Search,
                                "Buscar", tint = if (mode == "search") NeonOrange else TextMid
                            )
                        }
                        if (mode == "my") {
                            Box {
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(Icons.AutoMirrored.Filled.Sort, "Sort", tint = TextMid)
                                }
                                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }, containerColor = CardBg) {
                                    SortMode.entries.forEach { sortMode ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(sortMode.label,
                                                    color = if (state.sortMode == sortMode) NeonOrange else Color.White,
                                                    fontWeight = if (state.sortMode == sortMode) FontWeight.Bold else FontWeight.Normal)
                                            },
                                            leadingIcon = {
                                                if (state.sortMode == sortMode)
                                                    Icon(Icons.Default.Check, null, tint = NeonOrange, modifier = Modifier.size(16.dp))
                                            },
                                            onClick = { viewModel.setSortMode(sortMode); showSortMenu = false }
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = viewModel::refresh) {
                                Icon(Icons.Default.Refresh, "Refresh", tint = NeonOrange)
                            }
                        }
                        if (mode == "browse") {
                            IconButton(onClick = { viewModel.browseGame(state.browseGame) }) {
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
            if (mode == "my") {
                FloatingActionButton(onClick = { showAddDialog = true }, containerColor = NeonOrange, shape = CircleShape) {
                    Icon(Icons.Default.Add, "Add", tint = Color.Black)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // ─── TABS DE MODO ───
            item {
                ModeTabs(
                    currentMode = mode,
                    onSelect = { newMode ->
                        mode = newMode
                        if (newMode != "search") { searchQuery = ""; viewModel.clearSearch() }
                    }
                )
            }

            // ─── COPIADO TOAST ───
            if (copiedIp != null) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(NeonOrange.copy(0.15f)).padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, null, tint = NeonOrange, modifier = Modifier.size(14.dp))
                        Text("Copiado: $copiedIp", color = NeonOrange, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            // ═══════════════════════════════════════════════════
            // MODO: MIS SERVIDORES
            // ═══════════════════════════════════════════════════
            if (mode == "my") {
                if (state.watchingIp != null) {
                    item { WatchingBanner(ip = state.watchingIp!!, onCancel = viewModel::cancelWatch) }
                }

                item {
                    GameFilterRow(currentFilter = state.currentFilter, onFilterSelected = viewModel::setFilter)
                }

                item {
                    SectionHeader("MIS SERVIDORES", myServers.size, state.isLoadingMy)
                }

                if (state.isLoadingMy && myServers.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = NeonOrange, strokeWidth = 2.dp)
                        }
                    }
                } else if (myServers.isEmpty()) {
                    item { EmptyMyServers(onDiscover = { viewModel.discoverServers() }) }
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
            }

            // ═══════════════════════════════════════════════════
            // MODO: EXPLORAR (top servers por juego)
            // ═══════════════════════════════════════════════════
            if (mode == "browse") {
                item {
                    BrowseGameTabs(
                        current = state.browseGame,
                        onSelect = { viewModel.browseGame(it) }
                    )
                }

                item {
                    SectionHeader(
                        title = "TOP SERVIDORES — ${state.browseGame.label}",
                        count = state.browseResults.size,
                        isLoading = state.isLoadingBrowse,
                        subtitle = "Sudamerica primero · ordenados por jugadores"
                    )
                }

                if (state.isLoadingBrowse) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                CircularProgressIndicator(color = NeonOrange, strokeWidth = 2.dp)
                                Text("Buscando servidores activos...", color = TextDim, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                } else if (state.browseResults.isEmpty()) {
                    item {
                        Text("No se encontraron servidores. Intentá de nuevo.", color = TextDim, fontSize = 13.sp, modifier = Modifier.padding(8.dp))
                    }
                } else {
                    itemsIndexed(state.browseResults, key = { _, s -> "browse_${s.ip}" }) { _, result ->
                        val saved = viewModel.isServerSaved(result.ip)
                        SearchResultCard(
                            result = result,
                            alreadySaved = saved,
                            onSave = { viewModel.saveFromSearch(result) },
                            onLongPress = { clipboard.setText(AnnotatedString(result.ip)); copiedIp = result.ip }
                        )
                    }
                }
            }

            // ═══════════════════════════════════════════════════
            // MODO: BUSCAR POR NOMBRE
            // ═══════════════════════════════════════════════════
            if (mode == "search") {
                item {
                    SectionHeader(
                        title = if (state.lastSearchQuery.isBlank()) "BUSCAR SERVIDOR" else "RESULTADOS",
                        count = state.searchResults.size,
                        isLoading = state.isSearching,
                        subtitle = if (state.lastSearchQuery.isNotBlank()) "\"${state.lastSearchQuery}\"" else "Escribi el nombre del servidor"
                    )
                }

                when {
                    state.isSearching -> item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                CircularProgressIndicator(color = NeonOrange, strokeWidth = 2.dp)
                                Text("Buscando \"${state.lastSearchQuery}\"...", color = TextDim, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                    searchQuery.isNotBlank() && searchQuery.length < 3 -> item {
                        Text("Escribi al menos 3 letras para buscar", color = TextDim, fontSize = 12.sp, modifier = Modifier.padding(8.dp), fontFamily = FontFamily.Monospace)
                    }
                    state.searchResults.isEmpty() && state.lastSearchQuery.isNotBlank() -> item {
                        Text("Sin resultados para \"${state.lastSearchQuery}\"", color = TextDim, fontSize = 13.sp, modifier = Modifier.padding(8.dp))
                    }
                    else -> {
                        itemsIndexed(state.searchResults, key = { _, s -> "search_${s.ip}" }) { _, result ->
                            val saved = viewModel.isServerSaved(result.ip)
                            SearchResultCard(
                                result = result,
                                alreadySaved = saved,
                                onSave = { viewModel.saveFromSearch(result) },
                                onLongPress = { clipboard.setText(AnnotatedString(result.ip)); copiedIp = result.ip }
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

// ─── MODE TABS ───────────────────────────────────────────────────────────────

@Composable
fun ModeTabs(currentMode: String, onSelect: (String) -> Unit) {
    val tabs = listOf("my" to "Mis Servidores", "browse" to "Explorar", "search" to "Buscar")
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEach { (mode, label) ->
            val selected = currentMode == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) NeonOrange else CardBg)
                    .border(1.dp, if (selected) NeonOrange else CardBorder, RoundedCornerShape(8.dp))
                    .clickable { onSelect(mode) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (selected) Color.Black else TextMid,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// ─── BROWSE GAME TABS ────────────────────────────────────────────────────────

@Composable
fun BrowseGameTabs(current: BrowseGame, onSelect: (BrowseGame) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BrowseGame.entries.forEach { game ->
            val selected = current == game
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) NeonOrange.copy(0.2f) else CardBg)
                    .border(1.dp, if (selected) NeonOrange else CardBorder, RoundedCornerShape(8.dp))
                    .clickable { onSelect(game) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    game.label,
                    color = if (selected) NeonOrange else TextMid,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal
                )
            }
        }
    }
}

// ─── SECTION HEADER ──────────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, count: Int, isLoading: Boolean = false, subtitle: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.size(3.dp, 16.dp).background(NeonOrange, RoundedCornerShape(2.dp)))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = NeonOrange, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp, fontFamily = FontFamily.Monospace)
            if (subtitle != null) Text(subtitle, color = TextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
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

// ─── EMPTY STATE ─────────────────────────────────────────────────────────────

@Composable
fun EmptyMyServers(onDiscover: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("No tenés servidores guardados todavia.", color = TextDim, fontSize = 13.sp)
        TextButton(onClick = onDiscover) {
            Icon(Icons.Default.Search, null, tint = NeonOrange, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Buscar automaticamente", color = NeonOrange, fontSize = 12.sp)
        }
        Text("O explorá servidores en la pestaña Explorar", color = TextDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

// ─── SEARCH / BROWSE RESULT CARD ─────────────────────────────────────────────

@Composable
fun SearchResultCard(
    result: SearchResult,
    alreadySaved: Boolean,
    onSave: () -> Unit,
    onLongPress: () -> Unit
) {
    val cardFillColor = fillColor(result.fillRatio)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, if (alreadySaved) NeonOrange.copy(0.3f) else CardBorder, RoundedCornerShape(12.dp))
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
                    Text(
                        buildString {
                            if (result.map != "-" && result.map.isNotBlank()) append("${result.map}  ·  ")
                            append(result.ip)
                        },
                        color = TextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        result.players,
                        color = if (result.isFull) FillFull else Color.White,
                        fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, fontFamily = FontFamily.Monospace
                    )
                    if (alreadySaved) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(NeonOrange.copy(0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Guardado", color = NeonOrange, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    } else {
                        IconButton(
                            onClick = onSave,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.AddCircle, "Guardar", tint = NeonOrange, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
            // Barra de ocupacion
            Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(CardBorder)) {
                if (result.fillRatio > 0) {
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
}
