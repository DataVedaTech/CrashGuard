package com.dataveda.crashguard.ui.profile

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dataveda.crashguard.R
import com.dataveda.crashguard.domain.models.EmergencyProfile
import com.dataveda.crashguard.domain.models.VehicleType
import com.dataveda.crashguard.ui.CrashGuardViewModel
import com.dataveda.crashguard.ui.theme.AccentIndigo
import com.dataveda.crashguard.ui.theme.EmergencyRed
import com.dataveda.crashguard.ui.theme.HeroTealGradient
import com.dataveda.crashguard.ui.theme.IconBadge
import com.dataveda.crashguard.ui.theme.SafeGreen
import com.dataveda.crashguard.ui.theme.SafetyTeal
import com.dataveda.crashguard.ui.theme.StatusPill
import com.dataveda.crashguard.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    vm: CrashGuardViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val saved by vm.profile.collectAsState()
    var draft by remember { mutableStateOf(EmergencyProfile()) }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(saved) {
        if (!initialized || draft == EmergencyProfile()) {
            draft = saved
            initialized = true
        }
    }

    val savedMsg = stringResource(R.string.profile_saved)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    Box(modifier = Modifier.padding(end = 12.dp)) {
                        StatusPill(text = "AES-256 KEYSTORE", color = SafeGreen, icon = Icons.Default.Lock)
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
            // Gradient Security Banner
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HeroTealGradient)
                        .padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.MedicalInformation, contentDescription = null, tint = Color(0xFF5EEAD4), modifier = Modifier.size(26.dp))
                        }
                        Text(
                            text = stringResource(R.string.profile_encrypted_note),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.90f),
                        )
                    }
                }
            }

            // Driver Identity & Vehicle Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IconBadge(Icons.Default.Person, tint = SafetyTeal, size = 38.dp, iconSize = 20.dp)
                        Text("Driver Identity & Vehicle", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    OutlinedTextField(
                        value = draft.name,
                        onValueChange = { draft = draft.copy(name = it) },
                        label = { Text(stringResource(R.string.profile_name)) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = draft.phone,
                        onValueChange = { draft = draft.copy(phone = it) },
                        label = { Text(stringResource(R.string.profile_phone)) },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Text(stringResource(R.string.profile_vehicle), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        VehicleType.entries.forEach { type ->
                            val (label, vIcon) = when (type) {
                                VehicleType.CAR -> stringResource(R.string.vehicle_car) to Icons.Default.DirectionsCar
                                VehicleType.TWO_WHEELER -> stringResource(R.string.vehicle_two_wheeler) to Icons.Default.TwoWheeler
                                VehicleType.OTHER -> stringResource(R.string.vehicle_other) to Icons.Default.DirectionsCar
                            }
                            val selected = draft.vehicleType == type
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selected) SafetyTeal.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                border = BorderStroke(1.5.dp, if (selected) SafetyTeal else MaterialTheme.colorScheme.outline),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { draft = draft.copy(vehicleType = type) },
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        vIcon,
                                        contentDescription = null,
                                        tint = if (selected) SafetyTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) SafetyTeal else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Encrypted Medical Fields Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IconBadge(Icons.Default.LocalHospital, tint = EmergencyRed, size = 38.dp, iconSize = 20.dp)
                        Text("Encrypted Medical Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    EncryptedFieldWithConsent(
                        icon = Icons.Default.Bloodtype,
                        label = stringResource(R.string.profile_blood),
                        value = draft.bloodGroup,
                        onValueChange = { draft = draft.copy(bloodGroup = it) },
                        includeInAlert = draft.shareBloodGroup,
                        onIncludeChange = { draft = draft.copy(shareBloodGroup = it) },
                    )
                    EncryptedFieldWithConsent(
                        icon = Icons.Default.Warning,
                        label = stringResource(R.string.profile_allergies),
                        value = draft.allergies,
                        onValueChange = { draft = draft.copy(allergies = it) },
                        includeInAlert = draft.shareAllergies,
                        onIncludeChange = { draft = draft.copy(shareAllergies = it) },
                    )
                    EncryptedFieldWithConsent(
                        icon = Icons.Default.HealthAndSafety,
                        label = stringResource(R.string.profile_conditions),
                        value = draft.conditions,
                        onValueChange = { draft = draft.copy(conditions = it) },
                        includeInAlert = draft.shareConditions,
                        onIncludeChange = { draft = draft.copy(shareConditions = it) },
                    )
                    EncryptedFieldWithConsent(
                        icon = Icons.Default.Medication,
                        label = stringResource(R.string.profile_medications),
                        value = draft.medications,
                        onValueChange = { draft = draft.copy(medications = it) },
                        includeInAlert = draft.shareMedications,
                        onIncludeChange = { draft = draft.copy(shareMedications = it) },
                    )
                }
            }

            // Optional Insurance & ABHA Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IconBadge(Icons.Default.Badge, tint = AccentIndigo, size = 38.dp, iconSize = 20.dp)
                        Text("Optional Insurance & Health ID", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    OutlinedTextField(
                        value = draft.insurance,
                        onValueChange = { draft = draft.copy(insurance = it) },
                        label = { Text(stringResource(R.string.profile_insurance)) },
                        leadingIcon = { Icon(Icons.Default.HealthAndSafety, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = draft.abhaId,
                        onValueChange = { draft = draft.copy(abhaId = it) },
                        label = { Text(stringResource(R.string.profile_abha)) },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    vm.saveProfile(draft) {
                        Toast.makeText(context, savedMsg, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SafetyTeal),
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.profile_save), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun EncryptedFieldWithConsent(
    icon: ImageVector,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    includeInAlert: Boolean,
    onIncludeChange: (Boolean) -> Unit,
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            leadingIcon = { Icon(icon, contentDescription = null) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = includeInAlert, onCheckedChange = onIncludeChange)
            Text(
                text = stringResource(R.string.profile_include_in_alert),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
