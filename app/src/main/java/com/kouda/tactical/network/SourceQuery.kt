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

    private const val STEAM_API_KEY = ""

    private val QUERY_INFO = byteArrayOf(
        0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
        0x54,
        *"Source Engine Query\u0000".toByteArray(Charsets.UTF_8)
    )

    // ─── ENTRY POINT ─────────────────────────────────────────────────────────

    fun queryServer(address: String, getPlayers: Boolean = false): Pair<ServerInfo?, List<PlayerInfo>> {
        return try {
            val parts = address.trim().split(":")
            if (parts.size != 2) return null to emptyList()
            val ip = parts[0].trim()
            val port = parts[1].trim().toIntOrNull() ?: return null to emptyList()

            val addr = InetAddress.getByName(ip)
            val socket = DatagramSocket()
            socket.soTimeout = 2000

            // Tres mediciones para mayor precision
            val pingSamples = mutableListOf<Int>()
            var lastData = ByteArray(0)

            repeat(3) {
                try {
                    val t0 = System.currentTimeMillis()
                    socket.send(DatagramPacket(QUERY_INFO, QUERY_INFO.size, addr, port))
                    val buf = ByteArray(4096)
                    val resp = DatagramPacket(buf, buf.size)
                    socket.receive(resp)
                    pingSamples.add((System.currentTimeMillis() - t0).toInt())
                    if (lastData.isEmpty()) lastData = resp.data.copyOf(resp.length)
                } catch (e: Exception) { }
            }

            // Si no respondio al query normal, intentar con challenge (TF2 y algunos CS)
            if (lastData.isEmpty()) {
                lastData = queryWithChallenge(socket, addr, port) ?: return null to emptyList()
                pingSamples.add(999)
            }

            val ping = pingSamples.sorted().getOrElse(1) { pingSamples.firstOrNull() ?: 999 }
            val data = lastData

            val info = parseInfoResponse(data, ip, address, ping)
            val country = fetchCountry(ip)
            val playerList = if (getPlayers) fetchPlayersWithFallback(ip, port, addr, info?.folder ?: "") else emptyList()
            socket.close()

            info?.copy(country = country) to playerList
        } catch (e: Exception) {
            null to emptyList()
        }
    }

    // ─── CHALLENGE SUPPORT (TF2 y servidores modernos) ───────────────────────

    private fun queryWithChallenge(socket: DatagramSocket, addr: InetAddress, port: Int): ByteArray? {
        return try {
            socket.soTimeout = 3000

            val challengeQuery = byteArrayOf(
                0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
                0x54
            ) + "Source Engine Query\u0000".toByteArray(Charsets.UTF_8) +
            byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())

            socket.send(DatagramPacket(challengeQuery, challengeQuery.size, addr, port))

            val buf = ByteArray(4096)
            val resp = DatagramPacket(buf, buf.size)
            socket.receive(resp)
            val data = resp.data.copyOf(resp.length)

            if (data.size >= 9 && data[4] == 0x41.toByte()) {
                val challengeNum = data.copyOfRange(5, 9)
                val realQuery = byteArrayOf(
                    0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
                    0x54
                ) + "Source Engine Query\u0000".toByteArray(Charsets.UTF_8) + challengeNum

                socket.send(DatagramPacket(realQuery, realQuery.size, addr, port))
                socket.receive(resp)
                return resp.data.copyOf(resp.length)
            }

            if (data.size > 4 && (data[4] == 0x49.toByte() || data[4] == 0x6D.toByte())) data else null
        } catch (e: Exception) {
            null
        }
    }

    // ─── PARSE INFO RESPONSE ─────────────────────────────────────────────────

    private fun parseInfoResponse(data: ByteArray, ip: String, address: String, ping: Int): ServerInfo? {
        return try {
            if (data.size < 5) return null
            val header = data[4].toInt() and 0xFF
            var name = "Unknown Server"
            var map = "-"
            var folder = "unknown"
            var curPlayers = 0
            var maxPlayers = 0

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
                    name = strings.getOrElse(0) { "Unknown" }.take(30)
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
                    name = strings.getOrElse(1) { "Unknown" }.take(30)
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
                name = name,
                map = map,
                curPlayers = curPlayers,
                maxPlayers = maxPlayers,
                ping = ping,
                ip = address,
                country = "??",
                folder = folder
            )
        } catch (e: Exception) {
            null
        }
    }

    // ─── FALLBACK CHAIN PARA JUGADORES ───────────────────────────────────────

    private fun fetchPlayersWithFallback(
        ip: String,
        port: Int,
        addr: InetAddress,
        folder: String
    ): List<PlayerInfo> {
        val udpResult = queryPlayersWithRetry(addr, port)
        if (udpResult.isNotEmpty()) return udpResult

        val gtResult = fetchFromGametracker(ip, port)
        if (gtResult.isNotEmpty()) return gtResult

        if (STEAM_API_KEY.isNotEmpty()) {
            val appId = folderToSteamAppId(folder)
            if (appId != null) {
                val steamResult = fetchFromSteamApi(ip, port, appId)
                if (steamResult.isNotEmpty()) return steamResult
            }
        }

        return emptyList()
    }

    // ─── UDP DIRECTO ─────────────────────────────────────────────────────────

    private fun queryPlayersWithRetry(addr: InetAddress, port: Int): List<PlayerInfo> {
        val timeouts = listOf(2000, 3000, 4000)
        for (timeout in timeouts) {
            val result = tryQueryPlayers(addr, port, timeout)
            if (result.isNotEmpty()) return result
        }
        return emptyList()
    }

    private fun tryQueryPlayers(addr: InetAddress, port: Int, timeoutMs: Int): List<PlayerInfo> {
        val socket = DatagramSocket()
        return try {
            socket.soTimeout = timeoutMs
            val challengeReq = byteArrayOf(
                0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
                0x55,
                0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()
            )
            socket.send(DatagramPacket(challengeReq, challengeReq.size, addr, port))

            val buf = ByteArray(4096)
            val resp = DatagramPacket(buf, buf.size)
            socket.receive(resp)
            var data = resp.data.copyOf(resp.length)

            if (data.size >= 9 && data[4] == 0x41.toByte()) {
                val challengeNum = data.copyOfRange(5, 9)
                val realReq = byteArrayOf(
                    0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x55
                ) + challengeNum
                socket.send(DatagramPacket(realReq, realReq.size, addr, port))
                socket.receive(resp)
                data = resp.data.copyOf(resp.length)
            }

            if (data.size < 6 || data[4] != 0x44.toByte()) return emptyList()

            val players = mutableListOf<PlayerInfo>()
            var ptr = 5
            val numPlayers = data[ptr].toInt() and 0xFF
            ptr++

            repeat(numPlayers) {
                if (ptr >= data.size) return@repeat
                ptr++
                val nameEnd = data.indexOf(0, ptr)
                if (nameEnd == -1 || nameEnd + 8 > data.size) return@repeat
                val pName = String(data, ptr, nameEnd - ptr, Charsets.UTF_8).trim()
                val score = ByteBuffer.wrap(data, nameEnd + 1, 4)
                    .order(ByteOrder.LITTLE_ENDIAN).int
                ptr = nameEnd + 9
                if (pName.isNotBlank()) players.add(PlayerInfo(pName, score))
            }

            players.sortedByDescending { it.score }
        } catch (e: Exception) {
            emptyList()
        } finally {
            socket.close()
        }
    }

    // ─── GAMETRACKER ─────────────────────────────────────────────────────────

    private fun fetchFromGametracker(ip: String, port: Int): List<PlayerInfo> {
        return try {
            val url = "https://www.gametracker.com/server_info/$ip:$port/"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val body = httpClient.newCall(request).execute().body?.string() ?: return emptyList()
            val players = mutableListOf<PlayerInfo>()

            val rowRegex = Regex(
                """<tr[^>]*class="[^"]*player_row[^"]*"[^>]*>.*?<td[^>]*>(.*?)</td>.*?<td[^>]*>(\d+)</td>""",
                RegexOption.DOT_MATCHES_ALL
            )
            rowRegex.findAll(body).forEach { match ->
                val rawName = match.groupValues[1].replace(Regex("<[^>]+>"), "").trim()
                val score = match.groupValues[2].toIntOrNull() ?: 0
                if (rawName.isNotBlank() && rawName != "Player Name") {
                    players.add(PlayerInfo(rawName, score))
                }
            }

            players.sortedByDescending { it.score }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ─── STEAM API ───────────────────────────────────────────────────────────

    private fun fetchFromSteamApi(ip: String, port: Int, appId: Int): List<PlayerInfo> {
        return try {
            val url = "https://api.steampowered.com/IGameServersService/GetServerList/v1/" +
                "?key=$STEAM_API_KEY&filter=appid\\$appId\\addr\\$ip:$port&limit=1"
            val request = Request.Builder().url(url).build()
            val body = httpClient.newCall(request).execute().body?.string() ?: return emptyList()
            val json = JSONObject(body)
            val servers = json.optJSONObject("response")?.optJSONArray("servers") ?: return emptyList()
            if (servers.length() == 0) return emptyList()
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private fun folderToSteamAppId(folder: String): Int? = when (folder.lowercase()) {
        "csgo", "cs2" -> 730
        "tf" -> 440
        "valve" -> 70
        else -> null
    }

    private fun fetchCountry(ip: String): String {
        return try {
            val request = Request.Builder()
                .url("http://ip-api.com/json/$ip?fields=countryCode")
                .build()
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: return "??"
            JSONObject(body).optString("countryCode", "??")
        } catch (e: Exception) {
            "??"
        }
    }

    private fun ByteArray.indexOf(target: Byte, start: Int): Int {
        for (i in start until size) if (this[i] == target) return i
        return -1
    }
}
