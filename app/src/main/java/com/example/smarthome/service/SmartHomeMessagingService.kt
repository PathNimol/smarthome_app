package com.example.smarthome.service

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

/**
 * Handles FCM messages in ALL states:
 *  - App in foreground  → shows heads-up banner via notifyForeground()
 *  - App in background  → FCM shows the notification automatically from the "notification" payload
 *  - App killed/closed  → same as background; system tray shows it, opens MainActivity on tap
 */
class SmartHomeMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID   = "smarthome_alerts"
        const val CHANNEL_NAME = "Smart Home Alerts"

        /** Call once in Application.onCreate() */
        fun createChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description   = "Smart home sensor and device alerts"
                enableLights(true)
                enableVibration(true)
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    // ── Called when a new FCM token is generated ──────────────────────────
    // Save this token to Firestore so your backend can target this device.
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // TODO: save to Firestore, e.g.:
        // FirebaseFirestore.getInstance()
        //     .collection("fcmTokens")
        //     .document(FirebaseAuth.getInstance().currentUser?.uid ?: return)
        //     .set(mapOf("token" to token))
    }

    // ── Called when message arrives while app is in FOREGROUND ───────────
    // Background/killed: FCM SDK handles the notification automatically —
    // this method is NOT called in those states.
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "Smart Home Alert"

        val body  = message.notification?.body
            ?: message.data["body"]
            ?: return          // nothing to show

        // Deep-link: pass "route" from data payload so MainActivity opens the right screen
        val route = message.data["route"] ?: "alerts"

        showHeadsUpNotification(title, body, route, message.messageId.hashCode())
    }

    // ── Build & post the in-app heads-up banner ───────────────────────────
    private fun showHeadsUpNotification(title: String, body: String, route: String, id: Int) {
        // Tapping the notification opens MainActivity and passes the target route
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", route)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // replace with R.drawable.ic_notification
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