package com.example.smarthome.ui.dashboard

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.StrokeCap
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
import com.example.smarthome.viewmodel.AuthViewModel
import com.example.smarthome.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    onNavigateToDevices       : () -> Unit,
    onNavigateToNotifications : () -> Unit,
    onLogout                  : () -> Unit,          // ← NEW
    viewModel                 : DashboardViewModel = hiltViewModel(),
    authViewModel             : AuthViewModel      = hiltViewModel()  // ← for sign-out
) {
    val state by viewModel.state.collectAsState()

    // Confirm-logout dialog state
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon             = { Icon(Icons.Default.Logout, null, tint = ErrorRed) },
            title            = { Text("Sign Out", fontWeight = FontWeight.Bold) },
            text             = { Text("Are you sure you want to sign out?", color = TextSecondary) },
            confirmButton    = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    authViewModel.signOut()
                    onLogout()
                }) {
                    Text("Sign Out", color = ErrorRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton    = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardLight
        )
    }

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
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date()),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Text(
                    "Smart Home",
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Notification bell with dot badge
                val unread = state.recentNotifications.count { !it.isRead }
                Box {
                    IconButton(
                        onClick  = onNavigateToNotifications,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PageBackground)
                    ) {
                        Icon(
                            Icons.Default.NotificationsNone,
                            contentDescription = "Notifications",
                            tint               = TextPrimary,
                            modifier           = Modifier.size(22.dp)
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

                // ── Logout button ─────────────────────────────────────────
                IconButton(
                    onClick  = { showLogoutDialog = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(PageBackground)
                ) {
                    Icon(
                        Icons.Default.Logout,
                        contentDescription = "Sign Out",
                        tint               = TextSecondary,
                        modifier           = Modifier.size(20.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = DividerColor)

        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.isLoading) {
                repeat(3) { LoadingCard() }
                return@Column
            }

            // ── Devices ───────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Devices", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                TextButton(onClick = onNavigateToDevices) {
                    Text("Manage", color = BrandBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DeviceStatusCard(
                    name     = "Fan",
                    isOn     = state.fan.isOn,
                    subLabel = if (state.fan.controlledBy == "auto") "Auto-triggered" else "1 device",
                    icon     = Icons.Default.Air,
                    modifier = Modifier.weight(1f).clickable { onNavigateToDevices() }
                )
                DeviceStatusCard(
                    name     = "LED Light",
                    isOn     = state.ledLight.isOn,
                    subLabel = if (state.ledLight.controlledBy == "auto") "Auto by rain" else "1 device",
                    icon     = Icons.Default.LightMode,
                    modifier = Modifier.weight(1f).clickable { onNavigateToDevices() }
                )
            }

            WaterLevelCard(state.waterLevel)
            RainStatusCard(rain = state.rain)

            if (state.recentNotifications.isNotEmpty()) {
                SectionHeader(
                    title  = "Recent Alerts",
                    badge  = state.recentNotifications.count { !it.isRead }
                        .let { if (it > 0) "$it new" else null },
                    action = {
                        TextButton(onClick = onNavigateToNotifications) {
                            Text("See all", color = BrandBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
                state.recentNotifications.take(3).forEach { notif ->
                    NotificationPreviewItem(notif = notif, onClick = onNavigateToNotifications)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ── Water Level Card ──────────────────────────────────────────────────────────

@Composable
fun WaterLevelCard(waterLevel: WaterLevel) {
    val statusColor = when (waterLevel.status) {
        WaterLevel.WaterStatus.LOW      -> WarningAmber
        WaterLevel.WaterStatus.OVERFLOW -> ErrorRed
        WaterLevel.WaterStatus.NORMAL   -> BrandBlue
    }
    val statusLabel = when (waterLevel.status) {
        WaterLevel.WaterStatus.LOW      -> "Low"
        WaterLevel.WaterStatus.OVERFLOW -> "Overflow"
        WaterLevel.WaterStatus.NORMAL   -> "Normal"
    }

    val progress by animateFloatAsState(
        targetValue   = waterLevel.percentage / 100f,
        animationSpec = tween(1000, easing = EaseOutCubic),
        label         = "water"
    )

    SmartCard {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier         = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                        .background(statusColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.WaterDrop, null, tint = statusColor, modifier = Modifier.size(18.dp))
                }
                Text("Water Level", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
            StatusChip(statusLabel, statusColor)
        }

        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${waterLevel.percentage}%", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = statusColor)
            Text("capacity", style = MaterialTheme.typography.bodySmall, color = TextSecondary,
                modifier = Modifier.padding(bottom = 5.dp))
        }

        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress    = { progress },
            modifier    = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color       = statusColor,
            trackColor  = DividerColor,
            strokeCap   = StrokeCap.Round
        )
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Low <20%",      style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text("Overflow >90%", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }

        if (waterLevel.status != WaterLevel.WaterStatus.NORMAL) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(statusColor.copy(alpha = 0.08f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, null, tint = statusColor, modifier = Modifier.size(14.dp))
                Text(
                    when (waterLevel.status) {
                        WaterLevel.WaterStatus.LOW      -> "💧 Water level is critically low! Refill as soon as possible."
                        WaterLevel.WaterStatus.OVERFLOW -> "⚠️ Tank is nearly full! Risk of overflow."
                        else                            -> ""
                    },
                    style      = MaterialTheme.typography.bodySmall,
                    color      = statusColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ── Rain Status Card ──────────────────────────────────────────────────────────

@Composable
fun RainStatusCard(rain: RainStatus) {
    SmartCard {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (rain.isRaining) RainBlueBg else PageBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (rain.isRaining) Icons.Default.WaterDrop else Icons.Default.WbSunny,
                        contentDescription = null,
                        tint               = if (rain.isRaining) RainBlue else TextSecondary,
                        modifier           = Modifier.size(24.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Rain Status", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(
                        if (rain.isRaining) "Raining" else "No Rain",
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary
                    )
                    if (rain.isRaining) {
                        Text(
                            "Intensity: ${rain.intensity.replaceFirstChar { it.uppercase() }}",
                            color      = RainBlue,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 13.sp
                        )
                    }
                    if (rain.isRaining && rain.detectedAt > 0) {
                        Text(
                            "Detected at ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(rain.detectedAt))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (rain.active) WarningAmber.copy(0.12f) else TextSecondary.copy(0.1f))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    if (rain.active) "Active" else "Inactive",
                    color      = if (rain.active) WarningAmber else TextSecondary,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (rain.isRaining) {
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(RainBlueBg)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, null, tint = WarningAmber, modifier = Modifier.size(14.dp))
                Text(
                    "Rain Detected! Please bring your clothes inside!",
                    color      = RainBlue,
                    style      = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Light", "Moderate", "Heavy").forEach { intensity ->
                    val isSelected = rain.intensity.equals(intensity, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.5.dp, if (isSelected) WarningAmber else DividerColor, RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            intensity,
                            fontSize   = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color      = if (isSelected) WarningAmber else TextSecondary
                        )
                    }
                }
            }
        }
    }
}

// ── Notification Preview Item ─────────────────────────────────────────────────

@Composable
fun NotificationPreviewItem(notif: AppNotification, onClick: () -> Unit = {}) {
    val (icon, accentColor) = when (notif.type) {
        NotificationType.RAIN_ALERT -> Pair(Icons.Default.WaterDrop,   RainBlue)
        NotificationType.LOW_WATER  -> Pair(Icons.Default.WaterDrop,   WarningAmber)
        NotificationType.OVERFLOW   -> Pair(Icons.Default.Warning,      ErrorRed)
        NotificationType.DEVICE_ON  -> Pair(Icons.Default.CheckCircle, SuccessGreen)
        NotificationType.DEVICE_OFF -> Pair(Icons.Default.PowerOff,    TextSecondary)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardLight)
            .border(1.dp, if (!notif.isRead) accentColor.copy(0.2f) else DividerColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Box(
            modifier         = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(accentColor.copy(0.1f)),
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
                    style    = MaterialTheme.typography.bodySmall,
                    color    = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
        if (!notif.isRead) {
            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(accentColor))
        }
    }
}