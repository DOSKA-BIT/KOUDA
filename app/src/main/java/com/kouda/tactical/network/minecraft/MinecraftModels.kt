package com.kouda.tactical.network.minecraft

data class MinecraftServerInfo(
    val ip: String,
    val port: Int,
    val name: String,
    val motdRaw: String,
    val version: String,
    val protocolVersion: Int,
    val curPlayers: Int,
    val maxPlayers: Int,
    val ping: Int,
    val faviconBase64: String?,
    val modType: String?,
    val mods: List<MinecraftMod>,
    val playerSample: List<String>,   // nombres de jugadores (muestra parcial del servidor)
    val isOnline: Boolean
) {
    val address: String get() = if (port == 25565) ip else "$ip:$port"
    val players: String get() = "$curPlayers/$maxPlayers"
    val pingStr: String get() = when {
        ping == -2 -> "..."
        ping == -1 -> "offline"
        else       -> "${ping}ms"
    }
    val fillRatio: Float get() = if (maxPlayers > 0) curPlayers.toFloat() / maxPlayers else 0f
    val isFull: Boolean get() = maxPlayers > 0 && curPlayers >= maxPlayers
    val isLoading: Boolean get() = ping == -2
}

data class MinecraftMod(
    val modId: String,
    val version: String
)

data class SavedMinecraftServer(
    val ip: String,
    val port: Int = 25565,
    val nickname: String = ""
) {
    val address: String get() = if (port == 25565) ip else "$ip:$port"
}
