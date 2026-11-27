package com.dataveda.crashguard.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dataveda.crashguard.R
import com.dataveda.crashguard.core.emergency.AlertMessageBuilder
import com.dataveda.crashguard.core.emergency.IncidentReportExporter
import com.dataveda.crashguard.core.emergency.toFix
import com.dataveda.crashguard.data.local.IncidentEntity
import com.dataveda.crashguard.data.local.TripEntity
import com.dataveda.crashguard.domain.models.IncidentStatus
import com.dataveda.crashguard.ui.CrashGuardViewModel
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
fun HistoryScreen(
    vm: CrashGuardViewModel,
    onBack: () -> Unit,
    onSelectIncident: (String) -> Unit,
) {
    val incidents by vm.incidents.collectAsState()
    val trips by vm.trips.collectAsState()
    var tab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    text = { Text("${stringResource(R.string.history_tab_incidents)} (${incidents.size})", fontWeight = FontWeight.Bold) },
                )
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    text = { Text("${stringResource(R.string.history_tab_trips)} (${trips.size})", fontWeight = FontWeight.Bold) },
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (tab == 0) {
                    if (incidents.isEmpty()) {
                        Text(
                            stringResource(R.string.history_empty_incidents),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        incidents.forEach { inc ->
                            IncidentSummaryCard(inc) { onSelectIncident(inc.id) }
                        }
                    }
                } else {
                    if (trips.isEmpty()) {
                        Text(
                            stringResource(R.string.history_empty_trips),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        trips.forEach { trip -> TripSummaryCard(trip) }
                    }
                }
            }
        }
    }
}

@Composable
private fun IncidentSummaryCard(incident: IncidentEntity, onClick: () -> Unit) {
    val accent = when {
        incident.isTest -> AccentPurple
        incident.status == IncidentStatus.ACTIVE -> EmergencyRed
        incident.status == IncidentStatus.FALSE_ALARM -> WarningAmber
        else -> SafeGreen
    }
    val icon = when {
        incident.isTest -> Icons.Default.Science
        incident.status == IncidentStatus.RESOLVED -> Icons.Default.VerifiedUser
        else -> Icons.Default.Emergency
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(18.dp),
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
                IconBadge(icon = icon, tint = accent, size = 46.dp, iconSize = 24.dp)
                Spacer(Modifier.width(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val badge = if (incident.isTest) "TEST ALERT" else incident.source.name
                        Text(badge, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        StatusPill(text = incident.status.name, color = accent)
                    }
                    Text(
                        text = AlertMessageBuilder.timestamp(incident.detectedAt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Score: ${incident.crashConfidence}/100 • Impact: ${
                            "%.1f".format(Locale.US, incident.impactG)
                        }g • Response: ${incident.userResponse.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun TripSummaryCard(trip: TripEntity) {
    val durationMin = ((trip.endedAt ?: System.currentTimeMillis()) - trip.startedAt).coerceAtLeast(0) / 60_000L
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            IconBadge(Icons.Default.DirectionsCar, tint = SafetyTeal, size = 46.dp, iconSize = 24.dp)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val tag = when {
                        trip.simulated -> "Simulated Trip"
                        trip.manual -> "Manual Driving Mode"
                        else -> "Auto-detected Trip"
                    }
                    Text(tag, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    StatusPill(text = trip.status.name, color = SafetyTeal)
                }
                Text(
                    text = AlertMessageBuilder.timestamp(trip.startedAt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Duration: ${durationMin}m • Distance: ${"%.2f".format(Locale.US, trip.distanceKm)} km • Max: ${
                        "%.1f".format(Locale.US, trip.maxSpeedKmh)
                    } km/h",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Crash alerts: ${trip.crashAlertCount} • False alarms: ${trip.falseAlarmCount} • Incidents: ${trip.incidentCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentDetailScreen(
    incidentId: String,
    vm: CrashGuardViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val incident by vm.observeIncident(incidentId).collectAsState(initial = null)
    val deliveries by vm.observeDeliveries(incidentId).collectAsState(initial = emptyList())
    val profile by vm.profile.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.incident_report_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val inc = incident
            if (inc == null) {
                Text("Loading…")
            } else {
                val reportText = IncidentReportExporter.toPlainText(inc, profile, deliveries)

                // Telemetry Summary Strip
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                IconBadge(Icons.Default.Sensors, tint = EmergencyRed, size = 42.dp, iconSize = 22.dp)
                                Column {
                                    Text("Incident Telemetry Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(AlertMessageBuilder.timestamp(inc.detectedAt), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            StatusPill(text = inc.status.name, color = SafeGreen)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            MetricMiniCard("Confidence", "${inc.crashConfidence}/100", EmergencyRed, Modifier.weight(1f))
                            MetricMiniCard("Impact", "${"%.2f".format(Locale.US, inc.impactG)} g", WarningAmber, Modifier.weight(1f))
                            MetricMiniCard("Speed Prior", "${"%.0f".format(Locale.US, inc.speedBeforeKmh)} km/h", SafetyTeal, Modifier.weight(1f))
                        }
                        inc.toFix()?.let { fix ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.GpsFixed, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(16.dp))
                                Text(fix.mapsLink, style = MaterialTheme.typography.bodyMedium, color = AccentIndigo, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Export Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { vm.shareIncidentText(context, inc, deliveries) },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SafetyTeal),
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.incident_share_text), fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { vm.shareIncidentJson(context, inc, deliveries) },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.incident_share_json), fontWeight = FontWeight.Bold)
                    }
                }

                // Structured Raw Report Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF5EEAD4), modifier = Modifier.size(18.dp))
                            Text("HANDOVER REPORT PAYLOAD", color = Color(0xFF5EEAD4), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Text(
                            text = reportText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = Color(0xFFE2E8F0),
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun MetricMiniCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.10f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, color = color, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
