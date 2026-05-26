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

object SourceQuery {

    private val httpClient = OkHttpClient.Builder()
        .callTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val QUERY_INFO = byteArrayOf(
        0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
        0x54,
        *"Source Engine Query\u0000".toByteArray(Charsets.UTF_8)
    )

    fun queryServer(address: String, getPlayers: Boolean = false): Pair<ServerInfo?, List<PlayerInfo>> {
        return try {
            val parts = address.split(":")
            if (parts.size != 2) return null to emptyList()
            val ip = parts[0]
            val port = parts[1].toIntOrNull() ?: return null to emptyList()

            val addr = InetAddress.getByName(ip)
            val socket = DatagramSocket()
            socket.soTimeout = 2000

            val startTime = System.currentTimeMillis()
            socket.send(DatagramPacket(QUERY_INFO, QUERY_INFO.size, addr, port))

            val buf = ByteArray(4096)
            val response = DatagramPacket(buf, buf.size)
            socket.receive(response)
            val ping = (System.currentTimeMillis() - startTime).toInt()

            val data = response.data.copyOf(response.length)
            val info = parseInfoResponse(data, ip, address, ping)

            val country = fetchCountry(ip)
            val playerList = if (getPlayers) queryPlayersWithRetry(addr, port) else emptyList()
            socket.close()

            info?.copy(country = country) to playerList
        } catch (e: Exception) {
            null to emptyList()
        }
    }

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

    // Reintenta hasta 3 veces con distintos timeouts
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

            // Paso 1: pedir challenge
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

            // Paso 2: si nos manda challenge (0x41), re-enviar con el numero correcto
            if (data.size >= 9 && data[4] == 0x41.toByte()) {
                val challengeNum = data.copyOfRange(5, 9)
                val realReq = byteArrayOf(
                    0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
                    0x55
                ) + challengeNum
                socket.send(DatagramPacket(realReq, realReq.size, addr, port))
                socket.receive(resp)
                data = resp.data.copyOf(resp.length)
            }

            // Paso 3: parsear respuesta de jugadores (0x44)
            if (data.size < 6 || data[4] != 0x44.toByte()) return emptyList()

            val players = mutableListOf<PlayerInfo>()
            var ptr = 5
            val numPlayers = data[ptr].toInt() and 0xFF
            ptr++

            repeat(numPlayers) {
                if (ptr >= data.size) return@repeat
                ptr++ // skip index byte
                val nameEnd = data.indexOf(0, ptr)
                if (nameEnd == -1 || nameEnd + 8 > data.size) return@repeat
                val pName = String(data, ptr, nameEnd - ptr, Charsets.UTF_8).trim()
                val score = ByteBuffer.wrap(data, nameEnd + 1, 4)
                    .order(ByteOrder.LITTLE_ENDIAN).int
                ptr = nameEnd + 9
                if (pName.isNotBlank()) {
                    players.add(PlayerInfo(pName, score))
                }
            }

            players.sortedByDescending { it.score }
        } catch (e: Exception) {
            emptyList()
        } finally {
            socket.close()
        }
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
