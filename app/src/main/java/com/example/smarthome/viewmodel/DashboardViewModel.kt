package com.example.smarthome.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarthome.data.model.DashboardState
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
            }.catch {
                _state.update { it.copy(isLoading = false) }
            }.collect { dashboardState ->
                _state.value = dashboardState
            }
        }
    }
}