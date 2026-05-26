package com.example.smarthome.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.smarthome.data.model.*
import com.example.smarthome.data.model.AppNotification.NotificationType
import com.google.firebase.Timestamp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartHomeRepository @Inject constructor(
    private val auth: FirebaseAuth,
    database: FirebaseDatabase,
    private val firestore: FirebaseFirestore
) {
    private val root = database.getReference("smarthome")

    val currentUserId: String get() = auth.currentUser?.uid ?: ""

    // ─── Device Control ───────────────────────────────────────────────────────

    suspend fun toggleDevice(deviceId: String, newState: Boolean) {
        val stateStr = if (newState) "ON" else "OFF"
        val updates = mapOf(
            "state"         to stateStr,
            "lastChanged"   to System.currentTimeMillis(),
            "controlledBy"  to "manual"
        )
        root.child("devices/$deviceId").updateChildren(updates).await()
        logDeviceNotification(deviceId, newState)
    }

    private suspend fun logDeviceNotification(deviceId: String, isOn: Boolean) {
        val label = if (deviceId == "fan") "Fan" else "LED Light"
        val type  = if (isOn) NotificationType.DEVICE_ON else NotificationType.DEVICE_OFF
        addNotification(
            type     = type,
            title    = "$label turned ${if (isOn) "ON" else "OFF"}",
            message  = "$label turned ${if (isOn) "ON" else "OFF"}.",
            metadata = mapOf("deviceId" to deviceId)
        )
    }

    // ─── Real-time Flows ──────────────────────────────────────────────────────

    fun observeDevice(deviceId: String): Flow<Device> = callbackFlow {
        val ref = root.child("devices/$deviceId")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val device = Device(
                    id            = deviceId,
                    state         = snapshot.child("state").getValue(String::class.java) ?: "OFF",
                    lastChanged   = snapshot.child("lastChanged").getValue(Long::class.java) ?: 0L,
                    controlledBy  = snapshot.child("controlledBy").getValue(String::class.java) ?: "manual",
                    powerWatts    = snapshot.child("powerWatts").getValue(Int::class.java) ?: 0
                )
                trySend(device)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun observeWaterLevel(): Flow<WaterLevel> = callbackFlow {
        val ref = root.child("sensors/water_level")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val pct = snapshot.child("percentage").getValue(Int::class.java) ?: 0
                val ts  = snapshot.child("lastUpdated").getValue(Long::class.java) ?: 0L
                trySend(WaterLevel.fromPercentage(pct, ts))
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun observeRain(): Flow<RainStatus> = callbackFlow {
        val ref = root.child("sensors/rain")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val rain = RainStatus(
                    isRaining  = snapshot.child("isRaining").getValue(Boolean::class.java) ?: false,
                    intensity  = snapshot.child("intensity").getValue(String::class.java) ?: "none",
                    detectedAt = snapshot.child("detectedAt").getValue(Long::class.java) ?: 0L,
                    active     = snapshot.child("active").getValue(Boolean::class.java) ?: false
                )
                trySend(rain)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // ─── Notifications ────────────────────────────────────────────────────────

    fun observeNotifications(limit: Long = 20): Flow<List<AppNotification>> = callbackFlow {
        val query = firestore.collection("notifications")
            .whereEqualTo("userId", currentUserId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)

        val registration = query.addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
                return@addSnapshotListener
            }
            val notifications = snap?.documents?.mapNotNull { doc ->
                val typeStr = doc.getString("type") ?: return@mapNotNull null
                AppNotification(
                    id        = doc.id,
                    type      = NotificationType.from(typeStr),
                    title     = doc.getString("title") ?: "",
                    message   = doc.getString("message") ?: "",
                    timestamp = doc.getTimestamp("timestamp"),
                    isRead    = doc.getBoolean("isRead") ?: false,
                    userId    = doc.getString("userId") ?: "",
                    metadata  = @Suppress("UNCHECKED_CAST")
                    (doc.get("metadata") as? Map<String, Any>) ?: emptyMap()
                )
            } ?: emptyList()
            trySend(notifications)
        }
        awaitClose { registration.remove() }
    }

    suspend fun markAllRead() {
        val batch = firestore.batch()
        val unread = firestore.collection("notifications")
            .whereEqualTo("userId", currentUserId)
            .whereEqualTo("isRead", false)
            .get().await()
        unread.documents.forEach { batch.update(it.reference, "isRead", true) }
        batch.commit().await()
    }

    private suspend fun addNotification(
        type: NotificationType,
        title: String,
        message: String,
        metadata: Map<String, Any> = emptyMap()
    ) {
        firestore.collection("notifications").add(
            mapOf(
                "type"      to type.name,
                "title"     to title,
                "message"   to message,
                "timestamp" to Timestamp.now(),
                "isRead"    to false,
                "userId"    to currentUserId,
                "metadata"  to metadata
            )
        ).await()
    }

    // ─── Simulation ───────────────────────────────────────────────────────────

    suspend fun simulateWaterLevel(percentage: Int) {
        root.child("sensors/water_level").updateChildren(
            mapOf(
                "percentage"  to percentage,
                "lastUpdated" to System.currentTimeMillis()
            )
        ).await()
        when {
            percentage < 20 -> addNotification(
                NotificationType.LOW_WATER,
                "Water Level Low!",
                "⚠️ Water Level Low ($percentage%)! Please refill the tank soon.",
                mapOf("sensorValue" to percentage)
            )
            percentage >= 90 -> addNotification(
                NotificationType.OVERFLOW,
                "Tank Almost Full!",
                "🚨 Tank Almost Full ($percentage%)! Turn off water supply.",
                mapOf("sensorValue" to percentage)
            )
        }
    }

    suspend fun simulateRain(isRaining: Boolean, intensity: String = "moderate") {
        root.child("sensors/rain").updateChildren(
            mapOf(
                "isRaining"  to isRaining,
                "intensity"  to intensity,
                "detectedAt" to System.currentTimeMillis(),
                "active"     to isRaining
            )
        ).await()
        if (isRaining) {
            addNotification(
                NotificationType.RAIN_ALERT,
                "Rain Detected!",
                "⚠️ Rain Detected! Please bring your clothes inside."
            )
        }
    }
}