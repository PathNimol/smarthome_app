package com.example.smarthome.data.model

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