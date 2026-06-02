package com.example.smarthome.data.model

/**
 * Matches Firebase: smarthome/sensors/water_level
 *
 * DashboardScreen uses WaterLevel.WaterStatus enum for when expressions,
 * so it must be a sealed/enum — not a plain String.
 *
 * Color rules (from DashboardScreen + DeviceControlScreen):
 *   percentage >= 80  → OVERFLOW  → Green  + overflow warning
 *   percentage <= 20  → LOW       → Red/Amber + low warning
 *   else              → NORMAL    → Blue
 */
data class WaterLevel(
    val percentage: Int = 0,
    val status: WaterStatus = WaterStatus.NORMAL,
    val lastUpdated: Long = 0L
) {
    enum class WaterStatus { LOW, NORMAL, OVERFLOW }

    companion object {
        /** Build a WaterLevel from a raw percentage int (used by repository parser) */
        fun fromPercentage(percentage: Int, lastUpdated: Long = 0L): WaterLevel {
            val status = when {
                percentage <= 20 -> WaterStatus.LOW
                percentage >= 80 -> WaterStatus.OVERFLOW
                else             -> WaterStatus.NORMAL
            }
            return WaterLevel(percentage, status, lastUpdated)
        }

        /** Build a WaterLevel from the Firebase "status" string field */
        fun fromString(percentage: Int, statusStr: String, lastUpdated: Long = 0L): WaterLevel {
            val status = when (statusStr.lowercase()) {
                "low"      -> WaterStatus.LOW
                "high",
                "overflow" -> WaterStatus.OVERFLOW
                else       -> WaterStatus.NORMAL
            }
            return WaterLevel(percentage, status, lastUpdated)
        }
    }
}