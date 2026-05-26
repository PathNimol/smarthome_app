package com.example.smarthome.data.model

import com.google.firebase.Timestamp

data class AppNotification(
    val id: String = "",
    val type: NotificationType = NotificationType.DEVICE_ON,
    val title: String = "",
    val message: String = "",
    val timestamp: Timestamp? = null,
    val isRead: Boolean = false,
    val userId: String = "",
    val metadata: Map<String, Any> = emptyMap()
) {
    enum class NotificationType {
        RAIN_ALERT, LOW_WATER, OVERFLOW, DEVICE_ON, DEVICE_OFF;

        companion object {
            fun from(s: String) = entries.firstOrNull { it.name.equals(s, true) } ?: DEVICE_ON
        }
    }
}
