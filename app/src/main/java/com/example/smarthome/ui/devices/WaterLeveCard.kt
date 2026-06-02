package com.example.smarthome.ui.devices

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.example.smarthome.ui.theme.*

private data class WaterLevelConfig(
    val color: Color,
    val backgroundColor: Color,
    val icon: ImageVector,
    val statusLabel: String,
    val message: String
)

private fun resolveConfig(percentage: Int): WaterLevelConfig = when {
    percentage >= 80 -> WaterLevelConfig(
        color           = SuccessGreen,
        backgroundColor = SuccessGreen.copy(alpha = 0.08f),
        icon            = Icons.Default.Warning,
        statusLabel     = "Almost Full",
        message         = "⚠️ Tank is nearly full! Please be careful — it may overflow soon."
    )
    percentage <= 20 -> WaterLevelConfig(
        color           = Color(0xFFE53935),          // Red
        backgroundColor = Color(0xFFE53935).copy(alpha = 0.08f),
        icon            = Icons.Default.WaterDrop,
        statusLabel     = "Low",
        message         = "🚨 Water level is critically low! Refill the tank as soon as possible."
    )
    else -> WaterLevelConfig(
        color           = Color(0xFF1E88E5),          // Blue
        backgroundColor = Color(0xFF1E88E5).copy(alpha = 0.08f),
        icon            = Icons.Default.Water,
        statusLabel     = "Normal",
        message         = "✅ Water level is normal."
    )
}

// Main composable

@Composable
fun WaterLevelCard(percentage: Int) {
    val config = resolveConfig(percentage)

    // Animate color and fill changes smoothly
    val animatedColor by animateColorAsState(
        targetValue = config.color,
        animationSpec = tween(durationMillis = 600),
        label = "waterColor"
    )
    val animatedFill by animateFloatAsState(
        targetValue = percentage / 100f,
        animationSpec = tween(durationMillis = 800),
        label = "waterFill"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardLight),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Water, null,
                        tint = animatedColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "Water Tank",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                }
                // Status badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(config.backgroundColor)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        config.statusLabel,
                        color = animatedColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Percentage + bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Big percentage number
                Text(
                    "$percentage%",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = animatedColor
                )

                // Vertical tank visual
                Column(modifier = Modifier.weight(1f)) {
                    // Progress bar styled as a tank
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PageBackground)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedFill)
                                .clip(RoundedCornerShape(10.dp))
                                .background(animatedColor)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    // Min / Max labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0%", fontSize = 10.sp, color = TextSecondary)
                        Text("100%", fontSize = 10.sp, color = TextSecondary)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Threshold markers row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThresholdChip(label = "Low ≤20%",   color = Color(0xFFE53935), isActive = percentage <= 20)
                ThresholdChip(label = "Normal",      color = Color(0xFF1E88E5), isActive = percentage in 21..79)
                ThresholdChip(label = "High ≥80%",  color = SuccessGreen,      isActive = percentage >= 80)
            }

            // Alert message — only shown when NOT normal
            if (percentage !in 21..<80) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(config.backgroundColor)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        config.icon, null,
                        tint = animatedColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        config.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = animatedColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// Threshold chip

@Composable
private fun ThresholdChip(label: String, color: Color, isActive: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) color.copy(alpha = 0.15f) else PageBackground)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) color else TextSecondary
        )
    }
}