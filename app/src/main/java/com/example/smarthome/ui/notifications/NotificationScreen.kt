package com.example.smarthome.ui.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

// ── Channel helper — called once from MainActivity.onCreate() ─────────────────

fun createNotificationChannel(context: Context) {
    val channel = NotificationChannel(
        "smarthome_alerts", "Smart Home Alerts", NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = "Smart home sensor and device alerts"
        enableLights(true)
        enableVibration(true)
    }
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}

// ── Reusable badge wrapper ────────────────────────────────────────────────────

@Composable
fun NotificationBadge(count: Int, content: @Composable () -> Unit) {
    BadgedBox(
        badge = {
            if (count > 0) {
                Badge(containerColor = ErrorRed, contentColor = Color.White) {
                    Text(if (count > 99) "99+" else count.toString(), fontSize = 9.sp)
                }
            }
        }
    ) { content() }
}

// ── Screen ────────────────────────────────────────────────────────────────────

/**
 * [viewModel] is provided by the caller (MainActivity) so that the same
 * hoisted instance is reused. This ensures:
 *   • No double Firestore subscription when navigating to this tab.
 *   • Unread count shown in the bottom-bar badge stays in sync.
 * Falls back to hiltViewModel() only when previewed in isolation.
 */
@Composable
fun NotificationScreen(
    onNavigateToDevices   : () -> Unit = {},
    onNavigateToDashboard : () -> Unit = {},
    viewModel             : NotificationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    val sorted = remember(state.notifications) {
        state.notifications.sortedByDescending { it.timestamp?.toDate() }
    }
    val unread = sorted.filter { !it.isRead }
    val read   = sorted.filter {  it.isRead }

    Column(modifier = Modifier.fillMaxSize().background(PageBackground)) {

        // ── Header ────────────────────────────────────────────────────────
        Column(modifier = Modifier.background(CardLight).padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text("Notifications", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    if (state.unreadCount > 0) {
                        Text(
                            "${state.unreadCount} unread alert${if (state.unreadCount == 1) "" else "s"}",
                            style = MaterialTheme.typography.bodySmall, color = BrandBlue
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    NotificationBadge(count = state.unreadCount) {
                        Icon(Icons.Default.Notifications, "Notifications", tint = BrandBlue, modifier = Modifier.size(26.dp))
                    }
                    if (state.unreadCount > 0) {
                        TextButton(onClick = { viewModel.markAllRead() }) {
                            Text("Mark all read", color = BrandBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            if (state.unreadCount > 0) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(BrandBlue.copy(0.07f)).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Notifications, null, tint = BrandBlue, modifier = Modifier.size(14.dp))
                    Text(
                        "You have ${state.unreadCount} unread alert${if (state.unreadCount == 1) "" else "s"}",
                        color = BrandBlue, fontWeight = FontWeight.Medium, fontSize = 13.sp
                    )
                }
            }
        }

        HorizontalDivider(color = DividerColor)

        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = BrandBlue, strokeWidth = 2.dp, modifier = Modifier.size(30.dp))
                }
            }

            sorted.isEmpty() -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier         = Modifier.size(60.dp).clip(CircleShape).background(PageBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.NotificationsNone, null, tint = TextSecondary, modifier = Modifier.size(30.dp))
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("No notifications yet", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text("You're all caught up!", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier            = Modifier.fillMaxSize(),
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (unread.isNotEmpty()) {
                        item { SectionLabel("NEW  •  ${unread.size}") }
                        items(unread, key = { it.id }) { n ->
                            NotificationItem(n) {
                                when (n.type) {
                                    NotificationType.DEVICE_ON,
                                    NotificationType.DEVICE_OFF -> onNavigateToDevices()
                                    else                        -> onNavigateToDashboard()
                                }
                            }
                        }
                    }
                    if (read.isNotEmpty()) {
                        item { Spacer(Modifier.height(4.dp)); SectionLabel("EARLIER") }
                        items(read, key = { it.id }) { n ->
                            NotificationItem(n) {
                                when (n.type) {
                                    NotificationType.DEVICE_ON,
                                    NotificationType.DEVICE_OFF -> onNavigateToDevices()
                                    else                        -> onNavigateToDashboard()
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }
            }
        }
    }
}

// ── Section label ─────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text          = text,
        style         = MaterialTheme.typography.labelSmall,
        color         = TextSecondary,
        letterSpacing = 1.2.sp,
        modifier      = Modifier.padding(horizontal = 2.dp, vertical = 6.dp)
    )
}

// ── Notification Item ─────────────────────────────────────────────────────────

@Composable
fun NotificationItem(notif: AppNotification, onNavigate: () -> Unit) {
    data class NStyle(val icon: ImageVector, val color: Color, val badge: String, val hint: String)

    val s = when (notif.type) {
        NotificationType.RAIN_ALERT -> NStyle(Icons.Default.WaterDrop,   RainBlue,      "Rain Alert", "View Dashboard")
        NotificationType.LOW_WATER  -> NStyle(Icons.Default.WaterDrop,   WarningAmber,  "Low Water",  "View Dashboard")
        NotificationType.OVERFLOW   -> NStyle(Icons.Default.Warning,      ErrorRed,      "Overflow",   "View Dashboard")
        NotificationType.DEVICE_ON  -> NStyle(Icons.Default.CheckCircle, SuccessGreen,  "Device",     "View Devices")
        NotificationType.DEVICE_OFF -> NStyle(Icons.Default.PowerOff,    TextSecondary, "Device",     "View Devices")
    }

    val isUnread = !notif.isRead

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isUnread) s.color.copy(alpha = 0.08f) else CardLight)
            .border(1.dp, if (isUnread) s.color.copy(alpha = 0.35f) else DividerColor, RoundedCornerShape(14.dp))
            .clickable { onNavigate() }
    ) {
        if (isUnread) {
            Box(
                modifier = Modifier.width(4.dp).fillMaxHeight()
                    .background(s.color, RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
            )
        }

        Row(
            modifier              = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment     = Alignment.Top
        ) {
            Box(
                modifier         = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                    .background(s.color.copy(alpha = if (isUnread) 0.18f else 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(s.icon, null, tint = if (isUnread) s.color else s.color.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                .background(s.color.copy(alpha = if (isUnread) 0.15f else 0.07f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                s.badge,
                                color        = if (isUnread) s.color else s.color.copy(alpha = 0.6f),
                                fontSize     = 10.sp,
                                fontWeight   = FontWeight.Bold,
                                letterSpacing = 0.3.sp
                            )
                        }
                        if (isUnread) {
                            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(s.color))
                        }
                    }
                    Text(
                        formatNotifTime(notif),
                        style  = MaterialTheme.typography.bodySmall,
                        color  = if (isUnread) TextSecondary else TextSecondary.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }

                Spacer(Modifier.height(5.dp))
                Text(
                    notif.message,
                    style      = MaterialTheme.typography.bodySmall,
                    color      = if (isUnread) TextPrimary else TextSecondary.copy(alpha = 0.7f),
                    fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal,
                    lineHeight = 18.sp
                )

                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        s.hint,
                        color      = if (isUnread) s.color else s.color.copy(alpha = 0.5f),
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        Icons.Default.ChevronRight, null,
                        tint     = if (isUnread) s.color else s.color.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatNotifTime(notif: AppNotification): String {
    val date     = notif.timestamp?.toDate() ?: return ""
    val today    = Calendar.getInstance()
    val notifCal = Calendar.getInstance().also { it.time = date }
    val timeFmt  = SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
    return if (today.get(Calendar.DAY_OF_YEAR) == notifCal.get(Calendar.DAY_OF_YEAR)) timeFmt
    else "Yesterday $timeFmt"
}