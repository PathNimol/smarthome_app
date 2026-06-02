package com.example.smarthome.data.model

data class Device(
    val id: String = "",
    val state: String = "OFF",
    val controlledBy: String = "manual",
    val lastChanged: Long = 0L,
    val powerWatts: Int = 0
) {
    val isOn: Boolean get() = state == "ON"
}