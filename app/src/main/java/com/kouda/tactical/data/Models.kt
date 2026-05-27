package com.kouda.tactical.data

data class ServerInfo(
    val name: String,
    val map: String,
    val curPlayers: Int,
    val maxPlayers: Int,
    val ping: Int,
    val ip: String,
    val country: String,
    val folder: String,
    var isFav: Boolean = false,
    var autoWatch: Boolean = false
) {
    val players: String get() = "$curPlayers/$maxPlayers"
    val pingStr: String get() = "${ping}ms"
    val isFull: Boolean get() = maxPlayers > 0 && curPlayers >= maxPlayers
    val fillRatio: Float get() = if (maxPlayers > 0) curPlayers.toFloat() / maxPlayers else 0f
}

data class PlayerInfo(
    val name: String,
    val score: Int
)

enum class GameFilter(val label: String, val tag: String) {
    ALL("Todos", ""),
    CS16("CS 1.6", "cstrike"),
    CSGO("CS:GO", "csgo"),
    HL("Half-Life", "valve"),
    TF2("TF2", "tf");
}

enum class SortMode(val label: String) {
    PING("Ping"),
    PLAYERS("Jugadores"),
    NAME("Nombre"),
    FAVORITES("Favoritos")
}
