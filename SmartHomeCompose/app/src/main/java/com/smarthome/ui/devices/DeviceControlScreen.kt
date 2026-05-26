// ui/devices/DeviceControlScreen.kt
package com.smarthome.ui.devices

import androidx.compose.foundation.*
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.smarthome.data.model.Device
import com.smarthome.ui.components.SmartCard
import com.smarthome.ui.components.StatusChip
import com.smarthome.ui.theme.*
import com.smarthome.viewmodel.DeviceControlViewModel

@Composable
fun DeviceControlScreen(viewModel: DeviceControlViewModel = hiltViewModel()) {
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
            Column {
                Text("Device Control", style = MaterialTheme.typography.headlineMedium)
                Text("Smart Appliance Control via ESP32", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            StatusChip("● Live", SuccessGreen, SuccessGreen.copy(alpha = 0.1f))
        }

        // ── How it works info card ────────────────────────────────────────
        SmartCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Info, null, tint = Teal700, modifier = Modifier.size(16.dp))
                Text("How it works", color = Teal700, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(10.dp))
            HowItWorksRow("📱", "Your phone sends command via internet")
            HowItWorksRow("🔥", "Firebase database updates instantly")
            HowItWorksRow("🔌", "ESP32 reads the update in real-time")
            HowItWorksRow("⚡", "Relay switches the device ON or OFF")
        }

        // ── Device Controls ───────────────────────────────────────────────
        Text("Device Controls", style = MaterialTheme.typography.titleLarge)

        DeviceToggleCard(
            device = state.fan,
            name = "Fan",
            icon = Icons.Default.Air,
            relayNote = "Relay module switches fan via ESP32 GPIO pin",
            onToggle = { viewModel.toggleFan(it) }
        )

        DeviceToggleCard(
            device = state.ledLight,
            name = "LED Light",
            icon = Icons.Default.LightMode,
            relayNote = "Relay module switches LED via ESP32 GPIO pin",
            onToggle = { viewModel.toggleLight(it) }
        )

        // ── Real-time Status ──────────────────────────────────────────────
        SmartCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Sync, null, tint = Teal700, modifier = Modifier.size(16.dp))
                    Text("Real-time Status", fontWeight = FontWeight.SemiBold)
                }
                Text("Auto-updates", color = Teal700, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStatusCard(
                    label = "Fan",
                    status = if (state.fan.isOn) "ON" else "OFF",
                    isOn = state.fan.isOn,
                    icon = Icons.Default.Air,
                    modifier = Modifier.weight(1f)
                )
                MiniStatusCard(
                    label = "LED Light",
                    status = if (state.ledLight.isOn) "ON" else "OFF",
                    isOn = state.ledLight.isOn,
                    icon = Icons.Default.LightMode,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "The app always shows you if the fan or light is currently ON or OFF. Status updates automatically whenever there is a change.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        // ── Simulation Controls (remove once ESP32 is connected) ──────────
        SimulationPanel(viewModel = viewModel)

        Spacer(Modifier.height(16.dp))
    }
}

// ── Device Toggle Card ────────────────────────────────────────────────────────

@Composable
fun DeviceToggleCard(
    device: Device,
    name: String,
    icon: ImageVector,
    relayNote: String,
    onToggle: (Boolean) -> Unit
) {
    val cardBg = if (device.isOn) Teal50 else MaterialTheme.colorScheme.surface
    val borderColor = if (device.isOn) Teal200 else MaterialTheme.colorScheme.outline

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (device.isOn) Teal100 else MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, tint = if (device.isOn) Teal700 else TextSecondary, modifier = Modifier.size(28.dp))
                    }
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(name, style = MaterialTheme.typography.titleMedium)
                            StatusChip(
                                if (device.isOn) "ON" else "OFF",
                                if (device.isOn) SuccessGreen else TextSecondary
                            )
                        }
                        Text(
                            "Changed ${formatTimestamp(device.lastChanged)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                Switch(
                    checked = device.isOn,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Teal700,
                        uncheckedTrackColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ElectricBolt, null, tint = Teal700, modifier = Modifier.size(14.dp))
                    Text(relayNote, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Text("${device.powerWatts}W", color = Teal700, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

// ── Mini Status Card ──────────────────────────────────────────────────────────

@Composable
fun MiniStatusCard(label: String, status: String, isOn: Boolean, icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isOn) Teal50 else MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Column {
            Icon(icon, null, tint = if (isOn) Teal700 else TextSecondary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text(status, color = if (isOn) Teal700 else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

// ── How it works row ──────────────────────────────────────────────────────────

@Composable
fun HowItWorksRow(emoji: String, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 16.sp)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

// ── Simulation Panel ──────────────────────────────────────────────────────────

@Composable
fun SimulationPanel(viewModel: DeviceControlViewModel) {
    var waterSlider by remember { mutableFloatStateOf(71f) }

    SmartCard {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Science, null, tint = WarningAmber, modifier = Modifier.size(16.dp))
            Text("Simulation Controls", fontWeight = FontWeight.SemiBold, color = WarningAmber)
            Text("(remove after ESP32 is connected)", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
        }

        Spacer(Modifier.height(14.dp))
        Text("Water Level: ${waterSlider.toInt()}%", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = waterSlider,
            onValueChange = { waterSlider = it },
            onValueChangeFinished = { viewModel.simulateWaterLevel(waterSlider.toInt()) },
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(thumbColor = Teal700, activeTrackColor = Teal700)
        )

        Spacer(Modifier.height(8.dp))
        Text("Rain Sensor", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { viewModel.simulateRain(true, "moderate") },
                colors = ButtonDefaults.buttonColors(containerColor = RainBlue),
                shape = RoundedCornerShape(8.dp)
            ) { Text("Rain ON") }
            OutlinedButton(
                onClick = { viewModel.simulateRain(false) },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, BorderLight)
            ) { Text("Rain OFF", color = TextSecondary) }
        }
    }
}

private fun formatTimestamp(ts: Long): String {
    if (ts == 0L) return "unknown"
    val diff = System.currentTimeMillis() - ts
    return when {
        diff < 60_000 -> "just now"
        diff < 3_600_000 -> "${diff / 60_000} min ago"
        else -> "${diff / 3_600_000}h ago"
    }
}
