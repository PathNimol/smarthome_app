// viewmodel/AuthViewModel.kt
package com.smarthome.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarthome.data.repository.SmartHomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: SmartHomeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState = _uiState.asStateFlow()

    val isLoggedIn = repository.isLoggedIn()

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Please fill in all fields")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            repository.signIn(email, password)
                .onSuccess { _uiState.value = AuthUiState.Success }
                .onFailure { _uiState.value = AuthUiState.Error(it.message ?: "Login failed") }
        }
    }

    fun signOut() = repository.signOut()
}

// ─────────────────────────────────────────────────────────────────────────────

// viewmodel/DashboardViewModel.kt
package com.smarthome.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarthome.data.model.DashboardState
import com.smarthome.data.repository.SmartHomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: SmartHomeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state = _state.asStateFlow()

    init {
        observeAll()
    }

    private fun observeAll() {
        // Combine all real-time streams into a single dashboard state
        viewModelScope.launch {
            combine(
                repository.observeDevice("fan"),
                repository.observeDevice("led_light"),
                repository.observeWaterLevel(),
                repository.observeRain(),
                repository.observeNotifications(limit = 3)
            ) { fan, led, water, rain, notifs ->
                DashboardState(
                    fan = fan,
                    ledLight = led,
                    waterLevel = water,
                    rain = rain,
                    recentNotifications = notifs,
                    isLoading = false
                )
            }.catch { e ->
                // Emit current state without loading — log error in prod
                _state.update { it.copy(isLoading = false) }
            }.collect { dashboardState ->
                _state.value = dashboardState
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

// viewmodel/DeviceControlViewModel.kt
package com.smarthome.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarthome.data.model.Device
import com.smarthome.data.repository.SmartHomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceControlState(
    val fan: Device = Device(id = "fan"),
    val ledLight: Device = Device(id = "led_light"),
    val isUpdating: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DeviceControlViewModel @Inject constructor(
    private val repository: SmartHomeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DeviceControlState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeDevice("fan"),
                repository.observeDevice("led_light")
            ) { fan, led -> DeviceControlState(fan = fan, ledLight = led) }
                .collect { _state.value = it }
        }
    }

    fun toggleFan(newState: Boolean) = toggleDevice("fan", newState)
    fun toggleLight(newState: Boolean) = toggleDevice("led_light", newState)

    private fun toggleDevice(id: String, newState: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isUpdating = true, error = null) }
            repository.toggleDevice(id, newState)
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
            _state.update { it.copy(isUpdating = false) }
        }
    }

    // Simulation controls (remove after ESP32 is connected)
    fun simulateWaterLevel(pct: Int) = viewModelScope.launch {
        repository.simulateWaterLevel(pct)
    }

    fun simulateRain(isRaining: Boolean, intensity: String = "moderate") = viewModelScope.launch {
        repository.simulateRain(isRaining, intensity)
    }
}

// ─────────────────────────────────────────────────────────────────────────────

// viewmodel/NotificationViewModel.kt
package com.smarthome.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarthome.data.model.AppNotification
import com.smarthome.data.repository.SmartHomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationUiState(
    val notifications: List<AppNotification> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = true
)

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
