package com.example.smarthome.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.smarthome.data.model.AppNotification
import com.example.smarthome.data.model.AppNotification.NotificationType
import com.example.smarthome.data.model.RainStatus
import com.example.smarthome.data.model.WaterLevel
import com.example.smarthome.ui.components.*
import com.example.smarthome.ui.theme.*
import com.example.smarthome.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    onNavigateToDevices: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardLight)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date()),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Text(
                    "Smart Home",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // Bell icon with clean red dot badge
            val unread = state.recentNotifications.count { !it.isRead }
            Box {
                IconButton(
                    onClick = onNavigateToNotifications,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(PageBackground)
                ) {
                    Icon(
                        Icons.Default.NotificationsNone,
                        contentDescription = "Notifications",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                if (unread > 0) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(ErrorRed)
                            .border(1.5.dp, CardLight, CircleShape)
                            .align(Alignment.TopEnd)
                            .offset(x = (-4).dp, y = 4.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = DividerColor)

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.isLoading) {
                repeat(3) { LoadingCard() }
                return@Column
            }

            // ── Feature 01 — Device Status ────────────────────────────────
            // Clicking either card navigates to Device Control (Feature 01)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Devices", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                TextButton(onClick = onNavigateToDevices) {
                    Text("Manage", color = BrandBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DeviceStatusCard(
                    name     = "Fan",
                    isOn     = state.fan.isOn,
                    subLabel = if (state.fan.controlledBy == "auto") "Auto-triggered" else "1 device",
                    icon     = Icons.Default.Air,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToDevices() }
                )
                DeviceStatusCard(
                    name     = "LED Light",
                    isOn     = state.ledLight.isOn,
                    subLabel = if (state.ledLight.controlledBy == "auto") "Auto by rain" else "1 device",
                    icon     = Icons.Default.LightMode,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToDevices() }
                )
            }

            // ── Feature 02 — Water Level ──────────────────────────────────
            val waterStatusColor = when (state.waterLevel.status) {
                WaterLevel.WaterStatus.LOW      -> WarningAmber
                WaterLevel.WaterStatus.OVERFLOW -> ErrorRed
                WaterLevel.WaterStatus.NORMAL   -> BrandBlue
            }
            val waterStatusLabel = when (state.waterLevel.status) {
                WaterLevel.WaterStatus.LOW      -> "Low"
                WaterLevel.WaterStatus.OVERFLOW -> "Overflow"
                WaterLevel.WaterStatus.NORMAL   -> "Normal"
            }
            WaterLevelCard(
                percentage  = state.waterLevel.percentage,
                statusLabel = waterStatusLabel,
                statusColor = waterStatusColor
            )

            // ── Feature 03 — Rain Detection ───────────────────────────────
            RainStatusCard(rain = state.rain)

            // ── Feature 05 — Notifications Panel (last 3) ────────────────
            if (state.recentNotifications.isNotEmpty()) {
                SectionHeader(
                    title  = "Recent Alerts",
                    badge  = state.recentNotifications.count { !it.isRead }
                        .let { if (it > 0) "$it new" else null },
                    action = {
                        TextButton(onClick = onNavigateToNotifications) {
                            Text(
                                "See all",
                                color = BrandBlue,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                )
                state.recentNotifications.take(3).forEach { notif ->
                    NotificationPreviewItem(
                        notif   = notif,
                        onClick = onNavigateToNotifications
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ── Feature 03 — Rain Status Card ────────────────────────────────────────────

@Composable
fun RainStatusCard(rain: RainStatus) {
    SmartCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (rain.isRaining) RainBlueBg else PageBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (rain.isRaining) Icons.Default.Umbrella else Icons.Default.WbSunny,
                        contentDescription = null,
                        tint = if (rain.isRaining) RainBlue else WarningAmber,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        "Rain Status",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        if (rain.isRaining) "Raining" else "No Rain",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (rain.isRaining) {
                        Text(
                            "Intensity: ${rain.intensity.replaceFirstChar { it.uppercase() }}",
                            color = RainBlue,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                        if (rain.detectedAt > 0) {
                            Text(
                                "Detected at ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(rain.detectedAt))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
            StatusChip(
                label = if (rain.active) "Active" else "Inactive",
                color = if (rain.active) WarningAmber else TextSecondary
            )
        }

        if (rain.isRaining) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(RainBlueBg)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, null, tint = WarningAmber, modifier = Modifier.size(14.dp))
                Text(
                    "Rain Detected! Please bring your clothes inside.",
                    color = RainBlue,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Light", "Moderate", "Heavy").forEach { intensity ->
                    val isSelected = rain.intensity.equals(intensity, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick  = {},
                        label    = { Text(intensity, fontSize = 12.sp) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandBlue,
                            selectedLabelColor     = Color.White,
                            containerColor         = PageBackground,
                            labelColor             = TextSecondary
                        )
                    )
                }
            }
        }
    }
}

// ── Feature 05 — Notification Preview Item ────────────────────────────────────

@Composable
fun NotificationPreviewItem(notif: AppNotification, onClick: () -> Unit = {}) {
    val (icon, accentColor) = when (notif.type) {
        NotificationType.RAIN_ALERT  -> Pair(Icons.Default.WaterDrop,    RainBlue)
        NotificationType.LOW_WATER   -> Pair(Icons.Default.WaterDrop,    WarningAmber)
        NotificationType.OVERFLOW    -> Pair(Icons.Default.Warning,       ErrorRed)
        NotificationType.DEVICE_ON   -> Pair(Icons.Default.CheckCircle,  SuccessGreen)
        NotificationType.DEVICE_OFF  -> Pair(Icons.Default.PowerOff,     TextSecondary)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardLight)
            .border(1.dp,
                if (!notif.isRead) accentColor.copy(0.2f) else DividerColor,
                RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accentColor.copy(0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accentColor, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                notif.message,
                style      = MaterialTheme.typography.bodySmall,
                color      = TextPrimary,
                fontWeight = if (!notif.isRead) FontWeight.SemiBold else FontWeight.Normal
            )
            notif.timestamp?.let {
                Text(
                    SimpleDateFormat("h:mm a", Locale.getDefault()).format(it.toDate()),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
        if (!notif.isRead) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
        }
    }
}