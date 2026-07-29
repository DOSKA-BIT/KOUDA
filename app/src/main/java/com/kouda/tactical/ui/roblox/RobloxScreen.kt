package com.kouda.tactical.ui.roblox

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kouda.tactical.network.roblox.RobloxCategory
import com.kouda.tactical.network.roblox.RobloxGame
import com.kouda.tactical.network.roblox.RobloxSearchResult
import com.kouda.tactical.ui.theme.*

private val RobloxRed = Color(0xFFE53935)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RobloxScreen(viewModel: RobloxViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var searchActive by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    if (state.selectedGame != null) {
        RobloxGameDialog(
            game = state.selectedGame!!,
            onOpenInRoblox = {
                context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(state.selectedGame!!.playUrl)))
            },
            onShare = {
                val text = "🟥 ${state.selectedGame!!.name}\n${state.selectedGame!!.playUrl}"
                context.startActivity(android.content.Intent.createChooser(
                    android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"; putExtra(android.content.Intent.EXTRA_TEXT, text)
                    }, "Compartir juego"
                ))
            },
            onDismiss = { viewModel.selectGame(null) }
        )
    }

    if (showAddDialog) {
        RobloxAddDialog(
            error = state.addError,
            onAdd = { viewModel.addByInput(it) },
            onDismiss = { showAddDialog = false; viewModel.clearAddError() }
        )
    }

    LaunchedEffect(state.selectedGame) {
        if (state.selectedGame != null) showAddDialog = false
    }

    Scaffold(
        containerColor = BgDark,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        if (searchActive) {
                            OutlinedTextField(
                                value = state.searchQuery,
                                onValueChange = { viewModel.search(it) },
                                placeholder = { Text("Buscar juego de Roblox...", color = TextDim, fontSize = 13.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RobloxRed, unfocusedBorderColor = Color.Transparent,
                                    cursorColor = RobloxRed, focusedTextColor = Color.White, unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth().padding(end = 8.dp)
                            )
                        } else {
                            Column {
                                Text("ROBLOX", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, letterSpacing = 2.sp)
                                Text("Explorador de experiencias", color = RobloxRed, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDark),
                    navigationIcon = {
                        IconButton(onClick = {
                            if (searchActive) { searchActive = false; viewModel.clearSearch() } else onBack()
                        }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = TextMid) }
                    },
                    actions = {
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.AddLink, "Agregar por URL/ID", tint = RobloxRed)
                        }
                        IconButton(onClick = {
                            searchActive = !searchActive
                            if (!searchActive) viewModel.clearSearch()
                        }) {
                            Icon(if (searchActive) Icons.Default.Close else Icons.Default.Search, "Buscar",
                                tint = if (searchActive) RobloxRed else TextMid)
                        }
                    }
                )
                Box(Modifier.fillMaxWidth().height(1.dp).background(
                    Brush.horizontalGradient(listOf(Color.Transparent, RobloxRed.copy(.5f), Color.Transparent))
                ))
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            if (!searchActive) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RobloxCategory.entries.forEach { cat ->
                        val selected = state.category == cat
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .background(if (selected) RobloxRed else CardBg)
                                .border(1.dp, if (selected) RobloxRed else CardBorder, RoundedCornerShape(8.dp))
                                .clickable { viewModel.loadCategory(cat) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(cat.label, color = if (selected) Color.Black else TextMid, fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal)
                        }
                    }
                }
            }

            val displayGames = if (searchActive && state.searchQuery.length >= 3) state.searchResults
                                else if (!searchActive) state.games else emptyList()
            val isLoading = if (searchActive) state.isSearching else state.isLoadingGames

            when {
                isLoading && displayGames.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            CircularProgressIndicator(color = RobloxRed, strokeWidth = 2.dp)
                            Text(if (searchActive) "Buscando..." else "Cargando juegos...", color = TextDim, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                        }
                    }
                }
                !searchActive && state.exploreFailed && displayGames.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text("🟥", fontSize = 40.sp)
                            Text("No se pudo cargar el listado ahora", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.Center)
                            Text(
                                "Podés buscar un juego por nombre o agregarlo directamente pegando el link, el Place ID o el Universe ID.",
                                color = TextDim, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 17.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            TextButton(onClick = { showAddDialog = true }) {
                                Icon(Icons.Default.AddLink, null, tint = RobloxRed, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Agregar por URL o ID", color = RobloxRed, fontSize = 13.sp)
                            }
                        }
                    }
                }
                displayGames.isEmpty() && searchActive && state.searchQuery.length >= 3 -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Text("Sin resultados para \"${state.searchQuery}\"", color = TextDim, fontSize = 13.sp)
                    }
                }
                displayGames.isEmpty() && searchActive -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Text("Escribí al menos 3 letras para buscar", color = TextDim, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(displayGames, key = { it.universeId }) { game ->
                            RobloxCardCompact(game = game, onClick = { viewModel.addByInput(game.universeId.toString()) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RobloxCardCompact(game: RobloxSearchResult, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .background(CardBg)
            .clickable(onClick = onClick)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color(0xFF111111)), Alignment.Center) {
                if (game.thumbnailUrl != null) {
                    AsyncImage(model = game.thumbnailUrl, contentDescription = game.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else Text("🎮", fontSize = 32.sp)

                if (game.activePlayers > 0) {
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                            .clip(RoundedCornerShape(4.dp)).background(Color.Black.copy(.7f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Default.People, null, tint = RobloxRed, modifier = Modifier.size(10.dp))
                            val cnt = if (game.activePlayers >= 1000) "${game.activePlayers / 1000}K" else "${game.activePlayers}"
                            Text(cnt, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(game.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
fun RobloxGameDialog(game: RobloxGame, onOpenInRoblox: () -> Unit, onShare: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        shape = RoundedCornerShape(16.dp),
        title = { Text(game.name, color = Color.White, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (game.thumbnailUrl != null) {
                    AsyncImage(model = game.thumbnailUrl, contentDescription = game.name, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp)))
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(BgDark).padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    RbStat("ACTIVOS", game.activePlayersStr, RobloxRed)
                    RbStat("VISITAS", game.formattedVisits, TextMid)
                    RbStat("FAVS", game.formattedFavorites, Color(0xFFE91E63))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RbChip("👤 ${game.creator}")
                    if (game.genre != "—" && game.genre.isNotBlank()) RbChip("🎭 ${game.genre}")
                }
                if (game.updated.isNotBlank()) {
                    Text("Actualizado: ${game.updated}", color = TextDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                if (game.description.isNotBlank()) {
                    Text(game.description, color = TextMid, fontSize = 12.sp, lineHeight = 18.sp, maxLines = 6, overflow = TextOverflow.Ellipsis)
                }

                Button(
                    onClick = onOpenInRoblox, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = RobloxRed), shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.OpenInNew, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("ABRIR EN ROBLOX", color = Color.White, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                }
                Button(
                    onClick = onShare, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A6B2A)), shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Share, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("COMPARTIR", color = Color.White, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar", color = TextDim) } }
    )
}

@Composable
fun RobloxAddDialog(error: String?, onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    var input by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        shape = RoundedCornerShape(16.dp),
        title = { Text("AGREGAR JUEGO", color = Color.White, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Pegá el link, el Place ID o el Universe ID:", color = TextMid, fontSize = 13.sp)
                OutlinedTextField(
                    value = input, onValueChange = { input = it },
                    placeholder = { Text("roblox.com/games/1234567890  o  1234567890", color = TextDim, fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                    singleLine = true, isError = error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RobloxRed, unfocusedBorderColor = CardBorder,
                        cursorColor = RobloxRed, focusedTextColor = Color.White, unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                if (error != null) Text(error, color = PingRed, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        },
        confirmButton = {
            Button(onClick = { if (input.isNotBlank()) onAdd(input.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = RobloxRed), shape = RoundedCornerShape(8.dp)) {
                Text("BUSCAR", color = Color.White, fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = TextDim) } }
    )
}

@Composable
private fun RbStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = TextDim, fontSize = 9.sp, letterSpacing = 1.sp, fontFamily = FontFamily.Monospace)
        Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun RbChip(text: String) {
    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(CardBorder).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(text, color = TextMid, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
