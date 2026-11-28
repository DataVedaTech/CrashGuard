package com.dataveda.crashguard.core.sensors

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Tunable thresholds. Defaults follow the MVP spec (section 4.2). */
data class DetectionConfig(
    val impactThresholdG: Double = 4.5,
    val minSpeedBeforeKmh: Double = 15.0,
    val speedDropKmh: Double = 10.0,
    val speedDropWindowMs: Long = 3_000,
    val rotationThresholdDps: Double = 200.0,
    val stillnessWindowMs: Long = 5_000,
    /** Samples above threshold required within [spikeWindowMs] — rejects single vibration spikes. */
    val minImpactSamples: Int = 2,
    val spikeWindowMs: Long = 250,
    /** Phone-drop detection: free-fall (|a| < freeFallG) lasting at least freeFallMinMs just before impact. */
    val freeFallG: Double = 0.35,
    val freeFallMinMs: Long = 120,
    val freeFallLookbackMs: Long = 1_500,
    val speedLookbackMs: Long = 6_000,
    val stillnessStdDevG: Double = 0.07,
    val stillnessTailMs: Long = 2_000,
    val cooldownMs: Long = 30_000,
)

data class CrashSignals(
    val highImpact: Boolean,
    val speedDrop: Boolean,
    val abnormalRotation: Boolean,
    val stillness: Boolean,
    val noInteraction: Boolean,
    val bluetoothCar: Boolean,
)

data class CrashEvent(
    val timestampMs: Long,
    val confidence: Int,
    val impactG: Double,
    val speedBeforeKmh: Double,
    val speedAfterKmh: Double?,
    val peakRotationDps: Double,
    val orientationChangeDeg: Double,
    val signals: CrashSignals,
    val simulated: Boolean = false,
) {
    val band: AlertBand get() = ConfidenceScorer.band(confidence)

    fun summary(): String =
        "impact=%.2fg speedBefore=%.1f speedAfter=%s rot=%.0fdps orient=%.0fdeg signals=%s".format(
            java.util.Locale.US, impactG, speedBeforeKmh, speedAfterKmh?.let { "%.1f".format(java.util.Locale.US, it) } ?: "n/a",
            peakRotationDps, orientationChangeDeg, signals,
        )
}

enum class AlertBand { IGNORE, NOTIFY, FULL_SCREEN, FULL_SCREEN_PREPARE }

object ConfidenceScorer {
    const val W_IMPACT = 30
    const val W_SPEED_DROP = 20
    const val W_ROTATION = 15
    const val W_STILLNESS = 15
    const val W_NO_INTERACTION = 10
    const val W_BLUETOOTH = 10

    fun score(s: CrashSignals): Int {
        var total = 0
        if (s.highImpact) total += W_IMPACT
        if (s.speedDrop) total += W_SPEED_DROP
        if (s.abnormalRotation) total += W_ROTATION
        if (s.stillness) total += W_STILLNESS
        if (s.noInteraction) total += W_NO_INTERACTION
        if (s.bluetoothCar) total += W_BLUETOOTH
        return total.coerceIn(0, 100)
    }

    fun band(score: Int): AlertBand = when {
        score >= 80 -> AlertBand.FULL_SCREEN_PREPARE
        score >= 60 -> AlertBand.FULL_SCREEN
        score >= 40 -> AlertBand.NOTIFY
        else -> AlertBand.IGNORE
    }
}

/**
 * Rule-based multi-sensor crash detector.
 *
 * Pipeline: impact candidate (accelerometer) -> gating (speed before, phone-drop, spike filter)
 * -> evaluation window (speed drop, rotation, stillness, interaction) -> confidence score -> event.
 *
 * Pure Kotlin with caller-supplied timestamps so it can be unit tested and driven by simulations.
 * Not thread-safe: feed from a single thread.
 */
