package com.dataveda.crashguard.ui

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dataveda.crashguard.core.emergency.AlertDispatcher
import com.dataveda.crashguard.core.emergency.EmergencyEscalationManager
import com.dataveda.crashguard.core.emergency.EmergencyState
import com.dataveda.crashguard.core.emergency.IncidentReportExporter
import com.dataveda.crashguard.core.sensors.LiveSensorTelemetry
import com.dataveda.crashguard.core.sensors.SensorMonitor
import com.dataveda.crashguard.core.sensors.SimulationLevel
import com.dataveda.crashguard.core.trip.TripDetectionManager
import com.dataveda.crashguard.core.trip.TripMode
import com.dataveda.crashguard.core.trip.TripState
import com.dataveda.crashguard.core.utils.DeviceStatusProvider
import com.dataveda.crashguard.data.local.AlertDeliveryEntity
import com.dataveda.crashguard.data.local.AppSettings
import com.dataveda.crashguard.data.local.ContactEntity
import com.dataveda.crashguard.data.local.IncidentEntity
import com.dataveda.crashguard.data.local.SettingsRepository
import com.dataveda.crashguard.data.local.TripEntity
import com.dataveda.crashguard.data.repository.ContactRepository
import com.dataveda.crashguard.data.repository.IncidentRepository
import com.dataveda.crashguard.data.repository.ProfileRepository
import com.dataveda.crashguard.data.repository.TripRepository
import com.dataveda.crashguard.domain.models.EmergencyProfile
import com.dataveda.crashguard.domain.models.TripRole
import com.dataveda.crashguard.domain.models.UserResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CrashGuardViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
    private val contactRepo: ContactRepository,
    private val profileRepo: ProfileRepository,
    private val tripRepo: TripRepository,
    private val incidentRepo: IncidentRepository,
    private val tripManager: TripDetectionManager,
    private val escalation: EmergencyEscalationManager,
    private val dispatcher: AlertDispatcher,
    private val device: DeviceStatusProvider,
    private val sensors: SensorMonitor,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val contacts: StateFlow<List<ContactEntity>> = contactRepo.contacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val profile: StateFlow<EmergencyProfile> = profileRepo.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EmergencyProfile())

    val trips: StateFlow<List<TripEntity>> = tripRepo.trips
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val incidents: StateFlow<List<IncidentEntity>> = incidentRepo.incidents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tripState: StateFlow<TripState> = tripManager.state
    val emergencyState: StateFlow<EmergencyState> = escalation.state
    val sensorTelemetry: StateFlow<LiveSensorTelemetry> = sensors.telemetry

    val hasAccelerometer: Boolean get() = sensors.hasAccelerometer
    val hasGyroscope: Boolean get() = sensors.hasGyroscope
    fun isIgnoringBatteryOptimizations(): Boolean = device.isIgnoringBatteryOptimizations()

    fun observeIncident(id: String): Flow<IncidentEntity?> = incidentRepo.observe(id)
    fun observeDeliveries(incidentId: String): Flow<List<AlertDeliveryEntity>> = incidentRepo.deliveries(incidentId)

    fun completeOnboarding(enableProtection: Boolean) {
        viewModelScope.launch {
            settingsRepo.setOnboardingDone(true)
            setProtectionEnabled(enableProtection)
        }
    }

    fun setProtectionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setProtectionEnabled(enabled)
            if (enabled) {
                tripManager.registerTransitions()
            } else {
                tripManager.unregisterTransitions()
                if (tripManager.isAutoTrip) tripManager.stopTrip()
            }
        }
    }

    /** Switches between LIVE MODE (false) and TESTER MODE (true). */
    fun setTesterMode(testerMode: Boolean) {
        viewModelScope.launch {
            settingsRepo.setTesterMode(testerMode)
            settingsRepo.setDeveloperMode(testerMode)
            if (!testerMode) {
                sensors.stopLiveInspector()
                val t = tripManager.state.value
                if (t is TripState.Active && t.mode == TripMode.SIMULATED) {
                    tripManager.stopTrip()
                }
            }
        }
    }

    fun setSmsEnabled(enabled: Boolean) { viewModelScope.launch { settingsRepo.setSmsEnabled(enabled) } }
    fun setAutoCallEnabled(enabled: Boolean) { viewModelScope.launch { settingsRepo.setAutoCallEnabled(enabled) } }
    fun setDeveloperMode(enabled: Boolean) { viewModelScope.launch { settingsRepo.setDeveloperMode(enabled) } }
    fun setImpactThreshold(g: Float) { viewModelScope.launch { settingsRepo.setImpactThreshold(g) } }
    fun setTesterShakeThreshold(g: Float) { viewModelScope.launch { settingsRepo.setTesterShakeThreshold(g) } }

    fun startHardwareSensorInspector(armShakeTest: Boolean, shakeThresholdG: Double) {
        sensors.startLiveInspector(
            shakeThresholdG = if (armShakeTest) shakeThresholdG else null,
            onShakeCrash = { peakG, peakDps -> escalation.onHardwareShakeCrash(peakG, peakDps) },
        )
    }

    fun stopHardwareSensorInspector() = sensors.stopLiveInspector()
    fun resetSensorPeaks() = sensors.resetPeaks()

    fun startTrip(mode: TripMode) = tripManager.startTrip(mode)
    fun stopTrip() = tripManager.stopTrip()
    fun setTripRole(role: TripRole) = tripManager.setRole(role)
    fun onUserScreenTouch() = tripManager.notifyUserInteraction()

    fun saveContact(id: String?, name: String, phone: String, primary: Boolean, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = contactRepo.save(id, name, phone, primary)
            onDone(ok)
        }
    }

    fun deleteContact(id: String) { viewModelScope.launch { contactRepo.delete(id) } }
    fun setPrimaryContact(id: String) { viewModelScope.launch { contactRepo.setPrimary(id) } }

    fun saveProfile(p: EmergencyProfile, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            profileRepo.save(p)
            onSaved()
        }
    }

    fun respondToCrash(response: UserResponse) = escalation.respond(response)
    fun triggerManualSos() = escalation.manualSos()
    fun openEmergencyScreen() = escalation.launchAlertScreen()
    fun stopSharing() = escalation.stopSharing()
    fun extendSharing() = escalation.extendSharing(15)
    fun cancelAutoCall() = escalation.cancelAutoCall()
    fun addQuickNote(note: String) = escalation.addNote(note)
    fun closeIncident(markSafe: Boolean) = escalation.closeIncident(markSafe)

    fun retrySms(deliveryId: Long) { viewModelScope.launch { dispatcher.sendSms(deliveryId) } }

    fun openSmsComposer(context: Context) {
        viewModelScope.launch {
            val msg = escalation.currentAlertMessage() ?: return@launch
            val phones = contacts.value.map { it.phone }
            if (phones.isEmpty()) return@launch
            runCatching { context.startActivity(dispatcher.composeIntent(phones, msg)) }
        }
    }

    fun shareIncidentText(context: Context, incident: IncidentEntity, deliveries: List<AlertDeliveryEntity>) {
        val text = IncidentReportExporter.toPlainText(incident, profile.value, deliveries)
        runCatching { context.startActivity(dispatcher.shareIntent(text)) }
    }

    fun shareIncidentJson(context: Context, incident: IncidentEntity, deliveries: List<AlertDeliveryEntity>) {
        val json = IncidentReportExporter.toJson(incident, profile.value, deliveries)
        runCatching { context.startActivity(dispatcher.shareIntent(json)) }
    }

    fun sendTestAlert(selected: List<ContactEntity>, onSent: (String) -> Unit) {
        viewModelScope.launch {
            val id = escalation.sendTestAlert(selected)
            onSent(id)
        }
    }

    fun simulate(level: SimulationLevel) = escalation.simulate(level)
    fun simulateUnresponsive() = escalation.simulateUnresponsive(10)
    fun simulateNeedHelp() = escalation.simulateNeedHelp()

    fun clearHistory() {
        viewModelScope.launch {
            tripRepo.deleteAll()
            incidentRepo.deleteAll()
        }
    }

    fun setLanguage(tag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    fun currentLanguageTag(): String {
        val tag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        return when {
            tag.startsWith("hi") -> "hi"
            tag.startsWith("mr") -> "mr"
            else -> "en"
        }
    }
}
