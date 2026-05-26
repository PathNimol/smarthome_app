package com.example.smarthome.data.model

import com.google.firebase.database.PropertyName

data class RainStatus(
    @get:PropertyName("isRaining") @set:PropertyName("isRaining")
    var isRaining: Boolean = false,
    val intensity: String = "none",     // "none" | "light" | "moderate" | "heavy"
    val detectedAt: Long = 0L,
    val active: Boolean = false
)