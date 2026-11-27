package com.dataveda.crashguard.core.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.sqrt

data class LiveSensorTelemetry(
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f,
    val accelG: Double = 1.0,
    val peakG: Double = 1.0,
    val gyroDps: Double = 0.0,
    val peakGyroDps: Double = 0.0,
    val sampleCount: Long = 0,
    val active: Boolean = false,
)

/**
 * Hardware SensorManager wrapper that:
 * 1) Feeds high-frequency accelerometer + gyroscope samples to [CrashDetectionEngine] on a dedicated HandlerThread.
 * 2) Publishes real-time hardware telemetry ([telemetry]) so users in Tester Mode or Live Mode can verify
 *    their phone's physical accelerometer and gyroscope are actively streaming.
 */
@Singleton
class SensorMonitor @Inject constructor(@ApplicationContext context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    val hasAccelerometer get() = accelerometer != null
    val hasGyroscope get() = gyroscope != null

    private val _telemetry = MutableStateFlow(LiveSensorTelemetry())
    val telemetry: StateFlow<LiveSensorTelemetry> = _telemetry.asStateFlow()

    private var thread: HandlerThread? = null
    private var listener: SensorEventListener? = null
    private var previewListener: SensorEventListener? = null
    private var peakG = 1.0
    private var peakDps = 0.0
    private var samples = 0L

    fun resetPeaks() {
        peakG = 1.0
        peakDps = 0.0
        _telemetry.value = _telemetry.value.copy(peakG = 1.0, peakGyroDps = 0.0)
    }

    /**
     * Starts hardware sensor streaming for trip monitoring and crash detection.
     * Timestamps delivered to callbacks are wall-clock millis so they align with GPS updates.
     */
    fun start(
        onAccel: (Long, Float, Float, Float) -> Unit,
        onGyro: (Long, Float, Float, Float) -> Unit,
    ) {
        stop()
        val t = HandlerThread("cg-sensors").also { it.start() }
        thread = t
        val handler = Handler(t.looper)
        val offset = System.currentTimeMillis() - SystemClock.elapsedRealtime()
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val ts = e.timestamp / 1_000_000L + offset
                when (e.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        val x = e.values[0]; val y = e.values[1]; val z = e.values[2]
                        updateAccelTelemetry(x, y, z)
                        onAccel(ts, x, y, z)
                    }
                    Sensor.TYPE_GYROSCOPE -> {
                        val x = e.values[0]; val y = e.values[1]; val z = e.values[2]
                        updateGyroTelemetry(x, y, z)
                        onGyro(ts, x, y, z)
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        listener = l
        accelerometer?.let { sensorManager.registerListener(l, it, SensorManager.SENSOR_DELAY_GAME, handler) }
        gyroscope?.let { sensorManager.registerListener(l, it, SensorManager.SENSOR_DELAY_GAME, handler) }
        _telemetry.value = _telemetry.value.copy(active = true)
    }

    /**
     * Lightweight hardware sensor listener for the Tester Mode Live Sensor Lab when a trip is not running,
     * optionally invoking [onShakeCrash] if physical g-force exceeds [shakeThresholdG].
     */
    fun startLiveInspector(
        shakeThresholdG: Double? = null,
        onShakeCrash: ((Double, Double) -> Unit)? = null,
    ) {
        stopLiveInspector()
        var lastTrigger = 0L
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                when (e.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        val g = updateAccelTelemetry(e.values[0], e.values[1], e.values[2])
                        val now = System.currentTimeMillis()
                        if (shakeThresholdG != null && onShakeCrash != null && g >= shakeThresholdG && now - lastTrigger > 8_000L) {
                            lastTrigger = now
                            onShakeCrash(g, peakDps)
                        }
                    }
                    Sensor.TYPE_GYROSCOPE -> updateGyroTelemetry(e.values[0], e.values[1], e.values[2])
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        previewListener = l
        accelerometer?.let { sensorManager.registerListener(l, it, SensorManager.SENSOR_DELAY_UI) }
        gyroscope?.let { sensorManager.registerListener(l, it, SensorManager.SENSOR_DELAY_UI) }
        _telemetry.value = _telemetry.value.copy(active = true)
    }

    fun stopLiveInspector() {
        previewListener?.let { sensorManager.unregisterListener(it) }
        previewListener = null
        if (listener == null) {
            _telemetry.value = _telemetry.value.copy(active = false)
        }
    }

    private fun updateAccelTelemetry(x: Float, y: Float, z: Float): Double {
        val g = sqrt((x * x + y * y + z * z).toDouble()) / CrashDetectionEngine.GRAVITY
        peakG = max(peakG, g)
        samples++
        if (samples % 4L == 0L) {
            _telemetry.value = _telemetry.value.copy(
                accelX = x, accelY = y, accelZ = z,
                accelG = g, peakG = peakG, sampleCount = samples, active = true,
            )
        }
        return g
    }

    private fun updateGyroTelemetry(x: Float, y: Float, z: Float) {
        val dps = Math.toDegrees(sqrt((x * x + y * y + z * z).toDouble()))
        peakDps = max(peakDps, dps)
        if (samples % 4L == 0L) {
            _telemetry.value = _telemetry.value.copy(gyroDps = dps, peakGyroDps = peakDps, active = true)
        }
    }

    /** Runs [block] on the sensor thread so engine access stays single-threaded. */
    fun post(block: () -> Unit) {
        thread?.looper?.let { Handler(it).post(block) } ?: block()
    }

    fun stop() {
        listener?.let { sensorManager.unregisterListener(it) }
        listener = null
        thread?.quitSafely()
        thread = null
        if (previewListener == null) {
            _telemetry.value = _telemetry.value.copy(active = false)
        }
    }
}
