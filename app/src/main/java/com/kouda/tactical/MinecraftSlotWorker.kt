package com.kouda.tactical

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kouda.tactical.network.minecraft.MinecraftQuery

class MinecraftSlotWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_IP   = "mc_ip"
        const val KEY_PORT = "mc_port"
        const val KEY_NAME = "mc_name"
        const val WORK_TAG = "mc_slot_watcher"
    }

    override suspend fun doWork(): Result {
        val ip   = inputData.getString(KEY_IP)   ?: return Result.failure()
        val port = inputData.getInt(KEY_PORT, 25565)
        val name = inputData.getString(KEY_NAME) ?: "$ip:$port"

        val info = MinecraftQuery.query(ip, port)

        return if (info.isOnline && !info.isFull) {
            notify(name, info.curPlayers, info.maxPlayers, ip, port)
            Result.success()
        } else {
            Result.retry()
        }
    }

    private fun notify(name: String, cur: Int, max: Int, ip: String, port: Int) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(
            NotificationChannel(
                SlotWorker.CHANNEL_ID,
                "Kouda Slot Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { enableVibration(true) }
        )

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_minecraft", "$ip:$port")
        }
        val pi = PendingIntent.getActivity(
            context, port,
            intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        nm.notify(
            ("mc_$ip:$port").hashCode(),
            NotificationCompat.Builder(context, SlotWorker.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Hay lugar en $name")
                .setContentText("$cur/$max jugadores — conectate ahora")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("Slot disponible en $name\n$cur/$max jugadores\n$ip:$port"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setColor(0x4CAF50)
                .build()
        )
    }
}
