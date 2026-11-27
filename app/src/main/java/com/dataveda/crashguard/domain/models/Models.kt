package com.dataveda.crashguard.domain.models

enum class TripType { CAR, TWO_WHEELER, UNKNOWN }
enum class TripRole { DRIVER, PASSENGER, NOT_DRIVING, UNKNOWN }
enum class TripStatus { ACTIVE, COMPLETED }
enum class VehicleType { CAR, TWO_WHEELER, OTHER }

enum class UserResponse { NONE, OKAY, NEED_HELP, FALSE_ALARM }

enum class IncidentStatus { MONITORING, ACTIVE, RESOLVED, FALSE_ALARM, LOGGED }

enum class IncidentSource { SENSOR, SIMULATION, MANUAL_SOS, TEST }

enum class AlertKind { INITIAL, LOCATION_UPDATE, SAFE, TEST }

enum class DeliveryStatus { QUEUED, SENDING, SENT, DELIVERED, FAILED, MANUAL_REQUIRED }

enum class EscalationReason { USER_REQUESTED_HELP, UNRESPONSIVE_AFTER_CRASH, MANUAL_SOS }

/** Decrypted emergency profile used by the UI and alert builder. */
data class EmergencyProfile(
    val name: String = "",
    val phone: String = "",
    val bloodGroup: String = "",
    val allergies: String = "",
    val conditions: String = "",
    val medications: String = "",
    val insurance: String = "",
    val abhaId: String = "",
    val vehicleType: VehicleType = VehicleType.CAR,
    val shareBloodGroup: Boolean = true,
    val shareAllergies: Boolean = false,
    val shareConditions: Boolean = false,
    val shareMedications: Boolean = false,
)

data class GeoFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyM: Float?,
    val timeMillis: Long,
    val isLastKnown: Boolean,
) {
    val mapsLink: String get() = mapsLink(latitude, longitude)

    companion object {
        fun mapsLink(lat: Double, lng: Double): String =
            "https://maps.google.com/?q=%.6f,%.6f".format(java.util.Locale.US, lat, lng)
    }
}

data class DeviceStatus(val batteryPercent: Int, val networkType: String)
