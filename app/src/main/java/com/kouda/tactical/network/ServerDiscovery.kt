package com.kouda.tactical.network

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kouda.tactical.data.GameFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

object ServerDiscovery {

    private val gson = Gson()

    /**
     * Descubre servidores automaticamente para todos los juegos soportados.
     * Guarda los resultados en SharedPreferences para uso futuro.
     * Solo corre si es la primera vez o si el usuario lo pide explicitamente.
     */
    suspend fun discoverAndSave(
        context: Context,
        onProgress: (String) -> Unit = {}
    ): List<String> = withContext(Dispatchers.IO) {

        val prefs = context.getSharedPreferences("kouda_prefs", Context.MODE_PRIVATE)
        val allServers = mutableSetOf<String>()

        // Juegos a escanear
        val gameDirs = listOf("cstrike", "csgo", "tf", "valve")

        val jobs = gameDirs.map { gameDir ->
            async {
                onProgress("Buscando servidores de ${gameDirToName(gameDir)}...")
                Log.d("Discovery", "Querying master for $gameDir")
                val servers = MasterServer.queryServers(
                    gameDir = gameDir,
                    maxServers = 20 // 20 por juego = hasta 80 en total
                )
                Log.d("Discovery", "Found ${servers.size} servers for $gameDir")
                servers
            }
        }

        jobs.awaitAll().forEach { allServers.addAll(it) }

        // Si la master no devolvio nada, usar lista de respaldo hardcodeada
        if (allServers.isEmpty()) {
            Log.w("Discovery", "Master server returned nothing, using fallback list")
            allServers.addAll(FALLBACK_SERVERS)
        }

        // Combinar con servidores existentes sin borrar los que el usuario agrego
        val existingJson = prefs.getString("servers", null)
        val existing: List<String> = if (existingJson != null) {
            try { gson.fromJson(existingJson, object : TypeToken<List<String>>() {}.type) }
            catch (e: Exception) { emptyList() }
        } else emptyList()

        val combined = (existing + allServers.toList()).distinct()
        prefs.edit().putString("servers", gson.toJson(combined)).apply()

        // Marcar que ya se hizo el descubrimiento
        prefs.edit().putLong("last_discovery", System.currentTimeMillis()).apply()

        Log.d("Discovery", "Total servers saved: ${combined.size}")
        combined
    }

    /**
     * Devuelve true si nunca se hizo un descubrimiento o si paso mas de 24hs
     */
    fun shouldDiscover(context: Context): Boolean {
        val prefs = context.getSharedPreferences("kouda_prefs", Context.MODE_PRIVATE)
        val lastDiscovery = prefs.getLong("last_discovery", 0L)
        val elapsed = System.currentTimeMillis() - lastDiscovery
        val oneDayMs = 24 * 60 * 60 * 1000L
        return elapsed > oneDayMs
    }

    private fun gameDirToName(dir: String) = when (dir) {
        "cstrike" -> "CS 1.6"
        "csgo"    -> "CS:GO"
        "tf"      -> "TF2"
        "valve"   -> "Half-Life"
        else      -> dir
    }

    // Servidores de respaldo por si la Master Server falla
    private val FALLBACK_SERVERS = listOf(
        "45.235.98.50:27015",   // Argentina CS
        "200.58.160.11:27015",  // Argentina CS 1.6
        "186.10.4.22:27015",    // Chile CS
        "177.54.144.200:27015", // Brasil CS
        "190.210.86.33:27015",  // Argentina CS:GO
        "64.74.97.44:27015",    // SA CS 1.6
        "45.235.98.51:27015",   // Argentina CS 2
        "190.210.86.34:27016"   // Argentina TF2
    )
}
