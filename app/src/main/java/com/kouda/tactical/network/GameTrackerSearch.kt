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
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Busca servidores en Gametracker por query libre
     * Ejemplo: "dust2 argentina", "cs 1.6 brasil", "pepito gang"
     */
    fun search(query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()

        return try {
            val encoded = query.trim().replace(" ", "+")
            val url = "https://www.gametracker.com/search/" +
                "?search_by=server_name&search_query=$encoded&sort=2&order=DESC"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android 13; Mobile)")
                .build()

            val body = httpClient.newCall(request).execute().body?.string()
                ?: return emptyList()

            parseResults(body)
        } catch (e: Exception) {
            Log.e("GameTrackerSearch", "Search error: ${e.message}")
            emptyList()
        }
    }

    /**
     * Busca servidores por region y juego (para el descubrimiento automatico)
     */
    fun searchByGame(gameId: String, country: String = ""): List<SearchResult> {
        return try {
            val countryParam = if (country.isNotBlank()) "&loc=${country.lowercase()}" else ""
            val url = "https://www.gametracker.com/search/" +
                "?search_by=server_variable&search_query=&game=$gameId" +
                "$countryParam&sort=2&order=DESC"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android 13; Mobile)")
                .build()

            val body = httpClient.newCall(request).execute().body?.string()
                ?: return emptyList()

            parseResults(body)
        } catch (e: Exception) {
            Log.e("GameTrackerSearch", "SearchByGame error: ${e.message}")
            emptyList()
        }
    }

    private fun parseResults(html: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()

        // Extraer filas de la tabla de resultados
        val rowRegex = Regex(
            """<tr[^>]*class="[^"]*(?:even|odd)[^"]*"[^>]*>(.*?)</tr>""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        )

        // Extraer IP
        val ipRegex = Regex("""/server_info/(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}:\d{1,5})/""")
        // Extraer nombre del servidor
        val nameRegex = Regex("""title="([^"]{3,60})"""")
        // Extraer jugadores (formato "X/Y")
        val playersRegex = Regex("""(\d+)\s*/\s*(\d+)""")
        // Extraer mapa
        val mapRegex = Regex("""map=([a-z0-9_]+)""", RegexOption.IGNORE_CASE)
        // Extraer pais
        val countryRegex = Regex("""flag_(\w{2})\.png""")
        // Extraer juego
        val gameRegex = Regex("""/games/(\w+)/""")

        rowRegex.findAll(html).forEach { row ->
            val rowContent = row.groupValues[1]

            val ip = ipRegex.find(rowContent)?.groupValues?.get(1) ?: return@forEach
            val name = nameRegex.find(rowContent)?.groupValues?.get(1)
                ?.replace("&amp;", "&")
                ?.replace("&#39;", "'")
                ?.trim() ?: "Unknown"
            val playersMatch = playersRegex.find(rowContent)
            val curPlayers = playersMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val maxPlayers = playersMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0
            val map = mapRegex.find(rowContent)?.groupValues?.get(1) ?: "-"
            val country = countryRegex.find(rowContent)?.groupValues?.get(1)?.uppercase() ?: "??"
            val game = gameRegex.find(rowContent)?.groupValues?.get(1) ?: "unknown"

            results.add(
                SearchResult(
                    ip = ip,
                    name = name,
                    map = map,
                    curPlayers = curPlayers,
                    maxPlayers = maxPlayers,
                    country = country,
                    game = game
                )
            )
        }

        return results.distinctBy { it.ip }
    }
}
