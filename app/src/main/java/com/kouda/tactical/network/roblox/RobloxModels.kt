package com.kouda.tactical.network.roblox

/**
 * Información de un juego/experiencia de Roblox.
 * Obtenida únicamente mediante APIs públicas oficiales de Roblox.
 */
data class RobloxGame(
    val universeId: Long,
    val placeId: Long,
    val name: String,
    val description: String,
    val creator: String,
    val creatorType: String,        // "User" o "Group"
    val activePlayers: Int,
    val totalVisits: Long,
    val favoritedCount: Long,
    val maxPlayers: Int,
    val genre: String,
    val thumbnailUrl: String?,
    val isPlayable: Boolean,
    val created: String,            // fecha ISO
    val updated: String
) {
    val formattedVisits: String get() = when {
        totalVisits >= 1_000_000_000 -> "${totalVisits / 1_000_000_000}B"
        totalVisits >= 1_000_000     -> "${totalVisits / 1_000_000}M"
        totalVisits >= 1_000         -> "${totalVisits / 1_000}K"
        else                         -> totalVisits.toString()
    }
    val formattedFavorites: String get() = when {
        favoritedCount >= 1_000_000 -> "${favoritedCount / 1_000_000}M"
        favoritedCount >= 1_000     -> "${favoritedCount / 1_000}K"
        else                         -> favoritedCount.toString()
    }
    val activePlayersStr: String get() = when {
        activePlayers >= 1_000 -> "${activePlayers / 1_000}K"
        else                   -> activePlayers.toString()
    }
}

/** Categorías de búsqueda en Roblox */
enum class RobloxCategory(val label: String, val sortType: String) {
    TOP_PLAYED("Más jugados",    "PlayerCount"),
    TOP_RATED("Mejor valorados", "Favorites"),
    NEW("Nuevos",               "RecentlyUpdated"),
    FEATURED("Destacados",      "Featured")
}

/** Resultado de búsqueda por nombre */
data class RobloxSearchResult(
    val universeId: Long,
    val placeId: Long,
    val name: String,
    val thumbnailUrl: String?,
    val activePlayers: Int,
    val totalVisits: Long
)
