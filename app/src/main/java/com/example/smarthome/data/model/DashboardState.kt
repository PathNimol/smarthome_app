package com.example.smarthome.data.model

data class DashboardState(
    val fan: Device = Device(id = "fan"),
    val ledLight: Device = Device(id = "led_light"),
    val waterLevel: WaterLevel = WaterLevel(),
    val rain: RainStatus = RainStatus(),
    val recentNotifications: List<AppNotification> = emptyList(),
    val isLoading: Boolean = true
)
