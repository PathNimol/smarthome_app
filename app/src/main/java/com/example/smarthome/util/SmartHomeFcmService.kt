package com.example.smarthome.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.smarthome.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class SmartHomeFcmService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID   = "smarthome_alerts"
        private const val CHANNEL_NAME = "Smart Home Alerts"

        fun createChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Smart home sensor and device alerts"
                enableLights(true)
                enableVibration(true)
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    // Called when FCM gives this device a new token.
    // Save it to Firestore so your Cloud Function knows where to send alerts.
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // TODO:
        // val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        // FirebaseFirestore.getInstance()
        //     .collection("users").document(uid)
        //     .update("fcmToken", token)
    }

    // Called ONLY when the app is in the FOREGROUND.
    // Background/killed: FCM posts the notification automatically — this is never called.
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: message.data["title"] ?: "Smart Home Alert"
        val body  = message.notification?.body  ?: message.data["body"]  ?: return
        val route = message.data["route"] ?: "alerts"

        showNotification(title, body, route, message.messageId.hashCode())
    }

    private fun showNotification(title: String, body: String, route: String, id: Int) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", route)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // swap for R.drawable.ic_notification
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        getSystemService(NotificationManager::class.java).notify(id, notification)
    }
}