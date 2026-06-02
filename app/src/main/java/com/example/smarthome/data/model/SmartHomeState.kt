package com.example.smarthome.data.model

/**
 * Combined UI state for all ViewModels.
 * All fields map to Firebase paths under "smarthome/".
 *
 * - fan / ledLight   → smarthome/devices/fan & led_light
 * - rain             → smarthome/sensors/rain        (RainStatus — used by Dashboard)
 * - waterLevel       → smarthome/sensors/water_level (WaterLevel with WaterStatus enum)
 * - alert            → smarthome/alerts
 * - rainAnalog       → smarthome/sensors/rain_analog
 * - lastSync         → smarthome/system/lastSync
 */
data class SmartHomeState(
    val fan: Device = Device(),
    val ledLight: Device = Device(),
    val rain: RainStatus = RainStatus(),          // DashboardScreen uses RainStatus
    val waterLevel: WaterLevel = WaterLevel(),    // DashboardScreen uses WaterLevel.WaterStatus
    val alert: Alert = Alert(),
    val rainAnalog: Int = 0,
    val lastSync: Long = 0L,
    val isLoading: Boolean = true,
    val error: String? = null,
    val recentNotifications: List<AppNotification> = emptyList()
)