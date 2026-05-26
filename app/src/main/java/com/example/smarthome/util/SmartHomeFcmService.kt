package com.example.smarthome.util

//noinspection SuspiciousImport
import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.smarthome.MainActivity

class SmartHomeFcmService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "SmartHome Alert"
        val body  = message.notification?.body  ?: message.data["message"] ?: ""
        val type  = message.data["type"] ?: "general"
        showNotification(title, body, type)
    }

    override fun onNewToken(token: String) {
        // TODO: save token to Firestore so Cloud Functions can target this device
    }

    private fun showNotification(title: String, body: String, type: String) {
        val channelId = when {
            type.contains("rain")                         -> CHANNEL_RAIN
            type.contains("water") || type == "overflow"  -> CHANNEL_WATER
            else                                          -> CHANNEL_DEVICE
        }

        createChannels()

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_dialog_info)  // uses built-in Android icon
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createChannels() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        listOf(
            NotificationChannel(CHANNEL_RAIN,   "Rain Alerts",   NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel(CHANNEL_WATER,  "Water Alerts",  NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel(CHANNEL_DEVICE, "Device Status", NotificationManager.IMPORTANCE_DEFAULT)
        ).forEach { manager.createNotificationChannel(it) }
    }

    companion object {
        const val CHANNEL_RAIN   = "rain_alerts"
        const val CHANNEL_WATER  = "water_alerts"
        const val CHANNEL_DEVICE = "device_status"
    }
}