package com.example.smarthome.data.model

import com.google.firebase.Timestamp

data class AppNotification(
    val id: String = "",
    val type: NotificationType = NotificationType.DEVICE_ON,
    val message: String = "",
    val isRead: Boolean = false,
    val timestamp: Timestamp? = null
) {
    enum class NotificationType {
        RAIN_ALERT,
        LOW_WATER,
        OVERFLOW,
        DEVICE_ON,
        DEVICE_OFF
    }
}