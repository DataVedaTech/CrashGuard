package com.dataveda.crashguard.core.emergency

import com.dataveda.crashguard.data.local.AlertDeliveryEntity
import com.dataveda.crashguard.data.local.IncidentEntity
import com.dataveda.crashguard.domain.models.EmergencyProfile
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** Exports structured incident reports as Plain Text and JSON per MVP spec section 4.7. */
object IncidentReportExporter {

    fun toPlainText(
        incident: IncidentEntity,
        profile: EmergencyProfile,
        deliveries: List<AlertDeliveryEntity>,
    ): String = buildString {
        appendLine("RAKSHAKAVACH INCIDENT REPORT")
        appendLine("============================")
        appendLine("Incident ID: ${incident.id}")
        appendLine("Timestamp: ${AlertMessageBuilder.timestamp(incident.detectedAt)}")
        if (incident.resolvedAt != null) {
            appendLine("Resolved At: ${AlertMessageBuilder.timestamp(incident.resolvedAt)}")
        }
        appendLine("Status: ${incident.status.name}")
        appendLine("Source: ${incident.source.name}")
        appendLine("User Response: ${incident.userResponse.name}")
        appendLine("Trip Type: ${incident.tripType.name}")
        appendLine()
        appendLine("--- Sensor & Motion Summary ---")
        appendLine("Crash Confidence: ${incident.crashConfidence}/100")
        appendLine("Speed Before Event: ${"%.1f".format(Locale.US, incident.speedBeforeKmh)} km/h")
        appendLine("Impact Magnitude: ${"%.2f".format(Locale.US, incident.impactG)} g")
        appendLine("Orientation Change: ${"%.1f".format(Locale.US, incident.orientationChangeDeg)} deg")
        appendLine()
        appendLine("--- Location & Device ---")
        val fix = incident.toFix()
        if (fix != null) {
            appendLine("Coordinates: ${"%.6f, %.6f".format(Locale.US, fix.latitude, fix.longitude)}")
            appendLine("Maps Link: ${fix.mapsLink}")
            appendLine("Accuracy: ${fix.accuracyM?.let { "±%.1f m".format(Locale.US, it) } ?: "Unknown"}")
            appendLine("Fix Type: ${if (fix.isLastKnown) "Last Known" else "Live GPS"}")
        } else {
            appendLine("Location: Unavailable")
        }
        appendLine("Battery Level: ${if (incident.batteryLevel >= 0) "${incident.batteryLevel}%" else "Unknown"}")
        appendLine("Network Type: ${incident.networkType}")
        if (incident.notes.isNotBlank()) {
            appendLine("User Notes: ${incident.notes}")
        }
        val med = AlertMessageBuilder.medicalLines(profile)
        if (med.isNotEmpty() || profile.name.isNotBlank()) {
            appendLine()
            appendLine("--- Driver & Emergency Profile ---")
            if (profile.name.isNotBlank()) appendLine("Name: ${profile.name}")
            med.forEach { appendLine(it) }
        }
        if (deliveries.isNotEmpty()) {
            appendLine()
            appendLine("--- Emergency Contacts Notified ---")
            deliveries.forEach { d ->
                appendLine("- ${d.contactName} (${d.phone}) [${d.kind.name}]: ${d.status.name}")
            }
        }
    }

    fun toJson(
        incident: IncidentEntity,
        profile: EmergencyProfile,
        deliveries: List<AlertDeliveryEntity>,
    ): String {
        val fix = incident.toFix()
        val obj = JSONObject().apply {
            put("incident_id", incident.id)
            put("trip_id", incident.tripId ?: JSONObject.NULL)
            put("detected_at", incident.detectedAt)
            put("resolved_at", incident.resolvedAt ?: JSONObject.NULL)
            put("status", incident.status.name.lowercase(Locale.US))
            put("source", incident.source.name.lowercase(Locale.US))
            put("is_test", incident.isTest)
            put("user_response", incident.userResponse.name.lowercase(Locale.US))
            put("trip_type", incident.tripType.name.lowercase(Locale.US))
            put("crash_confidence", incident.crashConfidence)
            put("impact_g", incident.impactG)
            put("speed_before_kmh", incident.speedBeforeKmh)
            put("orientation_change_deg", incident.orientationChangeDeg)
            put("latitude", fix?.latitude ?: JSONObject.NULL)
            put("longitude", fix?.longitude ?: JSONObject.NULL)
            put("accuracy_m", fix?.accuracyM ?: JSONObject.NULL)
            put("location_is_last_known", fix?.isLastKnown ?: false)
            put("maps_link", fix?.mapsLink ?: JSONObject.NULL)
            put("battery_level", incident.batteryLevel)
            put("network_type", incident.networkType)
            put("notes", incident.notes)
            put(
                "user_profile",
                JSONObject().apply {
                    put("name", profile.name)
                    if (profile.shareBloodGroup) put("blood_group", profile.bloodGroup)
                    if (profile.shareAllergies) put("allergies", profile.allergies)
                    if (profile.shareConditions) put("conditions", profile.conditions)
                    if (profile.shareMedications) put("medications", profile.medications)
                }
            )
            put(
                "contacts_notified",
                JSONArray().apply {
                    deliveries.forEach { d ->
                        put(
                            JSONObject().apply {
                                put("contact_id", d.contactId)
                                put("name", d.contactName)
                                put("phone", d.phone)
                                put("kind", d.kind.name.lowercase(Locale.US))
                                put("channel", d.channel)
                                put("status", d.status.name.lowercase(Locale.US))
                                put("attempts", d.attempts)
                            }
                        )
                    }
                }
            )
        }
        return obj.toString(2)
    }
}
