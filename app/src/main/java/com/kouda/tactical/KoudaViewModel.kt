package com.kouda.tactical

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kouda.tactical.data.GameFilter
import com.kouda.tactical.data.PlayerInfo
import com.kouda.tactical.data.ServerInfo
import com.kouda.tactical.data.SortMode
import com.kouda.tactical.network.SourceQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class UiState(
    val servers: List<ServerInfo> = emptyList(),
    val isLoading: Boolean = false,
    val currentFilter: GameFilter = GameFilter.ALL,
    val sortMode: SortMode = SortMode.FAVORITES,
    val watchingIp: String? = null,
    val watchingName: String? = null,
    val slotAlert: String? = null,
    val scanResult: List<PlayerInfo>? = null,
    val isScanning: Boolean = false,
    val totalOnline: Int = 0
)

class KoudaViewModel(application: Application) : AndroidViewModel(application) {

    private val gson = Gson()
    private val prefs = application.getSharedPreferences("kouda_prefs", Context.MODE_PRIVATE)
    private val workManager = WorkManager.getInstance(application)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val defaultServers = listOf("45.235.98.50:27015")

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true, servers = emptyList(), totalOnline = 0) }
            val favs = loadFavs()
            val servers = loadServers()
            val combined = (favs + servers.filter { it !in favs }).distinct()

            val results = combined.map { addr ->
                async {
                    val (info, _) = SourceQuery.queryServer(addr)
                    info?.copy(isFav = addr in favs)
                }
            }.awaitAll().filterNotNull()

            val total = results.sumOf { it.curPlayers }
            _state.update { it.copy(servers = results, isLoading = false, totalOnline = total) }
        }
    }

    fun setFilter(filter: GameFilter) = _state.update { it.copy(currentFilter = filter) }

    fun setSortMode(mode: SortMode) = _state.update { it.copy(sortMode = mode) }

    fun toggleFavorite(ip: String) {
        val favs = loadFavs().toMutableList()
        if (ip in favs) favs.remove(ip) else favs.add(ip)
        saveFavs(favs)
        _state.update { s ->
            s.copy(servers = s.servers.map {
                if (it.ip == ip) it.copy(isFav = !it.isFav) else it
            })
        }
    }

    fun addServer(address: String) {
        val servers = loadServers().toMutableList()
        if (address !in servers) {
            servers.add(address)
            saveServers(servers)
            refresh()
        }
    }

    fun removeServer(ip: String) {
        val servers = loadServers().toMutableList()
        servers.remove(ip)
        saveServers(servers)
        val favs = loadFavs().toMutableList()
        favs.remove(ip)
        saveFavs(favs)
        cancelWatch()
        _state.update { s -> s.copy(servers = s.servers.filter { it.ip != ip }) }
    }

    fun scanPlayers(ip: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isScanning = true, scanResult = null) }
            val (_, players) = SourceQuery.queryServer(ip, getPlayers = true)
            _state.update { it.copy(isScanning = false, scanResult = players) }
        }
    }

    fun clearScanResult() = _state.update { it.copy(scanResult = null, isScanning = false) }

    // ─── SLOT WATCHER con WorkManager ────────────────────────────────────────

    fun watchSlot(ip: String, serverName: String) {
        // Cancelar cualquier watcher anterior
        workManager.cancelAllWorkByTag(SlotWorker.WORK_TAG)

        val inputData = Data.Builder()
            .putString(SlotWorker.KEY_IP, ip)
            .putString(SlotWorker.KEY_SERVER_NAME, serverName)
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Chequea cada 30 segundos, con backoff exponencial si falla
        val workRequest = OneTimeWorkRequestBuilder<SlotWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                30, TimeUnit.SECONDS
            )
            .addTag(SlotWorker.WORK_TAG)
            .build()

        workManager.enqueue(workRequest)
        _state.update { it.copy(watchingIp = ip, watchingName = serverName) }
    }

    fun cancelWatch() {
        workManager.cancelAllWorkByTag(SlotWorker.WORK_TAG)
        _state.update { it.copy(watchingIp = null, watchingName = null) }
    }

    fun clearAlert() = _state.update { it.copy(slotAlert = null) }

    fun filteredAndSorted(): List<ServerInfo> {
        val s = _state.value
        val filtered = if (s.currentFilter == GameFilter.ALL) s.servers
        else s.servers.filter { it.folder == s.currentFilter.tag }
        return when (s.sortMode) {
            SortMode.PING -> filtered.sortedBy { it.ping }
            SortMode.PLAYERS -> filtered.sortedByDescending { it.curPlayers }
            SortMode.NAME -> filtered.sortedBy { it.name.lowercase() }
            SortMode.FAVORITES -> filtered.sortedWith(
                compareByDescending<ServerInfo> { it.isFav }.thenBy { it.ping }
            )
        }
    }

    private fun loadServers(): List<String> {
        val json = prefs.getString("servers", null) ?: return defaultServers
        return gson.fromJson(json, object : TypeToken<List<String>>() {}.type)
    }
    private fun saveServers(list: List<String>) =
        prefs.edit().putString("servers", gson.toJson(list)).apply()
    private fun loadFavs(): List<String> {
        val json = prefs.getString("favs", null) ?: return emptyList()
        return gson.fromJson(json, object : TypeToken<List<String>>() {}.type)
    }
    private fun saveFavs(list: List<String>) =
        prefs.edit().putString("favs", gson.toJson(list)).apply()
}
