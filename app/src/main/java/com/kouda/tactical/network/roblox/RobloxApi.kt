package com.kouda.tactical.network.roblox

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Cliente para las APIs públicas de Roblox.
 *
 * Endpoints utilizados (todos públicos, sin autenticación):
 * - games.roblox.com/v1/games  → info de universos
 * - games.roblox.com/v1/games/list → top games por categoría
 * - thumbnails.roblox.com       → imágenes de juegos
 * - apis.roblox.com/universes/  → búsqueda por nombre (vía catalog)
 */
object RobloxApi {

    private const val TAG = "RobloxApi"

    private val http = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .build()

    // ─── TOP GAMES POR CATEGORÍA ─────────────────────────────────────────────

    fun getTopGames(category: RobloxCategory, limit: Int = 20): List<RobloxGame> {
        return try {
            val url = "https://games.roblox.com/v1/games/list" +
                "?sortToken=&gameSetType=${category.sortType}" +
                "&startRows=0&maxRows=$limit"

            val body = get(url) ?: return emptyList()
            val json = JSONObject(body)
            val games = json.optJSONArray("games") ?: return emptyList()

            val universeIds = (0 until games.length()).map {
                games.getJSONObject(it).getLong("universeId")
            }

            fetchGameDetails(universeIds)
        } catch (e: Exception) {
            Log.e(TAG, "getTopGames error: ${e.message}")
            emptyList()
        }
    }

    // ─── BÚSQUEDA POR NOMBRE ─────────────────────────────────────────────────

    fun searchGames(query: String, limit: Int = 20): List<RobloxSearchResult> {
        return try {
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "https://catalog.roblox.com/v1/search/items" +
                "?category=Games&keyword=$encoded&limit=$limit"

            val body = get(url) ?: return emptyList()
            val json = JSONObject(body)
            val data = json.optJSONArray("data") ?: return emptyList()

            // Endpoint de catalog da placeIds, necesitamos universeIds
            val placeIds = (0 until data.length()).mapNotNull {
                data.getJSONObject(it).optLong("id").takeIf { id -> id > 0 }
            }

            if (placeIds.isEmpty()) return emptyList()

            // Convertir placeIds a universeIds
            val universeIds = placeIdsToUniverseIds(placeIds)
            val details = fetchGameDetails(universeIds)
            val thumbnails = fetchThumbnails(universeIds)

            details.map { game ->
                RobloxSearchResult(
                    universeId = game.universeId,
                    placeId = game.placeId,
                    name = game.name,
                    thumbnailUrl = thumbnails[game.universeId],
                    activePlayers = game.activePlayers,
                    totalVisits = game.totalVisits
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "searchGames error: ${e.message}")
            emptyList()
        }
    }

    // ─── DETALLE DE UN JUEGO ─────────────────────────────────────────────────

    fun getGameDetail(universeId: Long): RobloxGame? {
        return fetchGameDetails(listOf(universeId)).firstOrNull()
    }

    // ─── PRIVADOS ────────────────────────────────────────────────────────────

    private fun fetchGameDetails(universeIds: List<Long>): List<RobloxGame> {
        if (universeIds.isEmpty()) return emptyList()
        return try {
            val ids = universeIds.joinToString(",")
            val body = get("https://games.roblox.com/v1/games?universeIds=$ids")
                ?: return emptyList()
            val json = JSONObject(body)
            val data = json.optJSONArray("data") ?: return emptyList()

            val thumbnails = fetchThumbnails(universeIds)

            (0 until data.length()).mapNotNull { i ->
                try {
                    parseGame(data.getJSONObject(i), thumbnails)
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchGameDetails error: ${e.message}")
            emptyList()
        }
    }

    private fun parseGame(obj: JSONObject, thumbnails: Map<Long, String?>): RobloxGame {
        val universeId = obj.getLong("id")
        val creator = obj.optJSONObject("creator")
        return RobloxGame(
            universeId = universeId,
            placeId = obj.optLong("rootPlaceId", 0L),
            name = obj.optString("name", "Unknown"),
            description = obj.optString("description", "").take(300),
            creator = creator?.optString("name", "—") ?: "—",
            creatorType = creator?.optString("type", "User") ?: "User",
            activePlayers = obj.optInt("playing", 0),
            totalVisits = obj.optLong("visits", 0L),
            favoritedCount = obj.optLong("favoritedCount", 0L),
            maxPlayers = obj.optInt("maxPlayers", 0),
            genre = obj.optString("genre", "—"),
            thumbnailUrl = thumbnails[universeId],
            isPlayable = obj.optBoolean("isPlayable", false),
            created = obj.optString("created", "").take(10),
            updated = obj.optString("updated", "").take(10)
        )
    }

    private fun fetchThumbnails(universeIds: List<Long>): Map<Long, String?> {
        if (universeIds.isEmpty()) return emptyMap()
        return try {
            val ids = universeIds.joinToString(",")
            val url = "https://thumbnails.roblox.com/v1/games/multiget/thumbnails" +
                "?universeIds=$ids&thumbnailType=GameThumbnail&format=Png&size=768x432"
            val body = get(url) ?: return emptyMap()
            val data = JSONObject(body).optJSONArray("data") ?: return emptyMap()
            val result = mutableMapOf<Long, String?>()
            for (i in 0 until data.length()) {
                val item = data.getJSONObject(i)
                val uid = item.getLong("universeId")
                val thumbs = item.optJSONArray("thumbnails")
                val imageUrl = thumbs?.optJSONObject(0)?.optString("imageUrl")
                result[uid] = imageUrl
            }
            result
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun placeIdsToUniverseIds(placeIds: List<Long>): List<Long> {
        return try {
            val ids = placeIds.joinToString(",")
            val body = get("https://apis.roblox.com/universes/v1/places?placeIds=$ids")
                ?: return emptyList()
            val data = JSONObject(body).optJSONArray("universeIdsByPlaceIds")
                ?: return emptyList()
            (0 until data.length()).mapNotNull {
                data.optJSONObject(it)?.optLong("universeId")?.takeIf { id -> id > 0 }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun get(url: String): String? {
        return try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .header("Accept", "application/json")
                .build()
            val resp = http.newCall(req).execute()
            if (resp.isSuccessful) resp.body?.string() else null
        } catch (e: Exception) {
            Log.w(TAG, "GET $url → ${e.message}")
            null
        }
    }
}
