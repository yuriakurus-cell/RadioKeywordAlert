package com.example.radiokeywordalert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {
    const val CHANNEL_ID_ALERTS = "alerts"
    const val CHANNEL_ID_FOREGROUND = "listening"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alerts = NotificationChannel(CHANNEL_ID_ALERTS, "Keyword Alerts",
                NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Повідомлення при виявленні ключових слів"
            }
            val fg = NotificationChannel(CHANNEL_ID_FOREGROUND, "Listening",
                NotificationManager.IMPORTANCE_MIN).apply {
                description = "Фонова служба прослуховування"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(alerts)
            nm.createNotificationChannel(fg)
        }
    }

    fun buildForeground(context: Context): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ID_FOREGROUND)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Прослуховування ефіру")
            .setContentText("Сервіс активний — розпізнавання мовлення")
            .setOngoing(true)
            .setSilent(true)
    }

    fun notifyAlert(context: Context, title: String, text: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 400, 200, 400))

        with(NotificationManagerCompat.from(context)) {
            notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), builder.build())
        }
    }
}
