package com.dataveda.crashguard.core.emergency

import com.dataveda.crashguard.domain.models.EmergencyProfile
import com.dataveda.crashguard.domain.models.EscalationReason
import com.dataveda.crashguard.domain.models.GeoFix
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Builds SMS/plain-text alert bodies. Pure so it can be unit-tested. English by design: contacts may use any language. */
object AlertMessageBuilder {

    const val TEST_BANNER = "TEST ALERT — No emergency. Please ignore."

    fun timestamp(millis: Long, tz: TimeZone = TimeZone.getDefault()): String =
        SimpleDateFormat("dd MMM yyyy, hh:mm a z", Locale.US).apply { timeZone = tz }.format(Date(millis))

    fun statusText(reason: EscalationReason): String = when (reason) {
        EscalationReason.UNRESPONSIVE_AFTER_CRASH -> "Unresponsive after possible crash"
        EscalationReason.USER_REQUESTED_HELP -> "Needs help"
        EscalationReason.MANUAL_SOS -> "SOS triggered manually"
    }

    fun emergency(
        profile: EmergencyProfile,
        detectedAt: Long,
        reason: EscalationReason,
        fix: GeoFix?,
        batteryPercent: Int,
        networkType: String,
        notes: String = "",
        tz: TimeZone = TimeZone.getDefault(),
    ): String = buildString {
        appendLine(if (reason == EscalationReason.MANUAL_SOS) "RAKSHAKAVACH SOS ALERT" else "RAKSHAKAVACH EMERGENCY ALERT")
        appendLine()
        appendLine(
            if (reason == EscalationReason.MANUAL_SOS) "An emergency SOS was triggered."
            else "A possible road crash was detected."
        )
        appendLine()
        appendLine("Name: ${profile.name.ifBlank { "RakshaKavach user" }}")
        appendLine("Time: ${timestamp(detectedAt, tz)}")
        appendLine("Location: ${locationLine(fix, tz)}")
        appendLine("Status: ${statusText(reason)}")
        appendLine("Battery: ${if (batteryPercent in 0..100) "$batteryPercent%" else "Unknown"}")
        appendLine("Network: ${if (networkType == "none") "Unavailable" else "Available ($networkType)"}")
        medicalLines(profile).forEach { appendLine(it) }
        if (notes.isNotBlank()) appendLine("Notes: $notes")
        appendLine()
        appendLine("Location updates will follow by SMS while sharing is active.")
        appendLine("If they do not respond, call 112.")
        appendLine()
        append("This is an automated alert from RakshaKavach.")
    }

    fun locationUpdate(profile: EmergencyProfile, fix: GeoFix, tz: TimeZone = TimeZone.getDefault()): String =
        "RAKSHAKAVACH LOCATION UPDATE\n${profile.name.ifBlank { "RakshaKavach user" }}: ${fix.mapsLink}\n" +
            "As of ${timestamp(fix.timeMillis, tz)}"

    fun safe(profile: EmergencyProfile, now: Long, tz: TimeZone = TimeZone.getDefault()): String =
        "RAKSHAKAVACH UPDATE\n${profile.name.ifBlank { "RakshaKavach user" }} has marked themselves SAFE " +
            "at ${timestamp(now, tz)}. Location sharing has stopped."

    fun test(profile: EmergencyProfile, now: Long, fix: GeoFix?, tz: TimeZone = TimeZone.getDefault()): String = buildString {
        appendLine(TEST_BANNER)
        appendLine()
        appendLine("This is how a RakshaKavach emergency alert from ${profile.name.ifBlank { "this user" }} will look.")
        appendLine("Time: ${timestamp(now, tz)}")
        appendLine("Location: ${locationLine(fix, tz)}")
        appendLine()
        append(TEST_BANNER)
    }

    private fun locationLine(fix: GeoFix?, tz: TimeZone): String = when {
        fix == null -> "Unavailable"
        fix.isLastKnown -> "${fix.mapsLink} (last known, ${timestamp(fix.timeMillis, tz)})"
        else -> fix.mapsLink + (fix.accuracyM?.let { " (±${it.toInt()} m)" } ?: "")
    }

    fun medicalLines(p: EmergencyProfile): List<String> = buildList {
        if (p.shareBloodGroup && p.bloodGroup.isNotBlank()) add("Blood group: ${p.bloodGroup}")
        if (p.shareAllergies && p.allergies.isNotBlank()) add("Allergies: ${p.allergies}")
        if (p.shareConditions && p.conditions.isNotBlank()) add("Conditions: ${p.conditions}")
        if (p.shareMedications && p.medications.isNotBlank()) add("Medications: ${p.medications}")
    }
}
