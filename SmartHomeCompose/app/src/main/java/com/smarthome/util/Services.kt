// util/SmartHomeFcmService.kt
package com.smarthome.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.smarthome.R
import com.smarthome.ui.dashboard.MainActivity

/**
 * Receives FCM push notifications from Firebase Cloud Functions.
 * Cloud Functions detect threshold crossings in Realtime DB and
 * call the FCM Admin SDK → this service delivers the push.
 */
class SmartHomeFcmService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "SmartHome Alert"
        val body = message.notification?.body ?: message.data["message"] ?: ""
        val type = message.data["type"] ?: "general"

        showNotification(title, body, type)
    }

    override fun onNewToken(token: String) {
        // TODO: Send token to your backend / save to Firestore user document
        // so Cloud Functions can target this device
        // repository.updateFcmToken(token)
    }

    private fun showNotification(title: String, body: String, type: String) {
        val channelId = when (type) {
            "rain_alert" -> CHANNEL_RAIN
            "low_water", "overflow" -> CHANNEL_WATER
            else -> CHANNEL_DEVICE
        }

        createChannels()

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("navigate_to", "notifications")
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        listOf(
            NotificationChannel(CHANNEL_RAIN, "Rain Alerts", NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel(CHANNEL_WATER, "Water Level Alerts", NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel(CHANNEL_DEVICE, "Device Status", NotificationManager.IMPORTANCE_DEFAULT)
        ).forEach { manager.createNotificationChannel(it) }
    }

    companion object {
        const val CHANNEL_RAIN = "rain_alerts"
        const val CHANNEL_WATER = "water_alerts"
        const val CHANNEL_DEVICE = "device_status"
    }
}

// ─────────────────────────────────────────────────────────────────────────────

// util/di/AppModule.kt — Hilt Dependency Injection
package com.smarthome.util.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideRealtimeDatabase(): FirebaseDatabase =
        FirebaseDatabase.getInstance().also {
            it.setPersistenceEnabled(true) // Offline support
        }

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
}

// ─────────────────────────────────────────────────────────────────────────────

// SmartHomeApp.kt
package com.smarthome

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SmartHomeApp : Application()
