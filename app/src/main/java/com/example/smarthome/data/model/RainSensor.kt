package com.example.smarthome.data.model

data class RainSensor(
    val isRaining: Boolean = false,
    val active: Boolean = false,
    val intensity: String = "",
    val detectedAt: Long = 0L
)