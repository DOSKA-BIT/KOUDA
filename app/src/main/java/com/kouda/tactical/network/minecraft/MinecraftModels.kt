package com.kouda.tactical.network.minecraft

/**
 * Resultado de una consulta a un servidor Minecraft Java Edition.
 * Construido a partir del protocolo Server List Ping (SLP).
 */
data class MinecraftServerInfo(
    val ip: String,
    val port: Int,
    val name: String,           // MOTD limpio (sin códigos de color)
    val motdRaw: String,        // MOTD con códigos §
    val version: String,
    val protocolVersion: Int,
    val curPlayers: Int,
    val maxPlayers: Int,
    val ping: Int,              // ms, -1 = offline
    val faviconBase64: String?, // data:image/png;base64,...
    val modType: String?,       // "FORGE", "FABRIC", null
    val mods: List<MinecraftMod>,
    val isOnline: Boolean
) {
    val address: String get() = if (port == 25565) ip else "$ip:$port"
    val players: String get() = "$curPlayers/$maxPlayers"
    val pingStr: String get() = if (ping >= 0) "${ping}ms" else "offline"
    val fillRatio: Float get() = if (maxPlayers > 0) curPlayers.toFloat() / maxPlayers else 0f
    val isFull: Boolean get() = maxPlayers > 0 && curPlayers >= maxPlayers
}

data class MinecraftMod(
    val modId: String,
    val version: String
)

/** Servidor guardado por el usuario (solo IP/puerto, sin datos de sesión) */
data class SavedMinecraftServer(
    val ip: String,
    val port: Int = 25565,
    val nickname: String = ""   // apodo opcional del usuario
) {
    val address: String get() = if (port == 25565) ip else "$ip:$port"
}
