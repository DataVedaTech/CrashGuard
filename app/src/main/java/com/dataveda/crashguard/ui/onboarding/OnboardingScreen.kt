package com.dataveda.crashguard.ui.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.dataveda.crashguard.R
import com.dataveda.crashguard.core.permissions.AppPermission
import com.dataveda.crashguard.ui.theme.AccentIndigo
import com.dataveda.crashguard.ui.theme.AccentPurple
import com.dataveda.crashguard.ui.theme.EmergencyRed
import com.dataveda.crashguard.ui.theme.HeroTealGradient
import com.dataveda.crashguard.ui.theme.IconBadge
import com.dataveda.crashguard.ui.theme.SafeGreen
import com.dataveda.crashguard.ui.theme.SafetyTeal
import com.dataveda.crashguard.ui.theme.StatusPill
import com.dataveda.crashguard.ui.theme.WarningAmber

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) }
    var refreshTick by remember { mutableIntStateOf(0) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshTick++ }

    val heroIcon = when (step) {
        0 -> Icons.Default.Shield
        1 -> Icons.Default.Lock
        else -> Icons.Default.VerifiedUser
    }
    val stepSubtitle = when (step) {
        0 -> "AUTOMATIC ROAD-CRASH PROTECTION"
        1 -> "ZERO SURVEILLANCE • ON-DEVICE ENCRYPTION"
        else -> "READINESS & PERMISSIONS"
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(3) { idx ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (idx <= step) SafetyTeal
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    StatusPill(text = "Step ${step + 1}/3", color = SafetyTeal)
                }

                Card(
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(HeroTealGradient)
                            .padding(22.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(68.dp)) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawCircle(color = Color.White.copy(alpha = 0.12f))
                                    drawCircle(
                                        color = Color(0xFF5EEAD4).copy(alpha = 0.4f),
                                        style = Stroke(width = 2.dp.toPx()),
                                    )
                                }
                                Icon(
                                    imageVector = heroIcon,
                                    contentDescription = null,
                                    tint = Color(0xFF5EEAD4),
                                    modifier = Modifier.size(34.dp),
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = stepSubtitle,
                                    color = Color(0xFF5EEAD4),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                                Text(
                                    text = when (step) {
                                        0 -> stringResource(R.string.onboard_welcome_title)
                                        1 -> stringResource(R.string.onboard_privacy_title)
                                        else -> stringResource(R.string.onboard_perm_title)
                                    },
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White,
                                )
                            }
                        }
                    }
                }

                when (step) {
                    0 -> {
                        Text(
                            text = stringResource(R.string.onboard_welcome_body),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        OnboardHighlightCard(
                            icon = Icons.Default.Radar,
                            accent = SafetyTeal,
                            title = "Multi-Sensor Fusion Detection",
                            body = "Combines accelerometer (4.5g+), gyroscope rollover, and GPS speed drop to filter out bumps & phone drops.",
                        )
                        OnboardHighlightCard(
                            icon = Icons.Default.Timer,
                            accent = WarningAmber,
                            title = "45-Second Check-In Window",
                            body = "Loud alarm & full-screen countdown lets you cancel false alarms with one tap before anyone is notified.",
                        )
                        OnboardHighlightCard(
                            icon = Icons.Default.GpsFixed,
                            accent = EmergencyRed,
                            title = "Live Location & One-Tap 112",
                            body = "Shares your Google Maps link with up to 5 trusted contacts and gives instant access to 112 emergency services.",
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = EmergencyRed)
                                Text(
                                    text = stringResource(R.string.disclaimer_primary),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = stringResource(R.string.disclaimer_secondary),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(14.dp),
                            )
                        }
                    }

                    1 -> {
                        OnboardHighlightCard(
                            icon = Icons.Default.DirectionsCar,
                            accent = SafetyTeal,
                            title = "Trip-Scoped Monitoring Only",
                            body = "No continuous background location tracking when you are not driving.",
                        )
                        OnboardHighlightCard(
                            icon = Icons.Default.Security,
                            accent = SafeGreen,
                            title = "Android Keystore AES-256 Encryption",
                            body = "Blood group, allergies, and medical notes are encrypted locally and never displayed on the lock screen.",
                        )
                        OnboardHighlightCard(
                            icon = Icons.Default.Lock,
                            accent = AccentIndigo,
                            title = "Zero Camera or Microphone Access",
                            body = "CrashGuard MVP uses motion sensors and trip GPS only—never recording video or audio.",
                        )
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = stringResource(R.string.onboard_privacy_body),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }

                    else -> {
                        @Suppress("UNUSED_EXPRESSION") refreshTick
                        Text(
                            text = stringResource(R.string.onboard_perm_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        AppPermission.entries.filter { it.applicable }.forEach { perm ->
                            val granted = perm.isGranted(context)
                            val (permIcon, permAccent) = permissionIconAndColor(perm)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(
                                    1.dp,
                                    if (granted) SafeGreen.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f).padding(end = 10.dp),
                                    ) {
                                        IconBadge(
                                            icon = permIcon,
                                            tint = if (granted) SafeGreen else permAccent,
                                            size = 44.dp,
                                            iconSize = 22.dp,
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = stringResource(perm.title),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                            )
                                            Text(
                                                text = stringResource(perm.reason),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    if (granted) {
                                        StatusPill(
                                            text = stringResource(R.string.onboard_granted),
                                            color = SafeGreen,
                                            icon = Icons.Default.CheckCircle,
                                        )
                                    } else {
                                        OutlinedButton(
                                            onClick = { launcher.launch(perm.permissions.toTypedArray()) },
                                            shape = RoundedCornerShape(12.dp),
                                        ) {
                                            Text(stringResource(R.string.onboard_grant), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                        Text(
                            text = stringResource(R.string.onboard_contacts_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (step > 0) {
                    OutlinedButton(
                        onClick = { step-- },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.onboard_back))
                    }
                }
                Button(
                    onClick = { if (step < 2) step++ else onFinished() },
                    modifier = Modifier.weight(2f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SafetyTeal),
                ) {
                    Text(
                        text = if (step < 2) stringResource(R.string.onboard_next)
                        else stringResource(R.string.onboard_finish),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun OnboardHighlightCard(
    icon: ImageVector,
    accent: Color,
    title: String,
    body: String,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            IconBadge(icon = icon, tint = accent, size = 46.dp, iconSize = 24.dp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

fun permissionIconAndColor(perm: AppPermission): Pair<ImageVector, Color> = when (perm) {
    AppPermission.NOTIFICATIONS -> Icons.Default.NotificationsActive to SafetyTeal
    AppPermission.LOCATION -> Icons.Default.GpsFixed to AccentIndigo
    AppPermission.ACTIVITY -> Icons.Default.DirectionsCar to SafeGreen
    AppPermission.SMS -> Icons.Default.Sms to AccentPurple
    AppPermission.CALL -> Icons.Default.Call to EmergencyRed
    AppPermission.BLUETOOTH -> Icons.Default.Bluetooth to WarningAmber
}
