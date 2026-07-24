package com.kouda.tactical.ui.roblox

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kouda.tactical.network.roblox.RobloxApi
import com.kouda.tactical.network.roblox.RobloxCategory
import com.kouda.tactical.network.roblox.RobloxGame
import com.kouda.tactical.network.roblox.RobloxSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RobloxUiState(
    val category: RobloxCategory = RobloxCategory.TOP_PLAYED,
    val games: List<RobloxGame> = emptyList(),
    val isLoadingGames: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<RobloxSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val selectedGame: RobloxGame? = null
)

class RobloxViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(RobloxUiState())
    val state: StateFlow<RobloxUiState> = _state.asStateFlow()

    init { loadCategory(RobloxCategory.TOP_PLAYED) }

    fun loadCategory(category: RobloxCategory) {
        _state.update { it.copy(category = category, games = emptyList(), isLoadingGames = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val games = RobloxApi.getTopGames(category)
            _state.update { it.copy(games = games, isLoadingGames = false) }
        }
    }

    fun search(query: String) {
        _state.update { it.copy(searchQuery = query) }
        if (query.length < 3) {
            _state.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            delay(500) // debounce
            if (_state.value.searchQuery != query) return@launch // cancelado
            _state.update { it.copy(isSearching = true) }
            val results = RobloxApi.searchGames(query)
            _state.update { it.copy(searchResults = results, isSearching = false) }
        }
    }

    fun clearSearch() = _state.update { it.copy(searchQuery = "", searchResults = emptyList()) }

    fun selectGame(game: RobloxGame?) = _state.update { it.copy(selectedGame = game) }

    fun loadGameDetail(universeId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val detail = RobloxApi.getGameDetail(universeId) ?: return@launch
            _state.update { it.copy(selectedGame = detail) }
        }
    }
}
