package com.example.smarthome.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarthome.data.model.SmartHomeState
import com.example.smarthome.data.repository.SmartHomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceControlViewModel @Inject constructor(
    private val repository: SmartHomeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SmartHomeState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeAll().collect { newState ->
                _state.value = newState
            }
        }
    }

    // ── Device controls ───────────────────────────────────────────────────

    fun toggleFan(isOn: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(error = null) }
            try {
                repository.toggleDevice("fan", isOn)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun toggleLight(isOn: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(error = null) }
            try {
                repository.toggleDevice("led_light", isOn)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    // ── Simulation controls ───────────────────────────────────────────────

    fun simulateWaterLevel(pct: Int) {
        viewModelScope.launch {
            try {
                repository.simulateWaterLevel(pct)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun simulateRain(isRaining: Boolean, intensity: String = "moderate") {
        viewModelScope.launch {
            try {
                repository.simulateRain(isRaining, intensity)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    // ── Error handling ────────────────────────────────────────────────────

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}