package com.kouda.tactical.ui.minecraft

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kouda.tactical.MinecraftSlotWorker
import com.kouda.tactical.network.minecraft.MinecraftQuery
import com.kouda.tactical.network.minecraft.MinecraftServerInfo
import com.kouda.tactical.network.minecraft.SavedMinecraftServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class MinecraftUiState(
    val servers: List<MinecraftServerInfo> = emptyList(),
    val isLoading: Boolean = false,
    val scanResult: List<String>? = null,  // lista de jugadores del playerSample
    val isScanning: Boolean = false,
    val addError: String? = null,
    val watchedServers: Set<String> = emptySet()  // "ip:port" keys
)

class MinecraftViewModel(application: Application) : AndroidViewModel(application) {

    private val gson = Gson()
    private val prefs = application.getSharedPreferences("kouda_minecraft", Context.MODE_PRIVATE)
    private val workManager = WorkManager.getInstance(application)

    private val _state = MutableStateFlow(MinecraftUiState())
    val state: StateFlow<MinecraftUiState> = _state.asStateFlow()

    init {
        val watched = prefs.getStringSet("mc_watched", emptySet()) ?: emptySet()
        _state.update { it.copy(watchedServers = watched) }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true, servers = emptyList()) }
            val saved = loadSaved()
            val results = saved.map { s -> async { MinecraftQuery.query(s.ip, s.port) } }.awaitAll()
            _state.update { it.copy(servers = results, isLoading = false) }
        }
    }

    fun addServer(input: String) {
        val (ip, port) = parseAddress(input) ?: run {
            _state.update { it.copy(addError = "Formato inválido. Usá IP o IP:PUERTO") }
            return
        }
        val saved = loadSaved().toMutableList()
        if (saved.any { it.ip == ip && it.port == port }) {
            _state.update { it.copy(addError = "El servidor ya está en la lista") }
            return
        }
        saved.add(SavedMinecraftServer(ip, port))
        saveSaved(saved)
        _state.update { it.copy(addError = null) }

        // Placeholder inmediato mientras consulta en background
        val placeholder = MinecraftServerInfo(
            ip = ip, port = port,
            name = if (port == 25565) ip else "$ip:$port",
            motdRaw = "", version = "...", protocolVersion = -1,
            curPlayers = 0, maxPlayers = 0, ping = -2,
            faviconBase64 = null, modType = null,
            mods = emptyList(), playerSample = emptyList(), isOnline = false
        )
        _state.update { it.copy(servers = it.servers + placeholder) }

        viewModelScope.launch(Dispatchers.IO) {
            val result = MinecraftQuery.query(ip, port)
            _state.update { s ->
                s.copy(servers = s.servers.map { if (it.ip == ip && it.port == port) result else it })
            }
        }
    }

    fun removeServer(ip: String, port: Int) {
        cancelWatch(ip, port)
        val saved = loadSaved().toMutableList().also { it.removeAll { s -> s.ip == ip && s.port == port } }
        saveSaved(saved)
        _state.update { it.copy(servers = it.servers.filter { s -> !(s.ip == ip && s.port == port) }) }
    }

    // Muestra el playerSample que ya viene en la respuesta SLP — sin red adicional
    fun scanPlayers(ip: String, port: Int) {
        val server = _state.value.servers.find { it.ip == ip && it.port == port } ?: return
        if (!server.isOnline) {
            _state.update { it.copy(scanResult = emptyList()) }
            return
        }
        if (server.playerSample.isNotEmpty()) {
            _state.update { it.copy(scanResult = server.playerSample) }
            return
        }
        // Si el sample está vacío, hacer una consulta fresca por si el servidor los expone ahora
        _state.update { it.copy(isScanning = true, scanResult = null) }
        viewModelScope.launch(Dispatchers.IO) {
            val fresh = MinecraftQuery.query(ip, port)
            _state.update { it.copy(isScanning = false, scanResult = fresh.playerSample) }
        }
    }

    fun clearScan() = _state.update { it.copy(scanResult = null, isScanning = false) }

    fun toggleAutoWatch(ip: String, port: Int, serverName: String) {
        val key = "$ip:$port"
        val watched = _state.value.watchedServers.toMutableSet()
        if (key in watched) {
            watched.remove(key)
            cancelWatch(ip, port)
        } else {
            watched.add(key)
            val server = _state.value.servers.find { it.ip == ip && it.port == port }
            if (server?.isFull == true) scheduleWatch(ip, port, serverName)
        }
        prefs.edit().putStringSet("mc_watched", watched).apply()
        _state.update { it.copy(watchedServers = watched) }
    }

    fun isWatched(ip: String, port: Int) = "$ip:$port" in _state.value.watchedServers

    fun clearError() = _state.update { it.copy(addError = null) }

    private fun scheduleWatch(ip: String, port: Int, serverName: String) {
        val data = Data.Builder()
            .putString(MinecraftSlotWorker.KEY_IP, ip)
            .putInt(MinecraftSlotWorker.KEY_PORT, port)
            .putString(MinecraftSlotWorker.KEY_NAME, serverName)
            .build()
        val req = OneTimeWorkRequestBuilder<MinecraftSlotWorker>()
            .setInputData(data)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
            .addTag(MinecraftSlotWorker.WORK_TAG)
            .build()
        workManager.enqueueUniqueWork("mc_watch_$ip:$port", ExistingWorkPolicy.REPLACE, req)
    }

    private fun cancelWatch(ip: String, port: Int) =
        workManager.cancelUniqueWork("mc_watch_$ip:$port")

    private fun parseAddress(input: String): Pair<String, Int>? {
        val t = input.trim()
        return if (t.contains(":")) {
            val parts = t.split(":")
            val port = parts.last().toIntOrNull() ?: return null
            val ip = parts.dropLast(1).joinToString(":")
            if (ip.isBlank() || port !in 1..65535) null else ip to port
        } else {
            if (t.isBlank()) null else t to 25565
        }
    }

    private fun loadSaved(): List<SavedMinecraftServer> {
        val json = prefs.getString("mc_servers", null) ?: return emptyList()
        return try { gson.fromJson(json, object : TypeToken<List<SavedMinecraftServer>>() {}.type) }
        catch (e: Exception) { emptyList() }
    }

    private fun saveSaved(list: List<SavedMinecraftServer>) =
        prefs.edit().putString("mc_servers", gson.toJson(list)).apply()
}
