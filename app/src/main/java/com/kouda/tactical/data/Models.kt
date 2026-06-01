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
    val pingStr: String get() = when {
        ping == -2 -> "..."
        ping == -1 -> "offline"
        else -> "${ping}ms"
    }
    val isFull: Boolean get() = maxPlayers > 0 && curPlayers >= maxPlayers
    val fillRatio: Float get() = if (maxPlayers > 0) curPlayers.toFloat() / maxPlayers else 0f
    val isOffline: Boolean get() = ping == -1
    val isLoading: Boolean get() = ping == -2
}

data class PlayerInfo(
    val name: String,
    val score: Int
)

data class ServerSnapshot(
    val timestamp: Long,
    val players: Int,
    val maxPlayers: Int
)

data class ServerHistory(
    val ip: String,
    val snapshots: List<ServerSnapshot> = emptyList()
) {
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

    fun recentAverage(): Int {
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        val recent = snapshots.filter { it.timestamp > cutoff }
        if (recent.isEmpty()) return 0
        return recent.map { it.players }.average().toInt()
    }

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
