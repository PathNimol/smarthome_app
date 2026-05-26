// ui/dashboard/DashboardScreen.kt
package com.smarthome.ui.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smarthome.data.model.*
import com.smarthome.data.model.AppNotification.NotificationType
import com.smarthome.ui.components.*
import com.smarthome.ui.theme.*
import com.smarthome.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    onNotificationClick: () -> Unit,
    onSeeAllNotifications: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date()),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            // Bell with unread badge
            val unread = state.recentNotifications.count { !it.isRead }
            IconButton(onClick = onNotificationClick) {
                BadgedBox(
                    badge = {
                        if (unread > 0) Badge(containerColor = ErrorRed) { Text("$unread") }
                    }
                ) {
                    Icon(Icons.Default.Notifications, "Notifications")
                }
            }
        }

        Text("Smart Home Dashboard", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Control center — all devices and sensors in one place",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        if (state.isLoading) {
            repeat(4) { LoadingCard() }
            return@Column
        }

        // ── Device Cards ──────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DeviceStatusCard(
                name = "Fan",
                isOn = state.fan.isOn,
                subLabel = if (state.fan.controlledBy == "auto") "Auto-triggered" else "Controlled manually",
                icon = Icons.Default.Air,
                modifier = Modifier.weight(1f)
            )
            DeviceStatusCard(
                name = "LED Light",
                isOn = state.ledLight.isOn,
                subLabel = if (state.ledLight.controlledBy == "auto") "Auto-triggered by rain" else "Controlled manually",
                icon = Icons.Default.LightMode,
                modifier = Modifier.weight(1f)
            )
        }

        // ── Water Level ───────────────────────────────────────────────────
        val waterStatusColor = when (state.waterLevel.status) {
            WaterLevel.WaterStatus.LOW -> WarningAmber
            WaterLevel.WaterStatus.OVERFLOW -> ErrorRed
            WaterLevel.WaterStatus.NORMAL -> SuccessGreen
        }
        val waterStatusLabel = when (state.waterLevel.status) {
            WaterLevel.WaterStatus.LOW -> "Low"
            WaterLevel.WaterStatus.OVERFLOW -> "Overflow"
            WaterLevel.WaterStatus.NORMAL -> "Normal"
        }
        WaterLevelCard(
            percentage = state.waterLevel.percentage,
            statusLabel = waterStatusLabel,
            statusColor = waterStatusColor
        )

        // ── Rain Status ───────────────────────────────────────────────────
        RainStatusCard(rain = state.rain)

        // ── Recent Notifications ──────────────────────────────────────────
        if (state.recentNotifications.isNotEmpty()) {
            SectionHeader(
                title = "🔔 Recent Notifications",
                badge = if (state.recentNotifications.any { !it.isRead })
                    "${state.recentNotifications.count { !it.isRead }} new" else null,
                action = {
                    TextButton(onClick = onSeeAllNotifications) {
                        Text("See all", color = Teal700, fontWeight = FontWeight.SemiBold)
                    }
                }
            )
            state.recentNotifications.take(3).forEach { notif ->
                NotificationPreviewItem(notif)
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ── Rain Status Card ──────────────────────────────────────────────────────────

@Composable
fun RainStatusCard(rain: RainStatus) {
    SmartCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(RainBlueBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.WaterDrop, "Rain", tint = RainBlue, modifier = Modifier.size(24.dp))
                }
                Column {
                    Text("Rain Status", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(
                        if (rain.isRaining) "Raining" else "Clear",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (rain.isRaining) {
                        Text(
                            "Intensity: ${rain.intensity.replaceFirstChar { it.uppercase() }}",
                            color = RainBlue,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            "Detected at ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(rain.detectedAt))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
            StatusChip(
                if (rain.active) "Active" else "Inactive",
                if (rain.active) WarningAmber else TextSecondary
            )
        }

        // Rain alert banner
        if (rain.isRaining) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(RainBlueBg)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Notifications, null, tint = RainBlue, modifier = Modifier.size(16.dp))
                Icon(Icons.Default.Warning, null, tint = WarningAmber, modifier = Modifier.size(14.dp))
                Text(
                    "Rain Detected! Please bring your clothes inside.",
                    color = RainBlue,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }

            // Intensity selector chips
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Light", "Moderate", "Heavy").forEach { intensity ->
                    val isSelected = rain.intensity.equals(intensity, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = {},
                        label = { Text(intensity, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Teal700,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }
    }
}

// ── Notification Preview Item ─────────────────────────────────────────────────

@Composable
fun NotificationPreviewItem(notif: AppNotification) {
    val (iconBg, icon, iconTint) = when (notif.type) {
        NotificationType.RAIN_ALERT  -> Triple(RainBlueBg, Icons.Default.WaterDrop, RainBlue)
        NotificationType.LOW_WATER   -> Triple(ErrorRed.copy(0.1f), Icons.Default.WaterDrop, ErrorRed)
        NotificationType.OVERFLOW    -> Triple(WarningAmber.copy(0.1f), Icons.Default.Warning, WarningAmber)
        NotificationType.DEVICE_ON,
        NotificationType.DEVICE_OFF  -> Triple(Teal50, Icons.Default.CheckCircle, SuccessGreen)
    }

    val bgColor = if (!notif.isRead) RainBlueBg.copy(alpha = 0.5f)
                  else MaterialTheme.colorScheme.surface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(notif.message, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            notif.timestamp?.let {
                Text(
                    SimpleDateFormat("h:mm a", Locale.getDefault()).format(it.toDate()),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        if (!notif.isRead) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(RainBlue)
            )
        }
    }
}
