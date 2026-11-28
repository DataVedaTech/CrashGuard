package com.dataveda.crashguard.core.trip

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.dataveda.crashguard.R
import com.dataveda.crashguard.core.emergency.EmergencyEscalationManager
import com.dataveda.crashguard.core.location.LocationService
import com.dataveda.crashguard.core.notifications.Notifier
import com.dataveda.crashguard.core.sensors.CrashDetectionEngine
import com.dataveda.crashguard.core.sensors.DetectionConfig
import com.dataveda.crashguard.core.sensors.SensorMonitor
import com.dataveda.crashguard.core.utils.DeviceStatusProvider
import com.dataveda.crashguard.data.local.SettingsRepository
import com.dataveda.crashguard.data.local.TripEntity
import com.dataveda.crashguard.data.repository.ProfileRepository
import com.dataveda.crashguard.data.repository.TripRepository
import com.dataveda.crashguard.di.AppScope
import com.dataveda.crashguard.domain.models.TripRole
import com.dataveda.crashguard.domain.models.TripStatus
import com.dataveda.crashguard.domain.models.TripType
import com.dataveda.crashguard.domain.models.VehicleType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import kotlin.random.Random

/**
 * Foreground service active only during a trip. Confirms driving via GPS speed
 * (>= 10 km/h for 30 s in AUTO mode), feeds sensors + speed into [CrashDetectionEngine],
 * and maintains the trip summary. Stops all high-frequency monitoring when the trip ends.
 */
@AndroidEntryPoint
class TripMonitoringService : LifecycleService() {

    @Inject lateinit var tripManager: TripDetectionManager
    @Inject lateinit var escalation: EmergencyEscalationManager
    @Inject lateinit var sensors: SensorMonitor
    @Inject lateinit var location: LocationService
    @Inject lateinit var trips: TripRepository
    @Inject lateinit var profiles: ProfileRepository
    @Inject lateinit var device: DeviceStatusProvider
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var notifier: Notifier
    @Inject @AppScope lateinit var appScope: CoroutineScope

