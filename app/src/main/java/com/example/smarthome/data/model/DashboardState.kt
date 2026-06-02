package com.example.smarthome.data.model

/**
 * UI state for DashboardViewModel.
 * Separate from SmartHomeState so each screen owns its own state shape.
 *
 * Firebase paths:
 *   fan / ledLight        → smarthome/devices/fan & led_light
 *   waterLevel            → smarthome/sensors/water_level
 *   rain                  → smarthome/sensors/rain
 *   recentNotifications   → smarthome/notifications (or Firestore)
 */
data class DashboardState(
    val fan: Device = Device(),
    val ledLight: Device = Device(),
    val waterLevel: WaterLevel = WaterLevel(),
    val rain: RainStatus = RainStatus(),
    val recentNotifications: List<AppNotification> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)