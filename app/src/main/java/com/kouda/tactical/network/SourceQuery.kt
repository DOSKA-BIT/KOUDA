package com.kouda.tactical.network

import com.kouda.tactical.data.PlayerInfo
import com.kouda.tactical.data.ServerInfo
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

object SourceQuery {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .callTimeout(8, TimeUnit.SECONDS)
        .build()

    // A2S_INFO con challenge 0xFFFFFFFF incluido (formato moderno)
    private val QUERY_INFO_V1 = byteArrayOf(
        0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
        0x54
    ) + "Source Engine Query\u0000".toByteArray(Charsets.UTF_8)

    // A2S_INFO con challenge al final (para Source Engine moderno)
    private val QUERY_INFO_V2 = byteArrayOf(
        0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
        0x54
    ) + "Source Engine Query\u0000".toByteArray(Charsets.UTF_8) +
    byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())

    // ─── ENTRY POINT ─────────────────────────────────────────────────────────

    fun queryServer(address: String, getPlayers: Boolean = false): Pair<ServerInfo?, List<PlayerInfo>> {
        return try {
            val parts = address.trim().split(":")
            if (parts.size != 2) return null to emptyList()
            val ip = parts[0].trim()
            val port = parts[1].trim().toIntOrNull() ?: return null to emptyList()

            val addr = InetAddress.getByName(ip)
            val socket = DatagramSocket()
            socket.soTimeout = 2500

            // Intentar query con manejo completo del challenge
            val startTime = System.currentTimeMillis()
            val data = sendA2SInfo(socket, addr, port)
            val ping = (System.currentTimeMillis() - startTime).toInt()

            if (data == null) {
                socket.close()
                // Fallback Steam API para servidores que bloquean UDP
                val steamResult = queryViaSteamApi(ip, port, address)
                return steamResult to emptyList()
            }

            val info = parseInfoResponse(data, ip, address, ping)
            val country = fetchCountry(ip)
            val playerList = if (getPlayers) {
                fetchPlayersWithFallback(ip, port, addr, info?.folder ?: "")
            } else emptyList()
            socket.close()

            info?.copy(country = country) to playerList
        } catch (e: Exception) {
            null to emptyList()
        }
    }

    // ─── A2S_INFO CON CHALLENGE HANDLING ─────────────────────────────────────

    /**
     * Envía A2S_INFO con manejo completo del challenge de Source Engine moderno.
     *
     * Flujo:
     * 1. Enviamos A2S_INFO con challenge 0xFFFFFFFF
     * 2. Si el server responde con 0x41 → nos pide challenge real
     *    → armamos nuevo paquete con los 4 bytes del challenge
     *    → enviamos de nuevo
     *    → recibimos 0x49 con la info real
     * 3. Si responde directo con 0x49 o 0x6D → lo usamos
     */
    private fun sendA2SInfo(socket: DatagramSocket, addr: InetAddress, port: Int): ByteArray? {
        return try {
            // Primer intento con el query moderno (incluye 0xFFFFFFFF al final)
            socket.send(DatagramPacket(QUERY_INFO_V2, QUERY_INFO_V2.size, addr, port))

            val buf = ByteArray(4096)
            val resp = DatagramPacket(buf, buf.size)
            socket.receive(resp)
            var data = resp.data.copyOf(resp.length)

            // El servidor respondió con challenge (0x41) — necesita el token real
            if (data.size >= 9 && data[4] == 0x41.toByte()) {
                // Extraer los 4 bytes del challenge que nos mandó el servidor
                val challengeToken = data.copyOfRange(5, 9)

                // Armar nuevo A2S_INFO con el token correcto al final
                val queryWithChallenge = byteArrayOf(
                    0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
                    0x54
                ) + "Source Engine Query\u0000".toByteArray(Charsets.UTF_8) + challengeToken

                socket.send(DatagramPacket(queryWithChallenge, queryWithChallenge.size, addr, port))
                socket.receive(resp)
                data = resp.data.copyOf(resp.length)
            }

            // Verificar que la respuesta es info válida (0x49 Source o 0x6D GoldSrc)
            return when {
                data.size > 4 && data[4] == 0x49.toByte() -> data  // Source
                data.size > 4 && data[4] == 0x6D.toByte() -> data  // GoldSrc (CS 1.6, HL)
                else -> {
                    // Último intento con query viejo sin challenge (algunos servers viejos)
                    tryOldStyleQuery(socket, addr, port)
                }
            }
        } catch (e: Exception) {
            // Si timeout con V2, intentar con V1 (sin challenge al final)
            try {
                socket.soTimeout = 2000
                socket.send(DatagramPacket(QUERY_INFO_V1, QUERY_INFO_V1.size, addr, port))
                val buf = ByteArray(4096)
                val resp = DatagramPacket(buf, buf.size)
                socket.receive(resp)
                val data = resp.data.copyOf(resp.length)
                if (data.size > 4 && (data[4] == 0x49.toByte() || data[4] == 0x6D.toByte())) data
                else null
            } catch (e2: Exception) {
                null
            }
        }
    }

    private fun tryOldStyleQuery(socket: DatagramSocket, addr: InetAddress, port: Int): ByteArray? {
        return try {
            socket.soTimeout = 2000
            socket.send(DatagramPacket(QUERY_INFO_V1, QUERY_INFO_V1.size, addr, port))
            val buf = ByteArray(4096)
            val resp = DatagramPacket(buf, buf.size)
            socket.receive(resp)
            val data = resp.data.copyOf(resp.length)
            if (data.size > 4 && (data[4] == 0x49.toByte() || data[4] == 0x6D.toByte())) data else null
        } catch (e: Exception) {
            null
        }
    }

    // ─── STEAM API FALLBACK ───────────────────────────────────────────────────

    private fun queryViaSteamApi(ip: String, port: Int, address: String): ServerInfo? {
        return try {
            val url = "https://api.steampowered.com/IGameServersService/GetServerList/v1/" +
                "?filter=addr\\$ip:$port&limit=1&key="
            val request = Request.Builder().url(url)
                .header("User-Agent", "Mozilla/5.0").build()
            val body = httpClient.newCall(request).execute().body?.string() ?: return null
            val json = JSONObject(body)
            val servers = json.optJSONObject("response")?.optJSONArray("servers") ?: return null
            if (servers.length() == 0) return null
            val s = servers.getJSONObject(0)
            val name = s.optString("name", "").trim()
            if (name.isBlank()) return null
            val max = s.optInt("max_players", 0)
            if (max <= 0) return null
            ServerInfo(
                name = name.take(40),
                map = s.optString("map", "-"),
                curPlayers = s.optInt("players", 0),
                maxPlayers = max,
                ping = -1,
                ip = address,
                country = fetchCountry(ip),
                folder = s.optString("gamedir", "unknown")
            )
        } catch (e: Exception) {
            null
        }
    }

    // ─── PARSE INFO RESPONSE ─────────────────────────────────────────────────

    private fun parseInfoResponse(data: ByteArray, ip: String, address: String, ping: Int): ServerInfo? {
        return try {
            if (data.size < 5) return null
            val header = data[4].toInt() and 0xFF
            var name = "Unknown"; var map = "-"; var folder = "unknown"
            var curPlayers = 0; var maxPlayers = 0

            when (header) {
                0x49 -> {
                    var pos = 6
                    val strings = mutableListOf<String>()
                    repeat(4) {
                        val end = data.indexOf(0, pos)
                        if (end == -1) return null
                        strings.add(String(data, pos, end - pos, Charsets.UTF_8))
                        pos = end + 1
                    }
                    name = strings.getOrElse(0) { "Unknown" }.take(40)
                    map = strings.getOrElse(1) { "-" }
                    folder = strings.getOrElse(2) { "unknown" }
                    pos += 2
                    if (pos + 1 < data.size) {
                        curPlayers = data[pos].toInt() and 0xFF
                        maxPlayers = data[pos + 1].toInt() and 0xFF
                    }
                }
                0x6D -> {
                    var pos = 5
                    val strings = mutableListOf<String>()
                    repeat(5) {
                        val end = data.indexOf(0, pos)
                        if (end == -1) return null
                        strings.add(String(data, pos, end - pos, Charsets.UTF_8))
                        pos = end + 1
                    }
                    name = strings.getOrElse(1) { "Unknown" }.take(40)
                    map = strings.getOrElse(2) { "-" }
                    folder = strings.getOrElse(3) { "unknown" }
                    if (pos + 1 < data.size) {
                        curPlayers = data[pos].toInt() and 0xFF
                        maxPlayers = data[pos + 1].toInt() and 0xFF
                    }
                }
                else -> return null
            }

            ServerInfo(
                name = name, map = map,
                curPlayers = curPlayers, maxPlayers = maxPlayers,
                ping = ping, ip = address, country = "??", folder = folder
            )
        } catch (e: Exception) { null }
    }

    // ─── PLAYERS ─────────────────────────────────────────────────────────────

    private fun fetchPlayersWithFallback(ip: String, port: Int, addr: InetAddress, folder: String): List<PlayerInfo> {
        val udp = queryPlayersWithRetry(addr, port)
        if (udp.isNotEmpty()) return udp
        return fetchFromGametracker(ip, port)
    }

    private fun queryPlayersWithRetry(addr: InetAddress, port: Int): List<PlayerInfo> {
        for (timeout in listOf(2000, 3000, 4000)) {
            val r = tryQueryPlayers(addr, port, timeout)
            if (r.isNotEmpty()) return r
        }
        return emptyList()
    }

    private fun tryQueryPlayers(addr: InetAddress, port: Int, timeoutMs: Int): List<PlayerInfo> {
        val socket = DatagramSocket()
        return try {
            socket.soTimeout = timeoutMs
            val challengeReq = byteArrayOf(
                0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
                0x55, 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()
            )
            socket.send(DatagramPacket(challengeReq, challengeReq.size, addr, port))
            val buf = ByteArray(4096)
            val resp = DatagramPacket(buf, buf.size)
            socket.receive(resp)
            var data = resp.data.copyOf(resp.length)

            if (data.size >= 9 && data[4] == 0x41.toByte()) {
                val cn = data.copyOfRange(5, 9)
                val req = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x55) + cn
                socket.send(DatagramPacket(req, req.size, addr, port))
                socket.receive(resp)
                data = resp.data.copyOf(resp.length)
            }

            if (data.size < 6 || data[4] != 0x44.toByte()) return emptyList()
            val players = mutableListOf<PlayerInfo>()
            var ptr = 5
            val num = data[ptr].toInt() and 0xFF; ptr++
            repeat(num) {
                if (ptr >= data.size) return@repeat
                ptr++
                val nameEnd = data.indexOf(0, ptr)
                if (nameEnd == -1 || nameEnd + 8 > data.size) return@repeat
                val pName = String(data, ptr, nameEnd - ptr, Charsets.UTF_8).trim()
                val score = ByteBuffer.wrap(data, nameEnd + 1, 4).order(ByteOrder.LITTLE_ENDIAN).int
                ptr = nameEnd + 9
                if (pName.isNotBlank()) players.add(PlayerInfo(pName, score))
            }
            players.sortedByDescending { it.score }
        } catch (e: Exception) { emptyList() } finally { socket.close() }
    }

    private fun fetchFromGametracker(ip: String, port: Int): List<PlayerInfo> {
        return try {
            val request = Request.Builder()
                .url("https://www.gametracker.com/server_info/$ip:$port/")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            val body = httpClient.newCall(request).execute().body?.string() ?: return emptyList()
            val players = mutableListOf<PlayerInfo>()
            val rowRegex = Regex(
                """<tr[^>]*class="[^"]*player_row[^"]*"[^>]*>.*?<td[^>]*>(.*?)</td>.*?<td[^>]*>(\d+)</td>""",
                RegexOption.DOT_MATCHES_ALL
            )
            rowRegex.findAll(body).forEach { match ->
                val name = match.groupValues[1].replace(Regex("<[^>]+>"), "").trim()
                val score = match.groupValues[2].toIntOrNull() ?: 0
                if (name.isNotBlank() && name != "Player Name")
                    players.add(PlayerInfo(name, score))
            }
            players.sortedByDescending { it.score }
        } catch (e: Exception) { emptyList() }
    }

    private fun fetchCountry(ip: String): String {
        return try {
            val body = httpClient.newCall(
                Request.Builder().url("http://ip-api.com/json/$ip?fields=countryCode").build()
            ).execute().body?.string() ?: return "??"
            JSONObject(body).optString("countryCode", "??")
        } catch (e: Exception) { "??" }
    }

    private fun ByteArray.indexOf(target: Byte, start: Int): Int {
        for (i in start until size) if (this[i] == target) return i
        return -1
    }
}
