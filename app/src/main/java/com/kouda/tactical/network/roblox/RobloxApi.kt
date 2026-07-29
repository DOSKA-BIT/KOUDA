package com.kouda.tactical.network.roblox

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * games.roblox.com/v1/games/list (el que devuelve "top jugados" ordenado por
 * el algoritmo interno de Roblox) dejó de responder sin sesión autenticada,
 * así que no lo usamos más. Todo acá pasa por endpoints que siguen siendo
 * públicos hoy: omni-search para descubrir juegos, y games/thumbnails/universes
 * para los datos concretos de cada uno.
 */
object RobloxApi {

    private const val TAG = "RobloxApi"

    private val http = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    fun searchGames(query: String, limit: Int = 24): List<RobloxSearchResult> {
        if (query.isBlank()) return emptyList()
        return try {
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "https://apis.roblox.com/search-api/omni-search" +
                "?searchQuery=$encoded&pageType=all"

            val body = get(url) ?: return emptyList()
            val root = JSONObject(body)
            val searches = root.optJSONArray("searchResults") ?: return emptyList()

            val universeIds = mutableListOf<Long>()
            for (i in 0 until searches.length()) {
                val group = searches.getJSONObject(i)
                val contents = group.optJSONArray("contents") ?: continue
                for (j in 0 until contents.length()) {
                    val item = contents.getJSONObject(j)
                    val uid = item.optLong("universeId", -1)
                    if (uid > 0) universeIds.add(uid)
                    if (universeIds.size >= limit) break
                }
                if (universeIds.size >= limit) break
            }

            if (universeIds.isEmpty()) return emptyList()

            val details = fetchGameDetails(universeIds)
            details.map {
                RobloxSearchResult(it.universeId, it.placeId, it.name, it.thumbnailUrl, it.activePlayers, it.totalVisits)
            }
        } catch (e: Exception) {
            Log.e(TAG, "searchGames($query): ${e.message}")
            emptyList()
        }
    }

    fun getGameDetail(universeId: Long): RobloxGame? =
        fetchGameDetails(listOf(universeId)).firstOrNull()

    /** Acepta URL completa, placeId numérico o universeId numérico. */
    fun resolveInput(input: String): RobloxGame? {
        val trimmed = input.trim()

        val placeIdFromUrl = Regex("roblox\\.com/games/(\\d+)").find(trimmed)?.groupValues?.get(1)?.toLongOrNull()
        if (placeIdFromUrl != null) return placeIdToGame(placeIdFromUrl)

        val asNumber = trimmed.toLongOrNull()
        if (asNumber != null) {
            // probamos como universeId primero, si no da nada lo tratamos como placeId
            getGameDetail(asNumber)?.let { return it }
            return placeIdToGame(asNumber)
        }
        return null
    }

    private fun placeIdToGame(placeId: Long): RobloxGame? {
        val universeId = placeIdsToUniverseIds(listOf(placeId)).firstOrNull() ?: return null
        return getGameDetail(universeId)
    }

    private fun fetchGameDetails(universeIds: List<Long>): List<RobloxGame> {
        if (universeIds.isEmpty()) return emptyList()
        return try {
            val ids = universeIds.joinToString(",")
            val body = get("https://games.roblox.com/v1/games?universeIds=$ids") ?: return emptyList()
            val data = JSONObject(body).optJSONArray("data") ?: return emptyList()
            val thumbnails = fetchThumbnails(universeIds)

            buildList {
                for (i in 0 until data.length()) {
                    runCatching { parseGame(data.getJSONObject(i), thumbnails) }.getOrNull()?.let { add(it) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchGameDetails: ${e.message}")
            emptyList()
        }
    }

    private fun parseGame(obj: JSONObject, thumbnails: Map<Long, String?>): RobloxGame {
        val universeId = obj.getLong("id")
        val creator = obj.optJSONObject("creator")
        return RobloxGame(
            universeId = universeId,
            placeId = obj.optLong("rootPlaceId", 0L),
            name = obj.optString("name", "Sin nombre"),
            description = obj.optString("description", "").take(300),
            creator = creator?.optString("name", "—") ?: "—",
            creatorType = creator?.optString("type", "User") ?: "User",
            activePlayers = obj.optInt("playing", 0),
            totalVisits = obj.optLong("visits", 0L),
            favoritedCount = obj.optLong("favoritedCount", 0L),
            maxPlayers = obj.optInt("maxPlayers", 0),
            genre = obj.optString("genre", "—"),
            thumbnailUrl = thumbnails[universeId],
            isPlayable = obj.optBoolean("isPlayable", true),
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
            buildMap {
                for (i in 0 until data.length()) {
                    val item = data.getJSONObject(i)
                    val uid = item.getLong("universeId")
                    val imageUrl = item.optJSONArray("thumbnails")?.optJSONObject(0)?.optString("imageUrl")
                    put(uid, imageUrl)
                }
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun placeIdsToUniverseIds(placeIds: List<Long>): List<Long> {
        return try {
            val ids = placeIds.joinToString(",")
            val body = get("https://apis.roblox.com/universes/v1/places?placeIds=$ids") ?: return emptyList()
            val data = JSONObject(body).optJSONArray("universeIdsByPlaceIds") ?: return emptyList()
            buildList {
                for (i in 0 until data.length()) {
                    data.optJSONObject(i)?.optLong("universeId")?.takeIf { it > 0 }?.let { add(it) }
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun get(url: String): String? {
        return try {
            val req = Request.Builder().url(url)
                .header("User-Agent", "Mozilla/5.0")
                .header("Accept", "application/json")
                .build()
            val resp = http.newCall(req).execute()
            if (resp.isSuccessful) resp.body?.string() else {
                Log.w(TAG, "GET $url → ${resp.code}")
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "GET $url falló: ${e.message}")
            null
        }
    }
}
