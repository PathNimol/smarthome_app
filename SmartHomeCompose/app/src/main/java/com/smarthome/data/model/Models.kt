// data/model/Models.kt
package com.smarthome.data.model

import com.google.firebase.Timestamp
import com.google.firebase.database.PropertyName

// ─── Device ───────────────────────────────────────────────────────────────────

data class Device(
    val id: String = "",
    val state: String = "OFF",          // "ON" | "OFF"
    val lastChanged: Long = 0L,
    val controlledBy: String = "manual", // "manual" | "auto"
    val powerWatts: Int = 0
) {
    val isOn: Boolean get() = state == "ON"
}

// ─── Sensors ──────────────────────────────────────────────────────────────────

data class WaterLevel(
    val percentage: Int = 0,
    val status: WaterStatus = WaterStatus.NORMAL,
    val lastUpdated: Long = 0L
) {
    enum class WaterStatus { NORMAL, LOW, OVERFLOW }

    companion object {
        fun fromPercentage(pct: Int, ts: Long) = WaterLevel(
            percentage = pct,
            status = when {
                pct < 20 -> WaterStatus.LOW
                pct >= 90 -> WaterStatus.OVERFLOW
                else -> WaterStatus.NORMAL
            },
            lastUpdated = ts
        )
    }
}

data class RainStatus(
    @get:PropertyName("isRaining") @set:PropertyName("isRaining")
    var isRaining: Boolean = false,
    val intensity: String = "none",     // "none" | "light" | "moderate" | "heavy"
    val detectedAt: Long = 0L,
    val active: Boolean = false
)

// ─── Notifications ────────────────────────────────────────────────────────────

data class AppNotification(
    val id: String = "",
    val type: NotificationType = NotificationType.DEVICE_ON,
    val title: String = "",
    val message: String = "",
    val timestamp: Timestamp? = null,
    val isRead: Boolean = false,
    val userId: String = "",
    val metadata: Map<String, Any> = emptyMap()
) {
    enum class NotificationType {
        RAIN_ALERT, LOW_WATER, OVERFLOW, DEVICE_ON, DEVICE_OFF;

        companion object {
            fun from(s: String) = entries.firstOrNull { it.name.equals(s, true) } ?: DEVICE_ON
        }
    }
}

// ─── Dashboard aggregate ──────────────────────────────────────────────────────

data class DashboardState(
    val fan: Device = Device(id = "fan"),
    val ledLight: Device = Device(id = "led_light"),
    val waterLevel: WaterLevel = WaterLevel(),
    val rain: RainStatus = RainStatus(),
    val recentNotifications: List<AppNotification> = emptyList(),
    val isLoading: Boolean = true
)
