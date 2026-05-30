package com.kouda.tactical.network

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class SearchResult(
    val ip: String,
    val name: String,
    val map: String,
    val curPlayers: Int,
    val maxPlayers: Int,
    val country: String,
    val game: String
) {
    val players: String get() = "$curPlayers/$maxPlayers"
    val isFull: Boolean get() = maxPlayers > 0 && curPlayers >= maxPlayers
    val fillRatio: Float get() = if (maxPlayers > 0) curPlayers.toFloat() / maxPlayers else 0f
}

object GameTrackerSearch {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // User-Agent de desktop para evitar bloqueos
    private const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
        "AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/124.0.0.0 Safari/537.36"

    fun search(query: String): List<SearchResult> {
        if (query.isBlank()) return emptyList()

        // Intentar multiples estrategias
        val results = mutableListOf<SearchResult>()

        // 1. Buscar por nombre en Gametracker
        results.addAll(searchGametracker(query))

        // 2. Si no hay resultados, buscar via API alternativa
        if (results.isEmpty()) {
            results.addAll(searchFallback(query))
        }

        return results.distinctBy { it.ip }
    }

    private fun searchGametracker(query: String): List<SearchResult> {
        return try {
            val encoded = java.net.URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://www.gametracker.com/search/" +
                "?search_by=server_name&search_query=$encoded"

            Log.d("GTSearch", "Fetching: $url")

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", UA)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "es-AR,es;q=0.9,en;q=0.8")
                .header("Accept-Encoding", "identity")
                .header("Connection", "keep-alive")
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: run {
                Log.w("GTSearch", "Empty body")
                return emptyList()
            }

            Log.d("GTSearch", "Response length: ${body.length}")

            val results = parseGametracker(body)
            Log.d("GTSearch", "Parsed ${results.size} results")
            results
        } catch (e: Exception) {
            Log.e("GTSearch", "Gametracker error: ${e.message}")
            emptyList()
        }
    }

    private fun parseGametracker(html: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()

        // Patron principal: extraer bloques de servidor
        // Gametracker usa /server_info/IP:PORT/ en los links
        val ipPattern = Regex("""/server_info/([\d.]+:\d+)/""")
        val ips = ipPattern.findAll(html).map { it.groupValues[1] }.distinct().toList()

        Log.d("GTSearch", "Found ${ips.size} IPs in HTML")

        if (ips.isEmpty()) {
            // Log primeros 500 chars para debug
            Log.d("GTSearch", "HTML preview: ${html.take(500)}")
            return emptyList()
        }

        // Para cada IP extraer contexto cercano
        ips.forEach { ip ->
            try {
                // Encontrar la posicion de esta IP en el HTML
                val pos = html.indexOf(ip)
                if (pos == -1) return@forEach

                // Extraer ventana de 800 chars alrededor de la IP
                val start = maxOf(0, pos - 400)
                val end = minOf(html.length, pos + 400)
                val context = html.substring(start, end)

                // Nombre del servidor — buscar en title o alt attributes
                val name = extractName(context, ip)
                val map = extractMap(context)
                val players = extractPlayers(context)
                val country = extractCountry(context)
                val game = extractGame(context)

                results.add(SearchResult(
                    ip = ip,
                    name = name,
                    map = map,
                    curPlayers = players.first,
                    maxPlayers = players.second,
                    country = country,
                    game = game
                ))
            } catch (e: Exception) {
                Log.w("GTSearch", "Error parsing server $ip: ${e.message}")
            }
        }

        return results
    }

