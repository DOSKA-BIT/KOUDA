package com.kouda.tactical.network

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object MasterServer {

    // Servidor master de Valve
    private const val MASTER_HOST = "hl1master.steampowered.com"
    private const val MASTER_PORT = 27011

    // Filtros por juego
    private val GAME_FILTERS = mapOf(
        "cstrike" to """\gamedir\cstrike""",
        "csgo"    to """\appid\730""",
        "tf"      to """\appid\440""",
        "valve"   to """\gamedir\valve"""
    )

    // Regiones
    private const val REGION_SOUTH_AMERICA = 0x03.toByte()
    private const val REGION_ALL = 0xFF.toByte()

    /**
     * Consulta la Master Server List de Valve y devuelve una lista de IPs
     * @param gameDir carpeta del juego (cstrike, csgo, tf, valve)
     * @param region region geografica (usar REGION_SOUTH_AMERICA para SA)
     * @param maxServers maximo de servidores a devolver
     */
    fun queryServers(
        gameDir: String,
        region: Byte = REGION_SOUTH_AMERICA,
        maxServers: Int = 50
    ): List<String> {
        val results = mutableListOf<String>()
        val filter = GAME_FILTERS[gameDir] ?: """\gamedir\$gameDir"""

        return try {
            val addr = InetAddress.getByName(MASTER_HOST)
            val socket = DatagramSocket()
            socket.soTimeout = 5000

            var lastIp = "0.0.0.0"
            var lastPort = 0

            // La Master Server List usa paginacion — seguimos pidiendo hasta
            // que nos devuelvan 0.0.0.0:0 como señal de fin
            repeat(10) { // max 10 paginas = hasta 660 servidores
                if (results.size >= maxServers) return@repeat

                val query = buildQuery(region, lastIp, lastPort, filter)
                socket.send(DatagramPacket(query, query.size, addr, MASTER_PORT))

                val buf = ByteArray(4096)
                val resp = DatagramPacket(buf, buf.size)
                socket.receive(resp)

                val data = resp.data.copyOf(resp.length)
                val servers = parseResponse(data)

                if (servers.isEmpty()) return@repeat

                for (server in servers) {
                    if (server == "0.0.0.0:0") return@repeat
                    if (server !in results) results.add(server)
                    if (results.size >= maxServers) return@repeat
                }

                // Ultima IP para la siguiente pagina
                val last = servers.last().split(":")
                lastIp = last[0]
                lastPort = last[1].toIntOrNull() ?: 0

                if (lastIp == "0.0.0.0") return@repeat
            }

            socket.close()
            results.take(maxServers)
        } catch (e: Exception) {
            android.util.Log.e("MasterServer", "Error querying master: ${e.message}")
            emptyList()
        }
    }

    private fun buildQuery(
        region: Byte,
        lastIp: String,
        lastPort: Int,
        filter: String
    ): ByteArray {
        val seed = "$lastIp:$lastPort"
        val filterBytes = filter.toByteArray(Charsets.UTF_8)
        val seedBytes = seed.toByteArray(Charsets.UTF_8)

        return byteArrayOf(0x31, region) +
            seedBytes + byteArrayOf(0x00) +
            filterBytes + byteArrayOf(0x00)
    }

    private fun parseResponse(data: ByteArray): List<String> {
        val servers = mutableListOf<String>()
        if (data.size < 6) return servers

        // Header: FF FF FF FF 66 0A
        var ptr = 6
        while (ptr + 5 < data.size) {
            val ip = "${data[ptr].toInt() and 0xFF}." +
                     "${data[ptr+1].toInt() and 0xFF}." +
                     "${data[ptr+2].toInt() and 0xFF}." +
                     "${data[ptr+3].toInt() and 0xFF}"
            val port = ((data[ptr+4].toInt() and 0xFF) shl 8) or
                       (data[ptr+5].toInt() and 0xFF)
            servers.add("$ip:$port")
            ptr += 6
        }
        return servers
    }
}
