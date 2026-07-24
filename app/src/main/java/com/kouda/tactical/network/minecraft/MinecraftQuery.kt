package com.kouda.tactical.network.minecraft

import android.util.Log
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Implementación del protocolo Server List Ping (SLP) de Minecraft Java Edition.
 *
 * Flujo:
 * 1. Abrir socket TCP al servidor
 * 2. Enviar Handshake packet (0x00) con estado 1 (status)
 * 3. Enviar Status Request (0x00 vacío)
 * 4. Leer Status Response → JSON con MOTD, versión, jugadores, favicon
 * 5. Enviar Ping packet (0x01) con timestamp
 * 6. Leer Pong → calcular latencia
 */
object MinecraftQuery {

    private const val TAG = "MinecraftQuery"
    private const val TIMEOUT_MS = 5000
    private const val PROTOCOL_VERSION = 47 // Compatible con 1.8+

    fun query(ip: String, port: Int = 25565): MinecraftServerInfo {
        val startTime = System.currentTimeMillis()
        return try {
            Socket().use { socket ->
                socket.soTimeout = TIMEOUT_MS
                socket.connect(InetSocketAddress(ip, port), TIMEOUT_MS)

                val out = DataOutputStream(socket.getOutputStream())
                val input = DataInputStream(socket.getInputStream())

                // 1. Handshake
                sendHandshake(out, ip, port)

                // 2. Status Request
                sendStatusRequest(out)

                // 3. Leer Status Response
                val jsonStr = readStatusResponse(input)
                val ping = (System.currentTimeMillis() - startTime).toInt()

                // 4. Ping/Pong para medir latencia real
                val realPing = try {
                    val pingStart = System.currentTimeMillis()
                    sendPingPacket(out)
                    readPongPacket(input)
                    (System.currentTimeMillis() - pingStart).toInt()
                } catch (e: Exception) {
                    ping
                }

                parseStatusJson(jsonStr, ip, port, realPing)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query $ip:$port → ${e.message}")
            MinecraftServerInfo(
                ip = ip, port = port,
                name = ip, motdRaw = "",
                version = "—", protocolVersion = -1,
                curPlayers = 0, maxPlayers = 0,
                ping = -1, faviconBase64 = null,
                modType = null, mods = emptyList(),
                isOnline = false
            )
        }
    }

    // ─── PACKET WRITERS ──────────────────────────────────────────────────────

    private fun sendHandshake(out: DataOutputStream, ip: String, port: Int) {
        val ipBytes = ip.toByteArray(Charsets.UTF_8)
        val payload = buildPacket {
            writeVarInt(0x00)                    // Packet ID: Handshake
            writeVarInt(PROTOCOL_VERSION)        // Protocol version
            writeVarInt(ipBytes.size)
            write(ipBytes)                        // Server address
            writeShort(port)                      // Server port
            writeVarInt(1)                        // Next state: Status
        }
        writePacketToStream(out, payload)
    }

    private fun sendStatusRequest(out: DataOutputStream) {
        val payload = buildPacket { writeVarInt(0x00) }
        writePacketToStream(out, payload)
    }

    private fun sendPingPacket(out: DataOutputStream) {
        val payload = buildPacket {
            writeVarInt(0x01)
            writeLong(System.currentTimeMillis())
        }
        writePacketToStream(out, payload)
    }

    private fun readPongPacket(input: DataInputStream) {
        readVarInt(input) // length
        readVarInt(input) // packet id (0x01)
        input.readLong()  // timestamp echo
    }

    // ─── PACKET READER ───────────────────────────────────────────────────────

    private fun readStatusResponse(input: DataInputStream): String {
        readVarInt(input) // total packet length
        val packetId = readVarInt(input)
        if (packetId != 0x00) error("Unexpected packet ID: $packetId")

        val strLen = readVarInt(input)
        val strBytes = ByteArray(strLen)
        var bytesRead = 0
        while (bytesRead < strLen) {
            val n = input.read(strBytes, bytesRead, strLen - bytesRead)
            if (n < 0) error("Stream ended prematurely")
            bytesRead += n
        }
        return String(strBytes, Charsets.UTF_8)
    }

    // ─── JSON PARSER ─────────────────────────────────────────────────────────

    private fun parseStatusJson(
        jsonStr: String, ip: String, port: Int, ping: Int
    ): MinecraftServerInfo {
        val json = JSONObject(jsonStr)

        // Versión
        val versionObj = json.optJSONObject("version")
        val versionName = versionObj?.optString("name", "—") ?: "—"
        val protocolVer = versionObj?.optInt("protocol", -1) ?: -1

        // Jugadores
        val playersObj = json.optJSONObject("players")
        val curPlayers = playersObj?.optInt("online", 0) ?: 0
        val maxPlayers = playersObj?.optInt("max", 0) ?: 0

        // MOTD
        val rawMotd = when {
            json.has("description") -> {
                val desc = json.get("description")
                when {
                    desc is JSONObject -> extractTextFromComponent(desc)
                    else -> desc.toString()
                }
            }
            else -> ""
        }
        val cleanMotd = stripMinecraftFormatting(rawMotd).trim()

        // Favicon
        val favicon = json.optString("favicon", null)

        // Mods (Forge/Fabric)
        val (modType, mods) = extractMods(json)

        return MinecraftServerInfo(
            ip = ip, port = port,
            name = cleanMotd.ifBlank { ip },
            motdRaw = rawMotd,
            version = versionName,
            protocolVersion = protocolVer,
            curPlayers = curPlayers,
            maxPlayers = maxPlayers,
            ping = ping,
            faviconBase64 = favicon,
            modType = modType,
            mods = mods,
            isOnline = true
        )
    }

    /** Extrae texto de un componente JSON de texto de Minecraft */
    private fun extractTextFromComponent(obj: JSONObject): String {
        val sb = StringBuilder()
        if (obj.has("text")) sb.append(obj.getString("text"))
        if (obj.has("extra")) {
            val extra = obj.getJSONArray("extra")
            for (i in 0 until extra.length()) {
                val item = extra.get(i)
                when (item) {
                    is JSONObject -> sb.append(extractTextFromComponent(item))
                    else -> sb.append(item.toString())
                }
            }
        }
        return sb.toString()
    }

    /** Elimina códigos de color y formato de Minecraft (§a, §l, etc.) */
    private fun stripMinecraftFormatting(text: String): String =
        text.replace(Regex("§[0-9a-fk-or]"), "")
            .replace(Regex("\\u00A7[0-9a-fk-or]"), "")

    /** Detecta si el servidor usa Forge o Fabric y extrae la lista de mods */
    private fun extractMods(json: JSONObject): Pair<String?, List<MinecraftMod>> {
        // Forge detecta mods en "forgeData" o "modinfo"
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

        // Fabric detecta en "quiltLoader" o texto del MOTD
        if (json.has("quiltLoader") || json.toString().contains("fabric", ignoreCase = true)) {
            return "FABRIC" to emptyList()
        }

        return null to emptyList()
    }

    // ─── VARINT HELPERS ──────────────────────────────────────────────────────

    private fun writeVarInt(value: Int): ByteArray {
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

    private fun readVarInt(input: DataInputStream): Int {
        var result = 0
        var shift = 0
        var b: Int
        do {
            b = input.read()
            if (b == -1) error("Stream ended while reading VarInt")
            result = result or ((b and 0x7F) shl shift)
            shift += 7
        } while (b and 0x80 != 0)
        return result
    }

    private fun buildPacket(block: java.io.ByteArrayOutputStream.() -> Unit): ByteArray {
        val baos = object : java.io.ByteArrayOutputStream() {
            fun writeVarInt(value: Int) {
                write(this@MinecraftQuery.writeVarInt(value))
            }
        }
        baos.block()
        return baos.toByteArray()
    }

    private fun writePacketToStream(out: DataOutputStream, payload: ByteArray) {
        val lenBytes = writeVarInt(payload.size)
        out.write(lenBytes)
        out.write(payload)
        out.flush()
    }
}
