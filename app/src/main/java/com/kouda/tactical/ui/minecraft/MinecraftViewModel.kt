package com.kouda.tactical.ui.minecraft

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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

data class MinecraftUiState(
    val servers: List<MinecraftServerInfo> = emptyList(),
    val isLoading: Boolean = false,
    val selectedServer: MinecraftServerInfo? = null,
    val addError: String? = null
)

class MinecraftViewModel(application: Application) : AndroidViewModel(application) {

    private val gson = Gson()
    private val prefs = application.getSharedPreferences("kouda_minecraft", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(MinecraftUiState())
    val state: StateFlow<MinecraftUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true, servers = emptyList()) }
            val saved = loadSaved()
            val results = saved.map { s ->
                async { MinecraftQuery.query(s.ip, s.port) }
            }.awaitAll()
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
        _state.update { it.copy(addError = null) }

        // Guardar inmediatamente con placeholder cargando
        val placeholder = MinecraftServerInfo(
            ip = ip, port = port, name = "$ip:$port", motdRaw = "",
            version = "...", protocolVersion = -1,
            curPlayers = 0, maxPlayers = 0, ping = -2,
            faviconBase64 = null, modType = null, mods = emptyList(), isOnline = false
        )
        saved.add(SavedMinecraftServer(ip, port))
        saveSaved(saved)
        _state.update { s -> s.copy(servers = s.servers + placeholder) }

        viewModelScope.launch(Dispatchers.IO) {
            val result = MinecraftQuery.query(ip, port)
            _state.update { s ->
                s.copy(servers = s.servers.map {
                    if (it.ip == ip && it.port == port) result else it
                })
            }
        }
    }

    fun removeServer(ip: String, port: Int) {
        val saved = loadSaved().toMutableList()
        saved.removeAll { it.ip == ip && it.port == port }
        saveSaved(saved)
        _state.update { s -> s.copy(servers = s.servers.filter { !(it.ip == ip && it.port == port) }) }
    }

    fun selectServer(server: MinecraftServerInfo?) {
        _state.update { it.copy(selectedServer = server) }
    }

    fun clearError() = _state.update { it.copy(addError = null) }

    private fun parseAddress(input: String): Pair<String, Int>? {
        val trimmed = input.trim()
        return if (trimmed.contains(":")) {
            val parts = trimmed.split(":")
            val port = parts.last().toIntOrNull() ?: return null
            val ip = parts.dropLast(1).joinToString(":")
            if (ip.isBlank() || port !in 1..65535) null else ip to port
        } else {
            if (trimmed.isBlank()) null else trimmed to 25565
        }
    }

    private fun loadSaved(): List<SavedMinecraftServer> {
        val json = prefs.getString("mc_servers", null) ?: return emptyList()
        return try {
            gson.fromJson(json, object : TypeToken<List<SavedMinecraftServer>>() {}.type)
        } catch (e: Exception) { emptyList() }
    }

    private fun saveSaved(list: List<SavedMinecraftServer>) =
        prefs.edit().putString("mc_servers", gson.toJson(list)).apply()
}
