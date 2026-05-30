package com.kouda.tactical.network

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SearchResult(
    val ip: String,
    val name: String,
    val map: String,
    val curPlayers: Int,
    val maxPlayers: Int,
    val country: String,
    val game: String,
    val ping: Int = 0
) {
    val players: String get() = "$curPlayers/$maxPlayers"
    val isFull: Boolean get() = maxPlayers > 0 && curPlayers >= maxPlayers
    val fillRatio: Float get() = if (maxPlayers > 0) curPlayers.toFloat() / maxPlayers else 0f
    val pingStr: String get() = if (ping > 0) "${ping}ms" else "—"
}

enum class BrowseGame(
    val label: String,
    val appId: Int,
    val folder: String
) {
    CS16("CS 1.6", 10, "cstrike"),
    CSGO("CS:GO", 730, "csgo"),
    TF2("TF2", 440, "tf"),
    HL("Half-Life", 70, "valve")
}

object ServerBrowser {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Trae los servidores mas activos de un juego.
     * Usa Steam Web API — nombres reales, sin scraping.
     * Primero trae globales ordenados por jugadores,
     * luego filtra para mostrar Sudamerica al tope.
     */
    fun topServers(game: BrowseGame, limit: Int = 40): List<SearchResult> {
        val results = mutableListOf<SearchResult>()

        // Intentar Steam API primero
        try {
            val steamResults = fetchFromSteam(game, limit)
            results.addAll(steamResults)
            Log.d("ServerBrowser", "${game.label}: ${results.size} from Steam")
        } catch (e: Exception) {
            Log.w("ServerBrowser", "Steam API failed for ${game.label}: ${e.message}")
        }

        // Si Steam devolvio poco, complementar con Gametracker
        if (results.size < 10) {
            try {
                val gtResults = fetchFromGametracker(game, limit - results.size)
                // Agregar sin duplicar
                gtResults.forEach { gt ->
                    if (results.none { it.ip == gt.ip }) results.add(gt)
                }
                Log.d("ServerBrowser", "${game.label}: +${gtResults.size} from Gametracker")
            } catch (e: Exception) {
                Log.w("ServerBrowser", "Gametracker failed: ${e.message}")
            }
        }

        // Ordenar: Sudamerica primero, luego por jugadores
        return results
            .sortedWith(
                compareByDescending<SearchResult> { isSouthAmerica(it.country) }
                    .thenByDescending { it.curPlayers }
            )
            .take(limit)
    }

