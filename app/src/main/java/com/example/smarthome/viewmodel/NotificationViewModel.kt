package com.example.smarthome.viewmodel

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarthome.MainActivity
import com.example.smarthome.data.model.AppNotification
import com.example.smarthome.data.model.AppNotification.NotificationType
import com.example.smarthome.data.repository.SmartHomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val CHANNEL_ID = "smarthome_alerts"

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository : SmartHomeRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    // IDs we already posted so we never fire the same alert twice
    private val postedIds = mutableSetOf<String>()

    init {
        observeNotifications()
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            // observeNotifications() defaults limit=3; pass a higher number to show more
            repository.observeNotifications(limit = 50).collect { list: List<AppNotification> ->
                _uiState.update { state ->
                    state.copy(
                        notifications = list,
                        unreadCount   = list.count { it.isRead.not() },
                        isLoading     = false
                    )
                }

                // Fire a phone heads-up for every new unread item — runs on ALL screens
                // because the ViewModel is alive for the whole app session
                list.filter { it.isRead.not() && it.id !in postedIds }
                    .forEach { notif: AppNotification ->
                        postedIds += notif.id
                        postPhoneNotification(notif)
                    }
            }
        }
    }

    fun markAllRead() {
        viewModelScope.launch { repository.markAllRead() }
    }

    fun markRead(id: String) {
        viewModelScope.launch { repository.markRead(id) }
    }

    // ── Posts a heads-up banner to the phone's notification drawer ────────
    private fun postPhoneNotification(notif: AppNotification) {
        val title = when (notif.type) {
            NotificationType.RAIN_ALERT -> "🌧 Rain Alert"
            NotificationType.LOW_WATER  -> "💧 Low Water Level"
            NotificationType.OVERFLOW   -> "⚠️ Overflow Detected"
            NotificationType.DEVICE_ON  -> "✅ Device Turned On"
            NotificationType.DEVICE_OFF -> "🔌 Device Turned Off"
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "alerts")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notif.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(notif.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notif.message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(notif.id.hashCode(), notification)
    }
}