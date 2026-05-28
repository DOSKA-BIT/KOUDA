package com.kouda.tactical

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class KoudaWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("kouda_prefs", Context.MODE_PRIVATE)
        val gson = com.google.gson.Gson()

        // Cargar favoritos
        val favsJson = prefs.getString("favs", null)
        val favIps: List<String> = if (favsJson != null) {
            gson.fromJson(favsJson, object : com.google.gson.reflect.TypeToken<List<String>>() {}.type)
        } else emptyList()

        // Cargar servers guardados como cache del widget
        val cacheJson = prefs.getString("widget_cache", null)
        val cachedServers: List<WidgetServerData> = if (cacheJson != null) {
            try {
                gson.fromJson(cacheJson, object : com.google.gson.reflect.TypeToken<List<WidgetServerData>>() {}.type)
            } catch (e: Exception) { emptyList() }
        } else emptyList()

        // Mostrar solo favoritos (max 4 para no saturar el widget)
        val displayServers = cachedServers.filter { it.ip in favIps }.take(4)

        provideContent {
            WidgetContent(servers = displayServers)
        }
    }
}

data class WidgetServerData(
    val ip: String = "",
    val name: String = "",
    val curPlayers: Int = 0,
    val maxPlayers: Int = 0,
    val ping: Int = 0,
    val country: String = "??",
    val map: String = "-"
) {
    val players: String get() = "$curPlayers/$maxPlayers"
    val pingStr: String get() = "${ping}ms"
    val fillRatio: Float get() = if (maxPlayers > 0) curPlayers.toFloat() / maxPlayers else 0f
    val isFull: Boolean get() = maxPlayers > 0 && curPlayers >= maxPlayers
}

@Composable
fun WidgetContent(servers: List<WidgetServerData>) {
    val bgColor = ColorProvider(Color(0xFF0F0F0F))
    val orangeColor = ColorProvider(Color(0xFFFF6B00))
    val dimColor = ColorProvider(Color(0xFF606060))
    val whiteColor = ColorProvider(Color.White)
    val cardColor = ColorProvider(Color(0xFF1A1A1A))

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(12.dp)
    ) {
        // Header
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "KOUDA TACTICAL",
                style = TextStyle(
                    color = orangeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            Box(
                modifier = GlanceModifier
                    .clickable(actionRunCallback<WidgetRefreshCallback>())
                    .padding(4.dp)
            ) {
                Text(
                    text = "↻",
                    style = TextStyle(color = orangeColor, fontSize = 14.sp)
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        if (servers.isEmpty()) {
            Text(
                text = "Agrega favoritos en la app\npara verlos aqui",
                style = TextStyle(color = dimColor, fontSize = 11.sp),
                modifier = GlanceModifier.padding(vertical = 8.dp)
            )
        } else {
            servers.forEach { server ->
                WidgetServerRow(server = server)
                Spacer(modifier = GlanceModifier.height(6.dp))
            }
        }
    }
}

@Composable
fun WidgetServerRow(server: WidgetServerData) {
    val orangeColor = ColorProvider(Color(0xFFFF6B00))
    val dimColor = ColorProvider(Color(0xFF606060))
    val whiteColor = ColorProvider(Color.White)
    val cardColor = ColorProvider(Color(0xFF1A1A1A))
    val redColor = ColorProvider(Color(0xFFFF1744))
    val greenColor = ColorProvider(Color(0xFF00E676))

    val pingColor = when {
        server.ping < 60 -> ColorProvider(Color(0xFF00E676))
        server.ping < 120 -> ColorProvider(Color(0xFFFFD600))
        else -> ColorProvider(Color(0xFFFF1744))
    }

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(cardColor)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "[${server.country}] ${server.name}",
                style = TextStyle(
                    color = whiteColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight()
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
                text = server.players,
                style = TextStyle(
                    color = if (server.isFull) redColor else orangeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        Spacer(modifier = GlanceModifier.height(3.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = server.map,
                style = TextStyle(color = dimColor, fontSize = 10.sp),
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                text = server.pingStr,
                style = TextStyle(color = pingColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}

class KoudaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = KoudaWidget()
}
