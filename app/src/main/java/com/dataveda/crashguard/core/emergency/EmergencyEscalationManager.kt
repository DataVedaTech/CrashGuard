package com.dataveda.crashguard.core.emergency

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import com.dataveda.crashguard.core.notifications.Notifier
import com.dataveda.crashguard.core.location.LocationService
import com.dataveda.crashguard.core.sensors.AlertBand
import com.dataveda.crashguard.core.sensors.CrashDetectionEngine
import com.dataveda.crashguard.core.sensors.CrashEvent
import com.dataveda.crashguard.core.sensors.SimulationLevel
import com.dataveda.crashguard.core.utils.DeviceStatusProvider
import com.dataveda.crashguard.data.local.ContactEntity
import com.dataveda.crashguard.data.local.IncidentEntity
import com.dataveda.crashguard.data.local.SettingsRepository
import com.dataveda.crashguard.data.repository.ContactRepository
import com.dataveda.crashguard.data.repository.IncidentRepository
import com.dataveda.crashguard.data.repository.ProfileRepository
import com.dataveda.crashguard.data.repository.TripRepository
import com.dataveda.crashguard.di.AppScope
import com.dataveda.crashguard.domain.models.AlertKind
import com.dataveda.crashguard.domain.models.EscalationReason
import com.dataveda.crashguard.domain.models.GeoFix
import com.dataveda.crashguard.domain.models.IncidentSource
import com.dataveda.crashguard.domain.models.IncidentStatus
import com.dataveda.crashguard.domain.models.TripRole
import com.dataveda.crashguard.domain.models.TripType
import com.dataveda.crashguard.domain.models.UserResponse
import com.dataveda.crashguard.domain.models.VehicleType
import com.dataveda.crashguard.ui.crash.CrashAlertActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed interface EmergencyState {
    data object Idle : EmergencyState

    data class CheckIn(
        val incidentId: String,
        val confidence: Int,
        val secondsLeft: Int,
        val totalSeconds: Int,
        val fix: GeoFix? = null,
        val locating: Boolean = true,
    ) : EmergencyState

    data class Escalated(
        val incidentId: String,
        val reason: EscalationReason,
        val fix: GeoFix?,
        val locating: Boolean,
        val sharingActive: Boolean,
        val sharingEndsAt: Long,
        val autoCallSecondsLeft: Int? = null,
        val alertsDispatched: Boolean = false,
    ) : EmergencyState
}

/** Coroutine countdown that ticks once per second. Extracted for unit testing. */
class CountdownTimer(private val scope: CoroutineScope) {
    private var job: Job? = null
    val isRunning: Boolean get() = job?.isActive == true

