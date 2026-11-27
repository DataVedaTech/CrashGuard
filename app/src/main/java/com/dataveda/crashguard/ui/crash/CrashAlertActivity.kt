package com.dataveda.crashguard.ui.crash

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dataveda.crashguard.R
import com.dataveda.crashguard.core.emergency.AlertMessageBuilder
import com.dataveda.crashguard.core.emergency.EmergencyCaller
import com.dataveda.crashguard.core.emergency.EmergencyState
import com.dataveda.crashguard.data.local.AlertDeliveryEntity
import com.dataveda.crashguard.domain.models.DeliveryStatus
import com.dataveda.crashguard.domain.models.GeoFix
import com.dataveda.crashguard.domain.models.UserResponse
import com.dataveda.crashguard.ui.CrashGuardViewModel
import com.dataveda.crashguard.ui.theme.CrashGuardTheme
import com.dataveda.crashguard.ui.theme.EmergencyRed
import com.dataveda.crashguard.ui.theme.IconBadge
import com.dataveda.crashguard.ui.theme.SafeGreen
import com.dataveda.crashguard.ui.theme.StatusPill
import com.dataveda.crashguard.ui.theme.WarningAmber
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CrashAlertActivity : ComponentActivity() {

    private val vm: CrashGuardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureEmergencyWindow()
        setContent {
            CrashGuardTheme(darkTheme = true) {
                val state by vm.emergencyState.collectAsState()
                LaunchedEffect(state) {
                    if (state is EmergencyState.Idle) finish()
                }
                when (val s = state) {
                    is EmergencyState.CheckIn -> CrashCheckInScreen(
                        state = s,
                        onOkay = { vm.respondToCrash(UserResponse.OKAY) },
                        onNeedHelp = { vm.respondToCrash(UserResponse.NEED_HELP) },
                        onFalseAlarm = { vm.respondToCrash(UserResponse.FALSE_ALARM) },
                    )
                    is EmergencyState.Escalated -> SosActiveScreen(
                        state = s,
                        vm = vm,
                    )
                    EmergencyState.Idle -> Box(Modifier.fillMaxSize().background(Color(0xFF121212)))
                }
            }
        }
    }

    private fun configureEmergencyWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.attributes = window.attributes.apply { screenBrightness = 1.0f }
    }
}

