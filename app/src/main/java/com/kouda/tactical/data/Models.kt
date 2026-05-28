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

// Un punto de datos en el tiempo para un servidor
data class ServerSnapshot(
    val timestamp: Long,      // System.currentTimeMillis()
    val players: Int,
    val maxPlayers: Int
)

// Historial completo de un servidor
data class ServerHistory(
    val ip: String,
    val snapshots: List<ServerSnapshot> = emptyList()
) {
    // Hora pico: la hora del dia (0-23) con mas jugadores en promedio
    fun peakHour(): Int? {
        if (snapshots.size < 3) return null
        val byHour = snapshots.groupBy {
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = it.timestamp
            cal.get(java.util.Calendar.HOUR_OF_DAY)
        }
        return byHour.maxByOrNull { (_, snaps) ->
            snaps.map { it.players }.average()
        }?.key
    }

    // Promedio de jugadores en las ultimas 24hs
    fun recentAverage(): Int {
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        val recent = snapshots.filter { it.timestamp > cutoff }
        if (recent.isEmpty()) return 0
        return recent.map { it.players }.average().toInt()
    }

    // Cantidad de veces que se consulto
    fun totalChecks(): Int = snapshots.size
}

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
