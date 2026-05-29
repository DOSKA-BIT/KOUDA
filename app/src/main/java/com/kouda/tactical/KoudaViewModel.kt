package com.kouda.tactical

import com.kouda.tactical.network.ServerDiscovery
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
import com.kouda.tactical.data.GameFilter
import com.kouda.tactical.data.PlayerInfo
import com.kouda.tactical.data.ServerHistory
import com.kouda.tactical.data.ServerInfo
import com.kouda.tactical.data.ServerSnapshot
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
    val totalOnline: Int = 0,
    val histories: Map<String, ServerHistory> = emptyMap()
)

class KoudaViewModel(application: Application) : AndroidViewModel(application) {

    private val gson = Gson()
    private val prefs = application.getSharedPreferences("kouda_prefs", Context.MODE_PRIVATE)
    private val workManager = WorkManager.getInstance(application)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val defaultServers = listOf("45.235.98.50:27015")

    init {
    val app = getApplication<Application>()
    if (ServerDiscovery.shouldDiscover(app)) {
        viewModelScope.launch(Dispatchers.IO) {
            ServerDiscovery.discoverAndSave(app)
            refresh()
        }
    } else {
        refresh()
    }
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true, servers = emptyList(), totalOnline = 0) }
            val favs = loadFavs()
            val autoWatched = loadAutoWatch()
            val servers = loadServers()
            val combined = (favs + servers.filter { it !in favs }).distinct()

            val results = combined.map { addr ->
                async {
                    val (info, _) = SourceQuery.queryServer(addr)
                    info?.copy(
                        isFav = addr in favs,
                        autoWatch = addr in autoWatched
                    )
                }
            }.awaitAll().filterNotNull()

            val now = System.currentTimeMillis()
            val histories = loadAllHistories().toMutableMap()
            results.forEach { server ->
                val snap = ServerSnapshot(now, server.curPlayers, server.maxPlayers)
                val existing = histories[server.ip] ?: ServerHistory(server.ip)
                val updated = existing.copy(
                    snapshots = (existing.snapshots + snap).takeLast(200)
                )
                histories[server.ip] = updated
            }
            saveAllHistories(histories)

            val total = results.sumOf { it.curPlayers }
            _state.update {
                it.copy(
                    servers = results,
                    isLoading = false,
                    totalOnline = total,
                    histories = histories
                )
            }

            results.filter { it.autoWatch && it.isFull }.forEach { server ->
                scheduleWatch(server.ip, server.name)
            }

            // Actualizar cache del widget despues de tener los datos
            val app = getApplication<Application>()
            WidgetRefreshCallback.refreshWidgetData(app)
        }
    }

    fun getHistory(ip: String): ServerHistory? = _state.value.histories[ip]

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

    fun toggleAutoWatch(ip: String) {
        val autoWatched = loadAutoWatch().toMutableList()
        val server = _state.value.servers.find { it.ip == ip } ?: return
        if (ip in autoWatched) {
            autoWatched.remove(ip)
            workManager.cancelUniqueWork("watch_$ip")
        } else {
            autoWatched.add(ip)
            if (server.isFull) scheduleWatch(ip, server.name)
        }
        saveAutoWatch(autoWatched)
        _state.update { s ->
            s.copy(servers = s.servers.map {
                if (it.ip == ip) it.copy(autoWatch = !it.autoWatch) else it
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
        val autoWatched = loadAutoWatch().toMutableList()
        autoWatched.remove(ip)
        saveAutoWatch(autoWatched)
        workManager.cancelUniqueWork("watch_$ip")
        val histories = loadAllHistories().toMutableMap()
        histories.remove(ip)
        saveAllHistories(histories)
        _state.update { s ->
            s.copy(
                servers = s.servers.filter { it.ip != ip },
                histories = s.histories - ip
            )
        }
    }

    fun scanPlayers(ip: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isScanning = true, scanResult = null) }
            val (_, players) = SourceQuery.queryServer(ip, getPlayers = true)
            _state.update { it.copy(isScanning = false, scanResult = players) }
        }
    }

    fun clearScanResult() = _state.update { it.copy(scanResult = null, isScanning = false) }

    fun watchSlot(ip: String, serverName: String) {
        scheduleWatch(ip, serverName)
        _state.update { it.copy(watchingIp = ip, watchingName = serverName) }
    }

    fun cancelWatch() {
        val ip = _state.value.watchingIp ?: return
        val server = _state.value.servers.find { it.ip == ip }
        if (server?.autoWatch != true) workManager.cancelUniqueWork("watch_$ip")
        _state.update { it.copy(watchingIp = null, watchingName = null) }
    }

    fun clearAlert() = _state.update { it.copy(slotAlert = null) }

    private fun scheduleWatch(ip: String, serverName: String) {
        val inputData = Data.Builder()
            .putString(SlotWorker.KEY_IP, ip)
            .putString(SlotWorker.KEY_SERVER_NAME, serverName)
            .build()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val workRequest = OneTimeWorkRequestBuilder<SlotWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
            .addTag(SlotWorker.WORK_TAG)
            .build()
        workManager.enqueueUniqueWork("watch_$ip", ExistingWorkPolicy.REPLACE, workRequest)
    }
    
     fun discoverServers() {
    viewModelScope.launch(Dispatchers.IO) {
        _state.update { it.copy(isLoading = true) }
        ServerDiscovery.discoverAndSave(getApplication())
        refresh()
    }
     }
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

    private fun loadAutoWatch(): List<String> {
        val json = prefs.getString("auto_watch", null) ?: return emptyList()
        return gson.fromJson(json, object : TypeToken<List<String>>() {}.type)
    }
    private fun saveAutoWatch(list: List<String>) =
        prefs.edit().putString("auto_watch", gson.toJson(list)).apply()

    private fun loadAllHistories(): Map<String, ServerHistory> {
        val json = prefs.getString("histories", null) ?: return emptyMap()
        return try {
            gson.fromJson(json, object : TypeToken<Map<String, ServerHistory>>() {}.type)
        } catch (e: Exception) { emptyMap() }
    }
    private fun saveAllHistories(map: Map<String, ServerHistory>) =
        prefs.edit().putString("histories", gson.toJson(map)).apply()
}