    /**
     * Busca servidores por nombre usando Steam API
     */
    fun searchByName(query: String): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        return try {
            val encoded = java.net.URLEncoder.encode("*${query.trim()}*", "UTF-8")
            val url = "https://api.steampowered.com/IGameServersService/GetServerList/v1/" +
                "?filter=name_match\\$encoded&limit=30&key="
            val results = fetchSteamUrl(url, "unknown")
            Log.d("ServerBrowser", "Search '$query': ${results.size} results")
            results.sortedByDescending { it.curPlayers }
        } catch (e: Exception) {
            Log.e("ServerBrowser", "Search error: ${e.message}")
            emptyList()
        }
    }

    // ─── Steam Web API ────────────────────────────────────────────────────────

    private fun fetchFromSteam(game: BrowseGame, limit: Int): List<SearchResult> {
        // Traer los mas populares globalmente
        val url = "https://api.steampowered.com/IGameServersService/GetServerList/v1/" +
            "?filter=appid\\${game.appId}&limit=$limit&key="
        return fetchSteamUrl(url, game.folder).sortedByDescending { it.curPlayers }
    }

    private fun fetchSteamUrl(url: String, gameFolder: String): List<SearchResult> {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .build()

        val body = httpClient.newCall(request).execute().body?.string()
            ?: return emptyList()

        val json = JSONObject(body)
        val servers = json.optJSONObject("response")?.optJSONArray("servers")
            ?: return emptyList()

        val results = mutableListOf<SearchResult>()
        for (i in 0 until servers.length()) {
            val s = servers.getJSONObject(i)
            val addr = s.optString("addr", "").trim()
            if (addr.isBlank() || !addr.contains(":")) continue

            val name = s.optString("name", "").trim()
            if (name.isBlank()) continue

            val cur = s.optInt("players", 0)
            val max = s.optInt("max_players", 0)
            if (max <= 0 || max > 128) continue

            val folder = s.optString("gamedir", gameFolder)

            results.add(SearchResult(
                ip = addr,
                name = name.take(50),
                map = s.optString("map", "-"),
                curPlayers = cur,
                maxPlayers = max,
                country = guessCountry(addr),
                game = folder
            ))
        }
        return results
    }

    // ─── Gametracker fallback ─────────────────────────────────────────────────

    private fun fetchFromGametracker(game: BrowseGame, limit: Int): List<SearchResult> {
        val gameId = when (game) {
            BrowseGame.CS16 -> "cs"
            BrowseGame.CSGO -> "cs2"
            BrowseGame.TF2  -> "tf2"
            BrowseGame.HL   -> "hl"
        }

        val url = "https://www.gametracker.com/search/" +
            "?search_by=server_variable&search_query=&game=$gameId&sort=2&order=DESC"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .header("Accept", "text/html,*/*;q=0.8")
            .build()

        val body = httpClient.newCall(request).execute().body?.string()
            ?: return emptyList()

        return parseGametracker(body, game.folder).take(limit)
    }

    private fun parseGametracker(html: String, gameFolder: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()

        // Extraer filas de la tabla de Gametracker
        val rowRegex = Regex(
            """<tr[^>]*class="[^"]*(?:even|odd)[^"]*"[^>]*>(.*?)</tr>""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        )

        rowRegex.findAll(html).forEach { rowMatch ->
            try {
                val row = rowMatch.groupValues[1]

                // IP
                val ip = Regex("""/server_info/([\d.]+:\d+)/""")
                    .find(row)?.groupValues?.get(1) ?: return@forEach

                // Nombre — en el link del server
                val name = Regex("""<a[^>]*/server_info/[^>]*>([^<]+)</a>""", RegexOption.IGNORE_CASE)
                    .find(row)?.groupValues?.get(1)?.trim()
                    ?: return@forEach

                // Jugadores X/Y
                val playersMatch = Regex("""(\d+)\s*/\s*(\d+)""").find(row)
                val cur = playersMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val max = playersMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0
                if (max <= 0) return@forEach

                // Mapa
                val map = Regex("""/maps/([^/"]+)""", RegexOption.IGNORE_CASE)
                    .find(row)?.groupValues?.get(1) ?: "-"

                // Pais
                val country = Regex("""flag_([a-z]{2})\.(?:png|gif)""", RegexOption.IGNORE_CASE)
                    .find(row)?.groupValues?.get(1)?.uppercase() ?: "??"

                results.add(SearchResult(
                    ip = ip,
                    name = name.replace("&amp;", "&").replace("&#39;", "'").take(50),
                    map = map,
                    curPlayers = cur,
                    maxPlayers = max,
                    country = country,
                    game = gameFolder
                ))
            } catch (e: Exception) { /* skip malformed rows */ }
        }

        return results
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun isSouthAmerica(country: String): Boolean =
        country in setOf("AR", "BR", "CL", "UY", "PY", "PE", "CO", "VE", "BO", "EC")

    // Adivinanza basica de pais por rango de IP (solo para Steam results sin pais)
    private fun guessCountry(ip: String): String {
        val firstOctet = ip.split(".").firstOrNull()?.toIntOrNull() ?: return "??"
        return when (firstOctet) {
            in 177..200 -> "BR"
            in 181..190 -> "AR"
            in 186..190 -> "CL"
            else -> "??"
        }
    }
}
