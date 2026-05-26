package com.example.smarthome.viewmodel

import com.example.smarthome.data.model.AppNotification

data class NotificationUiState(
    val notifications: List<AppNotification> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = true
)

