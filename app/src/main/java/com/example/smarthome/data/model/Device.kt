package com.example.smarthome.data.model

data class Device(
    val id: String = "",
    val state: String = "OFF",          // "ON" | "OFF"
    val lastChanged: Long = 0L,
    val controlledBy: String = "manual", // "manual" | "auto"
    val powerWatts: Int = 0
) {
    val isOn: Boolean get() = state == "ON"
}