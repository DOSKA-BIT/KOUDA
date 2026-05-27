package com.kouda.tactical

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kouda.tactical.network.SourceQuery

class SlotWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_IP = "server_ip"
        const val KEY_SERVER_NAME = "server_name"
        const val CHANNEL_ID = "kouda_slot_channel"
        const val WORK_TAG = "slot_watcher"
    }

    override suspend fun doWork(): Result {
        val ip = inputData.getString(KEY_IP) ?: return Result.failure()
        val serverName = inputData.getString(KEY_SERVER_NAME) ?: ip

        return try {
            val parts = ip.split(":")
            if (parts.size != 2) return Result.failure()

            val (info, _) = SourceQuery.queryServer(ip)

            if (info != null && info.curPlayers < info.maxPlayers) {
                sendNotification(serverName, info.curPlayers, info.maxPlayers, ip)
                Result.success()
            } else {
                // Servidor todavía lleno, reintentar
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun sendNotification(
        serverName: String,
        curPlayers: Int,
        maxPlayers: Int,
        ip: String
    ) {
        val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager

        // Crear canal (Android 8+)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Kouda Slot Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisos cuando se libera un slot en un servidor"
            enableVibration(true)
        }
        notifManager.createNotificationChannel(channel)

        // Intent para abrir la app al tocar la notificación
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_server", ip)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("SLOT LIBRE en $serverName")
            .setContentText("$curPlayers/$maxPlayers jugadores — conectate ahora")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Hay un lugar disponible en $serverName\n$curPlayers/$maxPlayers jugadores\nIP: $ip")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(0xFF6B00.toInt())
            .build()

        notifManager.notify(ip.hashCode(), notification)
    }
}
