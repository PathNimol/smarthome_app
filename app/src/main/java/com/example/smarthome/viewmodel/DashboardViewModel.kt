package com.example.smarthome.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarthome.data.model.*
import com.example.smarthome.data.repository.SmartHomeRepository
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
        viewModelScope.launch {
            combine(
                repository.observeDevice("fan"),           // Flow<Device>
                repository.observeDevice("led_light"),     // Flow<Device>
                repository.observeWaterLevel(),            // Flow<WaterLevel>
                repository.observeRain(),                  // Flow<RainStatus>
                repository.observeNotifications(limit = 3) // Flow<List<AppNotification>>
            ) { fan: Device,
                led: Device,
                water: WaterLevel,
                rain: RainStatus,
                notifs: List<AppNotification> ->
                DashboardState(
                    fan                 = fan,
                    ledLight            = led,
                    waterLevel          = water,
                    rain                = rain,
                    recentNotifications = notifs,
                    isLoading           = false
                )
            }.catch { e ->
                _state.update { it.copy(isLoading = false, error = e.message) }
            }.collect { dashboardState ->
                _state.value = dashboardState
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}