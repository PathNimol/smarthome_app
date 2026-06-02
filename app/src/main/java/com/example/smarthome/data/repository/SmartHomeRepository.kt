package com.example.smarthome.data.repository

import com.example.smarthome.data.model.*
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartHomeRepository @Inject constructor(
    database: FirebaseDatabase
) {
    // ── Root reference ────────────────────────────────────────────────────
    private val root = database.getReference("smarthome")

    // ── Device paths (exact Firebase keys) ───────────────────────────────
    private val fanRef      = root.child("devices/fan")
    private val ledRef      = root.child("devices/led_light")   // underscore key

    // ── Sensor paths ──────────────────────────────────────────────────────
    private val rainRef       = root.child("sensors/rain")
    private val rainAnalogRef = root.child("sensors/rain_analog")
    private val waterRef      = root.child("sensors/water_level")

    // ── Alert path ────────────────────────────────────────────────────────
    private val alertRef = root.child("alerts")

    // ── System path ───────────────────────────────────────────────────────
    private val systemRef = root.child("system")

    // ─────────────────────────────────────────────────────────────────────
    // Real-time listeners — returns Flow<SmartHomeState>
    // ─────────────────────────────────────────────────────────────────────

    fun observeAll(): Flow<SmartHomeState> = callbackFlow {
        var current = SmartHomeState()

        fun emit() = trySend(current)

        // Fan listener
        val fanListener = root.child("devices/fan")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    current = current.copy(fan = parseDevice(snapshot), isLoading = false)
                    emit()
                }
                override fun onCancelled(error: DatabaseError) {
                    current = current.copy(error = error.message)
                    emit()
                }
            })

        // LED light listener — note: "led_light" with underscore
        val ledListener = root.child("devices/led_light")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    current = current.copy(ledLight = parseDevice(snapshot), isLoading = false)
                    emit()
                }
                override fun onCancelled(error: DatabaseError) {
                    current = current.copy(error = error.message)
                    emit()
                }
            })

        // Rain sensor listener
        val rainListener = root.child("sensors/rain")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    current = current.copy(rain = parseRain(snapshot))
                    emit()
                }
                override fun onCancelled(error: DatabaseError) {
                    current = current.copy(error = error.message)
                    emit()
                }
            })

        // Rain analog listener
        val rainAnalogListener = root.child("sensors/rain_analog")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    current = current.copy(
                        rainAnalog = snapshot.getValue(Int::class.java) ?: 0
                    )
                    emit()
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        // Water level listener
        val waterListener = root.child("sensors/water_level")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    current = current.copy(waterLevel = parseWaterLevel(snapshot))
                    emit()
                }
                override fun onCancelled(error: DatabaseError) {
                    current = current.copy(error = error.message)
                    emit()
                }
            })

        // Alert listener
        val alertListener = root.child("alerts")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    current = current.copy(alert = parseAlert(snapshot))
                    emit()
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        // System listener
        val systemListener = root.child("system")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    current = current.copy(
                        lastSync = snapshot.child("lastSync").getValue(Long::class.java) ?: 0L
                    )
                    emit()
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        awaitClose {
            root.child("devices/fan").removeEventListener(fanListener)
            root.child("devices/led_light").removeEventListener(ledListener)
            root.child("sensors/rain").removeEventListener(rainListener)
            root.child("sensors/rain_analog").removeEventListener(rainAnalogListener)
            root.child("sensors/water_level").removeEventListener(waterListener)
            root.child("alerts").removeEventListener(alertListener)
            root.child("system").removeEventListener(systemListener)
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Write operations — exact Firebase field names
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Toggle fan ON/OFF.
     * Writes "ON" or "OFF" string to match Firebase format.
     */
    suspend fun setFan(isOn: Boolean) {
        fanRef.updateChildren(
            mapOf(
                "state"       to if (isOn) "ON" else "OFF",   // string, not boolean
                "lastChanged" to ServerValue.TIMESTAMP,
                "controlledBy" to "manual"
            )
        ).await()
    }

    /**
     * Toggle LED light ON/OFF.
     * Path: smarthome/devices/led_light (underscore)
     */
    suspend fun setLed(isOn: Boolean) {
        ledRef.updateChildren(
            mapOf(
                "state"       to if (isOn) "ON" else "OFF",   // string, not boolean
                "lastChanged" to ServerValue.TIMESTAMP,
                "controlledBy" to "manual"
            )
        ).await()
    }

    /**
     * Simulate water level.
     * Writes to smarthome/sensors/water_level
     */
    suspend fun setWaterLevel(percentage: Int) {
        val status = when {
            percentage < 20  -> "low"
            percentage > 80  -> "high"
            else             -> "normal"
        }
        waterRef.updateChildren(
            mapOf(
                "percentage"  to percentage,
                "status"      to status,
                "lastUpdated" to ServerValue.TIMESTAMP
            )
        ).await()
    }

    /**
     * Simulate rain sensor.
     * Writes to smarthome/sensors/rain
     * Both "active" and "isRaining" are written to keep Firebase in sync.
     */
    suspend fun setRain(isRaining: Boolean, intensity: String = "") {
        rainRef.updateChildren(
            mapOf(
                "isRaining"  to isRaining,
                "active"     to isRaining,   // keep both fields in sync
                "intensity"  to intensity,
                "detectedAt" to ServerValue.TIMESTAMP
            )
        ).await()
    }

    // ─────────────────────────────────────────────────────────────────────
    // Aliases — match the method names the ViewModel calls
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Single entry-point for toggling any device by id.
     * id must be exactly "fan" or "led_light" (matches Firebase keys).
     */
    suspend fun toggleDevice(id: String, isOn: Boolean) {
        when (id) {
            "fan"       -> setFan(isOn)
            "led_light" -> setLed(isOn)
            else        -> throw IllegalArgumentException("Unknown device id: $id")
        }
    }

    /**
     * Alias used by SimulationPanel slider.
     * Writes to smarthome/sensors/water_level
     */
    suspend fun simulateWaterLevel(percentage: Int) = setWaterLevel(percentage)

    /**
     * Alias used by SimulationPanel rain buttons.
     * Writes to smarthome/sensors/rain
     */
    suspend fun simulateRain(isRaining: Boolean, intensity: String = "moderate") =
        setRain(isRaining, intensity)

    /**
     * Single-device observer kept for backward compatibility.
     * Returns a Flow<Device> for one device path.
     */
    fun observeDevice(id: String): kotlinx.coroutines.flow.Flow<Device> =
        kotlinx.coroutines.flow.callbackFlow {
            val ref = root.child("devices/$id")
            val listener = ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    trySend(parseDevice(snapshot))
                }
                override fun onCancelled(error: DatabaseError) { close(Exception(error.message)) }
            })
            awaitClose { ref.removeEventListener(listener) }
        }

    // ─────────────────────────────────────────────────────────────────────
    // Individual Flow observers — used by DashboardViewModel combine()
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Observe water level in real-time.
     * Returns Flow<WaterLevel> — maps Firebase "status" string → WaterStatus enum.
     * Path: smarthome/sensors/water_level
     */
    fun observeWaterLevel(): Flow<WaterLevel> = callbackFlow {
        val listener = waterRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(parseWaterLevel(snapshot))
            }
            override fun onCancelled(error: DatabaseError) { close(Exception(error.message)) }
        })
        awaitClose { waterRef.removeEventListener(listener) }
    }

    /**
     * Observe rain sensor in real-time.
     * Returns Flow<RainStatus>.
     * Path: smarthome/sensors/rain
     */
    fun observeRain(): Flow<RainStatus> = callbackFlow {
        val listener = rainRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(parseRain(snapshot))
            }
            override fun onCancelled(error: DatabaseError) { close(Exception(error.message)) }
        })
        awaitClose { rainRef.removeEventListener(listener) }
    }

    /**
     * Observe recent notifications.
     * Returns Flow<List<AppNotification>>.
     * Notifications are stored under smarthome/notifications in Realtime DB,
     * ordered by timestamp descending, limited to [limit] items.
     *
     * If you use Firestore for notifications, replace this implementation
     * with a Firestore listener — the ViewModel API stays the same.
     */
    fun observeNotifications(limit: Int = 3): Flow<List<AppNotification>> = callbackFlow {
        val ref = root.child("notifications")
            .orderByChild("timestamp")
            .limitToLast(limit)

        val listener = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val notifications = snapshot.children
                    .mapNotNull { child -> parseNotification(child) }
                    .sortedByDescending { it.timestamp?.seconds ?: 0L }
                trySend(notifications)
            }
            override fun onCancelled(error: DatabaseError) { close(Exception(error.message)) }
        })
        awaitClose { ref.removeEventListener(listener) }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Parsers — map DataSnapshot to model classes
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Parses a device snapshot.
     * Firebase "state" is "ON"/"OFF" string — NOT boolean.
     */
    private fun parseDevice(snapshot: DataSnapshot): Device {
        return Device(
            id           = snapshot.key ?: "",
            state        = snapshot.child("state").getValue(String::class.java) ?: "OFF",
            controlledBy = snapshot.child("controlledBy").getValue(String::class.java) ?: "manual",
            lastChanged  = snapshot.child("lastChanged").getValue(Long::class.java) ?: 0L,
            powerWatts   = snapshot.child("powerWatts").getValue(Int::class.java) ?: 0
        )
    }

    /**
     * Parses rain sensor snapshot.
     * Uses "isRaining" as source of truth (not "active").
     */
    /**
     * Parses rain sensor snapshot → RainStatus
     * DashboardScreen uses RainStatus (not RainSensor).
     */
    private fun parseRain(snapshot: DataSnapshot): RainStatus {
        return RainStatus(
            isRaining  = snapshot.child("isRaining").getValue(Boolean::class.java) ?: false,
            active     = snapshot.child("active").getValue(Boolean::class.java) ?: false,
            intensity  = snapshot.child("intensity").getValue(String::class.java) ?: "",
            detectedAt = snapshot.child("detectedAt").getValue(Long::class.java) ?: 0L
        )
    }

    /**
     * Parses water level snapshot → WaterLevel with WaterStatus enum.
     * Uses WaterLevel.fromString() to convert Firebase "status" string → enum.
     */
    private fun parseWaterLevel(snapshot: DataSnapshot): WaterLevel {
        val percentage  = snapshot.child("percentage").getValue(Int::class.java) ?: 0
        val statusStr   = snapshot.child("status").getValue(String::class.java) ?: "normal"
        val lastUpdated = snapshot.child("lastUpdated").getValue(Long::class.java) ?: 0L
        return WaterLevel.fromString(percentage, statusStr, lastUpdated)
    }


    /**
     * Parses alert snapshot.
     */
    private fun parseAlert(snapshot: DataSnapshot): Alert {
        return Alert(
            active = snapshot.child("active").getValue(Boolean::class.java) ?: false,
            type   = snapshot.child("type").getValue(String::class.java) ?: ""
        )
    }

    /**
     * Parses a notification snapshot from smarthome/notifications/{id}.
     * Firebase structure:
     * {
     *   "type":      "RAIN_ALERT" | "LOW_WATER" | "OVERFLOW" | "DEVICE_ON" | "DEVICE_OFF"
     *   "message":   "Rain detected!",
     *   "isRead":    false,
     *   "timestamp": <epoch ms>
     * }
     */
    /**
     * Mark all notifications as read.
     * Sets isRead = true on every child under smarthome/notifications.
     */
    suspend fun markAllRead() {
        val snapshot = root.child("notifications").get().await()
        val updates = mutableMapOf<String, Any>()
        snapshot.children.forEach { child ->
            child.key?.let { key ->
                updates["notifications/$key/isRead"] = true
            }
        }
        if (updates.isNotEmpty()) {
            root.updateChildren(updates).await()
        }
    }

    /**
     * Mark a single notification as read by its id.
     */
    suspend fun markRead(id: String) {
        root.child("notifications/$id/isRead").setValue(true).await()
    }

    private fun parseNotification(snapshot: DataSnapshot): AppNotification? {
        val typeStr  = snapshot.child("type").getValue(String::class.java) ?: return null
        val message  = snapshot.child("message").getValue(String::class.java) ?: return null
        val isRead   = snapshot.child("isRead").getValue(Boolean::class.java) ?: false
        val epochMs  = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L

        val type = runCatching {
            AppNotification.NotificationType.valueOf(typeStr)
        }.getOrNull() ?: return null

        // Convert epoch ms → Firestore Timestamp so DashboardScreen can call .toDate()
        val timestamp = com.google.firebase.Timestamp(epochMs / 1000L, ((epochMs % 1000L) * 1_000_000L).toInt())

        return AppNotification(
            id        = snapshot.key ?: "",
            type      = type,
            message   = message,
            isRead    = isRead,
            timestamp = timestamp
        )
    }
}