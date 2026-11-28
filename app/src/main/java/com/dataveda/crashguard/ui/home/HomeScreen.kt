package com.dataveda.crashguard.ui.home

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import com.dataveda.crashguard.ui.theme.HeroTealGradient
import com.dataveda.crashguard.ui.theme.IconBadge
import com.dataveda.crashguard.ui.theme.SafeGreen
import com.dataveda.crashguard.ui.theme.SafetyTeal
import com.dataveda.crashguard.ui.theme.StatusPill
import com.dataveda.crashguard.ui.theme.TelemetryTile
import com.dataveda.crashguard.ui.theme.WarningAmber
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: CrashGuardViewModel,
    openDrawerOnLaunch: Boolean = false,
    onOpenContacts: () -> Unit,
    onOpenTestSos: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsState()
    val contacts by vm.contacts.collectAsState()
    val profile by vm.profile.collectAsState()
    val tripState by vm.tripState.collectAsState()
    val emergencyState by vm.emergencyState.collectAsState()
    val incidents by vm.incidents.collectAsState()
    val trips by vm.trips.collectAsState()
    val telemetry by vm.sensorTelemetry.collectAsState()

    val drawerState = rememberDrawerState(initialValue = if (openDrawerOnLaunch) DrawerValue.Open else DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val permRefreshTick = androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permRefreshTick.intValue++ }

    LaunchedEffect(openDrawerOnLaunch) {
        if (openDrawerOnLaunch) drawerState.open()
    }

    @Suppress("UNUSED_EXPRESSION") permRefreshTick.intValue
    val missingPerms = AppPermission.entries
        .filter { it.critical && it.applicable && !it.isGranted(context) }
        .flatMap { it.permissions }
    val missingCritical = missingPerms.isNotEmpty()
    val batteryRestricted = !vm.isIgnoringBatteryOptimizations()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(315.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        // Drawer Header with Driver Identity & Mode Switcher
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(HeroTealGradient)
                                .padding(20.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.18f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = Color(0xFF5EEAD4),
                                            modifier = Modifier.size(26.dp),
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = profile.name.ifBlank { "RakshaKavach Driver" },
                                            style = MaterialTheme.typography.titleLarge,
                                            color = Color.White,
                                        )
                                        Text(
                                            text = "${profile.vehicleType.name.replace('_', '-')} • ${
                                                if (profile.bloodGroup.isNotBlank()) "Blood ${profile.bloodGroup}" else "Profile Ready"
                                            }",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.80f),
                                        )
                                    }
                                }

                                // Live Mode vs Tester Mode Segmented Switcher inside Drawer
                                ModeToggleRow(
                                    testerMode = settings.testerMode,
                                    onSelectTesterMode = { vm.setTesterMode(it) },
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "SAFETY & PROTECTION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                        )

                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Home, contentDescription = null) },
                            label = { Text("Home Cockpit", fontWeight = FontWeight.Bold) },
                            badge = {
                                StatusPill(
                                    text = if (settings.protectionEnabled) "ARMED" else "OFF",
                                    color = if (settings.protectionEnabled) SafeGreen else WarningAmber,
                                )
                            },
                            selected = true,
                            onClick = { scope.launch { drawerState.close() } },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        )

                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Contacts, contentDescription = null) },
                            label = { Text(stringResource(R.string.contacts_title), fontWeight = FontWeight.SemiBold) },
                            badge = {
                                StatusPill(
                                    text = "${contacts.size}/5",
                                    color = if (contacts.isNotEmpty()) SafeGreen else WarningAmber,
                                )
                            },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onOpenContacts()
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        )

                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.MedicalInformation, contentDescription = null) },
                            label = { Text(stringResource(R.string.home_profile), fontWeight = FontWeight.SemiBold) },
                            badge = { StatusPill(text = "AES-256", color = SafeGreen) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onOpenProfile()
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        )

                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.History, contentDescription = null) },
                            label = { Text(stringResource(R.string.history_title), fontWeight = FontWeight.SemiBold) },
                            badge = { StatusPill(text = "${incidents.size + trips.size}", color = AccentIndigo) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onOpenHistory()
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
                        Text(
                            text = "TESTING & SYSTEM SETTINGS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                        )

                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Science, contentDescription = null) },
                            label = { Text("Tester Mode & Sensor Lab", fontWeight = FontWeight.SemiBold) },
                            badge = {
                                StatusPill(
                                    text = if (settings.testerMode) "TESTER" else "LIVE",
                                    color = if (settings.testerMode) AccentPurple else SafeGreen,
                                )
                            },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onOpenTestSos()
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        )

                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.PrivacyTip, contentDescription = null) },
                            label = { Text(stringResource(R.string.home_privacy), fontWeight = FontWeight.SemiBold) },
                            badge = { StatusPill(text = vm.currentLanguageTag().uppercase(Locale.US), color = SafetyTeal) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onOpenPrivacy()
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        )
                    }

                    // Emergency Quick Footer inside Drawer
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        HorizontalDivider()
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    vm.triggerManualSos()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(46.dp),
                            ) {
                                Icon(Icons.Default.Emergency, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("SOS Alert", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { EmergencyCaller.call112(context) },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.5.dp, EmergencyRed),
                                modifier = Modifier.weight(1f).height(46.dp),
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, tint = EmergencyRed, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Call 112", color = EmergencyRed, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Open User Menu",
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    },
                    title = {
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
                                text = if (settings.testerMode) "TESTER MODE" else "LIVE MODE",
                                color = if (settings.testerMode) AccentPurple else SafeGreen,
                            )
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // LIVE MODE vs TESTER MODE Switcher Card on Home Screen
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        1.dp,
                        if (settings.testerMode) AccentPurple.copy(alpha = 0.45f) else SafeGreen.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = if (settings.testerMode) Icons.Default.Science else Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = if (settings.testerMode) AccentPurple else SafeGreen,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = if (settings.testerMode) "Tester Mode: Hardware Sensor Lab & Simulator"
                                    else "Live Mode: Real Hardware Sensors & GPS Armed",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        ModeToggleRow(
                            testerMode = settings.testerMode,
                            onSelectTesterMode = { vm.setTesterMode(it) },
                            darkSurface = false,
                        )
                    }
                }

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
                                is TripState.Active -> "%.0f km/h".format(Locale.US, t.speedKmh)
                                is TripState.Confirming -> "%.0f km/h".format(Locale.US, t.speedKmh)
                                TripState.Idle -> "0 km/h"
                            }
                            val tripLabel = when (val t = tripState) {
                                is TripState.Active -> "%.2f km".format(Locale.US, t.distanceKm)
                                is TripState.Confirming -> "Confirming"
                                TripState.Idle -> "Standby"
                            }
                            val gLabel = if (telemetry.active) {
                                "%.2fg".format(Locale.US, telemetry.accelG)
                            } else if (vm.hasAccelerometer) {
                                "%.1fg Gate".format(Locale.US, settings.impactThresholdG)
                            } else "Limited"

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
                                    label = "G-Sensor",
                                    value = gLabel,
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

                            // Driving Mode Buttons: In LIVE MODE, show only real GPS & hardware sensor driving mode.
                            // In TESTER MODE, also show the Simulated Trip button.
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                if (tripState is TripState.Idle) {
                                    Button(
                                        onClick = {
                                            if (missingPerms.isNotEmpty()) {
                                                permLauncher.launch(missingPerms.toTypedArray())
                                            }
                                            vm.startTrip(TripMode.MANUAL)
                                            Toast.makeText(
                                                context,
                                                if (settings.testerMode) "Driving Mode Started! Shake phone (>${settings.testerShakeThresholdG}g) to trigger crash alert."
                                                else "Live Driving Mode Started — Hardware Sensors & GPS Active",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        },
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
                                            text = if (settings.testerMode) stringResource(R.string.home_start_manual_trip)
                                            else "Start Live Driving Mode (Real GPS & Sensors)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                        )
                                    }
                                    if (settings.testerMode) {
                                        OutlinedButton(
                                            onClick = {
                                                if (missingPerms.isNotEmpty()) {
                                                    permLauncher.launch(missingPerms.toTypedArray())
                                                }
                                                vm.startTrip(TripMode.SIMULATED)
                                                Toast.makeText(
                                                    context,
                                                    "Simulated Trip (40 km/h) Started! Shake phone or tap Crash (90) below.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            },
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

                // Tester Mode Quick Strip on Home Screen when Tester Mode is active
                if (settings.testerMode) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, AccentPurple.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Science, contentDescription = null, tint = AccentPurple)
                                    Text("Tester Mode Quick Triggers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                TextButton(onClick = onOpenTestSos) {
                                    Text("Open Sensor Lab →", fontWeight = FontWeight.Bold, color = AccentPurple)
                                }
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

                if (missingCritical) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (missingPerms.isNotEmpty()) {
                                permLauncher.launch(missingPerms.toTypedArray())
                            } else {
                                onOpenPrivacy()
                            }
                        },
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
                    icon = Icons.Default.Science,
                    accentColor = AccentPurple,
                    title = "Tester Mode & Hardware Sensor Lab",
                    subtitle = "Live accelerometer/gyroscope g-meter, shake test & Test SOS",
                    badgeText = if (settings.testerMode) "TESTER ON" else "LIVE MODE",
                    badgeColor = if (settings.testerMode) AccentPurple else SafeGreen,
                    onClick = onOpenTestSos,
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
}

@Composable
fun ModeToggleRow(
    testerMode: Boolean,
    onSelectTesterMode: (Boolean) -> Unit,
    darkSurface: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (darkSurface) Color.White.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            onClick = { onSelectTesterMode(false) },
            color = if (!testerMode) SafeGreen else Color.Transparent,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            Row(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = if (!testerMode) Color.White else if (darkSurface) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "LIVE MODE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (!testerMode) Color.White else if (darkSurface) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Surface(
            onClick = { onSelectTesterMode(true) },
            color = if (testerMode) AccentPurple else Color.Transparent,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            Row(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Science,
                    contentDescription = null,
                    tint = if (testerMode) Color.White else if (darkSurface) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "TESTER MODE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (testerMode) Color.White else if (darkSurface) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
