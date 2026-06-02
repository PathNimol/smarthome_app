package com.example.smarthome.data.model

/**
 * UI state for NotificationViewModel.
 */
data class NotificationUiState(
    val notifications: List<AppNotification> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
)