class CrashDetectionEngine(
    var config: DetectionConfig = DetectionConfig(),
    private val bluetoothCar: () -> Boolean = { false },
    private val onEvent: (CrashEvent) -> Unit,
) {
    private data class Sample(val t: Long, val v: Double)

    private val accel = ArrayDeque<Sample>()   // |a| in g
    private val gyro = ArrayDeque<Sample>()    // angular velocity in deg/s
    private val speeds = ArrayDeque<Sample>()  // km/h
    private var lastGyroT = -1L

    private var evaluating = false
    private var impactT = 0L
    private var peakG = 0.0
    private var speedBefore = 0.0
    private var peakRotation = 0.0
    private var orientationDeg = 0.0
    private var interacted = false
    private var cooldownUntil = 0L

    /** Number of impact candidates rejected by gating (useful for debugging/telemetry). */
    var rejectedCandidates = 0
        private set

    fun reset() {
        accel.clear(); gyro.clear(); speeds.clear(); evaluating = false; lastGyroT = -1L; cooldownUntil = 0L
    }

    fun onSpeed(tMs: Long, speedKmh: Double) {
        speeds.addLast(Sample(tMs, speedKmh))
        while (speeds.isNotEmpty() && tMs - speeds.first().t > config.speedLookbackMs + config.stillnessWindowMs) speeds.removeFirst()
    }

    fun onUserInteraction(tMs: Long) {
        if (evaluating && tMs >= impactT) interacted = true
    }

    /** Raw accelerometer values in m/s² (gravity included). */
    fun onAccelerometer(tMs: Long, x: Float, y: Float, z: Float) =
        onAccelMagnitudeG(tMs, sqrt((x * x + y * y + z * z).toDouble()) / GRAVITY)

    fun onAccelMagnitudeG(tMs: Long, g: Double) {
        accel.addLast(Sample(tMs, g))
        while (accel.isNotEmpty() && tMs - accel.first().t > BUFFER_MS) accel.removeFirst()

        if (evaluating) {
            peakG = max(peakG, g)
            if (tMs - impactT >= config.stillnessWindowMs) finalizeEvaluation(tMs)
            return
        }
        if (tMs < cooldownUntil || g < config.impactThresholdG) return

        val spikeCount = accel.count { tMs - it.t <= config.spikeWindowMs && it.v >= config.impactThresholdG }
        if (spikeCount < config.minImpactSamples) return

        val before = maxSpeedBefore(tMs)
        if (before < config.minSpeedBeforeKmh) { rejectCandidate(tMs); return }
        if (isPhoneDrop(tMs)) { rejectCandidate(tMs); return }

        evaluating = true
        impactT = tMs
        peakG = accel.filter { tMs - it.t <= config.spikeWindowMs }.maxOf { it.v }
        speedBefore = before
        // Include any gyroscope spike captured during the impact onset window (rollover often starts at sample #1).
        val onsetGyro = gyro.filter { tMs - it.t <= config.spikeWindowMs * 2 }.maxOfOrNull { it.v } ?: 0.0
        peakRotation = onsetGyro
        orientationDeg = (onsetGyro * 0.1).coerceAtMost(180.0)
        interacted = false
    }

    /** Gyroscope in rad/s. */
    fun onGyroscope(tMs: Long, x: Float, y: Float, z: Float) {
        val dps = Math.toDegrees(sqrt((x * x + y * y + z * z).toDouble()))
        val dt = if (lastGyroT < 0) 0.0 else min((tMs - lastGyroT) / 1000.0, 0.1)
        lastGyroT = tMs
        gyro.addLast(Sample(tMs, dps))
        while (gyro.isNotEmpty() && tMs - gyro.first().t > GYRO_BUFFER_MS) gyro.removeFirst()
        if (!evaluating) return
        peakRotation = max(peakRotation, dps)
        orientationDeg = min(orientationDeg + dps * dt, 720.0)
    }

    private fun rejectCandidate(tMs: Long) {
        rejectedCandidates++
        cooldownUntil = tMs + REJECT_COOLDOWN_MS
    }

    private fun maxSpeedBefore(tMs: Long): Double =
        speeds.filter { it.t <= tMs && tMs - it.t <= config.speedLookbackMs }.maxOfOrNull { it.v } ?: 0.0

    private fun isPhoneDrop(tMs: Long): Boolean {
        var runStart = -1L
        var longest = 0L
        for (s in accel) {
            if (tMs - s.t > config.freeFallLookbackMs || s.t >= tMs) continue
            if (s.v < config.freeFallG) {
                if (runStart < 0) runStart = s.t
                longest = max(longest, s.t - runStart)
            } else runStart = -1L
        }
        return longest >= config.freeFallMinMs
    }

    private fun finalizeEvaluation(tMs: Long) {
        evaluating = false
        cooldownUntil = tMs + config.cooldownMs

        val after = speeds.filter { it.t > impactT && it.t - impactT <= config.speedDropWindowMs }
        val minAfter = after.minOfOrNull { it.v }
        val speedDrop = minAfter != null && speedBefore - minAfter >= config.speedDropKmh

        val tail = accel.filter { tMs - it.t <= config.stillnessTailMs && it.t > impactT + 500 }
        val stillness = tail.size >= 5 && stdDev(tail.map { it.v }) <= config.stillnessStdDevG &&
            abs(tail.map { it.v }.average() - 1.0) < 0.2

        val signals = CrashSignals(
            highImpact = peakG >= config.impactThresholdG,
            speedDrop = speedDrop,
            abnormalRotation = peakRotation >= config.rotationThresholdDps,
            stillness = stillness,
            noInteraction = !interacted,
            bluetoothCar = runCatching { bluetoothCar() }.getOrDefault(false),
        )
        onEvent(
            CrashEvent(
                timestampMs = impactT,
                confidence = ConfidenceScorer.score(signals),
                impactG = peakG,
                speedBeforeKmh = speedBefore,
                speedAfterKmh = minAfter,
                peakRotationDps = peakRotation,
                orientationChangeDeg = orientationDeg,
                signals = signals,
            )
        )
    }

    private fun stdDev(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        return sqrt(values.sumOf { (it - mean) * (it - mean) } / values.size)
    }

    companion object {
        const val GRAVITY = 9.80665
        private const val BUFFER_MS = 8_000L
        private const val GYRO_BUFFER_MS = 2_000L
        private const val REJECT_COOLDOWN_MS = 2_000L

        /** Synthetic events for the developer simulation panel. */
        fun simulated(level: SimulationLevel, nowMs: Long = System.currentTimeMillis()): CrashEvent {
            val signals = when (level) {
                SimulationLevel.LOW -> CrashSignals(true, false, false, false, false, false)          // 30
                SimulationLevel.MEDIUM -> CrashSignals(true, true, false, false, false, false)        // 50
                SimulationLevel.HIGH -> CrashSignals(true, true, true, true, true, false)             // 90
            }
            return CrashEvent(
                timestampMs = nowMs,
                confidence = ConfidenceScorer.score(signals),
                impactG = when (level) { SimulationLevel.LOW -> 4.7; SimulationLevel.MEDIUM -> 5.2; SimulationLevel.HIGH -> 7.8 },
                speedBeforeKmh = 42.0,
                speedAfterKmh = if (signals.speedDrop) 0.0 else 38.0,
                peakRotationDps = if (signals.abnormalRotation) 340.0 else 40.0,
                orientationChangeDeg = if (signals.abnormalRotation) 95.0 else 5.0,
                signals = signals,
                simulated = true,
            )
        }
    }
}

enum class SimulationLevel { LOW, MEDIUM, HIGH }