    private fun extractName(context: String, ip: String): String {
        // Varios patrones de nombre
        val patterns = listOf(
            Regex("""title="([^"]{3,60})""""),
            Regex("""alt="([^"]{3,60})""""),
            Regex("""<a[^>]*server_info[^>]*>([^<]{3,60})</a>""", RegexOption.IGNORE_CASE),
            Regex("""class="[^"]*server[^"]*"[^>]*>([^<]{3,60})<""", RegexOption.IGNORE_CASE)
        )
        for (pattern in patterns) {
            val match = pattern.find(context)?.groupValues?.get(1)
            if (!match.isNullOrBlank() && !match.contains("http") && match != ip) {
                return match.replace("&amp;", "&").replace("&#39;", "'").trim()
            }
        }
        return "Servidor $ip"
    }

    private fun extractMap(context: String): String {
        val patterns = listOf(
            Regex("""map=([a-z0-9_]+)""", RegexOption.IGNORE_CASE),
            Regex("""/maps/([a-z0-9_]+)""", RegexOption.IGNORE_CASE),
            Regex("""Map:\s*([a-z0-9_]+)""", RegexOption.IGNORE_CASE)
        )
        for (p in patterns) {
            val m = p.find(context)?.groupValues?.get(1)
            if (!m.isNullOrBlank()) return m
        }
        return "-"
    }

    private fun extractPlayers(context: String): Pair<Int, Int> {
        val patterns = listOf(
            Regex("""(\d+)\s*/\s*(\d+)\s*players""", RegexOption.IGNORE_CASE),
            Regex("""(\d+)\s*/\s*(\d+)""")
        )
        for (p in patterns) {
            val m = p.find(context)
            if (m != null) {
                val cur = m.groupValues[1].toIntOrNull() ?: 0
                val max = m.groupValues[2].toIntOrNull() ?: 0
                if (max > 0 && max <= 128) return cur to max
            }
        }
        return 0 to 0
    }

    private fun extractCountry(context: String): String {
        val patterns = listOf(
            Regex("""flag_([a-z]{2})\.(?:png|gif|svg)""", RegexOption.IGNORE_CASE),
            Regex("""country=([a-z]{2})""", RegexOption.IGNORE_CASE),
            Regex("""/flags?/([a-z]{2})""", RegexOption.IGNORE_CASE)
        )
        for (p in patterns) {
            val m = p.find(context)?.groupValues?.get(1)?.uppercase()
            if (!m.isNullOrBlank() && m.length == 2) return m
        }
        return "??"
    }

    private fun extractGame(context: String): String {
        return when {
            context.contains("cstrike", ignoreCase = true) -> "cstrike"
            context.contains("csgo", ignoreCase = true) ||
            context.contains("cs2", ignoreCase = true) -> "csgo"
            context.contains("/tf2", ignoreCase = true) ||
            context.contains("teamfortress", ignoreCase = true) -> "tf"
            context.contains("valve", ignoreCase = true) -> "valve"
            else -> "unknown"
        }
    }

    // Fallback: buscar via API publica de steam para CS:GO
    private fun searchFallback(query: String): List<SearchResult> {
        return try {
            val encoded = java.net.URLEncoder.encode(query.trim(), "UTF-8")
            // Usar la API de Steam sin key (devuelve resultados limitados)
            val url = "https://api.steampowered.com/IGameServersService/GetServerList/v1/" +
                "?filter=name_match=*${encoded}*&limit=20&key="

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", UA)
                .build()

            val body = request.let { httpClient.newCall(it).execute().body?.string() }
                ?: return emptyList()

            val json = org.json.JSONObject(body)
            val servers = json.optJSONObject("response")?.optJSONArray("servers")
                ?: return emptyList()

            val results = mutableListOf<SearchResult>()
            for (i in 0 until servers.length()) {
                val s = servers.getJSONObject(i)
                val addr = s.optString("addr", "")
                if (addr.isBlank() || !addr.contains(":")) continue
                results.add(SearchResult(
                    ip = addr,
                    name = s.optString("name", "Servidor $addr"),
                    map = s.optString("map", "-"),
                    curPlayers = s.optInt("players", 0),
                    maxPlayers = s.optInt("max_players", 0),
                    country = "??",
                    game = s.optString("gamedir", "unknown")
                ))
            }
            Log.d("GTSearch", "Steam fallback: ${results.size} results")
            results
        } catch (e: Exception) {
            Log.e("GTSearch", "Fallback error: ${e.message}")
            emptyList()
        }
    }

    fun searchByGame(gameId: String, country: String = ""): List<SearchResult> {
        return try {
            val countryParam = if (country.isNotBlank()) "&loc=${country.lowercase()}" else ""
            val url = "https://www.gametracker.com/search/" +
                "?search_by=server_variable&search_query=&game=$gameId" +
                "$countryParam&sort=2&order=DESC"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", UA)
                .header("Accept", "text/html,application/xhtml+xml,*/*;q=0.8")
                .build()

            val body = httpClient.newCall(request).execute().body?.string() ?: return emptyList()
            parseGametracker(body)
        } catch (e: Exception) {
            Log.e("GTSearch", "SearchByGame error: ${e.message}")
            emptyList()
        }
    }
}
