package com.dataveda.crashguard

import com.dataveda.crashguard.core.emergency.AlertMessageBuilder
import com.dataveda.crashguard.core.emergency.CountdownTimer
import com.dataveda.crashguard.core.emergency.IncidentReportExporter
import com.dataveda.crashguard.core.emergency.withFix
import com.dataveda.crashguard.core.sensors.AlertBand
import com.dataveda.crashguard.core.sensors.ConfidenceScorer
import com.dataveda.crashguard.core.sensors.CrashDetectionEngine
import com.dataveda.crashguard.core.sensors.CrashEvent
import com.dataveda.crashguard.core.sensors.CrashSignals
import com.dataveda.crashguard.core.sensors.SimulationLevel
import com.dataveda.crashguard.data.local.AlertDeliveryEntity
import com.dataveda.crashguard.data.local.IncidentEntity
import com.dataveda.crashguard.data.repository.ContactRepository
import com.dataveda.crashguard.domain.models.AlertKind
import com.dataveda.crashguard.domain.models.DeliveryStatus
import com.dataveda.crashguard.domain.models.EmergencyProfile
import com.dataveda.crashguard.domain.models.EscalationReason
import com.dataveda.crashguard.domain.models.GeoFix
import com.dataveda.crashguard.domain.models.IncidentSource
import com.dataveda.crashguard.domain.models.IncidentStatus
import com.dataveda.crashguard.domain.models.UserResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests covering Spec Section 12.1:
 * - Crash-detection rules & false-positive gates (low speed, phone drop, single vibration spike)
 * - Confidence scoring & bands (0-39, 40-59, 60-79, 80-100)
 * - Countdown timer tick & cancellation logic
 * - Incident creation & plain-text / JSON report export
 * - Contact-alert payload & medical consent filtering
 * - Location fallback (live GPS vs last known location)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CrashGuardUnitTest {

    @Test
    fun `confidence scorer matches spec weights and bands`() {
        val max = CrashSignals(
            highImpact = true,
            speedDrop = true,
            abnormalRotation = true,
            stillness = true,
            noInteraction = true,
            bluetoothCar = true,
        )
        assertEquals(100, ConfidenceScorer.score(max))
        assertEquals(AlertBand.FULL_SCREEN_PREPARE, ConfidenceScorer.band(80))
        assertEquals(AlertBand.FULL_SCREEN, ConfidenceScorer.band(65))
        assertEquals(AlertBand.NOTIFY, ConfidenceScorer.band(50))
        assertEquals(AlertBand.IGNORE, ConfidenceScorer.band(30))
    }

    @Test
    fun `high-confidence crash detected when multi-sensor signals align`() {
        var detected: CrashEvent? = null
        val engine = CrashDetectionEngine(
            bluetoothCar = { true },
            onEvent = { detected = it },
        )

        // Driving at 48 km/h prior to impact
        engine.onSpeed(1_000L, 48.0)
        engine.onSpeed(2_000L, 48.0)

        // Sustained high impact (>= 2 samples within 250ms above 4.5g) at t=3_000
        engine.onAccelMagnitudeG(3_000L, 6.2)
        engine.onAccelMagnitudeG(3_050L, 6.8)

        // Sudden rotation (250 deg/s -> ~4.36 rad/s)
        engine.onGyroscope(3_100L, 4.5f, 0f, 0f)

        // Sudden speed drop within 3 seconds
        engine.onSpeed(4_000L, 2.0)

        // Post-event stillness near 1.0g during the last 2 seconds of the 5s window
        for (dt in 1_000L..5_100L step 200L) {
            engine.onAccelMagnitudeG(3_050L + dt, 1.0)
        }

        val event = detected
        assertNotNull("Expected crash event to be emitted", event)
        assertEquals(100, event!!.confidence)
        assertEquals(AlertBand.FULL_SCREEN_PREPARE, event.band)
        assertTrue(event.signals.highImpact)
        assertTrue(event.signals.speedDrop)
        assertTrue(event.signals.abnormalRotation)
        assertTrue(event.signals.stillness)
        assertTrue(event.signals.noInteraction)
        assertTrue(event.signals.bluetoothCar)
    }

    @Test
    fun `low speed before impact is rejected as false positive`() {
        var detected: CrashEvent? = null
        val engine = CrashDetectionEngine(onEvent = { detected = it })

        // Speed is only 5 km/h (< 15 km/h threshold)
        engine.onSpeed(1_000L, 5.0)
        engine.onAccelMagnitudeG(2_000L, 7.0)
        engine.onAccelMagnitudeG(2_050L, 7.2)
        engine.onAccelMagnitudeG(8_000L, 1.0)

        assertNull(detected)
        assertEquals(1, engine.rejectedCandidates)
    }

    @Test
    fun `phone drop free-fall before impact is rejected`() {
        var detected: CrashEvent? = null
        val engine = CrashDetectionEngine(onEvent = { detected = it })

        engine.onSpeed(500L, 35.0)
        // Free-fall (< 0.35g for >= 120ms) just before impact
        engine.onAccelMagnitudeG(1_000L, 0.10)
        engine.onAccelMagnitudeG(1_080L, 0.12)
        engine.onAccelMagnitudeG(1_160L, 0.11)
        // Impact on floor
        engine.onAccelMagnitudeG(1_220L, 6.0)
        engine.onAccelMagnitudeG(1_260L, 6.5)

        assertNull(detected)
        assertEquals(1, engine.rejectedCandidates)
    }

    @Test
    fun `single brief vibration spike is ignored`() {
        var detected: CrashEvent? = null
        val engine = CrashDetectionEngine(onEvent = { detected = it })

        engine.onSpeed(500L, 40.0)
        // Only 1 sample above threshold
        engine.onAccelMagnitudeG(1_000L, 5.5)
        engine.onAccelMagnitudeG(1_100L, 1.0)
        engine.onAccelMagnitudeG(7_000L, 1.0)

        assertNull(detected)
    }

    @Test
    fun `countdown timer ticks down and triggers escalation unless cancelled`() = runTest {
        val ticks = mutableListOf<Int>()
        var finished = false
        val timer = CountdownTimer(this)

        timer.start(
            seconds = 5,
            onTick = { ticks += it },
            onFinish = { finished = true },
        )
        advanceTimeBy(5_000L)
        runCurrent()

        assertTrue(finished)
        assertEquals(listOf(5, 4, 3, 2, 1, 0), ticks)

        // Now verify cancellation midway ("I'm okay")
        val cancelledTicks = mutableListOf<Int>()
        var cancelledFinished = false
        timer.start(
            seconds = 45,
            onTick = { cancelledTicks += it },
            onFinish = { cancelledFinished = true },
        )
        advanceTimeBy(2_500L)
        runCurrent()
        timer.cancel()
        advanceTimeBy(60_000L)
        runCurrent()

        assertFalse(cancelledFinished)
        assertEquals(listOf(45, 44, 43), cancelledTicks)
    }

    @Test
    fun `alert message includes maps link, last-known fallback, and opted-in medical fields only`() {
        val profile = EmergencyProfile(
            name = "Aarav Sharma",
            bloodGroup = "O+",
            allergies = "Penicillin",
            conditions = "Asthma",
            shareBloodGroup = true,
            shareAllergies = false, // opted out
            shareConditions = true,
        )
        val liveFix = GeoFix(19.0760, 72.8777, 8f, 1_700_000_000_000L, isLastKnown = false)
        val msg = AlertMessageBuilder.emergency(
            profile = profile,
            detectedAt = 1_700_000_000_000L,
            reason = EscalationReason.UNRESPONSIVE_AFTER_CRASH,
            fix = liveFix,
            batteryPercent = 64,
            networkType = "mobile",
            notes = "I am injured.",
        )

        assertTrue(msg.contains("CRASHGUARD EMERGENCY ALERT"))
        assertTrue(msg.contains("Aarav Sharma"))
        assertTrue(msg.contains("https://maps.google.com/?q=19.076000,72.877700"))
        assertTrue(msg.contains("Blood group: O+"))
        assertTrue(msg.contains("Conditions: Asthma"))
        assertFalse("Opted-out allergy must not appear in alert", msg.contains("Penicillin"))
        assertTrue(msg.contains("Notes: I am injured."))

        // Last-known fallback indication
        val fallbackFix = liveFix.copy(isLastKnown = true)
        val fallbackMsg = AlertMessageBuilder.emergency(
            profile, 1_700_000_000_000L, EscalationReason.USER_REQUESTED_HELP, fallbackFix, 20, "none",
        )
        assertTrue(fallbackMsg.contains("last known"))
        assertTrue(fallbackMsg.contains("Network: Unavailable"))

        // Test SOS banner
        val testMsg = AlertMessageBuilder.test(profile, 1_700_000_000_000L, liveFix)
        assertTrue(testMsg.contains(AlertMessageBuilder.TEST_BANNER))
    }

    @Test
    fun `incident report exports structured plain text and delivery status`() {
        val fix = GeoFix(18.5204, 73.8567, 10f, 1_700_000_000_000L, isLastKnown = false)
        val incident = IncidentEntity(
            id = "inc-123",
            tripId = "trip-1",
            detectedAt = 1_700_000_000_000L,
            crashConfidence = 90,
            impactG = 7.8,
            speedBeforeKmh = 42.0,
            orientationChangeDeg = 95.0,
            userResponse = UserResponse.NEED_HELP,
            status = IncidentStatus.ACTIVE,
            source = IncidentSource.SIMULATION,
            batteryLevel = 80,
            networkType = "mobile",
            notes = "Need ambulance.",
        ).withFix(fix)

        val deliveries = listOf(
            AlertDeliveryEntity(
                id = 1,
                incidentId = "inc-123",
                contactId = "c1",
                contactName = "Mother",
                phone = "+919876543210",
                kind = AlertKind.INITIAL,
                status = DeliveryStatus.SENT,
                attempts = 1,
                message = "alert",
            )
        )
        val text = IncidentReportExporter.toPlainText(
            incident = incident,
            profile = EmergencyProfile(name = "Hrishi M", bloodGroup = "B+", shareBloodGroup = true),
            deliveries = deliveries,
        )

        assertTrue(text.contains("Incident ID: inc-123"))
        assertTrue(text.contains("Crash Confidence: 90/100"))
        assertTrue(text.contains("Mother (+919876543210) [INITIAL]: SENT"))
        assertEquals("+919876543210", ContactRepository.normalizePhone(" +91 98765-43210 "))

        val simHigh = CrashDetectionEngine.simulated(SimulationLevel.HIGH)
        assertEquals(90, simHigh.confidence)
    }
}
