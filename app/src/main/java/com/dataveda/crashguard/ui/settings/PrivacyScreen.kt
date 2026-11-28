package com.dataveda.crashguard.ui.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dataveda.crashguard.R
import com.dataveda.crashguard.core.permissions.AppPermission
import com.dataveda.crashguard.core.permissions.PermissionUtils
import com.dataveda.crashguard.ui.CrashGuardViewModel
import com.dataveda.crashguard.ui.onboarding.permissionIconAndColor
import com.dataveda.crashguard.ui.theme.AccentIndigo
import com.dataveda.crashguard.ui.theme.AccentPurple
import com.dataveda.crashguard.ui.theme.EmergencyRed
import com.dataveda.crashguard.ui.theme.IconBadge
import com.dataveda.crashguard.ui.theme.SafeGreen
import com.dataveda.crashguard.ui.theme.SafetyTeal
import com.dataveda.crashguard.ui.theme.StatusPill
import com.dataveda.crashguard.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    vm: CrashGuardViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsState()
    var refreshTick by remember { mutableIntStateOf(0) }
    var currentLang by remember { mutableStateOf(vm.currentLanguageTag()) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshTick++ }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.privacy_title), fontWeight = FontWeight.Bold) },
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
            @Suppress("UNUSED_EXPRESSION") refreshTick

            // 1. Language Switcher (English, Hindi, Marathi - Spec 2.14)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IconBadge(Icons.Default.Language, tint = SafetyTeal, size = 38.dp, iconSize = 20.dp)
                        Text(stringResource(R.string.privacy_language), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(
                            "en" to stringResource(R.string.lang_en),
                            "hi" to stringResource(R.string.lang_hi),
                            "mr" to stringResource(R.string.lang_mr),
                        ).forEach { (tag, label) ->
                            FilterChip(
                                selected = currentLang == tag,
                                onClick = {
                                    currentLang = tag
                                    vm.setLanguage(tag)
                                },
                                label = { Text(label, fontWeight = FontWeight.SemiBold) },
                            )
                        }
                    }
                }
            }

            // 2. Opt-in Escalation Settings (SMS fallback & Auto-call 112)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                            IconBadge(Icons.Default.Sms, tint = AccentIndigo, size = 38.dp, iconSize = 20.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(text = stringResource(R.string.privacy_sms_toggle), fontWeight = FontWeight.Medium)
                        }
                        Switch(
                            checked = settings.smsEnabled,
                            onCheckedChange = { enabled ->
                                vm.setSmsEnabled(enabled)
                                if (enabled && AppPermission.SMS.permissions.isNotEmpty() && !AppPermission.SMS.isGranted(context)) {
                                    launcher.launch(AppPermission.SMS.permissions.toTypedArray())
                                }
                            }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                            IconBadge(Icons.Default.Call, tint = EmergencyRed, size = 38.dp, iconSize = 20.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(text = stringResource(R.string.privacy_autocall_toggle), fontWeight = FontWeight.Medium)
                        }
                        Switch(
                            checked = settings.autoCallEnabled,
                            onCheckedChange = { enabled ->
                                vm.setAutoCallEnabled(enabled)
                                if (enabled && AppPermission.CALL.permissions.isNotEmpty() && !AppPermission.CALL.isGranted(context)) {
                                    launcher.launch(AppPermission.CALL.permissions.toTypedArray())
                                }
                            }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                            IconBadge(Icons.Default.Science, tint = AccentPurple, size = 38.dp, iconSize = 20.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(text = stringResource(R.string.privacy_dev_toggle), fontWeight = FontWeight.Medium)
                        }
                        Switch(
                            checked = settings.developerMode,
                            onCheckedChange = { vm.setDeveloperMode(it) },
                        )
                    }
                }
            }

            // 3. Hardware Sensor & System Readiness Check (Spec 11.4 & 11.5)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IconBadge(Icons.Default.Sensors, tint = SafeGreen, size = 38.dp, iconSize = 20.dp)
                        Text(
                            text = "Hardware & System Readiness",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    ReadinessRow("Accelerometer sensor", vm.hasAccelerometer)
                    ReadinessRow("Gyroscope sensor", vm.hasGyroscope)
                    val fullScreenOk = PermissionUtils.canUseFullScreenIntent(context)
                    ReadinessRow(stringResource(R.string.privacy_full_screen_perm), fullScreenOk)
                    if (!fullScreenOk) {
                        OutlinedButton(
                            onClick = { runCatching { context.startActivity(PermissionUtils.fullScreenIntentSettings(context)) } }
                        ) {
                            Text("Allow Full-Screen Alerts")
                        }
                    }
                    val batteryOk = vm.isIgnoringBatteryOptimizations()
                    ReadinessRow(stringResource(R.string.privacy_battery_opt), batteryOk)
                    if (!batteryOk) {
                        OutlinedButton(
                            onClick = { runCatching { context.startActivity(PermissionUtils.batteryOptimizationIntent(context)) } },
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Default.BatteryChargingFull, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.home_battery_fix), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 4. Per-Permission Status Cards (Spec section 9)
            AppPermission.entries.filter { it.applicable }.forEach { perm ->
                val granted = perm.isGranted(context)
                val (permIcon, permAccent) = permissionIconAndColor(perm)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        1.dp,
                        if (granted) SafeGreen.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                IconBadge(permIcon, tint = if (granted) SafeGreen else permAccent, size = 40.dp, iconSize = 20.dp)
                                Text(stringResource(perm.title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            StatusPill(
                                text = if (granted) "GRANTED" else if (perm.critical) "REQUIRED" else "OPTIONAL",
                                color = if (granted) SafeGreen else if (perm.critical) EmergencyRed else WarningAmber,
                                icon = if (granted) Icons.Default.CheckCircle else Icons.Default.Warning,
                            )
                        }
                        Text(stringResource(perm.reason), style = MaterialTheme.typography.bodyMedium)
                        if (!granted) {
                            Text(
                                text = stringResource(perm.limitation),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (perm.critical) EmergencyRed else WarningAmber,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { launcher.launch(perm.permissions.toTypedArray()) },
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Text(stringResource(R.string.onboard_grant), fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { runCatching { context.startActivity(PermissionUtils.appSettingsIntent(context)) } },
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Text(stringResource(R.string.privacy_open_settings))
                                }
                            }
                        }
                    }
                }
            }

            // 5. Data Minimization & Local History Wipe (Spec section 10)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IconBadge(Icons.Default.Lock, tint = SafetyTeal, size = 38.dp, iconSize = 20.dp)
                        Text(stringResource(R.string.privacy_data_collected), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(stringResource(R.string.onboard_privacy_body), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { vm.clearHistory() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.privacy_delete_history), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadinessRow(label: String, ok: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        StatusPill(
            text = if (ok) "READY" else "ACTION",
            color = if (ok) SafeGreen else WarningAmber,
            icon = if (ok) Icons.Default.CheckCircle else Icons.Default.Warning,
        )
    }
}