    fun start(seconds: Int, onTick: (Int) -> Unit, onFinish: suspend () -> Unit) {
        cancel()
        job = scope.launch {
            for (s in seconds downTo 1) {
                onTick(s)
                delay(1_000)
            }
            onTick(0)
            job = null
            onFinish()
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}

/**
 * Owns the crash-response state machine:
 * Idle -> CheckIn (alarm + countdown) -> Escalated (contacts alerted, live location) -> Idle.
 * All transitions are serialized by a mutex and run in the application scope so they survive UI teardown.
 */
@Singleton
class EmergencyEscalationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @AppScope private val scope: CoroutineScope,
    private val incidents: IncidentRepository,
    private val trips: TripRepository,
    private val contacts: ContactRepository,
    private val profiles: ProfileRepository,
    private val settings: SettingsRepository,
    private val location: LocationService,
    private val device: DeviceStatusProvider,
    private val dispatcher: AlertDispatcher,
    private val notifier: Notifier,
    private val alarm: AlarmPlayer,
) {
    private val _state = MutableStateFlow<EmergencyState>(EmergencyState.Idle)
    val state: StateFlow<EmergencyState> = _state.asStateFlow()

    private val mutex = Mutex()
    private val countdown = CountdownTimer(scope)
    private var softCheckJob: Job? = null
    private var sharingJob: Job? = null
    private var autoCallJob: Job? = null
    private var notifiedContacts: List<ContactEntity> = emptyList()
    private var incidentTripId: String? = null

    /** Set by the trip service while a confirmed trip is active. */
    @Volatile var activeTripId: String? = null

    // region Detection entry points

    fun onCrashEvent(event: CrashEvent, tripId: String?, role: TripRole) {
        scope.launch { handleCrash(event, tripId, role, null) }
    }

    fun simulate(level: SimulationLevel) {
        scope.launch { handleCrash(CrashDetectionEngine.simulated(level), activeTripId, TripRole.DRIVER, null) }
    }

    /** Triggered in Tester Mode when physical device shake exceeds the user-configured g-force threshold. */
    fun onHardwareShakeCrash(peakG: Double, peakRotationDps: Double) {
        val signals = com.dataveda.crashguard.core.sensors.CrashSignals(
            highImpact = true,
            speedDrop = true,
            abnormalRotation = peakRotationDps >= 120.0,
            stillness = true,
            noInteraction = true,
            bluetoothCar = device.isConnectedToCarBluetooth(),
        )
        val event = CrashEvent(
            timestampMs = System.currentTimeMillis(),
            confidence = com.dataveda.crashguard.core.sensors.ConfidenceScorer.score(signals),
            impactG = peakG,
            speedBeforeKmh = 36.0,
            speedAfterKmh = 0.0,
            peakRotationDps = peakRotationDps,
            orientationChangeDeg = (peakRotationDps * 0.4).coerceAtMost(180.0),
            signals = signals,
            simulated = false,
        )
        scope.launch { handleCrash(event, activeTripId, TripRole.DRIVER, null) }
    }

    /** High-confidence crash with a short countdown so the "no response" path can be demoed quickly. */
    fun simulateUnresponsive(countdownSeconds: Int = 10) {
        scope.launch {
            handleCrash(CrashDetectionEngine.simulated(SimulationLevel.HIGH), activeTripId, TripRole.DRIVER, countdownSeconds)
        }
    }

    fun simulateNeedHelp() {
        scope.launch {
            handleCrash(CrashDetectionEngine.simulated(SimulationLevel.HIGH), activeTripId, TripRole.DRIVER, null)
            delay(1_500)
            respond(UserResponse.NEED_HELP)
        }
    }

    private suspend fun handleCrash(event: CrashEvent, tripId: String?, role: TripRole, countdownOverride: Int?) = mutex.withLock {
        if (_state.value !is EmergencyState.Idle) {
            Log.i(TAG, "Crash event ignored: emergency already in progress")
            return@withLock
        }
        var band = event.band
        // Passengers / non-drivers: crash escalation disabled, only a gentle check.
        if ((role == TripRole.PASSENGER || role == TripRole.NOT_DRIVING) && band > AlertBand.NOTIFY) band = AlertBand.NOTIFY

        val status = device.current()
        val profile = profiles.get()
        val id = UUID.randomUUID().toString()
        val base = IncidentEntity(
            id = id,
            tripId = tripId,
            detectedAt = event.timestampMs,
            crashConfidence = event.confidence,
            impactG = event.impactG,
            speedBeforeKmh = event.speedBeforeKmh,
            orientationChangeDeg = event.orientationChangeDeg,
            tripType = when (profile.vehicleType) {
                VehicleType.CAR -> TripType.CAR
                VehicleType.TWO_WHEELER -> TripType.TWO_WHEELER
                VehicleType.OTHER -> TripType.UNKNOWN
            },
            source = if (event.simulated) IncidentSource.SIMULATION else IncidentSource.SENSOR,
            batteryLevel = status.batteryPercent,
            networkType = status.networkType,
            sensorSummary = event.summary(),
        )
        Log.i(TAG, "Crash event confidence=${event.confidence} band=$band ${event.summary()}")

        when (band) {
            AlertBand.IGNORE -> incidents.create(base.copy(status = IncidentStatus.LOGGED))
            AlertBand.NOTIFY -> {
                incidents.create(base.copy(status = IncidentStatus.MONITORING))
                notifier.showAreYouOkay(id)
                softCheckJob?.cancel()
                softCheckJob = scope.launch {
                    delay(SOFT_CHECK_TIMEOUT_MS)
                    softResponse(id, UserResponse.NONE)
                }
            }
            AlertBand.FULL_SCREEN, AlertBand.FULL_SCREEN_PREPARE -> {
                incidents.create(base.copy(status = IncidentStatus.ACTIVE))
                tripId?.let { trips.incrementCrashAlerts(it) }
                incidentTripId = tripId
                val seconds = countdownOverride ?: settings.current().countdownSeconds
                startCheckIn(id, event.confidence, seconds, prepare = band == AlertBand.FULL_SCREEN_PREPARE)
            }
        }
    }

    private suspend fun startCheckIn(id: String, confidence: Int, seconds: Int, prepare: Boolean) {
        _state.value = EmergencyState.CheckIn(id, confidence, seconds, seconds)
        alarm.start()
        notifier.showCrashAlert(seconds)
        launchAlertScreen()
        if (prepare) notifiedContacts = contacts.all() // immediate contact preparation (score >= 80)

        scope.launch {
            val fix = location.capture()
            _state.update { if (it is EmergencyState.CheckIn && it.incidentId == id) it.copy(fix = fix, locating = false) else it }
            if (fix != null) incidents.mutate(id) { it.withFix(fix) }
        }

        countdown.start(
            seconds,
            onTick = { s ->
                _state.update { if (it is EmergencyState.CheckIn && it.incidentId == id) it.copy(secondsLeft = s) else it }
                if (s > 0 && s % 5 == 0) notifier.showCrashAlert(s)
            },
            onFinish = { escalate(id, EscalationReason.UNRESPONSIVE_AFTER_CRASH) },
        )
    }

    // endregion

    // region User responses

    fun respond(response: UserResponse) {
        scope.launch {
            mutex.withLock {
                when (val s = _state.value) {
                    is EmergencyState.CheckIn -> when (response) {
                        UserResponse.OKAY, UserResponse.FALSE_ALARM -> {
                            stopCheckIn()
                            incidents.mutate(s.incidentId) {
                                it.copy(
                                    userResponse = response,
                                    status = if (response == UserResponse.FALSE_ALARM) IncidentStatus.FALSE_ALARM else IncidentStatus.RESOLVED,
                                    resolvedAt = System.currentTimeMillis(),
                                )
                            }
                            if (response == UserResponse.FALSE_ALARM) incidentTripId?.let { trips.incrementFalseAlarms(it) }
                            _state.value = EmergencyState.Idle
                        }
                        UserResponse.NEED_HELP -> escalateLocked(s.incidentId, EscalationReason.USER_REQUESTED_HELP)
                        UserResponse.NONE -> Unit
                    }
                    is EmergencyState.Escalated -> if (response == UserResponse.OKAY) closeLocked(markSafe = true)
                    EmergencyState.Idle -> Unit
                }
            }
        }
    }

    /** Response to the medium-confidence "Are you okay?" notification. */
    fun softResponse(incidentId: String, response: UserResponse) {
        scope.launch {
            mutex.withLock {
                softCheckJob?.cancel()
                notifier.cancelSoftCheck()
                val inc = incidents.get(incidentId) ?: return@withLock
                if (inc.status != IncidentStatus.MONITORING) return@withLock
                if (response == UserResponse.NEED_HELP && _state.value is EmergencyState.Idle) {
                    incidents.update(inc.copy(status = IncidentStatus.ACTIVE, userResponse = UserResponse.NEED_HELP))
                    incidentTripId = inc.tripId
                    escalateLocked(incidentId, EscalationReason.USER_REQUESTED_HELP)
                } else {
                    incidents.update(
                        inc.copy(
                            status = if (response == UserResponse.FALSE_ALARM) IncidentStatus.FALSE_ALARM else IncidentStatus.RESOLVED,
                            userResponse = response,
                            resolvedAt = System.currentTimeMillis(),
                        )
                    )
                }
            }
        }
    }

    /** Manual SOS from the home screen — skips the countdown. */
    fun manualSos() {
        scope.launch {
            mutex.withLock {
                when (val s = _state.value) {
                    is EmergencyState.Escalated -> launchAlertScreen()
                    is EmergencyState.CheckIn -> escalateLocked(s.incidentId, EscalationReason.USER_REQUESTED_HELP)
                    EmergencyState.Idle -> {
                        val status = device.current()
                        val id = UUID.randomUUID().toString()
                        incidents.create(
                            IncidentEntity(
                                id = id, tripId = activeTripId, detectedAt = System.currentTimeMillis(),
                                source = IncidentSource.MANUAL_SOS, status = IncidentStatus.ACTIVE,
                                userResponse = UserResponse.NEED_HELP,
                                batteryLevel = status.batteryPercent, networkType = status.networkType,
                            )
                        )
                        incidentTripId = activeTripId
                        escalateLocked(id, EscalationReason.MANUAL_SOS)
                    }
                }
            }
        }
    }

    // endregion

    // region Escalation

    private suspend fun escalate(id: String, reason: EscalationReason) = mutex.withLock {
        val s = _state.value
        if (s is EmergencyState.CheckIn && s.incidentId == id) escalateLocked(id, reason)
    }

    private suspend fun escalateLocked(id: String, reason: EscalationReason) {
        val priorFix = (_state.value as? EmergencyState.CheckIn)?.fix
        stopCheckIn()
        val cfg = settings.current()
        val endsAt = System.currentTimeMillis() + cfg.liveSharingMinutes * 60_000L
        _state.value = EmergencyState.Escalated(
            incidentId = id, reason = reason, fix = priorFix, locating = priorFix == null,
            sharingActive = true, sharingEndsAt = endsAt,
        )
        launchAlertScreen()

        val fix = priorFix ?: location.capture(timeoutMs = 6_000)
        val status = device.current()
        val incident = incidents.mutate(id) {
            val withResp = if (reason == EscalationReason.USER_REQUESTED_HELP) it.copy(userResponse = UserResponse.NEED_HELP) else it
            (if (fix != null) withResp.withFix(fix) else withResp).copy(
                status = IncidentStatus.ACTIVE, batteryLevel = status.batteryPercent, networkType = status.networkType,
            )
        } ?: return
        _state.update { if (it is EmergencyState.Escalated) it.copy(fix = fix, locating = false) else it }
        incidentTripId?.let { trips.incrementIncidents(it) }

        val list = contacts.all()
        notifiedContacts = list
        val profile = profiles.get()
        val message = AlertMessageBuilder.emergency(
            profile, incident.detectedAt, reason, fix, status.batteryPercent, status.networkType, incident.notes,
        )
        dispatcher.dispatch(id, list, AlertKind.INITIAL, message)
        _state.update { if (it is EmergencyState.Escalated) it.copy(alertsDispatched = true) else it }

        startSharing(id, fix)
        if (cfg.autoCallEnabled && reason == EscalationReason.UNRESPONSIVE_AFTER_CRASH) startAutoCall()
    }

    private fun stopCheckIn() {
        countdown.cancel()
        alarm.stop()
        notifier.cancelCrashAlert()
    }

    private fun startSharing(id: String, initialFix: GeoFix?) {
        runCatching { IncidentService.start(context) }
            .onFailure { Log.w(TAG, "Could not start incident foreground service", it) }
        sharingJob?.cancel()
        sharingJob = scope.launch {
            var lastSentFix = initialFix
            var lastSentAt = System.currentTimeMillis()
            while (isActive) {
                val s = _state.value as? EmergencyState.Escalated ?: break
                if (!s.sharingActive || System.currentTimeMillis() >= s.sharingEndsAt) break
                delay(LIVE_UPDATE_INTERVAL_MS)
                val fix = location.capture(timeoutMs = 10_000) ?: continue
                incidents.mutate(id) { it.withFix(fix) }
                _state.update { if (it is EmergencyState.Escalated && it.incidentId == id) it.copy(fix = fix) else it }
                val now = System.currentTimeMillis()
                val moved = lastSentFix?.let { distanceMeters(it, fix) } ?: Float.MAX_VALUE
                if (now - lastSentAt >= SMS_UPDATE_INTERVAL_MS && moved >= SMS_UPDATE_MIN_DISTANCE_M && notifiedContacts.isNotEmpty()) {
                    dispatcher.dispatch(id, notifiedContacts, AlertKind.LOCATION_UPDATE, AlertMessageBuilder.locationUpdate(profiles.get(), fix))
                    lastSentFix = fix
                    lastSentAt = now
                }
            }
            stopSharingInternal()
        }
    }

    private fun stopSharingInternal() {
        runCatching { IncidentService.stop(context) }
        _state.update { if (it is EmergencyState.Escalated) it.copy(sharingActive = false) else it }
    }

    fun stopSharing() {
        sharingJob?.cancel()
        sharingJob = null
        stopSharingInternal()
    }

    fun extendSharing(minutes: Int = 15) {
        _state.update {
            if (it is EmergencyState.Escalated && it.sharingActive) it.copy(sharingEndsAt = it.sharingEndsAt + minutes * 60_000L) else it
        }
    }

    private fun startAutoCall() {
        autoCallJob?.cancel()
        autoCallJob = scope.launch {
            for (s in AUTO_CALL_COUNTDOWN downTo 1) {
                _state.update { if (it is EmergencyState.Escalated) it.copy(autoCallSecondsLeft = s) else it }
                delay(1_000)
            }
            _state.update { if (it is EmergencyState.Escalated) it.copy(autoCallSecondsLeft = null) else it }
            EmergencyCaller.call112(context)
        }
    }

    fun cancelAutoCall() {
        autoCallJob?.cancel()
        autoCallJob = null
        _state.update { if (it is EmergencyState.Escalated) it.copy(autoCallSecondsLeft = null) else it }
    }

    fun addNote(note: String) {
        val id = currentIncidentId() ?: return
        scope.launch {
            incidents.mutate(id) { it.copy(notes = if (it.notes.isBlank()) note else "${it.notes}; $note") }
        }
    }

    /** Ends the active emergency. If [markSafe], contacts already alerted get an "I'm safe" SMS. */
    fun closeIncident(markSafe: Boolean) {
        scope.launch { mutex.withLock { closeLocked(markSafe) } }
    }

    private suspend fun closeLocked(markSafe: Boolean) {
        val s = _state.value as? EmergencyState.Escalated ?: return
        cancelAutoCall()
        stopSharing()
        incidents.mutate(s.incidentId) {
            it.copy(
                status = IncidentStatus.RESOLVED,
                userResponse = if (markSafe) UserResponse.OKAY else it.userResponse,
                resolvedAt = System.currentTimeMillis(),
            )
        }
        if (markSafe && s.alertsDispatched && notifiedContacts.isNotEmpty()) {
            dispatcher.dispatch(s.incidentId, notifiedContacts, AlertKind.SAFE, AlertMessageBuilder.safe(profiles.get(), System.currentTimeMillis()))
        }
        _state.value = EmergencyState.Idle
    }

    // endregion

    /** Sends a clearly labelled test alert. Logged as a test incident, separate from real ones. */
    suspend fun sendTestAlert(selected: List<ContactEntity>): String {
        val id = UUID.randomUUID().toString()
        val status = device.current()
        val fix = location.capture(timeoutMs = 5_000)
        val now = System.currentTimeMillis()
        incidents.create(
            IncidentEntity(
                id = id, detectedAt = now, source = IncidentSource.TEST, isTest = true,
                status = IncidentStatus.RESOLVED, resolvedAt = now,
                batteryLevel = status.batteryPercent, networkType = status.networkType,
            ).let { if (fix != null) it.withFix(fix) else it }
        )
        dispatcher.dispatch(id, selected, AlertKind.TEST, AlertMessageBuilder.test(profiles.get(), now, fix))
        return id
    }

    suspend fun currentAlertMessage(): String? {
        val id = currentIncidentId() ?: return null
        val inc = incidents.get(id) ?: return null
        val s = _state.value as? EmergencyState.Escalated
        val fix = s?.fix ?: inc.toFix()
        return AlertMessageBuilder.emergency(
            profiles.get(), inc.detectedAt, s?.reason ?: EscalationReason.MANUAL_SOS, fix, inc.batteryLevel, inc.networkType, inc.notes,
        )
    }

    fun currentIncidentId(): String? = when (val s = _state.value) {
        is EmergencyState.CheckIn -> s.incidentId
        is EmergencyState.Escalated -> s.incidentId
        EmergencyState.Idle -> null
    }

    fun launchAlertScreen() {
        runCatching {
            context.startActivity(
                Intent(context, CrashAlertActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
        }
    }

    companion object {
        private const val TAG = "Escalation"
        const val SOFT_CHECK_TIMEOUT_MS = 2 * 60_000L
        const val LIVE_UPDATE_INTERVAL_MS = 20_000L
        const val SMS_UPDATE_INTERVAL_MS = 5 * 60_000L
        const val SMS_UPDATE_MIN_DISTANCE_M = 50f
        const val AUTO_CALL_COUNTDOWN = 10

        fun distanceMeters(a: GeoFix, b: GeoFix): Float {
            val out = FloatArray(1)
            android.location.Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, out)
            return out[0]
        }
    }
}

fun IncidentEntity.withFix(fix: GeoFix) = copy(
    latitude = fix.latitude,
    longitude = fix.longitude,
    accuracyM = fix.accuracyM,
    locationFixTime = fix.timeMillis,
    locationIsLastKnown = fix.isLastKnown,
)

fun IncidentEntity.toFix(): GeoFix? =
    if (latitude != null && longitude != null) GeoFix(latitude, longitude, accuracyM, locationFixTime ?: detectedAt, locationIsLastKnown) else null

object EmergencyCaller {
    const val EMERGENCY_NUMBER = "112"

    /** Places the call directly when CALL_PHONE is granted, otherwise opens the dialer pre-filled with 112. */
    fun call112(context: Context) {
        val direct = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        val intent = Intent(if (direct) Intent.ACTION_CALL else Intent.ACTION_DIAL, Uri.parse("tel:$EMERGENCY_NUMBER"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }.onFailure {
            runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$EMERGENCY_NUMBER")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
    }

    fun dial(context: Context, phone: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
