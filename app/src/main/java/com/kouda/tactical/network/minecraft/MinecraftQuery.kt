package com.kouda.tactical.network.minecraft

import android.util.Log
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket

object MinecraftQuery {

    private const val TAG = "MinecraftQuery"
    private const val TIMEOUT_MS = 5000
    private const val PROTOCOL_VERSION = 47

    fun query(ip: String, port: Int = 25565): MinecraftServerInfo {
        return try {
            Socket().use { socket ->
                socket.soTimeout = TIMEOUT_MS
                socket.connect(InetSocketAddress(ip, port), TIMEOUT_MS)
                val out = DataInputStream(socket.getInputStream())
                val inp = DataOutputStream(socket.getOutputStream())
                sendHandshake(inp, ip, port)
                sendStatusRequest(inp)
                val t0 = System.currentTimeMillis()
                val json = readStatusResponse(out)
                val realPing = try {
                    val t1 = System.currentTimeMillis()
                    sendPingPacket(inp, t1)
                    readPongPacket(out)
                    (System.currentTimeMillis() - t1).toInt()
                } catch (e: Exception) { (System.currentTimeMillis() - t0).toInt() }
                parseJson(json, ip, port, realPing)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed $ip:$port — ${e.message}")
            offline(ip, port)
        }
    }

    private fun offline(ip: String, port: Int) = MinecraftServerInfo(
        ip = ip, port = port,
        name = if (port == 25565) ip else "$ip:$port",
        motdRaw = "", version = "—", protocolVersion = -1,
        curPlayers = 0, maxPlayers = 0, ping = -1,
        faviconBase64 = null, modType = null,
        mods = emptyList(), playerSample = emptyList(), isOnline = false
    )

    private fun sendHandshake(out: DataOutputStream, ip: String, port: Int) {
        val ipBytes = ip.toByteArray(Charsets.UTF_8)
        val payload = ByteArrayOutputStream().apply {
            writeVarInt(0x00)
            writeVarInt(PROTOCOL_VERSION)
            writeVarInt(ipBytes.size)
            write(ipBytes)
            write((port ushr 8) and 0xFF)
            write(port and 0xFF)
            writeVarInt(1)
        }.toByteArray()
        writeFramed(out, payload)
    }

    private fun sendStatusRequest(out: DataOutputStream) =
        writeFramed(out, ByteArrayOutputStream().apply { writeVarInt(0x00) }.toByteArray())

    private fun sendPingPacket(out: DataOutputStream, timestamp: Long) {
        val payload = ByteArrayOutputStream().apply {
            writeVarInt(0x01)
            for (i in 7 downTo 0) write(((timestamp ushr (i * 8)) and 0xFF).toInt())
        }.toByteArray()
        writeFramed(out, payload)
    }

    private fun readPongPacket(input: DataInputStream) {
        readVarInt(input); readVarInt(input); repeat(8) { input.read() }
    }

    private fun writeFramed(out: DataOutputStream, payload: ByteArray) {
        out.write(encodeVarInt(payload.size)); out.write(payload); out.flush()
    }

    private fun readStatusResponse(input: DataInputStream): String {
        readVarInt(input)
        val id = readVarInt(input)
        if (id != 0x00) error("Unexpected packet id: $id")
        val len = readVarInt(input)
        val buf = ByteArray(len)
        var read = 0
        while (read < len) {
            val n = input.read(buf, read, len - read)
            if (n < 0) error("Stream ended")
            read += n
        }
        return String(buf, Charsets.UTF_8)
    }

    private fun parseJson(raw: String, ip: String, port: Int, ping: Int): MinecraftServerInfo {
        val json = JSONObject(raw)

        val versionObj = json.optJSONObject("version")
        val version = versionObj?.optString("name", "—") ?: "—"
        val protocol = versionObj?.optInt("protocol", -1) ?: -1

        val playersObj = json.optJSONObject("players")
        val curPlayers = playersObj?.optInt("online", 0) ?: 0
        val maxPlayers = playersObj?.optInt("max", 0) ?: 0

        val playerSample = buildList {
            playersObj?.optJSONArray("sample")?.let { sample ->
                for (i in 0 until sample.length()) {
                    val name = sample.optJSONObject(i)?.optString("name") ?: continue
                    if (name.isNotBlank() && !name.startsWith("§")) add(name)
                }
            }
        }

        val rawMotd = if (json.has("description")) {
            val d = json.get("description")
            if (d is JSONObject) extractText(d) else d.toString()
        } else ""

        val (modType, mods) = extractMods(json)

        return MinecraftServerInfo(
            ip = ip, port = port,
            name = strip(rawMotd).trim().ifBlank { if (port == 25565) ip else "$ip:$port" },
            motdRaw = rawMotd, version = version, protocolVersion = protocol,
            curPlayers = curPlayers, maxPlayers = maxPlayers, ping = ping,
            faviconBase64 = json.optString("favicon", null),
            modType = modType, mods = mods, playerSample = playerSample, isOnline = true
        )
    }

    private fun extractText(obj: JSONObject): String = buildString {
        if (obj.has("text")) append(obj.getString("text"))
        obj.optJSONArray("extra")?.let { for (i in 0 until it.length()) {
            val item = it.get(i)
            append(if (item is JSONObject) extractText(item) else item.toString())
        }}
    }

    private fun strip(s: String) = s.replace(Regex("§[0-9a-fk-orA-FK-OR]"), "")

    private fun extractMods(json: JSONObject): Pair<String?, List<MinecraftMod>> {
        val forge = json.optJSONObject("forgeData") ?: json.optJSONObject("modinfo")
        if (forge != null) {
            val arr = forge.optJSONArray("modList") ?: forge.optJSONArray("mods")
            val mods = buildList {
                if (arr != null) for (i in 0 until minOf(arr.length(), 100)) {
                    val m = arr.getJSONObject(i)
                    add(MinecraftMod(m.optString("modid", m.optString("modId", "")), m.optString("version", "")))
                }
            }
            return "FORGE" to mods
        }
        if (json.toString().contains("fabric", ignoreCase = true)) return "FABRIC" to emptyList()
        return null to emptyList()
    }

    private fun encodeVarInt(value: Int): ByteArray {
        var v = value
        return buildList<Byte> {
            do {
                var b = (v and 0x7F).toByte()
                v = v ushr 7
                if (v != 0) b = (b.toInt() or 0x80).toByte()
                add(b)
            } while (v != 0)
        }.toByteArray()
    }

    private fun ByteArrayOutputStream.writeVarInt(value: Int) = write(encodeVarInt(value))

    private fun readVarInt(input: DataInputStream): Int {
        var result = 0; var shift = 0
        while (true) {
            val b = input.read().also { if (it == -1) error("Stream ended") }
            result = result or ((b and 0x7F) shl shift)
            if (b and 0x80 == 0) return result
            shift += 7
        }
    }
}
