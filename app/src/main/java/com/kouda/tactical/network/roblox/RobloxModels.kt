package com.kouda.tactical.network.roblox

data class RobloxGame(
    val universeId: Long,
    val placeId: Long,
    val name: String,
    val description: String,
    val creator: String,
    val creatorType: String,
    val activePlayers: Int,
    val totalVisits: Long,
    val favoritedCount: Long,
    val maxPlayers: Int,
    val genre: String,
    val thumbnailUrl: String?,
    val isPlayable: Boolean,
    val created: String,
    val updated: String
) {
    val formattedVisits: String get() = compact(totalVisits)
    val formattedFavorites: String get() = compact(favoritedCount)
    val activePlayersStr: String get() = if (activePlayers >= 1000) "${activePlayers / 1000}K" else "$activePlayers"
    val playUrl: String get() = "https://www.roblox.com/games/$placeId"

    private fun compact(n: Long): String = when {
        n >= 1_000_000_000 -> "${n / 1_000_000_000}B"
        n >= 1_000_000     -> "${n / 1_000_000}M"
        n >= 1_000         -> "${n / 1_000}K"
        else               -> n.toString()
    }
}

/**
 * Categorías de exploración. No son el ranking interno de Roblox (ese endpoint
 * ya no responde sin sesión) — son búsquedas por palabra clave contra omni-search,
 * elegidas para cubrir los géneros más populares.
 */
enum class RobloxCategory(val label: String, val query: String) {
    POPULAR("Populares", "obby"),
    SIMULATORS("Simuladores", "simulator"),
    ROLEPLAY("Roleplay", "roleplay"),
    TYCOON("Tycoon", "tycoon")
}

data class RobloxSearchResult(
    val universeId: Long,
    val placeId: Long,
    val name: String,
    val thumbnailUrl: String?,
    val activePlayers: Int,
    val totalVisits: Long
)
