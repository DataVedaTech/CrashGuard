package com.dataveda.crashguard.ui.test

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dataveda.crashguard.R
import com.dataveda.crashguard.core.emergency.AlertMessageBuilder
import com.dataveda.crashguard.core.sensors.SimulationLevel
import com.dataveda.crashguard.ui.CrashGuardViewModel
import com.dataveda.crashguard.ui.home.ModeToggleRow
import com.dataveda.crashguard.ui.theme.AccentIndigo
import com.dataveda.crashguard.ui.theme.AccentPurple
import com.dataveda.crashguard.ui.theme.EmergencyRed
import com.dataveda.crashguard.ui.theme.IconBadge
import com.dataveda.crashguard.ui.theme.SafeGreen
import com.dataveda.crashguard.ui.theme.SafetyTeal
import com.dataveda.crashguard.ui.theme.StatusPill
import com.dataveda.crashguard.ui.theme.WarningAmber
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestSosScreen(
    vm: CrashGuardViewModel,
    onBack: () -> Unit,
    onOpenIncident: (String) -> Unit,
) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsState()
    val contacts by vm.contacts.collectAsState()
    val profile by vm.profile.collectAsState()
    val telemetry by vm.sensorTelemetry.collectAsState()

    var armShakeCrash by remember { mutableStateOf(false) }

    LaunchedEffect(armShakeCrash, settings.testerShakeThresholdG) {
        vm.startHardwareSensorInspector(
            armShakeTest = armShakeCrash,
            shakeThresholdG = settings.testerShakeThresholdG.toDouble(),
        )
    }

    DisposableEffect(Unit) {
        onDispose { vm.stopHardwareSensorInspector() }
    }

    val selected = remember(contacts) {
        mutableStateMapOf<String, Boolean>().apply { contacts.forEach { put(it.id, true) } }
    }
    val sentToast = stringResource(R.string.test_sent_toast)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tester Mode & Sensor Lab", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    Box(modifier = Modifier.padding(end = 12.dp)) {
                        StatusPill(
                            text = if (settings.testerMode) "TESTER MODE" else "LIVE MODE",
                            color = if (settings.testerMode) AccentPurple else SafeGreen,
                            icon = if (settings.testerMode) Icons.Default.Science else Icons.Default.VerifiedUser,
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. Operating Mode Switcher (LIVE MODE vs TESTER MODE)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    1.5.dp,
                    if (settings.testerMode) AccentPurple.copy(alpha = 0.5f) else SafeGreen.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Operating Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    ModeToggleRow(
                        testerMode = settings.testerMode,
                        onSelectTesterMode = { vm.setTesterMode(it) },
                        darkSurface = false,
                    )
                    Text(
                        text = if (settings.testerMode) {
                            "TESTER MODE active: Real-time hardware sensor telemetry, physical shake-to-crash testing, simulated trip mode, and synthetic crash triggers are enabled."
                        } else {
                            "LIVE MODE active: Production road-safety thresholds (${settings.impactThresholdG}g impact + GPS >= 15 km/h) are enforced. Demo controls are hidden from the Home screen."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // 2. Real Hardware Sensor Live Telemetry & Physical Shake-to-Crash Test
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            IconBadge(Icons.Default.Sensors, tint = SafeGreen, size = 44.dp, iconSize = 24.dp)
                            Column {
                                StatusPill(text = "REAL HARDWARE SENSORS", color = SafeGreen)
                                Spacer(Modifier.height(2.dp))
                                Text("Live 9-Axis Sensor Telemetry", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                        IconButton(onClick = { vm.resetSensorPeaks() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset Peaks")
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        SensorMetricBox(
                            label = "Current G",
                            value = "%.2f g".format(Locale.US, telemetry.accelG),
                            accent = SafetyTeal,
                            modifier = Modifier.weight(1f),
                        )
                        SensorMetricBox(
                            label = "Peak G",
                            value = "%.2f g".format(Locale.US, telemetry.peakG),
                            accent = if (telemetry.peakG >= settings.testerShakeThresholdG) EmergencyRed else WarningAmber,
                            modifier = Modifier.weight(1f),
                        )
                        SensorMetricBox(
                            label = "Gyro Peak",
                            value = "%.0f °/s".format(Locale.US, telemetry.peakGyroDps),
                            accent = AccentIndigo,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    // Physical Shake-to-Crash Toggle
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = "Arm Physical Shake-to-Crash Test",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "Shake your phone above ${settings.testerShakeThresholdG}g to trigger a real hardware-sensor crash alert.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Switch(
                                    checked = armShakeCrash,
                                    onCheckedChange = {
                                        armShakeCrash = it
                                        if (it && !settings.testerMode) vm.setTesterMode(true)
                                    },
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(1.8f, 2.2f, 3.0f, 4.5f).forEach { g ->
                                    FilterChip(
                                        selected = settings.testerShakeThresholdG == g,
                                        onClick = { vm.setTesterShakeThreshold(g) },
                                        label = { Text("${g}g", fontWeight = FontWeight.Bold) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Safe Test SOS Alert Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        IconBadge(Icons.Default.VerifiedUser, tint = SafetyTeal, size = 44.dp, iconSize = 24.dp)
                        Column {
                            StatusPill(text = "SAFE SMS VALIDATION", color = SafeGreen)
                            Spacer(Modifier.height(2.dp))
                            Text(stringResource(R.string.test_sos_card_title), style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    Text(stringResource(R.string.test_sos_card_body), style = MaterialTheme.typography.bodyMedium)

                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = AlertMessageBuilder.test(profile, System.currentTimeMillis(), null),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color(0xFF93C5FD),
                            modifier = Modifier.padding(14.dp),
                        )
                    }

                    if (contacts.isEmpty()) {
                        Text(
                            text = stringResource(R.string.contacts_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        contacts.forEach { c ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = selected[c.id] ?: true,
                                    onCheckedChange = { selected[c.id] = it },
                                )
                                Text("${c.name} (${c.phone})", fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val chosen = contacts.filter { selected[it.id] != false }
                            vm.sendTestAlert(chosen) { id ->
                                Toast.makeText(context, sentToast, Toast.LENGTH_SHORT).show()
                                onOpenIncident(id)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SafetyTeal),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.test_send_button), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 4. Developer Sensor & Crash Simulation Panel (Spec 4.9)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        IconBadge(Icons.Default.Science, tint = AccentPurple, size = 44.dp, iconSize = 24.dp)
                        Column {
                            StatusPill(text = "SYNTHETIC SCENARIO TRIGGERS", color = AccentPurple)
                            Spacer(Modifier.height(2.dp))
                            Text(stringResource(R.string.sim_title), style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    Text(stringResource(R.string.sim_body), style = MaterialTheme.typography.bodyMedium)

                    SimulationScenarioCard(
                        icon = Icons.Default.Speed,
                        accent = AccentIndigo,
                        badge = "SCORE 30 • 4.7g",
                        title = stringResource(R.string.sim_low),
                        onClick = {
                            vm.simulate(SimulationLevel.LOW)
                            Toast.makeText(context, "Low-confidence event logged (score 30)", Toast.LENGTH_SHORT).show()
                        }
                    )

                    SimulationScenarioCard(
                        icon = Icons.Default.NotificationsActive,
                        accent = WarningAmber,
                        badge = "SCORE 50 • 5.2g",
                        title = stringResource(R.string.sim_medium),
                        onClick = {
                            vm.simulate(SimulationLevel.MEDIUM)
                            Toast.makeText(context, "Medium-confidence notification posted (score 50)", Toast.LENGTH_SHORT).show()
                        }
                    )

                    SimulationScenarioCard(
                        icon = Icons.Default.Warning,
                        accent = EmergencyRed,
                        badge = "SCORE 90 • 7.8g • ROLLOVER",
                        title = stringResource(R.string.sim_high),
                        highlighted = true,
                        onClick = { vm.simulate(SimulationLevel.HIGH) },
                    )

                    SimulationScenarioCard(
                        icon = Icons.Default.Timer,
                        accent = EmergencyRed,
                        badge = "10s FAST COUNTDOWN → AUTO-SOS",
                        title = stringResource(R.string.sim_unresponsive),
                        onClick = { vm.simulateUnresponsive() },
                    )

                    SimulationScenarioCard(
                        icon = Icons.Default.Emergency,
                        accent = SafeGreen,
                        badge = "INSTANT ESCALATION",
                        title = stringResource(R.string.sim_need_help),
                        onClick = { vm.simulateNeedHelp() },
                    )
                }
            }
        }
    }
}

@Composable
private fun SensorMetricBox(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = accent.copy(alpha = 0.10f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, color = accent, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun SimulationScenarioCard(
    icon: ImageVector,
    accent: Color,
    badge: String,
    title: String,
    highlighted: Boolean = false,
    onClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) EmergencyRed else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, if (highlighted) EmergencyRed else accent.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
            ) {
                IconBadge(
                    icon = icon,
                    tint = if (highlighted) Color.White else accent,
                    containerColor = if (highlighted) Color.White.copy(alpha = 0.2f) else accent.copy(alpha = 0.15f),
                    size = 42.dp,
                    iconSize = 22.dp,
                )
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = badge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (highlighted) Color(0xFFFFCDD2) else accent,
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (highlighted) Color.White else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (highlighted) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
