// ui/notifications/NotificationScreen.kt
package com.smarthome.ui.notifications

import androidx.compose.foundation.*
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
import com.smarthome.data.model.AppNotification
import com.smarthome.data.model.AppNotification.NotificationType
import com.smarthome.ui.theme.*
import com.smarthome.viewmodel.NotificationViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationScreen(viewModel: NotificationViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Sticky Header ─────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Notifications", style = MaterialTheme.typography.headlineMedium)
                    if (state.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(ErrorRed)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("${state.unreadCount}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (state.unreadCount > 0) {
                    TextButton(onClick = { viewModel.markAllRead() }) {
                        Text("Mark all read", color = Teal700, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Unread banner
            if (state.unreadCount > 0) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Teal50)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Notifications, null, tint = Teal700, modifier = Modifier.size(18.dp))
                    Text(
                        "You have ${state.unreadCount} unread alert${if (state.unreadCount == 1) "" else "s"}",
                        color = Teal700,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Teal700)
            }
            return@Column
        }

        if (state.notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.NotificationsNone, null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No notifications yet", color = TextSecondary)
                }
            }
            return@Column
        }

        // ── Notifications List ────────────────────────────────────────────
        Text(
            "All Notifications",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.notifications, key = { it.id }) { notif ->
                NotificationItem(notif)
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ── Notification List Item ────────────────────────────────────────────────────

@Composable
fun NotificationItem(notif: AppNotification) {
    data class Style(val icon: ImageVector, val iconTint: Color, val iconBg: Color, val badgeColor: Color, val badgeText: String)

    val style = when (notif.type) {
        NotificationType.RAIN_ALERT -> Style(Icons.Default.WaterDrop, RainBlue, RainBlueBg, RainBlue, "Rain Alert")
        NotificationType.LOW_WATER  -> Style(Icons.Default.WaterDrop, ErrorRed, ErrorRed.copy(0.1f), ErrorRed, "Low Water")
        NotificationType.OVERFLOW   -> Style(Icons.Default.Warning, WarningAmber, WarningAmber.copy(0.1f), WarningAmber, "Overflow")
        NotificationType.DEVICE_ON,
        NotificationType.DEVICE_OFF -> Style(Icons.Default.CheckCircle, SuccessGreen, Teal50, Teal700, "Device")
    }

    val bgColor = if (!notif.isRead)
        when (notif.type) {
            NotificationType.RAIN_ALERT -> RainBlueBg.copy(alpha = 0.4f)
            NotificationType.LOW_WATER  -> ErrorRed.copy(alpha = 0.05f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        }
    else MaterialTheme.colorScheme.surface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(style.iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(style.icon, null, tint = style.iconTint, modifier = Modifier.size(22.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            // Badge + timestamp row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(style.badgeColor.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(style.badgeText, color = style.badgeColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatNotifTime(notif),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    if (!notif.isRead) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(RainBlue))
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(notif.message, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

private fun formatNotifTime(notif: AppNotification): String {
    val date = notif.timestamp?.toDate() ?: return ""
    val today = Calendar.getInstance()
    val notifCal = Calendar.getInstance().also { it.time = date }
    val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
    return if (today.get(Calendar.DAY_OF_YEAR) == notifCal.get(Calendar.DAY_OF_YEAR)) timeFmt
           else "Yesterday $timeFmt"
}
