package com.kouda.tactical.ui.roblox

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kouda.tactical.network.roblox.RobloxCategory
import com.kouda.tactical.network.roblox.RobloxGame
import com.kouda.tactical.ui.theme.BgDark
import com.kouda.tactical.ui.theme.CardBg
import com.kouda.tactical.ui.theme.CardBorder
import com.kouda.tactical.ui.theme.TextDim
import com.kouda.tactical.ui.theme.TextMid

// Color acento Roblox
private val RobloxRed = Color(0xFFE53935)
private val RobloxRedDim = Color(0xFFB71C1C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RobloxScreen(
    viewModel: RobloxViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var searchActive by remember { mutableStateOf(false) }

    if (state.selectedGame != null) {
        RobloxGameDialog(
            game = state.selectedGame!!,
            onDismiss = { viewModel.selectGame(null) }
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
                                value = state.searchQuery,
                                onValueChange = { viewModel.search(it) },
                                placeholder = { Text("Buscar juego de Roblox...", color = TextDim, fontSize = 13.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RobloxRed,
                                    unfocusedBorderColor = Color.Transparent,
                                    cursorColor = RobloxRed,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth().padding(end = 8.dp)
                            )
                        } else {
                            Column {
                                Text(
                                    "ROBLOX", color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp, letterSpacing = 2.sp
                                )
                                Text(
                                    "Explorador de experiencias",
                                    color = RobloxRed, fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDark),
                    navigationIcon = {
                        IconButton(onClick = {
                            if (searchActive) { searchActive = false; viewModel.clearSearch() }
                            else onBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextMid)
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            searchActive = !searchActive
                            if (!searchActive) viewModel.clearSearch()
                        }) {
                            Icon(
                                if (searchActive) Icons.Default.Close else Icons.Default.Search,
                                "Buscar",
                                tint = if (searchActive) RobloxRed else TextMid
                            )
                        }
                        if (!searchActive) {
                            IconButton(onClick = { viewModel.loadCategory(state.category) }) {
                                Icon(Icons.Default.Refresh, "Refresh", tint = RobloxRed)
                            }
                        }
                    }
                )
                Box(
                    modifier = Modifier.fillMaxWidth().height(1.dp).background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, RobloxRed.copy(0.5f), Color.Transparent)
                        )
                    )
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // Category tabs (solo cuando no busca)
            if (!searchActive) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RobloxCategory.entries.forEach { cat ->
                        val selected = state.category == cat && !searchActive
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) RobloxRed else CardBg)
                                .border(1.dp, if (selected) RobloxRed else CardBorder, RoundedCornerShape(8.dp))
                                .clickable { viewModel.loadCategory(cat) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                cat.label,
                                color = if (selected) Color.Black else TextMid,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Grid de juegos
            val displayGames = if (searchActive && state.searchQuery.length >= 3) {
                state.searchResults.map { r ->
                    RobloxGame(
                        universeId = r.universeId, placeId = r.placeId,
                        name = r.name, description = "", creator = "",
                        creatorType = "User", activePlayers = r.activePlayers,
                        totalVisits = r.totalVisits, favoritedCount = 0,
                        maxPlayers = 0, genre = "", thumbnailUrl = r.thumbnailUrl,
                        isPlayable = true, created = "", updated = ""
                    )
                }
            } else if (!searchActive) state.games else emptyList()

            val isLoading = if (searchActive) state.isSearching else state.isLoadingGames

            when {
                isLoading && displayGames.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = RobloxRed, strokeWidth = 2.dp, strokeCap = StrokeCap.Round
                            )
                            Text(
                                if (searchActive) "Buscando..." else "Cargando juegos...",
                                color = TextDim, fontFamily = FontFamily.Monospace, fontSize = 13.sp
                            )
                        }
                    }
                }
                displayGames.isEmpty() && searchActive && state.searchQuery.length >= 3 -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Sin resultados para \"${state.searchQuery}\"", color = TextDim, fontSize = 13.sp)
                    }
                }
                displayGames.isEmpty() && searchActive -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                            RobloxGameCard(game = game, onClick = { viewModel.selectGame(game) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RobloxGameCard(game: RobloxGame, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .background(CardBg)
            .clickable(onClick = onClick)
    ) {
        Column {
            // Thumbnail
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                    .background(Color(0xFF111111)),
                contentAlignment = Alignment.Center
            ) {
                if (game.thumbnailUrl != null) {
                    AsyncImage(
                        model = game.thumbnailUrl,
                        contentDescription = game.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("🎮", fontSize = 32.sp)
                }

                // Badge jugadores activos
                if (game.activePlayers > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(0.7f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Default.People, null, tint = RobloxRed, modifier = Modifier.size(10.dp))
                            Text(
                                game.activePlayersStr, color = Color.White, fontSize = 9.sp,
                                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    game.name, color = Color.White, fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold, maxLines = 2,
                    overflow = TextOverflow.Ellipsis, lineHeight = 16.sp
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Default.Visibility, null, tint = TextDim, modifier = Modifier.size(10.dp))
                        Text(game.formattedVisits, color = TextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    if (game.favoritedCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Default.Favorite, null, tint = RobloxRed.copy(0.7f), modifier = Modifier.size(10.dp))
                            Text(game.formattedFavorites, color = TextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RobloxGameDialog(game: RobloxGame, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(game.name, color = Color.White, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Thumbnail grande
                if (game.thumbnailUrl != null) {
                    AsyncImage(
                        model = game.thumbnailUrl,
                        contentDescription = game.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp))
                    )
                }

                // Stats
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(BgDark).padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    RobloxStatItem("ACTIVOS", game.activePlayersStr, RobloxRed)
                    RobloxStatItem("VISITAS", game.formattedVisits, TextMid)
                    RobloxStatItem("FAVS", game.formattedFavorites, Color(0xFFE91E63))
                }

                // Info
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    InfoChip("👤 ${game.creator}")
                    if (game.genre != "—" && game.genre.isNotBlank()) InfoChip("🎭 ${game.genre}")
                }

                // Descripción
                if (game.description.isNotBlank()) {
                    Text(
                        game.description, color = TextMid, fontSize = 12.sp,
                        lineHeight = 18.sp, maxLines = 5, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar", color = TextDim) }
        }
    )
}

@Composable
private fun RobloxStatItem(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = TextDim, fontSize = 9.sp, letterSpacing = 1.sp, fontFamily = FontFamily.Monospace)
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun InfoChip(text: String) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(4.dp))
            .background(CardBorder).padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, color = TextMid, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
