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
    val category: RobloxCategory = RobloxCategory.POPULAR,
    val games: List<RobloxSearchResult> = emptyList(),
    val isLoadingGames: Boolean = false,
    val exploreFailed: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<RobloxSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val selectedGame: RobloxGame? = null,
    val addError: String? = null
)

class RobloxViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(RobloxUiState())
    val state: StateFlow<RobloxUiState> = _state.asStateFlow()

    init { loadCategory(RobloxCategory.POPULAR) }

    fun loadCategory(category: RobloxCategory) {
        _state.update { it.copy(category = category, games = emptyList(), isLoadingGames = true, exploreFailed = false) }
        viewModelScope.launch(Dispatchers.IO) {
            val results = RobloxApi.searchGames(category.query)
            _state.update { it.copy(games = results, isLoadingGames = false, exploreFailed = results.isEmpty()) }
        }
    }

    fun search(query: String) {
        _state.update { it.copy(searchQuery = query) }
        if (query.length < 3) {
            _state.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            delay(500)
            if (_state.value.searchQuery != query) return@launch
            _state.update { it.copy(isSearching = true) }
            val results = RobloxApi.searchGames(query)
            _state.update { it.copy(searchResults = results, isSearching = false) }
        }
    }

    fun clearSearch() = _state.update { it.copy(searchQuery = "", searchResults = emptyList()) }

    fun selectGame(game: RobloxGame?) = _state.update { it.copy(selectedGame = game) }

    fun addByInput(input: String) {
        if (input.isBlank()) return
        _state.update { it.copy(addError = null) }
        viewModelScope.launch(Dispatchers.IO) {
            val game = RobloxApi.resolveInput(input)
            if (game == null) {
                _state.update { it.copy(addError = "No se encontró ningún juego con ese dato") }
            } else {
                _state.update { it.copy(selectedGame = game, addError = null) }
            }
        }
    }

    fun clearAddError() = _state.update { it.copy(addError = null) }
}
