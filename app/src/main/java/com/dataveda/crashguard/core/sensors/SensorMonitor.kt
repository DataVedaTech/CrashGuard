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
import javax.inject.Inject
import javax.inject.Singleton

/** Thin wrapper around SensorManager that forwards accelerometer + gyroscope samples on a background thread. */
@Singleton
class SensorMonitor @Inject constructor(@ApplicationContext context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    val hasAccelerometer get() = accelerometer != null
    val hasGyroscope get() = gyroscope != null

    private var thread: HandlerThread? = null
    private var listener: SensorEventListener? = null

    /**
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
                    Sensor.TYPE_ACCELEROMETER -> onAccel(ts, e.values[0], e.values[1], e.values[2])
                    Sensor.TYPE_GYROSCOPE -> onGyro(ts, e.values[0], e.values[1], e.values[2])
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        listener = l
        accelerometer?.let { sensorManager.registerListener(l, it, SensorManager.SENSOR_DELAY_GAME, handler) }
        gyroscope?.let { sensorManager.registerListener(l, it, SensorManager.SENSOR_DELAY_GAME, handler) }
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
    }
}