    private var running = false
    private var mode = TripMode.AUTO
    private var engine: CrashDetectionEngine? = null
    private var tripId: String? = null
    private var startedAt = 0L
    private var distanceKm = 0.0
    private var maxSpeed = 0.0
    private var lastLocation: Location? = null
    private var lastSpeedT = 0L
    private var aboveSince: Long? = null
    private var lowSince: Long? = null
    private var lastPersist = 0L
    private var tripType = TripType.UNKNOWN
    private var speedJob: Job? = null
    private var confirmTimeoutJob: Job? = null
    private val persistMutex = Mutex()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_STOP -> finishTrip()
            else -> {
                val requested = intent?.getStringExtra(EXTRA_MODE)?.let { runCatching { TripMode.valueOf(it) }.getOrNull() } ?: TripMode.AUTO
                begin(requested)
            }
        }
        return START_REDELIVER_INTENT
    }

    private fun begin(requested: TripMode) {
        if (running) {
            // Upgrade an unconfirmed auto trip to manual/simulated immediately.
            if (tripId == null && requested != TripMode.AUTO) {
                mode = requested
                confirmTimeoutJob?.cancel()
                confirmTimeoutJob = null
                if (requested == TripMode.SIMULATED) {
                    speedJob?.cancel()
                    startSimulatedSpeed()
                }
                confirmTrip()
            }
            return
        }
        mode = requested
        startedAt = System.currentTimeMillis()
        if (!startForegroundSafe(confirming = mode == TripMode.AUTO)) {
            notifier.showWarning(getString(R.string.warn_location_title), getString(R.string.warn_location_body))
            stopSelf()
            return
        }
        running = true

        lifecycleScope.launch {
            val cfg = settings.current()
            tripType = when (profiles.get().vehicleType) {
                VehicleType.CAR -> TripType.CAR
                VehicleType.TWO_WHEELER -> TripType.TWO_WHEELER
                VehicleType.OTHER -> TripType.UNKNOWN
            }
            val e = CrashDetectionEngine(
                config = DetectionConfig(
                    impactThresholdG = cfg.impactThresholdG.toDouble(),
                    minSpeedBeforeKmh = cfg.minSpeedBeforeKmh.toDouble(),
                    speedDropKmh = cfg.speedDropKmh.toDouble(),
                    rotationThresholdDps = cfg.rotationThresholdDegPerSec.toDouble(),
                ),
                bluetoothCar = { device.isConnectedToCarBluetooth() },
                onEvent = { event -> lifecycleScope.launch { onCrash(event) } },
            )
            engine = e
            sensors.start(
                onAccel = { t, x, y, z -> e.onAccelerometer(t, x, y, z) },
                onGyro = { t, x, y, z -> e.onGyroscope(t, x, y, z) },
            )
            tripManager.setInteractionListener { tMs ->
                sensors.post { e.onUserInteraction(tMs) }
            }
            if (!sensors.hasAccelerometer) {
                notifier.showWarning(getString(R.string.warn_sensor_title), getString(R.string.warn_sensor_body))
            }

            if (mode == TripMode.SIMULATED) startSimulatedSpeed() else startGps()

            if (mode == TripMode.AUTO) {
                tripManager.set(TripState.Confirming(startedAt))
                confirmTimeoutJob = lifecycleScope.launch {
                    delay(CONFIRM_TIMEOUT_MS)
                    if (tripId == null) {
                        Log.i(TAG, "Trip not confirmed by GPS; stopping")
                        finishTrip()
                    }
                }
            } else {
                confirmTrip()
            }
        }
    }

    private fun startForegroundSafe(confirming: Boolean): Boolean = try {
        val fgsType = if (Build.VERSION.SDK_INT >= 29 && location.hasPermission()) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        } else 0
        ServiceCompat.startForeground(
            this, Notifier.ID_TRIP,
            notifier.tripNotification(startedAt, confirming, mode == TripMode.SIMULATED),
            fgsType,
        )
        true
    } catch (e: Exception) {
        Log.w(TAG, "startForeground failed", e)
        false
    }

    private fun startGps() {
        if (!location.hasPermission()) {
            notifier.showWarning(getString(R.string.warn_location_title), getString(R.string.warn_location_body))
            return
        }
        speedJob?.cancel()
        speedJob = lifecycleScope.launch {
            location.updates(GPS_INTERVAL_MS).collect { loc ->
                val kmh = if (loc.hasSpeed()) loc.speed * 3.6 else {
                    val prev = lastLocation
                    if (prev != null && loc.time > prev.time) prev.distanceTo(loc) / ((loc.time - prev.time) / 1000.0) * 3.6 else 0.0
                }
                onSpeed(System.currentTimeMillis(), kmh, loc)
            }
        }
    }

    private fun startSimulatedSpeed() {
        speedJob?.cancel()
        speedJob = lifecycleScope.launch {
            while (isActive) {
                onSpeed(System.currentTimeMillis(), 40.0 + Random.nextDouble(-3.0, 3.0), null)
                delay(GPS_INTERVAL_MS)
            }
        }
    }

    private fun onSpeed(t: Long, kmh: Double, loc: Location?) {
        engine?.let { e -> sensors.post { e.onSpeed(t, kmh) } }

        if (tripId == null) {
            tripManager.update { if (it is TripState.Confirming) it.copy(speedKmh = kmh) else it }
            if (kmh >= CONFIRM_SPEED_KMH) {
                val since = aboveSince ?: t.also { aboveSince = it }
                if (t - since >= CONFIRM_DURATION_MS) confirmTrip()
            } else aboveSince = null
            lastLocation = loc ?: lastLocation
            return
        }

        if (loc != null) {
            val prev = lastLocation
            if (prev != null && (!loc.hasAccuracy() || loc.accuracy < 50f)) distanceKm += prev.distanceTo(loc) / 1000.0
            lastLocation = loc
        } else if (lastSpeedT > 0) {
            distanceKm += kmh * ((t - lastSpeedT) / 3_600_000.0)
        }
        lastSpeedT = t
        maxSpeed = maxOf(maxSpeed, kmh)
        tripManager.update {
            if (it is TripState.Active) it.copy(speedKmh = kmh, distanceKm = distanceKm, maxSpeedKmh = maxSpeed) else it
        }

        if (mode == TripMode.AUTO) {
            if (kmh < STOPPED_SPEED_KMH) {
                val since = lowSince ?: t.also { lowSince = it }
                if (t - since >= AUTO_STOP_IDLE_MS) finishTrip()
            } else lowSince = null
        }

        if (t - lastPersist >= PERSIST_INTERVAL_MS) {
            lastPersist = t
            persistTrip(TripStatus.ACTIVE)
        }
    }

    private fun confirmTrip() {
        if (tripId != null) return
        confirmTimeoutJob?.cancel()
        confirmTimeoutJob = null
        val id = UUID.randomUUID().toString()
        tripId = id
        val bt = device.isConnectedToCarBluetooth()
        val role = when {
            mode != TripMode.AUTO -> TripRole.DRIVER
            bt -> TripRole.DRIVER
            else -> TripRole.UNKNOWN
        }
        tripManager.set(TripState.Active(id, startedAt, mode, role = role, bluetoothCar = bt))
        escalation.activeTripId = id
        val initialTrip = TripEntity(
            id = id, startedAt = startedAt, tripType = tripType, role = role,
            simulated = mode == TripMode.SIMULATED, manual = mode == TripMode.MANUAL,
        )
        appScope.launch {
            persistMutex.withLock {
                trips.insert(initialTrip)
            }
        }
        startForegroundSafe(confirming = false)
        if (role == TripRole.UNKNOWN) notifier.showRolePrompt()
    }

    private fun onCrash(event: com.dataveda.crashguard.core.sensors.CrashEvent) {
        val id = tripId ?: return // only escalate within a confirmed trip
        val role = (tripManager.state.value as? TripState.Active)?.role ?: TripRole.UNKNOWN
        escalation.onCrashEvent(event, id, role)
    }

    private fun persistTrip(status: TripStatus) {
        val id = tripId ?: return
        val role = (tripManager.state.value as? TripState.Active)?.role ?: TripRole.UNKNOWN
        val snapshot = Triple(distanceKm, maxSpeed, role)
        val ended = if (status == TripStatus.COMPLETED) System.currentTimeMillis() else null
        appScope.launch {
            persistMutex.withLock {
                val existing = trips.get(id) ?: return@withLock
                trips.update(
                    existing.copy(
                        distanceKm = snapshot.first, maxSpeedKmh = snapshot.second, role = snapshot.third,
                        status = status, endedAt = ended,
                    )
                )
            }
        }
    }

    private fun finishTrip() {
        if (tripId != null) persistTrip(TripStatus.COMPLETED)
        cleanup()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun cleanup() {
        speedJob?.cancel()
        speedJob = null
        confirmTimeoutJob?.cancel()
        confirmTimeoutJob = null
        tripManager.setInteractionListener(null)
        sensors.stop()
        engine = null
        tripId = null
        running = false
        aboveSince = null
        lowSince = null
        lastLocation = null
        lastSpeedT = 0L
        distanceKm = 0.0
        maxSpeed = 0.0
        escalation.activeTripId = null
        tripManager.set(TripState.Idle)
        notifier.cancelRolePrompt()
    }

    override fun onDestroy() {
        if (tripId != null) persistTrip(TripStatus.COMPLETED)
        if (running) cleanup()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "TripService"
        const val ACTION_START = "com.dataveda.crashguard.TRIP_START"
        const val ACTION_STOP = "com.dataveda.crashguard.TRIP_STOP"
        const val EXTRA_MODE = "mode"

        const val GPS_INTERVAL_MS = 1_000L
        const val CONFIRM_SPEED_KMH = 10.0
        const val CONFIRM_DURATION_MS = 30_000L
        const val CONFIRM_TIMEOUT_MS = 5 * 60_000L
        const val STOPPED_SPEED_KMH = 3.0
        const val AUTO_STOP_IDLE_MS = 10 * 60_000L
        const val PERSIST_INTERVAL_MS = 15_000L

        fun start(context: Context, mode: TripMode) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, TripMonitoringService::class.java).setAction(ACTION_START).putExtra(EXTRA_MODE, mode.name),
            )
        }

        fun stop(context: Context) {
            context.startService(Intent(context, TripMonitoringService::class.java).setAction(ACTION_STOP))
        }
    }
}
