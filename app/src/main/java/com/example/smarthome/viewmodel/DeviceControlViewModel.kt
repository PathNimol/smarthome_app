package com.example.smarthome.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarthome.data.model.Device
import com.example.smarthome.data.repository.SmartHomeRepository
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
            ) { fan, led ->
                DeviceControlState(fan = fan, ledLight = led)
            }.collect { _state.value = it }
        }
    }

    fun toggleFan(newState: Boolean) = toggleDevice("fan", newState)
    fun toggleLight(newState: Boolean) = toggleDevice("led_light", newState)

    private fun toggleDevice(id: String, newState: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isUpdating = true, error = null) }
            try {
                repository.toggleDevice(id, newState)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
            _state.update { it.copy(isUpdating = false) }
        }
    }

    fun simulateWaterLevel(pct: Int) = viewModelScope.launch {
        repository.simulateWaterLevel(pct)
    }

    fun simulateRain(isRaining: Boolean, intensity: String = "moderate") = viewModelScope.launch {
        repository.simulateRain(isRaining, intensity)
    }
}