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

                val out = DataOutputStream(socket.getOutputStream())
                val input = DataInputStream(socket.getInputStream())

                sendHandshake(out, ip, port)
                sendStatusRequest(out)

                val pingStart = System.currentTimeMillis()
                val jsonStr = readStatusResponse(input)
                val ping = (System.currentTimeMillis() - pingStart).toInt()

                val realPing = try {
                    val t = System.currentTimeMillis()
                    sendPingPacket(out, t)
                    readPongPacket(input)
                    (System.currentTimeMillis() - t).toInt()
                } catch (e: Exception) { ping }

                parseStatusJson(jsonStr, ip, port, realPing)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed $ip:$port → ${e.message}")
            MinecraftServerInfo(
                ip = ip, port = port, name = ip, motdRaw = "",
                version = "—", protocolVersion = -1,
                curPlayers = 0, maxPlayers = 0, ping = -1,
                faviconBase64 = null, modType = null,
                mods = emptyList(), isOnline = false
            )
        }
    }

    // ─── PACKET BUILDERS ─────────────────────────────────────────────────────

    private fun sendHandshake(out: DataOutputStream, ip: String, port: Int) {
        val ipBytes = ip.toByteArray(Charsets.UTF_8)
        val payload = ByteArrayOutputStream().apply {
            writeVarIntTo(this, 0x00)
            writeVarIntTo(this, PROTOCOL_VERSION)
            writeVarIntTo(this, ipBytes.size)
            write(ipBytes)
            // Puerto como big-endian short
            write((port ushr 8) and 0xFF)
            write(port and 0xFF)
            writeVarIntTo(this, 1)
        }.toByteArray()
        writeFramed(out, payload)
    }

    private fun sendStatusRequest(out: DataOutputStream) {
        val payload = ByteArrayOutputStream().apply {
            writeVarIntTo(this, 0x00)
        }.toByteArray()
        writeFramed(out, payload)
    }

    private fun sendPingPacket(out: DataOutputStream, timestamp: Long) {
        val payload = ByteArrayOutputStream().apply {
            writeVarIntTo(this, 0x01)
            // Long como 8 bytes big-endian
            for (i in 7 downTo 0) write(((timestamp ushr (i * 8)) and 0xFF).toInt())
        }.toByteArray()
        writeFramed(out, payload)
    }

    private fun readPongPacket(input: DataInputStream) {
        readVarInt(input) // length
        readVarInt(input) // packet id
        repeat(8) { input.read() } // timestamp (8 bytes)
    }

    private fun writeFramed(out: DataOutputStream, payload: ByteArray) {
        val lenBytes = encodeVarInt(payload.size)
        out.write(lenBytes)
        out.write(payload)
        out.flush()
    }

    // ─── STATUS RESPONSE ─────────────────────────────────────────────────────

    private fun readStatusResponse(input: DataInputStream): String {
        readVarInt(input) // total length
        val packetId = readVarInt(input)
        if (packetId != 0x00) error("Unexpected packet ID: $packetId")
        val strLen = readVarInt(input)
        val bytes = ByteArray(strLen)
        var read = 0
        while (read < strLen) {
            val n = input.read(bytes, read, strLen - read)
            if (n < 0) error("Stream ended")
            read += n
        }
        return String(bytes, Charsets.UTF_8)
    }

    // ─── JSON PARSER ─────────────────────────────────────────────────────────

    private fun parseStatusJson(jsonStr: String, ip: String, port: Int, ping: Int): MinecraftServerInfo {
        val json = JSONObject(jsonStr)

        val versionObj = json.optJSONObject("version")
        val versionName = versionObj?.optString("name", "—") ?: "—"
        val protocolVer = versionObj?.optInt("protocol", -1) ?: -1

        val playersObj = json.optJSONObject("players")
        val curPlayers = playersObj?.optInt("online", 0) ?: 0
        val maxPlayers = playersObj?.optInt("max", 0) ?: 0

        val rawMotd = when {
            json.has("description") -> {
                val desc = json.get("description")
                if (desc is JSONObject) extractText(desc) else desc.toString()
            }
            else -> ""
        }

        val (modType, mods) = extractMods(json)

        return MinecraftServerInfo(
            ip = ip, port = port,
            name = stripFormatting(rawMotd).trim().ifBlank { ip },
            motdRaw = rawMotd,
            version = versionName,
            protocolVersion = protocolVer,
            curPlayers = curPlayers,
            maxPlayers = maxPlayers,
            ping = ping,
            faviconBase64 = json.optString("favicon", null),
            modType = modType,
            mods = mods,
            isOnline = true
        )
    }

    private fun extractText(obj: JSONObject): String {
        val sb = StringBuilder()
        if (obj.has("text")) sb.append(obj.getString("text"))
        if (obj.has("extra")) {
            val extra = obj.getJSONArray("extra")
            for (i in 0 until extra.length()) {
                val item = extra.get(i)
                if (item is JSONObject) sb.append(extractText(item))
                else sb.append(item.toString())
            }
        }
        return sb.toString()
    }

    private fun stripFormatting(text: String): String =
        text.replace(Regex("§[0-9a-fk-orA-FK-OR]"), "")

    private fun extractMods(json: JSONObject): Pair<String?, List<MinecraftMod>> {
        val forgeData = json.optJSONObject("forgeData") ?: json.optJSONObject("modinfo")
        if (forgeData != null) {
            val modList = forgeData.optJSONArray("modList") ?: forgeData.optJSONArray("mods")
            val mods = mutableListOf<MinecraftMod>()
            if (modList != null) {
                for (i in 0 until minOf(modList.length(), 50)) {
                    val mod = modList.getJSONObject(i)
                    mods.add(MinecraftMod(
                        modId = mod.optString("modid", mod.optString("modId", "")),
                        version = mod.optString("version", "")
                    ))
                }
            }
            return "FORGE" to mods
        }
        if (json.toString().contains("fabric", ignoreCase = true)) return "FABRIC" to emptyList()
        return null to emptyList()
    }

    // ─── VARINT ──────────────────────────────────────────────────────────────

    private fun encodeVarInt(value: Int): ByteArray {
        var v = value
        val buf = mutableListOf<Byte>()
        do {
            var temp = (v and 0x7F).toByte()
            v = v ushr 7
            if (v != 0) temp = (temp.toInt() or 0x80).toByte()
            buf.add(temp)
        } while (v != 0)
        return buf.toByteArray()
    }

    private fun writeVarIntTo(out: ByteArrayOutputStream, value: Int) {
        out.write(encodeVarInt(value))
    }

    private fun readVarInt(input: DataInputStream): Int {
        var result = 0; var shift = 0; var b: Int
        do {
            b = input.read()
            if (b == -1) error("Stream ended reading VarInt")
            result = result or ((b and 0x7F) shl shift)
            shift += 7
        } while (b and 0x80 != 0)
        return result
    }
}
