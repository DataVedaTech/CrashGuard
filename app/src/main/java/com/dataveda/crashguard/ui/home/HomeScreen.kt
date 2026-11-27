package com.dataveda.crashguard.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dataveda.crashguard.R
import com.dataveda.crashguard.core.emergency.EmergencyCaller
import com.dataveda.crashguard.core.emergency.EmergencyState
import com.dataveda.crashguard.core.permissions.AppPermission
import com.dataveda.crashguard.core.permissions.PermissionUtils
import com.dataveda.crashguard.core.sensors.SimulationLevel
import com.dataveda.crashguard.core.trip.TripMode
import com.dataveda.crashguard.core.trip.TripState
import com.dataveda.crashguard.domain.models.TripRole
import com.dataveda.crashguard.ui.CrashGuardViewModel
import com.dataveda.crashguard.ui.theme.AccentIndigo
import com.dataveda.crashguard.ui.theme.AccentPurple
import com.dataveda.crashguard.ui.theme.EmergencyGradient
import com.dataveda.crashguard.ui.theme.EmergencyRed
import com.dataveda.crashguard.ui.theme.HeroArmedGradient
import com.dataveda.crashguard.ui.theme.HeroInactiveGradient
import com.dataveda.crashguard.ui.theme.IconBadge
import com.dataveda.crashguard.ui.theme.SafeGreen
import com.dataveda.crashguard.ui.theme.SafetyTeal
import com.dataveda.crashguard.ui.theme.StatusPill
import com.dataveda.crashguard.ui.theme.TelemetryTile
import com.dataveda.crashguard.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: CrashGuardViewModel,
    onOpenContacts: () -> Unit,
    onOpenTestSos: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsState()
    val contacts by vm.contacts.collectAsState()
    val tripState by vm.tripState.collectAsState()
    val emergencyState by vm.emergencyState.collectAsState()
    val incidents by vm.incidents.collectAsState()

    val missingCritical = AppPermission.entries.any { it.critical && it.applicable && !it.isGranted(context) }
    val batteryRestricted = !vm.isIgnoringBatteryOptimizations()

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SafetyTeal),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                                StatusPill(
                                    text = if (settings.protectionEnabled) "ARMED" else "OFF",
                                    color = if (settings.protectionEnabled) SafeGreen else WarningAmber,
                                )
                            }
                            Text(
                                stringResource(R.string.app_tagline),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                },
                actions = {
                    Surface(
                        onClick = { EmergencyCaller.call112(context) },
                        color = EmergencyRed,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("112", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        }
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
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (emergencyState !is EmergencyState.Idle) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = EmergencyRed, contentColor = Color.White),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().clickable { vm.openEmergencyScreen() },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Emergency, contentDescription = null, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.home_active_emergency_banner),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
                    }
                }
            }

            // 1. HERO COCKPIT CARD: Crash Protection & Live Telemetry
            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (settings.protectionEnabled) HeroArmedGradient else HeroInactiveGradient)
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawCircle(
                                            color = Color.White.copy(alpha = 0.12f),
                                            radius = size.minDimension / 2f,
                                        )
                                        drawCircle(
                                            color = Color(0xFF5EEAD4).copy(alpha = 0.35f),
                                            radius = size.minDimension / 2f,
                                            style = Stroke(width = 2.dp.toPx()),
                                        )
                                    }
                                    Icon(
                                        imageVector = if (settings.protectionEnabled) Icons.Default.VerifiedUser else Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = if (settings.protectionEnabled) Color(0xFF5EEAD4) else Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                                Column {
                                    Text(
                                        text = if (settings.protectionEnabled) stringResource(R.string.home_protection_on)
                                        else stringResource(R.string.home_protection_off),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White,
                                    )
                                    Text(
                                        text = if (settings.protectionEnabled) stringResource(R.string.home_protection_sub_on)
                                        else stringResource(R.string.home_protection_sub_off),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.80f),
                                    )
                                }
                            }
                            Switch(
                                checked = settings.protectionEnabled,
                                onCheckedChange = { vm.setProtectionEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = SafeGreen,
                                ),
                            )
                        }

                        val speedVal = when (val t = tripState) {
                            is TripState.Active -> "%.0f km/h".format(java.util.Locale.US, t.speedKmh)
                            is TripState.Confirming -> "%.0f km/h".format(java.util.Locale.US, t.speedKmh)
                            TripState.Idle -> "0 km/h"
                        }
                        val tripLabel = when (val t = tripState) {
                            is TripState.Active -> "%.2f km".format(java.util.Locale.US, t.distanceKm)
                            is TripState.Confirming -> "Confirming"
                            TripState.Idle -> "Standby"
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            TelemetryTile(
                                icon = Icons.Default.Speed,
                                label = "Live Speed",
                                value = speedVal,
                                modifier = Modifier.weight(1f),
                            )
                            TelemetryTile(
                                icon = Icons.Default.DirectionsCar,
                                label = "Trip Status",
                                value = tripLabel,
                                modifier = Modifier.weight(1f),
                            )
                            TelemetryTile(
                                icon = Icons.Default.Sensors,
                                label = "Sensors",
                                value = if (vm.hasAccelerometer) "9-Axis OK" else "Limited",
                                modifier = Modifier.weight(1f),
                            )
                        }

                        if (tripState is TripState.Active) {
                            val active = tripState as TripState.Active
                            Surface(
                                color = Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Color(0xFF5EEAD4),
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Text(
                                            text = stringResource(R.string.role_question),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        AssistChip(
                                            onClick = { vm.setTripRole(TripRole.DRIVER) },
                                            label = { Text(stringResource(R.string.role_driver), color = Color.White) },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = if (active.role == TripRole.DRIVER) SafeGreen else Color.White.copy(alpha = 0.14f)
                                            ),
                                        )
                                        AssistChip(
                                            onClick = { vm.setTripRole(TripRole.PASSENGER) },
                                            label = { Text(stringResource(R.string.role_passenger), color = Color.White) },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = if (active.role == TripRole.PASSENGER) WarningAmber else Color.White.copy(alpha = 0.14f)
                                            ),
                                        )
                                    }
                                    if (active.role == TripRole.PASSENGER) {
                                        Text(
                                            text = stringResource(R.string.home_passenger_note),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFFFDE68A),
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            if (tripState is TripState.Idle) {
                                Button(
                                    onClick = { vm.startTrip(TripMode.MANUAL) },
                                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color(0xFF063B47),
                                    ),
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        stringResource(R.string.home_start_manual_trip),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                    )
                                }
                                OutlinedButton(
                                    onClick = { vm.startTrip(TripMode.SIMULATED) },
                                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.55f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                ) {
                                    Icon(Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        stringResource(R.string.home_start_sim_trip),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { vm.stopTrip() },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = EmergencyRed,
                                    ),
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.home_stop_trip), fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }

            if (missingCritical) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onOpenPrivacy() },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            IconBadge(Icons.Default.Warning, tint = EmergencyRed, size = 38.dp, iconSize = 20.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.home_missing_perm_warning),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            } else if (batteryRestricted) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            IconBadge(Icons.Default.BatteryAlert, tint = WarningAmber, size = 38.dp, iconSize = 20.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.home_battery_warning),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        TextButton(
                            onClick = {
                                runCatching { context.startActivity(PermissionUtils.batteryOptimizationIntent(context)) }
                            }
                        ) {
                            Text(stringResource(R.string.home_battery_fix), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (settings.developerMode) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Science, contentDescription = null, tint = AccentPurple)
                            Text(stringResource(R.string.sim_title), style = MaterialTheme.typography.titleMedium)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = { vm.simulate(SimulationLevel.LOW) }, modifier = Modifier.weight(1f)) {
                                Text("Low (30)")
                            }
                            OutlinedButton(onClick = { vm.simulate(SimulationLevel.MEDIUM) }, modifier = Modifier.weight(1f)) {
                                Text("Med (50)")
                            }
                            Button(
                                onClick = { vm.simulate(SimulationLevel.HIGH) },
                                colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Crash (90)")
                            }
                        }
                    }
                }
            }

            HomeFeatureCard(
                icon = Icons.Default.Contacts,
                accentColor = SafetyTeal,
                title = stringResource(R.string.home_contacts_count, contacts.size),
                subtitle = stringResource(R.string.home_contacts_sub),
                badgeText = "${contacts.size}/5 Ready",
                badgeColor = if (contacts.isNotEmpty()) SafeGreen else WarningAmber,
                onClick = onOpenContacts,
            )

            HomeFeatureCard(
                icon = Icons.Default.Science,
                accentColor = AccentPurple,
                title = stringResource(R.string.home_test_sos),
                subtitle = stringResource(R.string.home_test_sos_sub),
                badgeText = "Demo Lab",
                badgeColor = AccentPurple,
                onClick = onOpenTestSos,
            )

            HomeFeatureCard(
                icon = Icons.Default.MedicalInformation,
                accentColor = EmergencyRed,
                title = stringResource(R.string.home_profile),
                subtitle = stringResource(R.string.home_profile_sub),
                badgeText = "AES-256",
                badgeColor = SafeGreen,
                onClick = onOpenProfile,
            )

            HomeFeatureCard(
                icon = Icons.Default.History,
                accentColor = AccentIndigo,
                title = stringResource(R.string.home_history),
                subtitle = stringResource(R.string.home_history_sub),
                badgeText = "${incidents.size} Logs",
                badgeColor = AccentIndigo,
                onClick = onOpenHistory,
            )

            HomeFeatureCard(
                icon = Icons.Default.PrivacyTip,
                accentColor = SafeGreen,
                title = stringResource(R.string.home_privacy),
                subtitle = stringResource(R.string.home_privacy_sub),
                badgeText = "EN • हिं • मरा",
                badgeColor = SafetyTeal,
                onClick = onOpenPrivacy,
            )

            Card(
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth().clickable { vm.triggerManualSos() },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(EmergencyGradient)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.Emergency,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.home_manual_sos),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                )
                                Text(
                                    text = "Instant alert + live GPS link to trusted contacts",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.85f),
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp).padding(top = 2.dp),
                    )
                    Text(
                        text = stringResource(R.string.disclaimer_primary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeFeatureCard(
    icon: ImageVector,
    accentColor: Color,
    title: String,
    subtitle: String,
    badgeText: String,
    badgeColor: Color,
    onClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
            ) {
                IconBadge(icon = icon, tint = accentColor, size = 48.dp, iconSize = 24.dp)
                Spacer(Modifier.width(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                StatusPill(text = badgeText, color = badgeColor)
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
