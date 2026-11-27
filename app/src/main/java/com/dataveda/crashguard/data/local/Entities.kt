package com.dataveda.crashguard.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dataveda.crashguard.domain.models.AlertKind
import com.dataveda.crashguard.domain.models.DeliveryStatus
import com.dataveda.crashguard.domain.models.IncidentSource
import com.dataveda.crashguard.domain.models.IncidentStatus
import com.dataveda.crashguard.domain.models.TripRole
import com.dataveda.crashguard.domain.models.TripStatus
import com.dataveda.crashguard.domain.models.TripType
import com.dataveda.crashguard.domain.models.UserResponse
import com.dataveda.crashguard.domain.models.VehicleType

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val isPrimary: Boolean = false,
    val hasApp: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val id: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val distanceKm: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val tripType: TripType = TripType.UNKNOWN,
    val role: TripRole = TripRole.UNKNOWN,
    val crashAlertCount: Int = 0,
    val falseAlarmCount: Int = 0,
    val incidentCount: Int = 0,
    val status: TripStatus = TripStatus.ACTIVE,
    val simulated: Boolean = false,
    val manual: Boolean = false,
)

@Entity(tableName = "incidents", indices = [Index("tripId")])
data class IncidentEntity(
    @PrimaryKey val id: String,
    val tripId: String? = null,
    val detectedAt: Long,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyM: Float? = null,
    val locationFixTime: Long? = null,
    val locationIsLastKnown: Boolean = false,
    val crashConfidence: Int = 0,
    val impactG: Double = 0.0,
    val speedBeforeKmh: Double = 0.0,
    val orientationChangeDeg: Double = 0.0,
    val tripType: TripType = TripType.UNKNOWN,
    val userResponse: UserResponse = UserResponse.NONE,
    val status: IncidentStatus = IncidentStatus.ACTIVE,
    val source: IncidentSource = IncidentSource.SENSOR,
    val batteryLevel: Int = -1,
    val networkType: String = "none",
    val notes: String = "",
    val isTest: Boolean = false,
    val resolvedAt: Long? = null,
    val sensorSummary: String = "",
    val synced: Boolean = false,
)

@Entity(tableName = "alert_deliveries", indices = [Index("incidentId")])
data class AlertDeliveryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val incidentId: String,
    val contactId: String,
    val contactName: String,
    val phone: String,
    val kind: AlertKind,
    val channel: String = "SMS",
    val status: DeliveryStatus = DeliveryStatus.QUEUED,
    val attempts: Int = 0,
    val message: String,
    val lastError: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)

/** Single-row table. Sensitive medical fields are stored encrypted (Android Keystore AES-GCM). */
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 0,
    val name: String = "",
    val phone: String = "",
    val bloodGroupEnc: String = "",
    val allergiesEnc: String = "",
    val conditionsEnc: String = "",
    val medicationsEnc: String = "",
    val insuranceEnc: String = "",
    val abhaEnc: String = "",
    val vehicleType: VehicleType = VehicleType.CAR,
    val shareBloodGroup: Boolean = true,
    val shareAllergies: Boolean = false,
    val shareConditions: Boolean = false,
    val shareMedications: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)
