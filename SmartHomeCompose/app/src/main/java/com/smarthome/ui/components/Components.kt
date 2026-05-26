// ui/components/Components.kt
package com.smarthome.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.ui.theme.*

// ── SmartHome Card (base surface) ─────────────────────────────────────────────

@Composable
fun SmartCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

// ── Device Status Card ─────────────────────────────────────────────────────────

@Composable
fun DeviceStatusCard(
    name: String,
    isOn: Boolean,
    subLabel: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isOn) Teal50 else MaterialTheme.colorScheme.surface,
        animationSpec = tween(400), label = "deviceBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isOn) Teal200 else MaterialTheme.colorScheme.outline,
        animationSpec = tween(400), label = "deviceBorder"
    )
    val statusColor = if (isOn) Teal700 else TextSecondary

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = name,
                    tint = if (isOn) Teal700 else TextSecondary,
                    modifier = Modifier.size(28.dp)
                )
                // Online indicator dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isOn) SuccessGreen else BorderLight)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (isOn) "ON" else "OFF",
                color = statusColor,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = subLabel,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

// ── Water Level Card ──────────────────────────────────────────────────────────

@Composable
fun WaterLevelCard(percentage: Int, statusLabel: String, statusColor: Color) {
    val progress by animateFloatAsState(
        targetValue = percentage / 100f,
        animationSpec = tween(800), label = "waterProgress"
    )

    SmartCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(RainBlueBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.WaterDrop, "water", tint = RainBlue, modifier = Modifier.size(20.dp))
                }
                Text("Water Level", style = MaterialTheme.typography.titleMedium)
            }
            StatusChip(statusLabel, statusColor)
        }

        Spacer(Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("$percentage%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Normal · Last updated just now",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = Teal700,
            trackColor = Teal50,
            strokeCap = StrokeCap.Round
        )
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Low alert <20%", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
            Text("Overflow >90%", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
        }
    }
}

// ── Status Chip ───────────────────────────────────────────────────────────────

@Composable
fun StatusChip(label: String, color: Color, bgColor: Color = color.copy(alpha = 0.12f)) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}

// ── Bottom Navigation Bar ─────────────────────────────────────────────────────

data class NavItem(val route: String, val label: String, val icon: ImageVector, val badgeCount: Int = 0)

@Composable
fun SmartBottomBar(
    currentRoute: String?,
    items: List<NavItem>,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) },
                icon = {
                    if (item.badgeCount > 0) {
                        BadgedBox(badge = { Badge { Text("${item.badgeCount}") } }) {
                            Icon(item.icon, contentDescription = item.label)
                        }
                    } else {
                        Icon(item.icon, contentDescription = item.label)
                    }
                },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Teal700,
                    selectedTextColor = Teal700,
                    indicatorColor = Teal50
                )
            )
        }
    }
}

// ── Section Header ────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, badge: String? = null, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (badge != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(ErrorRed)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(badge, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
        }
        action?.invoke()
    }
}

// ── Loading Indicator ─────────────────────────────────────────────────────────

@Composable
fun LoadingCard() {
    SmartCard {
        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Teal700)
        }
    }
}
