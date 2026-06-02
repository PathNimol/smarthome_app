package com.example.smarthome.data.model

/**
 * DashboardScreen imports RainStatus (not RainSensor).
 * This is the model used by DashboardScreen's RainStatusCard composable.
 *
 * Matches Firebase: smarthome/sensors/rain
 * {
 *   "active": false,
 *   "detectedAt": 1779793457405,
 *   "intensity": "moderate",
 *   "isRaining": false
 * }
 */
data class RainStatus(
    val isRaining: Boolean = false,
    val active: Boolean = false,
    val intensity: String = "",
    val detectedAt: Long = 0L
)