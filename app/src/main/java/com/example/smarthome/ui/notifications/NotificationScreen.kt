package com.example.smarthome.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smarthome.data.model.AppNotification
import com.example.smarthome.data.model.AppNotification.NotificationType
import com.example.smarthome.ui.theme.*
import com.example.smarthome.viewmodel.NotificationViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationScreen(viewModel: NotificationViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
    ) {
        // ── Header ────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .background(CardLight)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Notifications",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (state.unreadCount > 0) {
                        Text(
                            "${state.unreadCount} unread alert${if (state.unreadCount == 1) "" else "s"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandBlue
                        )
                    }
                }
                if (state.unreadCount > 0) {
                    TextButton(onClick = { viewModel.markAllRead() }) {
                        Text(
                            "Mark all read",
                            color = BrandBlue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (state.unreadCount > 0) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BrandBlue.copy(0.07f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Notifications, null,
                        tint = BrandBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "You have ${state.unreadCount} unread alert${if (state.unreadCount == 1) "" else "s"}",
                        color = BrandBlue,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }

        HorizontalDivider(color = DividerColor)

        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = BrandBlue,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            state.notifications.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(PageBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.NotificationsNone, null,
                                tint = TextSecondary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No notifications yet",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "You're all caught up!",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            else -> {
                Text(
                    "ALL ALERTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = state.notifications,
                        key   = { it.id }
                    ) { notification ->
                        NotificationItem(notification)
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }
            }
        }
    }
}

// ── Notification Item ─────────────────────────────────────────────────────────

@Composable
fun NotificationItem(notif: AppNotification) {
    // Feature 04 — all 4 notification types with correct icons and colors
    data class NStyle(val icon: ImageVector, val color: Color, val badge: String)

    val s = when (notif.type) {
        // Feature 03 — Rain Alert
        NotificationType.RAIN_ALERT  -> NStyle(Icons.Default.WaterDrop,   RainBlue,     "Rain Alert")
        // Feature 02 — Low Water Alert
        NotificationType.LOW_WATER   -> NStyle(Icons.Default.WaterDrop,   WarningAmber, "Low Water")
        // Feature 02 — Overflow Alert
        NotificationType.OVERFLOW    -> NStyle(Icons.Default.Warning,      ErrorRed,     "Overflow")
        // Feature 01 — Device Status Messages
        NotificationType.DEVICE_ON   -> NStyle(Icons.Default.CheckCircle, SuccessGreen, "Device")
        NotificationType.DEVICE_OFF  -> NStyle(Icons.Default.PowerOff,    TextSecondary,"Device")
    }

    val isUnread = !notif.isRead

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardLight)
            .border(
                1.dp,
                if (isUnread) s.color.copy(0.2f) else DividerColor,
                RoundedCornerShape(14.dp)
            )
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Icon box
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(s.color.copy(0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(s.icon, null, tint = s.color, modifier = Modifier.size(20.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            // Badge + timestamp row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(s.color.copy(0.1f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        s.badge,
                        color = s.color,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        formatNotifTime(notif),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    if (isUnread) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(s.color)
                        )
                    }
                }
            }

            Spacer(Modifier.height(5.dp))
            Text(
                notif.message,
                style      = MaterialTheme.typography.bodySmall,
                color      = if (isUnread) TextPrimary else TextSecondary,
                fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal,
                lineHeight = 18.sp
            )
        }
    }
}

private fun formatNotifTime(notif: AppNotification): String {
    val date = notif.timestamp?.toDate() ?: return ""
    val today    = Calendar.getInstance()
    val notifCal = Calendar.getInstance().also { it.time = date }
    val timeFmt  = SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
    return if (today.get(Calendar.DAY_OF_YEAR) == notifCal.get(Calendar.DAY_OF_YEAR)) timeFmt
    else "Yesterday $timeFmt"
}