@Composable
fun CrashCheckInScreen(
    state: EmergencyState.CheckIn,
    onOkay: () -> Unit,
    onNeedHelp: () -> Unit,
    onFalseAlarm: () -> Unit,
) {
    val bgBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF2B0709), Color(0xFF160405), Color(0xFF0E0507))
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            .padding(horizontal = 24.dp, vertical = 32.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    color = Color(0xFFFF5252).copy(alpha = 0.18f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF8A80),
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "CONFIDENCE SCORE ${state.confidence}/100",
                            color = Color(0xFFFFCDD2),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.crash_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFFFF8A80),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.crash_are_you_okay),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
            }

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
                val progress = if (state.totalSeconds > 0) {
                    state.secondsLeft.toFloat() / state.totalSeconds.toFloat()
                } else 0f
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color(0xFFFF5252).copy(alpha = 0.12f),
                        radius = size.minDimension / 2f,
                    )
                    val stroke = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(
                        color = Color(0xFF4A1518),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = stroke,
                    )
                    drawArc(
                        color = Color(0xFFFF5252),
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = stroke,
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.secondsLeft.toString(),
                        fontSize = 76.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                    Text(
                        text = "SECONDS LEFT",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Color(0xFFFFCDD2),
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.crash_countdown_copy, state.secondsLeft),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = Color(0xFFFFEBEE),
                )
                Surface(
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Default.GpsFixed,
                            contentDescription = null,
                            tint = if (state.fix != null) Color(0xFF4ADE80) else WarningAmber,
                            modifier = Modifier.size(16.dp),
                        )
                        LocationStatusLine(fix = state.fix, locating = state.locating)
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Button(
                        onClick = onOkay,
                        modifier = Modifier.weight(1f).height(80.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SafeGreen, contentColor = Color.White),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.crash_im_okay).uppercase(),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    Button(
                        onClick = onNeedHelp,
                        modifier = Modifier.weight(1f).height(80.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed, contentColor = Color.White),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Emergency, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.crash_need_help).uppercase(),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = onFalseAlarm,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFCDD2).copy(alpha = 0.4f)),
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFFFCDD2), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.crash_false_alarm),
                        color = Color(0xFFFFCDD2),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SosActiveScreen(
    state: EmergencyState.Escalated,
    vm: CrashGuardViewModel,
) {
    val context = LocalContext.current
    val contacts by vm.contacts.collectAsState()
    val incident by vm.observeIncident(state.incidentId).collectAsState(initial = null)
    val deliveries by vm.observeDeliveries(state.incidentId).collectAsState(initial = emptyList())

    val quickNotes = listOf(
        stringResource(R.string.note_injured),
        stringResource(R.string.note_other_injured),
        stringResource(R.string.note_fire),
        stringResource(R.string.note_trapped),
        stringResource(R.string.note_hazard),
        stringResource(R.string.note_ambulance),
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0E131A),
        contentColor = Color.White,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(EmergencyRed.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Emergency, contentDescription = null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(28.dp))
                }
                Column {
                    StatusPill(text = "SOS ESCALATED", color = Color(0xFFFF6B6B))
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.sos_title),
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                    )
                }
            }

            Text(
                text = if (contacts.isNotEmpty()) stringResource(R.string.sos_subtitle_shared)
                else stringResource(R.string.sos_subtitle_no_contacts),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFCBD5E1),
            )

            Card(
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = { EmergencyCaller.call112(context) },
                    modifier = Modifier.fillMaxWidth().height(78.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed, contentColor = Color.White),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(26.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = stringResource(R.string.sos_call_112),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                    )
                }
            }

            state.autoCallSecondsLeft?.let { sec ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4A1818)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.sos_autocall_banner, sec),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { vm.cancelAutoCall() }) {
                            Text(stringResource(R.string.sos_cancel_autocall), color = Color(0xFFFFCDD2))
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF17202B)),
                border = BorderStroke(1.dp, Color(0xFF283545)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            IconBadge(
                                icon = Icons.Default.LocationOn,
                                tint = if (state.sharingActive) Color(0xFF4ADE80) else Color.Gray,
                                size = 40.dp,
                                iconSize = 22.dp,
                            )
                            Text(
                                text = if (state.sharingActive) stringResource(R.string.sos_sharing_active)
                                else stringResource(R.string.sos_sharing_stopped),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                            )
                        }
                    }
                    LocationStatusLine(fix = state.fix, locating = state.locating)
                    state.fix?.let { fix ->
                        Surface(
                            color = Color(0xFF0E1620),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF233243)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = fix.mapsLink,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF67E8F9),
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }
                    if (state.sharingActive) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { vm.stopSharing() },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF475569)),
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.sos_stop_sharing), color = Color.White)
                            }
                            OutlinedButton(
                                onClick = { vm.extendSharing() },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF475569)),
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.sos_extend_sharing), color = Color.White)
                            }
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF17202B)),
                border = BorderStroke(1.dp, Color(0xFF283545)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IconBadge(Icons.Default.EditNote, tint = Color(0xFF60A5FA), size = 38.dp, iconSize = 20.dp)
                        Text(stringResource(R.string.sos_quick_notes), style = MaterialTheme.typography.titleMedium)
                    }
                    if (!incident?.notes.isNullOrBlank()) {
                        Surface(
                            color = WarningAmber.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = incident?.notes.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFFDE68A),
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        quickNotes.forEach { note ->
                            AssistChip(
                                onClick = { vm.addQuickNote(note) },
                                label = { Text(note, color = Color.White) },
                                colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF243242)),
                            )
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF17202B)),
                border = BorderStroke(1.dp, Color(0xFF283545)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.sos_contacts_status), style = MaterialTheme.typography.titleMedium)
                    if (deliveries.isEmpty()) {
                        contacts.forEach { c ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("${c.name} (${c.phone})")
                                IconButton(onClick = { EmergencyCaller.dial(context, c.phone) }) {
                                    Icon(Icons.Default.Call, contentDescription = stringResource(R.string.contacts_call))
                                }
                            }
                        }
                    } else {
                        deliveries.forEach { d ->
                            DeliveryRow(
                                delivery = d,
                                onCall = { EmergencyCaller.dial(context, d.phone) },
                                onRetry = { vm.retrySms(d.id) },
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (contacts.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { vm.openSmsComposer(context) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.sos_send_sms_app), color = Color.White)
                            }
                        }
                        incident?.let { inc ->
                            OutlinedButton(
                                onClick = { vm.shareIncidentText(context, inc, deliveries) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.sos_share_report), color = Color.White)
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { vm.closeIncident(markSafe = true) },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SafeGreen, contentColor = Color.White),
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.sos_im_safe_close), style = MaterialTheme.typography.labelLarge)
            }

            Text(
                text = stringResource(R.string.disclaimer_primary),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8),
            )
        }
    }
}

@Composable
private fun LocationStatusLine(fix: GeoFix?, locating: Boolean) {
    val text = when {
        fix != null && fix.isLastKnown -> stringResource(
            R.string.crash_location_last_known,
            AlertMessageBuilder.timestamp(fix.timeMillis),
        )
        fix != null -> stringResource(
            R.string.crash_location_ready,
            "%.4f, %.4f".format(java.util.Locale.US, fix.latitude, fix.longitude),
        )
        locating -> stringResource(R.string.crash_locating)
        else -> stringResource(R.string.crash_location_unavailable)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (fix?.isLastKnown == true) Color(0xFFFBBF24) else Color(0xFFCBD5E1),
    )
}

@Composable
private fun DeliveryRow(
    delivery: AlertDeliveryEntity,
    onCall: () -> Unit,
    onRetry: () -> Unit,
) {
    val badgeColor = when (delivery.status) {
        DeliveryStatus.DELIVERED, DeliveryStatus.SENT -> SafeGreen
        DeliveryStatus.SENDING, DeliveryStatus.QUEUED -> WarningAmber
        DeliveryStatus.FAILED, DeliveryStatus.MANUAL_REQUIRED -> EmergencyRed
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(delivery.contactName, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                text = "${delivery.phone} • ${delivery.status.name} (attempt ${delivery.attempts})",
                style = MaterialTheme.typography.bodyMedium,
                color = badgeColor,
            )
        }
        Row {
            if (delivery.status == DeliveryStatus.FAILED) {
                IconButton(onClick = onRetry) {
                    Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = Color.White)
                }
            }
            IconButton(onClick = onCall) {
                Icon(Icons.Default.Call, contentDescription = "Call", tint = Color.White)
            }
        }
    }
}
