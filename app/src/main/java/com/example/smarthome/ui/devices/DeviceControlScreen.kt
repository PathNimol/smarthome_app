package com.example.smarthome.ui.devices

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smarthome.data.model.Device
import com.example.smarthome.ui.components.GlowChip
import com.example.smarthome.ui.components.SmartCard
import com.example.smarthome.ui.theme.*
import com.example.smarthome.viewmodel.DeviceControlViewModel

@Composable
fun DeviceControlScreen(viewModel: DeviceControlViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    // Show error snackbar if there's an error
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = PageBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
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
                        "Device Control",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        "Smart Appliance Control via ESP32",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                // Live / Loading indicator
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (state.isLoading) WarningAmber.copy(0.1f)
                            else SuccessGreen.copy(0.1f)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (state.isLoading) WarningAmber else SuccessGreen)
                    )
                    Text(
                        if (state.isLoading) "Connecting..." else "Live",
                        color = if (state.isLoading) WarningAmber else SuccessGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            HorizontalDivider(color = DividerColor)

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // How it works card
                SmartCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        Icon(Icons.Default.Info, null, tint = BrandBlue, modifier = Modifier.size(15.dp))
                        Text(
                            "How it works",
                            color = BrandBlue,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                    listOf(
                        Icons.Default.Smartphone   to "Your phone sends command via internet",
                        Icons.Default.CloudSync    to "Firebase database updates instantly",
                        Icons.Default.Memory       to "ESP32 reads the update in real-time",
                        Icons.Default.ElectricBolt to "Relay switches the device ON or OFF"
                    ).forEach { (icon, text) ->
                        Row(
                            modifier = Modifier.padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, null, tint = BrandBlue.copy(0.6f), modifier = Modifier.size(14.dp))
                            Text(text, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }

                // Device Controls
                Text("Device Controls", style = MaterialTheme.typography.titleMedium, color = TextPrimary)

                // LED Light — maps to Firebase: smarthome/devices/led_light
                DeviceToggleCard(
                    device    = state.ledLight,
                    name      = "LED Light",
                    icon      = Icons.Default.LightMode,
                    relayNote = "Relay switches LED via ESP32 GPIO pin",
                    onToggle  = { viewModel.toggleLight(it) }
                )

                // Fan — maps to Firebase: smarthome/devices/fan
                DeviceToggleCard(
                    device    = state.fan,
                    name      = "Fan",
                    icon      = Icons.Default.Air,
                    relayNote = "Relay switches fan via ESP32 GPIO pin",
                    onToggle  = { viewModel.toggleFan(it) }
                )

                // Real-time Status
                SmartCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Sync, null, tint = BrandBlue, modifier = Modifier.size(14.dp))
                            Text(
                                "Real-time Status",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                        }
                        Text(
                            "Auto-updates",
                            color = BrandBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Fan status — reads from state.fan.isOn (computed from "ON"/"OFF" string)
                        MiniStatusCard(
                            label    = "Fan",
                            status   = state.fan.state,      // shows "ON" or "OFF" from Firebase
                            isOn     = state.fan.isOn,
                            icon     = Icons.Default.Air,
                            modifier = Modifier.weight(1f)
                        )
                        // LED status — reads from state.ledLight.isOn
                        MiniStatusCard(
                            label    = "LED Light",
                            status   = state.ledLight.state, // shows "ON" or "OFF" from Firebase
                            isOn     = state.ledLight.isOn,
                            icon     = Icons.Default.LightMode,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "The app always shows if the fan or light is ON or OFF. Status updates automatically whenever there is a change.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                // Water Tank Status
                // percentage from Firebase: smarthome/sensors/water_level/percentage
                // >= 80 → Green  + overflow warning
                // 21-79 → Blue   + normal message
                // <= 20 → Red    + critically low warning
                Text("Water Tank", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                WaterLevelCard(percentage = state.waterLevel.percentage)

                // Simulation Controls
                SimulationPanel(
                    // Pass current water level from Firebase so slider stays in sync
                    currentWaterLevel = state.waterLevel.percentage,
                    // Pass current rain state from Firebase so buttons reflect real DB state
                    currentIsRaining  = state.rain.isRaining,
                    viewModel         = viewModel
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// Device Toggle Card

@Composable
fun DeviceToggleCard(
    device: Device,
    name: String,
    icon: ImageVector,
    relayNote: String,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardLight),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (device.isOn) CardDark else PageBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon, null,
                            tint = if (device.isOn) Color.White else BrandBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                name,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                            // GlowChip shows the actual Firebase state string "ON"/"OFF"
                            GlowChip(
                                label = device.state,
                                color = if (device.isOn) SuccessGreen else TextSecondary
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
                    checked = device.isOn,       // isOn computed from "ON"/"OFF" string
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor   = Color.White,
                        checkedTrackColor   = SuccessGreen,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = BorderLight
                    )
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(PageBackground)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ElectricBolt, null,
                        tint = WarningAmber,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(relayNote, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // controlledBy: "manual" or "auto" from Firebase
                    Text(
                        device.controlledBy,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        "${device.powerWatts}W",
                        color = if (device.isOn) WarningAmber else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

// Mini Status Card

@Composable
fun MiniStatusCard(
    label: String,
    status: String,   // "ON" or "OFF" string from Firebase
    isOn: Boolean,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isOn) CardDark else PageBackground)
            .padding(14.dp)
    ) {
        Column {
            Icon(
                icon, null,
                tint = if (isOn) Color.White else BrandBlue,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = if (isOn) TextDimmed else TextSecondary
            )
            Text(
                status,
                color = if (isOn) Color.White else TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

//  Simulation Panel

@Composable
fun SimulationPanel(
    currentWaterLevel: Int,       // driven by Firebase: smarthome/sensors/water_level/percentage
    currentIsRaining: Boolean,    // driven by Firebase: smarthome/sensors/rain/isRaining
    viewModel: DeviceControlViewModel
) {
    // Slider initializes from Firebase value; remember(currentWaterLevel) resets
    // if Firebase changes externally (e.g. from another device / ESP32)
    var waterSlider by remember(currentWaterLevel) { mutableFloatStateOf(currentWaterLevel.toFloat()) }

    SmartCard {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Science, null, tint = WarningAmber, modifier = Modifier.size(14.dp))
            Text(
                "Simulation Controls",
                fontWeight = FontWeight.SemiBold,
                color = WarningAmber,
                fontSize = 13.sp
            )
        }
        Text(
            "Remove after ESP32 is connected",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )

        // Water level simulator
        // Writes to: smarthome/sensors/water_level/percentage
        Text(
            "Water Level: ${waterSlider.toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
        Slider(
            value = waterSlider,
            onValueChange = { waterSlider = it },
            onValueChangeFinished = { viewModel.simulateWaterLevel(waterSlider.toInt()) },
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = BrandBlue,
                activeTrackColor = BrandBlue,
                inactiveTrackColor = DividerColor
            )
        )

        Spacer(Modifier.height(6.dp))

        // Rain sensor simulator
        // Writes to: smarthome/sensors/rain/isRaining + active + intensity + detectedAt
        // Button active state is driven by Firebase (currentIsRaining), not local state.
        // This means any external change (ESP32, other client) is reflected automatically.
        Text(
            "Rain Sensor",
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {

            // Rain ON
            // Filled blue  = Firebase isRaining is true
            // Outlined     = Firebase isRaining is false
            if (currentIsRaining) {
                Button(
                    onClick = { viewModel.simulateRain(true, "moderate") },
                    colors = ButtonDefaults.buttonColors(containerColor = RainBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Umbrella, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Rain ON", color = Color.White, fontSize = 13.sp)
                }
            } else {
                OutlinedButton(
                    onClick = { viewModel.simulateRain(true, "moderate") },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, DividerColor)
                ) {
                    Icon(Icons.Default.Umbrella, null, tint = RainBlue, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Rain ON", color = TextSecondary, fontSize = 13.sp)
                }
            }

            // Rain OFF
            // Filled amber = Firebase isRaining is false  (OFF is the active state)
            // Outlined     = Firebase isRaining is true
            if (!currentIsRaining) {
                Button(
                    onClick = { viewModel.simulateRain(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.WbSunny, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Rain OFF", color = Color.White, fontSize = 13.sp)
                }
            } else {
                OutlinedButton(
                    onClick = { viewModel.simulateRain(false) },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, DividerColor)
                ) {
                    Icon(Icons.Default.WbSunny, null, tint = WarningAmber, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Rain OFF", color = TextSecondary, fontSize = 13.sp)
                }
            }
        }
    }
}

// Helpers

private fun formatTimestamp(ts: Long): String {
    if (ts == 0L) return "unknown"
    val diff = System.currentTimeMillis() - ts
    return when {
        diff < 60_000    -> "just now"
        diff < 3_600_000 -> "${diff / 60_000} min ago"
        else             -> "${diff / 3_600_000}h ago"
    }
}