package com.example.smarthome.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarthome.data.repository.SmartHomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository: SmartHomeRepository
) : ViewModel() {

    val uiState: StateFlow<NotificationUiState> = repository
        .observeNotifications(limit = 50)
        .map { notifs ->
            NotificationUiState(
                notifications = notifs,
                unreadCount = notifs.count { !it.isRead },
                isLoading = false
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NotificationUiState())

    fun markAllRead() = viewModelScope.launch { repository.markAllRead() }
}