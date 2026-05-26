package com.example.smarthome.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarthome.ui.theme.*

// ── NavItem ───────────────────────────────────────────────────────────────────

data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val badgeCount: Int = 0
)

// ── SmartCard ─────────────────────────────────────────────────────────────────

@Composable
fun SmartCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

// ── Device Status Card ────────────────────────────────────────────────────────

@Composable
fun DeviceStatusCard(
    name: String,
    isOn: Boolean,
    subLabel: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val cardBg by animateColorAsState(
        targetValue = if (isOn) CardDark else CardLight,
        animationSpec = tween(300), label = "cardBg"
    )
    val textColor      = if (isOn) TextLight    else TextPrimary
    val subTextColor   = if (isOn) TextDimmed   else TextSecondary
    val iconBg         = if (isOn) Color.White.copy(alpha = 0.12f) else PageBackground

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = name,
                    tint = if (isOn) TextLight else BrandBlue,
                    modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.height(14.dp))
            Text(name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = textColor)
            Text(subLabel, style = MaterialTheme.typography.bodySmall, color = subTextColor)

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isOn) "ON" else "Off",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = if (isOn) TextLight else TextSecondary
                )
                // Compact toggle indicator dot
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (isOn) SuccessGreen else BorderLight)
                )
            }
        }
    }
}

// ── Water Level Card ──────────────────────────────────────────────────────────

@Composable
fun WaterLevelCard(percentage: Int, statusLabel: String, statusColor: Color) {
    val progress by animateFloatAsState(
        targetValue = percentage / 100f,
        animationSpec = tween(1000, easing = EaseOutCubic),
        label = "water"
    )
    SmartCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                        .background(statusColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.WaterDrop, null,
                        tint = statusColor, modifier = Modifier.size(18.dp))
                }
                Text("Water Level", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
            StatusChip(statusLabel, statusColor)
        }

        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("$percentage%", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = statusColor)
            Text("capacity", style = MaterialTheme.typography.bodySmall, color = TextSecondary,
                modifier = Modifier.padding(bottom = 5.dp))
        }

        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = statusColor,
            trackColor = DividerColor,
            strokeCap = StrokeCap.Round
        )
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Low <20%",      style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text("Overflow >90%", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

// ── Status / Glow Chip ────────────────────────────────────────────────────────

@Composable
fun StatusChip(label: String, color: Color, bgColor: Color = color.copy(alpha = 0.12f)) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold)
    }
}

@Composable
fun GlowChip(label: String, color: Color, bgAlpha: Float = 0.12f) {
    StatusChip(label = label, color = color, bgColor = color.copy(alpha = bgAlpha))
}

// ── Bottom Navigation Bar ─────────────────────────────────────────────────────

@Composable
fun SmartBottomBar(
    currentRoute: String?,
    items: List<NavItem>,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = BottomNavBg,
        tonalElevation = 0.dp
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = {
                    if (item.badgeCount > 0) {
                        BadgedBox(badge = {
                            Badge(containerColor = ErrorRed) { Text("${item.badgeCount}") }
                        }) {
                            Icon(item.icon, contentDescription = item.label,
                                modifier = Modifier.size(22.dp))
                        }
                    } else {
                        Icon(item.icon, contentDescription = item.label,
                            modifier = Modifier.size(22.dp))
                    }
                },
                label = {
                    Text(
                        item.label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = Color.White,
                    selectedTextColor   = Color.White,
                    unselectedIconColor = Color.White.copy(alpha = 0.45f),
                    unselectedTextColor = Color.White.copy(alpha = 0.45f),
                    indicatorColor      = Color.White.copy(alpha = 0.12f)
                )
            )
        }
    }
}

// ── Section Header ────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(
    title: String,
    badge: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge,
            color = TextPrimary, modifier = Modifier.weight(1f))
        if (badge != null) {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(20.dp))
                    .background(ErrorRed.copy(0.12f))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(badge, color = ErrorRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(6.dp))
        }
        action?.invoke()
    }
}

// ── Loading Card ──────────────────────────────────────────────────────────────

@Composable
fun LoadingCard() {
    SmartCard {
        Box(modifier = Modifier.fillMaxWidth().padding(24.dp),
            contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BrandBlue, strokeWidth = 2.dp,
                modifier = Modifier.size(28.dp))
        }
    }
}