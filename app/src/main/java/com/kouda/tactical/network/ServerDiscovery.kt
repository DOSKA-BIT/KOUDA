package com.kouda.tactical.network

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object ServerDiscovery {

    private val gson = Gson()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // Juegos soportados con sus IDs en Gametracker
    private val GAMES = listOf(
        "cs"    to "CS 1.6",
        "cs2"   to "CS:GO / CS2",
        "tf2"   to "TF2",
        "hl2dm" to "Half-Life 2 DM"
    )

    // Paises de Sudamerica para filtrar
    private val SA_COUNTRIES = listOf("AR", "BR", "CL", "UY", "PY", "PE", "CO", "VE", "BO", "EC")

    suspend fun discoverAndSave(
        context: Context,
        onProgress: (String) -> Unit = {}
    ): List<String> = withContext(Dispatchers.IO) {

        val prefs = context.getSharedPreferences("kouda_prefs", Context.MODE_PRIVATE)
        val allServers = mutableSetOf<String>()

        // Buscar en Gametracker por cada juego
        GAMES.forEach { (gameId, gameName) ->
            onProgress("Buscando servidores de $gameName...")
            val servers = fetchFromGametracker(gameId)
            Log.d("Discovery", "$gameName: ${servers.size} servidores encontrados")
            allServers.addAll(servers)
        }

        // Si Gametracker no devolvio nada, usar fallback
        if (allServers.isEmpty()) {
            Log.w("Discovery", "Gametracker returned nothing, using fallback")
            allServers.addAll(FALLBACK_SERVERS)
        }

        // Combinar con servidores existentes
        val existingJson = prefs.getString("servers", null)
        val existing: List<String> = if (existingJson != null) {
            try { gson.fromJson(existingJson, object : TypeToken<List<String>>() {}.type) }
            catch (e: Exception) { emptyList() }
        } else emptyList()

        val combined = (existing + allServers.toList()).distinct()
        prefs.edit().putString("servers", gson.toJson(combined)).apply()
        prefs.edit().putLong("last_discovery", System.currentTimeMillis()).apply()

        Log.d("Discovery", "Total guardados: ${combined.size}")
        combined
    }

    private fun fetchFromGametracker(gameId: String): List<String> {
        val servers = mutableListOf<String>()

        // Buscar por cada pais de SA
        SA_COUNTRIES.forEach { country ->
            try {
                val url = "https://www.gametracker.com/search/?search_by=server_variable" +
                    "&search_query=&loc=${country.lowercase()}" +
                    "&game=${gameId}&sort=0&order=DESC&start=0"

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Android 13; Mobile)")
                    .build()

                val body = httpClient.newCall(request).execute().body?.string() ?: return@forEach

                // Extraer IPs del HTML — Gametracker las muestra en links como /server_info/IP:PORT/
                val regex = Regex("""/server_info/(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}:\d{1,5})/""")
                regex.findAll(body).forEach { match ->
                    val ip = match.groupValues[1]
                    if (ip !in servers) servers.add(ip)
                }

            } catch (e: Exception) {
                Log.w("Discovery", "Gametracker error for $country/$gameId: ${e.message}")
            }
        }

        // Tambien buscar globalmente los mas populares
        try {
            val url = "https://www.gametracker.com/search/?game=${gameId}&sort=0&order=DESC&start=0"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android 13; Mobile)")
                .build()

            val body = httpClient.newCall(request).execute().body?.string() ?: return servers
            val regex = Regex("""/server_info/(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}:\d{1,5})/""")
            regex.findAll(body).forEach { match ->
                val ip = match.groupValues[1]
                if (ip !in servers) servers.add(ip)
            }
        } catch (e: Exception) {
            Log.w("Discovery", "Gametracker global error: ${e.message}")
        }

        return servers.take(30) // max 30 por juego
    }

    fun shouldDiscover(context: Context): Boolean {
        val prefs = context.getSharedPreferences("kouda_prefs", Context.MODE_PRIVATE)
        val lastDiscovery = prefs.getLong("last_discovery", 0L)
        val elapsed = System.currentTimeMillis() - lastDiscovery
        val oneDayMs = 24 * 60 * 60 * 1000L
        return elapsed > oneDayMs
    }

    // Fallback por si Gametracker falla
    private val FALLBACK_SERVERS = listOf(
        "45.235.98.50:27015",
        "200.58.160.11:27015",
        "186.10.4.22:27015",
        "177.54.144.200:27015",
        "190.210.86.33:27015",
        "64.74.97.44:27015",
        "45.235.98.51:27015",
        "190.210.86.34:27016"
    )
}
