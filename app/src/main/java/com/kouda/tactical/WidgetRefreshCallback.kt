package com.kouda.tactical

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import com.google.gson.Gson
import com.kouda.tactical.network.SourceQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

class WidgetRefreshCallback : ActionCallback {

    override suspend fun onAction(context: Context, glanceId: GlanceId) {
        refreshWidgetData(context)
        KoudaWidget().update(context, glanceId)
    }

    companion object {
        suspend fun refreshWidgetData(context: Context) = withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences("kouda_prefs", Context.MODE_PRIVATE)
            val gson = Gson()

            // Leer todos los servidores guardados
            val serversJson = prefs.getString("servers", null)
            val servers: List<String> = if (serversJson != null) {
                gson.fromJson(serversJson, object : com.google.gson.reflect.TypeToken<List<String>>() {}.type)
            } else listOf("45.235.98.50:27015")

            // Consultar todos en paralelo
            val results = servers.map { addr ->
                async {
                    val (info, _) = SourceQuery.queryServer(addr)
                    info?.let {
                        WidgetServerData(
                            ip = addr,
                            name = it.name,
                            curPlayers = it.curPlayers,
                            maxPlayers = it.maxPlayers,
                            ping = it.ping,
                            country = it.country,
                            map = it.map
                        )
                    }
                }
            }.awaitAll().filterNotNull()

            // Guardar cache para el widget
            prefs.edit()
                .putString("widget_cache", gson.toJson(results))
                .apply()
        }
    }
}
